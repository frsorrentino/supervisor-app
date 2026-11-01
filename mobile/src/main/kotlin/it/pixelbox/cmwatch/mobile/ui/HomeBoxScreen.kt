package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.TimelinePage
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Kpi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** I tre box in alto della home, e la pagina che ognuno apre. */
enum class HomeBox { WAITING, BUSY, OUTCOMES }

private val BOX_HM = DateTimeFormatter.ofPattern("HH:mm")
private const val BOX_PAGE = 20
private const val VIEW_PREFS = "home_box_outcomes"
private const val VIEW_BY_TYPE = "by_type"

private fun outcomeLabel(t: Kpi.OutcomeType): Int = when (t) {
    Kpi.OutcomeType.PROBLEM -> R.string.outcome_type_problem
    Kpi.OutcomeType.WAITING -> R.string.outcome_type_waiting
    Kpi.OutcomeType.SHIPPED -> R.string.outcome_type_shipped
    Kpi.OutcomeType.DONE -> R.string.outcome_type_done
    Kpi.OutcomeType.ANSWER -> R.string.outcome_type_answer
    Kpi.OutcomeType.NONE -> R.string.outcome_type_none
    Kpi.OutcomeType.RUNNING -> R.string.outcome_type_running
}

internal fun outcomeIcon(t: Kpi.OutcomeType): ImageVector = when (t) {
    Kpi.OutcomeType.PROBLEM -> Icons.Rounded.Close
    Kpi.OutcomeType.WAITING -> Icons.Rounded.Person
    Kpi.OutcomeType.SHIPPED -> Icons.Rounded.RocketLaunch
    Kpi.OutcomeType.DONE -> Icons.Rounded.Check
    Kpi.OutcomeType.ANSWER -> Icons.Rounded.ChatBubbleOutline
    Kpi.OutcomeType.NONE -> Icons.Rounded.Remove
    Kpi.OutcomeType.RUNNING -> Icons.Rounded.Terminal
}

@Composable
internal fun outcomeTone(t: Kpi.OutcomeType): Color = when (t) {
    Kpi.OutcomeType.PROBLEM -> Cm.gone
    Kpi.OutcomeType.WAITING -> Cm.waiting
    Kpi.OutcomeType.SHIPPED, Kpi.OutcomeType.DONE -> Cm.idle
    Kpi.OutcomeType.RUNNING -> Cm.busy
    Kpi.OutcomeType.ANSWER, Kpi.OutcomeType.NONE -> Cm.text2
}

/**
 * Le pagine dietro i tre box in alto della home (compito app-box-cliccabili-29-10, mockup approvato da Franz il 29/10 alle
 * 18:57): il numero spiegato in una frase, sotto le righe che lo fanno. Un tocco su una riga apre la sessione, se è viva.
 */
@Composable
fun HomeBoxScreen(box: HomeBox, state: State, page: TimelinePage?, now: Long, onBack: () -> Unit, onSession: (String) -> Unit, zone: ZoneId = ZoneId.systemDefault(), startByType: Boolean? = null, embedded: Boolean = false) {
    val live = remember(state) { state.sessions.filter { it.state != SessionState.GONE }.map { it.name }.toSet() }
    val hm = { at: Long -> BOX_HM.format(Instant.ofEpochSecond(at).atZone(zone)) }
    var stillAll by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    val viewPrefs = remember { runCatching { ctx.getSharedPreferences(VIEW_PREFS, android.content.Context.MODE_PRIVATE) }.getOrNull() }
    var byType by rememberSaveable { mutableStateOf(startByType ?: viewPrefs?.getBoolean(VIEW_BY_TYPE, true) ?: true) }
    val groupOpen = remember { mutableStateMapOf<String, Boolean>() }
    val groupShown = remember { mutableStateMapOf<String, Int>() }
    LazyColumn(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().testTag("home-box-page"), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 28.dp)) {
        if (!embedded) item("bar") {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Cm.text) }
                Text(
                    stringResource(when (box) { HomeBox.WAITING -> R.string.box_waiting_title; HomeBox.BUSY -> R.string.box_busy_title; HomeBox.OUTCOMES -> R.string.box_outcomes_title }),
                    style = MaterialTheme.typography.titleLarge, color = Cm.text, modifier = Modifier.weight(1f),
                )
                if (box == HomeBox.OUTCOMES) Text(stringResource(R.string.box_since_midnight), style = MonoSmall, modifier = Modifier.padding(end = 8.dp))
            }
        }
        when (box) {
            HomeBox.WAITING -> {
                val rows = Kpi.waitingRows(state)
                item("hero") { BoxHero(rows.size.toString(), if (rows.isNotEmpty()) Cm.waiting else null, if (rows.isNotEmpty()) stringResource(R.string.box_waiting_head, Durations.since(rows.first().since, now)) else stringResource(R.string.box_waiting_none)) }
                if (rows.isNotEmpty()) {
                    item("rows") {
                        BoxCard {
                            rows.forEachIndexed { i, w ->
                                val kind = stringResource(when (w.kind) { Kpi.WaitKind.QUESTION -> R.string.box_kind_question; Kpi.WaitKind.NEEDS_YOU -> R.string.box_kind_needs_you; Kpi.WaitKind.BLOCKING -> R.string.box_kind_blocking })
                                BoxRow(
                                    i, when (w.kind) { Kpi.WaitKind.QUESTION -> Icons.Rounded.QuestionMark; Kpi.WaitKind.NEEDS_YOU -> Icons.Rounded.Person; Kpi.WaitKind.BLOCKING -> Icons.Rounded.HourglassEmpty }, Cm.waiting,
                                    w.session, stringResource(R.string.queue_age, Durations.since(w.since, now)), onClick = { onSession(w.session) },
                                ) {
                                    Text(if (w.closesItself) "$kind · ${stringResource(R.string.box_closes_itself)}" else kind, style = KindStyle, color = Cm.waiting)
                                    Text(w.text, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                                }
                            }
                        }
                    }
                    item("note") { BoxNote(stringResource(R.string.box_waiting_note)) }
                }
            }
            HomeBox.BUSY -> {
                val rows = Kpi.workingRows(state, now)
                val still = Kpi.stillRows(state)
                item("hero") { BoxHero(rows.size.toString(), if (rows.isNotEmpty()) Cm.busy else null, stringResource(if (rows.isNotEmpty()) R.string.box_busy_head else R.string.box_busy_none)) }
                if (rows.isNotEmpty()) {
                    item("rows") {
                        BoxCard {
                            rows.forEachIndexed { i, w ->
                                BoxRow(i, Icons.Rounded.Terminal, Cm.busy, w.session, w.turnS?.let { stringResource(R.string.queue_age, Durations.since(now - it, now)) }, onClick = { onSession(w.session) }) {
                                    w.goal?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text) }
                                    w.tool?.let { tool ->
                                        val name = tool.substringBefore(' ')
                                        Text(buildAnnotatedString {
                                            withStyle(SpanStyle(color = Cm.busy)) { append(name) }
                                            append(tool.removePrefix(name))
                                            w.toolNote?.let { append(" · $it") }
                                        }, style = MonoSmall)
                                    }
                                    listOfNotNull(w.model, w.context?.let { "ctx $it%" }).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · "), style = MonoSmall) }
                                }
                            }
                        }
                    }
                    item("note") { BoxNote(stringResource(R.string.box_busy_note)) }
                }
                if (still.isNotEmpty()) {
                    item("still-head") { BoxSection(stringResource(R.string.box_still), still.size.toString(), Cm.text2) }
                    item("still") {
                        BoxCard {
                            (if (stillAll) still else still.take(2)).forEachIndexed { i, s ->
                                BoxRow(i, Icons.Rounded.Check, Cm.idle, s.session, s.at?.let(hm), onClick = { onSession(s.session) }) {
                                    s.outcome?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text) }
                                }
                            }
                        }
                    }
                    if (!stillAll && still.size > 2) item("still-more") {
                        BoxMore(pluralStringResource(R.plurals.box_more, still.size - 2, still.size - 2)) { TextButton(onClick = { stillAll = true }) { Text(stringResource(R.string.box_show), color = Cm.actionIcon) } }
                    }
                }
            }
            HomeBox.OUTCOMES -> {
                val out = Kpi.outcomes(page, now, zone)
                item("hero") {
                    val head = when {
                        out == null -> stringResource(R.string.box_outcomes_loading)
                        out.red.isNotEmpty() -> pluralStringResource(R.plurals.box_outcomes_head, out.red.size, out.red.size)
                        else -> stringResource(R.string.box_outcomes_head_green)
                    }
                    BoxHero(out?.settled?.toString() ?: "–", null, head)
                }
                if (out != null) {
                    if (out.red.isNotEmpty()) {
                        item("red-head") { BoxSection(stringResource(R.string.box_red), out.red.size.toString(), Cm.gone) }
                        item("red") {
                            BoxCard {
                                out.red.forEachIndexed { i, r ->
                                    BoxRow(i, Icons.Rounded.Close, Cm.gone, r.session, hm(r.at), onClick = if (r.session in live) ({ onSession(r.session) }) else null) {
                                        val kind = stringResource(R.string.box_red_kind)
                                        Text(r.ref?.let { "$kind · $it" } ?: kind, style = KindStyle, color = Cm.gone)
                                        Text(r.text, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                                    }
                                }
                            }
                        }
                    }
                    if (out.done.isNotEmpty()) {
                        item("view") {
                            Row(foldItem().fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), horizontalArrangement = Arrangement.End) {
                                TwoWaySwitch(!byType, stringResource(R.string.box_view_type), stringResource(R.string.box_view_session)) { bySession -> byType = !bySession; viewPrefs?.edit()?.putBoolean(VIEW_BY_TYPE, byType)?.apply() }
                            }
                        }
                        (if (byType) Kpi.groupsByType(out.done) else Kpi.groupsBySession(out.done)).forEach { g ->
                            val isOpen = groupOpen[g.key] ?: !g.startsClosed
                            item("g-${g.key}") {
                                val tone = g.type?.let { outcomeTone(it) } ?: Cm.text2
                                BoxFold(g.session ?: stringResource(outcomeLabel(g.type!!)).uppercase(), g.rows.size, tone, isOpen, foldItem()) { groupOpen[g.key] = !isOpen }
                            }
                            if (isOpen) {
                                val n = groupShown[g.key] ?: BOX_PAGE
                                itemsIndexed(g.rows.take(n), key = { i, d -> "r-${g.key}-${d.at}-$i" }) { i, d ->
                                    BoxCardPart(first = i == 0, last = i == minOf(n, g.rows.size) - 1, modifier = foldItem()) {
                                        BoxRow(i, outcomeIcon(d.type), outcomeTone(d.type), if (g.session == null) d.session else null, hm(d.at), onClick = if (d.session in live) ({ onSession(d.session) }) else null) {
                                            // Nella vista per sessione il gruppo non dice il tipo: lo dice la riga, se ce l'ha.
                                            if (g.session != null && d.type != Kpi.OutcomeType.NONE) Text(stringResource(outcomeLabel(d.type)).uppercase(), style = KindStyle, color = outcomeTone(d.type))
                                            Text(d.text, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                                        }
                                    }
                                }
                                if (g.rows.size > n) item("more-${g.key}") {
                                    BoxMore(stringResource(R.string.box_rest, g.rows.size - n), foldItem()) {
                                        FilledTonalButton(onClick = { groupShown[g.key] = n + BOX_PAGE }) { Text(stringResource(R.string.box_show_more, minOf(BOX_PAGE, g.rows.size - n))) }
                                    }
                                }
                            }
                        }
                    } else item("none") { BoxNote(stringResource(R.string.box_outcomes_none)) }
                    item("note") { BoxNote(stringResource(R.string.box_outcomes_note) + (if (out.atLeast) " " + stringResource(R.string.box_at_least) else "") + (if (out.cut) " " + stringResource(R.string.box_rows_cut) else ""), foldItem()) }
                }
            }
        }
    }
}

private val KindStyle = MonoSmall.copy(fontSize = 11.5.sp, letterSpacing = 0.9.sp, fontWeight = FontWeight.Medium)

@Composable
private fun BoxHero(value: String, tone: Color?, text: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(value, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Medium), color = tone ?: Cm.text, maxLines = 1)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BoxSection(title: String, count: String, tone: Color) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MonoSmall.copy(letterSpacing = 1.7.sp, fontWeight = FontWeight.Medium), color = tone)
        Box(Modifier.weight(1f).height(1.dp).background(Cm.line))
        Text(count, style = MaterialTheme.typography.bodySmall, color = Cm.text2)
    }
}

/** La testata di un gruppo di «Esiti di oggi»: un tocco lo apre e lo chiude, e dice sempre quanti esiti ha. */
@Composable
private fun BoxFold(title: String, count: Int, tone: Color, open: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    Row(modifier.fillMaxWidth().handCursor().clickable(onClick = onToggle).padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MonoSmall.copy(letterSpacing = 1.7.sp, fontWeight = FontWeight.Medium), color = tone)
        Box(Modifier.weight(1f).height(1.dp).background(Cm.line))
        Text(count.toString(), style = MaterialTheme.typography.bodySmall, color = Cm.text2)
        TurnArrow(open, Cm.text2, Modifier.size(20.dp))
    }
}

@Composable
private fun BoxCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(horizontal = 14.dp, vertical = 4.dp), content = content)
}

/** Un pezzo della scheda per le liste lunghe: gli angoli tondi solo in cima e in fondo, così la lista resta pigra. */
@Composable
private fun BoxCardPart(first: Boolean, last: Boolean, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val r = 20.dp
    Column(
        modifier.fillMaxWidth().padding(bottom = if (last) 8.dp else 0.dp).clip(RoundedCornerShape(if (first) r else 0.dp, if (first) r else 0.dp, if (last) r else 0.dp, if (last) r else 0.dp))
            .background(Cm.surfaceLow).padding(start = 14.dp, end = 14.dp, top = if (first) 4.dp else 0.dp, bottom = if (last) 4.dp else 0.dp),
        content = content,
    )
}

@Composable
private fun BoxRow(index: Int, icon: ImageVector, tone: Color, session: String?, meta: String?, onClick: (() -> Unit)?, body: @Composable ColumnScope.() -> Unit) {
    if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Cm.line))
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tone.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tone, modifier = Modifier.size(16.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (session == null) meta?.let { Text(it, style = MonoSmall, maxLines = 1) }
            else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V3Split(Modifier.weight(1f), text = { Text(session, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text) }) { meta?.let { Text(it, style = MonoSmall, maxLines = 1) } }
            }
            body()
        }
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, tint = Cm.text2, modifier = Modifier.size(18.dp).align(Alignment.CenterVertically))
    }
}

@Composable
private fun BoxNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Cm.text2, modifier = modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp))
}

@Composable
private fun BoxMore(text: String, modifier: Modifier = Modifier, action: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MonoSmall, modifier = Modifier.weight(1f))
        action()
    }
}
