package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
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
        // Si legge a ogni impaginazione: l'ultima vede anche il ▶, che compare dopo la prima misura del testo.
        var shortPlay = true
        var longPlay = false
        var label: String? = null
        paparazzi.snapshot {
            val v = LocalView.current
            CompositionLocalProvider(LocalSpeakText provides { _: String -> }) {
                CmPhoneTheme {
                    Column(
                        Modifier.width(328.dp).onGloballyPositioned {
                            val r = (v as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                            val short = find(r) { n -> n.tag() == "corto" }
                            val long = find(r) { n -> n.tag() == "lungo" }
                            shortPlay = short == null || find(short) { n -> n.tag() == "leggi" } != null
                            val play = long?.let { l -> find(l) { n -> n.tag() == "leggi" } }
                            longPlay = play != null
                            label = play?.let { p -> find(p) { n -> n.config.getOrNull(SemanticsProperties.ContentDescription) != null } }
                                ?.config?.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                        },
                    ) {
                        LongText("Una riga sola", Modifier.testTag("corto"))
                        LongText(
                            "Un testo che a questa larghezza va di sicuro oltre una riga, perché continua ancora per un bel pezzo prima di finire.",
                            Modifier.testTag("lungo"),
                        )
                    }
                }
            }
        }
        assertTrue("una riga sola ha il ▶", !shortPlay)
        assertTrue("oltre una riga manca il ▶", longPlay)
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
