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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
internal fun DeskHead(kind: String, h: Kpi.Head, st: State, now: Long, onOpen: () -> Unit) {
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
                        tick?.let { t -> Box(Modifier.fillMaxWidth((t / 100f).coerceIn(0.01f, 1f)).fillMaxHeight()) { Box(Modifier.align(Alignment.CenterEnd).width(2.dp).fillMaxHeight().clip(RoundedCornerShape(1.dp)).background(V3.Tx)) } }
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

/** Il titolo di un gruppo di righe nella colonna (il `vsec` della PWA). */
@Composable
private fun DeskSec(icon: ImageVector, title: String, count: Int? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = V3.Name.copy(fontSize = 15.sp))
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
                cols.forEach { parts ->
                    val shape = RoundedCornerShape(14.dp)
                    Column(Modifier.weight(1f).fillMaxHeight().clip(shape).background(V3.S1).then(if (V3.day) Modifier.border(1.dp, V3.Line, shape) else Modifier.border(1.dp, LitEdge, shape)).testTag("plancia-col")) {
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
 * lato; il bordo verso le sessioni allarga e stringe con gli scatti a colonna e a metà schermo; la maniglia in alto del pannello,
 * o la barra tirata su, cambia l'altezza a scatti. Mentre si trascina la pagina si riorganizza già e compaiono i segni dove si può
 * lasciare, quello di arrivo grande e con l'etichetta; al rilascio `onPlace(…, true)` ricorda, annullato torna com'era.
 */
@Composable
internal fun MasterFrame(
    left: Boolean, panelWidthDp: Float, panel: (@Composable () -> Unit)?, bar: (@Composable () -> Unit)?,
    place: it.pixelbox.cmwatch.rules.MasterPlace? = null, open: Boolean = panel != null, sessionsOpen: Boolean = false,
    onPlace: ((it.pixelbox.cmwatch.rules.MasterPlace, Boolean, Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var marks by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Pair<L.Mark<*>, String>>>(emptyList()) }
    var ptr by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var live by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Float?>(null) }
    val names = mapOf<Any, String>(
        L.Side.LEFT to stringResource(R.string.master_pos_left), L.Side.RIGHT to stringResource(R.string.master_pos_right),
        L.WidthStop.COLUMN to stringResource(R.string.master_land_column), L.WidthStop.HALF_SCREEN to stringResource(R.string.master_half_screen),
        L.Height.COLUMN to stringResource(R.string.master_land_column), L.Height.HALF to stringResource(R.string.master_land_half),
        L.Height.COMPACT to stringResource(R.string.master_land_compact), L.Height.BAR to stringResource(R.string.master_land_bar),
    )
    // Il gesto lo legge il telaio, che resta fermo mentre la master cambia lato o la barra diventa pannello: le maniglie dicono
    // solo dove stanno (come `follow` della PWA, che continua il gesto sulla finestra).
    val grips = androidx.compose.runtime.remember { mutableMapOf<String, androidx.compose.ui.layout.LayoutCoordinates>() }
    var frame by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val placeNow by androidx.compose.runtime.rememberUpdatedState(place)
    val openNow by androidx.compose.runtime.rememberUpdatedState(open)
    val underOk by androidx.compose.runtime.rememberUpdatedState(sessionsOpen)
    val change by androidx.compose.runtime.rememberUpdatedState(onPlace)
    val namesNow by androidx.compose.runtime.rememberUpdatedState(names)
    fun Modifier.grip(key: String) = this.onGloballyPositioned { c -> grips[key] = c }
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxSize().background(V3.Bg).onGloballyPositioned { frame = it }.then(if (onPlace == null) Modifier else Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial
                val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
                val f = frame?.takeIf { it.isAttached } ?: return@awaitEachGesture
                val key = grips.entries.firstOrNull { (_, c) -> c.isAttached && f.localBoundingBoxOf(c, false).contains(down.position) }?.key ?: return@awaitEachGesture
                val p0 = placeNow ?: return@awaitEachGesture
                val open0 = openNow
                val W = size.width / density; val H = size.height / density
                var moved = false
                var cur = p0; var curOpen = open0
                var ended = false
                while (true) {
                    val c = awaitPointerEvent(pass).changes.firstOrNull { it.id == down.id }
                    if (c == null) break
                    if (!c.pressed) { ended = true; break }
                    if (!moved && (c.position - down.position).getDistance() < viewConfiguration.touchSlop) continue
                    moved = true; c.consume()
                    val x = c.position.x / density; val y = c.position.y / density
                    ptr = androidx.compose.ui.geometry.Offset(x, y)
                    when (key) {
                        "side" -> {
                            val on = L.sideDragged(W, x, if (cur.left) L.Side.LEFT else L.Side.RIGHT)
                            marks = L.sideMarks(W, H, on).map { m -> m to namesNow.getValue(m.key) }
                            if ((on == L.Side.LEFT) != cur.left) { cur = cur.copy(left = on == L.Side.LEFT); change?.invoke(cur, curOpen, false) }
                        }
                        "width" -> {
                            // Posti fissi e calamita (Franz 04/11 10:29-10:30): la master salta subito al posto più vicino, mai nel mezzo.
                            val w = L.widthAt(if (cur.left) x else W - x, W, live ?: cur.panelWidth())
                            live = w
                            marks = L.widthMarks(W, H, cur.left, w).map { m -> m to namesNow.getValue(m.key) }
                        }
                        else -> {
                            // La barra in basso sceglie anche il lato, dalla metà sotto il dito; il bordo superiore solo l'altezza.
                            val l = if (key == "bar") L.sideAt(W, x) == L.Side.LEFT else cur.left
                            val w = cur.panelWidth()
                            val col = L.Rect(if (l) 0f else W - w, 0f, w, H)
                            val zs = L.heightZones(col, underOk)
                            val on = L.heightDragged(zs, y, cur.copy(open = curOpen).height(underOk))
                            marks = L.heightMarks(col, underOk, on).map { m -> m to namesNow.getValue(m.key) }
                            val next = cur.copy(left = l).withHeight(on)
                            if (next != cur || next.open != curOpen) { cur = next; curOpen = next.open; change?.invoke(cur, curOpen, false) }
                        }
                    }
                }
                val w = live
                marks = emptyList(); live = null
                if (!moved) return@awaitEachGesture
                when {
                    !ended -> change?.invoke(p0, open0, false)
                    key == "width" && w != null -> change?.invoke(cur.widthDropped(w, size.width / density), curOpen, true)
                    else -> change?.invoke(cur, curOpen, true)
                }
            }
        }),
    ) {
        val W = maxWidth.value
        // Il pannello resta lo stesso nodo quando cambia lato: la riga si rovescia, l'ordine dei figli no.
        val dir = if (left) androidx.compose.ui.unit.LayoutDirection.Ltr else androidx.compose.ui.unit.LayoutDirection.Rtl
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides dir) {
            Row(Modifier.fillMaxSize()) {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    if (panel != null) {
                        Box(Modifier.width((live ?: panelWidthDp).dp).fillMaxHeight().background(V3.S1).testTag("master-pannello")) {
                            panel()
                            if (onPlace != null && place != null) {
                                // La maniglia del lato, in testa: trascinata, la master passa alla metà dello schermo sotto il dito.
                                Box(Modifier.align(Alignment.TopCenter).width(72.dp).height(16.dp).testTag("master-maniglia-lato").grip("side")) {
                                    Box(Modifier.align(Alignment.Center).width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
                                }
                                // Il bordo verso le sessioni: la larghezza, libera fra 360 e il 60 % dello schermo, scatti al rilascio.
                                Box(Modifier.align(if (left) Alignment.CenterEnd else Alignment.CenterStart).width(12.dp).fillMaxHeight().testTag("master-bordo").grip("width")) {
                                    Box(Modifier.align(Alignment.Center).width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
                                }
                                // Il bordo superiore: l'altezza a scatti (colonna, sotto la home a metà o compatta, riga).
                                Box(Modifier.align(Alignment.TopStart).fillMaxWidth(0.3f).height(10.dp).grip("height"))
                            }
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(V3.Line))
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                        bar?.let { b ->
                            Box(Modifier.fillMaxWidth()) {
                                b()
                                // La barra tirata su si apre, alle altezze del bordo superiore, sul lato sotto il dito.
                                if (onPlace != null && place != null) Box(Modifier.align(Alignment.TopCenter).width(72.dp).height(16.dp).testTag("master-maniglia-altezza").grip("bar")) {
                                    Box(Modifier.align(Alignment.Center).width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
                                }
                            }
                        }
                    }
                }
            }
        }
        // I segni di arrivo, sopra tutto e senza prendere i tocchi.
        marks.forEach { (m, label) -> LandingMark(m, label, ptr, W) }
    }
}

/** Un segno dove si può lasciare la master: grigio, ambra quello di arrivo con l'etichetta; la calamita lo allunga verso il dito. */
@Composable
private fun LandingMark(m: it.pixelbox.cmwatch.rules.MasterLanding.Mark<*>, label: String, ptr: androidx.compose.ui.geometry.Offset, screenW: Float) {
    val len = L.length(m, ptr.x, ptr.y)
    val (dx, dy) = L.shift(m, ptr.x, ptr.y)
    val w = if (m.vertical) 4f else len; val h = if (m.vertical) len else 4f
    Box(
        Modifier.offset((m.x + dx - w / 2).dp, (m.y + dy - h / 2).dp).size(w.dp, h.dp).graphicsLayer { alpha = L.alpha(m, ptr.x, ptr.y) }
            .clip(RoundedCornerShape(2.dp)).background(if (m.on) V3.Wait else V3.Tx3).testTag(if (m.on) "segno-arrivo" else "segno"),
    )
    if (m.on) {
        val right = m.x > screenW / 2
        Row(
            Modifier.offset(
                x = if (m.vertical) (if (right) (m.x - 16 - 180).dp else (m.x + 16).dp) else (m.x - 90).dp,
                y = if (m.vertical) (m.y + dy - 16).dp else (m.y - 46).dp,
            ).clip(RoundedCornerShape(14.dp)).background(V3.S2).border(1.dp, V3.Line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(V3.Wait))
            Spacer(Modifier.width(8.dp))
            Text(label, style = V3.Small.copy(color = V3.Tx), maxLines = 1)
        }
    }
}

/**
 * Home sopra e master sotto nella colonna della master (Franz 03/11 17:30-17:44): uno stacco di 12 dp, la master come scheda
 * con gli angoli tondi in alto; compatta ha la sua misura, a metà prende metà colonna.
 */
@Composable
internal fun MasterStack(half: Boolean, home: @Composable () -> Unit, master: @Composable () -> Unit, onStep: () -> Unit) {
    Column(Modifier.fillMaxSize().background(V3.Bg).testTag("master-sotto-home")) {
        // Gli ultimi 32 dp della home sfumano verso il fondo della colonna finché sotto c'è altro (app-home-sfuma-sopra-master-04-11,
        // come la web): una maschera sul bordo, mai l'opacità delle righe; in fondo l'ultima riga si legge intera.
        Box(Modifier.weight(1f).fillMaxWidth()
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
            }) { home() }
        Box(Modifier.fillMaxWidth().height(14.dp).handCursor().clickable(onClickLabel = stringResource(R.string.master_stack_step), onClick = onStep), contentAlignment = Alignment.Center) {
            Box(Modifier.width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
        }
        val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        Box((if (half) Modifier.weight(1f) else Modifier).fillMaxWidth().clip(shape).background(V3.S1).border(1.dp, V3.Line, shape)) { master() }
    }
}

