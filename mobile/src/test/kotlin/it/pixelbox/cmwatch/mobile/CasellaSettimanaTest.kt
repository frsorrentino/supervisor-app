package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
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
 * grande le tre caselle restano uguali. Il caso di Franz: nessuna sessione ferma su 3, 1 al lavoro, 48 % della settimana. Le misure si
 * prendono quando il tabellone è impaginato, senza aspettare fotogrammi.
 */
class CasellaSettimanaTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = W360, screenHeight = 2400), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )
    private val st = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
    private val head = Kpi.Head(0, null, 1, null, null, open = 3)
    private val boxes = listOf("home-box-aspetta", "home-box-lavoro", "home-box-utilizzo")

    /** Le misure prese quando il tabellone è impaginato: le altezze delle tre caselle e delle due righe della didascalia, i tag visti. */
    private class Seen(val heights: List<Int>, val label: Int?, val reset: Int?, val tags: Set<String>)

    private fun board(reset: String, scale: Float = 1f): Seen {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = W360, screenHeight = 2400, fontScale = scale))
        val usage = UsageCell(48, 40f, V3.Tx2, "della settimana", false, reset = reset, onToggle = {})
        var seen = Seen(emptyList(), null, null, emptySet())
        paparazzi.snapshot {
            val v = LocalView.current
            CmPhoneTheme(still = true) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).onGloballyPositioned {
                        val r = (v as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                        val use = find(r) { n -> n.tag() == "home-box-utilizzo" }
                        seen = Seen(
                            boxes.mapNotNull { t -> find(r) { n -> n.tag() == t }?.size?.height },
                            use?.let { u -> find(u) { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "della settimana" } == true }?.size?.height },
                            find(r) { n -> n.tag() == "home-box-utilizzo-azzera" }?.size?.height,
                            tags(r),
                        )
                    },
                ) { Kpi7(head, st, st.ts, {}, usage = usage) }
            }
        }
        return seen
    }

    private fun equalHeights(c: Seen, what: String) {
        assertTrue("$what: mancano caselle (${c.heights}); tag visti: ${c.tags}", c.heights.size == 3)
        assertTrue("$what: le tre caselle hanno altezze diverse: ${c.heights}", c.heights.all { abs(it - c.heights[0]) <= 1 })
    }

    // Una riga sola: l'altezza del testo non supera la sua interlinea (15 sp per l'azzeramento, 17 sp per «della settimana») a 440 dpi.
    private fun oneLine(h: Int?, lineSp: Int, what: String) {
        assertTrue("$what non c'è", h != null)
        assertTrue("$what va a capo: $h px", h!! <= lineSp * DENSITY + 2)
    }

    @Test fun theWeekBoxIsAsTallAsTheOthersAt360() {
        for (reset in listOf("si azzera mar 5:00", "si azzera mer 23:30")) {
            val c = board(reset)
            equalHeights(c, reset)
            oneLine(c.reset, 15, "«$reset»")
            oneLine(c.label, 17, "«della settimana»")
        }
    }

    @Test fun withLargeTextTheThreeBoxesStayEqual() {
        equalHeights(board("si azzera mar 5:00", scale = 1.3f), "testo grande")
    }

    private fun tags(n: SemanticsNode): Set<String> = (listOfNotNull(n.tag()) + n.children.flatMap { tags(it) }).toSet()

    private fun SemanticsNode.tag(): String? = config.getOrNull(SemanticsProperties.TestTag)

    private fun find(n: SemanticsNode, p: (SemanticsNode) -> Boolean): SemanticsNode? =
        if (p(n)) n else n.children.firstNotNullOfOrNull { find(it, p) }

    // Immagini buttate: qui contano le misure.
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
