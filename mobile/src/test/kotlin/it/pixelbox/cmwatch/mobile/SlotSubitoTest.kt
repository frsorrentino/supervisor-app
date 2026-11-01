package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.ComposeView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.mobile.ui.rememberSentOrder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Il difetto di Franz del 01/11 19:38: il gesto vive in `pointerInput(Unit)`, cattura l'oggetto della prima composizione e non riparte.
 * Qui lo fa un `LaunchedEffect(Unit)`: il relay manda il suo ordine, poi il gesto sceglie, e nessuna spinta nuova arriva.
 */
class SlotSubitoTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, snapshotHandler = silent(SnapshotHandler::class.java))

    @Test fun slot_subito_laSceltaSiVedeSenzaSpintaEUnoStatoVecchioNonLaToglie() {
        val old = listOf("sessions")
        val mine = listOf("sessions", "pin:system")
        val seen = mutableListOf<List<String>?>()
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                var relay by remember { mutableStateOf<List<String>?>(null) }
                val s = rememberSentOrder(relay)
                seen += s.shown
                Text(s.shown.toString())
                LaunchedEffect(Unit) {
                    withFrameNanos {}
                    relay = old            // il primo stato col suo ordine
                    repeat(3) { withFrameNanos {} }
                    s.send(mine)           // il rilascio: nessuna spinta dopo
                    repeat(3) { withFrameNanos {} }
                    relay = ArrayList(old) // arriva uno stato con l'ordine di prima
                    repeat(3) { withFrameNanos {} }
                }
            }
        }
        paparazzi.gif(view, end = 500, fps = 60)
        assertEquals(mine, seen.last())
        assertEquals("la riga compare subito, non torna indietro", 1, seen.dropWhile { it != mine }.distinct().size)
    }

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
