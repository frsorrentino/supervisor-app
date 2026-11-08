package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.snapshots.Snapshot
import it.pixelbox.cmwatch.mobile.ui.PageBack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * La pagina nera (app-ti-aspetta-nera-08-11, Franz 08/11 13:52: «si apre una finestra che rimane nera, nel momento in cui la
 * chiudo si visualizza il suo contenuto»). Chiusa una pagina col gesto Indietro, la pagina che esce cancella la coroutine del
 * gesto prima dei due fotogrammi d'attesa: lo stato del gesto deve tornare a zero lo stesso, o la pagina aperta dopo entra
 * con `leaving` a 1, a trasparenza zero.
 */
class PageBackTest {
    /**
     * Ogni prova in uno snapshot suo, buttato alla fine, come HomeFoldsTest: `PageBack` scritto fuori da una composizione nello
     * snapshot globale della JVM rompeva le gif di Paparazzi delle classi dopo (SlotSubitoTest, SuggFocusTest). `runBlocking`
     * resta sul filo dello snapshot.
     */
    private fun isolated(body: suspend CoroutineScope.() -> Unit) {
        val s = Snapshot.takeMutableSnapshot()
        try { s.enter { runBlocking { body() } } } finally { s.dispose() }
    }

    private fun gestureDone() { PageBack.progress.floatValue = 0.4f; PageBack.leaving.floatValue = 1f; PageBack.commit() }

    private fun assertClean() {
        assertEquals(0f, PageBack.leaving.floatValue)
        assertEquals(0f, PageBack.progress.floatValue)
        assertFalse(PageBack.takeCommitted())
    }

    @Test fun pageThatLeavesCancelsTheWaitAndTheGestureStillResets() = isolated {
        gestureDone()
        val waiting = CompletableDeferred<Unit>()
        val job = launch { PageBack.settle { waiting.complete(Unit); awaitCancellation() } }
        waiting.await()
        job.cancelAndJoin()
        assertClean()
    }

    @Test fun pageThatStaysResetsAfterTheFrames() = isolated {
        gestureDone()
        PageBack.settle { }
        assertClean()
    }
}
