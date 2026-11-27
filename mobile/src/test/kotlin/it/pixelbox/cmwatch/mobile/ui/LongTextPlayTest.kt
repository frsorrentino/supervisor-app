package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * app-play-lettura-ovunque-27-11 (Franz 27/11 13:29): il ▶ della lettura è il tasto unico dell'app, quello della chat, con l'etichetta
 * «Leggi ad alta voce». Quando compare (solo oltre una riga) lo dicono ReadRulesTest in core e la foto di LongTextTest.
 */
class LongTextPlayTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )

    @Test fun theReadButtonSaysReadAloud() {
        var tagged = false
        var label: String? = null
        paparazzi.snapshot {
            val v = LocalView.current
            CompositionLocalProvider(LocalSpeakText provides { _: String -> }) {
                CmPhoneTheme {
                    Column(
                        Modifier.width(328.dp).onGloballyPositioned {
                            val r = (v as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                            val play = find(r) { n -> n.tag() == "leggi" }
                            tagged = play != null
                            label = play?.let { p -> find(p) { n -> n.config.getOrNull(SemanticsProperties.ContentDescription) != null } }
                                ?.config?.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                        },
                    ) { ReadButton("Un testo da leggere") }
                }
            }
        }
        assertTrue("manca il ▶", tagged)
        assertEquals("Leggi ad alta voce", label)
    }

    // app-play-voce-ovunque-27-11 (prova 1400): con la voce data da ProvideSpeech, quella che avvolge tutta l'app, il ▶ c'è; senza voce no.
    @Test fun theAppWideVoiceGivesThePlay() {
        fun seen(withVoice: Boolean): Boolean {
            var tagged = false
            paparazzi.snapshot {
                val v = LocalView.current
                CmPhoneTheme {
                    Column(
                        Modifier.width(328.dp).onGloballyPositioned {
                            tagged = find((v as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode) { n -> n.tag() == "leggi" } != null
                        },
                    ) {
                        if (withVoice) ProvideSpeech(null, null, null, 1f, { t -> listOf(it.pixelbox.cmwatch.rules.AnswerText.Block(it.pixelbox.cmwatch.rules.AnswerText.Kind.PARA, t)) }, {}, {}) { ReadButton("Un testo da leggere") }
                        else ReadButton("Un testo da leggere")
                    }
                }
            }
            return tagged
        }
        assertTrue("con la voce di tutta l'app manca il ▶", seen(withVoice = true))
        assertTrue("senza voce il ▶ non deve esserci", !seen(withVoice = false))
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
