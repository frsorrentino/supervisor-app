package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import it.pixelbox.cmwatch.contract.ChangedFile
import it.pixelbox.cmwatch.contract.ChangesCount
import it.pixelbox.cmwatch.contract.ChangesView
import it.pixelbox.cmwatch.contract.Session
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Hunk
import it.pixelbox.cmwatch.contract.StepDiff
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.DiffRules
import it.pixelbox.cmwatch.rules.uniqueKeys
import it.pixelbox.cmwatch.ui.tokens.CmColors

/*
 * Il diff dal telefono (contratto 1.72, specifica del 15/10 approvata da Franz dalle 22:43 alle 23:11): la riga chiusa sotto
 * ogni passaggio di modifica, il diff aperto, la pillola della testata e la vista «Modifiche». Le regole stanno in DiffRules
 * (core), le stesse della web (diff.ts). Surface, IconButton e TextButton sono quelli del package, con la mano del mouse.
 */

/**
 * «Mostra tutto»: l'op `diff` con l'id della voce; null = il relay non la conosce. La fornisce MainActivity, come i file.
 * `got`: i passaggi interi già arrivati, per id, fuori dalla LazyColumn della chat: un passaggio che esce di vista e torna non
 * si richiede al PC (revisione finale, 16/10).
 */
data class DiffOpener(
    val full: (suspend (entryId: String) -> DiffRules.Load<StepDiff>)? = null,
    val got: MutableMap<String, StepDiff> = mutableMapOf(),
)
val LocalDiffOpener = compositionLocalOf { DiffOpener() }

/** Il carattere del diff: larghezza fissa, ogni riga alta 18 sp senza margini del carattere, uguale per numeri e testo. */
private val DiffText = TextStyle(
    fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * Il diff aperto: i numeri di riga fermi a sinistra (vecchio e nuovo), le righe tolte in rosso e aggiunte in verde, carattere
 * a larghezza fissa, nessun a capo; il testo scorre di lato dentro il riquadro, la pagina no. Due testi soli, i numeri e le
 * righe, con lo stesso passo: anche 5000 righe restano due elementi, e i fondi si disegnano dalle altezze del testo.
 */
@Composable
internal fun DiffBox(hunks: List<Hunk>, modifier: Modifier = Modifier, v3: Boolean = false) {
    val rows = remember(hunks) { DiffRules.rows(hunks) }
    val digits = remember(rows) { DiffRules.gutter(rows) }
    val add = if (v3) V3.Good else CmColors.briefGood
    val del = if (v3) V3.Red else CmColors.briefAlertRing
    val dim = if (v3) V3.Tx2 else CmColors.text2
    val more = stringResource(R.string.diff_more_chars)
    val numbers = remember(rows, digits) {
        rows.joinToString("\n") { r -> (r.old?.toString() ?: "").padStart(digits) + " " + (r.new?.toString() ?: "").padStart(digits) }
    }
    val code = remember(rows, more) {
        buildAnnotatedString {
            rows.forEachIndexed { i, r ->
                if (i > 0) append('\n')
                when (r.kind) {
                    DiffRules.Kind.ADDED -> { withStyle(SpanStyle(color = add, fontWeight = FontWeight.Bold)) { append('+') }; append(r.text) }
                    DiffRules.Kind.REMOVED -> { withStyle(SpanStyle(color = del, fontWeight = FontWeight.Bold)) { append(DiffRules.MINUS) }; append(r.text) }
                    DiffRules.Kind.SAME -> { append(' '); append(r.text) }
                    DiffRules.Kind.NOTE -> withStyle(SpanStyle(color = dim, fontStyle = FontStyle.Italic)) { append(r.text) }
                    DiffRules.Kind.GAP -> Unit
                }
                if (r.more > 0) withStyle(SpanStyle(color = dim)) { append("  "); append(more.format(r.more)) }
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // I fondi delle righe a tutta larghezza, sotto i numeri e sotto il testo, alle stesse altezze del testo.
    val paint: DrawScope.() -> Unit = {
        layout?.let { l ->
            rows.forEachIndexed { i, r ->
                if (i >= l.lineCount) return@forEachIndexed
                val top = l.getLineTop(i)
                val h = l.getLineBottom(i) - top
                when (r.kind) {
                    DiffRules.Kind.ADDED -> drawRect(add.copy(alpha = .14f), Offset(0f, top), Size(size.width, h))
                    DiffRules.Kind.REMOVED -> drawRect(del.copy(alpha = .14f), Offset(0f, top), Size(size.width, h))
                    DiffRules.Kind.GAP -> drawRect(CmColors.line, Offset(0f, top + h / 2f), Size(size.width, 1.dp.toPx()))
                    else -> Unit
                }
            }
        }
    }
    val shape = RoundedCornerShape(12.dp)
    Row(modifier.fillMaxWidth().clip(shape).background(if (v3) V3.Bg else CmColors.surfaceLow).border(1.dp, if (v3) V3.Line else CmColors.line, shape).padding(vertical = 6.dp)) {
        Text(numbers, style = DiffText, color = dim, softWrap = false, modifier = Modifier.drawBehind(paint).padding(start = 8.dp, end = 6.dp))
        BoxWithConstraints(Modifier.weight(1f)) {
            val min = maxWidth
            Text(
                code, style = DiffText, color = CmColors.text, softWrap = false, onTextLayout = { layout = it },
                modifier = Modifier.horizontalScroll(rememberScrollState()).widthIn(min = min).drawBehind(paint).padding(end = 12.dp),
            )
        }
    }
}

/** La riga chiusa di un diff: «totals.ts · +2 −1»; nella vista «Modifiche» anche la cartella sotto e l'etichetta. Il tocco apre. */
@Composable
internal fun DiffLine(
    name: String, added: Int, removed: Int, open: Boolean, onClick: () -> Unit,
    folder: String? = null, label: String? = null, counts: Boolean = true,
    /** Sotto un passaggio, nello stile v3 (app-sessione-blocchi-v3-30-10); la vista «Modifiche» resta com'era. */
    v3: Boolean = false,
) {
    val desc = stringResource(if (open) R.string.diff_close else R.string.diff_open, name)
    Surface(onClick = onClick, color = if (v3) V3.S2 else CmColors.surface, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().semantics { contentDescription = desc }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Add, null, tint = if (v3) V3.Tx2 else CmColors.actionIcon, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    buildAnnotatedString {
                        append(name)
                        if (counts) {
                            withStyle(SpanStyle(color = if (v3) V3.Tx2 else CmColors.text2)) { append(" · ") }
                            withStyle(SpanStyle(color = if (v3) V3.Good else CmColors.briefGood)) { append("+$added") }
                            append(" ")
                            withStyle(SpanStyle(color = if (v3) V3.Red else CmColors.briefAlertRing)) { append("${DiffRules.MINUS}$removed") }
                        }
                    },
                    style = MaterialTheme.typography.labelLarge, color = if (v3) V3.Tx else CmColors.text,
                )
                folder?.let { Text(it, style = MonoSmall, color = CmColors.text2) }
            }
            label?.let { Text(it, style = MonoSmall, color = CmColors.text2, modifier = Modifier.clip(CircleShape).background(CmColors.surfaceHigh).padding(horizontal = 8.dp, vertical = 3.dp)) }
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = if (v3) V3.Tx2 else CmColors.text2)
        }
    }
}

/**
 * Sotto un passaggio di modifica: la riga chiusa; il tocco apre il diff; con `cut` l'ultima riga è «Mostra tutto», che chiede
 * il passaggio intero (op `diff`, fino a 5000 righe). Aperto e «Mostra tutto» restano dopo una rotazione; il passaggio intero
 * si richiede da solo (DiffRules.shown). `startOpen` e `startAll`: nei test.
 */
@Composable
internal fun StepDiffRow(entryId: String, d: StepDiff, startOpen: Boolean = false, startAll: Boolean = false) {
    val opener = LocalDiffOpener.current
    var open by rememberSaveable(entryId) { mutableStateOf(startOpen) }
    var wantAll by rememberSaveable(entryId) { mutableStateOf(startAll) }
    var full by remember(entryId) { mutableStateOf<DiffRules.Load<StepDiff>?>(opener.got[entryId]?.let { DiffRules.Load.Ok(it) }) }
    var busy by remember(entryId) { mutableStateOf(false) }
    val failed = full as? DiffRules.Load.Failed
    val shown = DiffRules.shown(d, (full as? DiffRules.Load.Ok)?.value, wantAll, busy || failed != null)
    LaunchedEffect(shown.ask) {
        val fetch = opener.full
        if (!shown.ask || fetch == null) return@LaunchedEffect
        busy = true
        val r = fetch(entryId)
        if (r is DiffRules.Load.Ok) opener.got[entryId] = r.value
        full = r
        busy = false
    }
    V3Theme { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DiffLine(DiffRules.name(d.path), d.added, d.removed, open, onClick = { open = !open }, v3 = true)
        if (open) {
            if (shown.diff.hunks.isEmpty()) DiffNote(stringResource(R.string.diff_hidden_empty)) else DiffBox(shown.diff.hunks, v3 = true)
            when {
                shown.showAll && opener.full != null -> TextButton(onClick = { wantAll = true }) { Text(stringResource(R.string.diff_show_all), color = CmColors.actionIcon) }
                busy -> DiffNote(stringResource(R.string.diff_loading))
                failed != null -> DiffFailed(failed, onRetry = { full = null })
                shown.beyond -> DiffNote(stringResource(R.string.diff_beyond))
            }
        }
    } }
}

@Composable
private fun DiffNote(text: String, modifier: Modifier = Modifier.padding(horizontal = 4.dp)) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = modifier)

/** Un errore della vista o di «Mostra tutto», in chiaro, con Riprova: mai una vista vuota senza spiegazione. */
@Composable
private fun DiffFailed(f: DiffRules.Load.Failed, onRetry: () -> Unit) {
    val text = when (f.why) {
        DiffRules.Why.GIT -> stringResource(R.string.diff_git)
        DiffRules.Why.DOWN -> stringResource(R.string.diff_down)
        DiffRules.Why.OTHER -> f.text
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = CmColors.goneDim, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry), color = CmColors.actionIcon) }
    }
}

/** «4 file · +8 −2» sotto le pillole della testata; il tocco apre la vista «Modifiche». Senza `onClick` è solo da leggere. */
@Composable
internal fun ChangesPill(c: ChangesCount, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val files = pluralStringResource(R.plurals.changes_files, c.files, c.files)
    val desc = stringResource(R.string.changes_open) + ": " + files + " · " + DiffRules.counts(c.added, c.removed)
    Surface(
        onClick = { onClick?.invoke() }, enabled = onClick != null, color = V3.S2, shape = CircleShape,
        modifier = modifier.semantics { contentDescription = desc },
    ) {
        Row(Modifier.padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Add, null, tint = V3.Tx2, modifier = Modifier.size(16.dp))
            Text(
                buildAnnotatedString {
                    append(files); append(" · ")
                    withStyle(SpanStyle(color = V3.Good)) { append("+${c.added}") }
                    append(" ")
                    withStyle(SpanStyle(color = V3.Red)) { append("${DiffRules.MINUS}${c.removed}") }
                },
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = V3.Sans, fontSize = 13.sp), color = V3.Tx, maxLines = 1, softWrap = false,
            )
        }
    }
}

/**
 * La vista «Modifiche»: rispetto all'ultimo commit (o, fuori da git, le modifiche di Claude in questa conversazione) un file
 * per riga coi suoi numeri e l'etichetta; il tocco apre il diff del file (op `changes` con `file=`), Aggiorna rilegge tutto.
 * Sta sopra la sessione: sul telefono ne prende la pagina, sul tablet la colonna. Mai vuota senza spiegazione.
 * `start` e `startOpen`: negli snapshot la risposta e i file già aperti, senza chiedere niente al PC.
 */
@Composable
internal fun ChangesPanel(
    s: Session, load: suspend (String?) -> DiffRules.Load<ChangesView>, onClose: () -> Unit, modifier: Modifier = Modifier,
    start: DiffRules.Load<ChangesView>? = null, startOpen: Map<String, DiffRules.Load<ChangesView>> = emptyMap(),
) {
    if (!LocalStill.current) androidx.activity.compose.BackHandler(onBack = onClose)
    var got by remember(s.name) { mutableStateOf(start) }
    var asked by remember(s.name) { mutableIntStateOf(if (start == null) 1 else 0) }
    var loading by remember(s.name) { mutableStateOf(false) }
    LaunchedEffect(s.name, asked) {
        if (asked == 0) return@LaunchedEffect
        loading = true
        got = load(null)
        loading = false
    }
    Column(modifier.fillMaxSize().background(CmColors.bg).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = CmColors.text) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.changes_title), style = MaterialTheme.typography.titleLarge, color = CmColors.text)
                Text(s.name, style = MaterialTheme.typography.labelMedium, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip)
            }
            IconButton(onClick = { asked++ }, enabled = !loading) { Icon(Icons.Rounded.Refresh, stringResource(R.string.refresh), tint = CmColors.actionIcon) }
        }
        Box(Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 20.dp)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = CmColors.actionIcon, trackColor = CmColors.briefTrack)
        }
        when (val g = got) {
            null -> DiffNote(stringResource(R.string.changes_loading), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            is DiffRules.Load.Failed -> Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { DiffFailed(g) { asked++ } }
            is DiffRules.Load.Ok -> ChangesList(g.value, load, asked, startOpen)
        }
    }
}

@Composable
private fun ChangesList(v: ChangesView, load: suspend (String?) -> DiffRules.Load<ChangesView>, asked: Int, startOpen: Map<String, DiffRules.Load<ChangesView>>) {
    // Revisione finale (16/10): i file letti con `file=` restano qui, fuori dalla LazyColumn: un file che esce di vista e torna
    // non si richiede al PC. Aggiorna (asked) ricomincia da capo.
    val read = remember(asked) { mutableMapOf<String, DiffRules.Load<ChangesView>>() }
    val src = DiffRules.source(v)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "source") {
            val line = when (src) {
                DiffRules.Source.GIT -> stringResource(R.string.changes_base, v.base.orEmpty())
                DiffRules.Source.NO_COMMITS -> stringResource(R.string.changes_no_commits)
                DiffRules.Source.TRANSCRIPT -> stringResource(R.string.changes_outside_git)
            }
            Text(line, style = MaterialTheme.typography.bodyMedium, color = if (src == DiffRules.Source.TRANSCRIPT) CmColors.waiting else CmColors.text2)
        }
        if (v.files.isEmpty()) item(key = "none") {
            DiffNote(stringResource(if (src == DiffRules.Source.TRANSCRIPT) R.string.changes_none_transcript else R.string.changes_none))
        }
        // Chiavi mai ripetute, anche con due file dallo stesso percorso: una chiave doppia chiude l'app (15/10 23:41).
        val keys = uniqueKeys(v.files) { it.path }
        items(v.files.size, key = { keys[it] }) { i -> val f = v.files[i]; FileChange(f, load, asked, startOpen[f.path], read) }
        DiffRules.hiddenFiles(v).takeIf { it > 0 }?.let { n ->
            item(key = "more") { DiffNote(pluralStringResource(R.plurals.changes_more_files, n, n)) }
        }
    }
}

/** Un file della vista: la riga chiusa; aperto, le righe dell'elenco subito e intanto il file intero con `file=`. */
@Composable
private fun FileChange(f: ChangedFile, load: suspend (String?) -> DiffRules.Load<ChangesView>, asked: Int, start: DiffRules.Load<ChangesView>?, read: MutableMap<String, DiffRules.Load<ChangesView>>) {
    var open by rememberSaveable(f.path) { mutableStateOf(start != null) }
    var retry by remember(f.path) { mutableIntStateOf(0) }
    var alone by remember(f.path, asked) { mutableStateOf(start ?: read[f.path]) }
    val hidden = DiffRules.hidden(f)
    LaunchedEffect(f.path, open, retry, asked) {
        // Revisione finale (16/10): `file=` solo per un file tagliato, e una volta sola finché la lettura riesce.
        if (!open || !DiffRules.askFile(f) || alone is DiffRules.Load.Ok) return@LaunchedEffect
        alone = null
        val r = load(f.path)
        alone = r
        if (r is DiffRules.Load.Ok) read[f.path] = r
    }
    val label = when (DiffRules.label(f.status)) {
        DiffRules.Label.NEW -> stringResource(R.string.changes_label_new)
        DiffRules.Label.DELETED -> stringResource(R.string.changes_label_deleted)
        DiffRules.Label.RENAMED -> stringResource(R.string.changes_label_renamed)
        DiffRules.Label.BINARY -> stringResource(R.string.changes_label_binary)
        null -> null
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DiffLine(DiffRules.name(f.path), f.added, f.removed, open, onClick = { open = !open }, folder = DiffRules.folder(f.path), label = label, counts = DiffRules.showCounts(f))
        if (open) {
            if (hidden != null) DiffNote(hiddenText(hidden))
            else if (!DiffRules.askFile(f)) DiffBox(f.hunks)
            else when (val a = alone) {
                null -> { if (f.hunks.isNotEmpty()) DiffBox(f.hunks); DiffNote(stringResource(R.string.changes_file_loading)) }
                is DiffRules.Load.Failed -> { if (f.hunks.isNotEmpty()) DiffBox(f.hunks); DiffFailed(a) { retry++ } }
                is DiffRules.Load.Ok -> {
                    val one = DiffRules.alone(a.value, f.path)
                    when {
                        one == null -> DiffNote(stringResource(R.string.changes_file_gone))
                        one.hunks.isEmpty() -> DiffNote(hiddenText(DiffRules.hidden(one) ?: DiffRules.Hidden.EMPTY))
                        else -> { DiffBox(one.hunks); if (one.cut) DiffNote(stringResource(R.string.diff_beyond)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun hiddenText(h: DiffRules.Hidden): String = stringResource(when (h) {
    DiffRules.Hidden.BINARY -> R.string.diff_hidden_binary
    DiffRules.Hidden.SECRET -> R.string.diff_hidden_secret
    DiffRules.Hidden.BIG -> R.string.diff_hidden_big
    DiffRules.Hidden.RENAMED -> R.string.diff_hidden_renamed
    DiffRules.Hidden.EMPTY -> R.string.diff_hidden_empty
})
