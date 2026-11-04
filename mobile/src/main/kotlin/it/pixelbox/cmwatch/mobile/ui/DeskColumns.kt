package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.rules.Lavori
import it.pixelbox.cmwatch.rules.NightPage
import it.pixelbox.cmwatch.rules.Rail
import it.pixelbox.cmwatch.rules.UtilizzoRules

// La plancia del tablet senza sessioni aperte, come la PWA larga (Plancia.svelte, DeskHead, DeskNight, DeskWork, DeskTools;
// app-tablet-come-pwa-03-11): le colonne per categoria, ognuna con la sua cifra in testa, su pannelli staccati dallo sfondo.

private fun hm(at: Long): String = java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(java.time.Instant.ofEpochSecond(at).atZone(java.time.ZoneId.systemDefault()))
private fun tone(pct: Int): Color = if (pct >= 90) V3.Red else if (pct >= 75) V3.Wait else V3.Tx2

/**
 * La testa di una colonna (DeskHead.svelte): la cifra del tabellone sopra il suo argomento; il tocco apre la sua pagina. Si
 * colora solo chi ti aspetta, i rossi restano rossi; la quota delle 5 ore è neutra e diventa ambra da 75 % e rossa da 90 %, con
 * la tacca di quanto è passato della finestra.
 */
@Composable
internal fun DeskHead(kind: String, h: Kpi.Head, st: State, now: Long, onOpen: () -> Unit) {
    val rings = UtilizzoRules.shownQuota(st.quota).mapNotNull { (name, r) -> UtilizzoRules.bar(r.h5, r.w7, r.resetH5, r.resetW7, now, r.stale)?.let { b -> Triple(name, r, b) } }
    val top = rings.filter { (_, r, _) -> !r.stale }.maxByOrNull { (_, _, b) -> b.pct }
    val topPct = top?.third?.pct ?: 0
    val tag = when (kind) { "you" -> "home-box-aspetta"; "work" -> "home-box-lavoro"; "out" -> "home-box-esiti"; else -> "desk-head-tools" }
    Row(
        Modifier.testTag(tag).fillMaxWidth().heightIn(min = 92.dp).background(V3.S1).handCursor().clickable(onClick = onOpen).padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (n, color) = when (kind) {
            "you" -> h.waiting.toString() to (if (h.waiting > 0) V3.Wait else V3.Tx)
            "work" -> h.busy.toString() to V3.Tx
            "out" -> (h.outcomes?.toString() ?: "–") to V3.Tx
            else -> (if (top != null) "$topPct%" else "–") to (if (top != null && topPct >= 75) tone(topPct) else V3.Tx)
        }
        Text(n, style = V3.Section.copy(fontSize = 42.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, color = color))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(catName(kind), style = V3.Section.copy(fontSize = 19.sp, lineHeight = 22.sp))
            val sub = when (kind) {
                "you" -> if (h.waiting > 0) pluralStringResource(R.plurals.kpi7_waiting, h.waiting, h.waiting, maxOf(1L, ((h.oldestWaitS ?: 0L) + 30) / 60).toInt()) else stringResource(R.string.kpi7_none_waiting)
                "work" -> stringResource(R.string.kpi7_busy)
                "out" -> stringResource(R.string.desk_out_sub)
                else -> top?.third?.let { b ->
                    if (b.week) b.delta?.let { d -> paceText(d, b.day) } ?: stringResource(R.string.desk_tools_week)
                    else b.resetAt?.let { r -> stringResource(R.string.desk_tools_sub_reset, hm(r)) } ?: stringResource(R.string.desk_tools_sub)
                } ?: stringResource(R.string.desk_tools_sub)
            }
            Row(Modifier.padding(top = 3.dp)) {
                Text(sub, style = V3.Small.copy(fontSize = 13.5.sp, lineHeight = 17.sp))
                if (kind == "out" && (h.red ?: 0) > 0) Text(", " + pluralStringResource(R.plurals.kpi7_red, h.red!!, h.red!!), style = V3.Small.copy(fontSize = 13.5.sp, lineHeight = 17.sp, color = V3.Red, fontWeight = FontWeight.Medium))
            }
            if (kind == "tools") rings.forEach { (name, r, b) ->
                val tick = if (r.stale) null else UtilizzoRules.elapsedPct(b.resetAt, b.windowS, now)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(12.dp).testTag("desk-quota-$name"), contentAlignment = Alignment.CenterStart) {
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(V3.S3)) {
                            Box(Modifier.fillMaxWidth(b.pct.coerceIn(0, 100) / 100f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (r.stale) V3.Tx3 else tone(b.pct)))
                        }
                        // La tacca di quanto è passato della finestra.
                        tick?.let { t -> Box(Modifier.fillMaxWidth((t / 100f).coerceIn(0.01f, 1f)).fillMaxHeight()) { Box(Modifier.align(Alignment.CenterEnd).width(2.dp).fillMaxHeight().clip(RoundedCornerShape(1.dp)).background(V3.Tx)) } }
                    }
                    if (rings.size > 1) Text("${b.pct}%", style = V3.Meter.copy(fontSize = 12.sp, color = if (r.stale) V3.Tx3 else V3.Tx2), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3, modifier = Modifier.align(Alignment.Top).size(24.dp))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
}

@Composable
private fun paceText(delta: Int, day: Int?): String {
    val p = if (delta < 0) stringResource(R.string.usage_below_pace, -delta) else if (delta > 0) stringResource(R.string.usage_above_pace, delta) else stringResource(R.string.week_even)
    return if (day != null) stringResource(R.string.week_day_pace, day, p) else p
}

/** Il titolo di un gruppo di righe nella colonna (il `vsec` della PWA). */
@Composable
private fun DeskSec(icon: ImageVector, title: String, count: Int? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = V3.Name.copy(fontSize = 16.sp))
        count?.let { Text(it.toString(), style = V3.Meter, modifier = Modifier.padding(start = 6.dp)) }
    }
}

/** Una riga del gruppo: glifo, titolo e sotto la riga piccola; il tocco apre la pagina. */
@Composable
private fun DeskRow(glyph: Rail.Glyph?, title: String, small: String?, end: String? = null, nav: Boolean = false, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.handCursor().clickable(onClick = onClick) else Modifier).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { glyph?.let { StateGlyph(it, size = 18.dp) } }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // Senza glifo e senza tocco è una riga d'avviso: piccola e grigia (il `small` della PWA).
            if (glyph == null && onClick == null) Text(title, style = V3.Small, maxLines = 3)
            else Text(title, style = V3.Body.copy(fontWeight = FontWeight.Medium), maxLines = 2)
            small?.let { Text(it, style = V3.Meter, maxLines = 1) }
        }
        end?.let { Text(it, style = V3.Meter, modifier = Modifier.padding(start = 8.dp)) }
        if (nav) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3, modifier = Modifier.size(22.dp))
    }
}

/** «La notte scorsa» nella colonna Esiti (DeskNight.svelte): una riga per lavoro col suo glifo, al massimo sei. */
@Composable
internal fun DeskNight(night: NightPage.Page?, loading: Boolean, onNight: () -> Unit) {
    DeskSec(Icons.Outlined.Bedtime, stringResource(R.string.desk_last_night))
    if (night == null) { DeskRow(null, if (loading) stringResource(R.string.night_loading) else stringResource(R.string.desk_night_later), null, onClick = null); return }
    night.cards.take(6).forEach { c ->
        val g = when (c.icon) { NightPage.Icon.OK -> Rail.Glyph.DONE; NightPage.Icon.RUNNING -> Rail.Glyph.BUSY; NightPage.Icon.STOPPED -> Rail.Glyph.RED; else -> Rail.Glyph.STILL }
        DeskRow(g, c.title, listOfNotNull(c.folder, c.end?.let { e -> stringResource(R.string.desk_night_end, hm(e)) }).joinToString("  ").ifEmpty { null }, onClick = onNight)
    }
    if (night.cards.size > 6 || night.cards.isEmpty()) DeskRow(null, stringResource(R.string.desk_night_all), null, night.cards.size.toString(), nav = true, onClick = onNight)
}

/** «Fatto oggi» sotto la notte (DeskWork.svelte, parte done): le prime cinque righe, il resto nella pagina Lavori. */
@Composable
internal fun DeskDone(rows: List<Lavori.Done>, onLavori: () -> Unit) {
    DeskSec(Icons.Outlined.TaskAlt, stringResource(R.string.desk_done_today), rows.size.takeIf { it > 0 })
    if (rows.isEmpty()) DeskRow(null, stringResource(R.string.lavori_done_empty), null, onClick = null)
    rows.take(5).forEach { d -> DeskRow(Rail.Glyph.DONE, d.title, listOfNotNull(d.session, hm(d.at)).joinToString("  "), onClick = onLavori) }
    if (rows.size > 5) DeskRow(null, stringResource(R.string.desk_all_tasks), null, rows.size.toString(), nav = true, onClick = onLavori)
}

/** In fondo a Risorse (DeskTools.svelte): il PC Windows e i ricorrenti. */
@Composable
internal fun DeskTools(st: State, onTalk: (String) -> Unit) {
    DeskSec(Icons.Outlined.Computer, stringResource(R.string.desk_win))
    if (st.hostsStatus.isEmpty()) DeskRow(null, stringResource(R.string.desk_win_none), null, onClick = null)
    st.hostsStatus.forEach { h ->
        val ram = if (h.ramUsed != null && h.ramTotal != null && h.ramTotal!! > 0) "RAM ${h.ramUsed!! * 100 / h.ramTotal!!}%" else null
        DeskRow(if (h.up) Rail.Glyph.DONE else Rail.Glyph.RED, h.name, if (h.up) ram else stringResource(R.string.desk_host_down), onClick = null)
    }
    st.recurring?.takeIf { it.isNotEmpty() }?.let { rs ->
        DeskSec(Icons.Outlined.Repeat, stringResource(R.string.recurring))
        rs.forEach { r -> DeskRow(null, r.label, null, onClick = { onTalk(r.prompt) }) }
    }
}

/**
 * La plancia (Plancia.svelte): la testata fuori dalla griglia, le colonne per categoria nell'ordine della home, quante ne
 * stanno a 320 dp tolto il pannello della master, quelle in più in fondo all'ultima; la barra della master in fondo, o il
 * pannello di lato alto quanto lo schermo.
 */
@Composable
internal fun Plancia(
    widthDp: Int, order: List<String>, head: @Composable () -> Unit, column: @Composable (List<String>) -> Unit,
    bar: @Composable () -> Unit, panel: (@Composable () -> Unit)? = null, panelWidthDp: Int = 420, panelLeft: Boolean = false,
) {
    val n = it.pixelbox.cmwatch.rules.Plancia.count(widthDp.toFloat(), panel?.let { panelWidthDp.toFloat() })
    val cols = it.pixelbox.cmwatch.rules.Plancia.layout(order, n)
    Row(Modifier.fillMaxSize().background(V3.Bg).testTag("plancia")) {
        val side: @Composable () -> Unit = {
            panel?.let { p ->
                Box(Modifier.width(panelWidthDp.dp).fillMaxHeight().background(V3.S1)) { p() }
            }
        }
        if (panelLeft) { side(); if (panel != null) Box(Modifier.width(1.dp).fillMaxHeight().background(V3.Line)) }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            head()
            Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cols.forEach { parts ->
                    val shape = RoundedCornerShape(14.dp)
                    Column(Modifier.weight(1f).fillMaxHeight().clip(shape).background(V3.S1.copy(alpha = 0.6f)).then(if (V3.day) Modifier.border(1.dp, V3.Line, shape) else Modifier.border(1.dp, LitEdge, shape)).testTag("plancia-col")) {
                        // Una colonna con più categorie: una dopo l'altra nello stesso scorrimento, come nella PWA.
                        column(parts)
                    }
                }
            }
            if (panel == null) bar()
        }
        if (!panelLeft && panel != null) { Box(Modifier.width(1.dp).fillMaxHeight().background(V3.Line)); side() }
    }
}

/**
 * La master accanto a quello che c'è (App.svelte, `deskwrap`): chiusa la barra in fondo, aperta il pannello di lato alto quanto
 * lo schermo, sul lato e nella misura scelti; `stack` mette nel pannello la home sopra e la master sotto (app-tablet-come-pwa-03-11).
 */
@Composable
internal fun MasterFrame(left: Boolean, panelWidthDp: Float, panel: (@Composable () -> Unit)?, bar: (@Composable () -> Unit)?, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize().background(V3.Bg)) {
        val side: @Composable () -> Unit = { panel?.let { p -> Box(Modifier.width(panelWidthDp.dp).fillMaxHeight().background(V3.S1).testTag("master-pannello")) { p() } } }
        if (left && panel != null) { side(); Box(Modifier.width(1.dp).fillMaxHeight().background(V3.Line)) }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            bar?.invoke()
        }
        if (!left && panel != null) { Box(Modifier.width(1.dp).fillMaxHeight().background(V3.Line)); side() }
    }
}

/**
 * Home sopra e master sotto nella colonna della master (Franz 03/11 17:30-17:44): uno stacco di 12 dp, la master come scheda
 * con gli angoli tondi in alto; compatta ha la sua misura, a metà prende metà colonna.
 */
@Composable
internal fun MasterStack(half: Boolean, home: @Composable () -> Unit, master: @Composable () -> Unit, onStep: () -> Unit) {
    Column(Modifier.fillMaxSize().background(V3.Bg).testTag("master-sotto-home")) {
        Box(Modifier.weight(1f).fillMaxWidth()) { home() }
        Box(Modifier.fillMaxWidth().height(14.dp).handCursor().clickable(onClickLabel = stringResource(R.string.master_stack_step), onClick = onStep), contentAlignment = Alignment.Center) {
            Box(Modifier.width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
        }
        val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        Box((if (half) Modifier.weight(1f) else Modifier).fillMaxWidth().clip(shape).background(V3.S1).border(1.dp, V3.Line, shape)) { master() }
    }
}

