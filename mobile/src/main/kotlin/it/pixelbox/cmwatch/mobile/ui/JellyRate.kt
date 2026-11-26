package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Voce 5 del terzo giro, fedele all'originale (Jelly slider; app-animazioni-terzo-c-26-11), con la gelatina b: il collo fra pollice e
 * goccia è una forma calcolata (metaball), non un filtro. Valori dell'originale, in dp:
 * - il pollice segue il dito sulla molla 900 50, al rilascio torna dentro sulla 300 14; oltre le estremità l'elastico (fino a 18 dp,
 *   pendenza 0,55); si allunga con la velocità, `1 + min(|v|/2400, 0,4)`, conservando l'area; premuto scala a 1,16 (520 16);
 * - la goccia (raggio 18) sale di 38 dp sopra la traccia e scala da 0,5 a 1 (420 22); in orizzontale insegue il pollice sulla 260 18 e
 *   si inclina col ritardo, `(DX - X) * 0,9`, fino a 28°; il numero compare da scala 0,7, pieno a 0,95;
 * - il riempimento finisce sotto il centro del pollice e sparisce negli ultimi 12 dp.
 * Animazioni spente: pollice e goccia criticamente smorzati, come nell'originale; ritardo e scala del tocco restano.
 */
internal class JellySim {
    class S(var x: Float, var k: Float, var c: Float) { var v = 0f; var t = x }
    val X = S(0f, 900f, 50f)
    val DX = S(0f, 260f, 18f)
    val DY = S(0f, 420f, 22f)
    val DS = S(0.5f, 420f, 22f)
    val PS = S(1f, 520f, 16f)
    var ready = false
    fun step(dt: Float) {
        val sub = dt / 4
        repeat(4) {
            DX.t = X.x
            for (s in arrayOf(X, DX, DY, DS, PS)) { val a = -s.k * (s.x - s.t) - s.c * s.v; s.v += a * sub; s.x += s.v * sub }
        }
    }
    val still: Boolean get() = arrayOf(X, DX, DY, DS, PS).all { abs(it.x - it.t) < 0.05f && abs(it.v) < 0.05f }

    companion object {
        /** L'elastico oltre le estremità, dall'originale: asintoto [d] oltre il bordo, pendenza 0,55. */
        fun rubber(x: Float, lo: Float, hi: Float, d: Float): Float = when {
            x < lo -> lo - (1 - 1 / ((lo - x) * 0.55f / d + 1)) * d
            x > hi -> hi + (1 - 1 / ((x - hi) * 0.55f / d + 1)) * d
            else -> x
        }

        /** Il collo fra due cerchi (metaball, gelatina b): null se sono troppo lontani o uno sta dentro l'altro. */
        fun neck(c1: Offset, r1: Float, c2: Offset, r2: Float, maxDist: Float, v: Float = 0.5f, handle: Float = 2.4f): Path? {
            val d = hypot(c2.x - c1.x, c2.y - c1.y)
            if (d > maxDist || d <= abs(r1 - r2) || r1 <= 0f || r2 <= 0f) return null
            val (u1, u2) = if (d < r1 + r2) acos(((r1 * r1 + d * d - r2 * r2) / (2 * r1 * d)).coerceIn(-1f, 1f)) to acos(((r2 * r2 + d * d - r1 * r1) / (2 * r2 * d)).coerceIn(-1f, 1f)) else 0f to 0f
            val a = atan2(c2.y - c1.y, c2.x - c1.x)
            val spread = acos(((r1 - r2) / d).coerceIn(-1f, 1f))
            val pi = PI.toFloat()
            val a1 = a + u1 + (spread - u1) * v
            val a2 = a - u1 - (spread - u1) * v
            val a3 = a + pi - u2 - (pi - u2 - spread) * v
            val a4 = a - pi + u2 + (pi - u2 - spread) * v
            fun on(c: Offset, r: Float, t: Float) = Offset(c.x + r * cos(t), c.y + r * sin(t))
            val p1 = on(c1, r1, a1); val p2 = on(c1, r1, a2); val p3 = on(c2, r2, a3); val p4 = on(c2, r2, a4)
            val d2 = min(v * handle, hypot(p1.x - p3.x, p1.y - p3.y) / (r1 + r2)) * min(1f, d * 2 / (r1 + r2))
            val h1 = on(p1, r1 * d2, a1 - pi / 2); val h2 = on(p2, r1 * d2, a2 + pi / 2)
            val h3 = on(p3, r2 * d2, a3 + pi / 2); val h4 = on(p4, r2 * d2, a4 - pi / 2)
            return Path().apply {
                moveTo(p1.x, p1.y); cubicTo(h1.x, h1.y, h3.x, h3.y, p3.x, p3.y)
                lineTo(p4.x, p4.y); cubicTo(h4.x, h4.y, h2.x, h2.y, p2.x, p2.y); close()
            }
        }
    }
}

/** Fa girare la simulazione finché si muove o [running]; [tick] cambia a ogni passo, per ridisegnare. */
@Composable
internal fun rememberJellyClock(sim: JellySim, running: Boolean, off: Boolean, target: Float): Long {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sim, running, off, target) {
        var last = 0L
        while (true) {
            val more = withFrameNanos { n ->
                val dt = if (last == 0L) 0f else min((n - last) / 1e9f, 1f / 30f)
                last = n
                if (off) {
                    // Come `tune` dell'originale: pollice e goccia criticamente smorzati.
                    for (s in arrayOf(sim.X, sim.DY, sim.DS)) s.c = 2 * sqrt(s.k)
                }
                sim.step(dt)
                tick = n
                running || !sim.still
            }
            if (!more) break
        }
    }
    return tick
}

/** Il pollice (ellisse allungata dalla velocità, scalata dal tocco), il collo e la goccia col numero, attorno a [base] (centro del pollice in quiete). */
internal fun DrawScope.drawJelly(sim: JellySim, thumb: Offset, color: Color, ink: Color, label: String, measurer: TextMeasurer, style: TextStyle, withDrop: Boolean) {
    val r = 12.dp.toPx()
    val st = 1f + min(abs(sim.X.v) / 2400f, 0.4f)
    val ps = sim.PS.x
    val rx = r * st * ps; val ry = r * ps / st
    drawOval(color, Offset(thumb.x - rx, thumb.y - ry), Size(2 * rx, 2 * ry))
    if (!withDrop) return
    val dr = 18.dp.toPx() * sim.DS.x
    val drop = Offset(thumb.x + (sim.DX.x - sim.X.x), thumb.y + sim.DY.x)
    JellySim.neck(thumb, min(rx, ry).coerceAtLeast(1f), drop, dr, r + dr + 16.dp.toPx())?.let { drawPath(it, color) }
    drawCircle(color, dr, drop)
    val a = ((sim.DS.x - 0.7f) / 0.25f).coerceIn(0f, 1f)
    if (a > 0f) {
        val tilt = ((sim.DX.x - sim.X.x) * 0.9f / density).coerceIn(-28f, 28f)
        val l = measurer.measure(label, style.copy(color = ink.copy(alpha = a)))
        rotate(tilt, drop) { drawText(l, topLeft = Offset(drop.x - l.size.width / 2f, drop.y - l.size.height / 2f)) }
    }
}

/** Il popup della goccia: largo quanto la finestra sopra lo slider, così collo e goccia non si tagliano al bordo della pillola. */
internal class JellyWindow(private val lift: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
        IntOffset(anchorBounds.left - (popupContentSize.width - anchorBounds.width) / 2, anchorBounds.top + anchorBounds.height / 2 - lift)
}

@Composable
internal fun JellyPopup(lift: Int, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, draw: DrawScope.() -> Unit) {
    Popup(popupPositionProvider = remember(lift) { JellyWindow(lift) }) {
        Canvas(Modifier.size(width, height)) { draw() }
    }
}

/** Il centro del pollice in px per una frazione 0-1 della corsa: corsa da 12 dp a larghezza - 12 dp, come nell'originale. */
internal fun jellyX(f: Float, w: Float, r: Float): Float = r + f.coerceIn(0f, 1f) * (w - 2 * r)

