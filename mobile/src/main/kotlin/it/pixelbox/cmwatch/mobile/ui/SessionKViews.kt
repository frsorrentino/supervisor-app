package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SessionK

/*
 * La scheda aperta di una sessione nella forma K (app-scheda-sessione-semplice-19-11, Franz 19/11 18:42; bozza
 * docs/mockups/scheda-sessione-19-11/k1.html). Un binario a sinistra fa da linea del tempo, un nodo per gruppo: Prima, Risultati,
 * Adesso, Prossimo, dall'alto come la chat; ogni riga è etichetta, testo e al massimo un tasto, e il tasto pieno sta in fondo.
 * Rifinita con impeccable, apple-design, mobile-native ed emil-design-eng: righe toccabili alte almeno 44 dp, monospazio solo per ore
 * e minuti, icone disegnate, una sola misura di etichetta.
 */

private val KLabel = 64.dp
private val KRail = 22.dp

/** I gruppi della linea del tempo, col binario che li unisce. */
@Composable
internal fun KTimeline(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val rail = V3.S3
    Column(
        modifier.fillMaxWidth().testTag("scheda-k").drawBehind {
            val x = 5.dp.toPx()
            drawRoundRect(rail, Offset(x, 12.dp.toPx()), Size(2.dp.toPx(), (size.height - 30.dp.toPx()).coerceAtLeast(0f)), CornerRadius(1.dp.toPx()))
        },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

enum class KTone { PAST, NOW, NEXT }

/** Un gruppo: il nodo sul binario, il titolo e una nota piccola accanto. */
@Composable
internal fun KGroup(title: String, tone: KTone, note: String? = null, tag: String, content: @Composable ColumnScope.() -> Unit) {
    val node = when (tone) { KTone.PAST -> V3.Tx3; KTone.NOW -> V3.Busy; KTone.NEXT -> V3.Wait }
    Column(Modifier.fillMaxWidth().testTag(tag)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(KRail), contentAlignment = Alignment.CenterStart) {
                Box(
                    Modifier.padding(start = 1.dp).size(10.dp).clip(CircleShape)
                        .then(if (tone == KTone.PAST) Modifier.background(V3.S1).border(2.dp, node, CircleShape) else Modifier.background(node)),
                )
            }
            Text(title, style = V3.Name.copy(color = if (tone == KTone.NEXT) V3.Wait else V3.Tx))
            note?.let { Text("  $it", style = V3.Small.copy(color = V3.Tx3), maxLines = 1) }
        }
        Column(Modifier.padding(start = KRail)) { content() }
    }
}

/** Una riga: l'etichetta piccola, il testo e al massimo un tasto. [onTap] la rende toccabile per intero. */
@Composable
internal fun KRow(label: String, mono: Boolean = false, onTap: (() -> Unit)? = null, action: (@Composable RowScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
            .then(if (onTap != null) Modifier.handCursor().clickable(onClick = onTap) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = if (mono) V3.Meter.copy(color = V3.Tx2) else V3.Small.copy(color = V3.Tx2), maxLines = 2, modifier = Modifier.width(KLabel).padding(top = 1.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
        if (action != null) Row(Modifier.padding(start = 10.dp).align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) { action() }
    }
}

/** Prima: «Altri N turni prima» in cima, poi gli ultimi turni dal più vecchio; il tocco apre la conversazione. */
@Composable
internal fun KTurns(b: SessionK.Before, onAll: () -> Unit, onOpen: () -> Unit) {
    if (b.hidden > 0) Text(
        pluralStringResource(R.plurals.k_more_turns, b.hidden, b.hidden), style = V3.Body.copy(color = V3.Busy),
        modifier = Modifier.heightIn(min = 44.dp).handCursor().clickable(onClick = onAll).padding(vertical = 12.dp).testTag("k-altri-turni"),
    )
    b.turns.forEach { t ->
        KRow(hhmmOf(t.start), mono = true, onTap = onOpen, action = {
            Text(stringResource(R.string.k_minutes, t.minutes), style = V3.Meter.copy(color = V3.Tx3), maxLines = 1, modifier = Modifier.widthIn(min = 44.dp))
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (t.ok) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = if (t.ok) V3.Good else V3.Red, modifier = Modifier.size(18.dp))
                Text(
                    t.text ?: stringResource(if (t.ok) R.string.k_turn_ok else R.string.k_turn_failed),
                    style = V3.Body.copy(color = if (t.text == null) V3.Tx2 else V3.Tx, fontStyle = if (t.text == null) FontStyle.Italic else FontStyle.Normal),
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fadeEnd(),
                )
            }
        }
    }
}

/** La barra del contesto della riga Contesto: la percentuale accanto, ambra o rossa oltre le soglie. */
@Composable
internal fun KContextBar(fraction: Float, pct: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).heightIn(min = 6.dp).clip(RoundedCornerShape(3.dp)).background(V3.S3)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).heightIn(min = 6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
        Text("$pct%", style = V3.Meter.copy(color = if (color == V3.Busy) V3.Tx else color))
    }
}

/**
 * La pagina Utilizzo dopo la K: per ogni sessione che ha lavorato oggi, la più recente in cima, il nome con l'attività per ora e
 * «oggi Y%» dell'account, poi la giornata a fasce e i token per turno. Prima stavano nella carta aperta; come «Sessioni di oggi» in web/src/lib/SessionsToday.svelte.
 */
@Composable
internal fun KSessionsToday(st: it.pixelbox.cmwatch.contract.State, page: it.pixelbox.cmwatch.contract.TimelinePage?, data: it.pixelbox.cmwatch.contract.KpiData?, now: Long, zone: java.time.ZoneId) {
    val from = it.pixelbox.cmwatch.rules.Kpi.midnight(now, zone)
    val names = androidx.compose.runtime.remember(page, data, from) { SessionK.todaySessions(page, data, from) }
    if (names.isEmpty()) return
    val events = androidx.compose.runtime.remember(page) { page?.sessions.orEmpty().associate { it.session to it.events } }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(V3.S1).border(1.dp, V3.Line, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp).testTag("sessioni-oggi"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.sessions_today_title), style = V3.Section.copy(fontSize = 17.sp))
        names.forEachIndexed { i, name ->
            val s = st.sessions.firstOrNull { it.name == name }
            val live = s != null && s.state != it.pixelbox.cmwatch.contract.SessionState.GONE
            val ev = events[name].orEmpty()
            val share = it.pixelbox.cmwatch.rules.Kpi.todayShare(st, name)
            val turns = data?.let { d -> it.pixelbox.cmwatch.rules.Kpi.turnsOf(d, name) }.orEmpty()
            if (i > 0) Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(V3.Line))
            Column(Modifier.fillMaxWidth().testTag("sessione-oggi"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(name, style = V3.Body.copy(color = V3.Tx), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f).fadeEnd())
                    if (ev.isNotEmpty() && live) Kpi6(it.pixelbox.cmwatch.rules.Kpi.hourly(ev, now))
                    share?.let { p -> Text(stringResource(R.string.kpi5_today, p), style = V3.Meter.copy(color = V3.Tx2), maxLines = 1) }
                }
                if (ev.isNotEmpty()) Kpi11(it.pixelbox.cmwatch.rules.Kpi.day(ev, from, now, if (live) it.pixelbox.cmwatch.rules.Kpi.waitingSince(s!!) else null), from, now)
                if (turns.isNotEmpty()) Kpi10(turns)
            }
        }
    }
}
