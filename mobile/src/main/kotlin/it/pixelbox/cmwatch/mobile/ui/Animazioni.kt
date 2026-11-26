package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Animazioni
import kotlinx.coroutines.launch

/**
 * Voce 1 di app-animazioni-otto-26-11 (Copia che risponde, Franz 26/11 11:50): al tocco la copia diventa una spunta verde, «Copiato»
 * per il tasto a parole, per [Animazioni.COPY_HOLD_MS]; poi torna. Lo scambio entra da 0,9 con la molla veloce e sfuma in 180 ms;
 * con le animazioni spente è secco. Ogni tocco riparte da capo.
 */
@Composable
private fun rememberCopied(): Pair<Boolean, () -> Unit> {
    var taps by remember { mutableIntStateOf(0) }
    var on by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        on = true
        kotlinx.coroutines.delay(Animazioni.COPY_HOLD_MS)
        on = false
    }
    return on to { taps++ }
}

@Composable
private fun <T> CopySwap(state: T, content: @Composable (T) -> Unit) {
    val off = animationsOff()
    AnimatedContent(
        state,
        transitionSpec = {
            if (off) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else (fadeIn(tween(180, easing = CmMotion.easing)) + scaleIn(CmMotion.fast, initialScale = 0.9f)) togetherWith fadeOut(tween(120))
        },
        label = "copia",
    ) { content(it) }
}

/** Il tasto Copia a icona, sotto una risposta o un messaggio. */
@Composable
internal fun CopyIconAction(text: String) {
    val clip = LocalClipboardManager.current
    val (on, tap) = rememberCopied()
    val label = stringResource(if (on) R.string.copied_word else R.string.copy)
    IconButton(onClick = { clip.setText(AnnotatedString(text)); tap() }, modifier = Modifier.size(36.dp).testTag("copia")) {
        CopySwap(on) { done ->
            Icon(if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy, label, tint = if (done) V3.Good else Cm.text2, modifier = Modifier.size(18.dp))
        }
    }
}

/** Il tasto Copia a parole, dentro un testo da mandare o un comando. */
@Composable
internal fun CopyPill(copy: () -> String) {
    val clip = LocalClipboardManager.current
    val (on, tap) = rememberCopied()
    val style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    Row(
        Modifier.clip(CircleShape).border(1.dp, if (on) V3.Good else V3.Line, CircleShape).handCursor()
            .clickable { clip.setText(AnnotatedString(copy())); tap() }.padding(horizontal = 12.dp, vertical = 4.dp).testTag("copia"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CopySwap(on) { done ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done) Icon(Icons.Rounded.Check, null, tint = V3.Good, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                Text(stringResource(if (done) R.string.copied_word else R.string.copy), style = style, color = if (done) V3.Good else V3.Tx)
            }
        }
    }
}

/**
 * Voce 3 (Al lavoro, vivo): un riflesso attraversa il testo di chi lavora con lo stesso ciclo della linea del lavoro in corso,
 * [Animazioni.CYCLE_MS] lineare. Con le animazioni spente (e nelle immagini di prova) il colore pieno, fermo.
 */
@Composable
internal fun shineStyle(base: TextStyle, color: androidx.compose.ui.graphics.Color): TextStyle {
    if (animationsOff()) return base.copy(color = color)
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "al-lavoro")
    val x by t.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(tween(Animazioni.CYCLE_MS, easing = androidx.compose.animation.core.LinearEasing)),
        label = "riflesso",
    )
    val light = androidx.compose.ui.graphics.lerp(color, androidx.compose.ui.graphics.Color.White, 0.6f)
    val w = 48f
    val from = -2 * w + x * 6 * w
    return base.copy(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(color, light, color), start = androidx.compose.ui.geometry.Offset(from, 0f), end = androidx.compose.ui.geometry.Offset(from + w, 0f),
        ),
    )
}

/**
 * Voce 4 (Cifra che scorre): l'ora dell'ultima risposta data da qui. La cifra del tabellone scorre solo se cambia entro [WINDOW_MS]
 * da un gesto; per un aggiornamento dal PC cambia e basta.
 */
object Gesture {
    const val WINDOW_MS = 4000L
    @Volatile private var at = 0L
    fun mark() { at = android.os.SystemClock.uptimeMillis() }
    fun recent(): Boolean = at > 0 && android.os.SystemClock.uptimeMillis() - at < WINDOW_MS
}

/** La cifra grande del tabellone: con [roll] la nuova arriva da sotto in 220 ms con la molla veloce, solo dopo un gesto. */
@Composable
internal fun RollNumber(value: String, roll: Boolean, content: @Composable (String) -> Unit) {
    if (!roll) { content(value); return }
    val off = animationsOff()
    AnimatedContent(
        value,
        transitionSpec = {
            if (!off && Animazioni.rolls(initialState.toIntOrNull(), targetState.toIntOrNull() ?: 0, Gesture.recent()))
                (androidx.compose.animation.slideInVertically(tween(220, easing = CmMotion.easing)) { it } + fadeIn(tween(160))) togetherWith
                    (androidx.compose.animation.slideOutVertically(tween(220, easing = CmMotion.easing)) { -it } + fadeOut(tween(160)))
            else androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
        },
        label = "cifra",
    ) { content(it) }
}

/**
 * Voce 6 (Tieni premuto per la produzione): il tasto si riempie mentre il dito resta giù e l'ok parte solo quando è pieno, dopo
 * [Animazioni.HOLD_MS]; lasciato prima torna vuoto in 200 ms e non succede niente. Lineare perché è un tempo da leggere. Con le
 * animazioni spente il riempimento non si vede, ma il tempo da tenere resta.
 */
@Composable
internal fun HoldButton(label: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val fill = remember { androidx.compose.animation.core.Animatable(0f) }
    val off = animationsOff()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val prod = Cm.prod
    androidx.compose.foundation.layout.Box(
        modifier.clip(CircleShape).background(V3.S3).border(1.dp, prod.copy(alpha = 0.6f), CircleShape)
            .drawBehind { if (!off && fill.value > 0f) drawRect(prod.copy(alpha = 0.35f), size = size.copy(width = size.width * fill.value)) }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val job = scope.launch {
                        if (off) kotlinx.coroutines.delay(Animazioni.HOLD_MS)
                        else fill.animateTo(1f, tween(Animazioni.HOLD_MS.toInt(), easing = androidx.compose.animation.core.LinearEasing))
                        fill.snapTo(0f)
                        onDone()
                    }
                    val released = tryAwaitRelease()
                    if (job.isActive) { job.cancel(); scope.launch { fill.animateTo(0f, tween(200, easing = CmMotion.easing)) } }
                    else if (!released) scope.launch { fill.snapTo(0f) }
                })
            }
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("tieni-premuto"),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = V3.Tx) }
}

/**
 * Voce 7 di app-animazioni-seconde-26-11 (Limite ad anello): un anello piccolo si riempie mentre scrivi; negli ultimi 20 caratteri
 * diventa ambra e mostra quanti ne restano, oltre il limite rosso. Non blocca la scrittura.
 */
@Composable
internal fun LimitRing(length: Int, max: Int) {
    val state = Animazioni.limit(length, max)
    val off = animationsOff()
    val p by androidx.compose.animation.core.animateFloatAsState(Animazioni.fill(length, max), if (off) androidx.compose.animation.core.snap() else tween(120), label = "limite")
    val tone = when (state) { Animazioni.Limit.OK -> V3.Tx2; Animazioni.Limit.NEAR -> V3.Wait; Animazioni.Limit.OVER -> V3.Red }
    val track = V3.Tx.copy(alpha = 0.14f)
    val left = max - length
    val label = androidx.compose.ui.res.pluralStringResource(R.plurals.scelta_left, maxOf(0, left), maxOf(0, left))
    Row(Modifier.testTag("limite").semantics { contentDescription = label }, verticalAlignment = Alignment.CenterVertically) {
        if (state != Animazioni.Limit.OK) Text("$left", style = V3.Meter.copy(color = tone), modifier = Modifier.padding(end = 6.dp))
        androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
            val w = 3.dp.toPx()
            val st = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(track, 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = st)
            drawArc(tone, -90f, 360f * p, false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = st)
        }
    }
}


/**
 * Voce 4 di app-animazioni-seconde-26-11 (Annulla col tempo, scelta di Franz del 26/11 alle 12:49): un gesto che toglie qualcosa (Non
 * ora, Mai, Visto, Chiudi senza salvare) cambia subito quello che si vede, ma il comando parte dopo [Animazioni.UNDO_MS]; fino ad
 * allora «Annulla» lo ferma e rimette com'era. Un avviso per volta: un gesto nuovo fa partire subito quello di prima.
 */
object Undo {
    data class Shown(val id: Int, val text: String)

    var shown by androidx.compose.runtime.mutableStateOf<Shown?>(null)
        private set
    private var seq = 0
    private val own = kotlinx.coroutines.MainScope()
    private var job: kotlinx.coroutines.Job? = null
    private var go: (() -> Unit)? = null
    private var back: (() -> Unit)? = null

    @Suppress("UNUSED_PARAMETER")
    fun later(scope: kotlinx.coroutines.CoroutineScope, text: String, go: () -> Unit, back: () -> Unit = {}) {
        flush()
        val id = ++seq
        this.go = go; this.back = back
        shown = Shown(id, text)
        // L'attesa non dipende da chi l'ha chiesta: se la sua schermata se ne va, il comando parte lo stesso.
        job = own.launch {
            kotlinx.coroutines.delay(Animazioni.UNDO_MS)
            if (shown?.id == id) flush()
        }
    }

    fun cancel() {
        val b = back
        job?.cancel(); job = null; go = null; back = null; shown = null
        b?.invoke()
    }

    fun flush() {
        val g = go
        job?.cancel(); job = null; go = null; back = null; shown = null
        g?.invoke()
    }
}

/** L'avviso in basso con «Annulla» e l'anello che si svuota in 5 s; sopra tutto, in un popup che non prende i tocchi fuori da sé. */
@Composable
internal fun UndoSnack() {
    val s = Undo.shown ?: return
    val off = animationsOff()
    val ring = remember(s.id) { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(s.id) { if (!off) ring.animateTo(0f, tween(Animazioni.UNDO_MS.toInt(), easing = androidx.compose.animation.core.LinearEasing)) }
    androidx.compose.ui.window.Popup(alignment = Alignment.BottomCenter, offset = androidx.compose.ui.unit.IntOffset(0, -48)) {
        Row(
            Modifier.padding(horizontal = 16.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(V3.S3)
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp).testTag("annulla"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.text, style = V3.Body.copy(fontSize = 14.5.sp), maxLines = 2, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
            Row(
                Modifier.clip(CircleShape).handCursor().clickable { Undo.cancel() }.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tone = V3.Tx
                androidx.compose.foundation.Canvas(Modifier.size(22.dp).padding(end = 0.dp)) {
                    val w = 3.dp.toPx()
                    val st = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    val tl = androidx.compose.ui.geometry.Offset(w / 2, w / 2)
                    val sz = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
                    drawArc(tone.copy(alpha = 0.18f), 0f, 360f, false, topLeft = tl, size = sz, style = st)
                    drawArc(tone, -90f, 360f * ring.value, false, topLeft = tl, size = sz, style = st)
                }
                Text(stringResource(R.string.undo), style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = V3.Tx, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * Voce 5 della prima proposta (Passi della prova, app-animazioni-passi-26-11, contratto 1.162): le tappe del compito in verifica su
 * un filo. Il filo si riempie fino alla tappa a cui si è arrivati (400 ms), le passate sono spunte verdi, quella in corso ha un arco
 * che gira in 1,2 s, una fallita è rossa; sotto, a parole, dove si è. Con le animazioni spente niente movimento, arco fermo.
 */
@Composable
internal fun ProofRail(steps: it.pixelbox.cmwatch.contract.ProofSteps) {
    val items = steps.items
    if (items.isEmpty()) return
    val off = animationsOff()
    val fill by androidx.compose.animation.core.animateFloatAsState(
        it.pixelbox.cmwatch.rules.PassiProva.fill(items), if (off) androidx.compose.animation.core.snap() else tween(400, easing = CmMotion.easing), label = "filo",
    )
    val turn = if (off || items.none { it.state == "run" }) 0f else {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "tappa")
        t.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(tween(1200, easing = androidx.compose.animation.core.LinearEasing)), label = "arco").value
    }
    val good = V3.Good; val red = V3.Red; val busy = V3.Busy; val line = V3.S3; val ink = V3.Tx3; val bg = V3.S1
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(top = 6.dp).testTag("passi-prova")) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val n = items.size
            val cw = size.width / n
            val cy = size.height / 2
            val x0 = cw / 2; val x1 = size.width - cw / 2
            drawLine(line, androidx.compose.ui.geometry.Offset(x0, cy), androidx.compose.ui.geometry.Offset(x1, cy), 2.dp.toPx())
            if (n > 1) drawLine(good, androidx.compose.ui.geometry.Offset(x0, cy), androidx.compose.ui.geometry.Offset(x0 + (x1 - x0) * fill, cy), 2.dp.toPx())
            val r = 10.dp.toPx()
            items.forEachIndexed { i, s ->
                val c = androidx.compose.ui.geometry.Offset(x0 + i * cw, cy)
                when (it.pixelbox.cmwatch.rules.PassiProva.look(s)) {
                    it.pixelbox.cmwatch.rules.PassiProva.Look.DONE -> {
                        drawCircle(good, r, c)
                        val a = c + androidx.compose.ui.geometry.Offset(-r * .45f, 0f); val b = c + androidx.compose.ui.geometry.Offset(-r * .1f, r * .35f); val e = c + androidx.compose.ui.geometry.Offset(r * .5f, -r * .35f)
                        drawLine(androidx.compose.ui.graphics.Color.Black, a, b, 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(androidx.compose.ui.graphics.Color.Black, b, e, 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                    it.pixelbox.cmwatch.rules.PassiProva.Look.FAILED -> {
                        drawCircle(red, r, c)
                        val d = r * .35f
                        drawLine(androidx.compose.ui.graphics.Color.Black, c + androidx.compose.ui.geometry.Offset(-d, -d), c + androidx.compose.ui.geometry.Offset(d, d), 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(androidx.compose.ui.graphics.Color.Black, c + androidx.compose.ui.geometry.Offset(d, -d), c + androidx.compose.ui.geometry.Offset(-d, d), 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                    it.pixelbox.cmwatch.rules.PassiProva.Look.NOW -> {
                        drawCircle(bg, r, c)
                        drawCircle(busy.copy(alpha = 0.35f), r - 1.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        drawArc(busy, turn - 90f, 90f, false, topLeft = c - androidx.compose.ui.geometry.Offset(r - 1.dp.toPx(), r - 1.dp.toPx()), size = androidx.compose.ui.geometry.Size(2 * (r - 1.dp.toPx()), 2 * (r - 1.dp.toPx())), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                    it.pixelbox.cmwatch.rules.PassiProva.Look.TODO -> {
                        drawCircle(bg, r, c)
                        drawCircle(ink, r - 1.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            items.forEach { s ->
                val look = it.pixelbox.cmwatch.rules.PassiProva.look(s)
                Text(
                    s.label, style = V3.Small.copy(fontSize = 13.sp, color = when (look) { it.pixelbox.cmwatch.rules.PassiProva.Look.DONE -> V3.Tx; it.pixelbox.cmwatch.rules.PassiProva.Look.NOW -> V3.Busy; it.pixelbox.cmwatch.rules.PassiProva.Look.FAILED -> V3.Red; else -> V3.Tx2 }),
                    maxLines = 1, softWrap = false, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        it.pixelbox.cmwatch.rules.PassiProva.line(steps)?.let { l ->
            val text = when (l.say) {
                it.pixelbox.cmwatch.rules.PassiProva.Say.RUNNING -> stringResource(R.string.proof_running, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.FAILED -> stringResource(R.string.proof_failed, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.TODO -> stringResource(R.string.proof_todo, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.ALL_DONE -> stringResource(R.string.proof_all_done)
            }
            Text(text, style = V3.Meter, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
