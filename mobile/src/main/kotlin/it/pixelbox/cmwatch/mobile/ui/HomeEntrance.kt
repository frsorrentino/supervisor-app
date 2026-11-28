package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.rules.EntrataHome
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * app-entrata-home-27-11 (Franz 27/11 20:46, variante B del mockup): alla prima home del processo i blocchi della lista entrano
 * dall'alto uno dopo l'altro, 70 ms + 45 ms per posizione. Ognuno sfuma da 0 e da sfocato (5 dp, da Android 12) in 0,4 s ease, sale
 * di 24 dp e scala da 0,94 con la molla 220/15 (smorzamento 0,506). Tornando alla home da una sessione la lista si rifà ma non
 * rientra; con «riduci movimento» compare ferma. La chiede solo MainActivity: test, anteprime e colonne della plancia restano fermi.
 */
internal object HomeEntrance {
    private val once = EntrataHome.Once()

    /** La corsa per la home che si compone adesso; null se in questo processo c'è già stata, o col movimento ridotto. */
    fun start(reduced: Boolean): EntrataRun? = if (EntrataHome.plays(once.take(), reduced)) EntrataRun() else null
}

/** Una corsa: le posizioni dall'alto, nell'ordine in cui la lista compone i blocchi al primo disegno. */
@Stable
internal class EntrataRun {
    val order = EntrataHome.Order()
}

/** Un blocco che entra: la dissolvenza (da 0 a 1) e lo scostamento della molla (da 1 al posto, 0). */
@Stable
internal class EntrataBlock(val index: Int) {
    val fade = Animatable(0f)
    val rise: Animatable<Float, AnimationVector1D> = Animatable(1f)
}

/** L'«ease» del CSS, come nel mockup. */
private val EASE = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** Il rimbalzo delle barre del dock, cubic-bezier(.34, 1.56, .64, 1). */
private val BAR_EASE = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * Il blocco della lista che si compone adesso, con la sua posizione; null fuori dall'entrata o arrivato dopo il primo disegno. Il
 * conto parte dal fotogramma dopo il primo, uguale per tutti: un primo disegno lento non brucia i ritardi dei primi blocchi.
 */
@Composable
internal fun entrataBlock(run: EntrataRun?): EntrataBlock? {
    if (run == null) return null
    val index = remember { run.order.next() } ?: return null
    val b = remember { EntrataBlock(index) }
    LaunchedEffect(b) {
        withFrameNanos { }
        delay(EntrataHome.delayMs(b.index).toLong())
        launch { b.fade.animateTo(1f, tween(EntrataHome.FADE_MS, easing = EASE)) }
        b.rise.animateTo(0f, Molla.of(EntrataHome.SPRING.k, EntrataHome.SPRING.c))
    }
    return b
}

/** Il blocco [b] al suo punto dell'entrata; senza blocco il modificatore non c'è e la voce resta com'era (gli snapshot non cambiano). */
internal fun Modifier.entrata(b: EntrataBlock?): Modifier = if (b == null) this else graphicsLayer {
    val f = b.fade.value
    val y = b.rise.value
    alpha = f
    translationY = EntrataHome.RISE.dp.toPx() * y
    val s = 1f - (1f - EntrataHome.SCALE_FROM) * y
    scaleX = s
    scaleY = s
    // La sfocatura Android la dà da 12 in poi; prima il blocco entra senza.
    val r = EntrataHome.BLUR.dp.toPx() * (1f - f)
    renderEffect = if (r > 0.5f && android.os.Build.VERSION.SDK_INT >= 31) BlurEffect(r, r, TileMode.Decal) else null
}

/** La barra della settimana piena: fuori dall'entrata la crescita vale sempre 1. */
private val FULL: () -> Float = { 1f }

/** Quanto è cresciuta la barra della settimana del tabellone: 1 ferma, da 0 a 1 (oltre nel rimbalzo) durante l'entrata. */
internal val LocalWeekBarGrow = staticCompositionLocalOf { FULL }

/** La crescita della barra della settimana del tabellone [b]: 220 ms dopo il suo riquadro, in 0,8 s col rimbalzo; fuori dall'entrata piena. */
@Composable
internal fun entrataBar(b: EntrataBlock?): () -> Float {
    if (b == null) return FULL
    val g = remember { Animatable(0f) }
    LaunchedEffect(g) {
        withFrameNanos { }
        delay(EntrataHome.barDelayMs(b.index).toLong())
        g.animateTo(1f, tween(EntrataHome.BAR_MS, easing = BAR_EASE))
    }
    return remember(g) { { g.value } }
}
