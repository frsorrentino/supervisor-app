package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.ComposeView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.mobile.ui.rememberDraftField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Invio spento dopo /clear e dopo il passaggio di consegne (Franz 02/11 09:02, app-invio-dopo-clear-02-11): l'id della sessione cambia, la bozza
 * diventa un'altra, il campo deve scrivere in quella nuova. Il testo che si vede nel campo è quello che parte. Paparazzi fa solo da motore dei fotogrammi.
 */
class DraftFieldTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, snapshotHandler = silent(SnapshotHandler::class.java))

    @Test fun invio_dopo_clear_ilCampoScriveNellaBozzaNuova() {
        val old = mutableStateOf("")
        val new = mutableStateOf("")
        var shown = "x"
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                // Come la scheda: la bozza dipende da id e domanda, e onDraft scrive in quella della composizione corrente.
                var holder by remember { mutableStateOf<MutableState<String>>(old) }
                val h = holder
                val field = rememberDraftField(h) { h.value = it }
                LaunchedEffect(Unit) {
                    repeat(3) { withFrameNanos {} }
                    holder = new
                    repeat(3) { withFrameNanos {} }
                    field.edit { replace(0, length, "/clear") }
                    repeat(5) { withFrameNanos {} }
                    shown = field.text.toString()
                }
            }
        }
        paparazzi.gif(view, end = 1000, fps = 60)
        assertEquals("/clear", shown)
        assertEquals("il testo del campo è nella bozza che parte", "/clear", new.value)
        assertEquals("", old.value)
    }

    @Test fun invio_dopo_clear_laBozzaNuovaSiVedeNelCampo() {
        val old = mutableStateOf("vecchio")
        val new = mutableStateOf("nuovo")
        var shown = ""
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                var holder by remember { mutableStateOf<MutableState<String>>(old) }
                val h = holder
                val field = rememberDraftField(h) { h.value = it }
                LaunchedEffect(Unit) {
                    repeat(3) { withFrameNanos {} }
                    holder = new
                    repeat(5) { withFrameNanos {} }
                    shown = field.text.toString()
                }
            }
        }
        paparazzi.gif(view, end = 1000, fps = 60)
        assertEquals("il campo mostra la bozza che parte", "nuovo", shown)
        assertEquals("vecchio", old.value)
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
