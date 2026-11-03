package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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

// I KPI coi dati già nel relay (proposte 6, 11 e 18 del 25/10, compito app-kpi-medie-25-10). Etichette sul grafico, niente
// legende (Franz 25/10 21:33); ogni vista porta il suo testTag kpi-N per il controllo.

private fun bandColor(b: Kpi.Band) = when (b) {
    Kpi.Band.WORK -> Cm.busy
    Kpi.Band.WAIT -> Cm.waiting
    Kpi.Band.STILL -> Cm.briefTrack
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
            Box(Modifier.width(4.dp).height(h.dp).clip(RoundedCornerShape(1.dp)).background(if (c == 0) Cm.briefTrack else Cm.busy))
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
            Text(stringResource(R.string.kpi11_title).uppercase(), style = MonoSmall.copy(fontSize = 11.sp, letterSpacing = 0.7.sp), color = Cm.text2, maxLines = 1, modifier = Modifier.weight(1f))
            Text(if (h > 0) stringResource(R.string.kpi11_work_h, h, m) else stringResource(R.string.kpi11_work_m, m), style = MonoSmall.copy(fontSize = 11.sp), color = Cm.text2, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
            spans.forEach { s -> Box(Modifier.weight(((s.to - s.from) / len).coerceAtLeast(0.001f)).fillMaxHeight().background(bandColor(s.band))) }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(12.dp)) {
            val w = maxWidth
            var hour = 0
            while (from + hour * 3600L <= now) {
                val x = hour * 3600f / len
                if (x <= 0.92f) Text("%02d".format(hour), style = MonoSmall.copy(fontSize = 10.sp), color = Cm.text2, maxLines = 1, modifier = Modifier.offset(x = w * x))
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
                if (f.waitingNow > 0) Cm.waiting else null, Modifier.weight(1f),
            )
            Figure(tokens, f.topSession?.let { stringResource(R.string.kpi18_tokens, it, Math.round(f.topShare * 100).toInt()) } ?: stringResource(R.string.kpi18_tokens_none), null, Modifier.weight(1f))
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Cm.busy)) { append(stringResource(R.string.kpi18_prompt)) }
                append(" ${stringResource(R.string.kpi18_and)} ")
                withStyle(SpanStyle(color = Cm.idle)) { append(stringResource(R.string.kpi18_commit)) }
                append(" ${stringResource(R.string.kpi18_per_hour)}")
            },
            style = MaterialTheme.typography.bodySmall, color = Cm.text2,
        )
        val top = (f.perHour.maxOfOrNull { it.first + it.second } ?: 0).coerceAtLeast(1)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cm.surfaceLow).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom,
        ) {
            f.perHour.forEachIndexed { i, (p, c) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Column(Modifier.fillMaxWidth().height(58.dp), verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)) {
                        if (c > 0) Box(Modifier.fillMaxWidth().height((c * 56f / top).dp).clip(RoundedCornerShape(2.dp)).background(Cm.idle))
                        if (p > 0) Box(Modifier.fillMaxWidth().height((p * 56f / top).dp).clip(RoundedCornerShape(2.dp)).background(Cm.busy))
                    }
                    val hour = f.firstHour + i
                    Text(if (hour % 4 == 0) hour.toString() else "", style = MonoSmall.copy(fontSize = 10.sp), color = Cm.text2, maxLines = 1, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
internal fun Figure(value: String, label: String, tone: Color?, modifier: Modifier, chevron: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Cm.surfaceLow).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = tone ?: Cm.text, maxLines = 1, modifier = Modifier.weight(1f))
            // I box della home si aprono (app-box-cliccabili-29-10): la freccina lo dice.
            if (chevron) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.ChevronRight, null, tint = Cm.text2, modifier = Modifier.size(16.dp))
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = Cm.text2)
    }
}

// ---- Proposte piccole (4, 5, 7; compito app-kpi-piccole-25-10) ----

/**
 * 7 · il tabellone in testa alla home (stile «Studio e Diretta», 30/10): tre cifre, solo chi ti aspetta in ambra, i rossi in
 * rosso; ognuna apre la sua pagina, anche a zero. Sotto la quota delle 5 ore, neutra, ambra da 75 % e rossa da 90 %.
 * quota-senza-cinque-ore: dove il dato manca, per quell'account, la settimana col giorno e il ritmo.
 */
@Composable
internal fun Kpi7(h: Kpi.Head, st: it.pixelbox.cmwatch.contract.State, now: Long, onBox: (HomeBox) -> Unit, onQuota: () -> Unit = {}, joined: Boolean = false, slot: @Composable () -> Unit = {}) {
    // `joined`: sotto stanno le righe In cima (app-utilizzo-una-barra-03-11), attaccate: gli angoli in basso sono loro.
    val shape = if (joined) RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp) else V3.Shape
    Column(Modifier.testTag("kpi-7").fillMaxWidth().clip(shape).background(V3.S1).then(if (V3.day) Modifier.border(1.dp, V3.Line, shape) else Modifier.border(1.dp, LitEdge, shape))) {
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            val m = (((h.oldestWaitS ?: 0L) + 30) / 60)
            val box = { b: HomeBox, tag: String, w: Float -> Modifier.weight(w).fillMaxHeight().then(if (V3.day && b == HomeBox.WAITING) Modifier.background(V3.WaitFill) else Modifier).testTag(tag).handCursor().clickable { onBox(b) } }
            Board(h.waiting.toString(), if (h.waiting > 0) pluralStringResource(R.plurals.kpi7_waiting, h.waiting, h.waiting, m) else stringResource(R.string.kpi7_none_waiting), if (h.waiting > 0) V3.Wait else V3.Tx, null, box(HomeBox.WAITING, "home-box-aspetta", 1.15f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(V3.Line))
            Board(h.busy.toString(), stringResource(R.string.kpi7_busy), V3.Tx, null, box(HomeBox.BUSY, "home-box-lavoro", 1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(V3.Line))
            Board(h.outcomes?.toString() ?: "–", if (h.outcomes == null) stringResource(R.string.kpi7_outcomes_unknown) else stringResource(R.string.kpi7_outcomes), V3.Tx, (h.red ?: 0).takeIf { it > 0 }?.let { pluralStringResource(R.plurals.kpi7_red, it, it) }, box(HomeBox.OUTCOMES, "home-box-esiti", 1.15f))
        }
        // La barra fissa delle 5 ore non c'è più (app-utilizzo-una-barra-03-11): le finestre d'uso stanno nella riga Utilizzo, e una
        // vicina al limite entra da sola nel tabellone.
        slot()
    }
}

@Composable
internal fun Modifier.drawDashedEdge(hot: Boolean): Modifier {
    val c = if (hot) V3.Tx else V3.Tx3
    return then(Modifier.drawBehind {
        drawRoundRect(c, cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(if (hot) 2.dp.toPx() else 1.dp.toPx(), pathEffect = if (hot) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
    })
}

/**
 * Il bersaglio dello slot durante il trascinamento (Franz 01/11 15:43): sopra il tabellone, che resta alto uguale e attenuato. Niente
 * spinge l'elenco, né alla presa né al rilascio. Si accende solo col dito dentro.
 */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.SlotDrop(hot: Boolean = false, full: Boolean = false) {
    Box(Modifier.matchParentSize().clip(V3.Shape).background(V3.Bg.copy(alpha = if (hot) .55f else .72f)).testTag(if (full) "slot-pieno" else if (hot) "slot-rilascia-acceso" else "slot-rilascia").then(Modifier.drawDashedEdge(hot)), contentAlignment = Alignment.Center) {
        // Il tabellone a quattro voci (app-tabellone-quattro-02-11): pieno, lo dice invece di prendere la voce.
        Text(stringResource(if (full) R.string.home_board_full else R.string.home_slot_drop), style = V3.Name.copy(fontSize = 15.sp, color = V3.Tx), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

/** La riga di separazione sopra una voce dello slot: nel riquadro dei tre numeri sì, in un riquadro suo del tabellone no. */
internal val LocalSlotLine = androidx.compose.runtime.compositionLocalOf { true }

/** Dentro il tabellone le voci sono righe compatte su una riga sola (app-utilizzo-una-barra-03-11, Franz 03/11 13:11). */
internal val LocalBoardRow = androidx.compose.runtime.compositionLocalOf { false }

/** Tira su l'elemento della lista dello spazio fra gli elementi, così la riga si attacca a quella sopra. */
internal fun Modifier.pullUp(gap: androidx.compose.ui.unit.Dp): Modifier = layout { m, c ->
    val p = m.measure(c)
    val g = gap.roundToPx()
    layout(p.width, (p.height - g).coerceAtLeast(0)) { p.place(0, -g) }
}

/**
 * Una voce In cima: una riga del tabellone (app-utilizzo-una-barra-03-11). `above`: si attacca a quella sopra (il tabellone o la riga
 * prima); `below`: quella sotto le si attacca. Dove una voce aperta mette il suo corpo fra le righe, il pezzo si chiude con gli angoli.
 */
@Composable
internal fun BoardCard(modifier: Modifier = Modifier, above: Boolean = false, below: Boolean = false, content: @Composable () -> Unit) {
    val r = 14.dp
    val shape = RoundedCornerShape(topStart = if (above) 0.dp else r, topEnd = if (above) 0.dp else r, bottomStart = if (below) 0.dp else r, bottomEnd = if (below) 0.dp else r)
    androidx.compose.foundation.layout.Column(modifier.then(if (above) Modifier.pullUp(8.dp) else Modifier).fillMaxWidth().clip(shape).background(V3.S1).border(1.dp, V3.Line, shape)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalSlotLine provides false, LocalBoardRow provides true) { content() }
    }
}

/**
 * Una voce agganciata, senza un dato in più: la stessa struttura delle altre voci In cima (Franz 03/11 10:04,
 * app-in-cima-uguali-03-11), la riga del titolo e sotto il sottotitolo, cioè il riepilogo della riga in elenco su una riga, o i
 * nomi (`names`, come Suggerimenti). A destra solo il pallino e la freccia.
 */
/**
 * La riga compatta del tabellone (app-utilizzo-una-barra-03-11, mockup 1 approvato da Franz alle 13:11): icona, nome, numero e a destra
 * lo stato su una riga, o la barra col suo numero; il tocco apre la voce.
 */
@Composable
internal fun BoardRow(drag: Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String?, count: String? = null, right: String? = null, bar: (@Composable (Modifier) -> Unit)? = null, number: String? = null, tone: Color? = null, open: Boolean? = null, dot: Color? = null, nav: Boolean = false, hot: Boolean = false, alarm: Boolean = false, onToggle: () -> Unit) {
    val ink = tone ?: if (hot) V3.Wait else null
    Row(Modifier.fillMaxWidth().then(if (alarm) Modifier.background(V3.Wait.copy(alpha = 0.14f)) else Modifier).hotEdge(hot).then(drag).handCursor().clickable(onClick = onToggle).then(if (tag != null) Modifier.testTag(tag) else Modifier).heightIn(min = 48.dp).padding(start = 14.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ink ?: V3.Tx2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = V3.Name.copy(fontSize = 16.sp, color = ink ?: V3.Tx), maxLines = 1)
        count?.let { SectionCount(it, hot, V3.Name.copy(fontSize = 14.sp), Modifier.padding(start = 6.dp)) }
        Spacer(Modifier.width(12.dp))
        if (bar != null) bar(Modifier.weight(1f)) else if (right == null || number != null) Spacer(Modifier.weight(1f))
        if (number != null) { Spacer(Modifier.width(10.dp)); Text(number, style = V3.Meter.copy(color = ink ?: V3.Tx2), maxLines = 1) }
        else right?.let { Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { OneLineText(it, V3.Small.copy(fontSize = 14.sp, lineHeight = 18.sp, color = ink ?: V3.Tx2)) } }
        dot?.let { c -> Box(Modifier.padding(start = 6.dp).size(7.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c)) }
        if (open != null) Icon(if (open) androidx.compose.material.icons.Icons.Rounded.ExpandLess else androidx.compose.material.icons.Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.padding(start = 4.dp).size(24.dp))
        else if (nav) Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3, modifier = Modifier.padding(start = 4.dp).size(24.dp))
    }
}

@Composable
internal fun SlotRow(drag: Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, count: String? = null, right: String? = null, tag: String? = null, open: Boolean? = null, dot: Color? = null, nav: Boolean = false, hot: Boolean = false, names: List<String>? = null, onToggle: () -> Unit) {
    if (LocalBoardRow.current) return BoardRow(drag, title, icon, tag, count, right ?: names?.joinToString(" · "), open = open, dot = dot, nav = nav, hot = hot, onToggle = onToggle)
    if (LocalSlotLine.current) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
    Column(Modifier.fillMaxWidth().hotEdge(hot).then(drag).handCursor().clickable(onClick = onToggle).then(if (tag != null) Modifier.testTag(tag) else Modifier).padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)) {
        Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (hot) V3.Wait else V3.Tx2, modifier = Modifier.capAlign(V3.Name.copy(fontSize = 15.sp)).size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = V3.Name.copy(fontSize = 15.sp), maxLines = 1)
            count?.let { SectionCount(it, hot, V3.Name.copy(fontSize = 13.sp), Modifier.padding(start = 6.dp)) }
            Spacer(Modifier.weight(1f))
            dot?.let { c -> Box(Modifier.padding(end = 6.dp).size(7.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c)) }
            if (open != null) Icon(if (open) androidx.compose.material.icons.Icons.Rounded.ExpandLess else androidx.compose.material.icons.Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.padding(start = 4.dp).size(24.dp))
            else if (nav) Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3, modifier = Modifier.padding(start = 4.dp).size(24.dp))
        }
        if (!names.isNullOrEmpty()) SlotNames(names)
        else right?.let { OneLineText(it, V3.Small.copy(fontSize = 13.5.sp, lineHeight = 17.sp), Modifier.fillMaxWidth(), start = true) }
    }
}

/**
 * Una voce nello slot (Franz 01/11 17:44): la forma di [UsageSlotRow], titolo, barra e numero, sotto una riga. Il riepilogo a destra
 * non c'è più, per non ripetere. Compare al rilascio: durante la presa lo slot resta com'è.
 */
@Composable
internal fun SlotInfoRow(drag: Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, count: String? = null, bar: List<Pair<Int, Color>>? = null, number: String? = null, numberTone: Color? = null, note: String? = null, names: List<String>? = null, open: Boolean, onToggle: () -> Unit) {
    if (LocalBoardRow.current) return BoardRow(drag, title, icon, tag, count, right = if (bar != null && number != null) null else names?.joinToString(" · ") ?: note, bar = bar?.let { b -> { m -> SlotBar(b, m) } }, number = number.takeIf { bar != null }, open = open, onToggle = onToggle)
    if (LocalSlotLine.current) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
    Column(Modifier.fillMaxWidth().then(drag).handCursor().clickable(onClick = onToggle).testTag(tag).padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)) {
        Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = V3.Tx2, modifier = Modifier.capAlign(V3.Name.copy(fontSize = 15.sp)).size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = V3.Name.copy(fontSize = 15.sp), maxLines = 1)
            count?.let { Text(it, style = V3.Name.copy(fontSize = 13.sp, color = V3.Tx2), modifier = Modifier.padding(start = 6.dp)) }
            Spacer(Modifier.width(10.dp))
            if (bar != null) SlotBar(bar, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            if (number != null) {
                Spacer(Modifier.width(10.dp))
                Text(number, style = V3.Meter.copy(color = numberTone ?: V3.Tx), maxLines = 1)
            }
            Icon(if (open) androidx.compose.material.icons.Icons.Rounded.ExpandLess else androidx.compose.material.icons.Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.padding(start = 4.dp).size(24.dp))
        }
        if (names != null) SlotNames(names)
        else note?.let { Text(it, style = V3.Small.copy(fontSize = 13.5.sp, lineHeight = 17.sp)) }
    }
}

/** Suggerimenti nello slot: una riga ferma coi nomi; quelli interi che stanno nella larghezza vera, poi «+N». */
@Composable
internal fun SlotNames(names: List<String>) {
    val style = V3.Small.copy(fontSize = 13.5.sp, lineHeight = 17.sp)
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth.toFloat()
        val line = androidx.compose.runtime.remember(names, width, style) { it.pixelbox.cmwatch.rules.SlotInfo.fitNames(names, width) { s -> measurer.measure(s, style, softWrap = false, maxLines = 1).size.width.toFloat() } }
        line?.let { Text(it, style = style, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Clip) }
    }
}

/** La barra dello slot: uno o due tratti di fila, in percento della larghezza. */
@Composable
private fun SlotBar(parts: List<Pair<Int, Color>>, modifier: Modifier) {
    Row(modifier.height(6.dp).clip(RoundedCornerShape(3.dp)).background(V3.S3)) {
        parts.filter { it.first > 0 }.forEach { (pct, tone) -> Box(Modifier.weight(pct.toFloat()).fillMaxHeight().background(tone)) }
        val rest = 100 - parts.sumOf { it.first }
        if (rest > 0) Spacer(Modifier.weight(rest.toFloat()))
    }
}

/** Sistema nello slot: il carico, sotto la RAM del Chromebook e delle altre macchine, e le sessioni attive. */
@Composable
internal fun SystemSlotRow(i: it.pixelbox.cmwatch.rules.SlotInfo.SystemInfo, drag: Modifier, open: Boolean, onToggle: () -> Unit) {
    val hosts = i.hosts.map { h -> h.ramPct?.let { stringResource(R.string.slot_host_ram, h.name, it) } ?: stringResource(R.string.slot_host_off, h.name) }
    val note = (listOf(stringResource(R.string.slot_ram, i.ramPct)) + hosts + pluralStringResource(R.plurals.slot_sessions, i.sessions, i.sessions)).joinToString(" · ")
    SlotInfoRow(drag, stringResource(R.string.home_sec_system), androidx.compose.material.icons.Icons.Outlined.Memory, "home-sez-sistema", bar = listOf(i.barPct to barTone(i.level)), number = "${i.pct}%", note = note, open = open, onToggle = onToggle)
}

/** Lavori nello slot: quanti aspettano Franz su quanti, sotto l'ultimo che lo aspetta. */
@Composable
internal fun LavoriSlotRow(i: it.pixelbox.cmwatch.rules.SlotInfo.LavoriInfo, now: Long, drag: Modifier, open: Boolean, onToggle: () -> Unit) {
    val last = i.last
    val since = i.lastSince
    val note = when {
        last == null -> stringResource(R.string.slot_lavori_none)
        since != null && since > 0 -> stringResource(R.string.slot_lavori_last_since, last, it.pixelbox.cmwatch.contract.Durations.since(since, now))
        else -> stringResource(R.string.slot_lavori_last, last)
    }
    SlotInfoRow(drag, stringResource(R.string.lavori_title), androidx.compose.material.icons.Icons.Outlined.Checklist, "home-sez-lavori", bar = if (i.total > 0) listOf(i.youPct to V3.Wait, (100 - i.youPct) to V3.Tx3) else emptyList(), number = stringResource(R.string.slot_lavori_of, i.you, i.total), note = note, open = open, onToggle = onToggle)
}

/** Plugin e skill nello slot: i nomi da aggiornare, oppure l'ora del controllo. */
@Composable
internal fun PluginsSlotRow(i: it.pixelbox.cmwatch.rules.SlotInfo.PluginsInfo, zone: java.time.ZoneId, drag: Modifier, open: Boolean, onToggle: () -> Unit) {
    val checked = i.checkedAt
    val note = when {
        i.names.isNotEmpty() -> stringResource(R.string.slot_plugins_names, i.names.joinToString(", "))
        checked != null -> stringResource(R.string.slot_plugins_checked, java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(java.time.Instant.ofEpochSecond(checked).atZone(zone)))
        else -> null
    }
    SlotInfoRow(drag, stringResource(R.string.home_sec_plugins), androidx.compose.material.icons.Icons.Outlined.Extension, "home-sez-plugin", note = note, open = open, onToggle = onToggle)
}

/**
 * Utilizzo nel tabellone (app-utilizzo-una-barra-03-11): una riga con la barra e «49% sett.». Con l'allarme ([UtilizzoRules.usageAlarm])
 * la riga è ambra e dice la finestra vicina al limite: le 5 ore con l'ora in cui si azzera, o la settimana.
 */
@Composable
internal fun UsageSlotRow(w: it.pixelbox.cmwatch.rules.UtilizzoRules.WeekSlot?, now: Long, drag: Modifier, open: Boolean, alarm: it.pixelbox.cmwatch.rules.UtilizzoRules.UsageAlarm? = null, onToggle: () -> Unit) {
    if (LocalSlotLine.current) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
    val pct = alarm?.pct ?: w?.pct ?: 0
    val stale = alarm == null && w?.stale == true
    val tone = if (alarm != null) (if (pct >= 90) V3.Red else V3.Wait) else if (stale) V3.Tx3 else V3.Tx2
    val window = if (alarm == null || alarm.week) it.pixelbox.cmwatch.rules.UtilizzoRules.W7_S else it.pixelbox.cmwatch.rules.UtilizzoRules.H5_S
    val tick = it.pixelbox.cmwatch.rules.UtilizzoRules.elapsedPct(alarm?.resetAt ?: w?.resetAt, window, now)
    val title = when { alarm == null -> stringResource(R.string.home_sec_usage); alarm.week -> stringResource(R.string.usage_week); else -> stringResource(R.string.kpi7_quota) }
    val reset = alarm?.takeIf { a -> !a.week }?.resetAt?.takeIf { at -> at > now }?.let { at -> stringResource(R.string.quota_resets_at, java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(java.time.Instant.ofEpochSecond(at).atZone(java.time.ZoneId.systemDefault()))) }
    val number = if (alarm != null) listOfNotNull("$pct%", reset).joinToString(" · ") else stringResource(R.string.usage_week_pct, pct)
    BoardRow(drag, title, androidx.compose.material.icons.Icons.Outlined.DataUsage, "slot-utilizzo", bar = { m -> Meter(pct, null, tick, m, stale = stale, alarm = alarm != null) }, number = number, tone = if (alarm != null) tone else null, open = open, alarm = alarm != null, onToggle = onToggle)
}

@Composable
private fun Board(value: String, label: String, tone: Color, red: String?, modifier: Modifier) {
    Column(modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(value, style = androidx.compose.ui.text.TextStyle(fontFamily = V3.Display, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 38.sp, lineHeight = 40.sp, fontFeatureSettings = CM_TNUM), color = tone, maxLines = 1, modifier = Modifier.weight(1f))
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.ChevronRight, null, tint = V3.Tx3, modifier = Modifier.size(18.dp))
        }
        // Due righe per tutte, anche quando il testo ne occupa una: i tre numeri e le etichette stanno sulle stesse linee (Franz 03/11
        // 09:38, app-tabellone-numeri-03-11). Il rosso degli esiti è la seconda riga.
        Text(label, style = V3.Small.copy(fontSize = 13.5.sp, lineHeight = 17.sp), minLines = if (red != null) 1 else 2)
        red?.let { Text(it, style = V3.Small.copy(fontSize = 13.5.sp, lineHeight = 17.sp, color = V3.Red)) }
    }
}

/** 4 · la quota 5h di un account divisa per sessione; i nomi sono le etichette della barra, nel colore del loro tratto. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun Kpi4(s: Kpi.Split) {
    val tones = listOf(Cm.briefRing, Cm.briefWeek, Cm.briefGood, Cm.text2)
    Column(
        Modifier.testTag("kpi-4").fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cm.surfaceLow).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.kpi4_title, s.account).uppercase(), style = MonoSmall.copy(fontSize = 11.sp, letterSpacing = 0.7.sp), color = Cm.text2, modifier = Modifier.weight(1f))
            Text("${s.h5}%", style = MonoSmall, color = Cm.text)
        }
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Cm.briefTrack)) {
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
