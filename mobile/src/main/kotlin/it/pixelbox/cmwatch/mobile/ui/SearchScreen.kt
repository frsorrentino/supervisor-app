package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.SearchPage
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Badge
import it.pixelbox.cmwatch.rules.ChatSearch
import it.pixelbox.cmwatch.rules.ContextActions
import it.pixelbox.cmwatch.rules.SearchView
import it.pixelbox.cmwatch.rules.Sent
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Cerca (app-cerca-pagina-03-11, mockup del 03/11, scelta «Così» di Franz). Prima di scrivere: le ricerche recenti e i filtri (tutte
 * o una sessione aperta, Oggi o 7 giorni, anche le chiuse). Con il relay (contratto 1.27) cerca nelle conversazioni di tutte le
 * sessioni degli ultimi 7 giorni, partendo da sola quando smetti di scrivere ([onQuery], almeno 2 caratteri); i risultati stanno
 * raggruppati per sessione col passaggio evidenziato, e un tocco apre la conversazione lì ([onOpen] con la voce). Un evento senza
 * sessione apre il Diario. Una sessione chiusa si apre solo se il PC la conosce ancora.
 */
@Composable
fun SearchScreen(
    sent: List<Sent>, events: List<Event>, onOpen: (session: String?, entry: String?) -> Unit, initial: String = "",
    remote: Boolean = false, page: SearchPage? = null, loading: Boolean = false, known: Set<String> = emptySet(),
    onQuery: (String) -> Unit = {}, sessions: List<Session> = emptyList(), now: Long = System.currentTimeMillis() / 1000,
) {
    val ctx = LocalContext.current
    val prefs = remember { runCatching { ctx.getSharedPreferences("search", android.content.Context.MODE_PRIVATE) }.getOrNull() }
    var recent by remember { mutableStateOf(loadRecent(prefs)) }
    fun saveRecent(r: List<SearchView.Recent>) { recent = r; runCatching { prefs?.edit()?.putString("recent", r.joinToString("\n") { "${it.q}\t${it.session.orEmpty()}" })?.apply() } }
    var query by rememberSaveable { mutableStateOf(initial) }
    var session by rememberSaveable { mutableStateOf<String?>(null) }
    var today by rememberSaveable { mutableStateOf(false) }
    var closed by rememberSaveable { mutableStateOf(true) }
    val zone = remember { ZoneId.systemDefault() }
    val local = remember(query, sent, events) { ChatSearch.find(query, sent, events) }
    val hits = remember(local, page, remote) { if (remote && page != null) ChatSearch.withConversations(local, page) else local }
    val groups = remember(hits, session, today, closed, now) { SearchView.groups(hits, SearchView.Filter(session, today, closed), SearchView.dayStart(now, zone)) }
    // Le pillole: le sessioni aperte, senza la master (mockup).
    val open = sessions.filter { it.state != SessionState.GONE && it.name != ContextActions.MASTER }
    fun color(name: String?): Color = sessions.firstOrNull { it.name == name }?.let { s -> Badge.of(s.account, s.color, s.state, s.icon, s.accountKind).fill.takeIf { it != Badge.GREY }?.let(::Color) } ?: V3.Tx2
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // Quando smetti di scrivere: ogni ricerca costa al relay da mezzo secondo a dieci. Partita, la ricerca va fra le recenti.
    LaunchedEffect(query, remote) {
        val q = query.trim()
        if (q.length < 2) return@LaunchedEffect
        kotlinx.coroutines.delay(700)
        if (remote) onQuery(q)
        saveRecent(SearchView.remember(recent, SearchView.Recent(q, session)))
    }
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().imePadding().testTag("cerca")) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp)).background(V3.S2)
                .border(2.dp, V3.Tx2, RoundedCornerShape(26.dp)).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Search, null, tint = V3.Tx2)
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(stringResource(if (remote) R.string.search_hint_all else R.string.search_hint), style = V3.Body.copy(color = V3.Tx3), maxLines = 1, overflow = TextOverflow.Clip)
                BasicTextField(query, { query = it }, singleLine = true, textStyle = V3.Body.copy(color = V3.Tx), cursorBrush = SolidColor(V3.Busy), modifier = Modifier.fillMaxWidth().focusRequester(focus))
            }
            if (query.isNotEmpty()) IconButton(onClick = { query = ""; runCatching { focus.requestFocus() } }) { Icon(Icons.Rounded.Close, stringResource(R.string.search_clear), tint = V3.Tx2) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(stringResource(R.string.search_all), session == null) { session = null }
            open.forEach { s -> Pill(s.name, session == s.name, dot = color(s.name)) { session = if (session == s.name) null else s.name } }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(stringResource(R.string.search_today), today) { today = true }
            Pill(stringResource(R.string.search_week), !today) { today = false }
            Box(Modifier.width(1.dp).height(22.dp).background(V3.S3))
            Pill(stringResource(R.string.search_closed_too), closed, icon = true) { closed = !closed }
        }
        if (loading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), color = Cm.actionIcon)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (query.isBlank()) {
                if (recent.isNotEmpty()) {
                    item(key = "lbl") { Text(stringResource(R.string.search_recent).uppercase(), style = V3.RailText.copy(color = V3.Tx3, fontSize = 13.sp, letterSpacing = 1.sp), modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)) }
                    items(recent, key = { "r-" + it.q }) { r ->
                        Column {
                            if (r != recent.first()) HorizontalDivider(color = V3.Line)
                            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable {
                                session = r.session?.takeIf { n -> open.any { it.name == n } }; query = r.q
                            }.padding(start = 20.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Icon(Icons.Rounded.History, null, tint = V3.Tx2)
                                Text(buildAnnotatedString { append(r.q); r.session?.let { withStyle(SpanStyle(color = V3.Tx3, fontSize = 13.sp)) { append("  $it") } } },
                                    style = V3.Body.copy(color = V3.Tx), maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
                                IconButton(onClick = { saveRecent(recent.filterNot { it == r }) }) { Icon(Icons.Rounded.Close, stringResource(R.string.search_forget, r.q), tint = V3.Tx2) }
                            }
                        }
                    }
                }
                item(key = "help") { Text(stringResource(R.string.search_help), style = V3.Small.copy(color = V3.Tx3), modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) }
            } else {
                if (groups.isEmpty() && !loading) item(key = "none") { Text(stringResource(R.string.search_none), style = V3.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                items(groups, key = { "g-${it.session}-${it.live}" }) { g ->
                    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp).clip(RoundedCornerShape(16.dp)).background(V3.S1)) {
                        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(if (g.session == null) V3.Tx2 else if (g.live == false) V3.Tx3 else color(g.session)))
                            // Il nome intero, anche su due righe, mai tagliato (regole di larghezza di Franz).
                            Text(g.session ?: stringResource(R.string.tab_diary), style = V3.Section.copy(fontSize = 17.sp), maxLines = 2, modifier = Modifier.weight(1f, fill = false))
                            Text(g.hits.size.toString(), style = V3.Meter.copy(color = V3.Tx3, fontSize = 13.sp))
                            Spacer(Modifier.weight(1f))
                            g.live?.let { l -> Text(stringResource(if (l) R.string.search_open_one else R.string.search_closed), style = V3.Meter.copy(color = V3.Tx3, fontSize = 12.sp)) }
                        }
                        g.hits.forEach { h ->
                            val can = h.kind != ChatSearch.Kind.CONVERSATION || h.live == true || h.session in known
                            HorizontalDivider(color = V3.Line)
                            Row(
                                Modifier.fillMaxWidth().then(if (can) Modifier.handCursor().clickable { onOpen(h.session, h.entry) } else Modifier).padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(whenText(SearchView.whenOf(h.at, now, zone)), style = V3.Meter.copy(color = V3.Tx3, fontSize = 12.sp, lineHeight = 20.sp), maxLines = 1, softWrap = false, modifier = Modifier.widthIn(min = 42.dp))
                                Text(
                                    buildAnnotatedString {
                                        append(h.line.substring(0, h.start))
                                        withStyle(SpanStyle(color = V3.Tx, background = Color(0x42FFB23F))) { append(h.line.substring(h.start, h.end)) }
                                        append(h.line.substring(h.end))
                                    },
                                    style = V3.Body.copy(color = V3.Tx2, fontSize = 15.sp, lineHeight = 20.sp), maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f),
                                )
                                if (can) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                if (remote && page?.more == true) item(key = "more") { Text(stringResource(R.string.search_more), style = V3.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                if (groups.isNotEmpty()) item(key = "foot") {
                    val n = groups.sumOf { it.hits.size }
                    Text(
                        listOf(pluralStringResource(R.plurals.search_foot_results, n, n), pluralStringResource(R.plurals.search_foot_sessions, groups.size, groups.size)).joinToString(" ") +
                            " · " + stringResource(if (today) R.string.search_foot_today else R.string.search_foot_week),
                        style = V3.Small.copy(color = V3.Tx3, fontSize = 13.sp), modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, on: Boolean, dot: Color? = null, icon: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).then(if (on) Modifier.background(V3.Tx) else Modifier.border(1.dp, V3.S3, RoundedCornerShape(18.dp)))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        dot?.let { c -> Box(Modifier.size(8.dp).clip(CircleShape).background(c)) }
        if (icon) Icon(Icons.Outlined.Inventory2, null, tint = if (on) V3.Bg else V3.Tx2, modifier = Modifier.size(18.dp))
        Text(text, style = V3.Small.copy(color = if (on) V3.Bg else V3.Tx2, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun whenText(w: SearchView.When): String = when (w) {
    is SearchView.When.Time -> w.hhmm
    SearchView.When.Yesterday -> stringResource(R.string.search_yesterday)
    is SearchView.When.Weekday -> DayOfWeek.of(w.day).getDisplayName(TextStyle.SHORT, Locale.getDefault()).removeSuffix(".")
    is SearchView.When.Date -> w.ddmm
}

private fun loadRecent(p: android.content.SharedPreferences?): List<SearchView.Recent> = runCatching {
    p?.getString("recent", "").orEmpty().lines().filter { it.isNotBlank() }.map { l -> l.split('\t', limit = 2).let { SearchView.Recent(it[0], it.getOrNull(1)?.takeIf { s -> s.isNotBlank() }) } }
}.getOrDefault(emptyList())
