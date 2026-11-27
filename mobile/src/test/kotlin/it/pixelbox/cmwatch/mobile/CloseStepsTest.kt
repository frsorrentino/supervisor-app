package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.toSize
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CloseSplashOverlay
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.rules.CloseSplash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * I passi della chiusura (segnalazione di Franz del 27/11, «miglioriamo il loader della chiusura sessione?»; dalle 12:00 lo Step progress,
 * app-chiusura-step-progress-27-11). A 360 dp, anche col testo grande e in inglese, ogni passo ha il suo nome intero su una riga e dentro la
 * scheda, i passi sono larghi quanto la scheda e il passo in corso non si ripete in una riga sotto.
 */
abstract class CloseStepsCheck(locale: String, scale: Float, private val save: List<String>, private val plain: List<String>) {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = locale, screenWidth = (360 * 2.75).toInt(), screenHeight = (800 * 2.75).toInt(), fontScale = scale),
        theme = "android:Theme.Material.NoActionBar", snapshotHandler = silent(SnapshotHandler::class.java),
    )
    private val ts = ContractJson.decodeState(File("../contract/state-1-question.json").readText()).ts

    // «Salva e chiudi»: salvataggio fatto, uscita in corso, chiusa da fare.
    @Test fun saveAndCloseNamesItsSteps() =
        check(CloseSplash.Phase.Closing("atlas-shop", ts - 20, delivered = true, slow = false, save = true, step = "exit"), save, running = save[1])

    // Chiudi senza salvare: inviata, chiusa in corso.
    @Test fun plainCloseNamesItsSteps() =
        check(CloseSplash.Phase.Closing("atlas-shop", ts - 3, delivered = true, slow = false), plain, running = plain[1])

    private class Seen { var card: Rect? = null; var bar: Rect? = null; val texts = mutableListOf<Pair<String, TextLayoutResult?>>(); val boxes = mutableListOf<Pair<String, Rect>>() }

    private fun check(phase: CloseSplash.Phase, names: List<String>, running: String) {
        val seen = Seen()
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                CmPhoneTheme(still = true) { CloseSplashOverlay(phase, onHome = {}, onReopen = null) }
                LaunchedEffect(Unit) {
                    repeat(5) { withFrameNanos {} }
                    val root = (view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                    seen.card = find(root) { n -> n.config.getOrNull(SemanticsProperties.TestTag) == "close-card" }?.boundsInRoot
                    seen.bar = find(root) { n -> n.config.getOrNull(SemanticsProperties.TestTag) == "passi-chiusura" }?.boundsInRoot
                    walk(root) { n ->
                        n.config.getOrNull(SemanticsProperties.Text)?.let { t ->
                            val out = mutableListOf<TextLayoutResult>()
                            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(out)
                            seen.texts += t.joinToString("") { it.text } to out.firstOrNull()
                            // Dove sta davvero, non tagliato dai contenitori: un nome agli estremi esce dalla riga dei nomi ma deve restare nella scheda.
                            seen.boxes += t.joinToString("") { it.text } to Rect(n.positionInRoot, n.size.toSize())
                        }
                    }
                }
            }
        }
        paparazzi.gif(view, end = 500, fps = 30)

        val card = requireNotNull(seen.card) { "nessuna scheda della chiusura" }
        val bar = requireNotNull(seen.bar) { "nessun passo della chiusura" }
        // Il contenuto della scheda sta a 18 dp dai suoi bordi (CloseCardFrame): la barra ne prende tutta la larghezza.
        val inner = card.width - 2 * 18 * paparazzi.context.resources.displayMetrics.density
        assertEquals("i passi non sono larghi quanto la scheda", inner, bar.width, 2f)
        for (name in names.distinct()) {
            val hit = seen.texts.filter { it.first == name }
            assertEquals("«$name» non sta una volta sola nella scheda (${seen.texts.map { it.first }})", 1, hit.size)
            val tl = hit.single().second
            val box = seen.boxes.first { it.first == name }.second
            // Intero su una riga: la sua larghezza su una riga sta nello spazio che ha. (didOverflowWidth non serve: per un Text con una String il
            // risultato che la semantica restituisce è ricalcolato sulla larghezza massima ricevuta, e sembra sempre sforare.)
            assertTrue("«$name» va a capo o non sta intero", tl != null && tl.lineCount == 1 && tl.multiParagraph.intrinsics.maxIntrinsicWidth <= box.width + 1f)
            assertTrue("«$name» esce dalla scheda ($box fuori da $card)", box.left >= card.left - 0.5f && box.right <= card.right + 0.5f)
        }
        assertEquals("il passo in corso «$running» si ripete fuori dal segmento (${seen.texts.map { it.first }})", 1, seen.texts.count { it.first.contains(running) })
    }

    private fun find(n: SemanticsNode, p: (SemanticsNode) -> Boolean): SemanticsNode? =
        if (p(n)) n else n.children.firstNotNullOfOrNull { find(it, p) }

    private fun walk(n: SemanticsNode, f: (SemanticsNode) -> Unit) { f(n); n.children.forEach { walk(it, f) } }

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

class CloseStepsTest : CloseStepsCheck("it", 1f, listOf("Salvo", "Esco", "Chiusa"), listOf("Inviata", "Chiusa"))

class CloseStepsLargeTextTest : CloseStepsCheck("it", 1.3f, listOf("Salvo", "Esco", "Chiusa"), listOf("Inviata", "Chiusa"))

class CloseStepsEnglishTest : CloseStepsCheck("en", 1.3f, listOf("Saving", "Exiting", "Closed"), listOf("Sent", "Closed"))
