package it.pixelbox.cmwatch.mobile.ui

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.AnteprimeAnimate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/*
 * Le anteprime che si muovono (app-anteprime-webp-27-11, Franz 27/11 20:49: «ovunque ci siano delle anteprime che devono mostrare
 * un'animazione, queste siano animate in loop o a tocco»). Un WebP o una GIF animati, nelle carte e in chat, girano in loop mentre sono
 * a schermo e l'app è davanti; fuori schermo (una riga della lista che scorre via esce dalla composizione), con l'app dietro o con le
 * animazioni ridotte si fermano. Ridotte, resta il primo fotogramma col ▶ sopra e il tocco li apre a schermo intero, dove girano.
 * Decodifica con ImageDecoder in un AnimatedImageDrawable (API 28; il telefono parte dalla 33, quindi c'è sempre). Le regole in
 * AnteprimeAnimate.
 */

/**
 * Il WebP o la GIF animati come pittore di Compose: l'AnimatedImageDrawable disegnato nel canvas. Un fotogramma nuovo ridisegna
 * attraverso la richiamata del Drawable; con l'accelerazione grafica i fotogrammi li fa avanzare da solo il thread di disegno.
 */
internal class MovingPainter(val drawable: AnimatedImageDrawable) : Painter() {
    private var tick by mutableIntStateOf(0)
    private val main = Handler(Looper.getMainLooper())
    private val callback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) { tick++ }
        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) { main.postAtTime(what, `when`) }
        override fun unscheduleDrawable(who: Drawable, what: Runnable) { main.removeCallbacks(what) }
    }

    init { drawable.callback = callback }

    override val intrinsicSize: Size get() = Size(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())

    override fun DrawScope.onDraw() {
        // Letto qui: un fotogramma nuovo (invalidateDrawable) ridisegna.
        tick
        drawIntoCanvas { c ->
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            drawable.draw(c.nativeCanvas)
        }
    }

    /** Uscito dalla composizione: fermo, senza richiamate né fotogrammi in coda. */
    fun release() {
        drawable.stop()
        drawable.callback = null
        main.removeCallbacksAndMessages(null)
    }
}

/** Si muove davvero (AnteprimeAnimate.moves): di un WebP basta l'intestazione, una GIF si legge tutta, fino al tetto dei file a pezzi. */
internal fun movesFile(path: String): Boolean = runCatching {
    val f = java.io.File(path)
    val head = f.inputStream().use { s -> ByteArray(32).let { b -> b.copyOf(s.read(b).coerceAtLeast(0)) } }
    val gif = head.size >= 3 && head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() && head[2] == 'F'.code.toByte()
    if (gif) f.length() <= 26_214_400L && AnteprimeAnimate.moves(f.readBytes()) else AnteprimeAnimate.moves(head)
}.getOrDefault(false)

/** Il WebP o la GIF animati entro `max` pixel sul lato lungo, in giro continuo; null per un file fermo o che non si legge. */
internal fun decodeMoving(path: String, max: Int): AnimatedImageDrawable? = runCatching {
    val d = ImageDecoder.decodeDrawable(ImageDecoder.createSource(java.io.File(path))) { decoder, info, _ ->
        val w = info.size.width
        val h = info.size.height
        val long = maxOf(w, h)
        if (long > max) decoder.setTargetSize((w.toLong() * max / long).toInt().coerceAtLeast(1), (h.toLong() * max / long).toInt().coerceAtLeast(1))
    }
    (d as? AnimatedImageDrawable)?.apply { repeatCount = AnimatedImageDrawable.REPEAT_INFINITE }
}.getOrNull()

/**
 * Il pittore animato di un file che si muove, letto fuori dal thread dell'interfaccia: null finché non c'è, per un file fermo o con
 * `on` falso. Uscito dalla composizione si ferma e lascia le sue richiamate.
 */
@Composable
internal fun rememberMoving(path: String?, max: Int, on: Boolean = true): MovingPainter? {
    val p by produceState<MovingPainter?>(null, path, max, on) {
        value = if (path == null || !on) null else withContext(Dispatchers.IO) { if (movesFile(path)) decodeMoving(path, max) else null }?.let(::MovingPainter)
    }
    // Quello di adesso: all'uscita `p` darebbe già il pittore nuovo.
    DisposableEffect(p) { val held = p; onDispose { held?.release() } }
    return p
}

/**
 * Il giro del pittore: parte mentre è nella composizione, a schermo (`visible`) e con l'app almeno avviata (STARTED); si ferma fuori
 * schermo, con l'app dietro (ON_STOP) e quando esce dalla composizione.
 */
@Composable
internal fun PlayWhile(painter: MovingPainter?, visible: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var started by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val o = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_START) started = true
            if (e == Lifecycle.Event.ON_STOP) started = false
        }
        lifecycle.addObserver(o)
        onDispose { lifecycle.removeObserver(o) }
    }
    val run = painter != null && AnteprimeAnimate.plays(visible && started, reduced = false)
    DisposableEffect(painter, run) {
        val d = painter?.drawable
        if (d != null) { if (run) d.start() else d.stop() }
        onDispose { d?.stop() }
    }
}

/**
 * Un'immagine di una carta o della chat che può muoversi. `still` è il primo fotogramma, già letto da chi la mostra (la lettura di
 * sempre), e ne dà la forma; `capable` dice se il tipo o l'estensione sono di un WebP o di una GIF. Se si muove davvero gira in loop a
 * schermo; con le animazioni ridotte (e negli snapshot) resta il primo fotogramma col ▶ disegnato sopra: il tocco è di chi la contiene,
 * nel suo `modifier`. Le altre sono l'immagine di sempre.
 */
@Composable
internal fun MovingImage(
    path: String?, capable: Boolean, still: ImageBitmap, contentDescription: String?, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit, alignment: Alignment = Alignment.Center, max: Int = 1600, small: Boolean = false,
) {
    if (path == null || !capable) { Image(still, contentDescription, modifier, alignment, contentScale); return }
    val off = animationsOff()
    // Negli snapshot si legge subito: fuori dal thread dell'interfaccia arriverebbe dopo la foto.
    val now = LocalStill.current || LocalInspectionMode.current
    val moves = if (now) remember(path) { movesFile(path) } else produceState(false, path) { value = withContext(Dispatchers.IO) { movesFile(path) } }.value
    var visible by remember { mutableStateOf(false) }
    // Con le animazioni ridotte non si legge nemmeno l'animazione: resta il primo fotogramma.
    val painter = rememberMoving(path, max, on = moves && !off)
    PlayWhile(painter, visible)
    val first = remember(still) { BitmapPainter(still) }
    // Un solo Image per tutti gli stati: i tocchi e il pizzico di chi la contiene restano gli stessi quando arriva l'animazione.
    val m = when {
        !moves -> modifier
        off -> modifier.playBadge(small)
        else -> modifier.onVisibilityChanged(minFractionVisible = 0.01f) { v -> visible = v }
    }
    val label = if (moves && off) contentDescription ?: stringResource(R.string.anim_play) else contentDescription
    Image(painter ?: first, label, m, alignment, contentScale)
}

/** Il ▶ sopra il primo fotogramma, come quello dei video: un cerchio nero al 55 % col triangolo bianco. */
private fun Modifier.playBadge(small: Boolean): Modifier = drawWithContent {
    drawContent()
    val r = (if (small) 15.dp else 26.dp).toPx()
    drawCircle(Color.Black.copy(alpha = 0.55f), r, center)
    // Il triangolo di PlayArrow (8,5 8,19 19,12 su 24), grande quanto l'icona del video.
    val u = r * 1.3f / 24f
    val tri = Path().apply {
        moveTo(center.x - 4f * u, center.y - 7f * u)
        lineTo(center.x - 4f * u, center.y + 7f * u)
        lineTo(center.x + 7f * u, center.y)
        close()
    }
    drawPath(tri, Color.White)
}
