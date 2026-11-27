package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.Kpi7
import it.pixelbox.cmwatch.mobile.ui.UsageCell
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.Kpi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * app-casella-settimana-27-11 (Franz 27/11 16:41, scelta A): nel tabellone della home la casella della settimana è alta quanto le altre due
 * a 360 dp. La sua didascalia sta su due righe come nelle altre, «della settimana» e «si azzera mar 5:00», ciascuna su una riga; col testo
 * grande le tre caselle restano uguali. Il caso di Franz: nessuna sessione ferma su 3, 1 al lavoro, 48 % della settimana.
 */
class CasellaSettimanaTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = W360, screenHeight = 2400), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )
    private val st = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
    private val head = Kpi.Head(0, null, 1, null, null, open = 3)
    private val boxes = listOf("home-box-aspetta", "home-box-lavoro", "home-box-utilizzo")

    private fun board(reset: String, scale: Float = 1f): Map<String, SemanticsNode> {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = W360, screenHeight = 2400, fontScale = scale))
        val usage = UsageCell(48, 40f, V3.Tx2, "della settimana", false, reset = reset, onToggle = {})
        var root: SemanticsNode? = null
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                CmPhoneTheme(still = true) { Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Kpi7(head, st, st.ts, {}, usage = usage) } }
                LaunchedEffect(Unit) {
                    repeat(4) { withFrameNanos {} }
                    root = (view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                }
            }
        }
        paparazzi.gif(view, end = 300, fps = 30)
        val r = root ?: error("nessun albero letto")
        return (boxes + "home-box-utilizzo-azzera").associateWith { t -> find(r) { n -> n.tag() == t } ?: error("manca $t") }
    }

    private fun equalHeights(c: Map<String, SemanticsNode>, what: String) {
        val h = boxes.map { c.getValue(it).size.height }
        assertTrue("$what: le tre caselle hanno altezze diverse: $h", h.all { abs(it - h[0]) <= 1 })
    }

    // Una riga sola: l'altezza del testo non supera la sua interlinea (15 sp per l'azzeramento, 17 sp per «della settimana») a 440 dpi.
    private fun oneLine(n: SemanticsNode?, lineSp: Int, what: String) {
        val h = n?.size?.height ?: error("manca $what")
        assertTrue("$what va a capo: $h px", h <= lineSp * DENSITY + 2)
    }

    @Test fun theWeekBoxIsAsTallAsTheOthersAt360() {
        for (reset in listOf("si azzera mar 5:00", "si azzera mer 23:30")) {
            val c = board(reset)
            equalHeights(c, reset)
            oneLine(c.getValue("home-box-utilizzo-azzera"), 15, "«$reset»")
            val label = find(c.getValue("home-box-utilizzo")) { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "della settimana" } == true }
            oneLine(label, 17, "«della settimana»")
        }
    }

    @Test fun withLargeTextTheThreeBoxesStayEqual() {
        equalHeights(board("si azzera mar 5:00", scale = 1.3f), "testo grande")
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

    private companion object {
        /** 360 dp a 440 dpi (PIXEL_5). */
        const val DENSITY = 2.75f
        const val W360 = 990
    }
}
