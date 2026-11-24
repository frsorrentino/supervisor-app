package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.TiAspetta
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * La scheda «Ti aspetta» (app-scheda-ti-aspetta-07-11, mockup approvato da Franz il 07/11 alle 21:04): la testata con la sessione e
 * da quanto aspetta; la carta coi tasti (`card`, quella di Decidi, Scegli, Verifica o Rispondi); «Da dove viene» col pezzo di
 * trascrizione, la frase della domanda accesa e «Apri nella trascrizione»; in fondo le altre che aspettano. `wide` (tablet, nella
 * colonna): la carta e «Da dove viene» affiancate e la × al posto della freccia indietro. «Ti aspettano anche» parte chiusa e si
 * apre col tocco (app-ti-aspetta-lista-chiusa-08-11); `othersOpen` solo per i test. `stack` (app-scheda-scelta-12-11): una scelta con le
 * immagini tiene tutta la larghezza anche nella colonna del tablet, e «Da dove viene» le sta sotto. `title` e `sub`: una scheda che non
 * aspetta più (dall'archivio) dice com'è finita e quando, al posto di «Ti aspetta» e «da 12 min».
 */
@Composable
fun TiAspettaPage(
    page: TiAspetta.Page, now: Long, wide: Boolean, onBack: () -> Unit, onJump: (TiAspetta.Source) -> Unit, onOther: (String) -> Unit,
    othersOpen: Boolean = false, stack: Boolean = false, title: String? = null, sub: String? = null,
    /** app-attesa-dopo-scelta-17-11: la via per la home, sempre, anche aperta da un link senza schermata di partenza. */
    onHome: (() -> Unit)? = null,
    /** app-attesa-caricamento-17-11: il segno mentre i dettagli arrivano dal PC; lento o fallito con Riprova e la sessione. */
    load: TiAspetta.Load? = null, onRetry: () -> Unit = {}, onSession: (() -> Unit)? = null,
    card: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
    // Non si salva: ogni scheda, anche un'altra aperta da questa lista, parte con la lista chiusa.
    var open by remember(page.key) { mutableStateOf(othersOpen) }
    // Sul telefono la scheda prende lo schermo intero e lascia il posto alla barra di stato come le altre pagine (systemBarsPadding,
    // app-ti-aspetta-titolo-320-10-11: il titolo finiva sotto l'ora, screenshot di Franz dell'08/11 21:38); nella colonna del
    // tablet il posto lo lascia già la plancia.
    // app-tastiera-ti-aspetta-23-11 (Franz 23/11 19:59): la pagina si accorcia della tastiera (imePadding, dopo le barre di sistema),
    // così il campo di una carta e i suoi tasti possono stare sopra; ImeBlock li porta in vista.
    Column(Modifier.fillMaxSize().background(LocalPageBg.current ?: V3.Bg).then(if (wide) Modifier else Modifier.systemBarsPadding()).imePadding().testTag("ti-aspetta-${page.key}")) {
        Row(Modifier.fillMaxWidth().padding(start = if (wide) 18.dp else 6.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = V3.Tx) }
            Column(Modifier.weight(1f)) {
                Text(title ?: stringResource(R.string.ti_aspetta_title), style = V3.Section.copy(fontSize = 22.sp))
                // Il nome prima di tutto (app-ti-aspetta-titolo-320-10-11): «da 49 min» va intero sotto il nome se non c'è posto, mai
                // spezzato in «da 49» e «min».
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    page.session?.let { n -> val whole = LocalNameWhole.current; Text("$n ·", style = V3.Meter, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, onTextLayout = { l -> whole?.invoke(n, !l.hasVisualOverflow) }) }
                    if (sub != null) Text(sub, style = V3.Meter, maxLines = 1, softWrap = false)
                    else Text(stringResource(R.string.ti_aspetta_since, TiAspetta.age(now - page.since)), style = V3.Meter.copy(color = V3.Wait), maxLines = 1, softWrap = false)
                }
            }
            onHome?.let { h -> IconButton(onClick = h, modifier = Modifier.handCursor().testTag("ti-aspetta-home")) { Icon(Icons.Rounded.Home, stringResource(R.string.ti_aspetta_home), tint = V3.Tx2) } }
            if (wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = V3.Tx2) }
        }
        if (wide) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = if (wide && stack) 22.dp else 14.dp, vertical = if (wide && stack) 18.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (stack) 22.dp else 14.dp),
        ) {
            if (wide && !stack) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { card() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { page.source?.let { SourceBlock(it, onJump) } }
            } else {
                card()
                // Nella scheda di scelta «Da dove viene» sta sotto, raggruppato e non più largo di 720 dp; altrove come prima.
                page.source?.let { src ->
                    if (stack) Column(Modifier.widthIn(max = if (wide) 720.dp else androidx.compose.ui.unit.Dp.Infinity), verticalArrangement = Arrangement.spacedBy(8.dp)) { SourceBlock(src, onJump) }
                    else SourceBlock(src, onJump)
                }
            }
            if (page.others.isNotEmpty()) Others(page.others, now, open, { open = !open }, onOther)
        }
    }
    load?.let { l -> WaitLoadChip(l, onRetry, onSession, Modifier.align(Alignment.BottomCenter)) }
    }
}

private val HM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun SecLabel(text: String, count: Int? = null) {
    Row(Modifier.padding(start = 2.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text.uppercase(), style = V3.Small.copy(fontSize = 12.sp, letterSpacing = 1.2.sp, color = V3.Tx3))
        count?.let { Text(it.toString(), style = V3.Small.copy(fontSize = 12.sp, color = V3.Wait)) }
    }
}

/** «Da dove viene»: la sessione e l'ora della risposta, il pezzo con la riga a sinistra e la frase della domanda accesa, il salto. */
@Composable
private fun SourceBlock(src: TiAspetta.Source, onJump: (TiAspetta.Source) -> Unit) {
    SecLabel(stringResource(R.string.ti_aspetta_source))
    Column(Modifier.fillMaxWidth().clip(SubShape).border(1.dp, V3.S3, SubShape).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            src.at?.let { t -> stringResource(R.string.ti_aspetta_reply_at, src.session, HM.format(Instant.ofEpochSecond(t).atZone(ZoneId.systemDefault()))) } ?: src.session,
            style = V3.Meter,
        )
        val hi = V3.Wait.copy(alpha = 0.18f)
        val text = buildAnnotatedString {
            TiAspetta.marks(src.text, src.ask).forEach { s -> if (s.on) withStyle(SpanStyle(background = hi, color = V3.Tx)) { append(s.text) } else append(s.text) }
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(V3.Tx3))
            Text(text, style = V3.Body.copy(color = V3.Tx2), modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp).testTag("ti-aspetta-pezzo"))
        }
        if (src.entry != null) Box(
            Modifier.clip(CircleShape).background(V3.S3).handCursor().clickable { onJump(src) }.padding(horizontal = 14.dp, vertical = 8.dp).testTag("ti-aspetta-salto"),
        ) { Text(stringResource(R.string.ti_aspetta_jump), style = V3.Small.copy(color = V3.Tx, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)) }
    }
}

/** Le chiavi delle altre domande disegnate nella lista, per i test (app-ti-aspetta-lista-chiusa-08-11). */
internal val LocalWaitRows = staticCompositionLocalOf<((String) -> Unit)?> { null }

/**
 * Le altre domande che aspettano: la riga «Ti aspettano anche N» con la freccia apre e richiude la lista (Franz 08/11 21:38: «la
 * lista sotto deve essere chiusa e apribile»); ogni voce col pallino ambra, il titolo, l'etichetta e la sessione in mono, la freccia.
 */
@Composable
private fun Others(others: List<TiAspetta.Other>, now: Long, open: Boolean, onToggle: () -> Unit, onOther: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(SubShape).handCursor().clickable(onClick = onToggle).padding(end = 6.dp).testTag("ti-aspetta-altre"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { SecLabel(stringResource(R.string.ti_aspetta_others), others.size) }
        Icon(
            Icons.Rounded.KeyboardArrowDown, stringResource(if (open) R.string.ti_aspetta_others_close else R.string.ti_aspetta_others_open), tint = V3.Tx2,
            modifier = Modifier.graphicsLayer { rotationZ = if (open) 180f else 0f },
        )
    }
    if (!open) return
    val rows = LocalWaitRows.current
    V3List {
        others.forEachIndexed { i, o ->
            rows?.invoke(o.key)
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).handCursor().clickable { onOther(o.key) }.padding(horizontal = 12.dp, vertical = 10.dp).testTag("ti-aspetta-altra-${o.key}"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(V3.Wait))
                Column(Modifier.weight(1f)) {
                    Text(o.title, style = V3.Body.copy(color = V3.Tx))
                    Text(listOfNotNull(kindLabel(o.kind), o.session, TiAspetta.age(now - o.since)).joinToString(" · "), style = V3.Meter)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3)
            }
        }
    }
}

/** L'etichetta all'imperativo della carta (Franz 06/11): Decidi, Scegli, Verifica, Rispondi. */
@Composable
internal fun kindLabel(k: TiAspetta.Kind): String = stringResource(
    when (k) { TiAspetta.Kind.DECIDE -> R.string.ti_aspetta_decide; TiAspetta.Kind.CHOOSE -> R.string.ti_aspetta_choose; TiAspetta.Kind.VERIFY -> R.string.ti_aspetta_verify; TiAspetta.Kind.ANSWER -> R.string.ti_aspetta_answer },
)

/**
 * La carta di una domanda con i tasti del relay (contratto 1.131, `actions`): l'etichetta e il segno «produzione», il titolo, la
 * domanda e i campi della carta; le scelte numerate con la consigliata in ambra, un solo tasto pieno, gli altri tonali. Un tasto con
 * `textField` chiede prima il testo (il motivo di un «Non va», una risposta libera) e poi manda. Dopo l'invio i tasti si spengono e
 * resta acceso quello toccato. `onSend` riceve il tasto e il testo scritto; il comando lo compone Repo.commandOf.
 */
@Composable
fun WaitCardBody(
    item: it.pixelbox.cmwatch.contract.WaitItem,
    ops: List<String>? = null, now: Long = 0,
    /** Contratto 1.159: l'op `silence` col testo («3d» o «off»); null senza relay. */
    onSilence: (suspend (String) -> it.pixelbox.cmwatch.contract.CmdResult?)? = null,
    onSend: (it.pixelbox.cmwatch.contract.WaitAction, String?) -> Unit,
) {
    var sent by androidx.compose.runtime.saveable.rememberSaveable(item.id) { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    var writing by androidx.compose.runtime.saveable.rememberSaveable(item.id) { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    var written by androidx.compose.runtime.saveable.rememberSaveable(item.id) { androidx.compose.runtime.mutableStateOf("") }
    val actions = item.actions.orEmpty()
    fun go(i: Int, a: it.pixelbox.cmwatch.contract.WaitAction) {
        if (sent != null) return
        if (a.textField != null && writing != i) { writing = i; written = ""; return }
        sent = i; writing = null
        onSend(a, written.takeIf { a.textField != null })
    }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(V3.S1).border(1.dp, V3.Wait.copy(alpha = 0.45f), shape).padding(14.dp).testTag("ti-aspetta-carta"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.labelText.uppercase(), style = V3.Small.copy(fontSize = 12.sp, letterSpacing = 1.sp, color = V3.Tx3))
            if (item.card?.deploy == true) Text(
                stringResource(R.string.ti_aspetta_production).uppercase(),
                style = V3.Small.copy(fontSize = 12.sp, color = V3.Wait),
                modifier = Modifier.border(1.dp, V3.Wait.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        item.title?.takeIf { t -> t.isNotBlank() && t != item.text }?.let { t -> Text(t, style = V3.Name.copy(fontSize = 18.sp)) }
        Text(item.text, style = V3.Body.copy(color = V3.Tx))
        AdviceLine(item.advice, "ti-aspetta-consiglio")
        item.card?.let { c ->
            listOfNotNull(
                c.what?.let { R.string.ti_aspetta_what to it }, c.where?.let { R.string.ti_aspetta_where to it },
                c.untested?.let { R.string.ti_aspetta_untested to it }, (c.undoHow ?: c.undo)?.let { R.string.ti_aspetta_undo to it },
            ).filter { (_, v) -> v.isNotBlank() && v != item.text }.forEach { (k, v) ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(k), style = V3.Small.copy(color = V3.Tx3), modifier = Modifier.width(96.dp))
                    Text(v, style = V3.Small.copy(color = V3.Tx), modifier = Modifier.weight(1f))
                }
            }
        }
        // Contratto 1.137: i file della richiesta (le immagini di cosa guardare di una verifica, gli allegati di un ok).
        item.files?.takeIf { fs -> fs.isNotEmpty() }?.let { fs -> WaitFilesBlock(fs) }
        var n = 0
        actions.forEachIndexed { i, a ->
            if (TiAspetta.lookOf(a) != TiAspetta.Look.OPTION) return@forEachIndexed
            n += 1
            val num = n
            val rec = a.recommended
            val on = sent == i
            val oshape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            Row(
                Modifier.fillMaxWidth().clip(oshape)
                    .background(if (rec) V3.Wait.copy(alpha = 0.14f) else V3.S2)
                    .border(1.dp, if (on) V3.Good else if (rec) V3.Wait.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Transparent, oshape)
                    .then(if (sent == null) Modifier.handCursor().clickable { go(i, a) } else Modifier)
                    .graphicsLayer { alpha = if (sent != null && !on) 0.45f else 1f }
                    .padding(horizontal = 12.dp, vertical = 10.dp).testTag("ti-aspetta-scelta-$num"),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(if (rec) V3.Wait else androidx.compose.ui.graphics.Color.Transparent)
                        .border(1.5.dp, if (rec) V3.Wait else V3.Tx2, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(num.toString(), style = V3.Meter.copy(color = if (rec) V3.Bg else V3.Tx2)) }
                Column(Modifier.weight(1f)) {
                    Text(a.label, style = V3.Name.copy(fontSize = 15.sp, color = if (rec) V3.Wait else V3.Tx))
                    if (rec) Text(stringResource(R.string.ti_aspetta_recommended), style = V3.Small)
                }
            }
        }
        val pills = actions.withIndex().filter { (_, a) -> TiAspetta.lookOf(a) != TiAspetta.Look.OPTION }
        if (pills.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pills.forEach { (i, a) ->
                Box(Modifier.graphicsLayer { alpha = if (sent != null && sent != i) 0.45f else 1f }.testTag("ti-aspetta-tasto-$i")) {
                    V3Pill(a.label, filled = TiAspetta.lookOf(a) == TiAspetta.Look.MAIN, confirmed = sent == i) { go(i, a) }
                }
            }
            // Il silenzio assenso accanto ai tasti; a 360 dp va sotto, intero. La riga con la data prende la fila intera.
            if (onSilence != null) SilenceBlock(item, ops, now, sent == null, onSilence)
        }
        writing?.let { i ->
            val a = actions.getOrNull(i) ?: return@let
            ImeBlock {
                androidx.compose.material3.OutlinedTextField(
                    written, { v -> written = v.take(2000) }, modifier = Modifier.fillMaxWidth().testTag("ti-aspetta-testo"),
                    label = { Text(a.label) }, minLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V3Pill(stringResource(R.string.ti_aspetta_send), filled = true) { if (written.isNotBlank() || a.textField == "text") go(i, a) }
                    V3Pill(stringResource(R.string.cancel)) { writing = null }
                }
            }
        }
        // Dall'archivio (contratto 1.137) una scheda che non aspetta più dice com'è finita; senza tasti e ancora aperta, la fa la master.
        if (actions.isEmpty()) {
            val o = it.pixelbox.cmwatch.rules.Scelta.outcomeOf(item)
            val said = when (o.state) {
                it.pixelbox.cmwatch.rules.Scelta.State.OPEN -> stringResource(R.string.ti_aspetta_by_master)
                it.pixelbox.cmwatch.rules.Scelta.State.CHOSEN -> o.letter?.let { l -> stringResource(R.string.scelta_chosen_letter, l) } ?: stringResource(R.string.scelta_chosen_words, o.name.orEmpty())
                it.pixelbox.cmwatch.rules.Scelta.State.APPROVED -> stringResource(R.string.scelta_approved)
                it.pixelbox.cmwatch.rules.Scelta.State.REDO -> stringResource(R.string.scelta_redo)
                it.pixelbox.cmwatch.rules.Scelta.State.CLOSED -> stringResource(R.string.scelta_closed)
                it.pixelbox.cmwatch.rules.Scelta.State.SILENCE -> stringResource(R.string.scelta_done_silence)
            }
            Text(listOfNotNull(said, o.reason).joinToString(": "), style = V3.Small)
        }
    }
}

/**
 * app-tastiera-ti-aspetta-23-11 (Franz 23/11 19:59): il blocco di un campo di testo di una carta (l'etichetta, il campo, Annulla e Manda)
 * torna in vista intero sopra la tastiera quando il campo ha il fuoco e la tastiera si apre o cambia altezza: la pagina si è accorciata
 * della tastiera (imePadding in TiAspettaPage) e il blocco chiede di essere visto. Lo stesso per «Chiudi senza ok», «Nessuna, rifare»,
 * il motivo di «Non va» e ogni risposta scritta.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ImeBlock(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val req = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val ime = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    androidx.compose.runtime.LaunchedEffect(focused, ime) { if (focused && ime > 0) req.bringIntoView() }
    Column(
        modifier.fillMaxWidth().then(Modifier.bringIntoViewRequester(req)).onFocusChanged { f -> focused = f.hasFocus }.testTag("campo-sopra-tastiera"),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
    )
}

/**
 * app-silenzio-assenso-23-11 (variante A, Franz 23/11 18:52; contratto 1.159): sulle carte di ok e di prova, mai su una scelta né sulla
 * produzione, il tasto tonale «Dai per risolto se tace» mette la scadenza di 3 giorni. Messa, resta la riga blu con la data e
 * «Annulla»; i tasti della risposta restano. Il blu dell'attesa, mai l'ambra né il verde: chiuso così non è un ok di Franz. La scadenza
 * mostrata è quella che il PC ha confermato, finché la voce riletta non la porta da sé; un rifiuto del PC si legge sotto.
 */
@Composable
internal fun SilenceBlock(
    item: it.pixelbox.cmwatch.contract.WaitItem, ops: List<String>?, now: Long, live: Boolean,
    onSilence: suspend (String) -> it.pixelbox.cmwatch.contract.CmdResult?,
) {
    val S = it.pixelbox.cmwatch.rules.Silence
    var set by androidx.compose.runtime.saveable.rememberSaveable(item.id) { mutableStateOf<Long?>(null) }
    var touched by androidx.compose.runtime.saveable.rememberSaveable(item.id) { mutableStateOf(false) }
    var busy by remember(item.id) { mutableStateOf(false) }
    var refused by remember(item.id) { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val until = if (touched) set else item.silenceUntil
    val pending = S.pending(until, now)
    fun go(text: String) {
        if (busy) return
        busy = true; refused = null
        scope.launch {
            val r = onSilence(text)
            busy = false
            when {
                r == null -> Unit
                !r.ok -> refused = r.text
                else -> { touched = true; set = if (text == "off") null else r.silenceUntil ?: (now + 3 * 86_400) }
            }
        }
    }
    val clock = Icons.Rounded.Schedule
    if (live && !pending && S.offer(item, ops)) {
        androidx.compose.material3.Surface(
            onClick = { go(S.DEFAULT) }, enabled = !busy, shape = CircleShape, color = V3.S3, contentColor = V3.Tx,
            modifier = Modifier.handCursor().testTag("ti-aspetta-silenzio"),
        ) {
            Row(Modifier.padding(start = 14.dp, end = 16.dp, top = 9.dp, bottom = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(clock, null, modifier = Modifier.size(18.dp))
                Text(stringResource(if (busy) R.string.silence_sending else R.string.silence_key), style = V3.Name.copy(fontSize = 15.sp, color = V3.Tx))
            }
        }
    }
    if (live && pending && until != null) {
        val loc = S.localeOf(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(V3.Busy.copy(alpha = 0.08f)).border(1.dp, V3.Busy.copy(alpha = 0.35f), shape)
                .padding(horizontal = 12.dp, vertical = 10.dp).testTag("ti-aspetta-silenzio-data"),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(clock, null, tint = V3.Busy, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.silence_due, S.whenLong(until, java.time.ZoneId.systemDefault(), loc)), style = V3.Small.copy(fontSize = 14.5.sp, color = V3.Tx), modifier = Modifier.weight(1f))
            if (item.task != null && ops?.contains("silence") == true) Text(
                stringResource(R.string.silence_undo),
                style = V3.Name.copy(fontSize = 14.sp, color = V3.Tx, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                modifier = Modifier.heightIn(min = 32.dp).handCursor().clickable(enabled = !busy) { go("off") }.padding(vertical = 6.dp).testTag("ti-aspetta-silenzio-annulla"),
            )
        }
    }
    refused?.let { t -> Text(t, style = V3.Small, modifier = Modifier.fillMaxWidth()) }
}

/**
 * Una risposta dalla scheda chiude la scheda, che torna alla schermata da cui era stata aperta come il gesto Indietro; aperta da un
 * link, la home (Franz 08/11 21:38: «quando clicco ok deve riportarmi alla schermata precedente», app-ti-aspetta-lista-chiusa-08-11).
 * Il comando parte prima; le verifiche chiudono solo se il PC ha preso l'ok o il «Non va».
 */
internal class WaitAnswer(private val close: () -> Unit) {
    fun <A> one(send: (A) -> Unit): (A) -> Unit = { a -> send(a); close() }
    fun <A, B> two(send: (A, B) -> Unit): (A, B) -> Unit = { a, b -> send(a, b); close() }
    fun <A> taken(send: suspend (A) -> Boolean): suspend (A) -> Boolean = { a -> send(a).also { ok -> if (ok) close() } }
    fun <A, B, C> taken(send: suspend (A, B, C) -> Boolean): suspend (A, B, C) -> Boolean = { a, b, c -> send(a, b, c).also { ok -> if (ok) close() } }
}

/** app-attesa-caricamento-17-11: in basso, il segno dei dettagli in arrivo; lento o fallito, la frase con Riprova e Apri la sessione. */
@Composable
internal fun WaitLoadChip(l: TiAspetta.Load, onRetry: () -> Unit, onSession: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier.systemBarsPadding().padding(16.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(if (l == TiAspetta.Load.DETAILS) 999.dp else 14.dp))
            .background(V3.S3).padding(horizontal = 12.dp, vertical = 4.dp).heightIn(min = 34.dp).testTag("ti-aspetta-dettagli"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (l == TiAspetta.Load.DETAILS) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(12.dp), color = V3.Tx3, strokeWidth = 2.dp)
            Text(stringResource(R.string.ti_aspetta_details), style = V3.Small.copy(fontSize = 13.5.sp, color = V3.Tx2))
        } else {
            Text(stringResource(if (l == TiAspetta.Load.FAILED) R.string.ti_aspetta_failed else R.string.ti_aspetta_slow), style = V3.Small.copy(fontSize = 13.5.sp, color = V3.Tx), modifier = Modifier.weight(1f, fill = false))
            androidx.compose.material3.TextButton(onClick = onRetry, modifier = Modifier.handCursor()) { Text(stringResource(R.string.ti_aspetta_retry), color = V3.Tx) }
            onSession?.let { s -> androidx.compose.material3.TextButton(onClick = s, modifier = Modifier.handCursor()) { Text(stringResource(R.string.ti_aspetta_open_session), color = V3.Tx) } }
        }
    }
}

/** La scheda mentre si legge, o quando la domanda non aspetta più: la freccia indietro e una riga. */
@Composable
fun TiAspettaEmpty(text: String, wide: Boolean = false, onHome: (() -> Unit)? = null, loading: Boolean = false, onRetry: (() -> Unit)? = null, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(LocalPageBg.current ?: V3.Bg).then(if (wide) Modifier else Modifier.systemBarsPadding()).padding(6.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = V3.Tx) }
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loading) androidx.compose.material3.CircularProgressIndicator(Modifier.size(14.dp), color = V3.Tx3, strokeWidth = 2.dp)
            Text(text, style = V3.Body.copy(color = V3.Tx2))
        }
        // app-attesa-dopo-scelta-17-11: una voce che non c'è più non lascia senza via: la home. Lenta o fallita, anche Riprova.
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onRetry?.let { r -> androidx.compose.material3.FilledTonalButton(onClick = r, modifier = Modifier.handCursor()) { Text(stringResource(R.string.ti_aspetta_retry)) } }
            onHome?.let { h -> androidx.compose.material3.FilledTonalButton(onClick = h, modifier = Modifier.handCursor().testTag("ti-aspetta-home")) { Text(stringResource(R.string.ti_aspetta_home)) } }
        }
    }
}

/** Sul tablet la scheda sta al posto delle colonne, in un pannello come quello di una sessione: il grigio e il bordo delle schede. */
@Composable
fun TiAspettaColumn(content: @Composable () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxSize().padding(vertical = 10.dp).clip(shape).background(V3.S1).border(1.dp, V3.Line, shape)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalPageBg provides V3.S1) { content() }
    }
}

/** Contratto 1.149 (app-consiglio-carte-16-11): il consiglio di chi chiede, con la motivazione, sotto la domanda; senza consiglio niente. */
@Composable
internal fun AdviceLine(advice: it.pixelbox.cmwatch.contract.WaitAdvice?, tag: String) {
    val a = TiAspetta.adviceOf(advice) ?: return
    Text(
        TiAspetta.adviceLine(a, stringResource(R.string.ti_aspetta_advice), stringResource(R.string.ti_aspetta_advice_yes), stringResource(R.string.ti_aspetta_advice_no)),
        style = V3.Body.copy(fontSize = 15.sp, color = V3.Wait), modifier = Modifier.testTag(tag),
    )
}
