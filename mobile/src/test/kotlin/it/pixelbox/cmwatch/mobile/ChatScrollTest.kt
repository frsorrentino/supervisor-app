package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.mobile.ui.scrollToEnd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Il salto della chat in cima (video di Franz, 10/10 22:19): scesa in fondo, con l'ultima voce più alta dello schermo,
 * la chat mostrava per mezzo secondo i primi messaggi e poi tornava giù. Paparazzi qui fa solo da motore dei
 * fotogrammi, senza immagini di riferimento: si conta se la prima voce viene posata.
 */
class ChatScrollTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, snapshotHandler = silent(SnapshotHandler::class.java))

    // Sotto le transizioni condivise della home (un LookaheadScope) la testata che rientra stringe la chat a ogni
    // fotogramma, e a ogni fotogramma la chat torna in fondo.
    @Test fun endOfATallLastMessageNeverShowsTheTop() {
        val (topPlaced, atEnd) = scroll(animate = false)
        assertTrue("non arriva in fondo", atEnd)
        assertEquals("la prima voce posata mentre la chat scende in fondo", 0, topPlaced)
    }

    // «Torna all'ultimo messaggio»: dal punto in cui si rilegge, giù animato fino alla fine dell'ultima voce.
    @Test fun backToTheLatestNeverShowsTheTop() {
        val (topPlaced, atEnd) = scroll(animate = true)
        assertTrue("non arriva in fondo", atEnd)
        assertEquals("la prima voce posata mentre la chat torna in fondo", 0, topPlaced)
    }

    private fun scroll(animate: Boolean): Pair<Int, Boolean> {
        var topPlaced = 0
        var atEnd = false
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                LookaheadScope {
                    // Si parte già lontano dalla cima: la prima voce non ha motivo di comparire.
                    val list = rememberLazyListState(initialFirstVisibleItemIndex = if (animate) 10 else 30)
                    var header by remember { mutableIntStateOf(0) }
                    LaunchedEffect(Unit) {
                        if (animate) {
                            withFrameNanos {}
                            list.scrollToEnd(animate = true)
                        } else repeat(12) {
                            withFrameNanos {}
                            header += 10
                            withFrameNanos {}
                            list.scrollToEnd()
                        }
                        repeat(60) { withFrameNanos {} }
                        atEnd = !list.canScrollForward
                    }
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().height(header.dp))
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(), list, PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(30) { i -> Box(Modifier.fillMaxWidth().height(60.dp).then(if (i == 0) Modifier.onPlaced { topPlaced++ } else Modifier)) }
                            item { Box(Modifier.fillMaxWidth().height(2000.dp)) }
                        }
                    }
                }
            }
        }
        paparazzi.gif(view, end = 3000, fps = 60)
        return topPlaced to atEnd
    }

    // Fotogrammi buttati: un `SnapshotHandler` scritto qui nominerebbe `BufferedImage`, che il classpath dei test Android
    // non vede.
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
