package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.DonePage
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Lavori
import it.pixelbox.cmwatch.rules.RecapActions
import it.pixelbox.cmwatch.rules.RecapAgenda
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Le due metà del cruscotto del tablet: sinistra Per te e Stanotte, destra In corso e Fatto. */
enum class LavoriPart { ALL, LEFT, RIGHT }

enum class LavoriHeader { BAR, NONE, DESK }

/**
 * La pagina unica «Lavori» (mockup approvato da Franz il 22/10, `docs/mockup/2026-10-22-lavori/`): al posto di Registro,
 * Recap e della sezione Impegni della home. Quattro blocchi per domanda: Per te (ognuno col suo tasto), In corso (un
 * compito per riga con la barra dei sette giorni), Stanotte (la coda, solo qui), Fatto (per giorno, con la prova). La
 * quota resta in «Utilizzo e limiti». Un solo bottone pieno: «Approva» (o «Aggiungi» quando non c'è un ok da dare).
 */
@Composable
fun LavoriScreen(
    state: State, agenda: AgendaPage?, events: List<Event>, now: Long,
    onBack: () -> Unit, onApprove: (task: String, note: String) -> Unit, onAgendaEdit: (RecapAgenda.Edit) -> Unit,
    onStart: (RecapActions.Action) -> Unit, onDoneForPerson: (Commitment) -> Unit,
    onAddNight: () -> Unit, onRemoveNight: (String) -> Unit, onNight: () -> Unit, onSession: (String) -> Unit,
    /** BAR = ← e titolo; NONE = la testata la dà la shell del telefono; DESK = nel cruscotto del tablet, il titolo come le sezioni. */
    header: LavoriHeader = LavoriHeader.BAR, part: LavoriPart = LavoriPart.ALL,
    today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault(),
    /** Contratto 1.77: le pagine dell'op `done` già caricate, per data; con almeno una il blocco Fatto non guarda più gli eventi. `onLoadDay` carica un giorno richiuso. */
    donePages: Map<String, DonePage> = emptyMap(), onLoadDay: (String) -> Unit = {},
    /** Solo per i provini: le pieghe già aperte (`outside`, `claude`, `paused`, `others`, `day:<data>`). */
    startOpen: Set<String> = emptySet(),
) {
    val fy = remember(state, agenda, now, today) { Lavori.forYou(state, agenda, now, today) }
    val ip = remember(state, agenda, now, zone, today) { Lavori.inProgress(state, agenda, now, zone, today) }
    val dn = remember(events, donePages, zone, today) { if (donePages.isNotEmpty()) Lavori.doneFromPages(donePages, zone) else Lavori.done(events, zone, today) }
    var open by rememberSaveable { mutableStateOf(startOpen) }
    fun toggle(k: String) { open = if (k in open) open - k else open + k }
    val left = part != LavoriPart.RIGHT
    val right = part != LavoriPart.LEFT
    val oldRelay = state.commitments == null
    val hasOk = fy.ok.any { it.approval != null }

    LazyColumn(Modifier.fillMaxSize().background(CmColors.bg), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 28.dp)) {
        item("head") {
            if (header == LavoriHeader.BAR) Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = CmColors.text) }
                Text(stringResource(R.string.lavori_title), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                Text(stringResource(R.string.lavori_for_you_n, fy.total), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            } else if (header == LavoriHeader.DESK) DeskSectionTitle(stringResource(R.string.lavori_title))
        }
        if (left) {
            // ---- Per te ----
            item("sec-foryou") { HomeSection(stringResource(R.string.lavori_foryou) + " · " + fy.total, null, true) {} }
            item("expl-foryou") { Expl(stringResource(R.string.lavori_foryou_expl)) }
            if (fy.total == 0) item("foryou-empty") { Expl(stringResource(R.string.lavori_nothing), CmColors.text) }
            if (fy.ok.isNotEmpty()) {
                item("grp-ok") { GroupHeader(stringResource(R.string.lavori_ok) + " · " + fy.ok.size, CmColors.primary) }
                fy.ok.forEachIndexed { i, r ->
                    item("fy-ok-$i-${r.approval?.task ?: r.commitment?.task}") { ForYouCard(r, zone, stringResource(R.string.approve).takeIf { r.approval != null }, filled = true) { note -> r.approval?.let { a -> onApprove(a.task, note) } } }
                }
            }
            if (fy.outside.isNotEmpty()) {
                item("grp-outside") { GroupHeader(stringResource(R.string.lavori_outside) + " · " + fy.outside.size, CmColors.waiting) }
                val shown = if ("outside" in open) fy.outside else fy.outside.take(2)
                shown.forEachIndexed { i, r ->
                    item("fy-out-$i-${r.agenda?.key ?: r.commitment?.task ?: r.title}") {
                        ForYouCard(r, zone, stringResource(R.string.agenda_done), filled = false) {
                            r.agenda?.let { onAgendaEdit(RecapAgenda.Edit(RecapAgenda.Item.DONE, it)) }
                            r.commitment?.let(onDoneForPerson)
                        }
                    }
                }
                if (fy.outside.size > 2) item("fold-outside") { Fold(if ("outside" in open) stringResource(R.string.lavori_less) else stringResource(R.string.lavori_more, fy.outside.size - 2), "outside" in open) { toggle("outside") } }
            }
            if (fy.claude.isNotEmpty()) {
                item("grp-claude") { GroupHeader(stringResource(R.string.lavori_claude) + " · " + fy.claude.size, CmColors.busy) }
                val shown = if ("claude" in open) fy.claude else fy.claude.take(2)
                shown.forEachIndexed { i, r ->
                    item("fy-cl-$i-${r.agenda?.key ?: r.title}") { ForYouCard(r, zone, stringResource(R.string.lavori_start), filled = false) { r.agenda?.let { onStart(RecapAgenda.doIt(it)) } } }
                }
                if (fy.claude.size > 2) item("fold-claude") { Fold(if ("claude" in open) stringResource(R.string.lavori_less) else stringResource(R.string.lavori_more, fy.claude.size - 2), "claude" in open) { toggle("claude") } }
            }
        }
        if (right) {
            // ---- In corso ----
            val n = ip.active.size + ip.paused.size + ip.others.size
            item("sec-progress") { HomeSection(stringResource(R.string.lavori_progress) + " · " + n, null, true) {} }
            item("expl-progress") { Expl(stringResource(R.string.lavori_progress_expl)) }
            if (oldRelay) item("progress-old") { Expl(stringResource(R.string.lavori_old_relay), CmColors.text) }
            else if (n == 0) item("progress-empty") { Expl(stringResource(R.string.lavori_nothing_progress), CmColors.text) }
            if (ip.active.isNotEmpty() || ip.paused.isNotEmpty()) item("axis") { AxisRow(ip.axis) }
            ip.active.forEach { w -> item("wk-${w.row!!.c.task}") { WorkCard(w, ip.axis, today, zone) } }
            if (ip.paused.isNotEmpty()) {
                item("fold-paused") { Fold(stringResource(R.string.lavori_paused_n, ip.paused.size) + ": " + ip.paused.joinToString(", ") { it.row!!.c.title }, "paused" in open) { toggle("paused") } }
                if ("paused" in open) ip.paused.forEach { w -> item("wk-${w.row!!.c.task}") { WorkCard(w, ip.axis, today, zone) } }
            }
            if (ip.others.isNotEmpty()) {
                item("fold-others") { Fold(stringResource(R.string.lavori_others_n, ip.others.size) + ": " + ip.others.joinToString(", ") { it.agenda!!.title }, "others" in open) { toggle("others") } }
                if ("others" in open) ip.others.forEachIndexed { i, w -> item("ot-$i-${w.agenda!!.key}") { WorkCard(w, ip.axis, today, zone) } }
            }
        }
        if (left) {
            // ---- Stanotte ----
            item("sec-tonight") { HomeSection(stringResource(R.string.lavori_tonight), null, true) {} }
            item("expl-tonight") { Expl(stringResource(R.string.lavori_tonight_expl)) }
            item("tonight") { Box(Modifier.padding(horizontal = 4.dp)) { Tonight(state, onAddNight, onRemoveNight) } }
            state.night.report?.let { r ->
                item("last-night") {
                    val d = runCatching { LocalDate.parse(r.date) }.getOrNull()
                    Fold(stringResource(R.string.lavori_last_night, d?.let { dayMonth(it) } ?: r.date), null, onNight)
                }
            }
        }
        if (right) {
            // ---- Fatto ----
            item("sec-done") { HomeSection(stringResource(R.string.lavori_done), null, true) {} }
            item("expl-done") { Expl(stringResource(R.string.lavori_done_expl)) }
            if (dn.isEmpty()) item("done-empty") { Expl(stringResource(R.string.lavori_done_empty), CmColors.text) }
            dn.forEach { day ->
                val key = "day:${day.date}"
                val isToday = day.date == today
                val shown = isToday || key in open
                item("dh-${day.date}") {
                    val label = when {
                        isToday -> stringResource(R.string.lavori_today, dayMonth(day.date), day.n)
                        day.date == today.minusDays(1) -> stringResource(R.string.lavori_yesterday, dayMonth(day.date), day.n)
                        else -> stringResource(R.string.lavori_day, weekdayDayMonth(day.date), day.n)
                    }
                    // Un giorno richiuso con il solo conteggio (1.77) si carica al tocco con `done` di quel giorno.
                    DayHeader(label, shown, if (isToday) null else ({ toggle(key); if (day.rows.isEmpty() && day.n > 0) onLoadDay(day.date.toString()) }))
                }
                if (shown) day.rows.forEach { d -> item("dn-${day.date}-${d.task}") { DoneRow(d, zone, onSession) } }
            }
            item("quota-note") { Expl(stringResource(R.string.lavori_quota_note), CmColors.briefGoodInk, top = 14.dp) }
        }
        if (!hasOk && left && !right) item("end") { Spacer(Modifier.height(1.dp)) }
    }
}

/** La riga che spiega il blocco, con parole semplici. */
@Composable
private fun Expl(text: String, color: Color = CmColors.text2, top: androidx.compose.ui.unit.Dp = 0.dp) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = top, bottom = 8.dp))

/** Una riga richiusa: «Altre N», «Altri N in pausa», «La notte del …»; `open` null = un rimando, non una piega. */
@Composable
private fun Fold(text: String, open: Boolean?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).handCursor().clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.weight(1f))
        Icon(
            when (open) { null -> Icons.AutoMirrored.Rounded.KeyboardArrowRight; true -> Icons.Rounded.ExpandLess; false -> Icons.Rounded.ExpandMore },
            null, tint = CmColors.actionIcon, modifier = Modifier.size(20.dp),
        )
    }
}

/** Una riga di «Per te»: il segno (ok, agenda, pausa), il titolo, chi e quando, e il suo tasto. */
@Composable
internal fun ForYouCard(r: Lavori.ForYou, zone: ZoneId, button: String?, filled: Boolean, onClick: (note: String) -> Unit) {
    var ask by rememberSaveable(r.title) { mutableStateOf(false) }
    val a = r.approval; val who = r.who; val at = r.at; val ag = r.agenda; val cm = r.commitment
    val sub = when (r.kind) {
        Lavori.Kind.OK -> when {
            who != null && at != null -> stringResource(R.string.lavori_asks, who, hhmm(at, zone))
            who != null -> stringResource(R.string.lavori_asks_who, who)
            else -> stringResource(R.string.lavori_waits_ok)
        }
        Lavori.Kind.OUTSIDE -> if (cm != null) who?.let { stringResource(R.string.lavori_unblocks, it) }
            else listOfNotNull(who, ag?.scope?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { null }
        Lavori.Kind.CLAUDE -> listOfNotNull(who, ag?.scope?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { null }
    }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            ag != null -> AgendaMark(ag.scope)
            cm?.paused != null -> Icon(Icons.Rounded.Pause, null, tint = CmColors.text2, modifier = Modifier.size(22.dp))
            else -> Icon(Icons.Rounded.Verified, null, tint = CmColors.advice, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(r.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
            sub?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2) }
        }
        if (button != null) {
            val click = { if (a != null) ask = true else onClick("") }
            if (filled) Button(onClick = click, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)) { Text(button) }
            else FilledTonalButton(onClick = click) { Text(button) }
        }
    }
    if (ask && a != null) ApproveDialog(a, onConfirm = { n -> ask = false; onClick(n) }, onDismiss = { ask = false })
}

/** I sette giorni dell'asse, poi «oggi». */
@Composable
private fun AxisRow(axis: Lavori.Axis) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        axis.days.forEach { (label, _) -> Text(label, style = MonoSmall.copy(fontSize = 11.sp), modifier = Modifier.weight(1f)) }
        Text(stringResource(R.string.lavori_axis_today), style = MonoSmall.copy(fontSize = 11.sp, color = CmColors.text))
    }
    Text(stringResource(R.string.lavori_key), style = MonoSmall.copy(fontSize = 11.sp), modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp))
}

/** La barra dei sette giorni: pista, tratti pieni (al lavoro) o tratteggiati (in pausa), la riga di adesso. */
@Composable
private fun LavoriBar(bars: List<Lavori.Bar>, now: Double) {
    val solid = CmColors.busy; val dashed = CmColors.text2.copy(alpha = 0.7f); val track = CmColors.briefTrack; val line = CmColors.text
    Canvas(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp).height(16.dp)) {
        val h = 8.dp.toPx(); val y = (size.height - h) / 2; val w = size.width
        drawRoundRect(track, Offset(0f, y), Size(w, h), CornerRadius(h / 2))
        bars.forEach { b ->
            val x0 = (b.from * w).toFloat(); val x1 = (b.to * w).toFloat()
            if (!b.dashed) drawRoundRect(solid, Offset(x0, y), Size(x1 - x0, h), CornerRadius(h / 2))
            else { var x = x0; val step = 6.dp.toPx(); val gap = 4.dp.toPx(); while (x < x1) { drawRoundRect(dashed, Offset(x, y), Size(minOf(step, x1 - x), h), CornerRadius(2.dp.toPx())); x += step + gap } }
        }
        val nx = (now * w).toFloat().coerceIn(1f, w - 1f)
        drawRect(line, Offset(nx - 1f, 0f), Size(2f, size.height))
    }
}

/** Una riga di «In corso»: titolo, chi · da quando · prossimo passo, lo stato e la barra; una riga dell'agenda ferma su terzi senza barra. */
@Composable
private fun WorkCard(w: Lavori.Work, axis: Lavori.Axis, today: LocalDate, zone: ZoneId) {
    val c = w.row?.c; val a = w.agenda
    val title = c?.title ?: a?.title.orEmpty()
    val (tag, tone) = when {
        c?.paused != null -> stringResource(R.string.lavori_state_paused) to CmColors.text2
        c != null -> stringResource(R.string.lavori_state_active) to CmColors.busy
        else -> stringResource(R.string.lavori_state_others) to CmColors.text2
    }
    val meta = if (c != null) listOfNotNull(
        c.session,
        c.approvedAt?.let { at -> val d = Instant.ofEpochSecond(at).atZone(zone).toLocalDate(); if (d == today) stringResource(R.string.lavori_ok_at, hhmm(at, zone)) else stringResource(R.string.lavori_ok_on, dayMonth(d), hhmm(at, zone)) },
        c.paused?.let { stringResource(R.string.lavori_paused_reason, it.reason) } ?: c.next?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.lavori_next, it) },
    ).joinToString(" · ") else listOfNotNull(a?.ref?.takeIf { it.isNotBlank() }, a?.blocks?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.lavori_blocks, it) }).joinToString(" · ")
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.weight(1f))
            Tag(tag, tone)
        }
        val metaColor = when (w.row?.tone) { it.pixelbox.cmwatch.rules.Commitments.Tone.ALERT -> CmColors.gone; it.pixelbox.cmwatch.rules.Commitments.Tone.WARN -> CmColors.waiting; else -> CmColors.text2 }
        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = metaColor)
        if (w.bars.isNotEmpty()) LavoriBar(w.bars, axis.now)
    }
}

/** L'intestazione di un giorno di «Fatto»; oggi non si richiude. */
@Composable
private fun DayHeader(label: String, open: Boolean, onToggle: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onToggle != null) Modifier.clip(RoundedCornerShape(10.dp)).handCursor().clickable(onClick = onToggle) else Modifier).padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label.uppercase(), style = MonoSmall.copy(color = CmColors.text2, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold))
        Box(Modifier.weight(1f).height(1.dp).background(CmColors.line))
        if (onToggle != null) Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = CmColors.actionIcon, modifier = Modifier.size(18.dp))
    }
}

/** Un compito chiuso: l'ora, il titolo, la sessione e la prova («controllo verde» finché non c'è la 1.77). */
@Composable
private fun DoneRow(d: Lavori.Done, zone: ZoneId, onSession: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(18.dp)).background(CmColors.surfaceLow).padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (d.outcome) {
            "failed" -> Icon(Icons.Rounded.Close, null, tint = CmColors.gone, modifier = Modifier.size(18.dp).padding(top = 1.dp))
            "cancelled" -> Icon(Icons.Rounded.Remove, null, tint = CmColors.text2, modifier = Modifier.size(18.dp).padding(top = 1.dp))
            else -> Icon(Icons.Rounded.Check, null, tint = CmColors.briefGoodInk, modifier = Modifier.size(18.dp).padding(top = 1.dp))
        }
        Text(hhmm(d.at, zone), style = MonoSmall.copy(fontSize = 12.sp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(d.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                d.session?.let { s -> Text(s, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, modifier = Modifier.clip(RoundedCornerShape(6.dp)).handCursor().clickable { onSession(s) }) }
                val uri = LocalUriHandler.current
                val url = d.url
                Text(
                    (if (d.session != null) "· " else "") + (d.proof ?: stringResource(R.string.lavori_proof_check)), style = MaterialTheme.typography.bodySmall, color = CmColors.actionIcon,
                    modifier = if (url != null) Modifier.clip(RoundedCornerShape(6.dp)).handCursor().clickable { runCatching { uri.openUri(url) } } else Modifier,
                )
            }
        }
    }
}

private fun hhmm(epoch: Long, zone: ZoneId): String = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochSecond(epoch).atZone(zone))

@Composable private fun dayMonth(d: LocalDate): String = DateTimeFormatter.ofPattern("dd/MM").format(d)

@Composable private fun weekdayDayMonth(d: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofPattern("EEEE dd/MM", locale).format(d).replaceFirstChar { it.titlecase(locale) }
}

/** Stanotte (dal Registro, 22/10): una riga con «Aggiungi» tonale (il bottone pieno in fondo non c'è più), poi i lavori in coda. */
@Composable
internal fun Tonight(state: State, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    val items = state.night.items
    RegCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RegLabel(stringResource(R.string.reg_tonight))
                Text(
                    if (state.night.queued > 0) pluralStringResource(R.plurals.reg_queued, state.night.queued, state.night.queued) else stringResource(R.string.reg_queue_empty),
                    style = MaterialTheme.typography.bodyLarge, color = CmColors.text,
                )
            }
            // Contratto 1.17: senza `items` il relay è precedente e la coda non si tocca dall'app.
            if (items != null) FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.reg_add)) }
        }
        state.night.running?.let {
            Text(stringResource(R.string.night_running, it), color = CmColors.busy, style = MaterialTheme.typography.bodyMedium)
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), color = CmColors.busy, trackColor = CmColors.briefTrack)
        }
        if (items == null) Text(stringResource(R.string.night_update_pc), color = CmColors.text2, style = MaterialTheme.typography.bodyMedium)
        items?.forEach { job ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(job.name, fontWeight = FontWeight.SemiBold, color = CmColors.text, style = MaterialTheme.typography.bodyMedium)
                    Text(job.prompt, color = CmColors.text2, style = MaterialTheme.typography.bodySmall)
                    if (job.started != null) Text(stringResource(R.string.night_started), color = CmColors.busy, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { onRemove(job.id) }, enabled = job.started == null) { Text(stringResource(R.string.night_remove), color = CmColors.actionIcon) }
            }
        }
    }
}

/** Il blocco della coda (dal Registro): fondo basso, angoli ampi, come nel mockup. */
@Composable
internal fun RegCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).smoothSize().then(modifier).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
    )

@Composable
internal fun RegLabel(text: String) = Text(text.uppercase(), style = MonoSmall.copy(color = CmColors.briefLabel, letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)))


