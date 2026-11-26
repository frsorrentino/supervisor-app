package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import kotlin.math.sqrt
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.layout.layout
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
 * Le molle di designbookmark.com/motion (shared/motion.js: massa 1, a = -k(x - t) - c·v), per le animazioni fedeli
 * (app-animazioni-fedeli-26-11): in Compose `spring(stiffness = k, dampingRatio = c / 2√k)`, la stessa equazione.
 * Con le animazioni spente (e nelle immagini di prova) salta alla meta.
 */
internal object Molla {
    fun of(k: Float, c: Float): FiniteAnimationSpec<Float> = spring(stiffness = k, dampingRatio = c / (2f * sqrt(k)))
}

@Composable
private fun molla(target: Float, k: Float, c: Float, label: String): Float {
    val off = animationsOff()
    return animateFloatAsState(target, if (off) snap() else Molla.of(k, c), label = label).value
}

/**
 * Copia fedele all'originale (Copy Button): al tocco l'icona copia si rimpicciolisce a metà e gira di -40°, la spunta entra da 0,4
 * con un piccolo rimbalzo, la parola sale da «Copia» a «Copiato» e il tasto si allarga alla parola nuova; dopo
 * [Animazioni.COPY_HOLD_MS] torna. Molle dell'originale: rullo 260 20, icone 420 18, larghezza 300 24. Ogni tocco riparte da capo.
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

/** Le due icone sovrapposte: [i] è la molla 420 18, da 0 (copia) a 1 (spunta), libera fra -0,2 e 1,2 come nell'originale. */
@Composable
private fun CopyIcons(i0: Float, tint: Color, size: Dp) {
    val i = i0.coerceIn(-0.2f, 1.2f)
    val ic = i.coerceIn(0f, 1f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.ContentCopy, null, tint = tint,
            modifier = Modifier.size(size).graphicsLayer {
                alpha = (1f - i * 1.6f).coerceIn(0f, 1f); scaleX = 1f - 0.5f * ic; scaleY = 1f - 0.5f * ic; rotationZ = -40f * ic
            },
        )
        Icon(
            Icons.Rounded.Check, null, tint = V3.Good,
            modifier = Modifier.size(size).graphicsLayer { alpha = (i * 1.6f - 0.4f).coerceIn(0f, 1f); scaleX = 0.4f + 0.6f * i; scaleY = 0.4f + 0.6f * i },
        )
    }
}

/** Il tasto Copia a icona, sotto una risposta o un messaggio. */
@Composable
internal fun CopyIconAction(text: String) {
    val clip = LocalClipboardManager.current
    val (on, tap) = rememberCopied()
    val label = stringResource(if (on) R.string.copied_word else R.string.copy)
    val i = molla(if (on) 1f else 0f, 420f, 18f, "copia-icone")
    IconButton(
        onClick = { clip.setText(AnnotatedString(text)); tap() },
        modifier = Modifier.size(36.dp).testTag("copia").semantics { contentDescription = label },
    ) { CopyIcons(i, Cm.text2, 18.dp) }
}

/** Il tasto Copia a parole, dentro un testo da mandare o un comando: icone 16 dp e la parola che sale, larga quanto serve. */
@Composable
internal fun CopyPill(copy: () -> String) {
    val clip = LocalClipboardManager.current
    val (on, tap) = rememberCopied()
    val t = if (on) 1f else 0f
    val r = molla(t, 260f, 20f, "copia-rullo")
    val i = molla(t, 420f, 18f, "copia-icone")
    val w = molla(t, 300f, 24f, "copia-larghezza")
    val style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val label = stringResource(if (on) R.string.copied_word else R.string.copy)
    Row(
        Modifier.clip(CircleShape).border(1.dp, V3.Line, CircleShape).handCursor()
            .clickable { clip.setText(AnnotatedString(copy())); tap() }
            .padding(start = 10.dp, end = 12.dp, top = 4.dp, bottom = 4.dp).testTag("copia").semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CopyIcons(i, V3.Tx, 16.dp)
        androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
        Layout(
            content = {
                Text(stringResource(R.string.copy), style = style, color = V3.Tx, maxLines = 1, softWrap = false)
                Text(stringResource(R.string.copied_word), style = style, color = V3.Good, maxLines = 1, softWrap = false)
            },
            modifier = Modifier.clipToBounds(),
        ) { ms, cs ->
            val a = ms[0].measure(cs.copy(minWidth = 0, maxWidth = androidx.compose.ui.unit.Constraints.Infinity))
            val b = ms[1].measure(cs.copy(minWidth = 0, maxWidth = androidx.compose.ui.unit.Constraints.Infinity))
            val h = a.height
            val width = (a.width + (b.width - a.width) * w).roundToInt().coerceAtLeast(0)
            layout(width, h) {
                val dy = (-h * r).roundToInt()
                a.placeRelative(0, dy)
                b.placeRelative(0, h + dy)
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
 * Voce 6, fedele all'originale (Hold Button; app-animazioni-fedeli-26-11). Il dito giù schiaccia il tasto a 0,96 (molla 520 20) e lo
 * riempie da sinistra in [Animazioni.HOLD_MS], lineare; la scritta si inverte esattamente sotto il riempimento. Lasciato prima, il
 * riempimento torna con la molla 260 14 e il tasto fa «no» con una scossa (molla 700 9, spinta 260·min(1, 2p + 0,3) dp/s). Pieno:
 * il tasto diventa un cerchio verde (larghezza con la molla 320 20, pop +2,5 sulla scala), la scritta sfuma e si sfoca di 4 dp in
 * 200 ms, la spunta compare dopo 120 ms e si disegna in 450 ms dopo 160 ms. L'ok parte a spunta disegnata; dopo 1,7 s il tasto torna
 * (molla 256 20). Colori dell'app: rosso della produzione per il riempimento, scritta scura sopra. Con le animazioni spente niente
 * movimento, ma il tempo da tenere resta.
 */
@Composable
internal fun HoldButton(label: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val off = animationsOff()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val p = remember { androidx.compose.animation.core.Animatable(0f) }
    val sc = remember { androidx.compose.animation.core.Animatable(1f) }
    val shake = remember { androidx.compose.animation.core.Animatable(0f) }
    val morph = remember { androidx.compose.animation.core.Animatable(0f) }
    var done by remember { androidx.compose.runtime.mutableStateOf(false) }
    val labelA = animateFloatAsState(if (done) 0f else 1f, if (off) snap() else tween(200), label = "tieni-scritta").value
    val checkA = animateFloatAsState(if (done) 1f else 0f, if (off || !done) snap() else tween(100, delayMillis = 120), label = "tieni-spunta").value
    val draw = animateFloatAsState(
        if (done) 1f else 0f,
        if (off || !done) snap() else tween(450, delayMillis = 160, easing = androidx.compose.animation.core.CubicBezierEasing(0.65f, 0f, 0.35f, 1f)),
        label = "tieni-traccia",
    ).value
    val prod = Cm.prod
    val good = V3.Good
    val ink = V3.Bg
    val style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val pad = 18.dp
    androidx.compose.foundation.layout.Box(
        modifier
            .graphicsLayer { translationX = shake.value * density; scaleX = sc.value; scaleY = sc.value }
            .clip(CircleShape)
            .background(if (done) good else V3.S3)
            .drawWithContent {
                if (!done && !off) drawRect(prod, size = size.copy(width = size.width * p.value.coerceAtLeast(0f)))
                drawContent()
                if (checkA > 0f) {
                    val u = 26.dp.toPx() / 24f
                    val o = center - androidx.compose.ui.geometry.Offset(13.dp.toPx(), 13.dp.toPx())
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(o.x + 5f * u, o.y + 12.5f * u); lineTo(o.x + 9.5f * u, o.y + 17f * u); lineTo(o.x + 19f * u, o.y + 7.5f * u)
                    }
                    val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
                    val part = androidx.compose.ui.graphics.Path()
                    pm.getSegment(0f, pm.length * draw, part, true)
                    drawPath(part, ink.copy(alpha = checkA), style = androidx.compose.ui.graphics.drawscope.Stroke(2.4f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
            }
            .layout { m, c ->
                val pl = m.measure(c)
                val w = (pl.width + (pl.height - pl.width) * morph.value).roundToInt().coerceAtLeast(pl.height)
                layout(w, pl.height) { pl.place((w - pl.width) / 2, 0) }
            }
            .pointerInput(off) {
                detectTapGestures(onPress = {
                    if (done) return@detectTapGestures
                    scope.launch { if (!off) sc.animateTo(0.96f, Molla.of(520f, 20f)) }
                    val job = scope.launch {
                        if (off) kotlinx.coroutines.delay(Animazioni.HOLD_MS)
                        else p.animateTo(1f, tween(((1f - p.value.coerceIn(0f, 1f)) * Animazioni.HOLD_MS).toInt(), easing = androidx.compose.animation.core.LinearEasing))
                        done = true
                        if (!off) {
                            launch { morph.animateTo(1f, Molla.of(320f, 20f)) }
                            launch { sc.animateTo(1f, Molla.of(520f, 20f), initialVelocity = sc.velocity + 2.5f) }
                        }
                        kotlinx.coroutines.delay(if (off) 0L else 610L)
                        onDone()
                        kotlinx.coroutines.delay(if (off) 0L else 1700L - 610L)
                        done = false
                        p.snapTo(0f)
                        if (!off) morph.animateTo(0f, Molla.of(256f, 20f)) else morph.snapTo(0f)
                    }
                    tryAwaitRelease()
                    if (!done && job.isActive) {
                        job.cancel()
                        val kick = 260f * minOf(1f, p.value * 2f + 0.3f)
                        scope.launch { if (!off) sc.animateTo(1f, Molla.of(520f, 20f)) else sc.snapTo(1f) }
                        scope.launch { if (!off) p.animateTo(0f, Molla.of(260f, 14f)) else p.snapTo(0f) }
                        if (!off) scope.launch { shake.animateTo(0f, Molla.of(700f, 9f), initialVelocity = shake.velocity + kick) }
                    } else if (!done) scope.launch { sc.animateTo(1f, Molla.of(520f, 20f)) }
                })
            }
            .padding(horizontal = pad, vertical = 10.dp)
            .testTag("tieni-premuto"),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Box(Modifier.graphicsLayer { alpha = labelA }.blur(((1f - labelA) * 4f).dp), contentAlignment = Alignment.Center) {
            Text(label, style = style, color = V3.Tx, maxLines = 1)
            Text(
                label, style = style, color = ink, maxLines = 1,
                modifier = Modifier.drawWithContent {
                    val right = (p.value.coerceAtLeast(0f) * (size.width + 2 * pad.toPx()) - pad.toPx())
                    if (!off && right > 0f) clipRect(right = right) { this@drawWithContent.drawContent() }
                },
            )
        }
    }
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
internal fun ProofRail(steps: it.pixelbox.cmwatch.contract.ProofSteps, bleed: androidx.compose.ui.unit.Dp = 0.dp, inset: androidx.compose.ui.unit.Dp = 0.dp) {
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
    // app-passi-centrati-26-11 (Franz 26/11 15:02: «troppo a destra, serve ben centrato»): nella riga della sessione il filo esce a
    // sinistra della colonna del testo di [bleed] (la colonna del glifo) e rientra di [inset], lo stesso margine del lato destro: così
    // è centrato nella riga intera e non sotto il nome.
    val wide = Modifier.layout { m, c ->
        val extra = (bleed - inset).roundToPx()
        val w = (c.maxWidth + extra).coerceAtLeast(0)
        val p = m.measure(c.copy(minWidth = w, maxWidth = w))
        layout(c.maxWidth, p.height) { p.place(-extra, 0) }
    }
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().then(if (bleed > 0.dp) wide else Modifier).padding(top = 6.dp).testTag("passi-prova")) {
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
            Text(text, style = V3.Meter, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
        }
    }
}
