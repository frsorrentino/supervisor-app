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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import kotlinx.coroutines.launch
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.rules.MasterLanding as L
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
internal fun DeskHead(kind: String, h: Kpi.Head, st: State, now: Long, open: Int? = null, onOpen: () -> Unit) {
    val rings = UtilizzoRules.shownQuota(st.quota).mapNotNull { (name, r) -> UtilizzoRules.bar(r.h5, r.w7, r.resetH5, r.resetW7, now, r.stale)?.let { b -> Triple(name, r, b) } }
    val top = rings.filter { (_, r, _) -> !r.stale }.maxByOrNull { (_, _, b) -> b.pct }
    val topPct = top?.third?.pct ?: 0
    val tag = when (kind) { "you" -> "home-box-aspetta"; "work" -> "home-box-lavoro"; "out" -> "home-box-esiti"; else -> "desk-head-tools" }
    Row(
        Modifier.testTag(tag).fillMaxWidth().heightIn(min = 92.dp).background(V3.S2).handCursor().clickable(onClick = onOpen).padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (n, color) = when (kind) {
            "you" -> h.waiting.toString() to (if (h.waiting > 0) V3.Wait else V3.Tx)
            // Con `open` la testa di «In corso» è quella delle Sessioni (app-sessioni-evidenti-06-11, J2).
            "work" -> (open ?: h.busy).toString() to V3.Tx
            "out" -> (h.outcomes?.toString() ?: "–") to V3.Tx
            else -> (if (top != null) "$topPct%" else "–") to (if (top != null && topPct >= 75) tone(topPct) else V3.Tx)
        }
        Text(n, style = V3.Section.copy(fontSize = 42.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, color = color))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (kind == "work" && open != null) stringResource(R.string.desk_sessions_title) else catName(kind), style = V3.Section.copy(fontSize = 19.sp, lineHeight = 22.sp))
            val sub = when (kind) {
                "you" -> if (h.waiting > 0) pluralStringResource(R.plurals.kpi7_waiting, h.waiting, h.waiting, maxOf(1L, ((h.oldestWaitS ?: 0L) + 30) / 60).toInt()) else stringResource(R.string.kpi7_none_waiting)
                "work" -> if (open != null) stringResource(R.string.desk_sessions_sub, open, h.busy) else stringResource(R.string.kpi7_busy)
                "out" -> stringResource(R.string.desk_out_sub)
                else -> top?.third?.let { b ->
                    if (b.week) b.delta?.let { d -> paceText(d, b.day) } ?: stringResource(R.string.desk_tools_week)
                    else b.resetAt?.let { r -> stringResource(R.string.desk_tools_sub_reset, hm(r)) } ?: stringResource(R.string.desk_tools_sub)
                } ?: stringResource(R.string.desk_tools_sub)
            }
            Row(Modifier.padding(top = 3.dp)) {
                Text(sub, style = V3.Small.copy(fontSize = 14.sp, lineHeight = 17.sp))
                if (kind == "out" && (h.red ?: 0) > 0) Text(", " + pluralStringResource(R.plurals.kpi7_red, h.red!!, h.red!!), style = V3.Small.copy(fontSize = 14.sp, lineHeight = 17.sp, color = V3.Red, fontWeight = FontWeight.Medium))
            }
            if (kind == "tools") rings.forEach { (name, r, b) ->
                val tick = if (r.stale) null else UtilizzoRules.elapsedPct(b.resetAt, b.windowS, now)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(12.dp).testTag("desk-quota-$name"), contentAlignment = Alignment.CenterStart) {
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(V3.S3)) {
                            Box(Modifier.fillMaxWidth(b.pct.coerceIn(0, 100) / 100f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (r.stale) V3.Tx3 else tone(b.pct)))
                        }
                        // La tacca di quanto è passato della finestra.
                        tick?.let { t -> Box(Modifier.fillMaxWidth((t / 100f).coerceIn(0.01f, 1f)).fillMaxHeight()) { Box(Modifier.align(Alignment.CenterEnd).width(2.dp).fillMaxHeight().clip(RoundedCornerShape(1.dp)).background(Cm.qNow)) } }
                    }
                    if (rings.size > 1) Text("${b.pct}%", style = V3.Meter.copy(fontSize = 12.5.sp, color = if (r.stale) V3.Tx3 else V3.Tx2), modifier = Modifier.padding(start = 8.dp))
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

/**
 * Esiti chiuso in fondo a Tocca a te (app-sessioni-evidenti-06-11, J2): come il titolo di un gruppo, col numero di oggi; il tocco
 * apre il foglio degli esiti. Come `.outclosed` della web.
 */
@Composable
internal fun DeskOutClosed(n: Int?, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).handCursor().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp).testTag("plancia-esiti-chiuso"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Outlined.TaskAlt, null, tint = V3.Tx2, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.desk_out_closed), style = V3.Section.copy(fontSize = 17.sp))
        Text((n ?: 0).toString(), style = V3.Small)
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3, modifier = Modifier.size(22.dp))
    }
}

/**
 * Il titolo di un gruppo di righe nella colonna (il `vsec` della PWA). Con `open` è il tasto che lo apre e lo chiude, con la
 * freccia, come le sezioni di Tocca a te (web-pubblicazioni-voci-06-11: le sezioni di Risorse).
 */
@Composable
private fun DeskSec(icon: ImageVector, title: String, count: Int? = null, open: Boolean? = null, onToggle: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().then(if (open != null) Modifier.handCursor().clickable(onClick = onToggle) else Modifier).padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = V3.Name.copy(fontSize = 15.sp))
        count?.let { Text(it.toString(), style = V3.Meter, modifier = Modifier.padding(start = 6.dp)) }
        if (open != null) {
            Spacer(Modifier.weight(1f))
            Icon(if (open) androidx.compose.material.icons.Icons.Rounded.ExpandLess else androidx.compose.material.icons.Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.size(20.dp))
        }
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
    var winOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
    var recurringOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
    DeskSec(Icons.Outlined.Computer, stringResource(R.string.desk_win), open = winOpen, onToggle = { winOpen = !winOpen })
    if (winOpen && st.hostsStatus.isEmpty()) DeskRow(null, stringResource(R.string.desk_win_none), null, onClick = null)
    if (winOpen) st.hostsStatus.forEach { h ->
        val ram = if (h.ramUsed != null && h.ramTotal != null && h.ramTotal!! > 0) "RAM ${h.ramUsed!! * 100 / h.ramTotal!!}%" else null
        DeskRow(if (h.up) Rail.Glyph.DONE else Rail.Glyph.RED, h.name, if (h.up) ram else stringResource(R.string.desk_host_down), onClick = null)
    }
    st.recurring?.takeIf { it.isNotEmpty() }?.let { rs ->
        DeskSec(Icons.Outlined.Repeat, stringResource(R.string.recurring), rs.size, open = recurringOpen, onToggle = { recurringOpen = !recurringOpen })
        if (recurringOpen) rs.forEach { r -> DeskRow(null, r.label, null, onClick = { onTalk(r.prompt) }) }
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
    /** Lo spazio già preso dalla master nel telaio intorno (MasterFrame): le colonne contano senza. */
    widthOffsetDp: Int = 0,
) {
    val n = it.pixelbox.cmwatch.rules.Plancia.count((widthDp - widthOffsetDp).toFloat(), panel?.let { panelWidthDp.toFloat() })
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
                cols.forEachIndexed { i, parts ->
                    val shape = RoundedCornerShape(14.dp)
                    Column(Modifier.weight(1f).bootWave(i).fillMaxHeight().clip(shape).background(V3.S1).then(if (V3.day) Modifier.border(1.dp, V3.Line, shape) else Modifier.border(1.dp, LitEdge, shape)).testTag("plancia-col")) {
                        // Una colonna con più categorie: una dopo l'altra nello stesso scorrimento, come nella PWA.
                        androidx.compose.runtime.CompositionLocalProvider(LocalInColumn provides true) { column(parts) }
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
 * lo schermo, sul lato e nella misura scelti (app-tablet-come-pwa-03-11).
 *
 * I gesti della PWA (masterLanding, app-master-trascina-03-11), se `onPlace` c'è: la maniglia in testa al pannello cambia solo il
 * lato; il bordo verso le sessioni allarga e stringe fra i posti fissi a colonna e a metà schermo; la maniglia in alto del pannello,
 * o la barra tirata su, cambia l'altezza a scatti. Anteprima a fantasma (scelta A di Franz, 04/11 17:21, app-tablet-scelta-a-04-11,
 * come landingMorph.ts): durante il gesto la pagina resta ferma, lo sfondo si scurisce appena e la sagoma gialla occupa il posto
 * d'arrivo con la misura vera; gli altri posti sono contorni tratteggiati con l'etichetta, e spostando di lato la linea a metà
 * schermo dice con la pillola che basta passarla. Al rilascio `onPlace(…, true)` mette la master lì e ricorda; annullato niente cambia.
 */
@Composable
internal fun MasterFrame(
    left: Boolean, panelWidthDp: Float, panel: (@Composable () -> Unit)?, bar: (@Composable () -> Unit)?,
    place: it.pixelbox.cmwatch.rules.MasterPlace? = null, open: Boolean = panel != null, sessionsOpen: Boolean = false,
    onPlace: ((it.pixelbox.cmwatch.rules.MasterPlace, Boolean, Boolean) -> Unit)? = null,
    /** La home sta sopra la master nel pannello: la presa in alto è la maniglia fra le due (MasterStack), non la testa del pannello. */
    stacked: Boolean = false,
    content: @Composable () -> Unit,
) {
    val edge = androidx.compose.runtime.remember { EdgeSpots() }
    var ghost by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Ghost?>(null) }
    var hints by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Ghost>>(emptyList()) }
    var mid by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Mid?>(null) }
    val names = mapOf<Any, String>(
        L.Side.LEFT to stringResource(R.string.master_pos_left), L.Side.RIGHT to stringResource(R.string.master_pos_right),
        L.WidthStop.COLUMN to stringResource(R.string.master_land_column), L.WidthStop.HALF_SCREEN to stringResource(R.string.master_half_screen),
        L.Height.COLUMN to stringResource(R.string.master_land_column), L.Height.HALF to stringResource(R.string.master_land_half),
        L.Height.COMPACT to stringResource(R.string.master_land_compact), L.Height.BAR to stringResource(R.string.master_land_bar),
    )
    val midLeft = stringResource(R.string.master_mid_left); val midRight = stringResource(R.string.master_mid_right)
    val gripLeft = stringResource(R.string.master_bar_grip_left); val gripRight = stringResource(R.string.master_bar_grip_right)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // Il gesto lo legge il telaio, che resta fermo: le maniglie dicono solo dove stanno (come `follow` della PWA).
    // Un solo riconoscitore: il doppio tocco su una maniglia è un gesto solo.
    val taps = androidx.compose.runtime.remember { it.pixelbox.cmwatch.rules.MasterGestures.Taps() }
    val grips = androidx.compose.runtime.remember { mutableMapOf<String, androidx.compose.ui.layout.LayoutCoordinates>() }
    var frame by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    // Le misure vere della riga e del pannello, per la sagoma del posto di adesso.
    val seen = androidx.compose.runtime.remember { mutableMapOf<String, androidx.compose.ui.layout.LayoutCoordinates>() }
    val placeNow by androidx.compose.runtime.rememberUpdatedState(place)
    val openNow by androidx.compose.runtime.rememberUpdatedState(open)
    val underOk by androidx.compose.runtime.rememberUpdatedState(sessionsOpen)
    val change by androidx.compose.runtime.rememberUpdatedState(onPlace)
    // Il cambio di lato continua il gesto (app-lato-animazione-06-11, Franz 06/11 00:40: l'atterraggio era «avulso dal contesto
    // del movimento appena fatto»): il pannello segue il dito; al rilascio parte da lì con la velocità del dito, le colonne dal
    // loro posto di prima, e arrivano insieme con una molla breve senza rimbalzo (MasterGestures.SIDE_SPRING_*). Px nel telaio.
    var panelOff by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var contentOff by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var sideFade by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val sideScope = androidx.compose.runtime.rememberCoroutineScope()
    val motionOff by androidx.compose.runtime.rememberUpdatedState(animationsOff())
    val sideSpring = androidx.compose.animation.core.spring<Float>(dampingRatio = it.pixelbox.cmwatch.rules.MasterGestures.SIDE_SPRING_DAMPING, stiffness = it.pixelbox.cmwatch.rules.MasterGestures.SIDE_SPRING_STIFFNESS)
    // Anche l'altezza continua il gesto (app-altezza-animazione-06-11, Franz 06/11 00:55): la master che si sposta (la riga, il
    // pannello o la master nella pila) segue il dito in verticale; al rilascio quella d'arrivo parte da dove il dito ha lasciato
    // l'altra, con la sua velocità, e va al suo posto con la stessa molla. Niente schiacciamento d'atterraggio.
    val lift = androidx.compose.runtime.remember { MasterLift() }
    fun settleLift(from: Float, velocity: Float) {
        lift.y = from
        if (motionOff) { lift.y = 0f; if (from != 0f) { lift.alpha = 0f; sideScope.launch { androidx.compose.animation.core.animate(0f, 1f, animationSpec = androidx.compose.animation.core.tween(180)) { v, _ -> lift.alpha = v } } }; return }
        sideScope.launch { androidx.compose.animation.core.animate(from, 0f, velocity, sideSpring) { v, _ -> lift.y = v } }
    }
    fun settle(fromPanel: Float, fromContent: Float, velocity: Float) {
        panelOff = fromPanel; contentOff = fromContent
        if (motionOff) {
            panelOff = 0f; contentOff = 0f
            if (fromContent != 0f) { sideFade = 0f; sideScope.launch { androidx.compose.animation.core.animate(0f, 1f, animationSpec = androidx.compose.animation.core.tween(180)) { v, _ -> sideFade = v } } }
            return
        }
        sideScope.launch { androidx.compose.animation.core.animate(fromPanel, 0f, velocity, sideSpring) { v, _ -> panelOff = v } }
        if (fromContent != 0f) sideScope.launch { androidx.compose.animation.core.animate(fromContent, 0f, 0f, sideSpring) { v, _ -> contentOff = v } }
    }
    val namesNow by androidx.compose.runtime.rememberUpdatedState(names)
    // L'atterraggio (punto 2): arrivata al posto della sagoma, la master si schiaccia un poco verso il basso e torna.
    var landed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val squash = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(1f) }
    val squashOff = animationsOff()
    androidx.compose.runtime.LaunchedEffect(landed) { if (landed > 0 && !squashOff) { squash.snapTo(0.96f); squash.animateTo(1f, Fx.spring()) } }
    fun Modifier.landing() = this.graphicsLayer { scaleY = squash.value; scaleX = 2f - squash.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }
    fun Modifier.grip(key: String) = this.onGloballyPositioned { c -> grips[key] = c }
    val gripOf: (String) -> Modifier = { k -> Modifier.grip(k) }
    fun Modifier.seen(key: String) = this.onGloballyPositioned { c -> seen[key] = c }
    androidx.compose.foundation.layout.BoxWithConstraints(
        // Il telaio sta dentro le barre di sistema (app-tablet-barre-bordi-05-11): il pannello saliva sotto la barra di stato, con la
        // maniglia in alto dove il gesto lo prende Android; la riga scendeva sulla barra dei gesti.
        Modifier.fillMaxSize().background(V3.Bg)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars.union(androidx.compose.foundation.layout.WindowInsets.navigationBars))
            .onGloballyPositioned { frame = it }.then(if (onPlace == null) Modifier else Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial
                val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
                val f = frame?.takeIf { it.isAttached } ?: return@awaitEachGesture
                // La presa più piccola sotto il dito: la maniglia vince sulla riga che la contiene. Ogni presa si tocca su almeno
                // 48 dp per lato attorno al suo centro, anche dove il segno è sottile (app-tablet-barre-bordi-05-11).
                val minPx = 48.dp.toPx()
                fun hit(b: androidx.compose.ui.geometry.Rect): androidx.compose.ui.geometry.Rect {
                    val hw = maxOf(b.width, minPx) / 2; val hh = maxOf(b.height, minPx) / 2
                    return androidx.compose.ui.geometry.Rect(b.center.x - hw, b.center.y - hh, b.center.x + hw, b.center.y + hh)
                }
                var key = grips.entries.filter { (_, c) -> c.isAttached && hit(f.localBoundingBoxOf(c, false)).contains(down.position) }
                    .minByOrNull { (_, c) -> c.size.width.toLong() * c.size.height }?.key ?: return@awaitEachGesture
                val p0 = placeNow ?: return@awaitEachGesture
                val open0 = openNow
                val W = size.width / density; val H = size.height / density
                val area = L.Rect(0f, 0f, W, H)
                fun rectOf(k: String): L.Rect? = seen[k]?.takeIf { it.isAttached }?.let { c -> f.localBoundingBoxOf(c, false) }
                    ?.takeIf { it.width > 0f }?.let { b -> L.Rect(b.left / density, b.top / density, b.width / density, b.height / density) }
                val barH = rectOf("bar")?.h ?: L.BAR_H
                val h0 = p0.copy(open = open0).height(underOk)
                // Il posto di adesso con le misure vere; gli altri da masterLanding.
                val here = if (h0 == L.Height.BAR) rectOf("bar") else if (h0 == L.Height.COLUMN) rectOf("panel") else null
                fun rectFor(l: Boolean, h: L.Height, w: Float) = here?.takeIf { l == p0.left && h == h0 && w == p0.panelWidth() } ?: L.placeRect(area, l, h, w, barH)
                var moved = false
                var aim: Pair<it.pixelbox.cmwatch.rules.MasterPlace, Boolean>? = null
                var widthNow = p0.panelWidth()
                val sideHold = L.Steady(if (p0.left) L.Side.LEFT else L.Side.RIGHT)
                // La corsa breve (app-master-corsa-breve-05-11): per larghezza e altezza conta la direzione presa dal posto di
                // partenza; oltre 24 dp, o con un colpo, il posto dopo da quella parte (MasterGestures.step).
                val speed = androidx.compose.ui.input.pointer.util.VelocityTracker()
                val x0 = down.position.x / density; val y0 = down.position.y / density
                val wStops = L.widthStops(W)
                val wFrom = wStops.indices.minBy { kotlin.math.abs(wStops[it] - p0.panelWidth()) }
                val col0 = rectFor(p0.left, L.Height.COLUMN, p0.panelWidth())
                fun aimAt(key: String, x: Float, y: Float, vx: Float = 0f, vy: Float = 0f) {
                    if (key == "width") {
                        // Posti fissi (Franz 04/11 10:29-10:30): la sagoma salta a un posto, mai nel mezzo; quale lo dice la direzione.
                        val sgn = if (p0.left) 1f else -1f
                        widthNow = wStops[it.pixelbox.cmwatch.rules.MasterGestures.step(wStops, wFrom, (x - x0) * sgn, vx * sgn)]
                        ghost = Ghost(L.widthRect(col0, p0.left, widthNow), namesNow.getValue(if (widthNow == L.COLUMN) L.WidthStop.COLUMN else L.WidthStop.HALF_SCREEN))
                        hints = wStops.filter { it != widthNow }.map { s -> Ghost(L.widthRect(col0, p0.left, s), namesNow.getValue(if (s == L.COLUMN) L.WidthStop.COLUMN else L.WidthStop.HALF_SCREEN)) }
                        aim = p0.widthDropped(widthNow, W) to open0
                        return
                    }
                    // Solo verticale: il lato è quello della maniglia presa nella riga, se no quello di adesso.
                    val l = when (key) { "bar-left" -> true; "bar-right" -> false; else -> p0.left }
                    val w = p0.panelWidth()
                    val zs = L.heightZones(L.Rect(if (l) 0f else W - w, 0f, w, H), underOk)
                    val mids = zs.map { z -> z.rect.y + z.rect.h / 2f }
                    val from = zs.indexOfFirst { z -> z.key == h0 }.coerceAtLeast(0)
                    val on = zs[it.pixelbox.cmwatch.rules.MasterGestures.step(mids, from, y - y0, vy)].key
                    ghost = Ghost(rectFor(l, on, w), namesNow.getValue(on))
                    hints = zs.map { z -> z.key }.filter { k -> k != on }.map { k -> Ghost(rectFor(l, k, w), namesNow.getValue(k)) }
                    val next = p0.copy(left = l).withHeight(on)
                    aim = next to next.open
                }
                var ended = false
                var last = down.position
                var wait: Long? = null
                while (true) {
                    // La sosta: se un bersaglio nuovo aspetta, si riguarda anche col dito fermo.
                    val ev = wait?.let { w -> withTimeoutOrNull(w + 5) { awaitPointerEvent(pass) } } ?: if (wait == null) awaitPointerEvent(pass) else null
                    if (ev != null) {
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        speed.addPosition(c.uptimeMillis, c.position)
                        if (!c.pressed) {
                            ended = true
                            // Al rilascio conta anche il colpo: la velocità decide la direzione e si proietta.
                            if (moved && key != "side") { val v = speed.calculateVelocity(); aimAt(key, last.x / density, last.y / density, v.x / density, v.y / density) }
                            // Un tocco su una maniglia della riga: la master si apre da quel lato, e la riga sotto non lo prende.
                            if (!moved && key.startsWith("bar-")) { c.consume(); if (taps.tap(key, c.uptimeMillis)) change?.invoke(p0.copy(left = key == "bar-left"), true, true) }
                            break
                        }
                        // Oltre 6 dp è un trascinamento, e vince sui tocchi (app-master-gesti-senza-conflitti-04-11).
                        if (!moved && !it.pixelbox.cmwatch.rules.MasterGestures.isDrag((c.position.x - down.position.x) / density, (c.position.y - down.position.y) / density)) continue
                        if (!moved) {
                            taps.dragged()
                            // La presa in alto: di lato cambia il lato, in su o in giù l'altezza.
                            if (key == "top") key = if (kotlin.math.abs(c.position.x - down.position.x) > kotlin.math.abs(c.position.y - down.position.y)) "side" else "height"
                        }
                        moved = true; c.consume(); last = c.position
                    }
                    wait = null
                    val now = android.os.SystemClock.uptimeMillis()
                    val x = last.x / density; val y = last.y / density
                    when (key) {
                        "side" -> {
                            // Il pannello sotto il dito, di quanto si è spostato.
                            if (!motionOff) panelOff = (x - x0) * density
                            // Un margine di 48 intorno al centro e la sosta: il lato non sfarfalla sul confine.
                            val raw = if (kotlin.math.abs(x - W / 2) < L.SIDE_MARGIN) sideHold.current else L.sideAt(W, x)
                            val (on, w) = sideHold.feed(raw, now); wait = w
                            val onLeft = on == L.Side.LEFT
                            val r0 = rectFor(p0.left, h0, p0.panelWidth())
                            ghost = Ghost(if (onLeft == p0.left) r0 else L.mirrorRect(area, r0), namesNow.getValue(on))
                            hints = listOf(Ghost(if (onLeft == p0.left) L.mirrorRect(area, r0) else r0, namesNow.getValue(if (onLeft) L.Side.RIGHT else L.Side.LEFT)))
                            val crossed = L.crossed(W, x, p0.left)
                            val toward = if (p0.left) midRight else midLeft
                            mid = Mid(W / 2, y, crossed, if (crossed) ctx.getString(R.string.master_mid_release, toward) else ctx.getString(R.string.master_mid_cross, toward, if (p0.left) "→" else "←"))
                            aim = p0.copy(left = onLeft) to open0
                        }
                        else -> {
                            if (key != "width" && !motionOff) lift.y = (y - y0) * density
                            aimAt(key, x, y)
                        }
                    }
                }
                val go = aim
                hints = emptyList(); mid = null
                if (key == "side") {
                    // Dal punto e con la velocità del rilascio; annullato o sullo stesso lato, torna indietro allo stesso modo.
                    val v = speed.calculateVelocity().x
                    val panelPx = p0.panelWidth() * density; val gapPx = CORSIA_DP * density; val wPx = size.width.toFloat()
                    val flip = moved && ended && go != null && go.first.left != p0.left
                    if (flip) {
                        val oldLeft = if (p0.left) 0f else wPx - panelPx
                        val newLeft = if (go!!.first.left) 0f else wPx - panelPx
                        val oldContent = if (p0.left) panelPx + gapPx else 0f
                        val newContent = if (go.first.left) panelPx + gapPx else 0f
                        change?.invoke(go.first, go.second, true)
                        settle(oldLeft + panelOff - newLeft, oldContent - newContent, v)
                    } else settle(panelOff, 0f, v)
                    ghost = null
                    return@awaitEachGesture
                }
                if (key != "width") {
                    // Dal punto e con la velocità del rilascio; annullato, la master torna su o giù allo stesso modo.
                    val v = speed.calculateVelocity().y
                    if (moved && ended && go != null) {
                        val hN = go.first.copy(open = go.second).height(underOk)
                        val oldTop = rectFor(p0.left, h0, p0.panelWidth()).y * density + lift.y
                        val newTop = rectFor(go.first.left, hN, go.first.panelWidth()).y * density
                        change?.invoke(go.first, go.second, true)
                        settleLift(oldTop - newTop, v)
                    } else settleLift(lift.y, v)
                    ghost = null
                    return@awaitEachGesture
                }
                if (!moved || !ended || go == null) { ghost = null; return@awaitEachGesture }
                change?.invoke(go.first, go.second, true)
                ghost = null
            }
        }),
    ) {
        val W = maxWidth.value
        // Il pannello resta lo stesso nodo quando cambia lato: la riga si rovescia, l'ordine dei figli no.
        val dir =if (left) androidx.compose.ui.unit.LayoutDirection.Ltr else androidx.compose.ui.unit.LayoutDirection.Rtl
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides dir) {
            Row(Modifier.fillMaxSize()) {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    if (panel != null) {
                        val canGrip = onPlace != null && place != null
                        Box(Modifier.zIndex(if (panelOff != 0f || lift.y != 0f) 3f else 0f).graphicsLayer { translationX = panelOff; alpha = sideFade * (if (stacked) 1f else lift.alpha); if (!stacked) translationY = lift.y }.reflow("r-master").landing().width(panelWidthDp.dp).fillMaxHeight().background(V3.S1).testTag("master-pannello").seen("panel")) {
                            androidx.compose.runtime.CompositionLocalProvider(LocalEdgeSpots provides edge, LocalMasterGrip provides gripOf.takeIf { canGrip }, LocalMasterLift provides lift) { panel() }
                            // La presa in alto: di lato cambia il lato (la sagoma passa alla metà dello schermo sotto il dito), in su o
                            // in giù l'altezza a scatti. Nella pila è la maniglia fra la home e la master.
                            if (canGrip && !stacked) Box(Modifier.align(Alignment.TopCenter).width(72.dp).height(16.dp).testTag("master-maniglia-lato").grip("top"), contentAlignment = Alignment.Center) {
                                TopGripMark(sideways = true)
                            }
                        }
                        // La corsia verso le sessioni, come nella plancia: col + e il filo delle schede della pila; trascinata, la
                        // larghezza fra i posti fissi.
                        // Sopra i vicini: il filo esce dalla corsia fino alla colonna, e lo sfondo della plancia, composta dopo, lo copriva
                        // (prova 165 di Franz, 05/11 21:45: nella pila si fermava 18 px prima della colonna).
                        Corsia(edge, androidx.compose.ui.Modifier.zIndex(if (panelOff != 0f) 3f else 1f).graphicsLayer { translationX = panelOff; alpha = sideFade }.then(if (canGrip) Modifier.grip("width") else Modifier), grip = canGrip)
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().graphicsLayer { translationX = contentOff }) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                        bar?.let { b ->
                            Box(Modifier.zIndex(if (lift.y != 0f) 3f else 0f).graphicsLayer { translationY = lift.y; alpha = lift.alpha }.reflow("r-master").landing().fillMaxWidth().seen("bar").then(if (onPlace != null && place != null) Modifier.grip("bar") else Modifier)) {
                                b()
                                // Due maniglie sull'asse della colonna dove la master atterra, una per lato (app-master-riga-maniglie-04-11,
                                // Franz 04/11 17:40): tirata su apre la master da quel lato, alle altezze del bordo superiore; un tocco la
                                // apre da quel lato all'ultima misura. Il resto della riga tirato su apre sul lato di adesso.
                                if (onPlace != null && place != null) {
                                    val axis = place.panelWidth() / 2
                                    listOf(true to axis, false to W - axis).forEach { (l, cx) ->
                                        Box(
                                            Modifier.offset(x = (cx - 36f).dp).width(72.dp).height(16.dp)
                                                .semantics { contentDescription = if (l) gripLeft else gripRight }
                                                .testTag(if (l) "master-maniglia-riga-sx" else "master-maniglia-riga-dx").grip(if (l) "bar-left" else "bar-right"),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            // ⌃ sopra la barretta: tirata su, la master sale in colonna da quel lato (web-maniglia-riga-colonna-06-11).
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                androidx.compose.foundation.Canvas(Modifier.width(10.dp).height(5.dp)) {
                                                    val st = 1.5.dp.toPx()
                                                    drawLine(V3.Tx3, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width / 2, 0f), st, androidx.compose.ui.graphics.StrokeCap.Round)
                                                    drawLine(V3.Tx3, androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), st, androidx.compose.ui.graphics.StrokeCap.Round)
                                                }
                                                Box(Modifier.width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // La sagoma, le altre destinazioni e la linea di mezzo, sopra tutto e senza prendere i tocchi.
        GhostLayer(ghost, hints, mid)
    }
}

/**
 * Il segno della presa in alto della master, uguale in testa al pannello e fra home e master: la barretta, e con i gesti le due
 * frecce ‹ › che dicono che di lato cambia il lato (Franz 05/11 21:49: «non vedo il drag per il cambio lato»).
 */
@Composable
private fun TopGripMark(sideways: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (sideways) Chevron(left = true)
        Box(Modifier.width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
        if (sideways) Chevron(left = false)
    }
}

@Composable
private fun Chevron(left: Boolean) = androidx.compose.foundation.Canvas(Modifier.width(6.dp).height(10.dp)) {
    val s = 1.5.dp.toPx(); val x0 = if (left) size.width else 0f; val x1 = if (left) 0f else size.width
    drawLine(V3.Tx3, androidx.compose.ui.geometry.Offset(x0, 0f), androidx.compose.ui.geometry.Offset(x1, size.height / 2), s, androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(V3.Tx3, androidx.compose.ui.geometry.Offset(x1, size.height / 2), androidx.compose.ui.geometry.Offset(x0, size.height), s, androidx.compose.ui.graphics.StrokeCap.Round)
}

/**
 * Gli ultimi 32 dp della home sfumano verso il fondo finché sotto c'è altro (app-home-sfuma-sopra-master-04-11, come la web): una
 * maschera sul bordo, mai l'opacità delle righe; in fondo l'ultima riga si legge intera. Uguale nella pila e nella plancia.
 */
internal fun Modifier.homeFadeEnd(): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (HomeScroll.more.value) {
            val h = 32.dp.toPx()
            drawRect(
                androidx.compose.ui.graphics.Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, startY = size.height - h, endY = size.height),
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h), size = androidx.compose.ui.geometry.Size(size.width, h),
                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
            )
        }
    }

/** Lo spostamento verticale della master mentre se ne cambia l'altezza, e la dissolvenza col movimento ridotto (px). */
internal class MasterLift {
    var y by androidx.compose.runtime.mutableFloatStateOf(0f)
    var alpha by androidx.compose.runtime.mutableFloatStateOf(1f)
}

/** Il MasterLift del telaio per la master nella pila (MasterStack). */
internal val LocalMasterLift = androidx.compose.runtime.staticCompositionLocalOf<MasterLift?> { null }

/** Le prese del telaio per chi sta nel pannello (la maniglia della pila); null senza gesti. */
internal val LocalMasterGrip = androidx.compose.runtime.staticCompositionLocalOf<((String) -> Modifier)?> { null }

/** Un posto della sagoma o di una destinazione, in dp nel telaio, con la sua etichetta. */
internal data class Ghost(val rect: L.Rect, val text: String)
/** La linea a metà schermo del gesto del lato: `crossed` quando il dito l'ha passata; la pillola all'altezza del dito. */
internal data class Mid(val x: Float, val y: Float, val crossed: Boolean, val text: String)

/**
 * Lo strato dell'anteprima (landingMorph.ts): lo sfondo scurito al 22 %, la sagoma gialla traslucida col bordo, che scivola in
 * 200 ms da un posto all'altro; i contorni tratteggiati delle altre destinazioni; la linea a metà schermo tratteggiata, piena e
 * accesa quando il dito l'ha passata. Entra e sparisce in 160 ms; col movimento ridotto senza scivolate e senza sfumature.
 */
@Composable
private fun GhostLayer(ghost: Ghost?, hints: List<Ghost>, mid: Mid?) {
    val off = animationsOff()
    val shown by androidx.compose.animation.core.animateFloatAsState(if (ghost != null) 1f else 0f, if (off) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(160), label = "ghost")
    // L'ultima sagoma resta mentre sparisce.
    val lastGhost = androidx.compose.runtime.remember { arrayOfNulls<Ghost>(1) }
    if (ghost != null) lastGhost[0] = ghost
    val g = ghost ?: lastGhost[0]
    if (shown == 0f && ghost == null) return
    val ease = androidx.compose.animation.core.CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    // Elastica: si allunga nel movimento e si assesta con la molla (app-effetti-movimenti-04-11, punto 2).
    // La sagoma scivola con la molla senza rimbalzo dei gesti (app-lato-animazione-06-11, prova 277: «ha ancora il rimbalzo»).
    val slide: androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.unit.Dp> = if (off) androidx.compose.animation.core.snap() else Fx.land()
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = shown }.background(Color.Black.copy(alpha = 0.22f)))
    val shape = RoundedCornerShape(14.dp)
    hints.forEach { h ->
        Box(
            Modifier.offset(h.rect.x.dp, h.rect.y.dp).size(h.rect.w.dp, h.rect.h.dp).graphicsLayer { alpha = shown }
                .drawWithContent {
                    drawContent()
                    val s = 2.dp.toPx()
                    drawRoundRect(
                        V3.Wait.copy(alpha = 0.30f), topLeft = androidx.compose.ui.geometry.Offset(s / 2, s / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - s, size.height - s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(s, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                    )
                }.testTag("master-destinazione"),
        ) { GhostTag(h.text, V3.Tx2) }
    }
    g?.let { gg ->
        val x by androidx.compose.animation.core.animateDpAsState(gg.rect.x.dp, slide, label = "gx")
        val y by androidx.compose.animation.core.animateDpAsState(gg.rect.y.dp, slide, label = "gy")
        val w by androidx.compose.animation.core.animateDpAsState(gg.rect.w.dp, slide, label = "gw")
        val h by androidx.compose.animation.core.animateDpAsState(gg.rect.h.dp, slide, label = "gh")
        // Senza l'arco di prima (app-lato-animazione-06-11): passando dall'altra parte la sagoma scivola, il pannello segue il dito.
        Box(
            Modifier.offset(x, y).size(w, h).graphicsLayer { alpha = shown }.clip(shape).background(V3.Wait.copy(alpha = 0.16f))
                .border(2.dp, V3.Wait.copy(alpha = 0.70f), shape).testTag("master-sagoma"),
        ) { GhostTag(gg.text, V3.Tx) }
    }
    mid?.let { m ->
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val px = m.x.dp.toPx(); val s = 3.dp.toPx()
            drawLine(
                V3.Wait.copy(alpha = if (m.crossed) 1f else 0.55f), androidx.compose.ui.geometry.Offset(px, 0f), androidx.compose.ui.geometry.Offset(px, size.height), s,
                pathEffect = if (m.crossed) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx())),
            )
        }
        // La pillola sulla linea all'altezza del dito: «Oltre questa linea: a destra →», passata «Rilascia: a destra».
        androidx.compose.ui.layout.Layout({
            Text(
                m.text, style = V3.Small.copy(color = if (m.crossed) Color(0xFF1A1300) else V3.Tx, fontWeight = FontWeight.SemiBold), maxLines = 1,
                modifier = Modifier.clip(shape).background(if (m.crossed) V3.Wait else V3.S2)
                    .border(1.dp, V3.Wait.copy(alpha = if (m.crossed) 1f else 0.55f), shape).padding(horizontal = 12.dp, vertical = 6.dp).testTag("master-linea-mezzo"),
            )
        }, Modifier.fillMaxSize()) { ms, c ->
            val p = ms.first().measure(c.copy(minWidth = 0, minHeight = 0))
            layout(c.maxWidth, c.maxHeight) {
                p.place((m.x.dp.roundToPx() - p.width / 2).coerceIn(0, (c.maxWidth - p.width).coerceAtLeast(0)), (m.y.dp.roundToPx() - p.height / 2).coerceIn(0, (c.maxHeight - p.height).coerceAtLeast(0)))
            }
        }
    }
}

/** L'etichetta del posto, in alto al centro della sagoma o del contorno. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.GhostTag(text: String, color: Color) {
    Text(
        text, style = V3.Small.copy(color = color, fontWeight = FontWeight.SemiBold), maxLines = 1, softWrap = false,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 8.dp, end = 8.dp)
            .clip(RoundedCornerShape(10.dp)).background(V3.S2).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/**
 * Home sopra e master sotto nella colonna della master (Franz 03/11 17:30-17:44): uno stacco di 12 dp, la master come scheda
 * con gli angoli tondi in alto; compatta ha la sua misura, a metà prende metà colonna.
 */
@Composable
internal fun MasterStack(half: Boolean, home: @Composable () -> Unit, master: @Composable () -> Unit, onStep: () -> Unit) {
    Column(Modifier.fillMaxSize().background(V3.Bg).testTag("master-sotto-home")) {
        Box(Modifier.weight(1f).fillMaxWidth().homeFadeEnd()) { home() }
        // La maniglia fra le due: un tocco cambia misura; trascinata è la presa in alto della master (lato e altezza), come la
        // testa del pannello (app-tablet-barre-bordi-05-11: prima solo il tocco).
        val grip = LocalMasterGrip.current
        Box(Modifier.fillMaxWidth().height(14.dp).handCursor().clickable(onClickLabel = stringResource(R.string.master_stack_step), onClick = onStep), contentAlignment = Alignment.Center) {
            Box(Modifier.width(72.dp).fillMaxHeight().then(grip?.invoke("top") ?: Modifier).testTag("master-maniglia-pila"), contentAlignment = Alignment.Center) {
                TopGripMark(sideways = grip != null)
            }
        }
        // Compatta ed estesa (app-effetti-movimenti-04-11, punto 8): cambiando misura gli angoli si piegano verso l'interno e tornano
        // tondi con la molla.
        val corner = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(18f) }
        val cornersOff = animationsOff()
        val firstHalf = androidx.compose.runtime.remember { booleanArrayOf(half) }
        androidx.compose.runtime.LaunchedEffect(half) {
            if (half == firstHalf[0] || cornersOff) return@LaunchedEffect
            firstHalf[0] = half
            corner.animateTo(3f, androidx.compose.animation.core.tween(Fx.CORNERS_MS * 2 / 5)); corner.animateTo(18f, Fx.spring())
        }
        val shape = RoundedCornerShape(topStart = corner.value.dp, topEnd = corner.value.dp)
        // La master nella pila segue il dito quando se ne cambia l'altezza (app-altezza-animazione-06-11, MasterLift).
        val lift = LocalMasterLift.current
        Box((if (half) Modifier.weight(1f) else Modifier).fillMaxWidth().graphicsLayer { translationY = lift?.y ?: 0f; alpha = lift?.alpha ?: 1f }.clip(shape).background(V3.S1).border(1.dp, V3.Line, shape)) { master() }
    }
}

