package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.collectIsPressedAsState

/**
 * Gli effetti sui movimenti dell'app (app-effetti-movimenti-04-11, Franz 04/11 21:54-22:41, valori da tarare dal vivo), con
 * i tempi della web (cardFx.ts, motion.ts, landingMorph.ts, v3.css). Col movimento ridotto nessuno.
 */
object Fx {
    /** La molla che supera di poco la misura e torna (--spring-fx della web, circa 2,5 %). */
    fun <T> spring() = spring<T>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
    /** La scheda che si contrae al tocco (punto 1). */
    const val PRESS = 0.98f
    /** Le sezioni si aprono come un foglio, piegate all'indietro di tanto (punto 5). */
    const val SHEET_DEG = 14f
    const val SHEET_MS = 260
    /** La linea del lavoro che si raccoglie in verde (punto 7). */
    const val GATHER_MS = 520
    /** Le colonne all'avvio, una dopo l'altra (punto 6). */
    const val WAVE_STEP_MS = 60
    const val WAVE_RISE_DP = 14f
    /** La master che atterra e si schiaccia (punto 2), gli angoli che si piegano (punto 8). */
    const val SQUASH_MS = 280
    const val CORNERS_MS = 560
}

/**
 * Punto 5: il contenuto che compare scende dal bordo di sopra piegato all'indietro e si distende, come un foglio. Si usa sul
 * corpo di una sezione appena aperta (FoldIn), mai su ogni voce che entra in vista scorrendo.
 */
@Composable
internal fun Modifier.sheetIn(): Modifier {
    if (animationsOff()) return this
    val a = remember { Animatable(-Fx.SHEET_DEG) }
    LaunchedEffect(Unit) { a.animateTo(0f, tween(Fx.SHEET_MS, easing = CmMotion.easing)) }
    return this.graphicsLayer { rotationX = a.value; transformOrigin = TransformOrigin(0.5f, 0f); cameraDistance = 12f * density }
}

/**
 * Punto 7: la linea del lavoro in corso; quando il lavoro finisce si raccoglie verso il centro nel verde di «finito» fino a un
 * punto, che si spegne. Col movimento ridotto sparisce e basta.
 */
@Composable
internal fun WorkEdge(working: Boolean, modifier: Modifier = Modifier) {
    val off = animationsOff()
    var shown by remember { mutableStateOf(working) }
    val g = remember { Animatable(0f) }
    LaunchedEffect(working) {
        if (working) { g.snapTo(0f); shown = true }
        else if (shown) { if (!off) g.animateTo(1f, tween(Fx.GATHER_MS)); shown = false; g.snapTo(0f) }
    }
    if (!shown) return
    if (working || g.value == 0f) { WorkEdgeLine(modifier); return }
    Box(modifier.fillMaxWidth().height(2.dp).drawBehind {
        val u = g.value
        val k = (u / 0.6f).coerceAtMost(1f)
        val sx = 1f - 0.985f * (1f - (1f - k) * (1f - k) * (1f - k))
        val o = if (u > 0.6f) 1f - (u - 0.6f) / 0.4f else 1f
        val w = size.width * sx
        drawRect(V3.Good.copy(alpha = o), topLeft = Offset((size.width - w) / 2f, 0f), size = Size(w, size.height))
    })
}

/**
 * Punto 4: la scheda confermata esce girandosi, piegandosi in una striscia e salendo verso il contatore di «Tocca a te», che sta
 * sopra; al posto del solo restringersi in altezza.
 */
internal fun confirmedExit(off: Boolean): androidx.compose.animation.ExitTransition =
    if (off) androidx.compose.animation.ExitTransition.None
    else androidx.compose.animation.scaleOut(tween(650, easing = androidx.compose.animation.core.CubicBezierEasing(0.45f, 0f, 0.25f, 1f)), targetScale = 0.08f, transformOrigin = TransformOrigin(0.5f, 0f)) +
        androidx.compose.animation.slideOutVertically(tween(650)) { h -> -h * 2 } +
        androidx.compose.animation.shrinkVertically(tween(650, delayMillis = 200)) +
        androidx.compose.animation.fadeOut(tween(250, delayMillis = 400))

/** Punto 1: la scheda toccata si contrae al 98 % e torna. */
@Composable
internal fun Modifier.cardPress(source: androidx.compose.foundation.interaction.MutableInteractionSource): Modifier {
    if (animationsOff()) return this
    val pressed by source.collectIsPressedAsState()
    val s by androidx.compose.animation.core.animateFloatAsState(if (pressed) Fx.PRESS else 1f, tween(80), label = "card-press")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/** L'onda all'avvio c'è una volta sola, alla prima plancia dopo l'apertura dell'app. */
internal object BootWave { var done = false }

/**
 * Punto 6: all'avvio le colonne della plancia salgono di 14 dp e si assestano con la molla, una dopo l'altra (60 ms a passo),
 * come `.pboot` della web. Dopo la prima volta niente: il riposizionamento lo fanno i bordi condivisi.
 */
@Composable
internal fun Modifier.bootWave(i: Int): Modifier {
    // Si decide alla prima composizione: tutte le colonne della stessa passata la vedono ancora da fare.
    val run = remember { !BootWave.done }
    androidx.compose.runtime.SideEffect { BootWave.done = true }
    if (animationsOff() || !run) return this
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(i * Fx.WAVE_STEP_MS.toLong())
        a.animateTo(1f, Fx.spring())
    }
    return this.graphicsLayer { alpha = a.value.coerceIn(0f, 1f); translationY = (1f - a.value) * Fx.WAVE_RISE_DP * density }
}

/**
 * Punto 1 sul tablet: il collo, come cardFx.ts della web. Al tocco la scheda si contrae al 98 % per 50 ms, poi si alza di 10 dp e
 * fra lei e il suo posto resta un ponte della sua superficie che si stringe al centro (dal 100 al 25 % della larghezza) e si
 * spezza a 140 ms; poi la scheda vola subito nella colonna coi bordi condivisi (tempi in rules/Morph.kt).
 */
internal class Neck { val t = Animatable(0f) }

internal object NeckGeo {
    const val MS = it.pixelbox.cmwatch.rules.Morph.NECK_MS
    const val PRESS_MS = it.pixelbox.cmwatch.rules.Morph.NECK_PRESS_MS
    const val LIFT_DP = it.pixelbox.cmwatch.rules.Morph.NECK_LIFT_DP
    const val WAIST = 0.25f
    private fun ease(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)
    /** Quanto è alzata la scheda, da 0 a 1, al punto `t` del collo. */
    fun up(t: Float): Float { val p = PRESS_MS.toFloat() / MS; return if (t < p) 0f else ease(((t - p) / (1f - p)).coerceIn(0f, 1f)) }
    fun scale(t: Float): Float { val p = PRESS_MS.toFloat() / MS; return if (t < p) 1f - 0.02f * (t / p) else 0.98f + 0.02f * up(t) }
    fun waist(t: Float): Float = 1f - (1f - WAIST) * Math.pow(t.coerceIn(0f, 1f).toDouble(), 1.6).toFloat()
}

/** Il collo sulla scheda: il ponte disegnato sotto, nel posto lasciato, e la scheda alzata sopra. */
internal fun Modifier.neck(n: Neck, color: androidx.compose.ui.graphics.Color): Modifier = this
    .drawBehind {
        val t = n.t.value
        if (t <= 0f || t >= 1f) return@drawBehind
        val lift = NeckGeo.LIFT_DP * density * NeckGeo.up(t)
        if (lift < 1f) return@drawBehind
        val w = size.width; val top = size.height - lift; val bottom = size.height; val mid = (top + bottom) / 2f
        val half = w * NeckGeo.waist(t) / 2f; val cx = w / 2f
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, top)
            cubicTo(w * 0.12f, top, cx - half, mid - (mid - top) * 0.4f, cx - half, mid)
            cubicTo(cx - half, mid + (bottom - mid) * 0.4f, w * 0.12f, bottom, 0f, bottom)
            lineTo(w, bottom)
            cubicTo(w * 0.88f, bottom, cx + half, mid + (bottom - mid) * 0.4f, cx + half, mid)
            cubicTo(cx + half, mid - (mid - top) * 0.4f, w * 0.88f, top, w, top)
            close()
        }
        drawPath(p, color)
    }
    .graphicsLayer {
        val t = n.t.value
        if (t > 0f) { val s = NeckGeo.scale(t); scaleX = s; scaleY = s; translationY = -NeckGeo.LIFT_DP * density * NeckGeo.up(t) }
    }
