package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.GenieClose
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * La × del Genie al suo posto (Franz, 12/11 15:11, screenshot dal telefono): aperto «Vai a» dal nome della sessione, il tondo con la
 * × stava dentro il pannello, sopra la riga «Master», e non sul tasto toccato. Il tondo si posava prima di sapere dove sta il
 * pannello sullo schermo e nessuno lo spostava più: finiva più in basso e più a destra di quanto il pannello dista dall'angolo
 * dello schermo (12 dp e la barra in alto).
 */
class GenieCloseTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )

    // Il pannello sta sotto la barra e a 12 dp dal bordo, come in PanelShell; il tasto toccato sta sopra, nella barra.
    @Test fun closeSitsOnThePressedPointWhereverThePanelIs() {
        val press = Offset(700f, 180f)
        var centre: Offset? = null
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                CmPhoneTheme(still = true) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.padding(start = 12.dp, top = 120.dp).fillMaxWidth()) {
                            Box(Modifier.fillMaxWidth().height(300.dp))
                            GenieClose(press, { 1f }) {}
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    repeat(10) { withFrameNanos {} }
                    val root = (view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                    val x = all(root).firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == "genie-chiudi" }
                    val screen = IntArray(2).also { view.getLocationOnScreen(it) }
                    centre = x?.let { Offset(screen[0] + it.positionInRoot.x + it.size.width / 2f, screen[1] + it.positionInRoot.y + it.size.height / 2f) }
                }
            }
        }
        paparazzi.gif(view, end = 500, fps = 60)
        assertNotNull("la × del Genie non c'è", centre)
        assertTrue("la × non sta sul punto toccato: è a $centre, il tocco a $press", (centre!! - press).getDistance() <= 1.5f)
    }

    private fun all(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap { all(it) }

    // Fotogrammi buttati, come in ChatScrollTest: un `SnapshotHandler` scritto qui nominerebbe `BufferedImage`.
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
