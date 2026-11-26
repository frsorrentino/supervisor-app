package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Sell
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
internal fun KRow(label: String, mono: Boolean = false, onTap: (() -> Unit)? = null, action: (@Composable RowScope.() -> Unit)? = null, stack: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    // «Ti aspetta» porta la domanda e le scelte (app-colonna-ti-aspetta-24-11): accanto all'etichetta, a 320 dp col testo grande,
    // restavano 120 dp e si spezzavano le parole («consigli» / «ata»). Con `stack`, se accanto restano meno di 10 em del testo,
    // l'etichetta sta sopra e il contenuto prende tutta la riga.
    if (stack) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val minText = with(androidx.compose.ui.platform.LocalDensity.current) { (V3.Body.fontSize * 10).toDp() }
            if (maxWidth - KLabel - 12.dp >= minText) KRow(label, mono, onTap, action, stack = false, content)
            else Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(if (onTap != null) Modifier.handCursor().clickable(onClick = onTap) else Modifier).padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(label, style = if (mono) V3.Meter.copy(color = V3.Tx2) else V3.Small.copy(color = V3.Tx2))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
                if (action != null) Row(verticalAlignment = Alignment.CenterVertically) { action() }
            }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
            .then(if (onTap != null) Modifier.handCursor().clickable(onClick = onTap) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Mai una parola spezzata (app-tasti-riscontro-20-11: col testo grande di Franz «Contest» / «o»): la colonna è larga KLabel
        // o quanto la parola più lunga dell'etichetta, e va a capo solo fra le parole.
        Text(label, style = if (mono) V3.Meter.copy(color = V3.Tx2) else V3.Small.copy(color = V3.Tx2), maxLines = 2, modifier = Modifier.widthIn(min = KLabel).width(IntrinsicSize.Min).padding(top = 1.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
        if (action != null) Row(Modifier.padding(start = 10.dp).align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) { action() }
    }
}

/** Prima: «Altri N turni prima» in cima, poi gli ultimi turni dal più vecchio; il tocco apre la conversazione. */
@Composable
internal fun KTurns(b: SessionK.Before, onAll: () -> Unit, onOpen: (entry: String?) -> Unit) {
    if (b.hidden > 0) Text(
        pluralStringResource(R.plurals.k_more_turns, b.hidden, b.hidden), style = V3.Body.copy(color = V3.Busy),
        modifier = Modifier.heightIn(min = 44.dp).handCursor().clickable(onClick = onAll).padding(vertical = 12.dp).testTag("k-altri-turni"),
    )
    b.turns.forEach { t ->
        KRow(hhmmOf(t.start), mono = true, onTap = { onOpen(t.entry) }, action = {
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

/**
 * Contratto 1.156, Risultati: i risultati di oggi come pillole, il più nuovo per primo, che vanno a capo; un link o una release si apre
 * fuori, un file o un'immagine sulla sua voce della chat. Oltre cinque, quattro e «Altri N». Come la sezione k-risultati di Home.svelte.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun KResults(r: SessionK.Results, onAll: () -> Unit, onEntry: (String?) -> Unit) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        r.shown.forEach { x ->
            val icon = when (x.kind) {
                "image" -> Icons.Rounded.Image; "file" -> Icons.Rounded.Description
                "release" -> Icons.Rounded.Sell; else -> Icons.Rounded.Link
            }
            Row(
                Modifier.heightIn(min = 44.dp).clip(CircleShape).background(V3.S2).handCursor()
                    .clickable { val u = x.url; if (u != null) runCatching { uri.openUri(u) } else onEntry(x.entry) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(16.dp))
                Text(x.label, style = V3.Body, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f, fill = false).fadeEnd())
            }
        }
        if (r.hidden > 0) Text(
            pluralStringResource(R.plurals.k_more_results, r.hidden, r.hidden), style = V3.Body.copy(color = V3.Busy),
            modifier = Modifier.align(Alignment.CenterVertically).heightIn(min = 44.dp).handCursor().clickable(onClick = onAll).padding(vertical = 12.dp).testTag("k-altri-risultati"),
        )
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

// ---- app-scheda-sessione-a-26-11 (proposta A approvata da Franz il 26/11): sezioni a tutta larghezza, senza binario né etichette ----

/** Le sezioni della scheda aperta, una sotto l'altra, divise da una riga. */
@Composable
internal fun KSections(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().testTag("scheda-k"), content = content)

/**
 * Una sezione: l'etichetta piccola in mono maiuscolo e, a destra, una nota (l'ora, «2 turni oggi»); [you] è «Tocca a te», un
 * riquadro ambra col suo tasto.
 */
@Composable
internal fun KSection(label: String, tag: String, note: String? = null, you: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val head: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = V3.Meter.copy(color = if (you) V3.Wait else V3.Tx3, letterSpacing = 0.08.em), maxLines = 1, modifier = Modifier.weight(1f))
            note?.let { Text(it, style = V3.Meter.copy(color = V3.Tx3), maxLines = 1) }
        }
    }
    if (you) Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(V3.Wait.copy(alpha = 0.09f).compositeOver(V3.S1))
            .border(1.dp, V3.Wait.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp).testTag(tag),
    ) { head(); content() }
    else Column(Modifier.fillMaxWidth().testTag(tag)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp)) { head(); content() }
    }
}

/** Il compito di una sezione: il nome a sinistra, lo stato a destra (in corso in blu, in pausa in ambra). */
@Composable
internal fun KTaskLine(title: String, state: String? = null, paused: Boolean = false) =
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = V3.Name.copy(fontSize = 17.sp), modifier = Modifier.weight(1f))
        state?.let { Text(it, style = V3.Small.copy(color = if (paused) V3.Wait else V3.Busy), maxLines = 1) }
    }

/**
 * Le tappe della prova come barra a 4 segmenti col nome dentro, sotto il compito che verificano: passata verde con ✓, fallita rossa
 * con ✕, quella in corso col bordo blu, da fare grigia; sotto il motivo a parole. Prende il posto del filo in alto.
 */
@Composable
internal fun KStepBar(steps: it.pixelbox.cmwatch.contract.ProofSteps) {
    if (steps.items.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("passi-prova"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            steps.items.forEach { st ->
                val look = it.pixelbox.cmwatch.rules.PassiProva.look(st)
                val (fg, bg, line) = when (look) {
                    it.pixelbox.cmwatch.rules.PassiProva.Look.DONE -> Triple(V3.Good, V3.Good.copy(alpha = 0.16f).compositeOver(V3.S1), V3.Good.copy(alpha = 0.45f))
                    it.pixelbox.cmwatch.rules.PassiProva.Look.FAILED -> Triple(V3.Red, V3.Red.copy(alpha = 0.16f).compositeOver(V3.S1), V3.Red.copy(alpha = 0.45f))
                    it.pixelbox.cmwatch.rules.PassiProva.Look.NOW -> Triple(V3.Busy, V3.S2, V3.Busy)
                    it.pixelbox.cmwatch.rules.PassiProva.Look.TODO -> Triple(V3.Tx2, V3.S2, V3.Line)
                }
                val mark = when (look) { it.pixelbox.cmwatch.rules.PassiProva.Look.DONE -> "✓ "; it.pixelbox.cmwatch.rules.PassiProva.Look.FAILED -> "✕ "; else -> "" }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(bg).border(1.dp, line, RoundedCornerShape(6.dp)).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                    Text(mark + st.label, style = V3.Small.copy(fontSize = 13.sp, color = fg), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                }
            }
        }
        it.pixelbox.cmwatch.rules.PassiProva.line(steps)?.let { l ->
            val text = when (l.say) {
                it.pixelbox.cmwatch.rules.PassiProva.Say.RUNNING -> stringResource(R.string.proof_running, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.FAILED -> stringResource(R.string.proof_failed, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.TODO -> stringResource(R.string.proof_todo, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.ALL_DONE -> stringResource(R.string.proof_all_done)
            }
            Text(text, style = V3.Small.copy(color = if (l.say == it.pixelbox.cmwatch.rules.PassiProva.Say.FAILED) V3.Red else V3.Tx2))
        }
    }
}
