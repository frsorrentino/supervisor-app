package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.KpiData
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.TimelinePage
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.UtilizzoRules
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LAB = 84.dp
private val ARROW = 24.dp
private val GAP = 10.dp

/**
 * Il blocco Utilizzo aperto (scelta D di Franz, 30/10 20:11): righe compatte, un tocco apre il mini-grafico di quella riga, una
 * aperta per volta. Tutte le frecce su una verticale. La barra delle 5 ore e il grafico hanno la stessa colonna, così la tacca e
 * «adesso» cadono nello stesso punto; il resto sta nella pagina «Utilizzo e limiti», da «Tutti i dettagli».
 */
@Composable
internal fun UtilizzoV3(rings: List<PhoneOverview.Ring>, st: State, page: TimelinePage?, data: KpiData?, now: Long, onDetails: () -> Unit, startOpen: String? = null, zone: ZoneId = ZoneId.systemDefault()) {
    var open by rememberSaveable { mutableStateOf(startOpen) }
    val shown = PhoneOverview.lineRings(rings)
    // La colonna dei valori si allarga coi caratteri di sistema; barra e grafico restano allineati perché la dividono.
    val valW = 80.dp * LocalDensity.current.fontScale
    val toggle = { k: String -> open = if (open == k) null else k }
    val hm = DateTimeFormatter.ofPattern("HH:mm")
    Column(Modifier.fillMaxWidth().testTag("utilizzo-v3")) {
        shown.forEach { r ->
            val many = shown.size > 1
            val pct = (r.h5 ?: 0).coerceIn(0, 100)
            val proj = r.pace?.projected
            val tick = UtilizzoRules.elapsedPct(r.resetAt, UtilizzoRules.H5_S, now)
            val k5 = "h5:${r.account}"
            UseRow(open == k5, { toggle(k5) }) {
                Label(stringResource(R.string.kpi7_quota), r, many)
                Spacer(Modifier.width(GAP))
                Meter(pct, proj, tick, Modifier.weight(1f))
                Spacer(Modifier.width(GAP))
                Text(if (proj != null && !r.stale) "$pct% → $proj%" else if (r.stale) stringResource(R.string.quota_line_stale) else "$pct%", style = V3.Meter.copy(color = V3.Tx), textAlign = TextAlign.End, modifier = Modifier.width(valW))
            }
            if (open == k5) Column(Modifier.padding(bottom = 10.dp)) {
                Aligned(valW) { Curve(r, now, Modifier.fillMaxWidth().height(58.dp)) }
                Aligned(valW) {
                    BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
                        val w = maxWidth
                        r.resetAt?.let { reset ->
                            Text(hm.format(Instant.ofEpochSecond(reset - UtilizzoRules.H5_S).atZone(zone)), style = V3.Meter.copy(fontSize = 12.sp), modifier = Modifier.align(Alignment.CenterStart))
                            Text(hm.format(Instant.ofEpochSecond(reset).atZone(zone)), style = V3.Meter.copy(fontSize = 12.sp), modifier = Modifier.align(Alignment.CenterEnd))
                            // «adesso» esattamente sotto la tacca, centrato; se non c'è posto fra le due ore resta solo la tacca.
                            val x = w * ((tick ?: 0f) / 100f)
                            if (tick != null && x > 70.dp && x < w - 70.dp) Text(stringResource(R.string.usage_now), style = V3.Meter.copy(fontSize = 12.sp, color = V3.Tx), textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.offset(x = x - 30.dp).width(60.dp).align(Alignment.CenterStart))
                        }
                    }
                }
                Note(when {
                    proj != null && r.resetAt != null -> stringResource(R.string.usage_projection, proj, hm.format(Instant.ofEpochSecond(r.resetAt!!).atZone(zone)))
                    r.resetAt != null -> stringResource(R.string.quota_resets_at, hm.format(Instant.ofEpochSecond(r.resetAt!!).atZone(zone)))
                    else -> stringResource(R.string.quota_old)
                })
            }
        }
        shown.forEach { r ->
            val w7 = (r.w7 ?: 0).coerceIn(0, 100)
            val kw = "w7:${r.account}"
            val delta = UtilizzoRules.weekDelta(r.w7, r.weekResetAt, now)
            UseRow(open == kw, { toggle(kw) }) {
                Label(stringResource(R.string.usage_week), r, shown.size > 1)
                Spacer(Modifier.width(GAP))
                Meter(w7, null, UtilizzoRules.elapsedPct(r.weekResetAt, UtilizzoRules.W7_S, now), Modifier.weight(1f))
                Spacer(Modifier.width(GAP))
                Text("$w7%", style = V3.Meter.copy(color = V3.Tx), textAlign = TextAlign.End, modifier = Modifier.width(valW))
            }
            if (open == kw) Column(Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                delta?.let { d -> Note(when { d < 0 -> stringResource(R.string.usage_below_pace, -d); d > 0 -> stringResource(R.string.usage_above_pace, d); else -> stringResource(R.string.week_even) }) }
                r.weekResetAt?.let { Note(stringResource(R.string.week_renewal, DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault()).format(Instant.ofEpochSecond(it).atZone(zone)))) }
                r.weekOrigins?.let { o -> Note(listOf(stringResource(R.string.week_origin_sessions, Math.round(o.sessions).toInt()), stringResource(R.string.week_origin_automations, Math.round(o.automations).toInt()), stringResource(R.string.week_origin_requests, Math.round(o.requests).toInt())).joinToString(" · ")) }
            }
        }
        // Le sessioni dell'account più carico: accanto al numero il nome intero, che va a capo se è lungo.
        val top = shown.filter { !it.stale }.maxByOrNull { it.h5 ?: 0 }
        val split = top?.let { Kpi.split(st, it.account, 99) }
        split?.slices?.firstOrNull()?.let { first ->
            UseRow(open == "sessions", { toggle("sessions") }) {
                Text(stringResource(R.string.home_sec_sessions), style = V3.Name.copy(fontSize = 15.sp), modifier = Modifier.width(LAB))
                Spacer(Modifier.width(GAP))
                Text(first.session, style = V3.Small.copy(color = V3.Tx), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${first.pct}%", style = V3.Meter.copy(color = V3.Tx), maxLines = 1)
            }
            if (open == "sessions") Column(Modifier.padding(start = 14.dp, end = 12.dp + ARROW, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                split.slices.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(s.session, style = V3.Small.copy(color = V3.Tx), modifier = Modifier.weight(1f))
                        Text("${s.pct}%", style = V3.Meter.copy(color = V3.Tx), maxLines = 1)
                    }
                }
                if (split.others > 0) Row { Text(stringResource(R.string.kpi4_others), style = V3.Small, modifier = Modifier.weight(1f)); Text("${split.others}%", style = V3.Meter) }
            }
        }
        page?.let { p ->
            val f = Kpi.dayFigures(p, state = st, now = now, zone = zone)
            val tok = if (f.tokens >= 1_000_000) "${f.tokens / 1_000_000} M" else "${f.tokens / 1_000} k"
            UseRow(open == "today", { toggle("today") }) {
                Text(stringResource(R.string.usage_today), style = V3.Name.copy(fontSize = 15.sp), modifier = Modifier.width(LAB))
                Spacer(Modifier.width(GAP))
                Text("${f.prompts} ${stringResource(R.string.kpi18_prompt)} · ${f.commits} ${stringResource(R.string.kpi18_commit)} · $tok", style = V3.Meter.copy(color = V3.Tx), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            if (open == "today") Column(Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Aligned(0.dp) { Hours(f.perHour, Modifier.fillMaxWidth().height(28.dp)) }
                UtilizzoRules.peakHour(f.perHour, f.firstHour)?.let { Note(stringResource(R.string.usage_peak, it)) }
                data?.waits?.let { UtilizzoRules.waits(it.week) }?.let { w -> Note(stringResource(R.string.usage_waits, w.medianMin, w.closed)) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).handCursor().clickable(onClick = onDetails).padding(start = 14.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.usage_details), style = V3.Name.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3, modifier = Modifier.size(ARROW))
        }
    }
}

/** Una riga col filo sopra e la freccia sulla verticale comune. */
@Composable
private fun UseRow(open: Boolean, onToggle: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).handCursor().clickable(onClick = onToggle).padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        content()
        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.padding(start = 4.dp).size(ARROW))
    }
}

/** La colonna della barra, sotto la barra: stesso inizio, stessa fine. */
@Composable
private fun Aligned(valW: Dp, content: @Composable () -> Unit) {
    val end = 12.dp + ARROW + 4.dp + (if (valW > 0.dp) valW + GAP else 0.dp)
    Box(Modifier.fillMaxWidth().padding(start = 14.dp + LAB + GAP, end = end)) { content() }
}

@Composable
private fun Note(text: String) = Text(text, style = V3.Small.copy(fontSize = 13.5.sp), modifier = Modifier.padding(start = 14.dp, end = 12.dp + ARROW))

@Composable
private fun Label(text: String, r: PhoneOverview.Ring, many: Boolean) {
    Row(Modifier.width(LAB), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        // Con due account la forma dice quale: tondo personale, quadrato lavoro.
        if (many) Box(Modifier.size(9.dp).border(2.dp, V3.Tx2, if (r.personal) CircleShape else RoundedCornerShape(3.dp)))
        Text(text, style = V3.Name.copy(fontSize = 15.sp))
    }
}

/** La barra: il pieno, la proiezione tratteggiata, la tacca di quanto è passato della finestra. */
@Composable
private fun Meter(pct: Int, projected: Int?, tick: Float?, modifier: Modifier) {
    val tone = if (pct >= 90) V3.Red else if (pct >= 75) V3.Wait else V3.Tx2
    Canvas(modifier.height(12.dp)) {
        val h = 6.dp.toPx(); val y = (size.height - h) / 2; val r = androidx.compose.ui.geometry.CornerRadius(h / 2)
        drawRoundRect(V3.S3, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, h), r)
        if (projected != null && projected > pct) drawLine(V3.Tx3, Offset(size.width * pct / 100f, size.height / 2), Offset(size.width * projected / 100f, size.height / 2), h, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
        if (pct > 0) drawRoundRect(tone, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width * pct / 100f, h), r)
        tick?.let { drawRect(V3.Tx, Offset(size.width * it / 100f - 1.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height)) }
    }
}

/** La curva delle 5 ore sulla stessa scala della barra: la linea verticale è la tacca. */
@Composable
private fun Curve(r: PhoneOverview.Ring, now: Long, modifier: Modifier) {
    val reset = r.resetAt ?: return
    val start = reset - UtilizzoRules.H5_S
    val pts = r.pace?.points.orEmpty()
    val tick = UtilizzoRules.elapsedPct(reset, UtilizzoRules.H5_S, now)
    Canvas(modifier) {
        val x = { t: Long -> size.width * ((t - start).toFloat() / UtilizzoRules.H5_S).coerceIn(0f, 1f) }
        val y = { p: Int -> size.height * (1f - p.coerceIn(0, 100) / 100f) }
        if (pts.size >= 2) {
            val line = Path().apply { pts.forEachIndexed { i, s -> if (i == 0) moveTo(x(s.ts), y(s.pct)) else lineTo(x(s.ts), y(s.pct)) } }
            val area = Path().apply { addPath(line); lineTo(x(pts.last().ts), size.height); lineTo(x(pts.first().ts), size.height); close() }
            drawPath(area, V3.S2)
            drawPath(line, V3.Tx2, style = Stroke(2.dp.toPx()))
            r.pace?.projected?.let { p -> drawLine(V3.Tx3, Offset(x(pts.last().ts), y(pts.last().pct)), Offset(size.width, y(p)), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))) }
        }
        tick?.let { val tx = size.width * it / 100f; drawLine(V3.Tx, Offset(tx, 0f), Offset(tx, size.height), 2.dp.toPx()) }
    }
}

/** Le colonnine per ora, neutre: il colore spetta agli stati. */
@Composable
private fun Hours(perHour: List<Pair<Int, Int>>, modifier: Modifier) {
    val top = (perHour.maxOfOrNull { it.first + it.second } ?: 0).coerceAtLeast(1)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        perHour.forEach { (p, c) ->
            val n = p + c
            Box(Modifier.weight(1f).fillMaxHeight(if (n == 0) 0.08f else (n.toFloat() / top).coerceAtLeast(0.15f)).clip(RoundedCornerShape(1.5.dp)).background(if (n == 0) V3.S3 else V3.Tx2))
        }
    }
}

/** La pagina «Utilizzo e limiti», aperta da «Tutti i dettagli», dal menu e dalla riga della quota: quello che il blocco non mostra più. */
@Composable
internal fun UsagePage(st: State, ov: PhoneOverview.Model, page: TimelinePage?, data: KpiData?, now: Long, dataStale: Boolean, onBack: () -> Unit, zone: ZoneId = ZoneId.systemDefault()) = V3Theme {
    Column(Modifier.fillMaxSize().background(V3.Bg).systemBarsPadding().testTag("usage-page")) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = V3.Tx) }
            Text(stringResource(R.string.menu_quadro), style = V3.Section.copy(fontSize = 22.sp, lineHeight = 26.sp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PhoneOverview.lineRings(ov.rings).forEach { r -> TabletQuotaPanel(r, now, dataStale = dataStale) }
            TodayPanel(ov.today, it.pixelbox.cmwatch.rules.UsageTodayRules.rows(st.usageToday))
            st.quota.keys.forEach { a -> Kpi.split(st, a)?.let { sp -> Kpi4(sp) } }
            page?.let { p -> Kpi18(Kpi.dayFigures(p, state = st, now = now, zone = zone)) }
            data?.waits?.let { w -> Kpi17(w, now) }
        }
    }
}
