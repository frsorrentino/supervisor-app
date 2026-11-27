package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Animazioni
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Voce 8 del terzo giro, fedele agli originali (Scrubber e Range slider; app-animazioni-terzo-c-26-11): sotto la pillola della lettura
 * una barra con un segno per paragrafo. La capsula che segna il punto è spenta (opacità 0,15, scala 0,7) e si accende mentre trascini
 * (0,8, 1) sulla molla di Scrubber (ζ 0,9, rigidità 1150); sopra il dito un'etichetta dice a quale paragrafo arrivi e si inclina con
 * la velocità come la bolla di Range slider (-0,02° per px/s, fino a 12°, perno in basso).
 *
 * Dopo la prova 1391 (Franz, 27/11: «troppo a scatti e poco fluido»): il punto sta all'inizio del paragrafo che la voce legge
 * ([Animazioni.scrubAt]) e quando la voce passa al dopo ci va in 150 ms con (0.23, 1, 0.32, 1). Il dito prende il punto appena tocca
 * e lo porta uno a uno, senza soglia di partenza; i segni dei paragrafi sono solo disegnati. Al rilascio il punto si aggancia al
 * paragrafo che comincia più vicino ([Animazioni.scrubNearest]) con la molla del pannello ([CmMotion.panelFloat]) e la lettura
 * riparte da lì. Mentre trascini niente si ricompone né si rimisura: punto e riempimento si leggono nel disegno, l'etichetta si
 * sposta con una trasformazione dentro un popup fermo. Il tocco su un paragrafo in chat resta. Animazioni spente: salti secchi.
 */
@Composable
internal fun ReadingScrub(blocks: Int, block: Int, onSeek: (Int) -> Unit) {
    val off = animationsOff()
    val offNow by rememberUpdatedState(off)
    val seek by rememberUpdatedState(onSeek)
    val ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    // Il punto, in frazione della barra, quando il dito non c'è.
    val pos = remember { Animatable(Animazioni.scrubAt(block, blocks)) }
    val cap = remember { Animatable(0f) }
    // Il dito, in frazione della barra (NaN senza dito), e l'inclinazione dell'etichetta: si leggono solo nel disegno e nelle trasformazioni.
    val finger = remember { mutableFloatStateOf(Float.NaN) }
    val tilt = remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var target by remember { mutableIntStateOf(block) }
    // Il paragrafo scelto al rilascio, finché la voce non ci arriva (al più 3 s): intanto il punto non torna su quello di prima.
    var landing by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val goal = landing ?: block
    LaunchedEffect(goal, blocks, dragging) {
        if (dragging) return@LaunchedEffect
        val t = Animazioni.scrubAt(goal, blocks)
        // La molla del rilascio ci sta già andando.
        if (pos.isRunning && pos.targetValue == t) return@LaunchedEffect
        if (off) pos.snapTo(t) else pos.animateTo(t, tween(150, easing = ease))
    }
    LaunchedEffect(block, landing) { if (landing == block) landing = null }
    LaunchedEffect(landing) { if (landing != null) { kotlinx.coroutines.delay(3_000); landing = null } }
    LaunchedEffect(dragging) {
        val t = if (dragging) 1f else 0f
        if (off) cap.snapTo(t) else cap.animateTo(t, spring(dampingRatio = 0.9f, stiffness = 1150f))
    }
    val track = V3.S3; val ink = V3.Tx; val tick = V3.Tx3
    val desc = stringResource(R.string.reading_seek, block + 1, blocks)
    // Dove sta la barra nella finestra: cambia solo se la barra si sposta, e lo legge la trasformazione dell'etichetta.
    val barLeft = remember { mutableFloatStateOf(0f) }
    val barWidth = remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp).height(24.dp).testTag("lettura-scrubber")
            .semantics { contentDescription = desc }
            .onGloballyPositioned { c -> barLeft.floatValue = c.positionInWindow().x; barWidth.floatValue = c.size.width.toFloat() },
    ) {
        Canvas(
            Modifier.fillMaxWidth().height(24.dp).pointerInput(blocks) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val vt = VelocityTracker()
                    vt.addPosition(down.uptimeMillis, down.position)
                    finger.floatValue = (down.position.x / w).coerceIn(0f, 1f)
                    target = Animazioni.scrubNearest(finger.floatValue, blocks)
                    dragging = true
                    var released = false
                    try {
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) { released = true; break }
                            if (!c.positionChanged()) continue
                            c.consume()
                            vt.addPosition(c.uptimeMillis, c.position)
                            finger.floatValue = (c.position.x / w).coerceIn(0f, 1f)
                            tilt.floatValue = (vt.calculateVelocity().x * -0.02f).coerceIn(-12f, 12f)
                            Animazioni.scrubNearest(finger.floatValue, blocks).let { k -> if (k != target) target = k }
                        }
                    } finally {
                        val f = finger.floatValue
                        if (released && !f.isNaN()) {
                            val k = Animazioni.scrubNearest(f, blocks)
                            landing = k
                            // Il punto riparte da dove l'ha lasciato il dito e va all'inizio del paragrafo con la molla del pannello.
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                pos.snapTo(f)
                                val t = Animazioni.scrubAt(k, blocks)
                                if (offNow) pos.snapTo(t) else pos.animateTo(t, CmMotion.panelFloat)
                            }
                            seek(k)
                        }
                        finger.floatValue = Float.NaN
                        tilt.floatValue = 0f
                        dragging = false
                    }
                }
            },
        ) {
            val f = finger.floatValue.let { x -> if (x.isNaN()) pos.value else x }.coerceIn(0f, 1f)
            val cy = size.height / 2
            val th = 8.dp.toPx()
            drawRoundRect(track, Offset(0f, cy - th / 2), Size(size.width, th), CornerRadius(th / 2))
            if (f > 0f) drawRoundRect(ink.copy(alpha = 0.14f), Offset(0f, cy - th / 2), Size(size.width * f, th), CornerRadius(th / 2))
            for (i in 1 until blocks) {
                val x = size.width * i / blocks
                drawRoundRect(tick, Offset(x - 0.5.dp.toPx(), cy - 3.dp.toPx()), Size(1.dp.toPx(), 6.dp.toPx()), CornerRadius(1.dp.toPx()))
            }
            val c = cap.value
            val k = 0.7f + 0.3f * c
            val cw = 4.dp.toPx() * k; val ch = 22.dp.toPx() * k
            drawRoundRect(ink.copy(alpha = 0.15f + 0.65f * c), Offset(size.width * f - cw / 2, cy - ch / 2), Size(cw, ch), CornerRadius(cw / 2))
        }
        if (dragging) {
            val screen = LocalConfiguration.current.screenWidthDp.dp
            val screenPx = with(LocalDensity.current) { screen.toPx() }
            Popup(popupPositionProvider = ScrubLabelWindow) {
                Box(Modifier.width(screen)) {
                    Text(
                        stringResource(R.string.reading_seek, target + 1, blocks), style = V3.Small.copy(color = Cm.onPrimary), maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            val f = finger.floatValue.let { x -> if (x.isNaN()) pos.value else x }.coerceIn(0f, 1f)
                            val x = barLeft.floatValue + f * barWidth.floatValue
                            translationX = (x - size.width / 2f).coerceIn(0f, (screenPx - size.width).coerceAtLeast(0f))
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            rotationZ = tilt.floatValue
                        }.clip(RoundedCornerShape(10.dp)).background(Cm.primary).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

/** Il popup dell'etichetta: fermo per tutto il trascinamento, largo quanto la finestra, sopra la barra di 1,6 volte la sua altezza. */
private object ScrubLabelWindow : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
        IntOffset(0, anchorBounds.top - popupContentSize.height - (anchorBounds.height * 1.6f).roundToInt())
}
