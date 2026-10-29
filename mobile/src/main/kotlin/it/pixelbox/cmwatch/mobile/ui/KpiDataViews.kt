package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.KpiChecksDay
import it.pixelbox.cmwatch.contract.KpiHealth
import it.pixelbox.cmwatch.contract.KpiSuggestions
import it.pixelbox.cmwatch.contract.KpiTask
import it.pixelbox.cmwatch.contract.KpiTasks
import it.pixelbox.cmwatch.contract.KpiTurn
import it.pixelbox.cmwatch.contract.KpiWaits
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// I KPI coi dati nuovi del relay (proposte 8, 9, 10, 12, 13 e 17 del 25/10, contratto 1.96, compito app-kpi-dati-relay-25-10).
// Etichette sul grafico, niente legende (Franz 25/10 21:33); ogni vista porta il suo testTag kpi-N per il controllo.

/** La card di una vista: il titolo in mono e, se c'è posto, la cifra di sintesi a destra; altrimenti va sotto. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KpiCard(tag: String, title: String, right: @Composable (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.testTag(tag).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), style = MonoSmall.copy(fontSize = 11.sp, letterSpacing = 0.7.sp), color = CmColors.text2, modifier = Modifier.padding(end = 12.dp))
            right?.invoke()
        }
        content()
    }
}

@Composable
private fun Right(text: String, color: Color = CmColors.text) = Text(text, style = MonoSmall, color = color)

@Composable
private fun age(s: Long): String = when {
    s < 3600 -> stringResource(R.string.kpi_age_m, maxOf(1L, s / 60))
    s < 86_400 -> stringResource(R.string.kpi_age_h, s / 3600)
    else -> stringResource(R.string.kpi_age_d, s / 86_400)
}

@Composable
private fun weekday(date: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return runCatching { DateTimeFormatter.ofPattern("EEE", locale).format(LocalDate.parse(date)).trimEnd('.') }.getOrDefault(date.takeLast(2))
}

@Composable
private fun compact(n: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return android.icu.text.CompactDecimalFormat.getInstance(locale, android.icu.text.CompactDecimalFormat.CompactStyle.SHORT).apply { maximumFractionDigits = 0 }.format(n)
}

/** Colonne per giorno con l'etichetta sopra e il giorno sotto; `cells` = per colonna le parti dal basso e l'etichetta. */
@Composable
private fun DayColumns(days: List<String>, cells: List<Pair<List<Pair<Float, Color>>, @Composable () -> Unit>>, height: Int = 56) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { i, d ->
            val (parts, label) = cells[i]
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                label()
                Column(Modifier.fillMaxWidth().height(height.dp), verticalArrangement = Arrangement.Bottom) {
                    parts.asReversed().forEach { (f, c) -> if (f > 0f) Box(Modifier.fillMaxWidth().height((f * height).dp.coerceAtLeast(2.dp)).clip(RoundedCornerShape(2.dp)).background(c)) }
                    if (parts.all { it.first <= 0f }) Box(Modifier.fillMaxWidth().height(2.dp).background(CmColors.briefTrack))
                }
                Text(weekday(d), style = MonoSmall.copy(fontSize = 10.sp), color = CmColors.text2, maxLines = 1)
            }
        }
    }
}

// ---- 8 · cifre, età e tentativi (in cima a «In corso» di Lavori) ----

@Composable
private fun stateWord(t: KpiTask): String = when {
    t.stalled -> stringResource(R.string.kpi8_stalled)
    t.state == "running" -> stringResource(R.string.kpi8_running)
    t.state == "paused" -> stringResource(R.string.kpi8_paused)
    t.state == "assigned" -> stringResource(R.string.kpi8_assigned)
    t.state == "approved" -> stringResource(R.string.kpi8_approved)
    else -> t.state
}

private fun stateTone(t: KpiTask): Color = when {
    t.stalled -> CmColors.briefAlertRing
    t.state == "running" -> CmColors.busy
    t.state == "paused" -> CmColors.stale
    else -> CmColors.waiting
}

@Composable
internal fun Kpi8(t: KpiTasks) {
    Column(Modifier.testTag("kpi-8").fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Figure(t.open.toString(), stringResource(R.string.kpi8_open), if (t.open > 0) CmColors.busy else null, Modifier.weight(1f).fillMaxHeight())
            Figure(t.waitingYou.toString(), stringResource(R.string.kpi8_waiting_you), if (t.waitingYou > 0) CmColors.waiting else null, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Figure(t.stalled.toString(), stringResource(R.string.kpi8_stalled_n), if (t.stalled > 0) CmColors.briefAlertRing else null, Modifier.weight(1f).fillMaxHeight())
            Figure(t.closedToday.toString(), stringResource(R.string.kpi8_closed_today), if (t.closedToday > 0) CmColors.briefGood else null, Modifier.weight(1f).fillMaxHeight())
        }
        if (t.tasks.isNotEmpty()) KpiCard("kpi-8-tasks", stringResource(R.string.kpi8_title), { Right(stringResource(R.string.kpi8_axis), CmColors.text2) }) {
            Kpi.tasksOrder(t).forEach { x ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(x.short ?: x.task, style = MaterialTheme.typography.bodyMedium, color = CmColors.text)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(CmColors.briefTrack)) {
                            Box(Modifier.fillMaxWidth(Kpi.ageFraction(x.ageS).coerceAtLeast(0.02f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(stateTone(x)))
                        }
                        Text(age(x.ageS), style = MonoSmall, color = CmColors.text2, maxLines = 1)
                        val tries = pluralStringResource(R.plurals.kpi8_tries, x.attempts, x.attempts, x.attemptsMax)
                        Row(Modifier.semantics { contentDescription = tries }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(x.attemptsMax.coerceIn(0, 6)) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i < x.attempts) CmColors.briefAlertRing else CmColors.briefTrack)) }
                        }
                    }
                    Text(listOfNotNull(stateWord(x), x.who).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = stateTone(x))
                }
            }
        }
    }
}

// ---- 9 · controlli verdi e rossi della settimana (sotto «Fatto» di Lavori) ----

@Composable
internal fun Kpi9(days: List<KpiChecksDay>) {
    val (green, red) = Kpi.checksTotals(days)
    val top = (days.maxOfOrNull { it.green + it.red } ?: 0).coerceAtLeast(1).toFloat()
    KpiCard("kpi-9", stringResource(R.string.kpi9_title), {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = CmColors.briefGood)) { append(pluralStringResource(R.plurals.kpi9_green, green, green)) }
                append(" · ")
                withStyle(SpanStyle(color = if (red > 0) CmColors.briefAlertRing else CmColors.text2)) { append(pluralStringResource(R.plurals.kpi9_red, red, red)) }
            },
            style = MonoSmall,
        )
    }) {
        DayColumns(days.map { it.date }, days.map { d ->
            listOf(d.green / top to CmColors.briefGood, d.red / top to CmColors.briefAlertRing) to @Composable {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = CmColors.text2)) { append(d.green.toString()) }
                        if (d.red > 0) withStyle(SpanStyle(color = CmColors.briefAlertRing)) { append(" ${d.red}") }
                    },
                    style = MonoSmall.copy(fontSize = 10.sp), maxLines = 1,
                )
            }
        })
        Text(stringResource(R.string.kpi9_caption), style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
    }
}

// ---- 10 · la striscia dei turni col costo (nella card aperta della sessione, sotto la giornata a fasce) ----

private fun turnTone(o: String): Color = when (o) {
    "ok" -> CmColors.briefGood
    "fail" -> CmColors.briefAlertRing
    "question" -> CmColors.waiting
    else -> CmColors.busy
}

@Composable
internal fun Kpi10(turns: List<KpiTurn>) {
    val s = Kpi.turnsSummary(turns)
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val label = MonoSmall.copy(fontSize = 10.sp, color = CmColors.briefWeek)
    val first = compact(s.tokensFirst)
    val last = compact(s.tokensLast)
    val aria = stringResource(R.string.kpi10_aria, s.n, s.minutes, first, last)
    Column(Modifier.testTag("kpi-10").fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.kpi10_title).uppercase(), style = MonoSmall.copy(fontSize = 11.sp, letterSpacing = 0.7.sp), color = CmColors.text2, maxLines = 1, modifier = Modifier.weight(1f))
            Text(
                buildAnnotatedString {
                    append(pluralStringResource(R.plurals.kpi10_turns, s.n, s.n, s.minutes))
                    if (s.failed > 0) withStyle(SpanStyle(color = CmColors.briefAlertRing)) { append(" · " + pluralStringResource(R.plurals.kpi10_failed, s.failed, s.failed)) }
                },
                style = MonoSmall.copy(fontSize = 11.sp), color = CmColors.text2, maxLines = 1,
            )
        }
        // La lunghezza di un blocco è la durata del turno, il colore l'esito; sopra, la linea del costo per turno.
        Canvas(Modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = aria }) {
            val gap = 2.dp.toPx()
            val minW = 4.dp.toPx()
            val durs = turns.map { (it.end - it.start).coerceAtLeast(1).toFloat() }
            val avail = size.width - gap * (turns.size - 1).coerceAtLeast(0)
            val k = (avail - minW * turns.size).coerceAtLeast(0f) / durs.sum().coerceAtLeast(1f)
            val barTop = size.height - 14.dp.toPx()
            val lineTop = 12.dp.toPx()
            val lineH = barTop - lineTop - 8.dp.toPx()
            val top = s.tokensMax.coerceAtLeast(1).toFloat()
            var x = 0f
            val pts = turns.mapIndexed { i, t ->
                val w = minW + durs[i] * k
                drawRoundRect(turnTone(t.outcome), Offset(x, barTop), Size(w, 14.dp.toPx()), CornerRadius(3.dp.toPx()))
                val p = Offset(x + w / 2, lineTop + lineH * (1f - t.tokens / top))
                x += w + gap
                p
            }
            for (i in 1 until pts.size) drawLine(CmColors.briefWeek, pts[i - 1], pts[i], strokeWidth = 2.dp.toPx())
            pts.forEach { drawCircle(CmColors.briefWeek, 3.dp.toPx(), it) }
            if (pts.isNotEmpty()) {
                val a = measurer.measure(first, label)
                drawText(a, topLeft = Offset((pts.first().x - a.size.width / 2f).coerceIn(0f, size.width - a.size.width), (pts.first().y - a.size.height - 3.dp.toPx()).coerceAtLeast(0f)))
                if (pts.size > 1) {
                    val b = measurer.measure(last, label)
                    val bx = (pts.last().x - b.size.width / 2f).coerceIn(0f, size.width - b.size.width)
                    if (bx > pts.first().x + a.size.width) drawText(b, topLeft = Offset(bx, (pts.last().y - b.size.height - 3.dp.toPx()).coerceAtLeast(0f)))
                }
            }
        }
        Text(stringResource(R.string.kpi10_caption), style = MonoSmall.copy(fontSize = 10.sp), color = CmColors.briefWeek)
    }
}

// ---- 12 · relay, questo computer e le altre macchine (nella sezione «Sistema») ----

@Composable
internal fun Kpi12(h: KpiHealth) {
    val locale = LocalConfiguration.current.locales[0]
    val lat = h.latency?.takeIf { it.n > 0 && it.p50S != null }
    val figures = buildList<@Composable (Modifier) -> Unit> {
        add { m ->
            if (lat == null) Figure("–", stringResource(R.string.kpi12_latency_none), null, m)
            else Figure(
                stringResource(R.string.kpi12_seconds, String.format(locale, "%.1f", lat.p50S)),
                lat.maxS?.let { stringResource(R.string.kpi12_latency_max, String.format(locale, "%.1f", it)) } ?: stringResource(R.string.kpi12_latency),
                if (lat.p50S!! > 5) CmColors.waiting else null, m,
            )
        }
        h.sessionsHere?.let { n ->
            add { m -> Figure(h.sessionsMax?.let { "$n / $it" } ?: n.toString(), stringResource(R.string.kpi12_sessions_here), if (h.sessionsMax != null && n >= h.sessionsMax!!) CmColors.waiting else null, m) }
        }
        h.hosts.forEach { x ->
            add { m ->
                if (!x.up) Figure(stringResource(R.string.kpi12_down), x.name, CmColors.briefAlertRing, m)
                else Figure(
                    x.ramPct?.let { "$it%" } ?: "–",
                    if (x.ramPct != null) stringResource(R.string.kpi12_ram, x.name) else stringResource(R.string.kpi12_up, x.name),
                    if ((x.ramPct ?: 0) >= 85) CmColors.waiting else null, m,
                )
            }
        }
    }
    Column(Modifier.testTag("kpi-12").fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        figures.chunked(2).forEach { row ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { f -> f(Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

// ---- 13 · il tasso di accettazione dei suggerimenti (sotto la card dei suggerimenti della master) ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Kpi13(s: KpiSuggestions) {
    val total = Kpi.suggestionsTotal(s)
    val rate = Kpi.acceptRate(s)
    val parts = listOf(
        Triple(s.accepted, R.plurals.kpi13_accepted, CmColors.briefGood), Triple(s.discussed, R.plurals.kpi13_discussed, CmColors.busy),
        Triple(s.later, R.plurals.kpi13_later, CmColors.briefWeek), Triple(s.never, R.plurals.kpi13_never, CmColors.stale),
        Triple(s.expired, R.plurals.kpi13_expired, CmColors.briefTrack),
    )
    KpiCard("kpi-13", stringResource(R.string.kpi13_title), rate?.let { { Right(stringResource(R.string.kpi13_rate, it), CmColors.briefGood) } }) {
        if (total == 0) Text(stringResource(R.string.kpi13_none), style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
        else {
            Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                parts.forEach { (n, _, c) -> if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(c)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                parts.forEach { (n, res, c) -> if (n > 0) Text(pluralStringResource(res, n, n), style = MaterialTheme.typography.bodySmall, color = if (c == CmColors.briefTrack) CmColors.text2 else c) }
            }
        }
    }
}

// ---- 17 · quanto aspettano le sessioni (in «Utilizzo», sotto la giornata in cifre) ----

@Composable
private fun waitKind(k: String): String = when (k) {
    "question" -> stringResource(R.string.kpi17_question)
    "permission" -> stringResource(R.string.kpi17_permission)
    "needs_you" -> stringResource(R.string.kpi17_needs_you)
    else -> k
}

@Composable
internal fun Kpi17(w: KpiWaits, now: Long) {
    val closed = Kpi.waitsClosed(w)
    val top = Kpi.waitsTop(w).coerceAtLeast(1.0).toFloat()
    KpiCard("kpi-17", stringResource(R.string.kpi17_title), { Right(pluralStringResource(R.plurals.kpi17_closed, closed, closed), CmColors.text2) }) {
        w.now.sortedBy { it.since }.forEach { x ->
            Text(
                stringResource(R.string.kpi17_now, x.session, waitKind(x.kind), maxOf(1L, (now - x.since + 30) / 60)),
                style = MaterialTheme.typography.bodySmall, color = CmColors.waiting,
            )
        }
        DayColumns(w.week.map { it.date }, w.week.map { d ->
            listOf(((d.medianMin ?: 0.0).toFloat() / top) to CmColors.waiting) to @Composable {
                Text(d.medianMin?.let { Math.round(it).toString() } ?: "–", style = MonoSmall.copy(fontSize = 10.sp), color = CmColors.text2, maxLines = 1)
            }
        })
        Text(stringResource(R.string.kpi17_caption), style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
    }
}
