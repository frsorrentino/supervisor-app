package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.PhonePrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Voce 4 del terzo giro, fedele all'originale (Prompt box e Mention picker; app-animazioni-terzo-c-26-11): il tasto d'invio è uno solo.
 * Fondo e colore cambiano in 200 ms (ease) fra pronto, tonale, Stop e spento; la freccia diventa il quadrato dello Stop sulla molla
 * 520 30 (la freccia scala a 0 ruotando di 90°, il quadrato scala a 1 e può sforare fino a 1,2). Al tocco d'invio il tasto si stringe a
 * 0,94 in 260 ms e la freccia vola: sale di 26 dp svanendo e sfocandosi (2 dp), poi rientra da 22 dp sotto, in 560 ms con
 * (0.3, 0.7, 0.2, 1). Animazioni spente: cambia e basta.
 */
@Composable
internal fun PromptSend(mode: PhonePrimary.Composer, filled: IconButtonColors, modifier: Modifier, onSend: () -> Unit, onStop: () -> Unit) {
    val off = animationsOff()
    val tonal = IconButtonDefaults.filledTonalIconButtonColors()
    val (bg, fg) = when (mode) {
        PhonePrimary.Composer.SEND, PhonePrimary.Composer.STOP -> filled.containerColor to filled.contentColor
        PhonePrimary.Composer.SEND_TONAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> tonal.disabledContainerColor to tonal.disabledContentColor
    }
    val spec = if (off) tween<androidx.compose.ui.graphics.Color>(0) else tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    val bgA by animateColorAsState(bg, spec, label = "fondo")
    val fgA by animateColorAsState(fg, spec, label = "segno")
    val stop = mode == PhonePrimary.Composer.STOP
    val m = remember { Animatable(if (stop) 1f else 0f) }
    LaunchedEffect(stop, off) { if (off) m.snapTo(if (stop) 1f else 0f) else m.animateTo(if (stop) 1f else 0f, Molla.of(520f, 30f)) }
    val fly = remember { Animatable(1f) }
    val press = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val flyEase = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)
    val click = {
        if (stop) onStop()
        else {
            if (!off) {
                scope.launch { fly.snapTo(0f); fly.animateTo(1f, tween(560, easing = LinearEasing)) }
                scope.launch { press.snapTo(0f); press.animateTo(1f, tween(260, easing = LinearEasing)) }
            }
            onSend()
        }
    }
    val colors = IconButtonColors(bgA, fgA, bgA, fgA)
    FilledIconButton(
        onClick = click, enabled = mode != PhonePrimary.Composer.NONE, colors = colors,
        modifier = modifier.graphicsLayer { val q = press.value; val k = if (q < 0.5f) 1f - 0.12f * q else 0.94f + 0.12f * (q - 0.5f); scaleX = k; scaleY = k },
    ) {
        val p = flyEase.transform(fly.value)
        val y = if (p < 0.5f) -26f * (p / 0.5f) else 22f * (1f - (p - 0.5f) / 0.5f)
        val a = if (p < 0.5f) 1f - p / 0.5f else (p - 0.5f) / 0.5f
        val b = if (fly.value >= 1f) 0f else 2f * (1f - a)
        Box(contentAlignment = Alignment.Center) {
            val mv = m.value.coerceIn(0f, 1.2f)
            Icon(
                Icons.AutoMirrored.Rounded.Send, if (stop) null else stringResource(R.string.send),
                modifier = Modifier.graphicsLayer {
                    val k = (1f - mv).coerceAtLeast(0f); scaleX = k; scaleY = k; rotationZ = mv * 90f
                    translationY = y.dp.toPx(); alpha = a
                }.then(if (b > 0.05f) Modifier.blur(b.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier),
            )
            Icon(
                Icons.Rounded.Stop, if (stop) stringResource(R.string.stop) else null,
                modifier = Modifier.graphicsLayer { val k = mv.coerceAtLeast(0f); scaleX = k; scaleY = k },
            )
        }
    }
}

/**
 * Voce 4 del terzo giro (Prompt box): il campo cresce con il testo, fino a cinque righe, sulla molla 420 34 invece di scattare.
 * Animazioni spente: segue subito.
 */
@Composable
internal fun Modifier.promptHeight(): Modifier {
    val off = animationsOff()
    val h = remember { Animatable(-1f) }
    val scope = rememberCoroutineScope()
    return clipToBounds().layout { m, c ->
        val p = m.measure(c)
        val target = p.height.toFloat()
        if (h.value < 0f || off) { if (h.targetValue != target) scope.launch { h.snapTo(target) } }
        else if (h.targetValue != target) scope.launch { h.animateTo(target, Molla.of(420f, 34f)) }
        val ht = if (h.value < 0f) p.height else h.value.roundToInt().coerceAtLeast(0)
        layout(p.width, ht) { p.place(0, 0) }
    }
}
