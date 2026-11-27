package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * app-play-lettura-ovunque-27-11 (Franz 27/11 13:29): il ▶ della lettura compare accanto a un testo solo quando il testo misurato va oltre
 * una riga, ed è il tasto unico dell'app, «Leggi ad alta voce». Una riga sola, niente tasto.
 */
class LongTextPlayTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )

    @Test fun thePlayShowsOnlyBeyondOneLine() {
        var root: SemanticsNode? = null
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                CompositionLocalProvider(LocalSpeakText provides { _: String -> }) {
                    CmPhoneTheme {
                        Column(Modifier.width(328.dp)) {
                            LongText("Una riga sola", Modifier.testTag("corto"))
                            LongText(
                                "Un testo che a questa larghezza va di sicuro oltre una riga, perché continua ancora per un bel pezzo prima di finire.",
                                Modifier.testTag("lungo"),
                            )
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    repeat(6) { withFrameNanos {} }
                    root = (view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                }
            }
        }
        paparazzi.gif(view, end = 1_000, fps = 60)

        val r = root ?: error("nessun albero letto")
        val short = find(r) { n -> n.tag() == "corto" } ?: error("manca il testo corto")
        val long = find(r) { n -> n.tag() == "lungo" } ?: error("manca il testo lungo")
        assertNull("una riga sola ha il ▶", find(short) { n -> n.tag() == "leggi" })
        val play = find(long) { n -> n.tag() == "leggi" }
        assertTrue("oltre una riga manca il ▶", play != null)
        val label = find(play!!) { n -> n.config.getOrNull(SemanticsProperties.ContentDescription) != null }
            ?.config?.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
        assertEquals("Leggi ad alta voce", label)
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
