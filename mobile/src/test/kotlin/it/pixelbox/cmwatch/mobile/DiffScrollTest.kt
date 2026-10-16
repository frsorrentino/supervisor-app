package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.Hunk
import it.pixelbox.cmwatch.contract.StepDiff
import it.pixelbox.cmwatch.mobile.ui.DiffOpener
import it.pixelbox.cmwatch.mobile.ui.LocalDiffOpener
import it.pixelbox.cmwatch.mobile.ui.StepDiffRow
import it.pixelbox.cmwatch.rules.DiffRules
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Revisione finale del diff dal telefono (16/10): un passaggio con «Mostra tutto» già toccato, uscito di vista scorrendo la
 * chat e tornato, non richiede di nuovo il passaggio intero al PC (fino a 1 MB). Paparazzi fa solo da motore dei fotogrammi.
 */
class DiffScrollTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, snapshotHandler = silent(SnapshotHandler::class.java))

    @Test fun theWholeStepIsAskedOnceAcrossScrolls() {
        var asked = 0
        val cut = StepDiff("/w/blog/bench/results.csv", "create", 261, 0, listOf(Hunk(0, 0, 1, 261, List(8) { "+home,$it,400,36" })), cut = true)
        val whole = cut.copy(hunks = listOf(Hunk(0, 0, 1, 261, List(261) { "+home,$it,400,36" })), cut = false)
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                val opener = remember { DiffOpener(full = { asked++; DiffRules.Load.Ok(whole) }) }
                val list = rememberLazyListState()
                LaunchedEffect(Unit) {
                    repeat(10) { withFrameNanos {} }
                    list.scrollToItem(40)
                    repeat(10) { withFrameNanos {} }
                    list.scrollToItem(0)
                    repeat(10) { withFrameNanos {} }
                }
                CompositionLocalProvider(LocalDiffOpener provides opener) {
                    LazyColumn(Modifier.fillMaxSize(), list) {
                        item(key = "step") { StepDiffRow("w1", cut, startOpen = true, startAll = true) }
                        items(40, key = { "pad$it" }) { Box(Modifier.fillMaxWidth().height(120.dp)) }
                    }
                }
            }
        }
        paparazzi.gif(view, end = 2000, fps = 60)
        assertEquals("il passaggio intero chiesto al PC", 1, asked)
    }

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
