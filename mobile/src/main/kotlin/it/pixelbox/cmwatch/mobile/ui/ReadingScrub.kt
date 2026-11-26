package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Voce 8 del terzo giro, fedele agli originali (Scrubber e Range slider; app-animazioni-terzo-c-26-11): sotto la pillola della lettura
 * una barra con una tacca per paragrafo; il riempimento arriva alla fine del paragrafo che legge, e ai salti va in 150 ms con
 * (0.23, 1, 0.32, 1). La capsula che segna il punto è spenta (opacità 0,15, scala 0,7) e si accende mentre trascini (0,8, 1) sulla
 * molla di Scrubber (ζ 0,9, rigidità 1150). Trascinando, sopra il dito un'etichetta dice a quale paragrafo arrivi e si inclina con
 * la velocità come la bolla di Range slider (-0,02° per px/s, fino a 12°, perno in basso); al rilascio la lettura riparte da lì.
 * Il tocco su un paragrafo in chat resta. Animazioni spente: salti secchi.
 */
@Composable
internal fun ReadingScrub(blocks: Int, block: Int, onSeek: (Int) -> Unit) {
    val off = animationsOff()
    val seek by rememberUpdatedState(onSeek)
    val ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val fill = remember { Animatable((block + 1f) / blocks) }
    val cap = remember { Animatable(0f) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var tilt by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(block, blocks, drag == null) {
        if (drag != null) return@LaunchedEffect
        val t = (block + 1f) / blocks
        if (off) fill.snapTo(t) else fill.animateTo(t, tween(150, easing = ease))
    }
    LaunchedEffect(drag != null) {
        val t = if (drag != null) 1f else 0f
        if (off) cap.snapTo(t) else cap.animateTo(t, spring(dampingRatio = 0.9f, stiffness = 1150f))
    }
    val track = V3.S3; val ink = V3.Tx; val tick = V3.Tx3
    val desc = stringResource(R.string.reading_seek, block + 1, blocks)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp).height(24.dp).testTag("lettura-scrubber").semantics { contentDescription = desc }) {
        val w = constraints.maxWidth.toFloat()
        fun at(x: Float) = ((x / w).coerceIn(0f, 1f) * blocks).toInt().coerceIn(0, blocks - 1)
        val shown = drag?.let { d -> (d / w).coerceIn(0f, 1f) } ?: fill.value
        Canvas(
            Modifier.fillMaxWidth().height(24.dp)
                .pointerInput(blocks) { detectTapGestures(onTap = { o -> seek(at(o.x)) }) }
                .pointerInput(blocks) {
                    val vt = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { o -> vt.resetTracking(); drag = o.x },
                        onDragEnd = { drag?.let { d -> seek(at(d)) }; drag = null; scope.launch { tilt = 0f } },
                        onDragCancel = { drag = null; tilt = 0f },
                        onHorizontalDrag = { c, _ ->
                            drag = c.position.x
                            vt.addPosition(c.uptimeMillis, c.position)
                            tilt = (vt.calculateVelocity().x * -0.02f).coerceIn(-12f, 12f)
                        },
                    )
                },
        ) {
            val cy = size.height / 2
            val th = 8.dp.toPx()
            drawRoundRect(track, Offset(0f, cy - th / 2), Size(size.width, th), CornerRadius(th / 2))
            drawRoundRect(ink.copy(alpha = 0.14f), Offset(0f, cy - th / 2), Size(size.width * shown, th), CornerRadius(th / 2))
            for (i in 1 until blocks) {
                val x = size.width * i / blocks
                drawRoundRect(tick, Offset(x - 0.5.dp.toPx(), cy - 3.dp.toPx()), Size(1.dp.toPx(), 6.dp.toPx()), CornerRadius(1.dp.toPx()))
            }
            val c = cap.value
            val k = 0.7f + 0.3f * c
            val cw = 4.dp.toPx() * k; val ch = 22.dp.toPx() * k
            drawRoundRect(ink.copy(alpha = 0.15f + 0.65f * c), Offset(size.width * shown - cw / 2, cy - ch / 2), Size(cw, ch), CornerRadius(cw / 2))
        }
        drag?.let { d ->
            val target = at(d)
            Popup(popupPositionProvider = remember(d) { ScrubLabel(d.roundToInt()) }) {
                Text(
                    stringResource(R.string.reading_seek, target + 1, blocks), style = V3.Small.copy(color = Cm.onPrimary),
                    modifier = Modifier.graphicsLayer { transformOrigin = TransformOrigin(0.5f, 1f); rotationZ = tilt }
                        .clip(RoundedCornerShape(10.dp)).background(Cm.primary).padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** L'etichetta sopra il dito, 40 dp sopra la barra come la bolla di Range slider. */
private class ScrubLabel(val x: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val cx = (anchorBounds.left + x - popupContentSize.width / 2).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        return IntOffset(cx, anchorBounds.top - popupContentSize.height - (anchorBounds.height * 1.6f).roundToInt())
    }
}
