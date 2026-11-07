package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.snapshots.Snapshot
import it.pixelbox.cmwatch.mobile.ui.QuoteDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le citazioni sopra il campo (app-citazione-trascrizione-07-11): «Cita» e «Cita tutto» aggiungono, la × toglie, l'invio manda il
 * Markdown e svuota. Ogni prova in uno snapshot suo, buttato alla fine, come HomeFoldsTest: niente stato nello snapshot globale.
 */
class QuoteDraftTest {
    private fun isolated(body: () -> Unit) {
        val s = Snapshot.takeMutableSnapshot()
        try { s.enter(body) } finally { s.dispose() }
    }

    @Test fun quoteRemoveAndSend() = isolated {
        val d = QuoteDraft()
        d.add("Il build è verde.", 100)
        d.add("Manca la nota di rilascio.", 200)
        d.add("   ", 300)
        assertEquals(2, d.items.size)
        d.remove(d.items[1].id)
        assertEquals("> Il build è verde.\n\nBene, procedi", d.take("Bene, procedi"))
        assertTrue(d.items.isEmpty())
    }

    @Test fun withoutQuotesTheReplyIsUnchanged() = isolated {
        assertEquals("ciao", QuoteDraft().take("  ciao "))
    }

    @Test fun aLongPieceIsCutWhenQuoted() = isolated {
        val d = QuoteDraft()
        d.add("x".repeat(1500), null)
        assertEquals(1001, d.items.single().text.length)
        assertTrue(d.items.single().text.endsWith("…"))
    }
}
