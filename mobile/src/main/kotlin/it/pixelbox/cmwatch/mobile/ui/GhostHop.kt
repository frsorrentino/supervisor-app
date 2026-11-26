package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Voce 6a, fedele all'originale (Ghost suggestion; app-animazioni-fedeli-26-11, scelta della master del 26/11): preso il suggerimento,
 * le sue lettere vere, sopra il campo, passano dal grigio al colore pieno in 180 ms e fanno un piccolo salto (spinta di 70 dp/s verso
 * l'alto sulla molla 520 16), da sinistra a destra con 16 ms di scarto; poi resta il testo del campo. Dura [GhostHop.durationS].
 */
internal object GhostHop {
    const val STAGGER_S = 0.016
    const val COLOR_S = 0.18
    const val V0 = -70.0
    private const val A = 16.0 / 2
    private val WD = sqrt(520.0 - A * A)
    /** Lo spostamento della lettera, in dp, `t` secondi dopo la sua spinta (molla 520 16 a riposo, velocità iniziale [V0]). */
    fun y(t: Double): Double = if (t <= 0) 0.0 else V0 / WD * exp(-A * t) * sin(WD * t)
    fun durationS(chars: Int): Double = chars * STAGGER_S + 0.5
}

/**
 * Il suggerimento che salta, disegnato sopra il campo nello stesso punto del suo testo (il campo intanto lo tiene trasparente):
 * [start], [top] e [width] sono la posizione e la larghezza di riga del testo dentro il campo; [onDone] alla fine.
 */
@Composable
internal fun Modifier.ghostHop(text: String?, style: TextStyle, from: Color, to: Color, start: Float, top: Float, width: Int, onDone: () -> Unit): Modifier {
    if (text == null || width <= 0) return this
    var t by remember(text) { mutableFloatStateOf(0f) }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    LaunchedEffect(text) {
        var start0 = 0L
        val end = GhostHop.durationS(text.length)
        while (t < end) withFrameNanos { n -> if (start0 == 0L) start0 = n; t = ((n - start0) / 1e9).toFloat() }
        onDone()
    }
    return drawWithContent {
        drawContent()
        val colored = buildAnnotatedString {
            text.forEachIndexed { i, ch ->
                val k = ((t - i * GhostHop.STAGGER_S) / GhostHop.COLOR_S).coerceIn(0.0, 1.0).toFloat()
                pushStyle(SpanStyle(color = lerp(from, to, k)))
                append(ch)
                pop()
            }
        }
        val l = measurer.measure(colored, style, constraints = androidx.compose.ui.unit.Constraints(maxWidth = width), maxLines = 5)
        translate(start, top) {
            val last = l.getLineEnd(l.lineCount - 1)
            for (i in text.indices) {
                if (i >= last) break
                if (text[i].isWhitespace()) continue
                val b = l.getBoundingBox(i)
                if (b.width <= 0f) continue
                val line = l.getLineForOffset(i)
                val dy = GhostHop.y(t - i * GhostHop.STAGGER_S).dp.toPx()
                clipRect(b.left, l.getLineTop(line) - 4.dp.toPx(), b.right, l.getLineBottom(line)) {
                    translate(top = dy) { drawText(l) }
                }
            }
        }
    }
}
