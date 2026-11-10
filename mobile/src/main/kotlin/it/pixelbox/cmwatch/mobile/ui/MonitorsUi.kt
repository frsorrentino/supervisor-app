package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.contract.MonitorCard
import it.pixelbox.cmwatch.rules.Monitors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// I Monitoraggi (app-monitoraggi-09-11, mockup A, B e C approvati da Franz il 09/11, contratto 1.132): le righe nella home in «Dalla
// master» e la scheda col grafico nella forma del catalogo chiuso (Monitors.Form). Una scala per grafico, righe da 2 dp, griglia
// leggera, il pallino sul minimo e sull'ultimo valore, al tocco il giorno e il valore; le serie in grigio chiaro, l'ambra solo per
// lo stato.

/** Il pallino: verde se tutto va, ambra se una soglia è scattata. */
private fun dotOf(m: MonitorCard) = if (Monitors.look(m)) V3.Wait else V3.Good

/**
 * La sezione nella home (mockup A): una riga per monitoraggio come le righe delle sessioni, pallino, nome e frase sotto, a destra il
 * valore con la sparkline; in fondo «Nuovo monitoraggio», che apre la chat della master con «monitora » nel campo.
 */
@Composable
fun MonitorsCard(items: List<MonitorCard>, onOpen: (String) -> Unit, onNew: () -> Unit) {
    V3List {
        items.forEachIndexed { i, m ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).handCursor().clickable { onOpen(m.id) }.padding(top = 10.dp, bottom = 10.dp, end = 12.dp).testTag("monitor-${m.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(V3.Rail), contentAlignment = Alignment.Center) { Box(Modifier.size(9.dp).clip(CircleShape).background(dotOf(m))) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(m.title, style = V3.Name, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                    Monitors.line(m).takeIf { it.isNotBlank() }?.let { Text(it, style = V3.Small.copy(color = if (Monitors.look(m)) V3.Wait else V3.Tx2)) }
                    MonitorRefreshOutcome(m)
                }
                Column(Modifier.padding(start = 10.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    m.value?.let { v -> Text(v, style = V3.Name.copy(fontWeight = FontWeight.Bold), maxLines = 1, softWrap = false) }
                    val sp = Monitors.spark(m)
                    if (sp.size >= 2) Sparkline(sp, Modifier.width(64.dp).height(20.dp))
                }
                MonitorRefreshButton(m)
            }
        }
        if (items.isNotEmpty()) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).handCursor().clickable(onClick = onNew).padding(top = 10.dp, bottom = 10.dp, end = 12.dp).testTag("monitor-nuovo"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(V3.Rail), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, tint = V3.Tx2, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.monitors_new), style = V3.Name.copy(fontSize = 15.sp))
            Text(" · " + stringResource(R.string.monitors_new_hint), style = V3.Small, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}

/** La sparkline di una riga: la serie in grigio chiaro, il pallino sull'ultimo valore. */
@Composable
internal fun Sparkline(values: List<Double>, modifier: Modifier) {
    val line = V3.Tx2; val dot = V3.Tx
    Canvas(modifier) {
        val s = Monitors.scale(values)
        val pts = values.mapIndexed { i, v -> Offset(i * (size.width - 4.dp.toPx()) / (values.size - 1).coerceAtLeast(1), (1f - s.y(v)) * (size.height - 4.dp.toPx()) + 2.dp.toPx()) }
        drawPath(Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }, line, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        pts.lastOrNull()?.let { p -> drawCircle(dot, 2.6.dp.toPx(), p) }
    }
}

private val DM = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val D = DateTimeFormatter.ofPattern("dd/MM")
private fun day(t: Long): String = D.format(Instant.ofEpochSecond(t).atZone(ZoneId.systemDefault()))

/**
 * La scheda di un monitoraggio (mockup B e C): il titolo, la fonte, ogni quanto e l'ora dell'ultimo valore; il riquadro con lo stato
 * e la frase; il grafico; gli avvisi, quelli scattati in ambra; la nota «Esempio» quando i dati non sono veri; Ferma e Chiedi alla
 * master (un solo bottone pieno). `wide`: nella colonna del tablet, con la × al posto della freccia.
 */
@Composable
fun MonitorPage(m: MonitorCard, wide: Boolean, onBack: () -> Unit, onStop: () -> Unit, onAsk: () -> Unit, onEditAlerts: () -> Unit) {
    Column(Modifier.fillMaxSize().background(LocalPageBg.current ?: V3.Bg).testTag("monitor-scheda-${m.id}")) {
        Row(Modifier.fillMaxWidth().padding(start = if (wide) 18.dp else 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = V3.Tx) }
            Column(Modifier.weight(1f)) {
                Text(m.title, style = V3.Section.copy(fontSize = 22.sp, lineHeight = 26.sp))
                val at = m.at?.let { t -> stringResource(R.string.monitor_last, DM.format(Instant.ofEpochSecond(t).atZone(ZoneId.systemDefault()))) }
                val every = m.every?.let { e -> stringResource(R.string.monitor_every, e) }
                listOfNotNull(m.source, every, at).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { Text(it, style = V3.Small) }
                MonitorRefreshOutcome(m)
            }
            MonitorRefreshButton(m)
            if (wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = V3.Tx2) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StateBox(m)
            ChartOf(m)
            if (m.alerts.isNotEmpty()) Row(Modifier.fillMaxWidth().clip(V3.Shape).border(1.dp, V3.S3, V3.Shape).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                val hit = V3.Wait
                val text = androidx.compose.ui.text.buildAnnotatedString {
                    append(stringResource(R.string.monitor_alerts) + " · ")
                    m.alerts.forEachIndexed { i, a ->
                        if (i > 0) append(" · ")
                        if (m.hits.any { h -> h.text == a.text }) withStyle(androidx.compose.ui.text.SpanStyle(color = hit)) { append(a.text) } else append(a.text)
                    }
                }
                Text(text, style = V3.Body.copy(color = V3.Tx2), modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.monitor_edit), style = V3.Name.copy(fontSize = 15.sp),
                    modifier = Modifier.padding(start = 12.dp).handCursor().clickable(onClick = onEditAlerts).testTag("monitor-avvisi"),
                )
            }
            if (m.example) Row(verticalAlignment = Alignment.Top) {
                Text(
                    stringResource(R.string.monitor_example), style = V3.Small.copy(color = V3.Wait, fontWeight = FontWeight.Medium),
                    modifier = Modifier.clip(CircleShape).background(V3.Wait.copy(alpha = 0.14f)).border(1.dp, V3.Wait.copy(alpha = 0.5f), CircleShape).padding(horizontal = 10.dp, vertical = 1.dp),
                )
                Text(stringResource(R.string.monitor_example_text), style = V3.Small.copy(color = V3.Tx3), modifier = Modifier.padding(start = 8.dp, top = 1.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WideButton(stringResource(R.string.monitor_stop), filled = false, tag = "monitor-ferma", onClick = onStop)
            WideButton(stringResource(R.string.monitor_ask), filled = true, tag = "monitor-chiedi", onClick = onAsk)
        }
    }
}

/**
 * I due tasti in fondo alla scheda, metà riga ciascuno come nel mockup B: Ferma tonale, Chiedi alla master l'unico pieno (il velo e
 * il bordo ambra delle azioni principali). Col testo grande l'etichetta va a capo invece di uscire dallo schermo.
 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.WideButton(text: String, filled: Boolean, tag: String, onClick: () -> Unit) {
    val shape = CircleShape
    Box(
        Modifier.weight(1f).heightIn(min = 48.dp).clip(shape)
            .background(if (filled) V3.Wait.copy(alpha = 0.14f) else V3.S3)
            .then(if (filled) Modifier.border(1.dp, V3.Wait.copy(alpha = 0.6f), shape) else Modifier)
            .handCursor().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = V3.Name.copy(fontSize = 16.sp, color = if (filled) V3.Wait else V3.Tx), textAlign = TextAlign.Center) }
}

/** Il riquadro dello stato: pallino ed etichetta (ambra se da guardare, verde se stabile) e la frase della master, o valore e variazione. */
@Composable
private fun StateBox(m: MonitorCard) {
    Column(Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).v3Edge().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dotOf(m)))
            Text(stringResource(if (Monitors.look(m)) R.string.monitor_state_look else R.string.monitor_state_ok).uppercase(), style = V3.Name.copy(fontSize = 15.sp, letterSpacing = 0.6.sp, color = dotOf(m)))
        }
        Text(m.verdict ?: listOfNotNull(m.value, m.delta).joinToString(" · "), style = V3.Body)
    }
}

/** Il grafico nella forma della scheda; una forma fuori dal catalogo non si disegna. */
@Composable
internal fun ChartOf(c: MonitorCard) {
    val form = Monitors.formOf(c) ?: return
    Column(
        Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).v3Edge().padding(horizontal = 14.dp, vertical = 12.dp).testTag("monitor-grafico-${form.key}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (form) {
            Monitors.Form.TILE -> TileForm(c)
            Monitors.Form.LINE -> LineForm(c)
            Monitors.Form.MULTIPLES -> MultiplesForm(c)
            Monitors.Form.BARS -> BarsForm(c)
            Monitors.Form.METER -> MeterForm(c)
            Monitors.Form.TABLE -> TableForm(c)
            Monitors.Form.STATUS -> StatusForm(c)
        }
    }
}

/** Un numero dell'asse o del tocco con la sua unità: 1.749 €, 82%, 130. */
@Composable
private fun num(v: Double, unit: String?): String {
    val f = java.text.NumberFormat.getInstance(if (italian()) java.util.Locale.ITALY else java.util.Locale.UK).apply { maximumFractionDigits = if (v % 1.0 == 0.0) 0 else 1 }
    val n = f.format(v)
    return when (unit) { null, "" -> n; "%" -> "$n%"; else -> "$n $unit" }
}

/** Al tocco: il giorno e il valore sopra il grafico; senza tocco, una riga vuota della stessa altezza. */
@Composable
private fun Picked(text: String?) = Text(text ?: " ", style = V3.Meter.copy(color = V3.Tx), modifier = Modifier.testTag("monitor-tocco"))

/** La serie come linea sull'asse del tempo della scheda; null salta. */
private fun seriesPath(values: List<Double?>, ts: List<Long>, span: Pair<Long, Long>, s: Monitors.Scale, w: Float, h: Float, top: Float): Path {
    val p = Path()
    var first = true
    values.forEachIndexed { i, v ->
        v ?: return@forEachIndexed
        val x = Monitors.x(ts[i], span) * w; val y = top + (1f - s.y(v)) * h
        if (first) p.moveTo(x, y) else p.lineTo(x, y)
        first = false
    }
    return p
}

/** Tile: il valore grande con la variazione, la sparkline sotto. */
@Composable
private fun TileForm(c: MonitorCard) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(c.value ?: "", style = V3.Section.copy(fontSize = 34.sp, lineHeight = 38.sp))
        c.delta?.let { Text(it, style = V3.Small, modifier = Modifier.padding(bottom = 4.dp)) }
    }
    val s0 = c.series.firstOrNull() ?: return
    val vs = Monitors.values(s0)
    if (vs.count { it != null } < 2) return
    val ts = Monitors.times(s0); val span = Monitors.span(c)
    val lineC = V3.Tx
    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
        val sc = Monitors.scale(vs); val w = size.width - 8.dp.toPx(); val top = 3.dp.toPx(); val h = size.height - 6.dp.toPx()
        drawPath(seriesPath(vs, ts, span, sc, w, h, top), lineC, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        Monitors.lastIndex(vs)?.let { i -> drawCircle(lineC, 3.5.dp.toPx(), Offset(Monitors.x(ts[i], span) * w, top + (1f - sc.y(vs[i]!!)) * h)) }
    }
}

/** Line: una serie nel tempo, la scala a sinistra, il massimo e l'ultimo valore col pallino, tre giorni sotto. */
@Composable
private fun LineForm(c: MonitorCard) {
    val s0 = c.series.firstOrNull() ?: return
    val values = Monitors.values(s0); val ts = Monitors.times(s0)
    if (values.isEmpty()) return
    val span = Monitors.span(c)
    var sel by remember(c) { mutableStateOf<Int?>(null) }
    Picked(sel?.let { i -> values[i]?.let { v -> "${day(ts[i])} · ${num(v, c.unit)}" } })
    val s = Monitors.scale(values)
    val gridC = V3.Line; val lineC = V3.Tx; val labelC = V3.Tx3
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = V3.Meter.copy(color = labelC, fontSize = 11.5.sp)
    val ticks = s.ticks.map { t -> t to num(t, null) }
    val mx = Monitors.maxIndex(values); val last = Monitors.lastIndex(values)
    val maxLabel = mx?.let { i -> stringResource(R.string.monitor_max, num(values[i]!!, c.unit)) }
    val lastLabel = last?.let { i -> s0.lastText ?: num(values[i]!!, c.unit) }
    val labels = listOf(0, ts.size / 2, ts.size - 1).distinct().filter { it >= 0 }.map { i -> i to day(ts[i]) }
    var left by remember { mutableStateOf(0f) }
    Canvas(Modifier.fillMaxWidth().height(150.dp).pointerInput(c) {
        detectTapGestures { o -> val i = Monitors.nearest(ts, (o.x - left) / (size.width - left - 4.dp.toPx()), span); sel = if (sel == i) null else i }
    }) {
        left = ticks.maxOf { (_, l) -> measurer.measure(l, labelStyle).size.width } + 8.dp.toPx()
        val top = 14.dp.toPx(); val bottom = 18.dp.toPx()
        val w = size.width - left - 4.dp.toPx(); val h = size.height - top - bottom
        ticks.forEach { (t, l) ->
            val y = top + (1f - s.y(t)) * h
            drawLine(gridC, Offset(left, y), Offset(size.width, y), 1.dp.toPx())
            val m = measurer.measure(l, labelStyle); drawText(m, topLeft = Offset(0f, y - m.size.height / 2f))
        }
        translate(left, 0f) {
            fun at(i: Int) = Offset(Monitors.x(ts[i], span) * w, top + (1f - s.y(values[i]!!)) * h)
            sel?.let { i -> val x = Monitors.x(ts[i], span) * w; drawLine(labelC, Offset(x, top), Offset(x, top + h), 1.dp.toPx()) }
            drawPath(seriesPath(values, ts, span, s, w, h, top), lineC, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            mx?.takeIf { it != last }?.let { i ->
                drawCircle(lineC, 3.5.dp.toPx(), at(i))
                maxLabel?.let { l -> val m = measurer.measure(l, labelStyle); drawText(m, topLeft = Offset((at(i).x - m.size.width / 2f).coerceIn(0f, w - m.size.width), at(i).y - m.size.height - 4.dp.toPx())) }
            }
            last?.let { i ->
                drawCircle(lineC, 3.5.dp.toPx(), at(i))
                lastLabel?.let { l -> val m = measurer.measure(l, labelStyle.copy(color = lineC, fontWeight = FontWeight.Medium)); drawText(m, topLeft = Offset((at(i).x - m.size.width - 6.dp.toPx()).coerceAtLeast(0f), at(i).y + 4.dp.toPx())) }
            }
            labels.forEachIndexed { k, (i, l) ->
                val m = measurer.measure(l, labelStyle)
                val x = Monitors.x(ts[i], span) * w
                drawText(m, topLeft = Offset(if (k == 0) 0f else (x - (if (k == labels.lastIndex) m.size.width.toFloat() else m.size.width / 2f)).coerceIn(0f, w - m.size.width), size.height - m.size.height))
            }
        }
    }
}

/**
 * Multiples: più serie con gli stessi giorni, un grafico piccolo per serie con la sua scala; a destra l'ultimo valore e, sotto, la
 * soglia scattata in ambra o il minimo. La serie che ha fatto scattare la soglia ha il pallino ambra (lo stato), le altre il cerchio
 * vuoto sul minimo e il pallino sull'ultimo.
 */
@Composable
private fun MultiplesForm(c: MonitorCard) {
    val span = Monitors.span(c)
    var sel by remember(c) { mutableStateOf<Float?>(null) }
    sel?.let { x ->
        val t = span.first + ((span.second - span.first) * x).toLong()
        Picked("${day(t)} · " + c.series.mapNotNull { s -> Monitors.nearest(Monitors.times(s), x, span)?.let { i -> Monitors.values(s)[i]?.let { v -> "${s.name.orEmpty()} ${num(v, c.unit)}" } } }.joinToString(" · "))
    }
    val lineC = V3.Tx; val gridC = V3.Line; val markC = V3.Tx3; val holeC = V3.S1; val lookC = V3.Wait
    c.series.forEachIndexed { k, s ->
        val vs = Monitors.values(s); val ts = Monitors.times(s)
        val hit = c.hits.firstOrNull { h -> h.series == s.name }
        if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name.orEmpty(), style = V3.Name, maxLines = 1)
                val sc = Monitors.scale(vs)
                val mn = Monitors.minIndex(vs); val last = Monitors.lastIndex(vs)
                Canvas(Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp).pointerInput(c) {
                    detectTapGestures { o -> val x = (o.x / (size.width - 8.dp.toPx())).coerceIn(0f, 1f); sel = if (sel != null && kotlin.math.abs(sel!! - x) < 0.02f) null else x }
                }) {
                    val top = 6.dp.toPx(); val h = size.height - 12.dp.toPx(); val w = size.width - 8.dp.toPx()
                    drawLine(gridC, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1.dp.toPx())
                    sel?.let { x -> drawLine(markC, Offset(x * w, 0f), Offset(x * w, size.height), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))) }
                    drawPath(seriesPath(vs, ts, span, sc, w, h, top), lineC, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
                    fun at(i: Int) = Offset(Monitors.x(ts[i], span) * w, top + (1f - sc.y(vs[i]!!)) * h)
                    if (hit != null) last?.let { i -> drawCircle(lookC, 4.dp.toPx(), at(i)) }
                    else {
                        mn?.takeIf { it != last }?.let { i -> drawCircle(holeC, 4.dp.toPx(), at(i)); drawCircle(lineC, 4.dp.toPx(), at(i), style = Stroke(1.5.dp.toPx())) }
                        last?.let { i -> drawCircle(lineC, 3.5.dp.toPx(), at(i)) }
                    }
                }
            }
            Column(Modifier.padding(start = 12.dp).widthIn(min = 96.dp, max = 132.dp), horizontalAlignment = Alignment.End) {
                Text(s.lastText ?: s.last?.let { num(it, c.unit) }.orEmpty(), style = V3.Name.copy(fontWeight = FontWeight.Bold), maxLines = 1, softWrap = false)
                if (hit != null) Text(hit.text, style = V3.Small.copy(color = V3.Wait), textAlign = TextAlign.End)
                else s.min?.let { Text(stringResource(R.string.monitor_min, num(it, c.unit)), style = V3.Small, maxLines = 1, softWrap = false) }
            }
        }
    }
    if (span.second > span.first) Row(Modifier.fillMaxWidth().padding(end = 108.dp)) {
        listOf(span.first, (span.first + span.second) / 2, span.second).forEachIndexed { k, t ->
            Text(day(t), style = V3.Meter.copy(color = V3.Tx3, fontSize = 11.5.sp), modifier = Modifier.weight(1f), textAlign = if (k == 0) TextAlign.Start else if (k == 2) TextAlign.End else TextAlign.Center, maxLines = 1)
        }
    }
}

/** Bars: i conteggi per giorno da zero, il massimo col suo giorno, tre giorni sotto. */
@Composable
private fun BarsForm(c: MonitorCard) {
    val s0 = c.series.firstOrNull() ?: return
    val values = Monitors.values(s0); val ts = Monitors.times(s0)
    if (values.isEmpty()) return
    var sel by remember(c) { mutableStateOf<Int?>(null) }
    Picked(sel?.let { i -> values[i]?.let { v -> "${day(ts[i])} · ${num(v, c.unit)}" } })
    val s = Monitors.scale(values, fromZero = true)
    val gridC = V3.Line; val barC = V3.Tx; val onC = V3.Tx2; val labelC = V3.Tx3
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = V3.Meter.copy(color = labelC, fontSize = 11.5.sp)
    val ticks = s.ticks.map { t -> t to num(t, null) }
    val mx = Monitors.maxIndex(values)
    val maxLabel = mx?.let { i -> stringResource(R.string.monitor_max_on, num(values[i]!!, null), day(ts[i])) }
    var left by remember { mutableStateOf(0f) }
    Canvas(Modifier.fillMaxWidth().height(130.dp).pointerInput(c) {
        detectTapGestures { o -> val i = ((o.x - left) / ((size.width - left) / values.size)).toInt().coerceIn(0, values.lastIndex); sel = if (sel == i) null else i }
    }) {
        left = ticks.maxOf { (_, l) -> measurer.measure(l, labelStyle).size.width } + 8.dp.toPx()
        val top = 14.dp.toPx(); val bottom = 18.dp.toPx()
        val w = size.width - left; val h = size.height - top - bottom
        ticks.forEach { (t, l) ->
            val y = top + (1f - s.y(t)) * h
            drawLine(gridC, Offset(left, y), Offset(size.width, y), 1.dp.toPx())
            val m = measurer.measure(l, labelStyle); drawText(m, topLeft = Offset(0f, y - m.size.height / 2f))
        }
        translate(left, 0f) {
            val slot = w / values.size; val bw = (slot * 0.7f).coerceAtLeast(1.dp.toPx())
            values.forEachIndexed { i, v ->
                v ?: return@forEachIndexed
                val y = top + (1f - s.y(v)) * h
                drawRect(if (sel == i) onC else barC, Offset(i * slot + (slot - bw) / 2f, y), Size(bw, top + h - y))
            }
            mx?.let { i -> maxLabel?.let { l -> val m = measurer.measure(l, labelStyle); val y = top + (1f - s.y(values[i]!!)) * h; drawText(m, topLeft = Offset((i * slot + slot / 2f - m.size.width / 2f).coerceIn(0f, w - m.size.width), (y - m.size.height - 2.dp.toPx()).coerceAtLeast(0f))) } }
            listOf(0, values.size / 2, values.size - 1).distinct().forEach { i ->
                val m = measurer.measure(day(ts[i]), labelStyle)
                drawText(m, topLeft = Offset((i * slot + slot / 2f - m.size.width / 2f).coerceIn(0f, w - m.size.width), size.height - m.size.height))
            }
        }
    }
}

/** Meter: il valore contro il suo massimo, la barra piena fino a `pct`, la soglia segnata; ambra quando la soglia è scattata. */
@Composable
private fun MeterForm(c: MonitorCard) {
    val (fill, mark) = Monitors.meter(c)
    val over = Monitors.look(c)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(c.value ?: "", style = V3.Section.copy(fontSize = 34.sp, lineHeight = 38.sp, color = if (over) V3.Wait else V3.Tx))
        c.limit?.let { Text(stringResource(R.string.monitor_limit, it), style = V3.Small, modifier = Modifier.padding(bottom = 4.dp)) }
    }
    val track = V3.S3; val bar = if (over) V3.Wait else V3.Tx; val markC = V3.Tx2
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = r)
        drawRoundRect(bar, size = Size(size.width * fill, size.height), cornerRadius = r)
        mark?.let { m -> val x = size.width * m; drawLine(markC, Offset(x, -3.dp.toPx()), Offset(x, size.height + 3.dp.toPx()), 2.dp.toPx()) }
    }
}

/** Table: righe col nome, il valore in grassetto e la nota. */
@Composable
private fun TableForm(c: MonitorCard) {
    c.rows.forEachIndexed { i, r ->
        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(r.name, style = V3.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(r.value, style = V3.Name.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 12.dp), maxLines = 1, softWrap = false)
            r.text?.let { Text(it, style = V3.Small, modifier = Modifier.padding(start = 14.dp).widthIn(min = 72.dp), textAlign = TextAlign.End, maxLines = 1, softWrap = false) }
        }
    }
}

/** Status: sì o no per ogni giorno, un quadretto per giorno; i giorni andati male in ambra. */
@Composable
private fun StatusForm(c: MonitorCard) {
    val s0 = c.series.firstOrNull() ?: return
    val values = Monitors.values(s0); val ts = Monitors.times(s0)
    if (values.isEmpty()) return
    var sel by remember(c) { mutableStateOf<Int?>(null) }
    Picked(sel?.let { i -> "${day(ts[i])} · " + stringResource(if (Monitors.up(values[i])) R.string.monitor_yes else R.string.monitor_no) }
        ?: stringResource(R.string.monitor_days_ok, values.count(Monitors::up), values.size))
    val on = V3.Tx2; val off = V3.Wait; val ring = V3.Tx
    Canvas(Modifier.fillMaxWidth().height(22.dp).pointerInput(c) {
        detectTapGestures { o -> val i = (o.x / (size.width / values.size)).toInt().coerceIn(0, values.lastIndex); sel = if (sel == i) null else i }
    }) {
        val gap = 3.dp.toPx(); val cell = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val o = Offset(i * (cell + gap), 0f)
            drawRoundRect(if (Monitors.up(v)) on else off, o, Size(cell, size.height), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            if (sel == i) drawRoundRect(ring, o, Size(cell, size.height), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        }
    }
    if (ts.size >= 2) Row(Modifier.fillMaxWidth()) {
        Text(day(ts.first()), style = V3.Meter.copy(color = V3.Tx3, fontSize = 11.5.sp), modifier = Modifier.weight(1f))
        Text(day(ts.last()), style = V3.Meter.copy(color = V3.Tx3, fontSize = 11.5.sp))
    }
}

/**
 * «Aggiorna adesso» (app-monitoraggi-aggiorna-10-11, contratto 1.133, Franz 10/11 13:03: «un tasto per ogni monitoraggio che effettui
 * l'aggiornamento immediato»): cosa serve al tasto, dato dalla schermata che ha i comandi. [asked]: i tocchi partiti senza risposta;
 * [failed]: il rifiuto del relay al tocco, per id. Senza (null) il tasto non c'è.
 */
data class MonitorRefreshUi(
    val ops: List<String>?, val now: Long, val asked: Set<String> = emptySet(), val failed: Map<String, String> = emptyMap(),
    val onRefresh: (String) -> Unit = {},
)

internal val LocalMonitorRefresh = androidx.compose.runtime.staticCompositionLocalOf<MonitorRefreshUi?> { null }

private val HM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Il tasto, lo stesso nella riga e nella scheda: fermo; gira mentre si aggiorna e non si ritocca; spento prima di `retry_at`. Il
 * tocco non apre la scheda e non chiude la sezione. Manca se il monitoraggio non si aggiorna o il relay non ha `monitor_refresh`.
 */
@Composable
internal fun MonitorRefreshButton(m: MonitorCard) {
    val ui = LocalMonitorRefresh.current ?: return
    val st = Monitors.refresh(m, ui.ops, ui.now, m.id in ui.asked) ?: return
    val running = st == Monitors.Refresh.Running
    val ready = st == Monitors.Refresh.Ready
    val turn = if (running && !animationsOff()) androidx.compose.animation.core.rememberInfiniteTransition(label = "aggiorna").animateFloat(
        0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing)), label = "giro",
    ) else null
    IconButton(onClick = { ui.onRefresh(m.id) }, enabled = ready, modifier = Modifier.size(40.dp).handCursor().testTag("monitor-aggiorna-${m.id}")) {
        Icon(
            Icons.Rounded.Refresh, stringResource(R.string.monitor_refresh), tint = if (ready) V3.Tx else V3.Tx3,
            modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = turn?.value ?: 0f },
        )
    }
}

/** La riga dell'ultimo aggiornamento sotto il tasto: «aggiornato alle HH:MM», o l'errore in una riga col valore di prima che resta. */
@Composable
internal fun MonitorRefreshOutcome(m: MonitorCard) {
    val ui = LocalMonitorRefresh.current ?: return
    if (Monitors.refresh(m, ui.ops, ui.now) == null) return
    when (val o = Monitors.outcome(m, ui.failed[m.id])) {
        is Monitors.Outcome.Done -> Text(stringResource(R.string.monitor_refreshed, HM.format(Instant.ofEpochSecond(o.at).atZone(ZoneId.systemDefault()))), style = V3.Small, maxLines = 1, modifier = Modifier.testTag("monitor-aggiornato-${m.id}"))
        is Monitors.Outcome.Failed -> Text(
            o.message ?: stringResource(if (o.timeout) R.string.monitor_refresh_timeout else R.string.monitor_refresh_failed),
            style = V3.Small.copy(color = V3.Red), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.testTag("monitor-errore-${m.id}"),
        )
        null -> Unit
    }
}

/**
 * Le righe dei Monitoraggi mentre le schede arrivano (app-monitoraggi-attesa-10-11): la sezione c'è già col numero del riassunto, e qui una
 * riga dice che le schede stanno arrivando dal PC, con la rotella; l'app intanto riprova la lettura.
 */
@Composable
internal fun MonitorsLoading() {
    V3List {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(top = 10.dp, bottom = 10.dp, end = 12.dp).testTag("monitor-in-arrivo"), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(V3.Rail), contentAlignment = Alignment.Center) {
                if (animationsOff()) Box(Modifier.size(9.dp).clip(CircleShape).background(V3.Tx3))
                else androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), color = V3.Tx2, strokeWidth = 2.dp)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.monitors_loading), style = V3.Small, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}
