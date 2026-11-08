package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
    // Tornati col gesto Indietro predittivo la pagina è già rientrata nella voce: la home compare sul posto, senza scivolare.
    val viaGesture = remember { fromStart && PageBack.takeCommitted() }
    if (!fromStart) { BackLayer(press) { if (press != null) MorphIn(press, content) else SlideIn(false, off || !animate, content) }; return }
    if (viaGesture && !off) {
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(shown, enter = fadeIn(tween(150)), exit = ExitTransition.None) { content() }
        return
    }
    SlideIn(true, off || !animate, content)
}

@Composable
private fun SlideIn(fromStart: Boolean, still: Boolean, content: @Composable () -> Unit) {
    val shown = remember { MutableTransitionState(still).apply { targetState = true } }
    AnimatedVisibility(
        shown,
        enter = slideInHorizontally(CmMotion.bounce) { w -> if (fromStart) -w / 10 else w / 3 } + fadeIn(tween(FLY_MS)),
        exit = ExitTransition.None,
    ) { content() }
}

/**
 * Effetto B (app-audit-grafica-animazioni-04-11, scelto da Franz il 04/11): il gesto Indietro predittivo di Android 14 e
 * successivi. `PageBackHandler` scrive qui l'avanzamento del gesto; la pagina in cima lo legge in `BackLayer`.
 */
object PageBack {
    internal val progress = androidx.compose.runtime.mutableFloatStateOf(0f)
    internal val leaving = androidx.compose.runtime.mutableFloatStateOf(0f)
    internal var fromLeft = true
    private var committed = false
    internal fun commit() { committed = true }
    internal fun takeCommitted(): Boolean = committed.also { committed = false }
    internal fun reset() { progress.floatValue = 0f; leaving.floatValue = 0f; committed = false }

    /**
     * Dopo l'Indietro: `frames` e poi tutto a zero, anche se la coroutine viene cancellata prima. La coroutine del gesto vive nella
     * composizione della pagina: la pagina che esce la cancella al primo fotogramma, e senza lo zero `leaving` restava a 1 e la
     * pagina aperta dopo entrava a trasparenza zero, nera fino al gesto Indietro (app-ti-aspetta-nera-08-11).
     */
    internal suspend fun settle(frames: suspend () -> Unit) {
        try { frames() } finally { reset() }
    }
}

/**
 * Indietro per le pagine: col gesto la pagina rimpicciolisce sotto il dito, verso il bordo opposto, e si arrotonda; lasciata,
 * si chiude nel riquadro del tocco che l'aveva aperta (il morphing al contrario) e poi `onBack`. Annullato, torna intera.
 * Il tasto Indietro a tre pulsanti non manda avanzamento: chiude e basta, come prima. Con le animazioni spente, solo `onBack`.
 */
@Composable
fun PageBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val off = animationsOff()
    val back = androidx.compose.runtime.rememberUpdatedState(onBack)
    androidx.activity.compose.PredictiveBackHandler(enabled) { events ->
        try {
            events.collect { e -> if (!off) { PageBack.fromLeft = e.swipeEdge == androidx.activity.BackEventCompat.EDGE_LEFT; PageBack.progress.floatValue = e.progress } }
        } catch (c: kotlin.coroutines.cancellation.CancellationException) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                androidx.compose.animation.core.animate(PageBack.progress.floatValue, 0f, animationSpec = tween(200, easing = MorphEasing)) { v, _ -> PageBack.progress.floatValue = v }
            }
            throw c
        }
        if (!off && PageBack.progress.floatValue > 0f) {
            androidx.compose.animation.core.animate(0f, 1f, animationSpec = tween(LEAVE_MS, easing = MorphEasing)) { v, _ -> PageBack.leaving.floatValue = v }
            PageBack.commit()
        }
        back.value()
        // Due fotogrammi: la pagina è uscita e la home ha letto `takeCommitted`; se la pagina resta (un Indietro interno), torna intera.
        PageBack.settle { repeat(2) { androidx.compose.runtime.withFrameNanos { } } }
    }
}

private const val LEAVE_MS = 240

@Composable
private fun BackLayer(press: androidx.compose.ui.geometry.Offset?, content: @Composable () -> Unit) {
    val at = remember { floatArrayOf(0f, 0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val shift = with(density) { 24.dp.toPx() }; val round = with(density) { 28.dp.toPx() }
    val startH = with(density) { 64.dp.toPx() }; val radius = with(density) { 14.dp.toPx() }
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier
            .onGloballyPositioned { c -> val o = c.positionOnScreen(); at[0] = o.x; at[1] = o.y }
            .graphicsLayer {
                val p = PageBack.progress.floatValue; val l = PageBack.leaving.floatValue
                if (p <= 0f && l <= 0f) { clip = false; scaleX = 1f; scaleY = 1f; translationX = 0f; alpha = 1f; return@graphicsLayer }
                val w = size.width; val h = size.height
                // Come nella prova (effetti.html, B): 10% più piccola, 24 dp verso il bordo opposto, angoli fino a 28 dp.
                val sc = 1f - 0.1f * p
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (PageBack.fromLeft) 1f else 0f, 0.5f)
                scaleX = sc; scaleY = sc
                translationX = (if (PageBack.fromLeft) 1f else -1f) * shift * p
                val cr = round * p
                if (press == null || l <= 0f) {
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(cr)
                    alpha = 1f - l
                } else {
                    // Lasciata: la pagina si stringe nel riquadro del tocco che l'aveva aperta (MorphIn al contrario) e sparisce.
                    val sw = w / 3f; val sh = startH.coerceAtMost(h)
                    val sx = (press.x - at[0] - sw / 2f).coerceIn(0f, (w - sw).coerceAtLeast(0f))
                    val sy = (press.y - at[1] - sh / 2f).coerceIn(0f, (h - sh).coerceAtLeast(0f))
                    val left = sx * l; val top = sy * l
                    val right = w + (sx + sw - w) * l; val bottom = h + (sy + sh - h) * l
                    val r = cr + (radius - cr) * l
                    shape = androidx.compose.foundation.shape.GenericShape { _, _ ->
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(left, top, right, bottom, androidx.compose.ui.geometry.CornerRadius(r)))
                    }
                    alpha = 1f - (l - 0.6f).coerceAtLeast(0f) / 0.4f
                }
                clip = true
            },
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

/**
 * Effetto D (app-audit-grafica-animazioni-04-11, scelto da Franz il 04/11): la testata della home si compatta scorrendo, legata
 * al dito. `SummaryList` scrive qui quanto è scesa la lista (in px, tetto a 1000); la testata lo legge nel
 * disegno, senza ricomporre. Con le animazioni spente, e negli snapshot, resta tutto fermo.
 */
object HomeScroll {
    internal val px = androidx.compose.runtime.mutableFloatStateOf(0f)
    /** Se sotto c'è ancora altro da scorrere (app-home-sfuma-sopra-master-04-11): la home sopra la master sfuma solo allora. */
    internal val more = androidx.compose.runtime.mutableStateOf(false)
}

/** Il titolo della home: da 22 a 19 sp nei primi 90 dp di scorrimento, appoggiato in basso a sinistra (come la prova D). */
@Composable
fun Modifier.compactTitle(): Modifier {
    if (animationsOff()) return this
    val range = with(androidx.compose.ui.platform.LocalDensity.current) { 90.dp.toPx() }
    return this.graphicsLayer {
        val f = (HomeScroll.px.floatValue / range).coerceIn(0f, 1f)
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
        val s = 1f - (3f / 22f) * f; scaleX = s; scaleY = s
    }
}

/** Il filo sotto la testata, che compare fra 40 e 90 dp di scorrimento: la testata si stacca dalla lista che le passa sotto. */
@Composable
fun Modifier.compactRule(color: androidx.compose.ui.graphics.Color): Modifier {
    if (animationsOff()) return this
    val d = androidx.compose.ui.platform.LocalDensity.current
    val from = with(d) { 40.dp.toPx() }; val to = with(d) { 90.dp.toPx() }; val w = with(d) { 1.dp.toPx() }
    return this.drawWithContent {
        drawContent()
        val a = ((HomeScroll.px.floatValue - from) / (to - from)).coerceIn(0f, 1f)
        if (a > 0f) drawRect(color.copy(alpha = color.alpha * a), androidx.compose.ui.geometry.Offset(0f, size.height - w), androidx.compose.ui.geometry.Size(size.width, w))
    }
}

