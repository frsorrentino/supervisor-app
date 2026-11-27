package it.pixelbox.cmwatch.mobile

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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

/**
 * app-scroll-master-lavoro-27-11 (video di Franz, 27/11 04:16): con la sessione al lavoro, un passo nuovo ogni secondo, la chat
 * riletta più in alto tornava in fondo da sola, anche col dito fermo sullo schermo. Il gesto è vero (MotionEvent sulla vista,
 * un fotogramma per movimento) e Paparazzi fa da motore dei fotogrammi, come in SuggFocusTest. Tre casi: riletta e lasciata,
 * riletta col dito appoggiato e fermo, in fondo (lì la chat segue il passo nuovo).
 */
class ChatReadingHoldTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val t0 = st.ts - 3_600

    private fun past(): List<TranscriptEntry> = (1..6).flatMap { k ->
        val t = t0 + k * 300L
        listOf(
            TranscriptEntry("u$k", "user", text = "Domanda $k: controlla la release e dimmi cosa manca", at = t, origin = "phone"),
            TranscriptEntry("a$k", "assistant", text = "Risposta $k. " + "Ho guardato la release, i test e le note: il kernel dà la stessa uscita del vecchio in dodici situazioni, su Linux e su Windows, e la suite è verde. ".repeat(4), at = t + 60, turn = TranscriptTurn(t, t + 60, 4000, 120)),
        )
    }

    // Il turno al lavoro: la richiesta e i passi fino a `n`, senza risposta.
    private fun live(n: Int): List<TranscriptEntry> {
        val t = t0 + 2_400
        return listOf(TranscriptEntry("w0", "user", text = "Verifica la release e avviala", at = t, origin = "phone")) +
            (1..n).map { i -> TranscriptEntry("w$i", "tool", text = "git log --oneline -$i", at = t + i * 5L, tool = "Bash", note = "Passo $i della verifica") }
    }

    private class Seen { var started = false; var anchored: String? = null; var drift = 0f; var lost = 0; var newestVisible = false; var newestBottom = 0f; var fieldTop = 0f }

    // Riletta e lasciata: nessun movimento della chat mentre arrivano i passi.
    @Test fun scrolledUpChatStaysWhileStepsArrive() {
        val r = run(drag = true, hold = false)
        assertTrue("il gesto non è partito", r.started)
        assertTrue("dopo il gesto nessuna risposta in vista da seguire", r.anchored != null)
        assertTrue("la chat riletta si è spostata di ${r.drift} px mentre arrivavano i passi (${r.anchored})", r.drift < 2f && r.lost == 0)
    }

    // Riletta col dito appoggiato e fermo: come sopra, il dito non conta.
    @Test fun scrolledUpChatStaysUnderAStillFinger() {
        val r = run(drag = true, hold = true)
        assertTrue("il gesto non è partito", r.started)
        assertTrue("dopo il gesto nessuna risposta in vista da seguire", r.anchored != null)
        assertTrue("la chat riletta col dito fermo si è spostata di ${r.drift} px (${r.anchored})", r.drift < 2f && r.lost == 0)
    }

    // In fondo: il passo nuovo si vede, sopra il campo.
    @Test fun chatAtTheEndFollowsTheNewStep() {
        val r = run(drag = false, hold = false)
        assertTrue("in fondo il passo nuovo non è in vista (fondo ${r.newestBottom}, campo ${r.fieldTop})", r.newestVisible)
    }

    private fun run(drag: Boolean, hold: Boolean): Seen {
        val seen = Seen()
        val s = st.sessions.first { it.state == SessionState.BUSY }.copy(question = null)
        val steps = mutableStateOf(3)
        val now = mutableLongStateOf(st.ts)
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                val n by steps
                val feed = ChatFeed.merge(past() + live(n), emptyList())
                CmPhoneTheme { SessionSheet(s, now.longValue, emptyList(), 120, none, feed = feed) }
                LaunchedEffect(Unit) {
                    repeat(40) { withFrameNanos {} }
                    val root = view.getChildAt(0)
                    fun tree() = (root as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                    val h = root.height.toFloat()
                    val x = root.width / 2f
                    var down: Long? = null
                    if (drag) {
                        // Il dito scende di 900 px in 18 movimenti, uno per fotogramma: la chat risale a rileggere.
                        val t = SystemClock.uptimeMillis()
                        val y0 = h * 0.3f
                        root.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y0, 0))
                        for (k in 1..18) { withFrameNanos {}; root.dispatchTouchEvent(MotionEvent.obtain(t, t + k * 16L, MotionEvent.ACTION_MOVE, x, y0 + k * 50f, 0)) }
                        withFrameNanos {}
                        root.dispatchTouchEvent(MotionEvent.obtain(t, t + 19 * 16L, MotionEvent.ACTION_UP, x, y0 + 900f, 0))
                        seen.started = true
                        // Lo slancio finisce, poi la chat è ferma dove l'ha lasciata il dito.
                        repeat(120) { withFrameNanos {} }
                        val k = answers(tree()).firstOrNull { (_, b) -> b > h * 0.1f && b < h * 0.7f }?.first
                        seen.anchored = k
                        if (hold) {
                            val td = SystemClock.uptimeMillis()
                            root.dispatchTouchEvent(MotionEvent.obtain(td, td, MotionEvent.ACTION_DOWN, x, h * 0.5f, 0))
                            down = td
                        }
                    }
                    val y0 = seen.anchored?.let { k -> answers(tree()).firstOrNull { it.first == k }?.second }
                    // Quattro secondi di lavoro: un passo nuovo e l'orologio avanti a ogni secondo.
                    repeat(4 * 60) { f ->
                        withFrameNanos {}
                        if (f % 60 == 30) { steps.value += 1; now.longValue += 1 }
                        if (y0 != null) {
                            val y = answers(tree()).firstOrNull { it.first == seen.anchored }?.second
                            if (y == null) seen.lost++ else seen.drift = maxOf(seen.drift, kotlin.math.abs(y - y0))
                        }
                    }
                    down?.let { td -> root.dispatchTouchEvent(MotionEvent.obtain(td, td + 4_000, MotionEvent.ACTION_UP, x, h * 0.5f, 0)) }
                    repeat(30) { withFrameNanos {} }
                    val newest = "Passo ${steps.value} della verifica"
                    val nb = find(tree()) { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { a -> a.text.contains(newest) } == true }?.boundsInRoot
                    val field = find(tree()) { n -> SemanticsProperties.EditableText in n.config }?.boundsInRoot
                    seen.newestBottom = nb?.bottom ?: -1f
                    seen.fieldTop = field?.top ?: h
                    seen.newestVisible = nb != null && nb.bottom > 0f && nb.bottom <= (field?.top ?: h) + 1f
                }
            }
        }
        paparazzi.gif(view, end = 7_500, fps = 60)
        return seen
    }

    // Le risposte in vista: la chiave («Risposta k») e il bordo alto del suo testo.
    private fun answers(n: SemanticsNode): List<Pair<String, Float>> {
        val out = mutableListOf<Pair<String, Float>>()
        fun walk(x: SemanticsNode) {
            x.config.getOrNull(SemanticsProperties.Text)?.firstNotNullOfOrNull { a -> Regex("Risposta (\\d+)\\.").find(a.text)?.value }?.let { k -> out += k to x.boundsInRoot.top }
            x.children.forEach(::walk)
        }
        walk(n)
        return out.distinctBy { it.first }
    }

    private fun find(n: SemanticsNode, p: (SemanticsNode) -> Boolean): SemanticsNode? =
        if (p(n)) n else n.children.firstNotNullOfOrNull { find(it, p) }

    // Fotogrammi buttati, come in ChatScrollTest.
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
