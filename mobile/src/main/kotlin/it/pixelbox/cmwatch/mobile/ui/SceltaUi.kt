package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.WaitFile
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Scelta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Chi dà i file delle richieste (contratto 1.137, app-scheda-scelta-12-11): il file sul telefono se c'è (chiave `Scelta.fileKey`), quelli
 * in arrivo, quelli che il PC non ha dato, la richiesta in silenzio (l'anteprima che nasce da sola) e l'apertura con l'app di sistema.
 * Le stesse vie dei file della chat: la cache sul disco e la fila dei file della sessione; un file dichiarato non ha sessione.
 */
data class WaitFilesHost(
    val local: Map<String, String> = emptyMap(),
    val loading: Set<String> = emptySet(),
    val missed: Set<String> = emptySet(),
    val ask: (WaitFile) -> Unit = {},
    val open: (WaitFile) -> Unit = {},
    /** Gli snapshot (Paparazzi): le immagini si leggono subito, senza EXIF e da un flusso; fuori dal thread arriverebbero dopo la foto. */
    val decodeNow: Boolean = false,
)
val LocalWaitFiles = compositionLocalOf { WaitFilesHost() }

/** Una scheda letta dal PC con `attesa` e `archive`: la voce, o Gone quando il PC non la conosce più (o è un relay di prima). */
sealed interface WaitCardRead {
    data class Got(val item: WaitItem) : WaitCardRead
    data object Gone : WaitCardRead
}

/**
 * Chi dà le schede compatte delle chat e della home: la scheda letta (null = in arrivo), la lettura (una volta per id, poi la cache) e
 * l'apertura. Senza (gli snapshot che non la danno) le schede compatte non ci sono.
 */
class WaitCardsHost(val read: (String) -> WaitCardRead?, val ask: (String) -> Unit, val open: (String) -> Unit)
val LocalWaitCards = staticCompositionLocalOf<WaitCardsHost?> { null }

private val HM = DateTimeFormatter.ofPattern("HH:mm")

/** «oggi alle 16:57» o «il 12/11 alle 16:57». */
@Composable
internal fun sceltaWhen(at: Long): String {
    val t = Instant.ofEpochSecond(at).atZone(ZoneId.systemDefault())
    val hm = HM.format(t)
    return if (t.toLocalDate() == LocalDate.now()) stringResource(R.string.scelta_today, hm) else stringResource(R.string.scelta_on, "${t.dayOfMonth}/${t.monthValue}", hm)
}

/** Il fondo neutro delle cornici: un gradino sopra la superficie su cui stanno, di notte e di giorno. */
private val Mat: Color @Composable get() = V3.Tx.copy(alpha = 0.07f).compositeOver(V3.S1)

/**
 * Un'immagine di una richiesta nella sua cornice (Franz 12/11 15:03: «Un link a una sezione che non mostra immagini ma che mostra solo il
 * testo non è di aiuto»): intera su un fondo neutro, con la lettera dell'opzione in una fascia sopra, così l'immagine non è mai coperta;
 * un tocco la ingrandisce. Si chiede al PC appena la cornice è in pagina; finché non arriva la cornice dice che è in arrivo, e se non
 * arriva lo dice col tasto per richiederla. `small`: le miniature, senza parole; `strip` (la scheda chiusa, scelta A di Franz del 12/11
 * alle 17:46): il nome dell'opzione nella fascia e di una schermata alta la parte in cima. `ratio` dà la forma dalla proporzione
 * dell'immagine (null finché non c'è).
 */
@Composable
internal fun WaitImage(
    file: WaitFile, modifier: Modifier = Modifier, letter: String? = null, name: String = "", small: Boolean = false, strip: Boolean = false,
    picked: Boolean = false, ratio: (Float?) -> Float = { r -> Scelta.frameRatio(listOf(r)) }, maxHeight: Dp = Dp.Infinity,
    onRatio: (Float) -> Unit = {}, onOpen: (() -> Unit)? = null,
) {
    val host = LocalWaitFiles.current
    val key = Scelta.fileKey(file)
    val local = host.local[key]
    LaunchedEffect(key) { if (host.local[key] == null && key !in host.loading && key !in host.missed) host.ask(file) }
    // Negli snapshot (Paparazzi) l'immagine si legge subito: fuori dal thread dell'interfaccia arriverebbe dopo la foto.
    val bmp = if (host.decodeNow || androidx.compose.ui.platform.LocalInspectionMode.current) remember(local) { local?.let { p -> decodePlain(p, if (small) 720 else 1600) } }
        else produceState<ImageBitmap?>(null, local) { value = local?.let { p -> withContext(Dispatchers.IO) { decodeScaled(p, if (small) 720 else 1600) } } }.value
    val own = bmp?.let { b -> b.width.toFloat() / b.height.coerceAtLeast(1) }
    androidx.compose.runtime.SideEffect { own?.let(onRatio) }
    val shape = RoundedCornerShape(if (small) 8.dp else 12.dp)
    Column(
        modifier.clip(shape).background(Mat).then(if (picked) Modifier.border(2.dp, V3.Good, shape) else Modifier).testTag("attesa-immagine"),
    ) {
        if (letter != null) Row(
            Modifier.fillMaxWidth().height(if (strip) 26.dp else if (small) 18.dp else 30.dp).padding(horizontal = if (small && !strip) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(letter, style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.Bold, fontSize = if (small && !strip) 12.sp else if (strip) 14.sp else 17.sp, color = if (picked) V3.Good else V3.Tx2), maxLines = 1)
            if (name.isNotEmpty()) Text(name, style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = V3.Tx), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
        Box(Modifier.frameBox(ratio(own), maxHeight)) {
            val b = bmp
            if (b != null) {
                val fill = Modifier.fillMaxSize()
                androidx.compose.foundation.Image(
                    b, if (onOpen == null) name.ifEmpty { file.name } else null,
                    contentScale = if (strip) ContentScale.Crop else ContentScale.Fit, alignment = if (strip) Alignment.TopCenter else Alignment.Center,
                    modifier = if (onOpen != null) fill.handCursor().clickable(onClickLabel = stringResource(R.string.scelta_zoom, letter ?: file.name)) { onOpen() } else fill,
                )
            } else if (!small) Column(
                Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    key in host.missed -> {
                        Text(stringResource(R.string.attesa_preview_missed), style = V3.Small, textAlign = TextAlign.Center)
                        V3Pill(stringResource(R.string.attesa_preview_retry)) { host.ask(file) }
                    }
                    key in host.loading -> Text(stringResource(R.string.attesa_preview_coming), style = V3.Small, textAlign = TextAlign.Center)
                    else -> Text(file.name, style = V3.Small, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/**
 * La cornice di un'immagine: tutta la larghezza, alta quanto vuole la proporzione `ratio` ma non oltre `maxHeight`. A misura esplicita:
 * `aspectRatio` con un'altezza massima più bassa misura il contenuto più alto della cornice, e l'immagine ne usciva coprendo la fascia
 * (le colonne del tablet).
 */
private fun Modifier.frameBox(ratio: Float, maxHeight: Dp): Modifier = this.fillMaxWidth().layout { m, c ->
    val w = if (c.hasBoundedWidth) c.maxWidth else 0
    val cap = if (maxHeight == Dp.Infinity) Int.MAX_VALUE else maxHeight.roundToPx()
    val h = minOf((w / ratio.coerceAtLeast(0.1f)).toInt(), cap).coerceIn(c.minHeight, c.maxHeight)
    val p = m.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
    layout(w, h) { p.place(0, 0) }
}

/** Un documento di una richiesta: una riga col suo nome e la misura; un tocco lo apre con l'app di sistema. */
@Composable
private fun WaitDoc(f: WaitFile, modifier: Modifier = Modifier) {
    val host = LocalWaitFiles.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp).clip(SubShape).handCursor().clickable { host.open(f) },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(if (f.mime == "application/pdf") Icons.Rounded.PictureAsPdf else Icons.AutoMirrored.Rounded.InsertDriveFile, null, tint = V3.Tx2, modifier = Modifier.size(18.dp))
        Text(f.name, style = V3.Body.copy(fontSize = 15.sp, color = V3.Tx, textDecoration = TextDecoration.Underline), modifier = Modifier.weight(1f, fill = false))
        f.size?.let { b -> Text(if (b >= 1_000_000) "%.1f MB".format(b / 1_000_000.0) else if (b >= 1000) "${b / 1000} KB" else "$b B", style = V3.Meter) }
    }
}

/** Il tasto di un'opzione: tonale; pieno (il velo ambra dei Tasti B) solo quello dell'opzione consigliata; confermato, la spunta. */
@Composable
private fun ChoiceButton(text: String, filled: Boolean, done: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val bg = if (done) CheckFill else if (filled) V3.Wait.copy(alpha = 0.14f) else V3.S3
    val fg = if (filled && !done) V3.Wait else V3.Tx
    Surface(
        onClick = onClick, enabled = enabled, interactionSource = press, shape = CircleShape, color = bg, contentColor = fg,
        border = if (done) BorderStroke(1.dp, CheckLine) else if (filled) BorderStroke(1.dp, V3.Wait.copy(alpha = 0.6f)) else null,
        modifier = modifier.pressScale(press).heightIn(min = 44.dp).alpha(if (!enabled && !done) 0.45f else 1f),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
            CheckLabel(done) { Text(text, style = V3.Name.copy(fontSize = 15.sp, color = fg), maxLines = 1, softWrap = false) }
        }
    }
}

/** Una riga sottile fra le opzioni e le sezioni. */
@Composable
private fun Rule() = Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))

/** Lo stato a parole di una scheda chiusa o dall'archivio: null finché aspetta. */
@Composable
private fun outcomeText(o: Scelta.Outcome): String? = when (o.state) {
    Scelta.State.CHOSEN -> o.letter?.let { l -> o.name?.takeIf { it.isNotEmpty() }?.let { n -> stringResource(R.string.scelta_chosen, l, n) } ?: stringResource(R.string.scelta_chosen_letter, l) }
        ?: stringResource(R.string.scelta_chosen_words, o.name.orEmpty())
    Scelta.State.APPROVED -> stringResource(R.string.scelta_approved)
    Scelta.State.REDO -> stringResource(R.string.scelta_redo)
    Scelta.State.CLOSED -> stringResource(R.string.scelta_closed)
    Scelta.State.OPEN -> null
}

/**
 * La scheda di scelta aperta (app-scheda-scelta-12-11, descrizione approvata da Franz il 12/11 alle 15:10): la descrizione vera, le
 * opzioni una accanto all'altra, ognuna con la sua immagine, la riga di pro e contro e il suo tasto; le immagini si ingrandiscono al
 * tocco e nel visore si passa dall'una all'altra; in fondo «Nessuna, rifare» col motivo, obbligatorio. Un giro nuovo entra nella stessa
 * scheda e i precedenti restano sotto col motivo per cui sono stati scartati. Dall'archivio (dopo la risposta) non ha tasti e dice
 * com'è finita. Stretta: le cornici affiancate in alto e una riga intera per opzione, così il testo non finisce in colonne strette;
 * larga (misurata sulla scheda: il tablet), ogni opzione è una colonna coi tasti allineati in fondo. Le regole in `Scelta`, coi casi
 * condivisi con la web; `rows` i campi della carta (cosa, dove, non provato, annullare). `onSend` riceve il tasto e il testo scritto.
 */
@Composable
fun SceltaAperta(item: WaitItem, rows: List<Pair<Int, String>>, onSend: (Int, String?) -> Unit) {
    var sent by rememberSaveable(item.id) { mutableStateOf<Int?>(null) }
    var writing by rememberSaveable(item.id) { mutableStateOf<Int?>(null) }
    var written by rememberSaveable(item.id) { mutableStateOf("") }
    val acts = item.actions.orEmpty()
    val c = remember(item) { Scelta.choiceOf(item) }
    val out = remember(item) { Scelta.outcomeOf(item) }
    val rounds = remember(item) { Scelta.roundsOf(item) }
    val ratios = remember(item.id) { mutableStateMapOf<String, Float>() }
    val ar = Scelta.frameRatio(c.options.flatMap { it.images }.map { ratios[Scelta.fileKey(it)] })
    val host = LocalWaitFiles.current
    var zoom by remember { mutableStateOf<Pair<List<Pair<String, String>>, Int>?>(null) }
    val screenH = LocalConfiguration.current.screenHeightDp
    val cap = (screenH * if (c.options.size >= 3) 0.34f else 0.4f).dp.coerceAtMost(480.dp)
    fun tap(i: Int) {
        if (sent != null) return
        val a = acts.getOrNull(i) ?: return
        if (a.textField != null) { writing = i; written = ""; return }
        sent = i; onSend(i, null)
    }
    val isRedo = writing != null && writing == c.redo
    val ready = writing?.let { i -> if (i == c.redo) Scelta.redoReady(written) else written.isNotBlank() || acts.getOrNull(i)?.textField == "text" } == true
    fun send() { val i = writing ?: return; if (!ready || sent != null) return; writing = null; sent = i; onSend(i, written) }
    // Il visore: le immagini già arrivate di un giro, nell'ordine delle opzioni, ognuna col suo nome.
    fun enlarge(options: List<Scelta.Opt>, from: WaitFile, prefix: String? = null) {
        val shots = options.flatMap { o -> o.images.map { f -> f to listOfNotNull(prefix, listOf(o.letter, o.name).filter { it.isNotEmpty() }.joinToString(", "), f.name.takeIf { o.images.size > 1 }).joinToString(" · ") } }
        val readyShots = shots.mapNotNull { (f, label) -> host.local[Scelta.fileKey(f)]?.let { p -> Triple(f, p, label) } }
        val start = readyShots.indexOfFirst { it.first == from }
        if (start >= 0) zoom = readyShots.map { it.second to it.third } to start
    }
    Column(Modifier.fillMaxWidth().testTag("scelta-aperta"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title?.takeIf { it.isNotBlank() } ?: item.text, style = V3.Section.copy(fontSize = 22.sp, lineHeight = 26.sp))
            val said = outcomeText(out)
            if (said != null) {
                val tone = when (out.state) { Scelta.State.CHOSEN, Scelta.State.APPROVED -> V3.Good; Scelta.State.REDO -> V3.Wait; else -> V3.Tx2 }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("scelta-esito")) {
                    if (tone == V3.Good) Icon(Icons.Rounded.Check, null, tint = tone, modifier = Modifier.size(18.dp))
                    Text(said, style = V3.Name.copy(fontSize = 16.sp, color = tone))
                }
                out.reason?.let { Text(it, style = V3.Body.copy(color = V3.Tx)) }
            } else if (rounds.isNotEmpty() || item.card?.deploy == true) FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (rounds.isNotEmpty()) Text(stringResource(R.string.scelta_round, rounds.size + 1), style = V3.Name.copy(fontSize = 16.sp, color = V3.Tx2))
                if (item.card?.deploy == true) Text(stringResource(R.string.scelta_production), style = V3.Name.copy(fontSize = 16.sp, color = V3.Wait))
            }
        }
        val title = item.title?.takeIf { it.isNotBlank() } ?: item.text
        if (item.text.isNotBlank() && item.text != title) Text(item.text, style = V3.Body.copy(fontSize = 17.sp, lineHeight = 24.sp))
        item.description?.takeIf { it.isNotBlank() && it != item.text }?.let { Text(it, style = V3.Body.copy(color = V3.Tx2, lineHeight = 23.sp)) }
        if (rows.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            rows.forEach { (k, v) ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(k), style = V3.Small.copy(color = V3.Tx3), modifier = Modifier.width(96.dp))
                    Text(v, style = V3.Small.copy(color = V3.Tx), modifier = Modifier.weight(1f))
                }
            }
        }
        val hasPics = c.options.any { it.images.isNotEmpty() }
        @Composable fun frame(o: Scelta.Opt, modifier: Modifier = Modifier) {
            val chosen = out.state == Scelta.State.CHOSEN && out.letter == o.letter
            Box(modifier) {
                WaitImage(
                    o.images[0], letter = o.letter, picked = chosen, ratio = { ar }, maxHeight = cap,
                    onRatio = { r -> ratios[Scelta.fileKey(o.images[0])] = r }, onOpen = { enlarge(c.options, o.images[0]) },
                )
                if (o.images.size > 1) Box(
                    Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(V3.S1.copy(alpha = 0.9f))
                        .handCursor().clickable { enlarge(c.options, o.images[1]) }.padding(horizontal = 8.dp, vertical = 4.dp),
                ) { Text("+${o.images.size - 1}", style = V3.Name.copy(fontSize = 13.sp), modifier = Modifier.testTag("scelta-altre-${o.letter}")) }
            }
        }
        @Composable fun button(o: Scelta.Opt, modifier: Modifier = Modifier) {
            val chosen = out.state == Scelta.State.CHOSEN && out.letter == o.letter
            val act = o.action
            if (act != null) Box(modifier.testTag("scelta-scegli-${o.letter}")) {
                ChoiceButton(stringResource(R.string.scelta_pick, o.letter), filled = o.recommended && writing == null, done = sent == act, enabled = sent == null, modifier = modifier) { tap(act) }
            } else if (chosen) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Check, null, tint = V3.Good, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.scelta_picked), style = V3.Name.copy(fontSize = 15.sp, color = V3.Good))
            }
        }
        @Composable fun nameOf(o: Scelta.Opt, dim: Boolean, modifier: Modifier = Modifier) {
            FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (o.name.isNotEmpty()) Text(o.name, style = V3.Name.copy(color = if (dim) V3.Tx2 else V3.Tx))
                if (o.recommended) Text(stringResource(R.string.ti_aspetta_recommended), style = V3.Body.copy(fontSize = 14.sp, color = V3.Wait))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val n = c.options.size
            val wide = maxWidth >= when { n <= 2 -> 560.dp; n == 3 -> 780.dp; else -> 980.dp }
            if (wide) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                c.options.forEach { o ->
                    val dim = out.state == Scelta.State.CHOSEN && out.letter != null && out.letter != o.letter
                    Column(Modifier.weight(1f).fillMaxHeight().testTag("scelta-opzione-${o.letter}")) {
                        if (o.images.isNotEmpty()) frame(o, Modifier.alpha(if (dim) 0.55f else 1f))
                        else if (hasPics) Spacer(Modifier.padding(top = 30.dp).frameBox(ar, cap))
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(o.letter, style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp, color = V3.Tx2), modifier = Modifier.width(18.dp))
                            nameOf(o, dim, Modifier.weight(1f))
                        }
                        o.line?.let { Text(it, style = V3.Body.copy(fontSize = 15.sp, color = V3.Tx2), modifier = Modifier.padding(start = 28.dp, top = 4.dp)) }
                        o.docs.forEach { f -> WaitDoc(f, Modifier.padding(start = 28.dp)) }
                        Spacer(Modifier.weight(1f).heightIn(min = 14.dp))
                        button(o, Modifier.fillMaxWidth())
                    }
                }
            } else Column(Modifier.fillMaxWidth()) {
                if (hasPics) {
                    val cols = if (n >= 4) 2 else n
                    c.options.filter { it.images.isNotEmpty() }.chunked(cols).forEach { line ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            line.forEach { o ->
                                val dim = out.state == Scelta.State.CHOSEN && out.letter != null && out.letter != o.letter
                                frame(o, Modifier.weight(1f).alpha(if (dim) 0.55f else 1f))
                            }
                            repeat(cols - line.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                c.options.forEachIndexed { i, o ->
                    val dim = out.state == Scelta.State.CHOSEN && out.letter != null && out.letter != o.letter
                    if (hasPics || i > 0) Rule()
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp).testTag("scelta-opzione-${o.letter}")) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(o.letter, style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = V3.Tx2), modifier = Modifier.width(26.dp))
                            nameOf(o, dim, Modifier.weight(1f))
                            button(o)
                        }
                        o.line?.let { Text(it, style = V3.Body.copy(fontSize = 15.sp, color = V3.Tx2), modifier = Modifier.padding(start = 36.dp, top = 2.dp)) }
                        o.docs.forEach { f -> WaitDoc(f, Modifier.padding(start = 36.dp)) }
                    }
                }
            }
        }
        if (c.extra.isNotEmpty()) WaitFilesBlock(c.extra)
        writing?.let { i ->
            val a = acts.getOrNull(i) ?: return@let
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Rule()
                Text(if (isRedo) stringResource(R.string.scelta_reason_label) else a.label, style = V3.Name, modifier = Modifier.padding(top = 4.dp))
                if (isRedo) Text(stringResource(R.string.scelta_reason_hint), style = V3.Small)
                OutlinedTextField(
                    written, { v -> written = if (isRedo) v.take(Scelta.REDO_MAX + 40) else v.take(2000) },
                    modifier = Modifier.fillMaxWidth().testTag("scelta-motivo"), minLines = 3, maxLines = 6, textStyle = V3.Body.copy(fontSize = 16.sp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val left = Scelta.REDO_MAX - written.trim().length
                    if (isRedo && left <= 60) Text(pluralStringResource(R.plurals.scelta_left, maxOf(0, left), maxOf(0, left)), style = V3.Meter.copy(color = if (left < 0) V3.Red else V3.Tx2))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.handCursor().clip(CircleShape).clickable { writing = null }.padding(horizontal = 12.dp, vertical = 11.dp)) {
                        Text(stringResource(R.string.cancel), style = V3.Name.copy(fontSize = 15.sp, color = V3.Tx2))
                    }
                    Box(Modifier.testTag("scelta-manda")) { ChoiceButton(stringResource(R.string.ti_aspetta_send), filled = true, done = false, enabled = ready && sent == null) { send() } }
                }
            }
        } ?: run {
            if (c.redo != null || c.rest.isNotEmpty()) Column {
                Rule()
                FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    c.redo?.let { r ->
                        Surface(
                            onClick = { tap(r) }, enabled = sent == null, shape = CircleShape, color = Color.Transparent, contentColor = V3.Tx,
                            border = BorderStroke(1.dp, V3.Tx3), modifier = Modifier.heightIn(min = 44.dp).alpha(if (sent != null) 0.45f else 1f).testTag("scelta-rifare"),
                        ) { Box(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) { Text(acts[r].label, style = V3.Name.copy(fontSize = 15.sp)) } }
                    }
                    c.rest.forEach { i ->
                        Box(
                            Modifier.heightIn(min = 44.dp).clip(CircleShape).then(if (sent == null) Modifier.handCursor().clickable { tap(i) } else Modifier)
                                .alpha(if (sent != null && sent != i) 0.45f else 1f).padding(horizontal = 12.dp, vertical = 11.dp).testTag("scelta-tasto-$i"),
                        ) { Text(acts[i].label, style = V3.Name.copy(fontSize = 15.sp, color = if (sent == i) V3.Good else V3.Tx2)) }
                    }
                }
            }
        }
        if (rounds.isNotEmpty()) Column(Modifier.padding(top = 10.dp).testTag("scelta-giri")) {
            Text(stringResource(R.string.scelta_rounds), style = V3.Section, modifier = Modifier.padding(bottom = 8.dp))
            rounds.forEach { r ->
                Rule()
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("scelta-giro-${r.n}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val roundName = stringResource(R.string.scelta_round, r.n)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(roundName, style = V3.Name.copy(fontSize = 16.sp))
                        Text(sceltaWhen(r.answeredAt ?: r.at), style = V3.Meter)
                    }
                    val why = stringResource(when (r.state) { "redo" -> R.string.scelta_round_redo; "replaced" -> R.string.scelta_round_replaced; else -> R.string.scelta_round_closed })
                    Text(buildAnnotatedString { withStyle(SpanStyle(color = V3.Tx2)) { append(why) }; r.reason?.let { append(": "); append(it) } }, style = V3.Body.copy(fontSize = 15.sp))
                    (r.description ?: r.text).takeIf { it.isNotBlank() }?.let { Text(it, style = V3.Small) }
                    val shown = r.options.filter { it.images.isNotEmpty() }
                    if (shown.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shown.forEach { o -> WaitImage(o.images[0], Modifier.width(96.dp), letter = o.letter, small = true, onOpen = { enlarge(r.options, o.images[0], roundName) }) }
                    }
                    Text(buildAnnotatedString {
                        r.options.forEachIndexed { i, o ->
                            if (i > 0) append(" · ")
                            withStyle(SpanStyle(color = V3.Tx, fontWeight = FontWeight.SemiBold)) { append(o.letter) }
                            if (o.name.isNotEmpty()) append(" ${o.name}")
                        }
                    }, style = V3.Small)
                }
            }
        }
    }
    zoom?.let { (items, start) -> WaitImagesViewer(items, start) { zoom = null } }
}

/**
 * I file di una richiesta che non stanno in un'opzione (contratto 1.137): le immagini di cosa guardare di una verifica, gli allegati di
 * un ok, i file in più di una scelta. Le immagini nelle loro cornici, affiancate due a due, e si ingrandiscono al tocco (nel visore si
 * passa dall'una all'altra); i documenti una riga l'uno, un tocco li apre.
 */
@Composable
fun WaitFilesBlock(files: List<WaitFile>) {
    val parts = remember(files) { Scelta.splitFiles(files) }
    val ratios = remember(files) { mutableStateMapOf<String, Float>() }
    val ar = Scelta.frameRatio(parts.images.map { ratios[Scelta.fileKey(it)] })
    val host = LocalWaitFiles.current
    var zoom by remember { mutableStateOf<Pair<List<Pair<String, String>>, Int>?>(null) }
    val cap = (LocalConfiguration.current.screenHeightDp * 0.4f).dp.coerceAtMost(480.dp)
    fun enlarge(f: WaitFile) {
        val ready = parts.images.mapNotNull { x -> host.local[Scelta.fileKey(x)]?.let { p -> Triple(x, p, x.name) } }
        val start = ready.indexOfFirst { it.first == f }
        if (start >= 0) zoom = ready.map { it.second to it.third } to start
    }
    Column(Modifier.fillMaxWidth().testTag("attesa-file"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (parts.images.size == 1) {
            val f = parts.images[0]
            WaitImage(f, Modifier.widthIn(max = 520.dp).fillMaxWidth(), ratio = { ar }, maxHeight = cap, onRatio = { r -> ratios[Scelta.fileKey(f)] = r }, onOpen = { enlarge(f) })
        } else parts.images.chunked(2).forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                line.forEach { f -> WaitImage(f, Modifier.weight(1f), ratio = { ar }, maxHeight = cap, onRatio = { r -> ratios[Scelta.fileKey(f)] = r }, onOpen = { enlarge(f) }) }
                if (line.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        parts.docs.forEach { f -> WaitDoc(f) }
    }
    zoom?.let { (items, start) -> WaitImagesViewer(items, start) { zoom = null } }
}

/**
 * Il visore delle immagini di una richiesta: a tutto schermo, una pagina per immagine; da intera il dito la trascina di lato e passa alla
 * vicina, i due tasti fanno lo stesso; ingrandita (due dita o doppio tocco) il dito la sposta. In alto il nome dell'opzione e «1 di 2».
 */
@Composable
fun WaitImagesViewer(items: List<Pair<String, String>>, start: Int, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect { dialogWindow?.setWindowAnimations(0); dialogWindow?.setDimAmount(0f) }
        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = start.coerceIn(0, (items.size - 1).coerceAtLeast(0))) { items.size }
        var zoomed by remember { mutableStateOf(false) }
        LaunchedEffect(pager.currentPage) { zoomed = false }
        val scope = rememberCoroutineScope()
        Box(Modifier.fillMaxSize().background(Cm.bg).testTag("visore-richiesta")) {
            androidx.compose.foundation.pager.HorizontalPager(pager, userScrollEnabled = !zoomed, modifier = Modifier.fillMaxSize()) { page ->
                val path = items[page].first
                val bmp by produceState<ImageBitmap?>(null, path) { value = withContext(Dispatchers.IO) { decodeScaled(path, 4096, 12_000_000L) ?: decodePlain(path, 4096) } }
                bmp?.let { b -> ZoomableImage(b, null, {}, onZoom = { z -> if (page == pager.currentPage) zoomed = z }) }
            }
            Row(
                Modifier.fillMaxWidth().systemBarsPadding().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(items.getOrNull(pager.currentPage)?.second.orEmpty(), style = V3.Name.copy(fontSize = 16.sp), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
                if (items.size > 1) Text(stringResource(R.string.viewer_at, pager.currentPage + 1, items.size), style = V3.Meter)
                IconButton(onClick = onClose, modifier = Modifier.handCursor()) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Cm.text) }
            }
            if (items.size > 1) {
                @Composable fun step(to: Int, left: Boolean) {
                    if (to !in items.indices) return
                    Box(
                        Modifier.align(if (left) Alignment.CenterStart else Alignment.CenterEnd).padding(10.dp).size(48.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)).handCursor().clickable { scope.launch { pager.animateScrollToPage(to) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (left) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            stringResource(if (left) R.string.viewer_prev else R.string.viewer_next), tint = Color.White,
                        )
                    }
                }
                step(pager.currentPage - 1, true)
                step(pager.currentPage + 1, false)
            }
        }
    }
}

/**
 * Le richieste come schede compatte (app-scheda-scelta-12-11: in chat al posto del loro link, in home e in Lavori finché aspettano):
 * il titolo e lo stato a parole sulla prima riga («Da scegliere» in ambra, «Scelto B» in verde), cosa decidere su una riga, poi le
 * immagini delle opzioni che si dividono la larghezza, ognuna con la lettera e il nome nella fascia sopra (scelta A di Franz, 12/11
 * 17:46); dopo la scelta quella scelta ha il bordo verde e le altre si spengono. Un tocco apre la scheda. Una richiesta che il PC non
 * conosce più resta un link. `flat`: dentro una riga della home, senza bordo né fondo.
 */
@Composable
fun SceltaCompatte(ids: List<String>, flat: Boolean = false) {
    val host = LocalWaitCards.current ?: return
    ids.forEach { id ->
        LaunchedEffect(id) { host.ask(id) }
        when (val r = host.read(id)) {
            WaitCardRead.Gone -> Text(
                stringResource(R.string.scelta_open_card), style = V3.Body.copy(color = V3.Tx, textDecoration = TextDecoration.Underline),
                modifier = Modifier.padding(top = 8.dp).handCursor().clickable { host.open(id) }.testTag("scelta-link-$id"),
            )
            else -> CompactCard(id, (r as? WaitCardRead.Got)?.item, flat) { host.open(id) }
        }
    }
}

@Composable
private fun CompactCard(id: String, w: WaitItem?, flat: Boolean, onOpen: () -> Unit) {
    val shape = V3.Shape
    Column(
        Modifier.fillMaxWidth().padding(top = if (flat) 6.dp else 8.dp, bottom = if (flat) 12.dp else 0.dp)
            .then(if (flat) Modifier else Modifier.clip(shape).background(V3.S1).border(1.dp, V3.Line, shape))
            .handCursor().clickable(onClick = onOpen)
            .then(if (flat) Modifier else Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
            .testTag("scelta-scheda-$id"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (w == null) { Text(stringResource(R.string.scelta_card_loading), style = V3.Small); return@Column }
        val o = Scelta.outcomeOf(w)
        val good = o.state == Scelta.State.CHOSEN || o.state == Scelta.State.APPROVED
        val tone = when { good -> V3.Good; o.state == Scelta.State.CLOSED -> V3.Tx2; else -> V3.Wait }
        val state = outcomeText(o) ?: stringResource(
            when (w.label) { "choose" -> R.string.scelta_open_choose; "verify" -> R.string.scelta_open_verify; "reply" -> R.string.scelta_open_answer; else -> R.string.scelta_open_decide },
        )
        val shortState = if (o.state == Scelta.State.CHOSEN) o.letter?.let { stringResource(R.string.scelta_chosen_letter, it) } ?: state else state
        val head = w.title?.takeIf { it.isNotBlank() } ?: w.text
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(head, style = V3.Name, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (good) Icon(Icons.Rounded.Check, null, tint = tone, modifier = Modifier.size(15.dp))
                Text(shortState, style = V3.Name.copy(fontSize = 14.sp, color = tone), maxLines = 1, softWrap = false)
            }
        }
        listOfNotNull(w.text, w.description).firstOrNull { it.isNotBlank() && it != head }?.let {
            Text(it, style = V3.Body.copy(fontSize = 14.5.sp, color = V3.Tx2), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
        val thumbs = remember(w) { Scelta.thumbsOf(w) }
        if (thumbs.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
            thumbs.forEach { t ->
                val picked = o.state == Scelta.State.CHOSEN && o.letter != null && t.letter == o.letter
                val dim = o.state == Scelta.State.CHOSEN && o.letter != null && t.letter != null && t.letter != o.letter
                WaitImage(
                    t.file, Modifier.weight(1f).alpha(if (dim) 0.5f else 1f), letter = t.letter, name = t.name, small = true, strip = true, picked = picked,
                    ratio = { r -> Scelta.stripRatio(r) },
                )
            }
        }
    }
}

/** I campi della carta di una richiesta (cosa, dove, non provato, annullare), senza quelli uguali al testo: come nella carta coi tasti. */
internal fun waitRows(item: WaitItem): List<Pair<Int, String>> = item.card?.let { c ->
    listOfNotNull(
        c.what?.let { R.string.ti_aspetta_what to it }, c.where?.let { R.string.ti_aspetta_where to it },
        c.untested?.let { R.string.ti_aspetta_untested to it }, (c.undoHow ?: c.undo)?.let { R.string.ti_aspetta_undo to it },
    ).filter { (_, v) -> v.isNotBlank() && v != item.text }
}.orEmpty()

/**
 * Una scelta che aspetta sta con la sua scheda compatta in home, nella scheda della sessione e in Lavori, finché non rispondi
 * (app-scheda-scelta-12-11, punto 3): un ok con opzioni che ha la sua richiesta (`wid`, contratto 1.130) e che il PC sa ancora leggere.
 * Gli altri ok restano la riga coi tasti di prima. Torna l'id della richiesta, o null.
 */
@Composable
internal fun choiceCardOf(a: it.pixelbox.cmwatch.contract.Approval?): String? {
    val host = LocalWaitCards.current ?: return null
    val wid = a?.wid ?: return null
    if (it.pixelbox.cmwatch.rules.Approvals.choices(a).isEmpty()) return null
    return wid.takeIf { w -> host.read(w) !is WaitCardRead.Gone }
}

/**
 * La lettura semplice di un'immagine, senza la rotazione dell'EXIF, da un flusso: negli snapshot (Paparazzi) né l'EXIF né la lettura per
 * percorso funzionano.
 */
private fun decodePlain(path: String, max: Int): ImageBitmap? = runCatching {
    val f = java.io.File(path)
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    f.inputStream().use { s -> android.graphics.BitmapFactory.decodeStream(s, null, bounds) }
    var sample = 1
    while (bounds.outWidth > 0 && maxOf(bounds.outWidth, bounds.outHeight) / sample > max) sample *= 2
    f.inputStream().use { s -> android.graphics.BitmapFactory.decodeStream(s, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }) }?.asImageBitmap()
}.getOrNull()
