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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/*
 * app-messaggio-master-effetto-27-11 (Franz 27/11 13:28, opzione B «L'esito per primo»; strada b della master): la risposta che arriva a
 * chat aperta ha la sua misura dal primo fotogramma, quindi la lista non salta. In fondo alla chat la lista sale una volta sola con la
 * molla del pannello (CmMotion.panelFloat, 400 ms) e la stessa corsa, nello stesso fotogramma, scopre la risposta: risposta e riga dei
 * dati si disegnano spostate in alto di k con quello che sta sopra la linea k ritagliato, dove k è quanto manca alla fine. Così il testo
 * nuovo sta fermo al pixel, l'esito compare per primo dove c'era «sta pensando» e la risposta si apre verso l'alto; si sposta solo quello
 * già letto. Un dito a metà corsa scopre tutto e lascia la lista al dito; una risposta nuova chiude subito quella in corsa; più in alto non
 * si muove niente e resta «1 nuovo ↓». Animazioni ridotte: la chat si spegne in 70 ms, va in fondo, si riaccende in 150 ms, e corpo e
 * riga dei dati entrano 80 ms dopo l'esito, in 120 ms. Prende il posto del parola per parola (WordStream).
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
    /** Quanto manca alla fine della corsa, in px: lo spostamento in su e la parte ritagliata. Infinito finché la corsa non parte: nascosta. */
    var k by mutableFloatStateOf(Float.POSITIVE_INFINITY)
    /** Finché arriva, ChatFollow aspetta. */
    var running by mutableStateOf(true)
    /** Animazioni ridotte: corpo e riga dei dati entrano dopo l'esito. */
    var body by mutableFloatStateOf(1f)
    /** Dove comincia la risposta, in coordinate della radice: la linea del ritaglio si conta da qui. */
    var top by mutableFloatStateOf(Float.NaN)
    var job: Job? = null
    /** Tutto scoperto e fermo: a fine corsa, sotto un dito, all'arrivo di un'altra risposta. */
    fun finish() { job?.cancel(); k = 0f; body = 1f; running = false }
}

/**
 * Una parte della risposta che arriva (la scheda della risposta, [first], o la riga dei dati): disegnata spostata in su di k, con nascosto
 * quello che sta sopra la linea che dista k dall'alto della risposta. Le posizioni si leggono prima dello spostamento, così la linea resta
 * quella del layout; misure e disegno cambiano solo nel disegno, mai nel layout.
 */
@Composable
internal fun Modifier.arrivalPart(a: Arrival?, first: Boolean): Modifier {
    if (a == null) return this
    val mine = remember(a) { mutableFloatStateOf(Float.NaN) }
    // Per i test: la risposta che sta arrivando si riconosce finché la corsa non è finita.
    return then(if (first && a.running) Modifier.testTag("risposta-in-arrivo") else Modifier)
        .onGloballyPositioned { c -> val y = c.positionInRoot().y; mine.floatValue = y; if (first) a.top = y }
        .graphicsLayer { val k = a.k; translationY = if (k.isInfinite()) 0f else -k }
        .drawWithContent {
            val k = a.k
            if (k <= 0f) { drawContent(); return@drawWithContent }
            val top = a.top
            val me = mine.floatValue
            if (k.isInfinite() || top.isNaN() || me.isNaN()) return@drawWithContent
            val cut = top + k - me
            if (cut >= size.height) return@drawWithContent
            if (cut <= 0f) drawContent() else clipRect(top = cut) { this@drawWithContent.drawContent() }
        }
}

/**
 * La corsa della risposta [a]. In fondo alla chat ([follow]) la lista sale una volta sola fino alla fine, con la molla del pannello e a
 * pixel interi, e k scende con lei nello stesso fotogramma. Più in alto niente corsa: la risposta compare al suo posto. Animazioni
 * ridotte ([reduced]): la chat ([chat], la sua opacità) si spegne, va in fondo, si riaccende; corpo e riga dei dati dopo.
 */
internal suspend fun LazyListState.runArrival(a: Arrival, follow: Boolean, reduced: Boolean, chat: Animatable<Float, AnimationVector1D>) {
    try {
        // La B parte solo a lista ferma (Franz 27/11 15:17): se la lista si muove, niente corsa e la risposta compare intera al suo posto.
        // Aspettare vorrebbe dire tenerla nascosta, cioè uno spazio vuoto.
        if (isScrollInProgress) return
        // La risposta è già misurata e piazzata (la corsa parte dopo il suo primo layout), ancora nascosta.
        val info = layoutInfo
        val last = info.visibleItemsInfo.lastOrNull()?.takeIf { it.index == info.totalItemsCount - 1 }
        val over = if (last == null) 0 else last.offset + last.size - (info.viewportEndOffset - info.afterContentPadding)
        if (!follow || over <= 0) return
        if (reduced) {
            chat.animateTo(0f, tween(70))
            scrollToEnd()
            a.body = 0f
            a.k = 0f
            coroutineScope {
                launch { delay(80); animate(0f, 1f, animationSpec = tween(120)) { v, _ -> a.body = v } }
                chat.animateTo(1f, tween(150))
            }
            return
        }
        a.k = over.toFloat()
        scroll {
            var done = 0
            animate(0f, over.toFloat(), animationSpec = CmMotion.panelFloat) { v, _ ->
                val to = v.roundToInt().coerceIn(0, over)
                if (to != done) { scrollBy((to - done).toFloat()); done = to }
                a.k = (over - done).toFloat()
            }
        }
    } finally {
        a.finish()
        if (chat.value < 1f) withContext(NonCancellable) { chat.snapTo(1f) }
    }
}
