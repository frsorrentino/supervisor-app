package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import it.pixelbox.cmwatch.contract.ResultGroup
import it.pixelbox.cmwatch.contract.ResultItem
import it.pixelbox.cmwatch.contract.ResultsPage
import it.pixelbox.cmwatch.contract.TranscriptFile
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Guardare

/**
 * «Da guardare» (app-da-guardare-02-11, mockup della master delle 08:16 approvato da Franz alle 16:44; contratto 1.106): i file che
 * le sessioni hanno consegnato. In home le ultime versioni non viste con Visto e Nota; la pagina coi lavori e le versioni
 * precedenti (anche nella sessione, dalla riga «Risultati»); il risultato aperto col lettore della chat.
 */
class GuardareActions(
    /**
     * Il file sul telefono (dalla cache o chiesto al PC con l'op `file`); `quiet` = una miniatura, niente avvisi se non arriva.
     * Il percorso, o null se il file non è arrivato: il visore lo dice e offre «Riprova» invece di girare per sempre.
     */
    val file: (ResultItem, TranscriptFile, Boolean, (String?) -> Unit) -> Unit = { _, _, _, _ -> },
    val seen: (ResultItem) -> Unit = {},
    val note: (ResultItem, String, Double?) -> Unit = { _, _, _ -> },
    val open: (ResultItem) -> Unit = {},
    val session: (String) -> Unit = {},
    /** Un documento o una pagina html: all'app di sistema. */
    val external: (ResultItem) -> Unit = {},
    /**
     * Contratto 1.136 (app-anteprime-verifica-11-11): l'anteprima di un documento, chiesta una volta per versione e poi in cache; null senza
     * anteprima o con un relay senza l'op `thumb`, e resta l'icona.
     */
    val preview: (ResultItem, (it.pixelbox.cmwatch.contract.ThumbPreview?) -> Unit) -> Unit = { _, _ -> },
)

/** L'anteprima di un documento (contratto 1.136): la prima pagina, le prime righe come una pagina, o null (resta l'icona). */
@Composable
private fun rememberPreview(i: ResultItem, a: GuardareActions): it.pixelbox.cmwatch.rules.Thumbs.Shown {
    val key = it.pixelbox.cmwatch.rules.Thumbs.key(i)
    var prev by remember(key) { mutableStateOf<it.pixelbox.cmwatch.contract.ThumbPreview?>(null) }
    LaunchedEffect(key) { if (it.pixelbox.cmwatch.rules.Thumbs.wants(i)) a.preview(i) { p -> prev = p } }
    return remember(prev) { it.pixelbox.cmwatch.rules.Thumbs.shown(prev) }
}

/** La casella con l'anteprima di un documento; `big` nella scheda aperta. Senza anteprima l'icona di oggi. */
@Composable
private fun PreviewBox(shown: it.pixelbox.cmwatch.rules.Thumbs.Shown, modifier: Modifier, big: Boolean = false) {
    val bmp = remember(shown) { (shown as? it.pixelbox.cmwatch.rules.Thumbs.Shown.Image)?.bytes?.let { b -> runCatching { android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull() } }
    val lines = (shown as? it.pixelbox.cmwatch.rules.Thumbs.Shown.Lines)?.text
    // Le prime righe su un foglio chiaro, come la prima pagina di un documento.
    Box(modifier.clip(RoundedCornerShape(if (big) 12.dp else 10.dp)).background(if (lines != null) V3.S1 else V3.S3).testTag("anteprima"), contentAlignment = if (lines != null) Alignment.TopStart else Alignment.Center) {
        when {
            bmp != null -> Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            lines != null -> Text(lines, style = V3.Small.copy(fontSize = if (big) 13.sp else 7.sp, lineHeight = if (big) 18.sp else 9.sp, color = V3.Tx2), modifier = Modifier.padding(if (big) 14.dp else 6.dp), overflow = TextOverflow.Clip)
            else -> Icon(Icons.Outlined.Description, null, tint = V3.Tx2, modifier = Modifier.size(28.dp))
        }
    }
}

/** Un file aperto altrove nell'app (la chat): chi lo apre lo dice, e i risultati con quel file diventano visti (app-risultati-visti-03-11). */
internal val LocalSeenPath = androidx.compose.runtime.compositionLocalOf<(String) -> Unit> { {} }

/** La riga sotto il titolo: «video-supervisor · video, 40 s · 06:38». */
@Composable
private fun sub(l: Guardare.Line, withSession: Boolean = true): String {
    val kind = stringResource(when (l.kind) {
        Guardare.Kind.VIDEO -> R.string.watch_kind_video; Guardare.Kind.MOCKUP -> R.string.watch_kind_mockup
        Guardare.Kind.IMAGE -> R.string.watch_kind_image; Guardare.Kind.DOCUMENT -> R.string.watch_kind_document; Guardare.Kind.OTHER -> R.string.watch_kind_other
    })
    val what = l.seconds?.let { "$kind, " + stringResource(R.string.watch_seconds, it) } ?: kind
    return listOfNotNull(l.session?.takeIf { withSession }, what, hhmmOf(l.at)).joinToString(" · ")
}

@Composable
private fun titleOf(l: Guardare.Line) = l.version?.let { stringResource(R.string.watch_title_version, l.title, it) } ?: l.title

/** La miniatura: l'immagine (o quella del mockup), altrimenti una tessera col segno del tipo; ▶ sopra i video. */
@Composable
private fun Thumb(i: ResultItem, a: GuardareActions, modifier: Modifier) {
    val t = Guardare.thumb(i)
    // Un documento senza immagine sua: la sua anteprima dal relay (app-anteprime-verifica-11-11), la stessa casella di immagini e mockup.
    if (t == null && it.pixelbox.cmwatch.rules.Thumbs.wants(i)) { PreviewBox(rememberPreview(i, a), modifier); return }
    var path by remember(i.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(i.id, t?.path) { if (t != null) a.file(i, t, true) { p -> if (p != null) path = p } }
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
        value = path?.let { p -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { decodeScaled(p, 480) } }
    }
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(V3.S3), contentAlignment = Alignment.Center) {
        bmp?.let { b -> Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter) }
        when (Guardare.kind(i.kind)) {
            Guardare.Kind.VIDEO -> Icon(Icons.Rounded.PlayCircleOutline, null, tint = V3.Tx, modifier = Modifier.size(34.dp))
            else -> if (bmp == null) Icon(Icons.Outlined.Description, null, tint = V3.Tx2, modifier = Modifier.size(28.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SeenNote(i: ResultItem, a: GuardareActions, seenFilled: Boolean = false) {
    var done by remember(i.id) { mutableStateOf(false) }
    // A capo interi quando non ci stanno (a 320 dp col testo grande «Nota» diventava «✎ I», app-bug-notte-03-11).
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (i.seenAt == null) V3Pill("✓ " + stringResource(R.string.watch_seen), filled = seenFilled, confirmed = done) { if (!done) { done = true; a.seen(i) } }
        V3Pill("✎ " + stringResource(R.string.watch_note)) { a.open(i) }
    }
}

/** Una voce di Da guardare: miniatura a sinistra, punto ambra se non vista, titolo, riga sotto, Visto e Nota. */
@Composable
internal fun ResultRow(i: ResultItem, a: GuardareActions, withSession: Boolean = true, title: String? = null) {
    val l = Guardare.line(i)
    Row(Modifier.fillMaxWidth().clickable { a.open(i) }.padding(vertical = 8.dp).testTag("risultato-${i.id}"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // La miniatura cede spazio sugli schermi stretti: al massimo 118 dp, mai più di un terzo della riga.
        Thumb(i, a, Modifier.fillMaxWidth(0.3f).widthIn(max = 118.dp).aspectRatio(118f / 74f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (l.unseen) Box(Modifier.size(9.dp).clip(CircleShape).background(V3.Wait))
                Text(title ?: titleOf(l), style = V3.Name, maxLines = 2, overflow = TextOverflow.Clip)
            }
            Text(sub(l, withSession), style = V3.Small, maxLines = 2)
            SeenNote(i, a)
        }
    }
}

/** Il corpo in home: le prime tre non viste e «Tutti i risultati». */
@Composable
fun GuardareBody(items: List<ResultItem>, a: GuardareActions, onAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        if (items.isEmpty()) Text(stringResource(R.string.watch_empty), style = V3.Small, modifier = Modifier.padding(vertical = 6.dp))
        items.take(3).forEachIndexed { n, i ->
            if (n > 0) HorizontalDivider(color = V3.Line)
            ResultRow(i, a)
        }
        HorizontalDivider(color = V3.Line)
        TextButton(onClick = onAll, modifier = Modifier.align(Alignment.CenterHorizontally).testTag("risultati-tutti")) { Text(stringResource(R.string.watch_all), color = V3.Act) }
    }
}

/** Un lavoro: il titolo, l'ultima versione con Visto e Nota, sotto le versioni precedenti con le note di Franz. */
@Composable
private fun GroupBlock(g: ResultGroup, a: GuardareActions) {
    val latest = g.items.firstOrNull() ?: return
    var olderOpen by rememberSaveable(g.title, g.session) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).v3Edge().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(g.title, style = V3.Section)
        HorizontalDivider(color = V3.Line)
        VersionRow(latest, a)
        val older = Guardare.older(g)
        if (older.isNotEmpty()) {
            // Area di tocco di almeno 48 dp (app-bug-notte-03-11).
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { olderOpen = !olderOpen }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (olderOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx2)
                Text(stringResource(R.string.watch_older) + "  " + older.size, style = V3.Small)
            }
            if (olderOpen) older.forEach { VersionRow(it, a) }
        }
    }
}

@Composable
private fun VersionRow(i: ResultItem, a: GuardareActions) {
    val l = Guardare.line(i)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().clickable { a.open(i) }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Thumb(i, a, Modifier.fillMaxWidth(0.3f).widthIn(max = 112.dp).aspectRatio(112f / 70f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (l.unseen) Box(Modifier.size(9.dp).clip(CircleShape).background(V3.Wait))
                    Text(stringResource(R.string.watch_version, i.version), style = V3.Name)
                }
                Text(listOfNotNull(hhmmOf(i.at), i.changed?.trim()?.takeIf { it.isNotEmpty() }, if (!l.unseen) stringResource(R.string.watch_seen_word) else null).joinToString(" · "), style = V3.Small)
                if (l.unseen) SeenNote(i, a)
            }
        }
        i.notes.forEach { n -> NoteLine(n.text, n.posS) }
    }
}

@Composable
private fun NoteLine(text: String, pos: Double?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Edit, null, tint = V3.Tx2, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Text(pos?.let { stringResource(R.string.watch_your_note_at, Guardare.pos(it), text) } ?: stringResource(R.string.watch_your_note, text), style = V3.Small)
    }
}

/** La pagina: tutti i lavori, o quelli di una sessione (la riga «Risultati» della chat). */
@Composable
fun GuardareScreen(page: ResultsPage?, failed: Boolean, session: String?, a: GuardareActions, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().verticalScroll(rememberScrollState()).testTag("da-guardare").padding(start = 12.dp, end = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Cm.text) }
            Text(session?.let { stringResource(R.string.watch_results) + " · " + it } ?: stringResource(R.string.home_sec_watch), style = MaterialTheme.typography.titleLarge, color = Cm.text)
        }
        val groups = page?.groups.orEmpty().filter { session == null || it.session == session }
        // In elenco solo i lavori non visti; i visti in fondo, chiusi (app-risultati-visti-03-11). Visto e l'apertura li tolgono
        // subito, senza aspettare la lettura dopo la risposta del PC.
        var local by remember { mutableStateOf(emptySet<String>()) }
        val seenNow: (ResultItem) -> Unit = { r -> if (r.id !in local) { local = local + r.id; a.seen(r) } }
        val acts = GuardareActions(a.file, seenNow, a.note, { r -> if (r.seenAt == null) seenNow(r); a.open(r) }, a.session, a.external)
        val split = Guardare.splitSeen(groups, local)
        var seenOpen by rememberSaveable { mutableStateOf(false) }
        when {
            failed && page == null -> Text(stringResource(R.string.watch_read_failed), style = V3.Small)
            page == null -> Text(stringResource(R.string.prove_loading), style = V3.Small)
            groups.isEmpty() -> Text(stringResource(R.string.watch_empty), style = V3.Small)
            split.fresh.isEmpty() -> Text(stringResource(R.string.watch_all_seen), style = V3.Small)
        }
        split.fresh.forEach { g -> androidx.compose.runtime.key(g.title, g.session) { GroupBlock(g, acts) } }
        if (split.seen.isNotEmpty()) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { seenOpen = !seenOpen }.testTag("risultati-visti").padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(if (seenOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx2)
            Text(stringResource(R.string.watch_seen_groups) + "  " + split.seen.size, style = V3.Small)
        }
        if (seenOpen) split.seen.forEach { g -> androidx.compose.runtime.key(g.title, g.session) { GroupBlock(g, acts) } }
    }
}

/** Nella chat, in cima: «Risultati N» e l'ultimo consegnato; il tocco apre la pagina della sessione. */
@Composable
fun ResultsChatRow(line: Guardare.SessionLine, onClose: (() -> Unit)? = null, onOpen: () -> Unit) {
    if (line.groups.isEmpty()) return
    val l = line.latest?.let { Guardare.line(it) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(V3.Shape).background(V3.S2).clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 10.dp).testTag("chat-risultati"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Visibility, null, tint = V3.Tx)
        Text(stringResource(R.string.watch_results), style = V3.Name)
        Text(line.groups.size.toString(), style = V3.Small)
        // Il titolo prende tutto lo spazio a destra (con uno Spacer a metà andava a capo e restava «Docs tour,»).
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (line.groups.any { it.unseen }) Box(Modifier.size(8.dp).clip(CircleShape).background(V3.Wait))
            if (l != null) Text(titleOf(l), style = V3.Small, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f, fill = false))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx2)
        // La X la chiude fino al risultato successivo (app-risultati-x-05-11).
        onClose?.let { close ->
            androidx.compose.material3.IconButton(onClick = close, modifier = Modifier.size(32.dp).testTag("chat-risultati-chiudi")) {
                Icon(Icons.Rounded.Close, stringResource(R.string.watch_results_close), tint = V3.Tx2, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Il risultato aperto: il video col lettore della chat, l'immagine intera, un documento o una pagina con «Apri il file»; sotto
 * versione, cosa è cambiato, Visto e Apri la sessione, e la nota che va alla sessione col secondo in cui si è.
 */
@Composable
fun ResultViewer(i: ResultItem, a: GuardareActions, onBack: () -> Unit) {
    val l = Guardare.line(i)
    var path by remember(i.id) { mutableStateOf<String?>(null) }
    var failed by remember(i.id) { mutableStateOf(false) }
    var attempt by remember(i.id) { mutableStateOf(0) }
    val kind = Guardare.kind(i.kind)
    val show = Guardare.thumb(i) ?: Guardare.main(i).takeIf { kind == Guardare.Kind.VIDEO }
    LaunchedEffect(i.id, attempt) { failed = false; show?.let { f -> a.file(i, f, false) { p -> if (p == null) failed = true else path = p } } }
    var pos by remember(i.id) { mutableStateOf<Double?>(null) }
    var text by rememberSaveable(i.id) { mutableStateOf("") }
    var sent by remember(i.id) { mutableStateOf(false) }
    var seen by remember(i.id) { mutableStateOf(i.seenAt != null) }
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).testTag("risultato-aperto").padding(start = 12.dp, end = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Cm.text) }
            Text(l.title, style = MaterialTheme.typography.titleLarge, color = Cm.text, maxLines = 2)
        }
        val p = path
        when {
            kind == Guardare.Kind.VIDEO && p != null -> VideoPreview(p) { s -> pos = s }
            p != null -> {
                val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, p) { value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { decodeScaled(p, 2048, 12_000_000) } }
                // Un tocco la apre a tutto schermo, dove si ingrandisce (app-immagini-ingrandibili-03-11: dal telefono non si leggeva).
                var full by remember(p) { mutableStateOf(false) }
                bmp?.let { b -> Image(b, stringResource(R.string.image_open_full), Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { full = true }.testTag("risultato-immagine"), contentScale = ContentScale.FillWidth) }
                if (full) FileImageViewer(p) { full = false }
            }
            // Senza rete o col file sparito dal PC: lo dice, e «Riprova» lo richiede (app-bug-notte-03-11, prima girava per sempre).
            show != null && failed -> Column(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(V3.S2).padding(16.dp).testTag("risultato-non-arrivato"), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.watch_file_failed), style = V3.Small, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                V3Pill(stringResource(R.string.watch_retry)) { attempt++ }
            }
            show != null -> Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(V3.S2), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = V3.Tx2) }
            // Un documento: la sua anteprima grande, se il relay ne ha una (app-anteprime-verifica-11-11).
            it.pixelbox.cmwatch.rules.Thumbs.wants(i) -> {
                val shown = rememberPreview(i, a)
                if (shown !is it.pixelbox.cmwatch.rules.Thumbs.Shown.Icon) PreviewBox(shown, Modifier.fillMaxWidth().aspectRatio(if (shown is it.pixelbox.cmwatch.rules.Thumbs.Shown.Image) 3f / 4f else 4f / 3f), big = true)
            }
        }
        if (kind != Guardare.Kind.VIDEO && kind != Guardare.Kind.IMAGE) V3Pill(stringResource(R.string.watch_open_file)) { a.external(i) }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.watch_version_of, i.version, i.versions), style = V3.Name)
            Text(listOfNotNull(hhmmOf(i.at), l.seconds?.let { stringResource(R.string.watch_seconds, it) }).joinToString(" · "), style = V3.Meter)
        }
        i.changed?.trim()?.takeIf { it.isNotEmpty() }?.let { c ->
            val label = stringResource(R.string.watch_changed)
            Text(androidx.compose.ui.text.buildAnnotatedString {
                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)) { append(label) }
                append(" "); append(c)
            }, style = V3.Body)
        }
        i.caption?.trim()?.takeIf { it.isNotEmpty() && it != i.title }?.let { LongText(it, style = V3.Small) }
        if (i.replaced == true) Text(stringResource(R.string.watch_replaced), style = V3.Small.copy(color = V3.Wait))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (i.seenAt == null) V3Pill("✓ " + stringResource(R.string.watch_seen), filled = true, confirmed = seen) { if (!seen) { seen = true; a.seen(i) } }
            i.session?.let { s -> V3Pill("› " + stringResource(R.string.watch_open_session)) { a.session(s) } }
        }
        i.notes.forEach { n -> NoteLine(n.text, n.posS) }
        i.session?.let { s ->
            OutlinedTextField(
                value = text, onValueChange = { v: String -> text = v.take(Guardare.NOTE_MAX) }, modifier = Modifier.fillMaxWidth().testTag("nota-risultato"),
                placeholder = { Text(stringResource(R.string.watch_note_hint, s)) }, shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    TextButton(onClick = { Guardare.note(text)?.let { t -> a.note(i, t, pos); text = ""; sent = true } }, enabled = text.isNotBlank()) {
                        Text(stringResource(R.string.watch_send) + (pos?.takeIf { kind == Guardare.Kind.VIDEO }?.let { " " + stringResource(R.string.watch_at_pos, Guardare.pos(it)) } ?: ""))
                    }
                },
            )
            Text(if (sent) stringResource(R.string.watch_note_sent, s) else stringResource(R.string.watch_note_explain), style = V3.Small)
        }
    }
}
