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
import androidx.compose.ui.graphics.drawscope.scale
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
import it.pixelbox.cmwatch.rules.PassiProva
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
 * Voce 3, fedele all'originale (Kokonut Thinking text; app-animazioni-fedeli-26-11): un'onda passa sul tempo di chi lavora, lettera per
 * lettera: opacità fino a 0,3 a metà di 1,6 s, ease-in-out sulle due metà, 70 ms di scarto per lettera. Con le animazioni spente, fermo.
 */
@Composable
internal fun WaveText(text: String, style: TextStyle, color: androidx.compose.ui.graphics.Color, step: Int = 70) {
    if (animationsOff()) { Text(text, style = style, color = color, maxLines = 1); return }
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "al-lavoro")
    val clock by t.animateFloat(
        0f, 1600f,
        androidx.compose.animation.core.infiniteRepeatable(tween(1600, easing = androidx.compose.animation.core.LinearEasing)),
        label = "onda",
    )
    val ease = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val shown = androidx.compose.ui.text.buildAnnotatedString {
        text.forEachIndexed { k, ch ->
            val ph = (((clock - k * step) % 1600f) + 1600f) % 1600f / 1600f
            val half = if (ph < 0.5f) ph * 2f else (1f - ph) * 2f
            pushStyle(androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = 1f - 0.7f * ease.transform(half))))
            append(ch)
            pop()
        }
    }
    Text(shown, style = style, color = color, maxLines = 1)
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

/**
 * Voce 4, fedele all'originale (Word roll; app-animazioni-fedeli-26-11): la cifra grande del tabellone, con [roll], cambia lettera per
 * lettera dopo un gesto. Le cifre vecchie salgono via a scalare di 22 ms, le nuove entrano da sotto dopo 60 ms a scalare di 28 ms, tutte
 * con la molla 420 26 (spostamento del 105 % dell'altezza, opacità 1 - 0,9·|y|); la larghezza va alla cifra nuova con la 320 30. Il
 * taglio è solo sopra e sotto. Senza gesto o con le animazioni spente, al posto.
 */
@Composable
internal fun RollNumber(value: String, roll: Boolean, content: @Composable (String) -> Unit) {
    if (!roll) { content(value); return }
    val off = animationsOff()
    class Word(val text: String, val id: Int) { val ys = text.map { androidx.compose.animation.core.Animatable(0f) } }
    var words by remember { androidx.compose.runtime.mutableStateOf(listOf(Word(value, 0))) }
    val wp = remember { androidx.compose.animation.core.Animatable(1f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(value) {
        val cur = words.last()
        if (value == cur.text) return@LaunchedEffect
        val go = !off && Animazioni.rolls(cur.text.toIntOrNull(), value.toIntOrNull() ?: 0, Gesture.recent())
        val new = Word(value, cur.id + 1)
        if (!go) { words = listOf(new); wp.snapTo(1f); return@LaunchedEffect }
        new.ys.forEach { it.snapTo(1f) }
        words = listOf(cur, new)
        wp.snapTo(0f)
        scope.launch { wp.animateTo(1f, Molla.of(320f, 30f)) }
        cur.ys.forEachIndexed { k, a -> scope.launch { kotlinx.coroutines.delay(k * 22L); a.animateTo(-1f, Molla.of(420f, 26f)) } }
        new.ys.forEachIndexed { k, a -> scope.launch { kotlinx.coroutines.delay(60L + k * 28L); a.animateTo(0f, Molla.of(420f, 26f)) } }
        kotlinx.coroutines.delay(1000)
        if (words.lastOrNull() === new) words = listOf(new)
    }
    Layout(
        content = {
            words.forEach { w ->
                androidx.compose.runtime.key(w.id) {
                    Row {
                        w.text.forEachIndexed { k, ch ->
                            Box(Modifier.graphicsLayer {
                                val y = w.ys[k].value
                                translationY = y * 1.05f * size.height
                                alpha = (1f - kotlin.math.abs(y) * 0.9f).coerceIn(0f, 1f)
                            }) { content(ch.toString()) }
                        }
                    }
                }
            }
        },
        modifier = Modifier.drawWithContent { clipRect(left = -size.width, right = size.width * 2f) { this@drawWithContent.drawContent() } },
    ) { ms, cs ->
        val ps = ms.map { it.measure(cs.copy(minWidth = 0)) }
        val to = ps.last()
        val from = ps.first()
        val w = (from.width + (to.width - from.width) * wp.value).roundToInt().coerceAtLeast(0)
        val h = ps.maxOf { it.height }
        val base = to[androidx.compose.ui.layout.FirstBaseline]
        layout(w, h, if (base != androidx.compose.ui.layout.AlignmentLine.Unspecified) mapOf(androidx.compose.ui.layout.FirstBaseline to base) else emptyMap()) {
            ps.forEach { it.placeRelative(0, 0) }
        }
    }
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
    // Fedele all'originale (Letter limit; app-animazioni-fedeli-26-11): l'anello va con la molla 280 22; a ogni carattere il numero fa
    // un pop (da 1,12, molla 520 16); ogni carattere oltre il limite dà una scossa (spinta 34 dp/s sulla molla 700 12, metà ampiezza)
    // e il pop da 1,25. Colori in 200 ms. Il testo oltre il limite non si taglia (regola dell'app).
    val state = Animazioni.limit(length, max)
    val off = animationsOff()
    val fillTo = Animazioni.fill(length, max)
    val F = remember { androidx.compose.animation.core.Animatable(fillTo) }
    val CP = remember { androidx.compose.animation.core.Animatable(1f) }
    val SH = remember { androidx.compose.animation.core.Animatable(0f) }
    var last by remember { mutableIntStateOf(length) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(length, off) {
        val before = last
        last = length
        if (off) { F.snapTo(fillTo); return@LaunchedEffect }
        scope.launch { F.animateTo(fillTo, Molla.of(280f, 22f)) }
        if (length == before) return@LaunchedEffect
        val over = length > max && length > before
        scope.launch { CP.snapTo(if (over) 1.25f else maxOf(CP.value, 1.12f)); CP.animateTo(1f, Molla.of(520f, 16f)) }
        if (over) scope.launch { SH.animateTo(0f, Molla.of(700f, 12f), initialVelocity = SH.velocity + 34f * (length - before)) }
    }
    val tone by androidx.compose.animation.animateColorAsState(
        when (state) { Animazioni.Limit.OK -> V3.Tx2; Animazioni.Limit.NEAR -> V3.Wait; Animazioni.Limit.OVER -> V3.Red }, if (off) snap() else tween(200), label = "limite-colore",
    )
    val track = V3.Tx.copy(alpha = 0.14f)
    val left = max - length
    val label = androidx.compose.ui.res.pluralStringResource(R.plurals.scelta_left, maxOf(0, left), maxOf(0, left))
    Row(Modifier.testTag("limite").semantics { contentDescription = label }.graphicsLayer { translationX = SH.value * 0.5f * density }, verticalAlignment = Alignment.CenterVertically) {
        if (state != Animazioni.Limit.OK) Text("$left", style = V3.Meter.copy(color = tone), modifier = Modifier.padding(end = 6.dp).graphicsLayer { scaleX = CP.value; scaleY = CP.value })
        androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
            val w = 3.dp.toPx()
            val st = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(track, 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = st)
            drawArc(tone, -90f, 360f * F.value.coerceIn(0f, 1f), false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = st)
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
            // Fedele all'originale (Kokonut Undo button; app-animazioni-fedeli-26-11): il bordo del tasto si svuota in senso orario dal
            // centro in alto, lineare in [Animazioni.UNDO_MS], tratto 1,5 dp; accanto i secondi che restano salgono di 10 dp, 200 ms,
            // ease-out (0.23, 1, 0.32, 1). Con le animazioni spente il bordo resta pieno e i secondi cambiano e basta.
            val ease = androidx.compose.animation.core.CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
            val secs = kotlin.math.ceil(ring.value * Animazioni.UNDO_MS / 1000f).toInt().coerceAtLeast(1)
            val tone = V3.Tx
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            Row(
                Modifier.clip(CircleShape).handCursor().clickable { Undo.cancel() }
                    .drawWithContent {
                        drawContent()
                        val sw = 1.5.dp.toPx()
                        val h = size.height; val w = size.width; val st = sw / 2; val r = h / 2 - st
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w / 2, st); lineTo(w - st - r, st)
                            arcTo(androidx.compose.ui.geometry.Rect(w - st - 2 * r, st, w - st, h - st), -90f, 180f, false)
                            lineTo(st + r, h - st)
                            arcTo(androidx.compose.ui.geometry.Rect(st, st, st + 2 * r, h - st), 90f, 180f, false)
                            close()
                        }
                        val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
                        val part = androidx.compose.ui.graphics.Path()
                        pm.getSegment(0f, pm.length * (if (off) 1f else ring.value), part, true)
                        drawPath(part, tone, style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.undo), style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = V3.Tx)
                AnimatedContent(
                    secs,
                    transitionSpec = {
                        if (off) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (androidx.compose.animation.slideInVertically(tween(200, easing = ease)) { (10 * density).toInt() } + fadeIn(tween(200, easing = ease))) togetherWith
                            (androidx.compose.animation.slideOutVertically(tween(200, easing = ease)) { -(10 * density).toInt() } + fadeOut(tween(200, easing = ease)))
                    },
                    label = "annulla-secondi",
                    modifier = Modifier.padding(start = 8.dp),
                ) { n -> Text("$n", style = V3.Meter.copy(color = V3.Tx2)) }
            }
        }
    }
}

/**
 * Voce 5, fedele agli originali (Step progress e Agent steps; app-animazioni-fedeli-26-11): le tappe del compito in verifica su un filo.
 * Il filo va alla tappa raggiunta con la molla 160 22; una tappa passata si riempie di verde (260 24) e la spunta si disegna 120 ms dopo
 * (140 20); la tappa che diventa quella in corso fa un pop da 0,7 (molla 500 14, spinta 6). La tappa in corso ha la rotella dell'Agent
 * steps (un giro in 0,8 s) e il nome con l'onda (opacità fino a 0,3 in 1,6 s, 50 ms di scarto per lettera); una fallita è rossa. Sotto,
 * a parole, dove si è. Con le animazioni spente niente movimento.
 */
@Composable
internal fun ProofRail(steps: it.pixelbox.cmwatch.contract.ProofSteps, bleed: androidx.compose.ui.unit.Dp = 0.dp, inset: androidx.compose.ui.unit.Dp = 0.dp) {
    val items = steps.items
    if (items.isEmpty()) return
    val off = animationsOff()
    val looks = items.map { PassiProva.look(it) }
    val n = items.size
    val bar = remember { androidx.compose.animation.core.Animatable(PassiProva.fill(items)) }
    val fs = remember(n) { List(n) { k -> androidx.compose.animation.core.Animatable(if (looks[k] == PassiProva.Look.DONE) 1f else 0f) } }
    val cs = remember(n) { List(n) { k -> androidx.compose.animation.core.Animatable(if (looks[k] == PassiProva.Look.DONE) 1f else 0f) } }
    val ps = remember(n) { List(n) { androidx.compose.animation.core.Animatable(1f) } }
    var prev by remember(n) { androidx.compose.runtime.mutableStateOf(looks) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(looks, off) {
        val before = prev
        prev = looks
        if (off) {
            bar.snapTo(PassiProva.fill(items))
            looks.forEachIndexed { k, l -> val d = if (l == PassiProva.Look.DONE) 1f else 0f; fs[k].snapTo(d); cs[k].snapTo(d); ps[k].snapTo(1f) }
            return@LaunchedEffect
        }
        scope.launch { bar.animateTo(PassiProva.fill(items), Molla.of(160f, 22f)) }
        looks.forEachIndexed { k, l ->
            val done = l == PassiProva.Look.DONE
            scope.launch { fs[k].animateTo(if (done) 1f else 0f, Molla.of(260f, 24f)) }
            if (done && cs[k].targetValue < 1f) scope.launch { kotlinx.coroutines.delay(120); cs[k].animateTo(1f, Molla.of(140f, 20f)) }
            if (!done) scope.launch { cs[k].snapTo(0f) }
            if (l == PassiProva.Look.NOW && before.getOrNull(k) != PassiProva.Look.NOW) scope.launch { ps[k].snapTo(0.7f); ps[k].animateTo(1f, Molla.of(500f, 14f), initialVelocity = 6f) }
        }
    }
    val running = !off && looks.any { it == PassiProva.Look.NOW }
    val clock = if (!running) 0f else {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "tappa")
        t.animateFloat(0f, 4000f, androidx.compose.animation.core.infiniteRepeatable(tween(4000, easing = androidx.compose.animation.core.LinearEasing)), label = "tempo").value
    }
    val turn = (clock % 800f) / 800f * 360f
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
            val cw = size.width / n
            val cy = size.height / 2
            val x0 = cw / 2; val x1 = size.width - cw / 2
            drawLine(line, androidx.compose.ui.geometry.Offset(x0, cy), androidx.compose.ui.geometry.Offset(x1, cy), 2.dp.toPx())
            if (n > 1) drawLine(good, androidx.compose.ui.geometry.Offset(x0, cy), androidx.compose.ui.geometry.Offset(x0 + (x1 - x0) * bar.value.coerceIn(0f, 1f), cy), 2.dp.toPx())
            val r = 10.dp.toPx()
            val round = { w: Float -> androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round) }
            looks.forEachIndexed { i, look ->
                val c = androidx.compose.ui.geometry.Offset(x0 + i * cw, cy)
                val f = fs[i].value.coerceIn(0f, 1f)
                scale(ps[i].value, c) {
                    if (look == PassiProva.Look.FAILED) {
                        drawCircle(red, r, c)
                        val d = r * .35f
                        drawLine(androidx.compose.ui.graphics.Color.Black, c + androidx.compose.ui.geometry.Offset(-d, -d), c + androidx.compose.ui.geometry.Offset(d, d), 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(androidx.compose.ui.graphics.Color.Black, c + androidx.compose.ui.geometry.Offset(d, -d), c + androidx.compose.ui.geometry.Offset(-d, d), 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                    } else {
                        drawCircle(androidx.compose.ui.graphics.lerp(bg, good, f), r, c)
                        if (f < 1f) {
                            val rr = r - 1.dp.toPx()
                            if (look == PassiProva.Look.NOW) {
                                drawCircle(busy.copy(alpha = 0.35f * (1f - f)), rr, c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                                drawArc(busy.copy(alpha = 1f - f), turn - 90f, 90f, false, topLeft = c - androidx.compose.ui.geometry.Offset(rr, rr), size = androidx.compose.ui.geometry.Size(2 * rr, 2 * rr), style = round(2.dp.toPx()))
                            } else drawCircle(ink.copy(alpha = 1f - f), rr, c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        }
                        val k = cs[i].value.coerceIn(0f, 1f)
                        if (k > 0f) {
                            val u = 16.dp.toPx() / 24f
                            val o = c - androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx())
                            val path = androidx.compose.ui.graphics.Path().apply { moveTo(o.x + 5f * u, o.y + 12.5f * u); lineTo(o.x + 9.5f * u, o.y + 17f * u); lineTo(o.x + 19f * u, o.y + 7.5f * u) }
                            val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
                            val part = androidx.compose.ui.graphics.Path()
                            pm.getSegment(0f, pm.length * k, part, true)
                            drawPath(part, androidx.compose.ui.graphics.Color.Black, style = round(2.8f * u))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            items.forEachIndexed { i, s ->
                val look = looks[i]
                val color = when (look) { PassiProva.Look.DONE -> V3.Tx; PassiProva.Look.NOW -> V3.Busy; PassiProva.Look.FAILED -> V3.Red; else -> V3.Tx2 }
                val wave = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
                val label = if (look == PassiProva.Look.NOW && running) androidx.compose.ui.text.buildAnnotatedString {
                    s.label.forEachIndexed { k, ch ->
                        val ph = (((clock - k * 50f) % 1600f) + 1600f) % 1600f / 1600f
                        val half = if (ph < 0.5f) ph * 2f else (1f - ph) * 2f
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = 1f - 0.7f * wave.transform(half))))
                        append(ch)
                        pop()
                    }
                } else AnnotatedString(s.label)
                Text(
                    label, style = V3.Small.copy(fontSize = 13.sp, color = color),
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
