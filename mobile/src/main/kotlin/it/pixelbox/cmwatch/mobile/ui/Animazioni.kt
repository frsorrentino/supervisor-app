package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Animazioni
import kotlinx.coroutines.launch

/**
 * Voce 1 di app-animazioni-otto-26-11 (Copia che risponde, Franz 26/11 11:50): al tocco la copia diventa una spunta verde, «Copiato»
 * per il tasto a parole, per [Animazioni.COPY_HOLD_MS]; poi torna. Lo scambio entra da 0,9 con la molla veloce e sfuma in 180 ms;
 * con le animazioni spente è secco. Ogni tocco riparte da capo.
 */
@Composable
private fun rememberCopied(): Pair<Boolean, () -> Unit> {
    var taps by remember { mutableIntStateOf(0) }
    var on by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        on = true
        kotlinx.coroutines.delay(Animazioni.COPY_HOLD_MS)
        on = false
    }
    return on to { taps++ }
}

@Composable
private fun <T> CopySwap(state: T, content: @Composable (T) -> Unit) {
    val off = animationsOff()
    AnimatedContent(
        state,
        transitionSpec = {
            if (off) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else (fadeIn(tween(180, easing = CmMotion.easing)) + scaleIn(CmMotion.fast, initialScale = 0.9f)) togetherWith fadeOut(tween(120))
        },
        label = "copia",
    ) { content(it) }
}

/** Il tasto Copia a icona, sotto una risposta o un messaggio. */
@Composable
internal fun CopyIconAction(text: String) {
    val clip = LocalClipboardManager.current
    val (on, tap) = rememberCopied()
    val label = stringResource(if (on) R.string.copied_word else R.string.copy)
    IconButton(onClick = { clip.setText(AnnotatedString(text)); tap() }, modifier = Modifier.size(36.dp).testTag("copia")) {
        CopySwap(on) { done ->
            Icon(if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy, label, tint = if (done) V3.Good else Cm.text2, modifier = Modifier.size(18.dp))
        }
    }
}

/** Il tasto Copia a parole, dentro un testo da mandare o un comando. */
@Composable
internal fun CopyPill(copy: () -> String) {
    val clip = LocalClipboardManager.current
    val (on, tap) = rememberCopied()
    val style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    Row(
        Modifier.clip(CircleShape).border(1.dp, if (on) V3.Good else V3.Line, CircleShape).handCursor()
            .clickable { clip.setText(AnnotatedString(copy())); tap() }.padding(horizontal = 12.dp, vertical = 4.dp).testTag("copia"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CopySwap(on) { done ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done) Icon(Icons.Rounded.Check, null, tint = V3.Good, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                Text(stringResource(if (done) R.string.copied_word else R.string.copy), style = style, color = if (done) V3.Good else V3.Tx)
            }
        }
    }
}

/**
 * Voce 3 (Al lavoro, vivo): un riflesso attraversa il testo di chi lavora con lo stesso ciclo della linea del lavoro in corso,
 * [Animazioni.CYCLE_MS] lineare. Con le animazioni spente (e nelle immagini di prova) il colore pieno, fermo.
 */
@Composable
internal fun shineStyle(base: TextStyle, color: androidx.compose.ui.graphics.Color): TextStyle {
    if (animationsOff()) return base.copy(color = color)
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "al-lavoro")
    val x by t.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(tween(Animazioni.CYCLE_MS, easing = androidx.compose.animation.core.LinearEasing)),
        label = "riflesso",
    )
    val light = androidx.compose.ui.graphics.lerp(color, androidx.compose.ui.graphics.Color.White, 0.6f)
    val w = 48f
    val from = -2 * w + x * 6 * w
    return base.copy(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(color, light, color), start = androidx.compose.ui.geometry.Offset(from, 0f), end = androidx.compose.ui.geometry.Offset(from + w, 0f),
        ),
    )
}

/**
 * Voce 4 (Cifra che scorre): l'ora dell'ultima risposta data da qui. La cifra del tabellone scorre solo se cambia entro [WINDOW_MS]
 * da un gesto; per un aggiornamento dal PC cambia e basta.
 */
object Gesture {
    const val WINDOW_MS = 4000L
    @Volatile private var at = 0L
    fun mark() { at = android.os.SystemClock.uptimeMillis() }
    fun recent(): Boolean = at > 0 && android.os.SystemClock.uptimeMillis() - at < WINDOW_MS
}

/** La cifra grande del tabellone: con [roll] la nuova arriva da sotto in 220 ms con la molla veloce, solo dopo un gesto. */
@Composable
internal fun RollNumber(value: String, roll: Boolean, content: @Composable (String) -> Unit) {
    if (!roll) { content(value); return }
    val off = animationsOff()
    AnimatedContent(
        value,
        transitionSpec = {
            if (!off && Animazioni.rolls(initialState.toIntOrNull(), targetState.toIntOrNull() ?: 0, Gesture.recent()))
                (androidx.compose.animation.slideInVertically(tween(220, easing = CmMotion.easing)) { it } + fadeIn(tween(160))) togetherWith
                    (androidx.compose.animation.slideOutVertically(tween(220, easing = CmMotion.easing)) { -it } + fadeOut(tween(160)))
            else androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
        },
        label = "cifra",
    ) { content(it) }
}

/**
 * Voce 6 (Tieni premuto per la produzione): il tasto si riempie mentre il dito resta giù e l'ok parte solo quando è pieno, dopo
 * [Animazioni.HOLD_MS]; lasciato prima torna vuoto in 200 ms e non succede niente. Lineare perché è un tempo da leggere. Con le
 * animazioni spente il riempimento non si vede, ma il tempo da tenere resta.
 */
@Composable
internal fun HoldButton(label: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val fill = remember { androidx.compose.animation.core.Animatable(0f) }
    val off = animationsOff()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val prod = Cm.prod
    androidx.compose.foundation.layout.Box(
        modifier.clip(CircleShape).background(V3.S3).border(1.dp, prod.copy(alpha = 0.6f), CircleShape)
            .drawBehind { if (!off && fill.value > 0f) drawRect(prod.copy(alpha = 0.35f), size = size.copy(width = size.width * fill.value)) }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val job = scope.launch {
                        if (off) kotlinx.coroutines.delay(Animazioni.HOLD_MS)
                        else fill.animateTo(1f, tween(Animazioni.HOLD_MS.toInt(), easing = androidx.compose.animation.core.LinearEasing))
                        fill.snapTo(0f)
                        onDone()
                    }
                    val released = tryAwaitRelease()
                    if (job.isActive) { job.cancel(); scope.launch { fill.animateTo(0f, tween(200, easing = CmMotion.easing)) } }
                    else if (!released) scope.launch { fill.snapTo(0f) }
                })
            }
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("tieni-premuto"),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = V3.Tx) }
}
