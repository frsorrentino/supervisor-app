package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.rules.Leave

/**
 * La spunta disegnata (D3 del mockup approvato da Franz, app-motion-loops-04-11, come Check.svelte): il tratto verde si disegna
 * in 780 ms dopo 120 ms; col movimento ridotto c'è già intera. Lo stesso tracciato della web, su 20 × 20.
 */
@Composable
internal fun DrawnCheck(modifier: Modifier = Modifier, size: Dp = 20.dp, color: Color = V3.Good) {
    val off = animationsOff()
    val p = remember { Animatable(if (off) 1f else 0f) }
    LaunchedEffect(Unit) { if (!off) p.animateTo(1f, tween(Leave.CHECK_DRAW_MS, delayMillis = Leave.CHECK_DELAY_MS)) }
    Canvas(modifier.size(size)) {
        val k = this.size.width / 20f
        val path = Path().apply { moveTo(4.5f * k, 10.5f * k); lineTo(8.5f * k, 14.5f * k); lineTo(15.5f * k, 6f * k) }
        val m = PathMeasure().apply { setPath(path, false) }
        val part = Path()
        m.getSegment(0f, m.length * p.value, part, true)
        drawPath(part, color, style = Stroke(width = 2.6f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Il contenuto di un tasto che diventa la conferma con la spunta: l'etichetta resta a dare la misura e sparisce in 180 ms, la
 * spunta si disegna al centro.
 */
@Composable
internal fun CheckLabel(confirmed: Boolean, label: @Composable () -> Unit) {
    val off = animationsOff()
    val a by androidx.compose.animation.core.animateFloatAsState(if (confirmed) 0f else 1f, if (off) androidx.compose.animation.core.snap() else tween(Leave.CHECK_LABEL_MS), label = "check-label")
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer { alpha = a }) { label() }
        if (confirmed) DrawnCheck()
    }
}

/** Il verde scuro del tasto confermato e il suo bordo (D3: #1E2A21 e #2F4A36 sul fondo della notte). */
internal val CheckFill: Color get() = androidx.compose.ui.graphics.lerp(V3.S2, V3.Good, 0.14f)
internal val CheckLine: Color get() = androidx.compose.ui.graphics.lerp(V3.S2, V3.Good, 0.34f)
