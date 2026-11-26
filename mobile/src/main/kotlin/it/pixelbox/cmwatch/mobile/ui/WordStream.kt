package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Voce 1, fedele all'originale (Streaming reply; app-animazioni-fedeli-26-11), parole della risposta che arriva a chat aperta: ogni parola
 * parte 70 ms dopo la precedente (oltre 20 parole l'intervallo si accorcia e la risposta intera esce in 1,4 s, scelta b della master
 * del 26/11), sale di 6 dp, l'opacità va da 0 e passa da sfocata (4 dp) a nitida con la molla 340 26; la scheda cresce fino alla riga
 * dell'ultima parola con la molla 260 26. Il testo resta un testo vero (grassetto, codice, link, selezione): le parole si disegnano
 * ritagliate dal suo disegno. La sfocatura c'è da Android 12; prima solo salita e opacità.
 */
internal class WordStream(val words: Int) {
    var start by mutableLongStateOf(0L)
    var now by mutableLongStateOf(0L)
    var done by mutableStateOf(false)
    /** Il fondo, in coordinate della radice, della riga dell'ultima parola partita. */
    var bottom by mutableFloatStateOf(0f)
    private var next = 0
    fun reserve(n: Int): Int { val i = next; next += n; return i }
    val gapS: Double = (if (words <= GAP_WORDS) GAP_MS else TOTAL_MS / words) / 1000.0
    /** Secondi dalla partenza della parola `i`; negativo se non è ancora partita. */
    fun t(i: Int): Double = if (start == 0L) -1.0 else (now - start) / 1e9 - i * gapS
    val endS: Double get() = words * gapS + SETTLE_S

    companion object {
        const val GAP_MS = 70.0
        const val GAP_WORDS = 20
        const val TOTAL_MS = 1400.0
        const val SETTLE_S = 1.0
        val WORD = Regex("\\S+")
        fun count(text: String): Int = WORD.findAll(text).count()
        private val A = 26.0 / 2
        private val WD = sqrt(340.0 - A * A)
        /** La molla 340 26 da 0 a 1, in forma chiusa (massa 1). */
        fun x(t: Double): Double = if (t <= 0) 0.0 else 1 - exp(-A * t) * (cos(WD * t) + A / WD * sin(WD * t))
    }
}

/** Chi decide quali risposte scorrono: solo quelle arrivate dopo l'apertura della chat, una volta sola. */
internal class StreamGate {
    private val known = mutableSetOf<String>()
    var primed = false
        private set
    fun prime(ids: Collection<String>) { if (!primed) { known.addAll(ids); primed = true } }
    fun claim(id: String): Boolean = primed && known.add(id)
}

internal val LocalStreamGate = compositionLocalOf<StreamGate?> { null }
internal val LocalWordStream = compositionLocalOf<WordStream?> { null }

/** La scheda della risposta che scorre: cresce fino alla riga dell'ultima parola partita, poi torna alla sua altezza. */
@Composable
internal fun Modifier.streamBox(ws: WordStream?): Modifier {
    if (ws == null) return this
    val d = LocalDensity.current
    val pill = with(d) { 40.dp.toPx() }
    val pad = with(d) { 16.dp.toPx() }
    val h = remember(ws) { Animatable(pill) }
    val full = remember(ws) { intArrayOf(0) }
    var top by remember(ws) { mutableFloatStateOf(0f) }
    LaunchedEffect(ws) {
        while (!ws.done) withFrameNanos { n -> if (ws.start == 0L) ws.start = n; ws.now = n; if (ws.t(0) > ws.endS) ws.done = true }
    }
    val target = when {
        ws.done -> full[0].toFloat()
        ws.bottom == 0f -> pill
        else -> ws.bottom - top + pad
    }
    LaunchedEffect(target) { h.animateTo(target, Molla.of(260f, 26f)) }
    return onGloballyPositioned { top = it.positionInRoot().y }.clipToBounds().layout { m, c ->
        val p = m.measure(c)
        full[0] = p.height
        val settled = ws.done && !h.isRunning
        val ht = if (settled) p.height else h.value.roundToInt().coerceIn(0, p.height)
        layout(p.width, ht) { p.place(0, 0) }
    }
}

/** Un paragrafo della risposta: se scorre, le sue parole entrano una alla volta. */
@Composable
internal fun StreamText(text: AnnotatedString, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val ws = LocalWordStream.current
    if (ws == null || ws.done) { Text(text, modifier = modifier, color = color, style = style); return }
    var tl by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(text, modifier = modifier.streamWords(ws, text.text) { tl }, color = color, style = style, onTextLayout = { tl = it })
}

@Composable
private fun Modifier.streamWords(ws: WordStream, text: String, layout: () -> TextLayoutResult?): Modifier {
    val ranges = remember(text) { WordStream.WORD.findAll(text).map { it.range }.toList() }
    val base = remember(ws) { ws.reserve(ranges.size) }
    val blur = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val gc = LocalGraphicsContext.current
    val d = LocalDensity.current
    // Quattro livelli di sfocatura (1-4 dp): ogni parola prende il più vicino al suo valore.
    val layers = remember(ws) {
        if (!blur) emptyList() else List(4) { i -> gc.createGraphicsLayer().also { l -> val r = with(d) { (i + 1).dp.toPx() }; l.renderEffect = BlurEffect(r, r) } }
    }
    DisposableEffect(layers) { onDispose { layers.forEach(gc::releaseGraphicsLayer) } }
    var rootY by remember { mutableFloatStateOf(0f) }
    return onGloballyPositioned { rootY = it.positionInRoot().y }.drawWithContent {
        val l = layout()
        if (l == null || ws.done) { drawContent(); return@drawWithContent }
        ws.now
        val lift = 6.dp.toPx()
        val recorded = BooleanArray(layers.size)
        val paint = Paint()
        ranges.forEachIndexed { j, r ->
            val t = ws.t(base + j)
            if (t <= 0) return@forEachIndexed
            val x = WordStream.x(t)
            val a = x.coerceIn(0.0, 1.0).toFloat()
            val line = l.getLineForOffset(r.first)
            val endLine = l.getLineForOffset(r.last)
            val left = l.getBoundingBox(r.first).left
            val right = if (endLine == line) l.getBoundingBox(r.last).right else l.getLineRight(line)
            val wTop = l.getLineTop(line)
            val wBot = l.getLineBottom(line)
            if (rootY + wBot > ws.bottom) ws.bottom = rootY + wBot
            val v = min(x, 1.2)
            val level = if (layers.isEmpty() || v >= 0.98) 0 else ((1 - a) * 4f).roundToInt().coerceIn(0, 4)
            translate(top = ((1 - x) * lift).toFloat()) {
                clipRect(left - 1f, wTop - lift, right + 1f, wBot + lift) {
                    paint.alpha = a
                    drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(left - 1f, wTop - lift, right + 1f, wBot + lift), paint)
                    if (level == 0) drawContent()
                    else {
                        val g = layers[level - 1]
                        if (!recorded[level - 1]) { g.record { this@drawWithContent.drawContent() }; recorded[level - 1] = true }
                        drawLayer(g)
                    }
                    drawContext.canvas.restore()
                }
            }
        }
    }
}
