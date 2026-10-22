package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.NightIndex
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.NightGantt
import it.pixelbox.cmwatch.rules.NightGantt.GoalState
import it.pixelbox.cmwatch.rules.NightGantt.Where
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.ZoneId

/*
 * Il Gantt della pagina «Notte» (bozze A e B approvate da Franz il 21/10 alle 10:29, docs/proposte/2026-10-21-notte-gantt.md):
 * colori e testi delle bozze. Uno stato non si legge solo dal colore: ha la sua icona (StateIcons) e la sua luminosità, e
 * nella vista B anche il tratto (pieno, a righe, tratteggiato).
 */

private val gridLine = Color(0xFF262C34)
private val markLine = Color(0xFF8AB4F8)
private val pubFill = Color(0xFFF1F3F4)
private val ganttCard = Color(0xFF1A1E24)
private val axisText = Color(0xFF8B929C)
private val dimText = Color(0xFFA3AAB4)

/** Barra, fondo e inchiostro della pillola di uno stato del progetto (bozza A). */
private data class Tone(val bar: Color, val chipBg: Color, val ink: Color)

private fun tone(w: Where): Tone = when (w) {
    Where.DONE -> Tone(Color(0xFF5F7D69), Color(0xFF1F3326), Color(0xFFA8DAB5))
    Where.RUNNING -> Tone(Color(0xFF5D7FB8), Color(0xFF1C2A44), Color(0xFFA8C7FA))
    Where.PARTIAL -> Tone(Color(0xFF8D7A46), Color(0xFF3A3220), Color(0xFFFDE293))
    Where.STOPPED -> Tone(Color(0xFF8C5752), Color(0xFF3C2224), Color(0xFFF6AEA9))
    Where.UNKNOWN -> Tone(Color(0xFF5F6670), Color(0xFF262C34), Color(0xFFC4C9D0))
}

/** Vista B: pieno se fatto, a righe se in corso, tratteggiato se fermo o in parte. */
private val goalDone = Color(0xFF6FA57F)
private val goalRun = Color(0xFF5D7FB8)
private val goalRunDark = Color(0xFF33476B)
private val goalPartial = Color(0xFFC9A54A)
private val goalStopped = Color(0xFFC26A63)

@Composable
private fun whereLabel(w: Where): String = stringResource(when (w) {
    Where.DONE -> R.string.night_w_done; Where.RUNNING -> R.string.night_w_running; Where.PARTIAL -> R.string.night_w_partial
    Where.STOPPED -> R.string.night_w_stopped; Where.UNKNOWN -> R.string.night_w_unknown
})

/** Lo switch a due voci, «Progetti» e «Obiettivi»: il segmented button di Material 3, non due bottoni pieni. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GanttSwitch(byGoal: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = CmColors.surfaceHigh, activeContentColor = CmColors.text, activeBorderColor = CmColors.line,
        inactiveContainerColor = CmColors.bg, inactiveContentColor = CmColors.text2, inactiveBorderColor = CmColors.line,
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        listOf(R.string.night_view_projects, R.string.night_view_goals).forEachIndexed { i, label ->
            SegmentedButton(
                selected = byGoal == (i == 1), onClick = { onChange(i == 1) },
                shape = SegmentedButtonDefaults.itemShape(i, 2), colors = colors, modifier = Modifier.handCursor(),
            ) { Text(stringResource(label)) }
        }
    }
}

/** «7 arrivati · 1 in corso · 1 in parte · 1 fermo» con l'icona di stato davanti: dove sono arrivati i progetti, solo gli stati presenti. */
@Composable
fun WhereChips(counts: Map<Where, Int>) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Where.entries.forEach { w ->
            val n = counts[w] ?: return@forEach
            val t = tone(w)
            val text = when (w) {
                Where.DONE -> pluralStringResource(R.plurals.night_wc_done, n, n)
                Where.RUNNING -> pluralStringResource(R.plurals.night_wc_running, n, n)
                Where.PARTIAL -> pluralStringResource(R.plurals.night_wc_partial, n, n)
                Where.STOPPED -> pluralStringResource(R.plurals.night_wc_stopped, n, n)
                Where.UNKNOWN -> pluralStringResource(R.plurals.night_wc_unknown, n, n)
            }
            MarkedText(StateIcons.of(w), text, t.ink, MaterialTheme.typography.labelLarge, modifier = Modifier.clip(CircleShape).background(t.chipBg).padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp))
        }
    }
}

/** Le ore sull'asse, sopra le righe: la colonna dei nomi resta vuota. */
@Composable
private fun TickLabels(ticks: List<NightGantt.Tick>, nameWidth: Dp, endWidth: Dp = 0.dp) {
    Row(Modifier.fillMaxWidth().height(18.dp)) {
        Spacer(Modifier.width(nameWidth))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            ticks.forEach { t ->
                Text(
                    "%02d".format(t.hour), fontSize = 11.sp, color = axisText, textAlign = TextAlign.Center,
                    modifier = Modifier.width(24.dp).offset(x = maxWidth * t.at.toFloat() - 12.dp),
                )
            }
        }
        if (endWidth > 0.dp) Spacer(Modifier.width(endWidth))
    }
}

private fun DrawScope.grid(ticks: List<NightGantt.Tick>, mark: Double) {
    ticks.forEach { t -> val x = size.width * t.at.toFloat(); drawLine(gridLine, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()) }
    val x = size.width * mark.toFloat()
    drawLine(markLine, Offset(x, 0f), Offset(x, size.height), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
}

/** Vista A: una riga per progetto con gli intervalli di lavoro (barre), le pubblicazioni (rombi) e la linea dell'ora. */
@Composable
fun ProjectsGantt(g: NightGantt.Projects, zone: ZoneId) {
    val nameW = 108.dp
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ganttCard).padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)) {
        TickLabels(g.axis.ticks, nameW)
        g.rows.forEach { r ->
            val t = tone(r.where)
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(nameW).padding(end = 6.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(r.name, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
                    Text(
                        if (r.total > 0) stringResource(R.string.night_points, r.done, r.total) else stringResource(R.string.night_no_goal),
                        fontSize = 11.sp, lineHeight = 14.sp, color = t.ink, maxLines = 1,
                    )
                }
                Canvas(Modifier.weight(1f).fillMaxHeight()) {
                    grid(g.axis.ticks, g.axis.mark)
                    val h = 14.dp.toPx(); val y = (size.height - h) / 2
                    r.bars.forEach { b ->
                        drawRoundRect(t.bar, Offset(size.width * b.from.toFloat(), y), Size(size.width * (b.to - b.from).toFloat(), h), CornerRadius(h / 2))
                    }
                    val d = 5.5.dp.toPx()
                    r.pubs.forEach { p ->
                        val cx = size.width * p.toFloat(); val cy = size.height / 2
                        val diamond = Path().apply { moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close() }
                        drawPath(diamond, pubFill)
                        drawPath(diamond, CmColors.bg, style = Stroke(1.dp.toPx()))
                    }
                }
            }
        }
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(Modifier.padding(start = nameW, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Legend({ drawRoundRect(tone(Where.DONE).bar, cornerRadius = CornerRadius(size.height / 2)) }, stringResource(R.string.night_legend_work))
            Legend({
                val c = center; val d = size.height / 2
                drawPath(Path().apply { moveTo(c.x, c.y - d); lineTo(c.x + d, c.y); lineTo(c.x, c.y + d); lineTo(c.x - d, c.y); close() }, pubFill)
            }, stringResource(R.string.night_legend_pub), w = 9.dp, h = 9.dp)
            MarkLegend(g.axis, zone)
        }
    }
}

@Composable
private fun Legend(draw: DrawScope.() -> Unit, label: String, w: Dp = 16.dp, h: Dp = 8.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(w, h), onDraw = draw)
        Text(label, fontSize = 11.sp, color = dimText)
    }
}

@Composable
private fun MarkLegend(a: NightGantt.Axis, zone: ZoneId) = Legend({
    drawLine(markLine, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))
}, stringResource(if (a.markKind == NightGantt.Mark.NOW) R.string.night_legend_now else R.string.night_legend_end, NightGantt.hm(a.markAt, zone)), w = 4.dp, h = 12.dp)

/** Sotto il Gantt A: dove è arrivato ogni progetto, con lo stato a parole e con l'icona. */
@Composable
fun WhereRow(r: NightGantt.ProjectRow) {
    val t = tone(r.where)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ganttCard).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(r.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = CmColors.text)
            r.line?.let { Text(readableSlash(it), fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFFC4C9D0)) }
        }
        StateChip(StateIcons.of(r.where), whereLabel(r.where) + if (r.needsYou && r.where != Where.STOPPED) stringResource(R.string.night_w_needs_you) else "", t)
    }
}

@Composable
private fun StateChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, t: Tone) = MarkedText(
    icon, text, t.ink, androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1, size = 12.dp,
    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(t.chipBg).padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
)

/** In testa alla vista B: i punti fatti sul totale e una barra divisa per stato. */
@Composable
fun GoalTotals(t: NightGantt.Totals) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ganttCard).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.night_goals_reached), fontSize = 15.sp, color = Color(0xFFC4C9D0), modifier = Modifier.weight(1f))
            Text("${t.done}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CmColors.text)
            Text(" " + stringResource(R.string.night_goals_of, t.total), fontSize = 16.sp, color = dimText, modifier = Modifier.padding(bottom = 4.dp))
        }
        val parts = listOf(t.done to goalDone, t.running to goalRun, t.partial to goalPartial, t.todo to goalPartial.copy(alpha = .45f), t.stopped to goalStopped)
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(gridLine)) {
            parts.filter { it.first > 0 }.forEach { (n, c) -> Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(c)) }
        }
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val small = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            if (t.done > 0) MarkedText(StateIcons.done, pluralStringResource(R.plurals.night_gt_done, t.done, t.done), dimText, small, size = 12.dp)
            if (t.running > 0) MarkedText(StateIcons.running, pluralStringResource(R.plurals.night_gt_running, t.running, t.running), dimText, small, size = 12.dp)
            if (t.partial > 0) MarkedText(StateIcons.partial, pluralStringResource(R.plurals.night_gt_partial, t.partial, t.partial), dimText, small, size = 12.dp)
            if (t.todo > 0) MarkedText(StateIcons.todo, pluralStringResource(R.plurals.night_gt_todo, t.todo, t.todo), dimText, small, size = 12.dp)
            if (t.stopped > 0) MarkedText(StateIcons.stopped, pluralStringResource(R.plurals.night_gt_stopped, t.stopped, t.stopped), dimText, small, size = 12.dp)
        }
    }
}

private val goalEndW = 56.dp

/** Le ore sopra i progetti: l'asse è largo quanto la card, il titolo dell'obiettivo sta sulla riga sopra la sua barra. */
@Composable
fun GoalTicks(a: NightGantt.Axis) = Box(Modifier.padding(horizontal = 10.dp)) {
    TickLabels(a.every(if (a.ticks.size > 7) 3 else 1), 0.dp, goalEndW)
}

/**
 * Vista B, un progetto: un segmento per obiettivo, con i suoi punti elencati dentro. L'ora del singolo punto il rapporto
 * non la ha (ogni punto prende quella del suo obiettivo), quindi i punti non hanno barre loro.
 */
@Composable
fun GoalGroupCard(g: NightGantt.Group, a: NightGantt.Axis, zone: ZoneId) {
    val t = tone(g.where)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ganttCard).padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 30.dp).padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(g.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = CmColors.text, modifier = Modifier.weight(1f))
            StateChip(StateIcons.of(g.where), whereLabel(g.where) + if (g.needsYou && g.where != Where.STOPPED) stringResource(R.string.night_w_needs_you) else "", t)
        }
        if (g.goals.isEmpty()) g.line?.let { Text(readableSlash(it), fontSize = 13.sp, color = Color(0xFFC4C9D0), modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)) }
        g.goals.forEach { goal ->
            // Il titolo intero sulla sua riga: tagliato a metà parola non si capirebbe (mai «…» nei testi).
            Text(goal.title, fontSize = 13.sp, lineHeight = 17.sp, color = Color(0xFFC4C9D0), modifier = Modifier.padding(start = 2.dp, top = 4.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.weight(1f).height(28.dp)) {
                    grid(a.every(if (a.ticks.size > 7) 3 else 1), a.mark)
                    goal.bar?.let { b -> goalBar(goal.state, size.width * b.from.toFloat(), size.width * (b.to - b.from).toFloat()) }
                }
                val (end, ink) = when (goal.state) {
                    GoalState.DONE -> (goal.doneAt?.let { NightGantt.hm(it, zone) } ?: stringResource(R.string.night_g_done)) to Color(0xFFA8DAB5)
                    GoalState.RUNNING -> stringResource(R.string.night_g_running) to Color(0xFFA8C7FA)
                    GoalState.PARTIAL -> stringResource(R.string.night_g_partial) to Color(0xFFFDE293)
                    GoalState.STOPPED -> stringResource(R.string.night_g_stopped) to Color(0xFFF6AEA9)
                    GoalState.TODO -> stringResource(R.string.night_g_todo) to Color(0xFFFDE293)
                }
                Text(end, fontSize = 11.sp, color = ink, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(goalEndW))
            }
            // I punti dell'obiettivo, dentro il suo segmento: hanno lo stato dell'obiettivo, non un'ora loro.
            goal.points.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 1.dp, bottom = 1.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StateMark(StateIcons.of(goal.state), dimText, 12.dp)
                    Text(p, fontSize = 12.sp, color = dimText)
                }
            }
        }
    }
}

private fun DrawScope.goalBar(s: GoalState, x: Float, w: Float) {
    val h = 10.dp.toPx(); val y = (size.height - h) / 2; val r = CornerRadius(h / 2)
    when (s) {
        GoalState.DONE -> drawRoundRect(goalDone, Offset(x, y), Size(w, h), r)
        GoalState.RUNNING -> {
            drawRoundRect(goalRunDark, Offset(x, y), Size(w, h), r)
            clipRect(x, y, x + w, y + h) {
                val step = 8.dp.toPx(); var k = x - h
                while (k < x + w) { drawLine(goalRun, Offset(k, y + h), Offset(k + h, y), 4.dp.toPx() * .7f); k += step }
            }
        }
        GoalState.PARTIAL, GoalState.STOPPED -> drawRoundRect(
            if (s == GoalState.STOPPED) goalStopped else goalPartial, Offset(x, y), Size(w, h), r,
            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
        GoalState.TODO -> Unit
    }
}

@Composable
fun GoalLegend(a: NightGantt.Axis, zone: ZoneId) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Legend({ goalBar(GoalState.DONE, 0f, size.width) }, stringResource(R.string.night_g_done), h = 10.dp)
        Legend({ goalBar(GoalState.RUNNING, 0f, size.width) }, stringResource(R.string.night_g_running), h = 10.dp)
        Legend({ goalBar(GoalState.PARTIAL, 0f, size.width) }, stringResource(R.string.night_g_partial), h = 10.dp)
        Legend({ goalBar(GoalState.STOPPED, 0f, size.width) }, stringResource(R.string.night_g_stopped), h = 10.dp)
        MarkLegend(a, zone)
    }
}

/** Le notti prima di quella mostrata (op `nights`, contratto 1.74): al tocco si apre il loro rapporto. */
@Composable
fun NightIndexRow(n: NightIndex, title: String, onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CmColors.surface).handCursor().clickable(onClick = onOpen)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = CmColors.text)
        Text(n.title ?: stringResource(R.string.night_old_report), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
    }
}
