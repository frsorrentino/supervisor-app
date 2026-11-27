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
 * Chi decide quali risposte arrivano: solo quelle nate dopo l'apertura della chat, una volta sola. app-apertura-master-scatti-26-11
 * (Franz 26/11 23:27): non basta che la risposta manchi dalla prima lista, che all'apertura può arrivare a pezzi; deve anche essere
 * nata dopo l'apertura ([openedAt], secondi).
 */
internal class StreamGate(private val openedAt: Long = System.currentTimeMillis() / 1000) {
    private val known = mutableSetOf<String>()
    var primed = false
        private set
    fun prime(ids: Collection<String>) { if (!primed) { known.addAll(ids); primed = true } }
    fun claim(id: String, at: Long? = null): Boolean = primed && (at == null || at >= openedAt) && known.add(id)

    /** La risposta che sta arrivando adesso: una sola, e una nuova chiude subito quella prima. */
    var current by mutableStateOf<Arrival?>(null)
        private set
    fun publish(a: Arrival) { if (current !== a) { current?.finish(); current = a } }
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
    return onGloballyPositioned { c -> val y = c.positionInRoot().y; mine.floatValue = y; if (first) a.top = y }
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
