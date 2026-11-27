package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.RateValue
import it.pixelbox.cmwatch.rules.SpeechRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Prova 1391 (Franz, 27/11 11:38): trascinando la velocità la seconda cifra andava e veniva e la pillola del valore cambiava
 * larghezza a scatti, portandosi dietro lo slider. La pillola è larga uguale al minimo, a 1×, al massimo e ai valori a due cifre.
 */
abstract class RateValueWidthCheck(locale: String, scale: Float) {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = locale, fontScale = scale),
        theme = "android:Theme.Material.NoActionBar", snapshotHandler = silent(SnapshotHandler::class.java),
    )

    @Test fun sameWidthFromMinToMax() {
        val values = listOf(SpeechRate.MIN, SpeechRate.NORMAL, SpeechRate.MAX, 1.15f, 0.55f, 1.05f)
        val widths = FloatArray(values.size) { -1f }
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                CmPhoneTheme(still = true) {
                    Column { values.forEachIndexed { i, v -> Box(Modifier.testTag("v$i")) { RateValue(v, onClick = {}) } } }
                }
                LaunchedEffect(Unit) {
                    repeat(3) { withFrameNanos {} }
                    val root = (view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                    values.indices.forEach { i -> find(root, "v$i")?.let { n -> widths[i] = n.boundsInRoot.width } }
                }
            }
        }
        paparazzi.gif(view, end = 200, fps = 30)
        assertTrue("pillole non trovate: ${widths.toList()}", widths.all { it > 0f })
        values.indices.forEach { i -> assertEquals("a ${values[i]}× la pillola è larga ${widths[i]} invece di ${widths[0]}", widths[0], widths[i], 0.5f) }
    }

    private fun find(n: SemanticsNode, tag: String): SemanticsNode? =
        if (n.config.getOrNull(SemanticsProperties.TestTag) == tag) n else n.children.firstNotNullOfOrNull { find(it, tag) }

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

class RateValueWidthTest : RateValueWidthCheck("it", 1f)

class RateValueWidthEnglishLargeTest : RateValueWidthCheck("en", 1.3f)
