package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.TranscriptEntry
import it.pixelbox.cmwatch.contract.TranscriptTurn
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.SessionSheet
import it.pixelbox.cmwatch.mobile.ui.SheetActions
import it.pixelbox.cmwatch.rules.ChatFeed
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * app-messaggio-master-effetto-27-11 (opzione B «L'esito per primo», strada b): la risposta che arriva con la chat in fondo. L'esito compare
 * dove c'era «sta pensando» e da lì non si muove di un pixel mentre la lista sale; la lista sale una volta sola, senza salti né ritorni.
 * Franz 27/11 15:17: aprendo la sessione di un compito già finito niente arriva con un'animazione, nemmeno se la conversazione arriva a
 * pezzi o lo stato è ancora quello di prima per qualche fotogramma: esito e testo si vedono interi dal primo fotogramma, fermi.
 * Paparazzi fa da motore dei fotogrammi, come in ChatReadingHoldTest; le posizioni sono quelle disegnate (con lo spostamento del layer).
 */
class ArrivoRispostaFrameTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val t0 = st.ts - 3_600
    private val busy = st.sessions.first { it.state == SessionState.BUSY }.copy(question = null, turnStarted = t0 + 2_400)
    private val idle = busy.copy(state = SessionState.IDLE, turnStarted = null)

    private fun past(): List<TranscriptEntry> = (1..4).flatMap { k ->
        val t = t0 + k * 300L
        listOf(
            TranscriptEntry("u$k", "user", text = "Domanda $k: controlla la release e dimmi cosa manca", at = t, origin = "phone"),
            TranscriptEntry("a$k", "assistant", text = "Risposta $k. " + "Ho guardato la release, i test e le note: la suite è verde su Linux e su Windows. ".repeat(3), at = t + 60, turn = TranscriptTurn(t, t + 60, 4000, 120)),
        )
    }

    // Il turno al lavoro: la richiesta e quattro passi, senza risposta.
    private fun work(): List<TranscriptEntry> {
        val t = t0 + 2_400
        return listOf(TranscriptEntry("w0", "user", text = "Verifica la release e avviala", at = t, origin = "phone")) +
            (1..4).map { i -> TranscriptEntry("w$i", "tool", text = "git log --oneline -$i", at = t + i * 5L, tool = "Bash", note = "Passo $i della verifica") }
    }

    // La risposta del turno al lavoro, nata da quel turno (le ore sono quelle del PC, come `turn_started`).
    private val answer = TranscriptEntry(
        "ans", "assistant",
        text = "Fatto: la release è partita.\n\n- Verifiche: tutte verdi.\n- Release: la CI costruisce la 1.65.4.\n- Memoria aggiornata.\n\nTi avviso quando la CI è verde.\n\nEsito: release 1.65.4 avviata",
        at = t0 + 2_460, turn = TranscriptTurn(t0 + 2_400, t0 + 2_460, 10_769, 300),
    )

    @Test fun theOutcomeStaysStillToThePixelWhileTheListRises() {
        val sess = mutableStateOf(busy)
        val arrived = mutableStateOf(false)
        val esito = mutableListOf<Float>()
        val step = mutableListOf<Float>()
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                val s by sess
                val feed = ChatFeed.merge(past() + work() + (if (arrived.value) listOf(answer) else emptyList()), emptyList())
                CmPhoneTheme { SessionSheet(s, st.ts, emptyList(), 120, none, feed = feed) }
                LaunchedEffect(Unit) {
                    // L'apertura: la chat si ancora in fondo, finisce l'apertura (un secondo) e vede la sessione lavorare.
                    repeat(90) { withFrameNanos {} }
                    arrived.value = true
                    sess.value = idle
                    repeat(54) {
                        withFrameNanos {}
                        val root = (view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                        esito += find(root) { n -> n.tag() == "chat-esito" }?.let { n -> n.positionInRoot.y + n.size.height } ?: Float.NaN
                        step += find(root) { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { a -> a.text.contains("Passo 2 della verifica") } == true }?.positionInRoot?.y ?: Float.NaN
                    }
                }
            }
        }
        paparazzi.gif(view, end = 2_700, fps = 60)

        // L'esito arriva al suo posto entro i primi fotogrammi e da lì non si muove più (al pixel), mentre la lista sale.
        assertTrue("nessun esito misurato: $esito", esito.isNotEmpty() && !esito.last().isNaN())
        val final = esito.last()
        val first = esito.indexOfFirst { y -> !y.isNaN() && abs(y - final) <= 0.5f }
        assertTrue("l'esito non arriva subito al suo posto: $esito", first in 0..5)
        assertTrue("l'esito si muove dopo essere comparso: ${esito.drop(first)}", esito.drop(first).all { y -> abs(y - final) <= 0.5f })
        // Il lavoro già letto sale e basta, una volta sola: mai giù, e sale davvero.
        val w = step.filter { y -> !y.isNaN() }
        assertTrue("il lavoro non è in vista: $step", w.size > 10)
        assertTrue("la lista torna giù o salta: $w", w.zipWithNext().all { (a, b) -> b <= a + 0.5f })
        val rise = w.first() - w.last()
        assertTrue("la lista non sale: $w", rise > 40f)
        // Una corsa, non un salto: la salita dura più fotogrammi e nessun passo ne fa più di metà (un salto in fondo farebbe tutto in uno).
        val moves = w.zipWithNext().map { (a, b) -> a - b }.filter { it > 0.5f }
        assertTrue("la lista salta invece di correre: $w", moves.size >= 5 && moves.max() < rise / 2)
    }

    // Franz 27/11 15:17: la sessione del compito appena finito, aperta con la risposta già arrivata.
    @Test fun aReplyAlreadyThereAtOpeningIsWholeAndStillFromTheFirstFrame() {
        wholeAndStill("risposta già arrivata", open(idle, past() + work() + answer))
    }

    // All'apertura la conversazione arriva a pezzi: prima senza la risposta, poi, a chat ferma, con la risposta di un compito già finito.
    @Test fun aReplyLoadedInPiecesAfterTheOpeningDoesNotArrive() {
        wholeAndStill("a pezzi", open(idle, past() + work(), switchAt = 90, then = idle, thenFeed = past() + work() + answer))
    }

    // Lo stato del telefono dice ancora «al lavoro» per qualche fotogramma, poi arrivano insieme lo stato vero e la risposta, prima che la
    // chat sia ferma: la risposta è di prima.
    @Test fun aStaleBusyStateAtOpeningDoesNotMakeTheReplyArrive() {
        wholeAndStill("stato vecchio", open(busy, past() + work(), switchAt = 20, then = idle, thenFeed = past() + work() + answer))
    }

    private class Frame(val arriving: Boolean, val entering: Boolean, val esito: Float, val visible: Boolean, val text: Boolean)

    private fun open(
        first: Session, firstFeed: List<TranscriptEntry>, switchAt: Int = -1, then: Session = first, thenFeed: List<TranscriptEntry> = firstFeed,
    ): List<Frame> {
        val sess = mutableStateOf(first)
        val entries = mutableStateOf(firstFeed)
        val out = mutableListOf<Frame>()
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                val s by sess
                val feed = ChatFeed.merge(entries.value, emptyList())
                CmPhoneTheme { SessionSheet(s, st.ts, emptyList(), 120, none, feed = feed) }
                LaunchedEffect(Unit) {
                    repeat(140) { i ->
                        if (i == switchAt) { sess.value = then; entries.value = thenFeed }
                        withFrameNanos {}
                        out += frame((view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode)
                    }
                }
            }
        }
        paparazzi.gif(view, end = 2_700, fps = 60)
        return out
    }

    // Un fotogramma: c'è una risposta in corsa o una riga che entra; dove sta l'esito e se si vede dentro la lista; se c'è il testo.
    private fun frame(root: SemanticsNode): Frame {
        val e = find(root) { n -> n.tag() == "chat-esito" }
        val list = generateSequence(e?.parent) { it.parent }.firstOrNull { it.config.contains(SemanticsProperties.VerticalScrollAxisRange) }
        val bottom = e?.let { it.positionInRoot.y + it.size.height } ?: Float.NaN
        val box = list?.boundsInRoot
        return Frame(
            arriving = find(root) { n -> n.tag() == "risposta-in-arrivo" } != null,
            entering = find(root) { n -> n.tag() == "riga-in-entrata" } != null,
            esito = bottom,
            visible = e != null && box != null && e.positionInRoot.y >= box.top - 0.5f && bottom <= box.bottom + 0.5f,
            text = find(root) { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { a -> a.text.contains("la release è partita") } == true } != null,
        )
    }

    private fun wholeAndStill(what: String, fs: List<Frame>) {
        assertTrue("$what: la risposta arriva con la corsa", fs.none { it.arriving })
        assertTrue("$what: una riga entra con l'animazione", fs.none { it.entering })
        val first = fs.indexOfFirst { it.visible }
        assertTrue("$what: l'esito non si vede mai", first >= 0)
        val shown = fs.drop(first)
        assertTrue("$what: al primo fotogramma con l'esito manca il testo della risposta", shown.first().text)
        val final = shown.last().esito
        assertTrue("$what: l'esito si muove dopo essere comparso: ${shown.map { it.esito }}", shown.all { abs(it.esito - final) <= 0.5f })
        assertTrue("$what: il testo della risposta sparisce", shown.all { it.text && it.visible })
    }

    private fun SemanticsNode.tag(): String? = config.getOrNull(SemanticsProperties.TestTag)

    private fun find(n: SemanticsNode, p: (SemanticsNode) -> Boolean): SemanticsNode? =
        if (p(n)) n else n.children.firstNotNullOfOrNull { find(it, p) }

    // Fotogrammi buttati, come in ChatReadingHoldTest.
    private fun <T> silent(type: Class<T>): T = type.cast(
        java.lang.reflect.Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, _ ->
            when (m.name) {
                "newFrameHandler" -> silent(SnapshotHandler.FrameHandler::class.java)
                "hashCode" -> 0
                "equals" -> false
                "toString" -> "silent"
                else -> null
            }
        },
    )
}
