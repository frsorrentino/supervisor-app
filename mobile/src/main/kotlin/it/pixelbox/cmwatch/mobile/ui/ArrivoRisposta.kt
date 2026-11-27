package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/*
 * app-messaggio-master-effetto-27-11, opzione E «Subito, due segnali» (Franz 27/11 15:48, mockup 04-subito.html; prima la B): la risposta
 * che arriva a chat aperta e ferma compare intera in 120 ms e sale con la lista, una corsa sola con la molla del pannello (CmMotion.panelFloat,
 * 400 ms) a pixel interi, senza scoperte. Poi due segnali: un lampo sull'esito, col fondo del piede che si accende e torna in 900 ms e la
 * spunta che entra da 0,8 con la molla di casa (CmMotion.bounceFloat, 620 ms); e nel riquadro della master l'ora che scorre (RollNumber,
 * MasterDock.kt). Un dito sulla lista mostra subito tutto; una risposta nuova chiude subito quella in corsa; più in alto niente corsa e
 * resta «1 nuovo ↓». Animazioni ridotte: la chat si spegne in 70 ms, va in fondo e si riaccende in 150 ms; niente lampo.
 */

/**
 * Chi decide che cosa arriva con un'animazione: una volta sola, solo quello che nasce a chat aperta e ferma. app-apertura-master-scatti-26-11
 * (Franz 26/11 23:27): all'apertura la conversazione può arrivare a pezzi, quindi non basta che la risposta manchi dalla prima lista.
 * Franz 27/11 15:17 («l'animazione del testo quando un compito è terminato e si apre la sessione… solo quando si ferma nella posizione
 * finale», «mantiene uno spazio che poi risulta essere quello dell'esito»): aprendo la sessione di un compito appena finito, la risposta già
 * scritta passava per nuova, perché la sua ora (orologio del PC) si confrontava con l'apertura sull'orologio del telefono; partiva la corsa
 * e, finché non la scopriva, la risposta restava nascosta col suo spazio vuoto. Ora una risposta arriva solo se: la chat è ferma al suo
 * posto ([settle]: ancorata in fondo e finita l'apertura, [OPENING_MS]); da lì la sessione è stata vista lavorare ([working], con l'inizio
 * del turno sull'orologio del PC); la risposta è nata da quel turno in poi (il suo `at`, sullo stesso orologio). Quello che compare prima, o
 * senza `at`, è di prima: si vede intero e fermo. Lo stesso vale per le righe che entrano ([live]).
 */
internal class StreamGate {
    private val known = mutableSetOf<String>()
    var primed = false
        private set
    /** La chat è ferma al suo posto: ancorata in fondo e finita l'apertura. */
    var settled = false
        private set
    /** L'inizio del primo turno visto aperto a chat ferma (turn_started, secondi del PC); null finché la sessione non lavora. */
    var turnFrom: Long? = null
        private set
    /** Da qui può arrivare qualcosa della sessione con un'animazione: la chat è ferma e la sessione lavora. */
    val live: Boolean get() = settled && turnFrom != null
    fun prime(ids: Collection<String>) { if (!primed) { known.addAll(ids); primed = true } }
    fun settle() { settled = true }
    /** La sessione lavora, col turno cominciato a [started]: conta solo a chat ferma. */
    fun working(started: Long?) { if (settled && turnFrom == null && started != null) turnFrom = started }
    fun claim(id: String, at: Long? = null): Boolean {
        if (!primed || !known.add(id)) return false
        val from = turnFrom ?: return false
        return settled && at != null && at >= from
    }

    /** La risposta che sta arrivando adesso: una sola, e una nuova chiude subito quella prima. */
    var current by mutableStateOf<Arrival?>(null)
        private set
    fun publish(a: Arrival) { if (current !== a) { current?.finish(); current = a } }

    companion object {
        /** L'apertura: la scheda vola al suo posto e la chat si ancora; fin qui niente arriva con un'animazione. */
        const val OPENING_MS = 1_000L
    }
}

internal val LocalStreamGate = compositionLocalOf<StreamGate?> { null }

/** Una risposta in arrivo. */
internal class Arrival(val id: String) {
    /** L'opacità di risposta e riga dei dati: 0 finché la corsa non parte, poi a 1 in 120 ms. */
    var alpha by mutableFloatStateOf(0f)
    /** Il lampo sull'esito: 1 acceso, 0 il fondo di sempre. */
    var flash by mutableFloatStateOf(0f)
    /** La scala della spunta dell'esito: entra da 0,8. */
    var check by mutableFloatStateOf(CHECK_FROM)
    /** Finché la lista corre, ChatFollow aspetta e un dito chiude tutto. */
    var running by mutableStateOf(true)
    var job: Job? = null
    /** Tutto intero e fermo: a fine corsa, sotto un dito, all'arrivo di un'altra risposta. */
    fun finish() { job?.cancel(); alpha = 1f; flash = 0f; check = 1f; running = false }

    companion object {
        const val CHECK_FROM = 0.8f
        /** I tempi della E: dissolvenza, attesa prima dei segnali, lampo. */
        const val FADE_MS = 120
        const val SIGNAL_AFTER_MS = 150L
        const val FLASH_MS = 900
        /** La curva del mockup per dissolvenze e lampo, cubic-bezier(.3, 0, .2, 1). */
        val Ease = androidx.compose.animation.core.CubicBezierEasing(.3f, 0f, .2f, 1f)
    }
}

/** Una parte della risposta che arriva (la scheda, [first], o la riga dei dati): la sua opacità, senza toccare il layout. */
@Composable
internal fun Modifier.arrivalPart(a: Arrival?, first: Boolean): Modifier {
    if (a == null) return this
    // Per i test: la risposta che sta arrivando si riconosce finché la corsa non è finita.
    return then(if (first && a.running) Modifier.testTag("risposta-in-arrivo") else Modifier).graphicsLayer { alpha = a.alpha }
}

/**
 * La corsa della risposta [a]. In fondo alla chat ([follow]) e a lista ferma la risposta compare in 120 ms e la lista sale una volta sola
 * fino alla fine, con la molla del pannello e a pixel interi; 150 ms dopo il lampo sull'esito e la spunta. Più in alto, o con la lista che
 * si muove, niente: la risposta c'è intera al suo posto. Animazioni ridotte ([reduced]): la chat ([chat], la sua opacità) si spegne, va in
 * fondo e si riaccende; niente lampo.
 */
internal suspend fun LazyListState.runArrival(a: Arrival, follow: Boolean, reduced: Boolean, chat: Animatable<Float, AnimationVector1D>) {
    try {
        // La corsa parte solo a lista ferma (Franz 27/11 15:17): se la lista si muove, la risposta compare intera al suo posto.
        if (!follow || isScrollInProgress) return
        // La risposta è già misurata e piazzata (la corsa parte dopo il suo primo layout), ancora trasparente.
        val info = layoutInfo
        val last = info.visibleItemsInfo.lastOrNull()?.takeIf { it.index == info.totalItemsCount - 1 }
        val over = if (last == null) 0 else last.offset + last.size - (info.viewportEndOffset - info.afterContentPadding)
        if (reduced) {
            if (over > 0) {
                chat.animateTo(0f, tween(70))
                a.alpha = 1f
                scrollToEnd()
                chat.animateTo(1f, tween(150, easing = Arrival.Ease))
            } else animate(0f, 1f, animationSpec = tween(Arrival.FADE_MS, easing = Arrival.Ease)) { v, _ -> a.alpha = v }
            return
        }
        coroutineScope {
            launch { animate(0f, 1f, animationSpec = tween(Arrival.FADE_MS, easing = Arrival.Ease)) { v, _ -> a.alpha = v } }
            launch {
                delay(Arrival.SIGNAL_AFTER_MS)
                launch { animate(1f, 0f, animationSpec = tween(Arrival.FLASH_MS, easing = Arrival.Ease)) { v, _ -> a.flash = v } }
                animate(Arrival.CHECK_FROM, 1f, animationSpec = CmMotion.bounceFloat) { v, _ -> a.check = v }
            }
            if (over > 0) scroll {
                var done = 0
                animate(0f, over.toFloat(), animationSpec = CmMotion.panelFloat) { v, _ ->
                    val to = v.roundToInt().coerceIn(0, over)
                    if (to != done) { scrollBy((to - done).toFloat()); done = to }
                }
            }
            // Finita la corsa la lista torna a seguire il fondo (ChatFollow); lampo e spunta finiscono da soli.
            a.running = false
        }
    } finally {
        a.finish()
        if (chat.value < 1f) withContext(NonCancellable) { chat.snapTo(1f) }
    }
}
