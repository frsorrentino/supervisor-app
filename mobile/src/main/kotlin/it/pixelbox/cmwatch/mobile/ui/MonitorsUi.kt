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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Monitors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// I Monitoraggi (app-monitoraggi-09-11, mockup A, B e C approvati da Franz il 09/11): le righe nella home in «Dalla master» e la
// scheda col grafico nella forma del catalogo chiuso (Monitors.Form). Una scala per grafico, righe da 2 dp, griglia leggera, il
// pallino sul minimo e sull'ultimo valore, al tocco il giorno e il valore; le serie in grigio chiaro, l'ambra solo per lo stato.

/** Il pallino: verde se tutto va, ambra se una soglia è scattata. */
private fun dotOf(m: Monitors.Monitor) = if (m.look) V3.Wait else V3.Good

/**
 * La sezione nella home (mockup A): una riga per monitoraggio come le righe delle sessioni, pallino, nome e frase sotto, a destra il
 * valore con la sparkline; in fondo «Nuovo monitoraggio», che apre la chat della master con «monitora » nel campo.
 */
@Composable
fun MonitorsCard(items: List<Monitors.Monitor>, onOpen: (String) -> Unit, onNew: () -> Unit) {
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
                    Text(m.name, style = V3.Name, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                    if (m.text.isNotBlank()) Text(m.text, style = V3.Small.copy(color = if (m.look) V3.Wait else V3.Tx2))
                }
                Column(Modifier.padding(start = 10.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    m.value?.let { v -> Text(v, style = V3.Name.copy(fontWeight = FontWeight.Bold), maxLines = 1, softWrap = false) }
                    if (m.spark.size >= 2) Sparkline(m.spark, Modifier.width(64.dp).height(20.dp))
                }
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
    val line = V3.Tx2
    Canvas(modifier) {
        val s = Monitors.scale(values)
        val pts = values.mapIndexed { i, v -> Offset(i * (size.width - 4.dp.toPx()) / (values.size - 1).coerceAtLeast(1), (1f - s.y(v)) * (size.height - 4.dp.toPx()) + 2.dp.toPx()) }
        drawPath(Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }, line, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        pts.lastOrNull()?.let { p -> drawCircle(V3.Tx, 2.6.dp.toPx(), p) }
    }
}

private val DM = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/**
 * La scheda di un monitoraggio (mockup B e C): il titolo, la fonte e l'ora dell'ultimo valore; il riquadro con lo stato e la frase;
 * i grafici; gli avvisi; la nota «Esempio» quando il monitoraggio lo dice; Ferma e Chiedi alla master (un solo bottone pieno).
 * `wide`: nella colonna del tablet, con la × al posto della freccia.
 */
@Composable
fun MonitorPage(m: Monitors.Monitor, wide: Boolean, onBack: () -> Unit, onStop: () -> Unit, onAsk: () -> Unit, onEditAlerts: () -> Unit) {
    Column(Modifier.fillMaxSize().background(LocalPageBg.current ?: V3.Bg).testTag("monitor-scheda-${m.id}")) {
        Row(Modifier.fillMaxWidth().padding(start = if (wide) 18.dp else 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = V3.Tx) }
            Column(Modifier.weight(1f)) {
                Text(m.name, style = V3.Section.copy(fontSize = 22.sp, lineHeight = 26.sp))
                val at = m.at?.let { t -> stringResource(R.string.monitor_last, DM.format(Instant.ofEpochSecond(t).atZone(ZoneId.systemDefault()))) }
                listOfNotNull(m.source, at).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { Text(it, style = V3.Small) }
            }
            if (wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = V3.Tx2) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StateBox(m)
            m.charts.forEach { c -> ChartOf(c) }
            m.alerts?.let { a ->
                Row(Modifier.fillMaxWidth().clip(V3.Shape).border(1.dp, V3.S3, V3.Shape).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.monitor_alerts) + " · " + a, style = V3.Body.copy(color = V3.Tx2), modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.monitor_edit), style = V3.Name.copy(fontSize = 15.sp),
                        modifier = Modifier.padding(start = 12.dp).handCursor().clickable(onClick = onEditAlerts).testTag("monitor-avvisi"),
                    )
                }
            }
            m.example?.let { e ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        stringResource(R.string.monitor_example), style = V3.Small.copy(color = V3.Wait, fontWeight = FontWeight.Medium),
                        modifier = Modifier.clip(CircleShape).background(V3.Wait.copy(alpha = 0.14f)).border(1.dp, V3.Wait.copy(alpha = 0.5f), CircleShape).padding(horizontal = 10.dp, vertical = 1.dp),
                    )
                    Text(e, style = V3.Small.copy(color = V3.Tx3), modifier = Modifier.padding(start = 8.dp, top = 1.dp))
                }
            }
            m.notes.forEach { n -> Text(n, style = V3.Small.copy(color = V3.Tx3)) }
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

/** Il riquadro dello stato: pallino ed etichetta (ambra se da guardare, verde se stabile) e la frase della master. */
@Composable
private fun StateBox(m: Monitors.Monitor) {
    Column(Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).v3Edge().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dotOf(m)))
            Text(stringResource(if (m.look) R.string.monitor_state_look else R.string.monitor_state_ok).uppercase(), style = V3.Name.copy(fontSize = 15.sp, letterSpacing = 0.6.sp, color = dotOf(m)))
        }
        Text(m.summary ?: m.text, style = V3.Body)
    }
}

/** Un grafico nella sua forma; una forma fuori dal catalogo non si disegna. */
@Composable
internal fun ChartOf(c: Monitors.Chart) {
    val form = Monitors.formOf(c) ?: return
    Column(
        Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).v3Edge().padding(horizontal = 14.dp, vertical = 12.dp).testTag("monitor-grafico-${form.key}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (c.title != null || c.sub != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.title.orEmpty(), style = V3.Name, modifier = Modifier.weight(1f))
            c.sub?.let { Text(it, style = V3.Small, textAlign = TextAlign.End, modifier = Modifier.padding(start = 8.dp)) }
        }
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

/** Al tocco: il giorno e il valore sopra il grafico, la riga verticale sul giorno; senza tocco, una riga vuota della stessa altezza. */
@Composable
private fun Picked(c: Monitors.Chart, sel: Int?, values: List<Double?>) {
    val v = sel?.let { values.getOrNull(it) }
    Text(if (sel == null || v == null) " " else "${c.days.getOrElse(sel) { "" }} · ${num(v, c.unit)}", style = V3.Meter.copy(color = V3.Tx), modifier = Modifier.testTag("monitor-tocco"))
}

private fun Modifier.dayTap(n: Int, onDay: (Int?) -> Unit): Modifier = pointerInput(n) {
    detectTapGestures { o -> onDay(Monitors.dayAt(o.x / size.width, n)) }
}

private fun DrawScope.grid(ticks: List<Float>, color: Color) = ticks.forEach { y -> drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }

/** La serie come linea (o a gradini) nel riquadro; null salta. Le x vanno da 0 a `w` sui giorni, le y dalla scala. */
private fun seriesPath(values: List<Double?>, s: Monitors.Scale, w: Float, h: Float, top: Float, step: Boolean): Path {
    val p = Path()
    var last: Offset? = null
    val n = values.size
    values.forEachIndexed { i, v ->
        v ?: return@forEachIndexed
        val pt = Offset(if (n <= 1) 0f else i * w / (n - 1), top + (1f - s.y(v)) * h)
        when {
            last == null -> p.moveTo(pt.x, pt.y)
            step -> { p.lineTo(pt.x, last!!.y); p.lineTo(pt.x, pt.y) }
            else -> p.lineTo(pt.x, pt.y)
        }
        last = pt
    }
    return p
}

/** Tile: il valore grande con la variazione, la sparkline sotto. */
@Composable
private fun TileForm(c: Monitors.Chart) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(c.value ?: "", style = V3.Section.copy(fontSize = 34.sp, lineHeight = 38.sp))
        c.delta?.let { Text(it, style = V3.Small, modifier = Modifier.padding(bottom = 4.dp)) }
    }
    val vs = c.series.firstOrNull()?.values.orEmpty()
    if (vs.count { it != null } >= 2) LineForm(c, height = 64.dp, axis = false)
}

/** Line: una serie nel tempo, la scala a sinistra, il massimo e l'ultimo valore col pallino, tre giorni sotto. */
@Composable
private fun LineForm(c: Monitors.Chart, height: Dp = 150.dp, axis: Boolean = true) {
    val values = c.series.firstOrNull()?.values.orEmpty()
    if (values.isEmpty()) return
    var sel by remember(c) { mutableStateOf<Int?>(null) }
    Picked(c, sel, values)
    val s = Monitors.scale(values)
    val gridC = V3.Line; val lineC = V3.Tx; val dotC = V3.Tx; val labelC = V3.Tx3
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = V3.Meter.copy(color = labelC, fontSize = 11.5.sp)
    val ticks = s.ticks.map { t -> t to num(t, null) }
    val mx = Monitors.maxIndex(values); val last = Monitors.lastIndex(values)
    val maxLabel = mx?.let { i -> stringResource(R.string.monitor_max, num(values[i]!!, c.unit)) }
    val lastLabel = last?.let { i -> num(values[i]!!, c.unit) }
    Canvas(Modifier.fillMaxWidth().height(height).dayTap(values.size) { sel = if (sel == it) null else it }) {
        val left = if (axis) ticks.maxOf { (_, l) -> measurer.measure(l, labelStyle).size.width } + 8.dp.toPx() else 0f
        val top = if (axis) 14.dp.toPx() else 3.dp.toPx(); val bottom = if (axis) 18.dp.toPx() else 3.dp.toPx()
        val w = size.width - left - 4.dp.toPx(); val h = size.height - top - bottom
        if (axis) ticks.forEach { (t, l) ->
            val y = top + (1f - s.y(t)) * h
            drawLine(gridC, Offset(left, y), Offset(size.width, y), 1.dp.toPx())
            val m = measurer.measure(l, labelStyle); drawText(m, topLeft = Offset(0f, y - m.size.height / 2f))
        }
        translate(left, 0f) {
            sel?.let { i -> val x = if (values.size <= 1) 0f else i * w / (values.size - 1); drawLine(labelC, Offset(x, top), Offset(x, top + h), 1.dp.toPx()) }
            drawPath(seriesPath(values, s, w, h, top, c.step), lineC, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            fun at(i: Int) = Offset(if (values.size <= 1) 0f else i * w / (values.size - 1), top + (1f - s.y(values[i]!!)) * h)
            if (axis) mx?.takeIf { it != last }?.let { i ->
                drawCircle(dotC, 3.5.dp.toPx(), at(i))
                maxLabel?.let { l -> val m = measurer.measure(l, labelStyle); drawText(m, topLeft = Offset((at(i).x - m.size.width / 2f).coerceIn(0f, w - m.size.width), at(i).y - m.size.height - 4.dp.toPx())) }
            }
            last?.let { i ->
                drawCircle(dotC, 3.5.dp.toPx(), at(i))
                if (axis) lastLabel?.let { l -> val m = measurer.measure(l, labelStyle.copy(color = lineC, fontWeight = FontWeight.Medium)); drawText(m, topLeft = Offset((at(i).x - m.size.width - 6.dp.toPx()).coerceAtLeast(0f), at(i).y + 4.dp.toPx())) }
            }
            if (axis && c.days.size >= 2) listOf(0, c.days.size / 2, c.days.size - 1).distinct().forEach { i ->
                val m = measurer.measure(c.days[i], labelStyle)
                val x = i * w / (c.days.size - 1)
                drawText(m, topLeft = Offset((x - m.size.width / 2f).coerceIn(0f, w - m.size.width), size.height - m.size.height))
            }
        }
    }
}

/**
 * Multiples: più serie con gli stessi giorni, un grafico piccolo per serie con la sua scala; a destra il valore, la nota e la
 * variazione. La serie da guardare ha il pallino ambra (lo stato), le altre il cerchio vuoto sul minimo e il pallino sull'ultimo.
 */
@Composable
private fun MultiplesForm(c: Monitors.Chart) {
    var sel by remember(c) { mutableStateOf<Int?>(null) }
    val n = c.days.size.coerceAtLeast(c.series.maxOfOrNull { it.values.size } ?: 0)
    sel?.let { i -> Text("${c.days.getOrElse(i) { "" }} · " + c.series.mapNotNull { s -> s.values.getOrNull(i)?.let { v -> "${s.name.orEmpty()} ${num(v, c.unit)}" } }.joinToString(" · "), style = V3.Meter.copy(color = V3.Tx), modifier = Modifier.testTag("monitor-tocco")) }
    val lineC = V3.Tx; val gridC = V3.Line; val markC = V3.Tx3; val holeC = V3.S1; val lookC = V3.Wait
    c.series.forEachIndexed { k, s ->
        if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.name.orEmpty(), style = V3.Name, maxLines = 1)
                    s.sub?.let { Text(it, style = V3.Small.copy(color = V3.Tx3), maxLines = 1) }
                }
                val sc = Monitors.scale(s.values)
                val mn = Monitors.minIndex(s.values); val last = Monitors.lastIndex(s.values)
                Canvas(Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp).dayTap(n) { sel = if (sel == it) null else it }) {
                    val top = 6.dp.toPx(); val h = size.height - 12.dp.toPx(); val w = size.width - 8.dp.toPx()
                    grid(listOf(size.height - 1.dp.toPx()), gridC)
                    fun x(i: Int) = if (n <= 1) 0f else i * w / (n - 1)
                    c.marks.forEach { mk -> c.days.indexOf(mk.day).takeIf { it >= 0 }?.let { i -> drawLine(markC, Offset(x(i), 0f), Offset(x(i), size.height), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))) } }
                    sel?.let { i -> drawLine(markC, Offset(x(i), 0f), Offset(x(i), size.height), 1.dp.toPx()) }
                    drawPath(seriesPath(s.values.let { v -> v + List((n - v.size).coerceAtLeast(0)) { null } }, sc, w, h, top, c.step), lineC, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
                    fun at(i: Int) = Offset(x(i), top + (1f - sc.y(s.values[i]!!)) * h)
                    if (s.look) last?.let { i -> drawCircle(lookC, 4.dp.toPx(), at(i)) }
                    else {
                        mn?.takeIf { it != last }?.let { i -> drawCircle(holeC, 4.dp.toPx(), at(i)); drawCircle(lineC, 4.dp.toPx(), at(i), style = Stroke(1.5.dp.toPx())) }
                        last?.let { i -> drawCircle(lineC, 3.5.dp.toPx(), at(i)) }
                    }
                }
            }
            Column(Modifier.padding(start = 12.dp).widthIn(min = 96.dp), horizontalAlignment = Alignment.End) {
                s.value?.let { Text(it, style = V3.Name.copy(fontWeight = FontWeight.Bold), maxLines = 1, softWrap = false) }
                s.note?.let { Text(it, style = V3.Small.copy(color = if (s.look) V3.Wait else V3.Tx2), maxLines = 1, softWrap = false) }
                s.delta?.let { Text(it, style = V3.Small, maxLines = 1, softWrap = false) }
            }
        }
    }
    if (c.days.size >= 2) Row(Modifier.fillMaxWidth().padding(end = 108.dp)) {
        val picks = listOf(0, c.days.size / 3, 2 * c.days.size / 3, c.days.size - 1).distinct()
        picks.forEachIndexed { k, i ->
            Text(c.days[i], style = V3.Meter.copy(color = V3.Tx3, fontSize = 11.5.sp), modifier = Modifier.weight(1f), textAlign = if (k == 0) TextAlign.Start else if (k == picks.lastIndex) TextAlign.End else TextAlign.Center, maxLines = 1)
        }
    }
}

/** Bars: i conteggi per giorno da zero, il massimo col suo giorno, tre giorni sotto. */
@Composable
private fun BarsForm(c: Monitors.Chart) {
    val values = c.series.firstOrNull()?.values.orEmpty()
    if (values.isEmpty()) return
    var sel by remember(c) { mutableStateOf<Int?>(null) }
    Picked(c, sel, values)
    val s = Monitors.scale(values, fromZero = true)
    val gridC = V3.Line; val barC = V3.Tx; val labelC = V3.Tx3
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = V3.Meter.copy(color = labelC, fontSize = 11.5.sp)
    val ticks = s.ticks.map { t -> t to num(t, null) }
    val mx = Monitors.maxIndex(values)
    val maxLabel = mx?.let { i -> stringResource(R.string.monitor_max_on, num(values[i]!!, c.unit), c.days.getOrElse(i) { "" }) }
    Canvas(Modifier.fillMaxWidth().height(130.dp).dayTap(values.size) { sel = if (sel == it) null else it }) {
        val left = ticks.maxOf { (_, l) -> measurer.measure(l, labelStyle).size.width } + 8.dp.toPx()
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
                drawRect(if (sel == i) V3.Tx2 else barC, Offset(i * slot + (slot - bw) / 2f, y), Size(bw, top + h - y))
            }
            mx?.let { i -> maxLabel?.let { l -> val m = measurer.measure(l, labelStyle); val y = top + (1f - s.y(values[i]!!)) * h; drawText(m, topLeft = Offset((i * slot + slot / 2f - m.size.width / 2f).coerceIn(0f, w - m.size.width), (y - m.size.height - 2.dp.toPx()).coerceAtLeast(0f))) } }
            if (c.days.size >= 2) listOf(0, c.days.size / 2, c.days.size - 1).distinct().forEach { i ->
                val m = measurer.measure(c.days[i], labelStyle)
                drawText(m, topLeft = Offset((i * slot + slot / 2f - m.size.width / 2f).coerceIn(0f, w - m.size.width), size.height - m.size.height))
            }
        }
    }
}

/** Meter: il valore contro il limite; il limite sta ai quattro quinti della barra, che diventa ambra quando l'ha passato. */
@Composable
private fun MeterForm(c: Monitors.Chart) {
    val v = c.series.firstOrNull()?.values?.lastOrNull { it != null } ?: return
    val limit = c.limit ?: return
    val (fill, over) = Monitors.meter(v, limit)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(c.value ?: num(v, c.unit), style = V3.Section.copy(fontSize = 34.sp, lineHeight = 38.sp, color = if (over) V3.Wait else V3.Tx))
        Text(stringResource(R.string.monitor_limit, num(limit, c.unit)), style = V3.Small, modifier = Modifier.padding(bottom = 4.dp))
    }
    val track = V3.S3; val bar = if (over) V3.Wait else V3.Tx; val mark = V3.Tx2
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = r)
        drawRoundRect(bar, size = Size(size.width * fill, size.height), cornerRadius = r)
        val x = size.width * 0.8f
        drawLine(mark, Offset(x, -3.dp.toPx()), Offset(x, size.height + 3.dp.toPx()), 2.dp.toPx())
    }
}

/** Table: righe col nome, il valore in grassetto e la nota. */
@Composable
private fun TableForm(c: Monitors.Chart) {
    c.rows.forEachIndexed { i, r ->
        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(r.name, style = V3.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(r.value, style = V3.Name.copy(fontWeight = FontWeight.Bold, color = if (r.look) V3.Wait else V3.Tx), modifier = Modifier.padding(start = 12.dp), maxLines = 1, softWrap = false)
            r.note?.let { Text(it, style = V3.Small, modifier = Modifier.padding(start = 14.dp).widthIn(min = 72.dp), textAlign = TextAlign.End, maxLines = 1, softWrap = false) }
        }
    }
}

/** Status: sì o no per ogni giorno, un quadretto per giorno; i giorni andati male in ambra. */
@Composable
private fun StatusForm(c: Monitors.Chart) {
    val values = c.series.firstOrNull()?.values.orEmpty()
    if (values.isEmpty()) return
    var sel by remember(c) { mutableStateOf<Int?>(null) }
    val ok = values.count { it != null && it > 0 }
    Text(sel?.let { i -> "${c.days.getOrElse(i) { "" }} · " + stringResource(if ((values[i] ?: 0.0) > 0) R.string.monitor_yes else R.string.monitor_no) }
        ?: stringResource(R.string.monitor_days_ok, ok, values.count { it != null }), style = V3.Meter.copy(color = V3.Tx), modifier = Modifier.testTag("monitor-tocco"))
    val on = V3.Tx2; val off = V3.Wait; val ring = V3.Tx
    Canvas(Modifier.fillMaxWidth().height(22.dp).dayTap(values.size) { sel = if (sel == it) null else it }) {
        val gap = 3.dp.toPx(); val cell = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val o = Offset(i * (cell + gap), 0f)
            if (v != null) drawRoundRect(if (v > 0) on else off, o, Size(cell, size.height), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            if (sel == i) drawRoundRect(ring, o, Size(cell, size.height), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        }
    }
    if (c.days.size >= 2) Row(Modifier.fillMaxWidth()) {
        Text(c.days.first(), style = V3.Meter.copy(color = V3.Tx3, fontSize = 11.5.sp), modifier = Modifier.weight(1f))
        Text(c.days.last(), style = V3.Meter.copy(color = V3.Tx3, fontSize = 11.5.sp))
    }
}
