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
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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

/** La vista di «In corso» (mockup 22/10 21:16): l'elenco per chi deve muoversi, o il tempo dei sette giorni. */
enum class LavoriView { LIST, TIME }

/**
 * La pagina unica «Lavori» (mockup approvato da Franz il 22/10, `docs/mockup/2026-10-22-lavori/`): al posto di Registro,
 * Recap e della sezione Impegni della home. Quattro blocchi per domanda: Per te (ognuno col suo tasto), In corso (un
 * compito per riga con la barra dei sette giorni), Stanotte (la coda, solo qui), Fatto (per giorno, con la prova). La
 * quota resta in «Utilizzo e limiti». Un solo bottone pieno: «Approva» (o «Aggiungi» quando non c'è un ok da dare).
 */
@Composable
fun LavoriScreen(
    state: State, agenda: AgendaPage?, events: List<Event>, now: Long,
    onBack: () -> Unit, onApprove: (task: String, note: String, choice: String?) -> Unit, onAgendaEdit: (RecapAgenda.Edit) -> Unit,
    onStart: (RecapActions.Action) -> Unit, onDoneForPerson: (Commitment) -> Unit,
    onAddNight: () -> Unit, onRemoveNight: (String) -> Unit, onNight: () -> Unit, onSession: (String) -> Unit,
    /** BAR = ← e titolo; NONE = la testata la dà la shell del telefono; DESK = nel cruscotto del tablet, il titolo come le sezioni. */
    header: LavoriHeader = LavoriHeader.BAR, part: LavoriPart = LavoriPart.ALL,
    today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault(),
    /** Contratto 1.77: le pagine dell'op `done` già caricate, per data; con almeno una il blocco Fatto non guarda più gli eventi. `onLoadDay` carica un giorno richiuso. */
    donePages: Map<String, DonePage> = emptyMap(), onLoadDay: (String) -> Unit = {},
    /** Solo per i provini: le pieghe già aperte (`outside`, `claude`, `day:<data>`). */
    startOpen: Set<String> = emptySet(),
    /** La vista di «In corso» all'apertura; dal vivo vale l'ultima scelta (preferenze `lavori`), per i provini questa. */
    startView: LavoriView? = null,
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
    // Elenco | Tempo: la scelta resta (preferenze «lavori»); nei provini non c'è un contesto vero, si legge con cautela.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { runCatching { ctx.getSharedPreferences("lavori", android.content.Context.MODE_PRIVATE) }.getOrNull() }
    var timeView by rememberSaveable { mutableStateOf(startView?.let { it == LavoriView.TIME } ?: runCatching { prefs?.getBoolean("time", false) }.getOrNull() ?: false) }

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
                    item("fy-ok-$i-${r.approval?.task ?: r.commitment?.task}") { ForYouCard(r, zone, stringResource(R.string.approve).takeIf { r.approval != null }, filled = true) { note, choice -> r.approval?.let { a -> onApprove(a.task, note, choice) } } }
                }
            }
            if (fy.outside.isNotEmpty()) {
                item("grp-outside") { GroupHeader(stringResource(R.string.lavori_outside) + " · " + fy.outside.size, CmColors.waiting) }
                val shown = if ("outside" in open) fy.outside else fy.outside.take(2)
                shown.forEachIndexed { i, r ->
                    item("fy-out-$i-${r.agenda?.key ?: r.commitment?.task ?: r.title}") {
                        ForYouCard(r, zone, stringResource(R.string.agenda_done), filled = false) { _, _ ->
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
                    item("fy-cl-$i-${r.agenda?.key ?: r.title}") { ForYouCard(r, zone, stringResource(R.string.lavori_start), filled = false) { _, _ -> r.agenda?.let { onStart(RecapAgenda.doIt(it)) } } }
                }
                if (fy.claude.size > 2) item("fold-claude") { Fold(if ("claude" in open) stringResource(R.string.lavori_less) else stringResource(R.string.lavori_more, fy.claude.size - 2), "claude" in open) { toggle("claude") } }
            }
        }
        if (right) {
            // ---- In corso: tre gruppi per chi deve muoversi (mockup della master approvato il 22/10 alle 21:16) ----
            item("sec-progress") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { HomeSection(stringResource(R.string.lavori_progress) + " · " + ip.total, null, true) {} }
                    ViewSwitch(timeView) { v -> timeView = v; runCatching { prefs?.edit()?.putBoolean("time", v)?.apply() } }
                }
            }
            if (oldRelay) item("progress-old") { Expl(stringResource(R.string.lavori_old_relay), CmColors.text) }
            else if (ip.total == 0) item("progress-empty") { Expl(stringResource(R.string.lavori_nothing_progress), CmColors.text) }
            val groups = listOf(
                Triple(ip.working, R.string.lavori_working, CmColors.busy), Triple(ip.you, R.string.lavori_waits_you, CmColors.waiting), Triple(ip.others, R.string.lavori_others, CmColors.text2),
            )
            if (!timeView) groups.forEach { (lines, label, tone) ->
                if (lines.isEmpty()) return@forEach
                item("grp-$label") { GroupHeader(stringResource(label) + " · " + lines.sumOf { it.n }, tone) }
                lines.forEachIndexed { i, l -> item("ln-$label-$i-${lineKey(l)}") { WorkLine(l, today, zone) } }
            } else {
                if (ip.working.isNotEmpty() || ip.you.isNotEmpty()) item("axis") { AxisRow(ip.axis, labelWidth = GANTT_LABEL) }
                groups.forEach { (lines, label, tone) ->
                    if (lines.isEmpty()) return@forEach
                    item("tgrp-$label") { GroupHeader(stringResource(label) + " · " + lines.sumOf { it.n }, tone) }
                    lines.forEachIndexed { i, l -> item("tl-$label-$i-${lineKey(l)}") { GanttRow(l, ip.axis) } }
                }
                if (ip.total > 0) item("time-key") { Text(stringResource(R.string.lavori_time_key), style = MonoSmall.copy(fontSize = 11.sp), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 8.dp)) }
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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun ForYouCard(r: Lavori.ForYou, zone: ZoneId, button: String?, filled: Boolean, onClick: (note: String, choice: String?) -> Unit) {
    var ask by rememberSaveable(r.title) { mutableStateOf(false) }
    var choice by rememberSaveable(r.title) { mutableStateOf<String?>(null) }
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
    // Contratto 1.81: un tasto per opzione quando l'ok chiede una scelta; la domanda intera a tutta larghezza e i tasti interi
    // sotto, che vanno a capo fra loro, come nella home (Franz 24/10 18:35, regola di e8afbe1c).
    val opts = a?.let { x -> it.pixelbox.cmwatch.rules.Approvals.choices(x) }.orEmpty()
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            ag != null -> AgendaMark(ag.scope)
            cm?.paused != null -> Icon(Icons.Rounded.Pause, null, tint = CmColors.text2, modifier = Modifier.size(22.dp))
            else -> Icon(Icons.Rounded.Verified, null, tint = CmColors.advice, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(r.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
            sub?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2) }
        }
        if (opts.isEmpty() && button != null) {
            val click = { if (a != null) ask = true else onClick("", null) }
            if (filled) Button(onClick = click, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)) { Text(button) }
            else FilledTonalButton(onClick = click) { Text(button) }
        }
    }
    if (opts.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(
        Modifier.padding(start = 34.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { opts.forEach { o -> FilledTonalButton(onClick = { choice = o; ask = true }) { Text(o, maxLines = 1, softWrap = false) } } }
    }
    if (ask && a != null) ApproveDialog(a, choice, onConfirm = { n -> ask = false; onClick(n, choice) }, onDismiss = { ask = false })
}

/** La larghezza dell'etichetta nella vista Tempo (mockup: 120 px su 412). */
private val GANTT_LABEL = 120.dp

/** I sette giorni dell'asse, poi «oggi»; una volta sola in testa alla vista Tempo, dopo lo spazio dell'etichetta. */
@Composable
private fun AxisRow(axis: Lavori.Axis, labelWidth: androidx.compose.ui.unit.Dp = 0.dp) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (labelWidth > 0.dp) Spacer(Modifier.width(labelWidth))
        Row(Modifier.weight(1f)) {
            axis.days.forEach { (label, _) -> Text(label, style = MonoSmall.copy(fontSize = 11.sp), modifier = Modifier.weight(1f)) }
            Text(stringResource(R.string.lavori_axis_today), style = MonoSmall.copy(fontSize = 11.sp, color = CmColors.text))
        }
    }
}

/** Elenco | Tempo nella testata di «In corso»: il segmented button di Material 3, la scelta resta. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ViewSwitch(time: Boolean, onChange: (Boolean) -> Unit) {
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = CmColors.surfaceHigh, activeContentColor = CmColors.text, activeBorderColor = CmColors.line,
        inactiveContainerColor = CmColors.bg, inactiveContentColor = CmColors.text2, inactiveBorderColor = CmColors.line,
    )
    SingleChoiceSegmentedButtonRow(Modifier.height(36.dp)) {
        listOf(R.string.lavori_view_list, R.string.lavori_view_time).forEachIndexed { i, label ->
            SegmentedButton(
                selected = time == (i == 1), onClick = { onChange(i == 1) },
                shape = SegmentedButtonDefaults.itemShape(i, 2), colors = colors, modifier = Modifier.handCursor(),
            ) { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Una chiave stabile per una riga di «In corso»: il compito, o la chiave dell'agenda. */
private fun lineKey(l: Lavori.Line): String = l.works.first().row?.c?.task ?: l.agenda?.key.orEmpty()

/** «dalle 20:50» oggi, «dal 18/10» prima. */
@Composable
private fun since(at: Long, today: LocalDate, zone: ZoneId): String {
    val d = Instant.ofEpochSecond(at).atZone(zone).toLocalDate()
    return if (d == today) stringResource(R.string.lavori_since_at, hhmm(at, zone)) else stringResource(R.string.lavori_since_on, dayMonth(d))
}

/**
 * Una riga di «In corso» (mockup 22/10): l'icona del gruppo in un cerchio, il titolo breve su una riga, la riga sotto con la
 * cosa da sapere (per chi aspetta te cosa serve da Franz, per chi lavora sessione e ok, per i fermi su altri il rimando),
 * a destra da quando o fino a quando; «N compiti» per le righe raggruppate. Mai «…»: il titolo breve sta per costruzione,
 * la riga sotto si taglia a due righe.
 */
@Composable
private fun WorkLine(l: Lavori.Line, today: LocalDate, zone: ZoneId) {
    val (icon, tint) = when (l.who) {
        Lavori.Who.WORKING -> Icons.Rounded.PlayArrow to CmColors.busy
        Lavori.Who.YOU -> Icons.Rounded.Person to CmColors.waiting
        Lavori.Who.OTHERS -> Icons.Rounded.HourglassEmpty to CmColors.text2
    }
    val a = l.agenda
    val meta = when (l.who) {
        Lavori.Who.OTHERS -> a?.ref?.takeIf { it.isNotBlank() } ?: a?.detail?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
        Lavori.Who.YOU -> l.reason ?: l.next ?: l.session
        Lavori.Who.WORKING -> listOfNotNull(
            l.session,
            l.reason ?: l.approvedAt?.let { at -> val d = Instant.ofEpochSecond(at).atZone(zone).toLocalDate(); if (d == today) stringResource(R.string.lavori_ok_at, hhmm(at, zone)) else stringResource(R.string.lavori_ok_on, dayMonth(d), hhmm(at, zone)) },
        ).joinToString(" · ").ifBlank { null }
    }
    val right = when (l.who) {
        Lavori.Who.OTHERS -> a?.until?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { stringResource(R.string.lavori_until, dayMonth(it)) }
        else -> l.since?.let { since(it, today, zone) }
    }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(16.dp)).background(CmColors.surfaceLow).padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(l.short, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
                if (l.n > 1) Tag(stringResource(R.string.lavori_n_tasks, l.n), tint)
            }
            meta?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Clip) }
        }
        right?.let { Text(it, style = MonoSmall.copy(fontSize = 12.sp), color = CmColors.text2) }
    }
}

/** Una riga della vista Tempo: l'etichetta breve (con «(N)» se raggruppata) e la barra dei sette giorni; una riga dell'agenda lo dice, senza barra. */
@Composable
private fun GanttRow(l: Lavori.Line, axis: Lavori.Axis) {
    Row(Modifier.fillMaxWidth().height(40.dp).padding(start = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (l.n > 1) "${l.short} (${l.n})" else l.short, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, modifier = Modifier.width(GANTT_LABEL))
        if (l.bars.isNotEmpty()) Box(Modifier.weight(1f)) { LavoriBar(l.bars, axis.now) }
        else Text(stringResource(R.string.lavori_agenda_no_date), style = MonoSmall.copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
    }
}

/** La barra dei sette giorni: pista, tratti pieni (al lavoro) o tratteggiati (in pausa o in coda), la riga di adesso. */
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
                    // Senza prova l'esito in parole, mai il comando del controllo (Franz 24/10 18:36).
                    (if (d.session != null) "· " else "") + (d.proof ?: stringResource(when (d.outcome) { "failed" -> R.string.lavori_proof_failed; "cancelled" -> R.string.lavori_proof_cancelled; else -> R.string.lavori_proof_check })),
                    style = MaterialTheme.typography.bodySmall, color = if (d.proof != null) CmColors.actionIcon else CmColors.text2,
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


