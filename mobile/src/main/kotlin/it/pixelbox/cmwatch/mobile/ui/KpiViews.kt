package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.ui.tokens.CmColors

// I KPI coi dati già nel relay (proposte 6, 11 e 18 del 25/10, compito app-kpi-medie-25-10). Etichette sul grafico, niente
// legende (Franz 25/10 21:33); ogni vista porta il suo testTag kpi-N per il controllo.

private fun bandColor(b: Kpi.Band) = when (b) {
    Kpi.Band.WORK -> CmColors.busy
    Kpi.Band.WAIT -> CmColors.waiting
    Kpi.Band.STILL -> CmColors.briefTrack
}

/** 6 · sei colonnine, una per ora, l'ultima a destra; l'altezza è il numero di eventi dell'ora. */
@Composable
internal fun Kpi6(counts: List<Int>) {
    val top = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val aria = stringResource(R.string.kpi6_aria, counts.sum())
    Row(
        Modifier.testTag("kpi-6").height(16.dp).semantics { contentDescription = aria },
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom,
    ) {
        counts.forEach { c ->
            val h = if (c == 0) 2f else maxOf(3f, c * 16f / top)
            Box(Modifier.width(4.dp).height(h.dp).clip(RoundedCornerShape(1.dp)).background(if (c == 0) CmColors.briefTrack else CmColors.busy))
        }
    }
}

/** 11 · la giornata a fasce dalla mezzanotte a ora; il totale al lavoro in alto, le ore sotto. */
@Composable
internal fun Kpi11(spans: List<Kpi.Span>, from: Long, now: Long) {
    val len = (now - from).coerceAtLeast(1).toFloat()
    val work = Kpi.workSeconds(spans)
    val h = (work / 3600).toInt(); val m = ((work % 3600) / 60).toInt()
    Column(Modifier.testTag("kpi-11").fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.kpi11_title).uppercase(), style = MonoSmall.copy(fontSize = 11.sp, letterSpacing = 0.7.sp), color = CmColors.text2, maxLines = 1, modifier = Modifier.weight(1f))
            Text(if (h > 0) stringResource(R.string.kpi11_work_h, h, m) else stringResource(R.string.kpi11_work_m, m), style = MonoSmall.copy(fontSize = 11.sp), color = CmColors.text2, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
            spans.forEach { s -> Box(Modifier.weight(((s.to - s.from) / len).coerceAtLeast(0.001f)).fillMaxHeight().background(bandColor(s.band))) }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(12.dp)) {
            val w = maxWidth
            var hour = 0
            while (from + hour * 3600L <= now) {
                val x = hour * 3600f / len
                if (x <= 0.92f) Text("%02d".format(hour), style = MonoSmall.copy(fontSize = 10.sp), color = CmColors.text2, maxLines = 1, modifier = Modifier.offset(x = w * x))
                hour += 4
            }
        }
    }
}

/** 18 · quattro cifre della giornata e i prompt e i commit per ora; la quarta dice chi ti aspetta adesso (master, 29/10). */
@Composable
internal fun Kpi18(f: Kpi.DayFigures) {
    val plus = if (f.atLeast) "+" else ""
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val tokens = android.icu.text.CompactDecimalFormat.getInstance(locale, android.icu.text.CompactDecimalFormat.CompactStyle.SHORT).apply { maximumFractionDigits = 1 }.format(f.tokens)
    Column(Modifier.testTag("kpi-18").fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Figure("${f.prompts}$plus", stringResource(R.string.kpi18_prompts), null, Modifier.weight(1f))
            Figure("${f.commits}$plus", stringResource(R.string.kpi18_commits), null, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Figure(
                f.waitingNow.toString(),
                if (f.waitingNow > 0) stringResource(R.string.kpi18_waiting_since, maxOf(1L, ((f.oldestWaitS ?: 0L) + 30) / 60)) else stringResource(R.string.kpi18_none_waiting),
                if (f.waitingNow > 0) CmColors.waiting else null, Modifier.weight(1f),
            )
            Figure(tokens, f.topSession?.let { stringResource(R.string.kpi18_tokens, it, Math.round(f.topShare * 100).toInt()) } ?: stringResource(R.string.kpi18_tokens_none), null, Modifier.weight(1f))
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = CmColors.busy)) { append(stringResource(R.string.kpi18_prompt)) }
                append(" ${stringResource(R.string.kpi18_and)} ")
                withStyle(SpanStyle(color = CmColors.idle)) { append(stringResource(R.string.kpi18_commit)) }
                append(" ${stringResource(R.string.kpi18_per_hour)}")
            },
            style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
        )
        val top = (f.perHour.maxOfOrNull { it.first + it.second } ?: 0).coerceAtLeast(1)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom,
        ) {
            f.perHour.forEachIndexed { i, (p, c) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Column(Modifier.fillMaxWidth().height(58.dp), verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)) {
                        if (c > 0) Box(Modifier.fillMaxWidth().height((c * 56f / top).dp).clip(RoundedCornerShape(2.dp)).background(CmColors.idle))
                        if (p > 0) Box(Modifier.fillMaxWidth().height((p * 56f / top).dp).clip(RoundedCornerShape(2.dp)).background(CmColors.busy))
                    }
                    val hour = f.firstHour + i
                    Text(if (hour % 4 == 0) hour.toString() else "", style = MonoSmall.copy(fontSize = 10.sp), color = CmColors.text2, maxLines = 1, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun Figure(value: String, label: String, tone: Color?, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = tone ?: CmColors.text, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
    }
}

// ---- Proposte piccole (4, 5, 7; compito app-kpi-piccole-25-10) ----

/** 7 · tre cifre in testa alla home; un tocco su «ti aspettano» porta alla prima che aspetta. */
@Composable
internal fun Kpi7(h: Kpi.Head, onWaiting: () -> Unit) {
    Row(Modifier.testTag("kpi-7").fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val m = maxOf(1L, ((h.oldestWaitS ?: 0L) + 30) / 60)
        Figure(
            h.waiting.toString(), if (h.waiting > 0) stringResource(R.string.kpi7_waiting, m) else stringResource(R.string.kpi7_none_waiting),
            if (h.waiting > 0) CmColors.waiting else null,
            Modifier.weight(1f).fillMaxHeight().then(if (h.waiting > 0) Modifier.handCursor().clickable(onClick = onWaiting) else Modifier),
        )
        Figure(h.busy.toString(), stringResource(R.string.kpi7_busy), if (h.busy > 0) CmColors.busy else null, Modifier.weight(1f).fillMaxHeight())
        Figure(
            h.outcomes?.toString() ?: "–",
            when { h.outcomes == null -> stringResource(R.string.kpi7_outcomes_unknown); (h.red ?: 0) > 0 -> pluralStringResource(R.plurals.kpi7_outcomes_red, h.red!!, h.red!!); else -> stringResource(R.string.kpi7_outcomes) },
            null, Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

/** 4 · la quota 5h di un account divisa per sessione; i nomi sono le etichette della barra, nel colore del loro tratto. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun Kpi4(s: Kpi.Split) {
    val tones = listOf(CmColors.briefRing, CmColors.briefWeek, CmColors.briefGood, CmColors.text2)
    Column(
        Modifier.testTag("kpi-4").fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.kpi4_title, s.account).uppercase(), style = MonoSmall.copy(fontSize = 11.sp, letterSpacing = 0.7.sp), color = CmColors.text2, modifier = Modifier.weight(1f))
            Text("${s.h5}%", style = MonoSmall, color = CmColors.text)
        }
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(CmColors.briefTrack)) {
            val parts = s.slices.map { it.pct } + listOfNotNull(s.others.takeIf { it > 0 })
            parts.forEachIndexed { i, p -> if (p > 0) Box(Modifier.fillMaxHeight().weight(p / 100f).background(tones[minOf(i, 3)])) }
            val free = 100 - parts.sum()
            if (free > 0) Box(Modifier.fillMaxHeight().weight(free / 100f))
        }
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            s.slices.forEachIndexed { i, x -> Text("${x.session} ${x.pct}%", style = MaterialTheme.typography.bodySmall, color = tones[i]) }
            if (s.others > 0) Text("${stringResource(R.string.kpi4_others)} ${s.others}%", style = MaterialTheme.typography.bodySmall, color = tones[3])
        }
    }
}
