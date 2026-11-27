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

    // La risposta nasce dopo l'apertura della chat: è lei che arriva.
    private val answer = TranscriptEntry(
        "ans", "assistant",
        text = "Fatto: la release è partita.\n\n- Verifiche: tutte verdi.\n- Release: la CI costruisce la 1.65.4.\n- Memoria aggiornata.\n\nTi avviso quando la CI è verde.\n\nEsito: release 1.65.4 avviata",
        at = System.currentTimeMillis() / 1000 + 60, turn = TranscriptTurn(t0 + 2_400, t0 + 2_460, 10_769, 300),
    )

    @Test fun theOutcomeStaysStillToThePixelWhileTheListRises() {
        val busy = st.sessions.first { it.state == SessionState.BUSY }.copy(question = null)
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
                    // L'apertura: la chat si ancora in fondo e lo segue.
                    repeat(60) { withFrameNanos {} }
                    arrived.value = true
                    sess.value = busy.copy(state = SessionState.IDLE)
                    repeat(54) {
                        withFrameNanos {}
                        val root = (view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                        esito += find(root) { n -> n.config.getOrNull(SemanticsProperties.TestTag) == "chat-esito" }?.let { n -> n.positionInRoot.y + n.size.height } ?: Float.NaN
                        step += find(root) { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { a -> a.text.contains("Passo 2 della verifica") } == true }?.positionInRoot?.y ?: Float.NaN
                    }
                }
            }
        }
        paparazzi.gif(view, end = 2_200, fps = 60)

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
        assertTrue("la lista non sale: $w", w.first() - w.last() > 40f)
    }

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
