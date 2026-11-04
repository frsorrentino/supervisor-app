package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.dp

/**
 * Le pagine che prendevano lo schermo di colpo (Franz, 10/10 15:59: «possiamo introdurre transizioni su ogni passaggio di
 * schermate che avviene di colpo?»): Impostazioni, Terminale, Recap, Notte, Cerca, Coda e Registro entrano da destra con
 * una dissolvenza, come una sessione aperta senza la sua card (transizione C); tornando indietro la home rientra da sinistra.
 * L'entrata si decide al primo disegno: `animate` falso, o le animazioni spente, e il contenuto è subito al suo posto.
 */
@Composable
fun PageIn(fromStart: Boolean = false, animate: Boolean = true, content: @Composable () -> Unit) {
    val off = animationsOff()
    // Il morphing delle pagine (app-audit-grafica-animazioni-04-11, come morphStart sul web): aperta da un tocco, la pagina si scopre
    // da un riquadro attorno al punto premuto fino allo schermo intero. Senza un tocco recente entra di lato come sempre.
    val press = remember { if (!off && animate && !fromStart) LastPress.at else null }
    if (press != null) { MorphIn(press, content); return }
    val shown = remember { MutableTransitionState(off || !animate).apply { targetState = true } }
    AnimatedVisibility(
        shown,
        enter = slideInHorizontally(CmMotion.bounce) { w -> if (fromStart) -w / 10 else w / 3 } + fadeIn(tween(FLY_MS)),
        exit = ExitTransition.None,
    ) { content() }
}

/** La durata e la curva del web (`::view-transition-group(morph)` in v3.css): 380 ms, uscita morbida senza rimbalzo. */
private const val MORPH_MS = 380
private val MorphEasing = androidx.compose.animation.core.CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
private fun MorphIn(press: androidx.compose.ui.geometry.Offset, content: @Composable () -> Unit) {
    val p = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { p.animateTo(1f, tween(MORPH_MS, easing = MorphEasing)) }
    val at = remember { floatArrayOf(0f, 0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val startH = with(density) { 64.dp.toPx() }; val radius = with(density) { 14.dp.toPx() }
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier
            .onGloballyPositioned { c -> val o = c.positionOnScreen(); at[0] = o.x; at[1] = o.y }
            .graphicsLayer {
                val t = p.value
                if (t >= 1f) { clip = false; alpha = 1f; return@graphicsLayer }
                val w = size.width; val h = size.height
                // Il riquadro di partenza: un terzo della larghezza e 64 dp, centrato sul tocco e dentro la pagina.
                val sw = w / 3f; val sh = startH.coerceAtMost(h)
                val sx = (press.x - at[0] - sw / 2f).coerceIn(0f, (w - sw).coerceAtLeast(0f))
                val sy = (press.y - at[1] - sh / 2f).coerceIn(0f, (h - sh).coerceAtLeast(0f))
                val l = sx * (1f - t); val tp = sy * (1f - t)
                val r = l + sw + (w - sw) * t; val b = tp + sh + (h - sh) * t
                val cr = radius * (1f - t)
                shape = androidx.compose.foundation.shape.GenericShape { _, _ ->
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(l, tp, r, b, androidx.compose.ui.geometry.CornerRadius(cr)))
                }
                clip = true
                alpha = (t / 0.3f).coerceIn(0f, 1f)
            },
    ) { content() }
}
