package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateDp
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Voce 2, fedele all'originale (Morph island; app-animazioni-fedeli-26-11), la barra della master che si allunga: l'altezza va con la
 * molla 300 22 quando cresce e 390 22 (1,3 volte più rigida) quando cala; mentre l'altezza corre la larghezza cede fino al 5 %
 * (`-v/7000`, il volume si conserva); a ogni cambio la barra si abbassa e torna con la molla 520 18 (spinta -1,6 se cresce, -1 se
 * cala). La barra è larga quanto lo schermo, quindi la larghezza dell'originale qui è fissa. Moto ridotto: altezza criticamente
 * smorzata, come nell'originale; la spinta resta.
 */
@Composable
internal fun Modifier.islandMorph(off: Boolean): Modifier {
    val h = remember { Animatable(-1f) }
    val press = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var target by remember { mutableIntStateOf(-1) }
    LaunchedEffect(target) {
        if (target < 0) return@LaunchedEffect
        if (h.value < 0f) { h.snapTo(target.toFloat()); return@LaunchedEffect }
        val grow = target > h.targetValue
        val k = 300f * (if (grow) 1f else 1.3f)
        val c = if (off) 2f * sqrt(k) else 22f
        scope.launch { press.animateTo(1f, spring(stiffness = 520f, dampingRatio = 18f / (2f * sqrt(520f))), initialVelocity = press.velocity + if (grow) -1.6f else -1f) }
        // Nello scope della composizione: un cambio a metà corsa riparte dalla velocità che ha, come la molla dell'originale.
        scope.launch { h.animateTo(target.toFloat(), spring(stiffness = k, dampingRatio = c / (2f * sqrt(k)))) }
    }
    return graphicsLayer {
        val give = (-h.velocity / 7000f).coerceIn(-0.05f, 0.05f)
        transformOrigin = TransformOrigin(0.5f, 1f)
        scaleX = press.value * (1f + give); scaleY = press.value
    }.clipToBounds().layout { m, c ->
        val p = m.measure(c)
        if (p.height != target) target = p.height
        val ht = if (h.value < 0f) p.height else h.value.roundToInt().coerceAtLeast(20)
        layout(p.width, ht) { p.place(0, 0) }
    }
}

/** Il cambio di contenuto dell'isola: chi esce svanisce in 140 ms e scala a 0,9 in 200 ms; chi entra dopo 90 ms, 350 ms e la scala in 600 ms. */
internal fun islandSwap(off: Boolean): ContentTransform =
    if (off) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
    else (fadeIn(tween(350, 90, FastOutSlowInEasing)) + scaleIn(tween(600, 90, CubicBezierEasing(0.16f, 1f, 0.3f, 1f)), initialScale = 0.9f)) togetherWith
        (fadeOut(tween(140, easing = FastOutSlowInEasing)) + scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.9f))

/** La sfocatura del cambio di contenuto: chi entra da 6 dp a nitido in 350 ms dopo 90 ms, chi esce a 6 dp in 140 ms (Android 12 e oltre). */
@Composable
internal fun Modifier.islandBlur(scope: androidx.compose.animation.AnimatedVisibilityScope): Modifier {
    val b by scope.transition.animateDp(
        transitionSpec = { if (targetState == androidx.compose.animation.EnterExitState.Visible) tween(350, 90) else tween(140) }, label = "sfoca",
    ) { s -> if (s == androidx.compose.animation.EnterExitState.Visible) 0.dp else 6.dp }
    return if (b > 0.dp) blur(b, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else this
}
