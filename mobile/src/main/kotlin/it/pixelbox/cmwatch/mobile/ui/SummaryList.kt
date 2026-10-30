package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SystemRules
import it.pixelbox.cmwatch.rules.AttentionQueue
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.OutsideSessions
import it.pixelbox.cmwatch.rules.uniqueKeys
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.rules.Lavori
import it.pixelbox.cmwatch.rules.RecapAgenda
import it.pixelbox.cmwatch.rules.AppUpdate
import it.pixelbox.cmwatch.rules.Commitments
import it.pixelbox.cmwatch.mobile.update.AppUpdater

/**
 * Il riepilogo unico (design 03/10, tavola 4): ogni sessione una volta, nei gruppi del bisogno, una riga aperta alla
 * volta; aperta mostra anche i dettagli delle vecchie card di Sessioni. Sotto, «Chiuse · N» e le righe di servizio.
 */
@Composable
fun SummaryList(
    model: Summary.Model, onOpen: (String) -> Unit, onAnswer: (String, Int) -> Unit, onStep: (String, String) -> Unit,
    onService: (MasterHome.Row) -> Unit, onClosed: () -> Unit, initiallyOpen: String? = null,
    /** Il tablet (Franz, 04/10 14:31): nell'altezza in più, sotto, le schede di «Utilizzo e limiti». */
    footer: (@Composable () -> Unit)? = null,
    /** L'ok a un compito (contratto 1.37), dalla sezione Lavori; la nota vuota vale «ok». */
    onApprove: (task: String, note: String, choice: String?) -> Unit = { _, _, _ -> },
    /** Contratto 1.80: il tasto «Fatto» di una ferma nella card (op `attention_done`). */
    onDone: (AttentionQueue.Done) -> Unit = {},
    /** Contratto 1.37: «Chiudi» per le sessioni finite o doppie senza finestra; `canExit` = il PC accetta /exit. */
    canExit: Boolean = false, onClose: (String) -> Unit = {},
    /** Il menu contestuale della card (pressione lunga, Franz 14/10 20:58): segui, fai controllare alla master. */
    onFollow: (String, Boolean) -> Unit = { _, _ -> }, onAskMaster: ((String) -> Unit)? = null,
    now: Long = System.currentTimeMillis() / 1000,
    /** Il riquadro Notte (mockup approvato l'08/10 alle 12:57): il giorno del rapporto, e la pagina quando è arrivata. */
    nightDate: java.time.LocalDate? = null, nightPage: it.pixelbox.cmwatch.rules.NightPage.Page? = null, onNight: () -> Unit = {},
    /** «Utilizzo»: le schede della quota, nella sua sezione richiudibile. */
    usage: (@Composable () -> Unit)? = null,
    /** Le sezioni aperte la prima volta, prima che tu le apra o chiuda. */
    startOpen: Set<String> = setOf("sessions", "suggestions", "lavori", "esiti"),
    /**
     * La sessione appena chiusa dal pannello di chiusura, con la riga «chiusa alle …» (Franz, 08/10 18:59, volo al contrario):
     * la sua card in cima alle sessioni è dove la pagina atterra tornando alla home; poi si richiude da sola.
     */
    justClosed: Pair<it.pixelbox.cmwatch.contract.Session, String>? = null,
    /** La sessione chiusa da una card, finché non esce dallo stato o il PC rifiuta (Franz 24/10 13:20: «nessun feedback»). */
    closing: String? = null,
    /** Contratto 1.83: le domande aperte dei server MCP, in cima; `onElicit` manda la risposta (action, content). */
    elicitations: List<it.pixelbox.cmwatch.contract.Elicitation> = emptyList(),
    onElicit: (it.pixelbox.cmwatch.contract.Elicitation, String, Map<String, String?>) -> Unit = { _, _, _ -> },
    /** Contratto 1.84 (scheda A): i suggerimenti della master, dopo chi ti aspetta; `onSuggestion` = accept | discuss | later | never, o choose con l'opzione (1.88). */
    suggestions: it.pixelbox.cmwatch.rules.Suggestions.Visible? = null,
    onSuggestion: (it.pixelbox.cmwatch.contract.Suggestion, String, String?) -> Unit = { _, _, _ -> },
    /** Contratto 1.84 (B4): l'anteprima di cosa si salva chiudendo le finite, poi la chiusura; null = il relay non le ha. */
    closePreview: (suspend (List<String>) -> it.pixelbox.cmwatch.rules.CloseFinished.Preview?)? = null,
    onCloseMany: (List<String>) -> Unit = {},
    /** La scheda Stanotte con i lavori della coda (mockup approvato l'08/10 alle 20:43); null = la riga di prima. */
    tonight: it.pixelbox.cmwatch.rules.TonightCard.Model? = null,
    /**
     * La sezione «Lavori» (mockup approvato il 22/10): al posto di Da approvare, Impegni e Recap, solo le righe che hanno
     * un tasto (gli ok, le prime due cose da fare fuori dalla chat, la prima che può fare Claude) e la riga «Tutti i lavori».
     * `foryou` null = relay senza impegni e senza agenda: la sezione non c'è.
     */
    foryou: Lavori.ForYouGroups? = null, lavoriLine: String? = null, onLavori: () -> Unit = {},
    onAgendaEdit: (RecapAgenda.Edit) -> Unit = {}, onStart: (it.pixelbox.cmwatch.rules.RecapActions.Action) -> Unit = {},
    onDoneForPerson: (it.pixelbox.cmwatch.contract.Commitment) -> Unit = {},
    today: java.time.LocalDate = java.time.LocalDate.now(),
    /** Sul tablet senza colonne Lavori e Utilizzo stanno nel cruscotto a destra (variante B, 09/10): qui non si ripetono. */
    showLavori: Boolean = true, showUsage: Boolean = true,
    /**
     * «Utilizzo e limiti» dal menu, la riga della quota in alto e l'avviso in una sessione portano qui (Utilizzo unito,
     * 09/10): a ogni nuovo valore la sezione si apre, la lista ci arriva e i pannelli si illuminano un attimo.
     */
    usageFocus: Int = 0,
    /**
     * L'ultimo salto già fatto, tenuto da chi ospita la lista: una lista ricreata (il ritorno da una sessione col volo) non lo
     * rifà. Senza, la home scappava su Utilizzo sotto il volo (Franz, 09/10 18:27: «il volo è peggiorato»).
     */
    usageSeen: Int = 0, onUsageFocused: (Int) -> Unit = {},
    /** ▶ sul dettaglio di «Approfondisci» (09/10 16:31). */
    onSpeak: ((String) -> Unit)? = null,
    /** Dove la lista era arrivata (primo elemento e scarto), tenuto da chi la ospita: ricreata, riparte da lì (09/10 17:23). */
    scrollMemo: IntArray? = null,
    /** Contratto 1.68: il banner della casa della master, le voci di «Serve te» delle altre sessioni; il tocco porta alla prima. */
    attention: List<it.pixelbox.cmwatch.contract.Attention> = emptyList(), onAttention: () -> Unit = {},
    /** P6 (21/10): la card dell'aggiornamento in cima quando una release nuova è pronta; null o senza release = niente. */
    update: AppUpdater.Ui? = null, onInstallUpdate: (AppUpdate.Release) -> Unit = {}, onDismissUpdate: (String) -> Unit = {},
    /** Contratto 1.90, la sezione «Sistema»: null con un relay precedente; `onSystem` null = sola lettura (demo, relay senza l'op). */
    system: it.pixelbox.cmwatch.contract.SystemStatus? = null, ops: List<String>? = null,
    onSystem: ((it.pixelbox.cmwatch.contract.CmdOp, String) -> Unit)? = null,
    /** I KPI 6, 11 e 18 (compito app-kpi-medie-25-10): la cronologia di oggi di tutte le sessioni; null finché non c'è. */
    kpiPage: it.pixelbox.cmwatch.contract.TimelinePage? = null, kpiState: it.pixelbox.cmwatch.contract.State? = null,
    /** I tre box in alto aprono la loro pagina (app-box-cliccabili-29-10). */
    onBox: (HomeBox) -> Unit = {},
    /** La riga della quota nel tabellone apre il quadro (stile «Studio e Diretta», 30/10: era nella testata). */
    onQuota: () -> Unit = {},
    /** Contratto 1.96: le cifre dell'op `kpi` (proposte 10, 12, 13, 17, compito app-kpi-dati-relay-25-10); null finché non arrivano. */
    kpiData: it.pixelbox.cmwatch.contract.KpiData? = null,
    /** Contratto 1.95: la sezione «Plugin e skill»; null = relay senza le op. `pluginsInitial` per i test. */
    plugins: it.pixelbox.cmwatch.contract.PluginsSummary? = null,
    onPluginsLoad: (suspend () -> it.pixelbox.cmwatch.contract.PluginsPage?)? = null,
    onPluginCmd: (suspend (it.pixelbox.cmwatch.contract.CmdOp, String?) -> it.pixelbox.cmwatch.contract.CmdResult?)? = null,
    pluginsInitial: it.pixelbox.cmwatch.contract.PluginsPage? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    var openWork by rememberSaveable { mutableStateOf<String?>(null) }
    // Sezioni richiudibili (Franz, 08/10 12:30): restano come le hai lasciate anche alla prossima apertura. File nuovo col
    // riordino (app-home-riordino-29-10): Utilizzo parte chiusa anche per chi l'aveva aperta, come nella web.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { runCatching { ctx.getSharedPreferences("home_sections_2", android.content.Context.MODE_PRIVATE) }.getOrNull() }
    fun section(key: String) = mutableStateOf(runCatching { prefs?.getBoolean(key, key in startOpen) }.getOrNull() ?: (key in startOpen))
    var sessionsOpen by remember { section("sessions") }
    var usageOpen by remember { section("usage") }
    var esitiOpen by remember { section("esiti") }
    var suggestionsOpen by remember { section("suggestions") }
    var lavoriOpen by remember { section("lavori") }
    var pluginsOpen by remember { section("plugins") }
    var laterOpen by remember { mutableStateOf(false) }
    var sysManual by remember { mutableStateOf<SystemRules.Manual?>(null) }
    // I KPI: per ogni sessione i suoi eventi di oggi, la voce viva prima di una chiusa omonima.
    val kpiEvents = remember(kpiPage) { kpiPage?.sessions?.sortedBy { it.live }?.associate { it.session to it.events } }
    // Contratto 1.84 (B4): le sessioni di «Chiudi le finite» in attesa della conferma, e l'anteprima del PC.
    var closeAsk by remember { mutableStateOf<List<String>?>(null) }
    var preview by remember { mutableStateOf<it.pixelbox.cmwatch.rules.CloseFinished.Preview?>(null) }
    var previewDone by remember { mutableStateOf(false) }
    closeAsk?.let { names ->
        androidx.compose.runtime.LaunchedEffect(names) { previewDone = false; preview = closePreview?.invoke(names); previewDone = true }
        CloseFinishedDialog(preview, previewDone, onDismiss = { closeAsk = null; preview = null }) { ok -> closeAsk = null; preview = null; onCloseMany(ok) }
    }
    fun save(key: String, v: Boolean) { runCatching { prefs?.edit()?.putBoolean(key, v)?.apply() } }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(scrollMemo?.get(0) ?: 0, scrollMemo?.get(1) ?: 0)
    if (scrollMemo != null) androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (i, o) -> scrollMemo[0] = i; scrollMemo[1] = o }
    }
    var usageFlash by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(usageFocus) {
        if (usageFocus == 0 || usageFocus == usageSeen || usage == null || !showUsage) return@LaunchedEffect
        onUsageFocused(usageFocus)
        usageOpen = true; save("usage", true)
        // La sezione può stare sotto lo schermo: dalla cima si scende finché il suo titolo non entra in vista, poi ci si ferma.
        listState.scrollToItem(0)
        repeat(30) {
            androidx.compose.runtime.withFrameNanos { }
            val at = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "sec-usage" }
            if (at != null) { listState.scrollToItem(at.index); usageFlash = true; kotlinx.coroutines.delay(1_200); usageFlash = false; return@LaunchedEffect }
            if (!listState.canScrollForward) return@LaunchedEffect
            listState.scrollBy(listState.layoutInfo.viewportSize.height * 0.8f)
        }
    }
    // Una lista «pigra» con le card riconosciute dal nome della sessione: quando una sessione cambia gruppo (da «Al lavoro» a
    // «Ha finito») la sua card scivola al posto nuovo e le altre si spostano (osservazioni del 03/10, transizione 4).
    val off = animationsOff()
    val moving: @Composable androidx.compose.foundation.lazy.LazyItemScope.() -> Modifier = {
        if (off) Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
        else Modifier.animateItem(
            fadeInSpec = androidx.compose.animation.core.tween(150), fadeOutSpec = androidx.compose.animation.core.tween(150),
            placementSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                visibilityThreshold = androidx.compose.ui.unit.IntOffset.VisibilityThreshold,
            ),
        )
    }
    androidx.compose.foundation.lazy.LazyColumn(
        // In fondo lo spazio del controller della lettura, che si posa sopra la barra della master (09/10 16:50).
        Modifier.fillMaxSize().background(V3.Bg), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp + readingRoom()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (update?.phase?.release != null) item(key = "update-card") { Box(moving()) { UpdateCard(update, now, onInstallUpdate, onDismissUpdate) } }
        // Contratto 1.83: dopo 120 s la domanda torna al terminale, quindi sta sopra a tutto il resto.
        elicitations.forEach { e -> item(key = "elicit-${e.id}") { Box(moving()) { ElicitationCard(e, now, onElicit) } } }
        if (attention.isNotEmpty()) item(key = "attention-banner") {
            Box(moving()) { AttentionBanner(attention) { sessionsOpen = true; save("sessions", true); onAttention() } }
        }
        // Strada B (Franz 23/10 23:08, mockup del 23/10): un lavoro di una sessione aperta sta nella sua scheda, come gli ok;
        // in Lavori restano quelli senza sessione, in una lista compatta.
        val placed = foryou?.let { Lavori.place(it, model.rows.map { r -> r.session.name }.toSet(), Summary.attachedTasks(model.rows)) }
        // Franz 24/10 00:46: un suggerimento che riguarda una sessione aperta (il contesto alto) sta nella sua scheda; in Altro restano gli altri.
        val outsidePlaced = OutsideSessions.place(OutsideSessions.groups(model.service, model.closed), model.rows.map { r -> r.session.name }.toSet())
        // Proposta 7 (compito app-kpi-piccole-25-10): tre cifre in testa alla home, con sotto la quota (stile «Studio e Diretta», 30/10).
        kpiState?.let { st ->
            item(key = "kpi-7") { Box(moving()) { Kpi7(it.pixelbox.cmwatch.rules.Kpi.head(st, kpiPage, now, java.time.ZoneId.systemDefault()), st, now, onBox, onQuota) } }
        }
        // L'ordine (Franz 29/10 20:48, app-home-riordino-29-10): chi aspetta te, le sessioni, i lavori, gli esiti di oggi; poi «Altro».
        val waitRows = model.rows.filter { it.group == Summary.Group.WAITING }
        val restRows = model.rows.filter { it.group != Summary.Group.WAITING }
        val looseOk = placed?.loose?.ok.orEmpty()
        val zone = java.time.ZoneId.systemDefault()
        // Un solo bottone pieno: «Fatto» della prima che ti aspetta, se la richiesta non si chiude da sola (correzione 2 del 30/10).
        val primary = waitRows.firstOrNull()?.session?.name
        val lastRed = it.pixelbox.cmwatch.rules.Kpi.outcomes(kpiPage, now, zone)?.red.orEmpty().sortedBy { d -> d.at }.associate { d -> d.session to d.at }
        val card: @Composable (Summary.Row, Boolean) -> Unit = { r, first ->
            val s = r.session
            val id = r.group.name + ":" + s.name
            SummaryCard(
                r, expanded == id, onToggle = { expanded = if (expanded == id) null else id },
                onAnswer = { n -> onAnswer(s.name, n) }, onStep = { t -> onStep(s.name, t) }, onOpen = { onOpen(s.name) },
                firstFilled = false, canExit = canExit, onClose = { onClose(s.name) }, closing = closing == s.name, now = now,
                onFollow = { f -> onFollow(s.name, f) }, onAskMaster = onAskMaster?.let { ask -> { ask(s.name) } },
                onApprove = onApprove, onDone = onDone,
                works = placed?.inCards?.get(s.name).orEmpty(), services = outsidePlaced.inCards[s.name].orEmpty(), onService = onService,
                kpiEvents = kpiEvents?.let { m -> m[s.name].orEmpty() },
                kpiTurns = kpiData?.let { d -> it.pixelbox.cmwatch.rules.Kpi.turnsOf(d, s.name) },
                todayShare = kpiState?.let { st -> it.pixelbox.cmwatch.rules.Kpi.todayShare(st, s.name) },
                onWorkDone = { w -> w.agenda?.let { row -> onAgendaEdit(RecapAgenda.Edit(RecapAgenda.Item.DONE, row)) }; w.commitment?.let(onDoneForPerson) },
                onWorkStart = { w -> w.agenda?.let { row -> onStart(RecapAgenda.doIt(row)) } },
                doneFilled = s.name == primary && s.needsYou?.until != true, lastRed = lastRed[s.name], firstInList = first,
            )
        }
        if (waitRows.isNotEmpty() || looseOk.isNotEmpty()) {
            item(key = "sec-waiting") { Box(moving()) { V3Section(stringResource(R.string.home_waiting), androidx.compose.material.icons.Icons.Outlined.PanTool, (waitRows.size + looseOk.size).toString(), tag = "home-sez-aspettano") } }
            // Chiave dall'id, mai dal nome: due sessioni con lo stesso nome chiudevano l'app (15/10 23:41).
            val keys = uniqueKeys(waitRows) { "s-" + it.session.id }
            items(waitRows.size, key = { keys[it] }) { i -> Box(moving()) { card(waitRows[i], true) } }
            looseOk.forEachIndexed { i, r ->
                val k = r.approval?.task ?: r.commitment?.task ?: r.agenda?.key?.takeIf { it.isNotBlank() } ?: "${r.kind}-$i"
                item(key = "lv-$k") { Box(moving()) { WorkCard(r, openWork == k, { openWork = if (openWork == k) null else k }, onApprove, onDoneForPerson, onAgendaEdit, onStart) } }
            }
        }
        val outside = outsidePlaced.groups.filter { g -> g.category != OutsideSessions.Category.CLOSED }
        val closedGroup = outsidePlaced.groups.firstOrNull { g -> g.category == OutsideSessions.Category.CLOSED }
        // Contratto 1.84 (scheda A): i suggerimenti dopo le sessioni (Franz 30/10 16:44), fuori dal numero di chi aspetta: un consiglio non blocca nessuno.
        val sug = suggestions?.takeIf { it.shown.isNotEmpty() }
        val sugAt = sug?.let { java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(java.time.Instant.ofEpochSecond(it.shown.first().at).atZone(zone)) }
        val renderSessions: () -> Unit = {
        if (restRows.isNotEmpty() || justClosed != null) {
            item(key = "sec-sessions") {
                val n = { g: Summary.Group -> restRows.count { it.group == g } }
                Box(moving()) { V3Section(stringResource(R.string.home_sec_sessions), androidx.compose.material.icons.Icons.Outlined.PlayCircle, restRows.size.toString(), stringResource(R.string.home_sessions_line, n(Summary.Group.WORKING), n(Summary.Group.FINISHED), n(Summary.Group.STILL)), "home-sez-sessioni", sessionsOpen) { sessionsOpen = !sessionsOpen; save("sessions", sessionsOpen) } }
            }
            justClosed?.let { (js, line) -> item(key = "just-closed") { Box(moving()) { JustClosedCard(js, line) } } }
            // Una lista sola nell'ordine dei gruppi: il glifo dice lo stato al posto delle intestazioni.
            if (sessionsOpen && restRows.isNotEmpty()) item(key = "sessions-list") {
                val fin = if (closePreview != null) it.pixelbox.cmwatch.rules.CloseFinished.candidates(model.rows, now) else emptyList()
                Column(moving(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    V3List { restRows.forEachIndexed { i, r -> androidx.compose.runtime.key(r.session.id, i) { Box(if (V3.day && r.group == Summary.Group.WAITING) Modifier.background(V3.WaitFill) else Modifier) { card(r, i == 0) } } } }
                    if (fin.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { V3Pill(stringResource(R.string.close_finished_n, fin.size)) { closeAsk = fin.map { x -> x.name } } }
                }
            }
        }
        }
        val renderSuggestions: () -> Unit = {
            if (sug != null) {
                // Una sezione come le altre (Franz 30/10 17:33): titolo, numero, freccina; da chiusa resta il numero, la barra dei 7 giorni sta dentro.
                item(key = "sec-suggestions") { Box(moving()) { V3Section(stringResource(R.string.sug_title), androidx.compose.material.icons.Icons.Outlined.AutoAwesome, sug.total.toString(), sugAt.takeIf { suggestionsOpen }, "home-sez-suggerimenti", suggestionsOpen) { suggestionsOpen = !suggestionsOpen; save("suggestions", suggestionsOpen) } } }
                if (suggestionsOpen) item(key = "suggestions") { Column(moving(), verticalArrangement = Arrangement.spacedBy(8.dp)) { SuggestionsCard(sug, onSuggestion); kpiData?.suggestions7d?.let { Kpi13(it) } } }
            }
        }
        val renderLavori: () -> Unit = {
        if (showLavori && (foryou != null || outside.isNotEmpty())) {
            val laterRows = placed?.loose?.let { it.outside + it.claude }.orEmpty()
            item(key = "sec-lavori") { Box(moving()) { V3Section(stringResource(R.string.lavori_title), androidx.compose.material.icons.Icons.Outlined.Checklist, right = if (lavoriOpen) null else lavoriLine, tag = "home-sez-lavori", open = lavoriOpen, onToggle = { lavoriOpen = !lavoriOpen; save("lavori", lavoriOpen) }) } }
            if (lavoriOpen) {
                if (laterRows.isNotEmpty()) item(key = "lv-later") {
                    Box(moving()) {
                        V3List { V3Row(first = true, onClick = { laterOpen = !laterOpen }, rail = {}, trailing = { Icon(if (laterOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx3) }) { Text(stringResource(R.string.lavori_later, laterRows.size), style = V3.Name) } }
                    }
                }
                if (laterOpen) laterRows.forEachIndexed { i, r ->
                    val k = r.commitment?.task ?: r.agenda?.key?.takeIf { it.isNotBlank() } ?: "${r.kind}-$i"
                    item(key = "lv-$k") { Box(moving()) { WorkCard(r, openWork == k, { openWork = if (openWork == k) null else k }, onApprove, onDoneForPerson, onAgendaEdit, onStart) } }
                }
                outside.filter { g -> g.category == OutsideSessions.Category.NIGHT && tonight?.items?.isNotEmpty() == true }.forEach { g ->
                    item(key = "out-${g.category}-card") { Box(moving()) { OutsideCard(g.rows, onService, tonight, nightPage, onNight, g.category) } }
                }
                item(key = "lv-list") {
                    Box(moving()) {
                        V3List {
                            var first = true
                            outside.forEach { g ->
                                g.rows.forEach { row ->
                                    if (g.category == OutsideSessions.Category.NIGHT && row.kind == MasterHome.Kind.NIGHT) {
                                        // La Notte su una riga: la coda di stanotte e com'è andata la notte scorsa; il tocco apre la pagina.
                                        if (tonight?.items?.isNotEmpty() != true) V3Row(first, onNight, { Icon(Icons.Rounded.Bedtime, null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }, { V3Pill(stringResource(R.string.fy_btn_add)) { onService(row) } }) {
                                            Text(stringResource(R.string.home_sec_night), style = V3.Name)
                                            Text(nightPage?.let { p -> stringResource(R.string.home_night_row, p.counts.done, p.cards.size) } ?: stringResource(R.string.fy_night_empty_short), style = V3.Small)
                                        }
                                    } else {
                                        val (title, detail, action) = serviceText(row)
                                        V3Row(first, null, { Icon(categoryIcon(g.category), null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }, { V3Pill(stringResource(action)) { onService(row) } }) {
                                            Text(stringResource(categoryLabel(g.category)), style = V3.Meter)
                                            Text(if (row.kind == MasterHome.Kind.CONTEXT) row.title else title, style = V3.Name)
                                            detail?.let { d -> Text(d, style = V3.Small, maxLines = 2, overflow = TextOverflow.Clip) }
                                        }
                                    }
                                    first = false
                                }
                            }
                            if (foryou != null) V3Row(first, onLavori, { Icon(androidx.compose.material.icons.Icons.Outlined.EventNote, null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }, { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3) }) {
                                Text(lavoriLine ?: stringResource(R.string.lavori_all), style = V3.Name)
                            }
                        }
                    }
                }
            }
        }
        }
        val renderEsiti: () -> Unit = {
        // Gli esiti di oggi dalla cronologia: i rossi in cima, poi «Tutti gli esiti» apre la pagina del terzo box.
        it.pixelbox.cmwatch.rules.Kpi.outcomes(kpiPage, now, zone)?.let { out ->
            val liveNames = kpiState?.sessions.orEmpty().filter { x -> x.state != it.pixelbox.cmwatch.contract.SessionState.GONE }.map { x -> x.name }.toSet()
            item(key = "sec-esiti") { Box(moving()) { V3Section(stringResource(R.string.box_outcomes_title), androidx.compose.material.icons.Icons.Outlined.SportsScore, tag = "home-sez-esiti", open = esitiOpen, onToggle = { esitiOpen = !esitiOpen; save("esiti", esitiOpen) }) } }
            if (esitiOpen) item(key = "esiti") {
                Box(moving()) {
                    V3List {
                        val shown = if (out.red.isNotEmpty()) out.red.take(3) else out.done.take(2)
                        if (shown.isEmpty()) V3Row(true, null, {}) { Text(stringResource(R.string.box_outcomes_none), style = V3.Small) }
                        shown.forEachIndexed { i, d ->
                            val red = d.ref != null
                            V3Row(i == 0, { if (d.session in liveNames) onOpen(d.session) else onBox(HomeBox.OUTCOMES) }, {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    StateGlyph(if (red) it.pixelbox.cmwatch.rules.Rail.Glyph.RED else it.pixelbox.cmwatch.rules.Rail.Glyph.DONE)
                                    Text(hhmmOf(d.at), style = V3.RailText.copy(color = if (red) V3.Red else V3.Tx2))
                                }
                            }, fill = if (V3.day && red) V3.RedFill else androidx.compose.ui.graphics.Color.Transparent) {
                                Text(d.session, style = V3.Name)
                                Text(d.text, style = V3.Small, maxLines = 2, overflow = TextOverflow.Clip)
                            }
                        }
                        V3Row(shown.isEmpty(), { onBox(HomeBox.OUTCOMES) }, { StateGlyph(it.pixelbox.cmwatch.rules.Rail.Glyph.DONE) }, {
                            Text(out.done.size.toString(), style = V3.Meter, modifier = Modifier.padding(end = 4.dp))
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3)
                        }) { Text(stringResource(R.string.home_all_outcomes), style = V3.Name) }
                    }
                }
            }
        }
        }
        val renderAltro: () -> Unit = {
        // «Altro»: Utilizzo, Sistema, Plugin e skill e Chiuse come righe di una lista, col numero in vista; il tocco apre sul posto.
        val hasUsage = usage != null && showUsage
        val hasPlugins = plugins != null && onPluginsLoad != null && onPluginCmd != null
        if (hasUsage || system != null || hasPlugins || closedGroup != null) {
            // Senza testata (Franz 30/10 21:05): il margine lo tiene staccato dalla sezione di prima.
            item(key = "sec-usage") {
                val rowIcon = @Composable { v: androidx.compose.ui.graphics.vector.ImageVector -> Icon(v, null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }
                val toggle = @Composable { o: Boolean -> Icon(if (o) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx3) }
                Box(moving().padding(top = 14.dp)) {
                    V3List {
                        var first = true
                        if (hasUsage) {
                            val weeks = kpiState?.quota.orEmpty().filter { (_, q) -> !q.stale && q.w7 != null }.entries
                            val line = weeks.singleOrNull()?.let { (_, q) -> stringResource(R.string.home_usage_week, q.w7!!) } ?: weeks.joinToString(" · ") { (a, q) -> "$a ${q.w7}%" }
                            V3Row(first, { usageOpen = !usageOpen; save("usage", usageOpen) }, { rowIcon(androidx.compose.material.icons.Icons.Outlined.DataUsage) }, {
                                toggle(usageOpen)
                            }, meter = { if (!usageOpen && line.isNotEmpty()) Text(line, style = V3.Meter, textAlign = androidx.compose.ui.text.style.TextAlign.End) }) { Text(stringResource(R.string.home_sec_usage), style = V3.Name) }
                            first = false
                            if (usageOpen) {
                                usage!!()
                            }
                        }
                        // Contratto 1.90 (Franz 28/10 13:19): compatta in verde, aperta da sola in giallo e in rosso; il tocco vale finché il livello non cambia.
                        system?.let { sys ->
                            val sysOpen = SystemRules.isOpen(sys.level, sysManual)
                            V3Row(first, { sysManual = SystemRules.Manual(sys.level, !sysOpen) }, { rowIcon(androidx.compose.material.icons.Icons.Outlined.Memory) }, {
                                toggle(sysOpen)
                            }, meter = {
                                if (!sysOpen) {
                                    Box(Modifier.padding(end = 6.dp).size(7.dp).clip(CircleShape).background(systemTone(sys.level)))
                                    Text(systemSummary(sys), style = V3.Meter, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                            }) { Text(stringResource(R.string.home_sec_system), style = V3.Name) }
                            first = false
                            if (sysOpen) Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { SystemCard(sys, ops, onSystem); kpiData?.health?.let { Kpi12(it) } }
                        }
                        // Contratto 1.95: «Plugin e skill», chiusa col numero degli aggiornamenti; l'elenco si chiede all'apertura.
                        if (hasPlugins) {
                            val n = plugins!!.accounts.sumOf { it.updates.size }
                            V3Row(first, { pluginsOpen = !pluginsOpen; save("plugins", pluginsOpen) }, { rowIcon(androidx.compose.material.icons.Icons.Outlined.Extension) }, {
                                toggle(pluginsOpen)
                            }, meter = { if (!pluginsOpen && n > 0) Text(pluralStringResource(R.plurals.plugins_to_update, n, n), style = V3.Meter, textAlign = androidx.compose.ui.text.style.TextAlign.End) }) { Text(stringResource(R.string.home_sec_plugins), style = V3.Name) }
                            first = false
                            if (pluginsOpen) Box(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) { PluginsCard(plugins, onPluginsLoad!!, onPluginCmd!!, pluginsInitial) }
                        }
                        closedGroup?.let { g ->
                            V3Row(first, onClosed, { rowIcon(Icons.Rounded.Inventory2) }, {
                                Text(g.closed.size.toString(), style = V3.Meter, modifier = Modifier.padding(end = 4.dp))
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3)
                            }) { Text(stringResource(R.string.cat_closed), style = V3.Name) }
                        }
                    }
                }
            }
        }
        }
        // L'ordine scelto da Franz (Impostazioni › Riordina la home); tabellone e «Ti aspettano» restano fissi qui sopra.
        it.pixelbox.cmwatch.rules.HomeOrder.normalize(kpiState?.homeOrder).forEach { k ->
            when (k) {
                "sessions" -> renderSessions()
                "suggestions" -> renderSuggestions()
                "lavori" -> renderLavori()
                "esiti" -> renderEsiti()
                "altro" -> renderAltro()
            }
        }
        footer?.let { f -> item(key = "footer") { f() } }
    }
}

internal fun groupLabel(g: Summary.Group) = when (g) {
    Summary.Group.WAITING -> R.plurals.summary_waiting
    Summary.Group.FINISHED -> R.plurals.summary_finished
    Summary.Group.WORKING -> R.plurals.summary_working
    Summary.Group.STILL -> R.plurals.summary_still
}

internal fun groupTone(g: Summary.Group): Color = when (g) {
    Summary.Group.WAITING -> Cm.briefWarn
    Summary.Group.FINISHED -> Cm.briefGood
    Summary.Group.WORKING -> Cm.briefRing
    Summary.Group.STILL -> Cm.text2
}

/** Priorità e «modello · effort · account» (il contesto sta in testa alla card, l'obiettivo sopra il testo). */
@Composable
private fun details(s: Session): List<String> = listOfNotNull(
    SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered)),
    listOfNotNull(ModelText.short(s.model), s.effort, s.account).joinToString(" · ").ifEmpty { null },
)

/**
 * Una sessione del riepilogo (Franz, 03/10 15:18, variante B): una card con il badge (colore della sessione, cerchio o
 * quadrato dell'account, glifo dello stato), nome, contesto e quota delle 5 ore; sotto l'ultimo esito o la domanda su due righe e la
 * barretta del contesto. Aperta: il testo intero, i dettagli, le opzioni o i consigli e la conversazione.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SummaryCard(
    r: Summary.Row, open: Boolean, onToggle: () -> Unit,
    onAnswer: (Int) -> Unit, onStep: (String) -> Unit, onOpen: () -> Unit,
    firstFilled: Boolean = true, canExit: Boolean = false, onClose: () -> Unit = {}, closing: Boolean = false, now: Long = System.currentTimeMillis() / 1000,
    onFollow: (Boolean) -> Unit = {}, onAskMaster: (() -> Unit)? = null,
    /** L'ok a un compito del registro che questa sessione chiede (Franz 23/10: «Approva» nella scheda della sessione). */
    onApprove: (task: String, note: String, choice: String?) -> Unit = { _, _, _ -> },
    /** Contratto 1.80: il tasto «Fatto» di una ferma (op `attention_done`). */
    onDone: (AttentionQueue.Done) -> Unit = {},
    /** Strada B (23/10): i lavori di questa sessione che aspettano te, ognuno col suo tasto (Fatto o Avvia). */
    works: List<Lavori.ForYou> = emptyList(),
    onWorkDone: (Lavori.ForYou) -> Unit = {}, onWorkStart: (Lavori.ForYou) -> Unit = {},
    /** Franz 24/10 00:46: i suggerimenti su questa sessione (il contesto alto), ognuno col suo tasto tonale. */
    services: List<MasterHome.Row> = emptyList(), onService: (MasterHome.Row) -> Unit = {},
    /** I KPI 6 e 11: gli eventi di oggi della sessione; null = cronologia non ancora arrivata (niente grafici). */
    kpiEvents: List<it.pixelbox.cmwatch.contract.TimelineEvent>? = null,
    /** Proposta 10 (1.96): i turni di oggi della sessione col costo; null = op `kpi` non ancora arrivata. */
    kpiTurns: List<it.pixelbox.cmwatch.contract.KpiTurn>? = null,
    /** Proposta 5: la parte di token di oggi della sessione sul suo account; null senza dati. */
    todayShare: Int? = null,
    /** Stile «Studio e Diretta»: il «Fatto» è pieno solo per la prima che aspetta; l'ora dell'ultimo rosso di oggi; `firstInList` senza il filo sopra. */
    doneFilled: Boolean = false, lastRed: Long? = null, firstInList: Boolean = true,
) {
    val s = r.session
    // La card si segna sullo schermo finché è composta (transizione C): aperta da qui, la sessione vola da lei.
    val onScreen = LocalCardsOnScreen.current
    androidx.compose.runtime.DisposableEffect(onScreen, s.name) {
        onScreen?.let { m -> m[s.name] = (m[s.name] ?: 0) + 1 }
        onDispose { onScreen?.let { m -> val n = (m[s.name] ?: 1) - 1; if (n <= 0) m.remove(s.name) else m[s.name] = n } }
    }
    var closeAsk by rememberSaveable(s.name) { mutableStateOf(false) }
    // Il menu contestuale della sessione sulla card (Franz, 14/10 20:58: «il menu contestuale di una sessione non lo vedo»):
    // pressione lunga o tasto destro; «Chiudi» chiede conferma nella card, come la chiusura del riordino.
    var menu by remember { mutableStateOf(false) }
    var menuClose by rememberSaveable(s.name) { mutableStateOf(false) }
    // Il tasto di un passo toccato resta segnato «mandato» finché la sessione non cambia esito (Franz, 06/10 12:32).
    var sentSteps by rememberSaveable(s.name, s.outcome?.at) { mutableStateOf(listOf<String>()) }
    val waiting = r.group == Summary.Group.WAITING
    val parsed = androidx.compose.runtime.remember(r.text) { it.pixelbox.cmwatch.rules.NextSteps.parse(r.text.orEmpty()) }
    // L'esito per primo e senza l'etichetta «Esito:»; la domanda così com'è.
    val shown = if (waiting) parsed.text else s.outcome?.let { o -> it.pixelbox.cmwatch.rules.OutcomeText.summary(o) } ?: parsed.text
    val body = it.pixelbox.cmwatch.rules.Markdown.parse(shown).text.trim()
    val glyph = it.pixelbox.cmwatch.rules.Rail.glyph(s, r.group)
    val sinceWait = it.pixelbox.cmwatch.rules.Kpi.waitingSince(s)
    val railText = when {
        glyph == it.pixelbox.cmwatch.rules.Rail.Glyph.WAITING && sinceWait != null -> railSpan(sinceWait, now)
        glyph == it.pixelbox.cmwatch.rules.Rail.Glyph.BUSY && s.turnStarted != null -> railSpan(s.turnStarted!!, now)
        glyph == it.pixelbox.cmwatch.rules.Rail.Glyph.DONE && s.outcome?.at != null -> hhmmOf(s.outcome!!.at)
        glyph == it.pixelbox.cmwatch.rules.Rail.Glyph.STILL -> hhmmOf(s.since)
        else -> null
    }
    val square = !it.pixelbox.cmwatch.rules.Accounts.personal(s.account, s.accountKind)
    if (!waiting && !firstInList) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
    Row(
        // La chat si restringe verso la sua card durante il gesto indietro.
        Modifier.fly("card-${s.id}").fillMaxWidth().then(if (waiting) Modifier.clip(V3.Shape).background(if (V3.day) V3.WaitFill else V3.S2).border(1.dp, if (V3.day) V3.WaitLine else androidx.compose.ui.graphics.Color.Transparent, V3.Shape) else Modifier).smoothSize()
            // Franz, 03/10 17:05: il tocco sulla card porta dritto alla sessione; il tasto ▼ la apre sul posto.
            .handCursor().combinedClickable(onClick = onOpen, onLongClick = { menu = true }).contextClick { menu = true }
            .padding(end = 10.dp, top = 8.dp, bottom = 10.dp),
    ) {
    // Il binario: il glifo dello stato e, sotto, da quanto aspetta o lavora, o l'ora.
    Column(Modifier.width(V3.Rail).padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        StateGlyph(glyph, s.context, square = square)
        railText?.let { x -> Text(x, style = V3.RailText.copy(color = if (glyph == it.pixelbox.cmwatch.rules.Rail.Glyph.STILL) V3.Tx2 else V3.tone(glyph)), maxLines = 1) }
    }
    Spacer(Modifier.width(8.dp))
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Il segno della modalità dei permessi dopo il nome, solo se diverso dal solito (mockup del 14/10, schermata 2).
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    s.name, style = V3.Name, modifier = Modifier.weight(1f, fill = false),
                )
                PermissionGlyph(it.pixelbox.cmwatch.rules.PermissionModes.homeGlyph(s), 14.dp)
            }
            if (s.duplicateOf != null) Tag(stringResource(R.string.cleanup_dup_tag), Cm.briefWarn)
            // «ctx 17%»: il contesto occupato, non un avanzamento del lavoro (osservazioni del 03/10).
            // La quota delle 5 ore dell'account al posto dell'età della sessione (Franz, 03/10 20:31: «non è un'informazione
            // rilevante»); il dato vecchio nel colore dell'attesa, come nel Quadro.
            // La quota 5h sta una volta sola, nel tabellone; qui il contesto e l'ora dell'ultimo rosso di oggi.
            if (s.context != null && (waiting || r.group == Summary.Group.WORKING)) Text(stringResource(R.string.ctx_short, s.context!!), style = V3.Meter, maxLines = 1)
            if (!waiting && lastRed != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StateGlyph(it.pixelbox.cmwatch.rules.Rail.Glyph.RED, size = 14.dp)
                Text(hhmmOf(lastRed), style = V3.Meter.copy(color = V3.Red), maxLines = 1)
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                androidx.compose.material3.Icon(
                    if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    stringResource(if (open) R.string.steps_close else R.string.steps_open), tint = Cm.text2,
                )
            }
        }
        // L'obiettivo della sessione, stabile, sopra l'attività del momento (osservazioni del 03/10): si legge anche dopo ore.
        SessionsText.goalLine(s, stringResource(R.string.goal))?.let { g ->
            Text(g, style = V3.Small, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
        }
        // Franz 24/10 19:46 (B2): l'esito di una ferma più vecchio di 6 ore si spegne e dice da quante ore.
        val old = Summary.staleHours(r, now)
        // Chiusa, una sessione al lavoro è una riga: lo strumento in mono (app-home-riordino-29-10).
        val toolLine = s.tool?.takeIf { r.group == Summary.Group.WORKING && !open }
        if (toolLine != null) Text(toolLine, style = V3.Meter, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
        else if (body.isNotBlank()) Text(
            if (old == null) androidx.compose.ui.text.AnnotatedString(body) else androidx.compose.ui.text.buildAnnotatedString {
                append(body); append("  ")
                withStyle(androidx.compose.ui.text.SpanStyle(color = Cm.briefWarn, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal, fontFamily = V3.Mono, fontSize = 11.sp)) { append(stringResource(R.string.stale_ago, old)) }
            },
            style = (if (waiting || open) V3.Body else V3.Small).copy(fontStyle = if (old != null) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal),
            color = if (waiting || open) V3.Tx else if (old != null) V3.Tx2.copy(alpha = 0.75f) else V3.Tx2,
            maxLines = if (open) Int.MAX_VALUE else if (waiting) 2 else 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        )
        // Contratto 1.84: la richiesta di un'altra sessione per un compito di questa dice chi la fa.
        if (waiting) Summary.askedFor(s)?.let { a ->
            Text(if (a.task != null) stringResource(R.string.asked_for_task, a.from, a.task!!) else stringResource(R.string.asked_for, a.from), style = MonoSmall)
        }
        if (open) {
            details(s).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = Cm.text2, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip) }
            // Proposta 11: la giornata a fasce nella card aperta; «ti aspetta» solo per il tratto in corso, dallo stato.
            kpiEvents?.let { ev ->
                val from = it.pixelbox.cmwatch.rules.Kpi.midnight(now, java.time.ZoneId.systemDefault())
                Kpi11(it.pixelbox.cmwatch.rules.Kpi.day(ev, from, now, if (s.state == it.pixelbox.cmwatch.contract.SessionState.GONE) null else it.pixelbox.cmwatch.rules.Kpi.waitingSince(s)), from, now)
            }
            // Proposta 10: la striscia dei turni di oggi col costo, sotto la giornata a fasce.
            kpiTurns?.takeIf { t -> t.isNotEmpty() }?.let { t -> Kpi10(t) }
        }
        // Franz 23/10 19:34: gli ok del registro che questa sessione chiede, ognuno col suo Approva, qui e non in schede a parte.
        r.approvals.forEach { a -> ApprovalLine(a, onApprove) }
        // Strada B (Franz 23/10 23:08): i lavori di questa sessione che aspettano te, qui come gli ok.
        works.forEach { w -> WorkLine(w, onDone = { onWorkDone(w) }, onStart = { onWorkStart(w) }) }
        services.forEach { row -> ServiceLine(row) { onService(row) } }
        // Contratto 1.80: una ferma si chiude da qui col tasto «Fatto»; la scadenza di un needs-you accanto.
        if (waiting && s.question == null) AttentionQueue.doneOf(s)?.let { d ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.needsYou?.expiresAt?.let { expiryText(it, now) } ?: "", style = MonoSmall, color = Cm.text2, modifier = Modifier.weight(1f))
                V3Pill(stringResource(R.string.attention_done), filled = doneFilled) { onDone(d) }
            }
        }
        // Le opzioni della domanda subito, anche a card chiusa: rispondere è il motivo per cui la card è in cima.
        if (waiting) s.question?.let { q ->
            var holdHint by rememberSaveable(q.id) { mutableStateOf(false) }
            // Sul fondo della card il tonale di sempre quasi spariva (provini 03/10): un velo del colore primario.
            QuestionOptions(q, firstFilled = firstFilled, holdHint = holdHint, onHold = { holdHint = true }, onAnswer = onAnswer, tonal = if (V3.day) V3.S1 else V3.S3, v3 = true)
        }
        // I consigli come tasti, sempre visibili (Franz, 03/10 17:27): il tocco li manda subito a quella sessione, che li
        // prende a fine turno se sta lavorando.
        if (!waiting && parsed.steps.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Contratto 1.38, variante 2: chi sblocca ha il filo ambra e il lucchetto (dal «!» o da `next_steps`).
            val blocking = parsed.blocking + s.nextSteps.orEmpty().filter { it.blocking }.map { it.text }
            parsed.steps.forEach { step -> StepChip(step, step in blocking, sent = step in sentSteps) { sentSteps = sentSteps + step; onStep(step) } }
        }
        // Contratto 1.37: compito chiuso o doppione, a sessione ferma; «Chiudi» solo senza finestra. Chi ha finito resta qui e
        // dice di essere aperta; la conferma sta nella card, senza finestre (Franz, 06/10 10:57-12:45).
        it.pixelbox.cmwatch.rules.MasterService.cleanup(s, canExit, now)?.let { c ->
            run {
                val detail = when {
                    s.attached -> null
                    c.kind == it.pixelbox.cmwatch.rules.MasterService.Cleanup.Kind.DUPLICATE -> stringResource(R.string.cleanup_duplicate, c.of.orEmpty())
                    else -> s.outcome?.at?.let { stringResource(R.string.cleanup_finished_at, clockHm(it)) } ?: stringResource(R.string.cleanup_finished)
                }
                val open = stringResource(R.string.cleanup_open)
                // Il nome sta già in testa alla scheda: il tasto dice solo «Chiudi» (Franz 30/10 19:46).
                V3Split(Modifier.fillMaxWidth(), gap = 10.dp, text = {
                    Text(
                        if (detail == null) androidx.compose.ui.text.AnnotatedString(stringResource(R.string.cleanup_attached))
                        else androidx.compose.ui.text.buildAnnotatedString {
                            withStyle(androidx.compose.ui.text.SpanStyle(color = Cm.idle, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)) { append("● $open") }
                            append(" · $detail")
                        },
                        style = MaterialTheme.typography.bodySmall, color = Cm.text2,
                    )
                }) {
                    if (!closing && c.canClose && !closeAsk) Surface(onClick = { closeAsk = true }, shape = CircleShape, color = Cm.surfaceHigh, contentColor = Cm.text) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.PowerSettingsNew, null, tint = Cm.briefAlertRing, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.cleanup_close_short), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            // Su una riga sua (Franz 27/10 13:23): accanto, col nome lungo, lasciava allo stato due-tre lettere per riga.
            if (closing) ClosingChip(s.name)
            if (c.canClose && closeAsk) androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.close_inline), style = MaterialTheme.typography.bodySmall, color = Cm.text2, modifier = Modifier.padding(end = 4.dp))
                androidx.compose.material3.TextButton(onClick = { closeAsk = false }) { Text(stringResource(R.string.close_keep), color = Cm.actionIcon) }
                androidx.compose.material3.Button(
                    onClick = { closeAsk = false; onClose() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = V3.S3, contentColor = Cm.gone),
                ) { Text(stringResource(R.string.cleanup_close_short)) }
            }
        }
        if (closing && it.pixelbox.cmwatch.rules.MasterService.cleanup(s, canExit, now) == null) ClosingChip(s.name)
        if (menuClose) androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.close_inline), style = MaterialTheme.typography.bodySmall, color = Cm.text2, modifier = Modifier.padding(end = 4.dp))
            androidx.compose.material3.TextButton(onClick = { menuClose = false }) { Text(stringResource(R.string.close_keep), color = Cm.actionIcon) }
            androidx.compose.material3.Button(
                onClick = { menuClose = false; onClose() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = V3.S3, contentColor = Cm.gone),
            ) { Text(stringResource(R.string.cleanup_close_short)) }
        }
        if (menu) MenuPanel(
            onDismiss = { menu = false },
            entries = listOfNotNull(
                MenuEntry(
                    if (s.followed) Icons.Outlined.NotificationsOff else Icons.Outlined.NotificationsActive,
                    stringResource(if (s.followed) R.string.unfollow else R.string.follow),
                    stringResource(if (s.followed) R.string.unfollow_sub else R.string.follow_sub),
                ) { onFollow(!s.followed) },
                onAskMaster?.let { ask -> MenuEntry(Icons.Outlined.SupervisorAccount, stringResource(R.string.ask_master), stringResource(R.string.ask_master_sub), onClick = ask) },
            ),
            footer = if (canExit && s.state != it.pixelbox.cmwatch.contract.SessionState.GONE)
                MenuEntry(Icons.Outlined.PowerSettingsNew, stringResource(R.string.menu_exit), stringResource(R.string.menu_exit_sub), destructive = true) { menuClose = true } else null,
        ) {
            SessionBadge(s, 20.dp)
            Text(
                s.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Cm.text,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, modifier = Modifier.weight(1f),
            )
        }
        // Le barre dei KPI 5 e 6 solo nella card aperta (riordino del 29/10, punto 8).
        if (open) s.context?.let { pct ->
            val bar = when (it.pixelbox.cmwatch.rules.SessionMeters.contextTone(pct)) {
                it.pixelbox.cmwatch.rules.BriefCards.Tone.ALERT -> Cm.briefAlertRing
                it.pixelbox.cmwatch.rules.BriefCards.Tone.WARN -> Cm.briefWarn
                else -> Cm.briefRing
            }
            // Grammatica unica (Franz 24/10 00:01): il contesto sta sulla barra, col valore in mono accanto; non in testa.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Proposta 6: le ultime sei ore sulla riga della barra, dove c'è posto anche a 320 dp.
                if (s.state != it.pixelbox.cmwatch.contract.SessionState.GONE) kpiEvents?.let { ev -> Kpi6(it.pixelbox.cmwatch.rules.Kpi.hourly(ev, now)) }
                // Proposta 5: il segno della soglia di handoff sulla barra, e la parte di quota di oggi accanto al contesto.
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f).height(7.dp).testTag("kpi-5"), contentAlignment = Alignment.CenterStart) {
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Cm.briefTrack)) {
                        Box(Modifier.fillMaxWidth(it.pixelbox.cmwatch.rules.SessionMeters.contextFraction(pct) ?: 0f).fillMaxHeight().background(bar))
                    }
                    Box(Modifier.offset(x = maxWidth * (it.pixelbox.cmwatch.rules.Kpi.HANDOFF / 100f)).width(2.dp).fillMaxHeight().clip(RoundedCornerShape(1.dp)).background(Cm.text.copy(alpha = 0.8f)))
                }
                Text(stringResource(R.string.ctx_short, pct) + (todayShare?.let { " · " + stringResource(R.string.kpi5_today, it) } ?: ""), style = MonoSmall.copy(fontSize = 11.sp), color = Cm.text2, maxLines = 1)
            }
        } ?: run { if (open && s.state != it.pixelbox.cmwatch.contract.SessionState.GONE) kpiEvents?.let { ev -> Kpi6(it.pixelbox.cmwatch.rules.Kpi.hourly(ev, now)) } }
    }
    }
}

/** «12'», «3 h», «2 g»: corto come in una diretta. */
@Composable
private fun railSpan(from: Long, now: Long): String {
    val sp = it.pixelbox.cmwatch.rules.Rail.span(from, now)
    return when (sp.unit) {
        it.pixelbox.cmwatch.rules.Rail.Unit.MIN -> stringResource(R.string.rail_min, sp.n)
        it.pixelbox.cmwatch.rules.Rail.Unit.H -> stringResource(R.string.rail_h, sp.n)
        it.pixelbox.cmwatch.rules.Rail.Unit.D -> stringResource(R.string.rail_d, sp.n)
    }
}

/** «Chiuse · N»: una riga sola, anche con molte sessioni chiuse; apre l'elenco con «Riapri». */

/** Il titolo della parte sotto le sessioni: più forte delle etichette dei gruppi, con il filo e una riga che spiega. */
@Composable
private fun OutsideHeader() {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.outside_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Cm.text)
            Box(Modifier.weight(1f).height(1.dp).background(Cm.line))
        }
        Text(stringResource(R.string.outside_sub), style = MaterialTheme.typography.bodySmall, color = Cm.stale)
    }
}

/**
 * L'etichetta di una categoria: icona, nome e conteggio in maiuscoletto, neutri (le etichette colorate sono gli stati delle
 * sessioni, sopra); solo «Da sistemare» nel colore dell'attesa.
 */
@Composable
private fun CategoryHeader(g: OutsideSessions.Group) {
    val (icon, label) = when (g.category) {
        OutsideSessions.Category.FIX -> Icons.Rounded.WarningAmber to R.string.cat_fix
        OutsideSessions.Category.SCHEDULED -> Icons.Rounded.Schedule to R.string.cat_scheduled
        OutsideSessions.Category.RESUME -> Icons.Rounded.Replay to R.string.cat_resume
        OutsideSessions.Category.NIGHT -> Icons.Rounded.Bedtime to R.string.cat_night
        OutsideSessions.Category.CLOSED -> Icons.Rounded.Inventory2 to R.string.cat_closed
    }
    val tone = if (g.category == OutsideSessions.Category.FIX) Cm.waiting else Cm.text2
    val name = stringResource(label)
    val text = if (g.category == OutsideSessions.Category.NIGHT) name else stringResource(R.string.cat_count, name, g.count)
    Row(Modifier.padding(start = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tone, modifier = Modifier.size(16.dp))
        Text(text.uppercase(), style = MonoSmall.copy(color = tone))
        if (g.category == OutsideSessions.Category.RESUME) Text(stringResource(R.string.cat_resume_note), style = MaterialTheme.typography.labelSmall, color = Cm.stale)
    }
}

/** Grammatica unica (Franz 24/10 00:01): ogni voce di Altro è una scheda della famiglia, col glifo della categoria. */
@Composable
private fun OutsideCard(
    rows: List<MasterHome.Row>, onAction: (MasterHome.Row) -> Unit,
    tonight: it.pixelbox.cmwatch.rules.TonightCard.Model? = null, lastNight: it.pixelbox.cmwatch.rules.NightPage.Page? = null, onNight: () -> Unit = {},
    category: OutsideSessions.Category = OutsideSessions.Category.FIX,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            if (row.kind == MasterHome.Kind.NIGHT && tonight != null) TonightBlock(row, tonight, lastNight, onAction, onNight) else OutsideRow(row, category, onAction)
        }
    }
}

private fun categoryIcon(c: OutsideSessions.Category): androidx.compose.ui.graphics.vector.ImageVector = when (c) {
    OutsideSessions.Category.FIX -> Icons.Rounded.WarningAmber
    OutsideSessions.Category.SCHEDULED -> Icons.Rounded.Schedule
    OutsideSessions.Category.RESUME -> Icons.Rounded.Replay
    OutsideSessions.Category.NIGHT -> Icons.Rounded.Bedtime
    OutsideSessions.Category.CLOSED -> Icons.Rounded.Inventory2
}

private fun categoryLabel(c: OutsideSessions.Category): Int = when (c) {
    OutsideSessions.Category.FIX -> R.string.cat_fix
    OutsideSessions.Category.SCHEDULED -> R.string.cat_scheduled
    OutsideSessions.Category.RESUME -> R.string.cat_resume
    OutsideSessions.Category.NIGHT -> R.string.cat_night
    OutsideSessions.Category.CLOSED -> R.string.cat_closed
}

/**
 * La scheda Stanotte (mockup approvato da Franz l'08/10 alle 20:43), nella grammatica unica (24/10): luna, «Stanotte», «in corso ·
 * in coda» in mono, quanti lavori, poi un lavoro per riga con il progetto e le prime due righe di cosa deve fare; l'ultima notte
 * e il rapporto; in fondo «Aggiungi» tonale.
 */
@Composable
private fun TonightBlock(
    row: MasterHome.Row, m: it.pixelbox.cmwatch.rules.TonightCard.Model, last: it.pixelbox.cmwatch.rules.NightPage.Page?,
    onAction: (MasterHome.Row) -> Unit, onNight: () -> Unit,
) {
    val line = @Composable { Box(Modifier.fillMaxWidth().height(1.dp).background(Cm.line)) }
    Column(familyCard(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FamilyTop(Icons.Rounded.Bedtime, stringResource(R.string.cat_night), stringResource(R.string.tonight_meta, m.running, m.queued), chevron = false)
        Text(
            if (m.items.isEmpty()) stringResource(R.string.fy_night_empty_short) else pluralStringResource(R.plurals.tonight_title, m.items.size, m.items.size),
            style = MaterialTheme.typography.bodyMedium, color = Cm.text2,
        )
        m.items.forEachIndexed { i, it ->
            line()
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val running = it.runningMin != null
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (running) Cm.busy else Cm.surface), contentAlignment = Alignment.Center) {
                    if (running) Icon(Icons.Rounded.PlayArrow, null, tint = Cm.bg, modifier = Modifier.size(16.dp))
                    else Text((i + 1).toString(), style = MaterialTheme.typography.labelMedium, color = Cm.text2)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(it.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Cm.text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
                    Text(it.prompt, style = MaterialTheme.typography.bodyMedium, color = Cm.text2, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
                }
                Text(
                    if (running) stringResource(R.string.tonight_running, it.runningMin ?: 0) else stringResource(R.string.tonight_queued),
                    style = MonoSmall.copy(color = if (running) Cm.busy else Cm.text2),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (running) Cm.busy.copy(alpha = 0.14f) else Cm.surface).padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
        last?.let { p ->
            line()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.tonight_last, p.dayBefore.dayOfMonth, p.day.dayOfMonth), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
                if (p.counts.done > 0) MarkedText(StateIcons.done, pluralStringResource(R.plurals.night_c_done, p.counts.done, p.counts.done), Cm.briefGood, MaterialTheme.typography.bodyMedium)
                if (p.counts.stopped > 0) MarkedText(StateIcons.stopped, pluralStringResource(R.plurals.night_c_stopped, p.counts.stopped, p.counts.stopped), Cm.gone, MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.tonight_report), style = MaterialTheme.typography.labelLarge, color = Cm.actionIcon, modifier = Modifier.clip(RoundedCornerShape(8.dp)).handCursor().clickable(onClick = onNight).padding(6.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (m.items.isEmpty()) stringResource(R.string.tonight_empty_sub) else stringResource(R.string.tonight_sub, m.running, m.queued),
                style = MonoSmall.copy(color = Cm.text2), maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f),
            )
            MiniButton(stringResource(R.string.fy_btn_add), filled = false) { onAction(row) }
        }
    }
}

/** Titolo, dettaglio e tasto di una riga di servizio, per la scheda di Altro e per la riga nella scheda della sessione. */
@Composable
private fun serviceText(row: MasterHome.Row): Triple<String, String?, Int> {
    val n = row.number ?: 0
    return when (row.kind) {
        MasterHome.Kind.CONTEXT -> Triple(stringResource(R.string.fy_context, row.title, n), stringResource(R.string.fy_context_detail), R.string.fy_btn_handoff)
        MasterHome.Kind.SCHEDULED -> Triple(
            androidx.compose.ui.res.pluralStringResource(R.plurals.fy_scheduled, n.coerceAtLeast(1), n.coerceAtLeast(1)),
            row.at?.let { stringResource(R.string.fy_scheduled_detail, OUT_HM.format(java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault()))) },
            R.string.fy_btn_see,
        )
        // Il nome del progetto basta: «prossimo passo» lo dice già l'etichetta della categoria.
        MasterHome.Kind.NEXT_STEP -> Triple(row.title, row.detail, R.string.fy_btn_start)
        MasterHome.Kind.NIGHT_REPORT -> Triple(row.title, row.detail?.lineSequence()?.firstOrNull { it.isNotBlank() }, R.string.fy_btn_listen)
        MasterHome.Kind.NIGHT -> Triple(
            if (n > 0) stringResource(R.string.fy_night_queued, n) else stringResource(R.string.fy_night_empty_short),
            stringResource(R.string.fy_night_detail), R.string.fy_btn_add,
        )
        MasterHome.Kind.QUESTION, MasterHome.Kind.FINISHED -> Triple(row.title, row.detail, R.string.fy_btn_see)
    }
}

@Composable
private fun OutsideRow(row: MasterHome.Row, category: OutsideSessions.Category, onAction: (MasterHome.Row) -> Unit) {
    val n = row.number ?: 0
    val (title, detail, action) = serviceText(row)
    // Grammatica unica (24/10): il contesto di una sessione ha il suo nome in testa e «ctx N%» in mono; gli altri il titolo.
    val context = row.kind == MasterHome.Kind.CONTEXT
    val meta = when (row.kind) {
        MasterHome.Kind.CONTEXT -> stringResource(R.string.ctx_short, n)
        MasterHome.Kind.SCHEDULED -> row.at?.let { OUT_HM.format(java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault())) }
        else -> null
    }
    val body = if (context) listOfNotNull(title, detail).joinToString(". ") else detail
    Column(familyCard(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FamilyTop(categoryIcon(category), if (context) row.title else title, meta, chevron = false)
        body?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text2, maxLines = 2, overflow = TextOverflow.Clip) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(categoryLabel(category)), style = MonoSmall.copy(color = Cm.text2), maxLines = 1, modifier = Modifier.weight(1f))
            MiniButton(stringResource(action), filled = false) { onAction(row) }
        }
    }
}

private val OUT_HM = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

/** Le sessioni chiuse in una riga: i primi tre nomi e quante altre; il tocco apre l'elenco. */
/** La card della sessione appena chiusa: stessa chiave di volo della sua pagina, bordo d'accento finché resta. */
@Composable
private fun JustClosedCard(s: it.pixelbox.cmwatch.contract.Session, line: String) {
    Row(
        Modifier.fly("card-${s.id}").fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surface)
            .border(1.5.dp, Cm.actionIcon.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SessionBadge(s, 22.dp)
        Column(Modifier.weight(1f)) {
            Text(s.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Cm.text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = Cm.text2, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
        }
    }
}

@Composable
private fun ClosedCard(closed: List<Session>, onClick: () -> Unit) {
    val names = closed.take(3).joinToString(", ") { it.name } + if (closed.size > 3) " " + stringResource(R.string.closed_more, closed.size - 3) else ""
    val description = pluralStringResource(R.plurals.summary_closed, closed.size, closed.size)
    Column(familyCard(onClick).semantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FamilyTop(Icons.Rounded.Inventory2, stringResource(R.string.cat_closed), closed.size.toString(), chevron = true)
        Text(names, style = MaterialTheme.typography.bodyMedium, color = Cm.text2, maxLines = 2, overflow = TextOverflow.Clip)
    }
}

/** Un Prossimo come tasto; con `blocking` (contratto 1.38, variante 2) il filo ambra e il lucchetto aperto. */
@Composable
internal fun StepChip(text: String, blocking: Boolean, sent: Boolean = false, onClick: () -> Unit) {
    val label = stringResource(R.string.step_sent, text)
    Surface(
        onClick = onClick, enabled = !sent, shape = CircleShape,
        color = if (sent) Cm.idle.copy(alpha = 0.14f) else Cm.primary.copy(alpha = 0.12f), contentColor = if (sent) Cm.text2 else Cm.text,
        border = if (blocking && !sent) androidx.compose.foundation.BorderStroke(1.5.dp, Cm.waiting.copy(alpha = 0.65f)) else null,
        modifier = if (sent) Modifier.semantics { contentDescription = label } else Modifier,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (sent) Icon(Icons.Rounded.Check, null, tint = Cm.idle, modifier = Modifier.size(16.dp))
            else if (blocking) Icon(Icons.Rounded.LockOpen, null, tint = Cm.waiting, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Un'etichetta piccola nel colore dato, «DOPPIONE» o «PRODUZIONE». */
@Composable
internal fun Tag(text: String, tone: Color) = Text(
    text.uppercase(), style = MonoSmall.copy(color = tone, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    modifier = Modifier.clip(CircleShape).background(tone.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 3.dp),
)

/** Contratto 1.37: un compito che aspetta l'ok — cosa esce, dove, da quanto — con «Approva» e la conferma. */
@Composable
internal fun ApprovalCard(a: it.pixelbox.cmwatch.contract.Approval, now: Long, onApprove: (String, String, String?) -> Unit) {
    var ask by rememberSaveable(a.task) { mutableStateOf(false) }
    var choice by rememberSaveable(a.task) { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Verified, null, tint = Cm.advice, modifier = Modifier.size(24.dp))
            Text(a.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Cm.text, modifier = Modifier.weight(1f))
            if (a.deploy) Tag(stringResource(R.string.approval_prod), Cm.prod)
        }
        listOfNotNull(
            a.what?.let { stringResource(R.string.approval_what) to it },
            a.where?.let { stringResource(R.string.approval_where) to it },
            stringResource(R.string.approval_asked) to it.pixelbox.cmwatch.contract.Durations.since(a.requestedAt, now),
        ).forEach { (k, v) ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.width(64.dp))
                Text(v, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
            }
        }
        androidx.compose.material3.Button(
            onClick = { ask = true }, modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary),
        ) { Text(stringResource(R.string.approve)) }
    }
    if (ask) ApproveDialog(a, choice, onConfirm = { n -> ask = false; onApprove(a.task, n, choice) }, onDismiss = { ask = false })
}

/** Un ok del registro dentro la card della sessione che lo chiede: il titolo e «Approva», che apre la stessa finestra della scheda. */
@Composable
private fun ApprovalLine(a: it.pixelbox.cmwatch.contract.Approval, onApprove: (String, String, String?) -> Unit) {
    var ask by rememberSaveable(a.task) { mutableStateOf(false) }
    var choice by rememberSaveable(a.task) { mutableStateOf<String?>(null) }
    // Franz 24/10 09:22: con le opzioni il titolo intero sopra e i tasti sotto, che vanno a capo fra loro.
    val withOptions = it.pixelbox.cmwatch.rules.Approvals.choices(a).isNotEmpty()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Verified, null, tint = Cm.advice, modifier = Modifier.size(18.dp))
            V3Split(Modifier.weight(1f), gap = 10.dp, text = { Text(a.title, style = MaterialTheme.typography.bodySmall, color = Cm.text) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (a.deploy) Tag(stringResource(R.string.approval_prod), Cm.prod)
                    if (!withOptions) OkButtons(a, filled = true) { o -> choice = o; ask = true }
                }
            }
        }
        if (withOptions) Box(Modifier.padding(start = 28.dp)) { OkButtons(a, filled = true) { o -> choice = o; ask = true } }
    }
    if (ask) ApproveDialog(a, choice, onConfirm = { n -> ask = false; onApprove(a.task, n, choice) }, onDismiss = { ask = false })
}

/** Strada B: un lavoro nella scheda della sua sessione: glifo nel colore del gruppo, titolo breve, «da te · chiede alle HH:MM» o «può partire», e il tasto. */
@Composable
private fun WorkLine(w: Lavori.ForYou, onDone: () -> Unit, onStart: () -> Unit) {
    val outside = w.kind == Lavori.Kind.OUTSIDE
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WorkGlyph(w.kind, if (outside) Cm.briefWarn else Cm.briefRing)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(Lavori.shortTitle(w.title, w.commitment?.short ?: w.agenda?.short), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = Cm.text, maxLines = 1, overflow = TextOverflow.Clip)
            val sub = if (outside) (w.at?.let { stringResource(R.string.lavori_you_at, hhmmOf(it)) } ?: stringResource(R.string.lavori_you)) else stringResource(R.string.lavori_can_start)
            Text(sub, style = MonoSmall.copy(color = Cm.text2), maxLines = 1, overflow = TextOverflow.Clip)
        }
        if (outside) MiniButton(stringResource(R.string.agenda_done), filled = true, onClick = onDone)
        else MiniButton(stringResource(R.string.lavori_start), filled = false, onClick = onStart)
    }
}

/** Contratto 1.81 (Franz 23/10 23:08): un tasto per opzione quando l'ok chiede una scelta (`Approvals.choices`), altrimenti «Approva». */
@Composable
private fun OkButtons(a: it.pixelbox.cmwatch.contract.Approval, filled: Boolean, onPick: (choice: String?) -> Unit) {
    val opts = it.pixelbox.cmwatch.rules.Approvals.choices(a)
    if (opts.isEmpty()) MiniButton(stringResource(R.string.approve), filled = filled) { onPick(null) }
    else {
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { opts.forEach { o -> MiniButton(o, filled = false) { onPick(o) } } }
    }
}

/** Il tasto piccolo delle righe (come l'Approva della scheda): pieno per quello che sblocca una sessione, tonale per l'agenda. */
@Composable
private fun MiniButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (filled) Cm.primary else Cm.surfaceHigh, contentColor = if (filled) Cm.onPrimary else Cm.text) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}



/** Franz 24/10 00:44: ogni voce di Lavori è una scheda della famiglia (badge, titolo breve, meta in mono, chevron); aperta mostra i dettagli (`Lavori.details`). */
@Composable
private fun WorkCard(
    r: Lavori.ForYou, open: Boolean, onToggle: () -> Unit, onApprove: (String, String, String?) -> Unit, onDoneForPerson: (it.pixelbox.cmwatch.contract.Commitment) -> Unit,
    onAgendaEdit: (RecapAgenda.Edit) -> Unit, onStart: (it.pixelbox.cmwatch.rules.RecapActions.Action) -> Unit,
) {
    var ask by rememberSaveable(r.title) { mutableStateOf(false) }
    var choice by rememberSaveable(r.title) { mutableStateOf<String?>(null) }
    val a = r.approval; val ag = r.agenda; val cm = r.commitment; val who = r.who?.takeIf { it.isNotBlank() }; val at = r.at
    // Franz 24/10 09:22: per un ok «chi chiede» sta nella riga in fondo; in testa resta corto e non schiaccia il titolo.
    val meta = ag?.scope?.takeIf { it.isNotBlank() } ?: cm?.session
    val sub = when (r.kind) {
        Lavori.Kind.OK -> when {
            who != null && at != null -> stringResource(R.string.lavori_asks, who, hhmmOf(at))
            who != null -> stringResource(R.string.lavori_asks_who, who)
            else -> stringResource(R.string.lavori_waits_ok)
        }
        Lavori.Kind.OUTSIDE -> if (cm != null) who?.let { stringResource(R.string.lavori_unblocks, it) } else who
        Lavori.Kind.CLAUDE -> who
    }
    Column(familyCard(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkBadge(r.kind, ag?.let { RecapAgenda.mark(it.scope) } ?: RecapAgenda.Mark.NONE)
            Text(Lavori.shortTitle(r.title, ag?.short ?: cm?.short), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            meta?.let { Text(it, style = MonoSmall, color = Cm.text2, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.widthIn(max = 120.dp)) }
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, stringResource(if (open) R.string.steps_close else R.string.steps_open), tint = Cm.text2)
            }
        }
        // Franz 24/10 09:22: per una scelta la domanda intera, sempre.
        val asksChoice = a?.let { x -> it.pixelbox.cmwatch.rules.Approvals.choices(x).isNotEmpty() } == true
        Text(r.title, style = MaterialTheme.typography.bodyMedium, color = Cm.text2, maxLines = if (open || asksChoice) Int.MAX_VALUE else 2, overflow = TextOverflow.Clip)
        if (open) Lavori.details(r).forEach { d ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(detailLabel(d.kind)), style = MaterialTheme.typography.bodySmall, color = Cm.text2)
                Text(if (d.kind == Lavori.Detail.ASKED_AT) hhmmOf(d.value.toLong()) else d.value, style = MaterialTheme.typography.bodySmall, color = Cm.text)
            }
        }
        val opts = a?.let { x -> it.pixelbox.cmwatch.rules.Approvals.choices(x) }.orEmpty()
        if (opts.isNotEmpty()) {
            // Franz 24/10 09:22: con le opzioni, chi chiede su una riga sua e i tasti interi sotto, che vanno a capo fra loro.
            sub?.let { Text(it, style = MonoSmall.copy(color = Cm.text2), maxLines = 1, overflow = TextOverflow.Clip) }
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                opts.forEach { o -> MiniButton(o, filled = false) { choice = o; ask = true } }
            }
        } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(sub.orEmpty(), style = MonoSmall.copy(color = Cm.text2), maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            when (r.kind) {
                Lavori.Kind.OK -> if (a != null) OkButtons(a, filled = true) { o -> choice = o; ask = true }
                Lavori.Kind.OUTSIDE -> MiniButton(stringResource(R.string.agenda_done), filled = false) { ag?.let { row -> onAgendaEdit(RecapAgenda.Edit(RecapAgenda.Item.DONE, row)) }; cm?.let(onDoneForPerson) }
                Lavori.Kind.CLAUDE -> MiniButton(stringResource(R.string.lavori_start), filled = false) { ag?.let { row -> onStart(RecapAgenda.doIt(row)) } }
            }
        }
    }
    if (ask && a != null) ApproveDialog(a, choice, onConfirm = { n -> ask = false; onApprove(a.task, n, choice) }, onDismiss = { ask = false })
}

private fun detailLabel(k: Lavori.Detail): Int = when (k) {
    Lavori.Detail.WAITS -> R.string.lavori_d_waits
    Lavori.Detail.LAST_STEP -> R.string.lavori_d_last_step
    Lavori.Detail.NEXT -> R.string.lavori_d_next
    Lavori.Detail.FOLDER -> R.string.lavori_d_folder
    Lavori.Detail.EXPECTED -> R.string.lavori_d_expected
    Lavori.Detail.PAUSED -> R.string.lavori_d_paused
    Lavori.Detail.REF -> R.string.lavori_d_ref
    Lavori.Detail.DETAIL -> R.string.lavori_d_detail
    Lavori.Detail.BLOCKS -> R.string.lavori_d_blocks
    Lavori.Detail.UNTIL -> R.string.lavori_d_until
    Lavori.Detail.WHAT -> R.string.lavori_d_what
    Lavori.Detail.WHERE -> R.string.lavori_d_where
    Lavori.Detail.ASKED_AT -> R.string.lavori_d_asked_at
}

/** Franz 24/10 00:46: un suggerimento su questa sessione (il contesto alto) nella sua scheda, come gli ok: icona, titolo, dettaglio in mono, tasto tonale. */
@Composable
private fun ServiceLine(row: MasterHome.Row, onAction: () -> Unit) {
    val (title, detail, action) = serviceText(row)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.WarningAmber, null, tint = Cm.briefWarn, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = Cm.text, maxLines = 1, overflow = TextOverflow.Clip)
            detail?.let { Text(it, style = MonoSmall.copy(color = Cm.text2), maxLines = 1, overflow = TextOverflow.Clip) }
        }
        MiniButton(stringResource(action), filled = false, onClick = onAction)
    }
}

/** Titolo di sezione della home: si apre e si chiude al tocco; chiusa, a destra un riassunto se c'è. */
@Composable
internal fun HomeSection(title: String, summary: String?, open: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().handCursor().clickable(onClick = onToggle).padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.Icon(
            if (open) androidx.compose.material.icons.Icons.Rounded.ExpandMore else androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null, tint = Cm.actionIcon, modifier = Modifier.size(18.dp),
        )
        Text(title.uppercase(), style = MonoSmall.copy(color = Cm.text2, letterSpacing = 1.8.sp))
        Box(Modifier.weight(1f).height(1.dp).background(Cm.line))
        summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Cm.text2) }
    }
}

/** Grammatica unica (Franz 24/10 00:01): la Notte è una scheda come le altre: luna, «Stanotte», la finestra in mono, una riga di testo, i conteggi, «Apri» tonale. */
@Composable
private fun NightHomeCard(date: java.time.LocalDate, page: it.pixelbox.cmwatch.rules.NightPage.Page?, onOpen: () -> Unit) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val day = page?.day ?: date
    val before = page?.dayBefore ?: date.minusDays(1)
    val hm = remember { java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()) }
    val title = stringResource(R.string.night_title, before.dayOfMonth, day.dayOfMonth, day.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale))
    Column(familyCard(onOpen), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FamilyTop(Icons.Rounded.Bedtime, stringResource(R.string.cat_night), page?.let { "${hm.format(java.time.Instant.ofEpochSecond(it.start))}–${hm.format(java.time.Instant.ofEpochSecond(it.end))} · ${it.cards.size}" }, chevron = true)
        if (page == null) Text(stringResource(R.string.night_loading), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
        else {
            Text(stringResource(R.string.night_card_body, title, page.cards.size), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
            val c = page.counts
            if (c.done + c.running + c.partial + c.stopped + c.asking > 0) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c.done > 0) NightCount(StateIcons.done, pluralStringResource(R.plurals.night_c_done, c.done, c.done), Cm.briefGood)
                    if (c.running > 0) NightCount(StateIcons.running, pluralStringResource(R.plurals.night_c_running, c.running, c.running), Cm.actionIcon)
                    if (c.partial > 0) NightCount(StateIcons.partial, pluralStringResource(R.plurals.night_wc_partial, c.partial, c.partial), Cm.briefWarn)
                    if (c.stopped > 0) NightCount(StateIcons.stopped, pluralStringResource(R.plurals.night_c_stopped, c.stopped, c.stopped), Cm.goneDim)
                    if (c.asking > 0) NightCount(StateIcons.question, pluralStringResource(R.plurals.night_c_asking, c.asking, c.asking), Cm.advice)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val need = page.needs.firstOrNull()
                Text(
                    need?.let { pluralStringResource(R.plurals.home_night_needs, page.needs.size, page.needs.size, it.session, readableSlash(it.text)) } ?: stringResource(R.string.night_no_needs),
                    style = MonoSmall.copy(color = Cm.text2), maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f),
                )
                MiniButton(stringResource(R.string.home_night_open), filled = false, onClick = onOpen)
            }
        }
    }
}

/** Grammatica unica: il badge della famiglia per le schede che non sono sessioni (cerchio del colore della barra, icona bianca). */
@Composable
private fun FamilyBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.size(24.dp).clip(CircleShape).background(Cm.briefTrack), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

/** La testa di una scheda della famiglia: badge, nome in grassetto, meta in mono, chevron se la scheda si apre. */
@Composable
private fun FamilyTop(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, meta: String?, chevron: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FamilyBadge(icon)
        V3Split(Modifier.weight(1f), text = { Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text) }) { meta?.let { Text(it, style = MonoSmall, color = Cm.text2, maxLines = 1) } }
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Cm.text2, modifier = Modifier.size(22.dp))
    }
}

/** La scheda della famiglia: stesso fondo, raggio e margini della scheda di una sessione. */
@Composable
private fun familyCard(onClick: (() -> Unit)? = null): Modifier =
    Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).v3Edge()
        .then(if (onClick != null) Modifier.handCursor().clickable(onClick = onClick) else Modifier)
        .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 12.dp)

@Composable
private fun NightCount(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tone: Color) = MarkedText(
    icon, text, tone, MaterialTheme.typography.labelLarge,
    modifier = Modifier.clip(CircleShape).background(Cm.surfaceHigh).padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
)

/**
 * Contratto 1.68 (Franz, 14/10 22:45): in cima alla casa della master quante cose aspettano Franz nelle altre sessioni, coi
 * nomi; il tocco porta alla prima. Ambra come le card che aspettano.
 */
@Composable
private fun AttentionBanner(items: List<it.pixelbox.cmwatch.contract.Attention>, onClick: () -> Unit) {
    val names = items.mapNotNull { it.session }.distinct().joinToString(", ")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cm.briefWarn).handCursor().clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            pluralStringResource(R.plurals.attention_banner, items.size, items.size), style = MaterialTheme.typography.titleSmall,
            color = Cm.briefWarnInk, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
        if (names.isNotEmpty()) Text(
            names, style = MaterialTheme.typography.bodyMedium, color = Cm.briefWarnInk, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
    }
}

/**
 * L'aggiornamento dentro l'app (P6, approvata da Franz il 21/10 alle 23:58): in cima alla home la release nuova di GitHub
 * con «Installa» (scarica, verifica col digest, apre l'installer di Android) e «Non ora». Mentre scarica, la barra; se
 * Android vuole prima il permesso di installare app, lo dice; se non va, il perché e «Riprova».
 */
@Composable
private fun UpdateCard(u: AppUpdater.Ui, now: Long, onInstall: (AppUpdate.Release) -> Unit, onDismiss: (String) -> Unit) {
    val p = u.phase
    val r = p.release ?: return
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = Cm.briefGood, modifier = Modifier.size(24.dp))
            Text(
                stringResource(R.string.update_available), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Cm.text, modifier = Modifier.weight(1f),
            )
        }
        Text(
            stringResource(R.string.update_sub, r.name, AppUpdate.sizeLabel(r.size), it.pixelbox.cmwatch.contract.Durations.since(r.publishedAt, now)),
            style = MaterialTheme.typography.bodyMedium, color = Cm.text2,
        )
        when (p) {
            is AppUpdater.Phase.Downloading -> {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { p.percent / 100f }, modifier = Modifier.fillMaxWidth(), color = Cm.briefGood, trackColor = Cm.briefTrack,
                )
                Text(stringResource(R.string.update_downloading, p.percent), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
            }
            is AppUpdater.Phase.Ready -> Text(
                stringResource(if (u.needsPermission) R.string.update_allow else R.string.update_ready),
                style = MaterialTheme.typography.bodyMedium, color = Cm.text2,
            )
            is AppUpdater.Phase.Failed -> Text(
                stringResource(R.string.update_failed, stringResource(when (p.reason) {
                    AppUpdater.Reason.NO_DIGEST -> R.string.update_fail_no_digest
                    AppUpdater.Reason.BAD_DIGEST -> R.string.update_fail_bad_digest
                    AppUpdater.Reason.NETWORK -> R.string.update_fail_network
                    AppUpdater.Reason.HTTP -> R.string.update_fail_http
                })),
                style = MaterialTheme.typography.bodyMedium, color = Cm.gone,
            )
            else -> {}
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (p !is AppUpdater.Phase.Downloading) androidx.compose.material3.Button(
                onClick = { onInstall(r) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary),
            ) { Text(stringResource(if (p is AppUpdater.Phase.Failed) R.string.update_retry else R.string.update_install)) }
            androidx.compose.material3.TextButton(onClick = { onDismiss(r.tag) }) { Text(stringResource(R.string.update_not_now), color = Cm.text2) }
        }
    }
}

/**
 * Un impegno del registro (contratto 1.76, P2 e P4): titolo, «approvato alle 12:01 · fermo da 9 h» (ambra da 2 ore, rosso da
 * 8), a chi tocca e il prossimo passo; «Approva» quando tocca a Franz e c'è l'ok collegato (lo stesso `approve` di «Da
 * approvare»: dopo l'ok il relay risveglia la sessione).
 */
@Composable
internal fun CommitmentCard(r: Commitments.Row, now: Long, onApprove: (String, String, String?) -> Unit) {
    val c = r.c
    var ask by rememberSaveable(c.task) { mutableStateOf(false) }
    var choice by rememberSaveable(c.task) { mutableStateOf<String?>(null) }
    val toneColor = when (r.tone) { Commitments.Tone.ALERT -> Cm.gone; Commitments.Tone.WARN -> Cm.waiting; Commitments.Tone.NORMAL -> Cm.text2 }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Verified, null, tint = if (c.paused != null) Cm.text2 else Cm.advice, modifier = Modifier.size(22.dp))
            Text(c.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text, modifier = Modifier.weight(1f))
            if (c.stalled && c.paused == null) Tag(stringResource(R.string.commit_stalled), toneColor)
        }
        val approvedAt = c.approvedAt
        val when_ = if (c.approved && approvedAt != null) stringResource(R.string.commit_approved_at, hhmmOf(approvedAt)) else if (c.state == "awaiting_ok") stringResource(R.string.commit_awaiting) else null
        val idle = c.paused?.let { stringResource(R.string.commit_paused, it.reason) } ?: stringResource(R.string.commit_idle, it.pixelbox.cmwatch.contract.Durations.since(now - r.idle, now))
        Text(listOfNotNull(when_, idle).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = toneColor)
        Text(
            if (r.yourTurn) stringResource(R.string.commit_your_turn) else stringResource(R.string.commit_turn, r.turn ?: c.session ?: "?"),
            style = MaterialTheme.typography.bodyMedium, color = Cm.text,
        )
        c.next?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.commit_next) + ": " + it, style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
        if (c.check?.outcome == "red") Text(stringResource(R.string.commit_check_red), style = MaterialTheme.typography.bodySmall, color = Cm.gone)
        val a = r.approval
        if (r.yourTurn && a != null) androidx.compose.material3.Button(
            onClick = { ask = true }, modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary),
        ) { Text(stringResource(R.string.approve)) }
        if (ask && a != null) ApproveDialog(a, choice, onConfirm = { n -> ask = false; onApprove(a.task, n, choice) }, onDismiss = { ask = false })
    }
}

/** La conferma dell'ok (contratto 1.37), la stessa in home e nella pagina Lavori: cosa, dove, una nota facoltativa. */
@Composable
internal fun ApproveDialog(a: it.pixelbox.cmwatch.contract.Approval, choice: String? = null, onConfirm: (note: String) -> Unit, onDismiss: () -> Unit) {
    var note by rememberSaveable(a.task) { mutableStateOf("") }
    // Franz 24/10 09:25: titolo corto, la domanda una volta sola con il tasto ▶, le opzioni con quella scelta evidenziata,
    // la nota facoltativa, poi Annulla e la conferma.
    val question = listOfNotNull(a.title.takeIf { it.isNotBlank() }, a.what?.takeIf { it.isNotBlank() }, a.where?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.approve_where, it) }).joinToString(". ")
    val opts = it.pixelbox.cmwatch.rules.Approvals.choices(a)
    val speak = LocalSpeakText.current
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Cm.surface,
        title = { Text(if (choice != null) stringResource(R.string.choose_short, choice) else if (a.deploy) stringResource(R.string.approve_prod_title) else stringResource(R.string.approve_short), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (speak != null) Speakable(question, speak = true, onSpeak = speak)
                else Text(question, style = MaterialTheme.typography.bodyLarge, color = Cm.text)
                if (opts.isNotEmpty()) {
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        opts.forEach { o ->
                            val on = o == choice
                            Text(o, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, color = if (on) Cm.onPrimary else Cm.text2,
                                modifier = Modifier.clip(CircleShape).background(if (on) Cm.primary else Cm.surfaceHigh).padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
                androidx.compose.material3.OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.approve_note)) }, placeholder = { Text("ok") }, singleLine = true)
            }
        },
        confirmButton = { androidx.compose.material3.Button(onClick = { onConfirm(note) }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary)) { Text(choice ?: stringResource(R.string.approve)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Cm.text2) } },
    )
}

/** Contratto 1.80: la scadenza di un needs-you con il giorno (`Durations.expiry`): oggi solo l'ora, domani, poi la data. */
@Composable
internal fun expiryText(at: Long, now: Long): String {
    val e = it.pixelbox.cmwatch.contract.Durations.expiry(at, now, java.time.ZoneId.systemDefault())
    return when (e.day) {
        it.pixelbox.cmwatch.contract.Durations.Day.TODAY -> stringResource(R.string.expires_at, e.hm)
        it.pixelbox.cmwatch.contract.Durations.Day.TOMORROW -> stringResource(R.string.expires_tomorrow, e.hm)
        it.pixelbox.cmwatch.contract.Durations.Day.LATER -> stringResource(R.string.expires_on, e.date.orEmpty(), e.hm)
    }
}

internal fun hhmmOf(epoch: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(java.time.Instant.ofEpochSecond(epoch).atZone(java.time.ZoneId.systemDefault()))

/** Mandato /exit da una card: lo dice finché la sessione non esce; poi in cima la card «chiusa alle» (Franz 24/10 13:20). */
@Composable
private fun ClosingChip(name: String) {
    Surface(shape = CircleShape, color = Cm.surfaceHigh, contentColor = Cm.text) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.CircularProgressIndicator(color = Cm.actionIcon, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Text(stringResource(R.string.splash_closing, name), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Contratto 1.84 (B4, Franz 24/10 19:46): la conferma di «Chiudi le finite» dice per ogni sessione cosa salva il PC (esito,
 * prossimo passo, l'id per riprenderla), avvisa dei file non committati e dice chi resta aperta e perché.
 */
@Composable
private fun CloseFinishedDialog(p: it.pixelbox.cmwatch.rules.CloseFinished.Preview?, done: Boolean, onDismiss: () -> Unit, onConfirm: (List<String>) -> Unit) {
    val ok = p?.let { pv -> it.pixelbox.cmwatch.rules.CloseFinished.closable(pv) }.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Cm.surface,
        title = { Text(stringResource(R.string.close_finished_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    !done -> Text(stringResource(R.string.close_finished_loading), color = Cm.text2)
                    p == null -> Text(stringResource(R.string.close_finished_no_preview), color = Cm.text2)
                    else -> {
                        Text(stringResource(R.string.close_finished_sub), color = Cm.text2, style = MaterialTheme.typography.bodyMedium)
                        p.sessions.forEach { x ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(x.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = if (x.closable) Cm.text else Cm.text2)
                                val small = MaterialTheme.typography.bodySmall
                                if (!x.closable) Text(stringResource(when (x.blockedBy) { "busy" -> R.string.close_blocked_busy; "question" -> R.string.close_blocked_question; "background" -> R.string.close_blocked_background; "attached" -> R.string.close_blocked_attached; "gone" -> R.string.close_blocked_gone; else -> R.string.close_blocked_other }), style = small, color = Cm.text2)
                                else {
                                    x.save?.outcome?.let { Text(stringResource(R.string.close_saves_outcome, it), style = small, color = Cm.text2) }
                                    x.save?.next?.let { Text(stringResource(R.string.close_saves_next, it), style = small, color = Cm.text2) }
                                    x.save?.dirtyN?.takeIf { it > 0 }?.let { n -> Text(stringResource(R.string.close_dirty, n, x.save?.dirty.orEmpty().take(3).joinToString(", ")), style = small, color = Cm.briefWarn) }
                                    if (x.save?.sessionId != null) Text(stringResource(R.string.close_resumable), style = small, color = Cm.idle)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { if (ok.isNotEmpty()) androidx.compose.material3.Button(onClick = { onConfirm(ok) }) { Text(stringResource(R.string.close_finished_go, ok.size)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

