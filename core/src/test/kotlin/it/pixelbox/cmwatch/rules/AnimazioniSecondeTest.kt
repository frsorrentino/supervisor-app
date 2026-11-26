package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il secondo giro di animazioni scelto da Franz il 26/11 alle 12:49 (app-animazioni-seconde-26-11): le regole che non sono disegno.
 * Gli stessi casi di web/src/lib/animazioniSeconde.test.ts.
 */
class AnimazioniSecondeTest {
    @Test fun commonTimes() {
        assertEquals(5000L, Animazioni.UNDO_MS)
        assertEquals(3000L, Animazioni.DOCK_PEEK_MS)
        assertEquals(40, Animazioni.STAGGER_MS)
    }

    // 7: anello del limite.
    @Test fun limitRing() {
        assertEquals(Animazioni.Limit.OK, Animazioni.limit(100, 300))
        assertEquals(Animazioni.Limit.NEAR, Animazioni.limit(280, 300))
        assertEquals(Animazioni.Limit.NEAR, Animazioni.limit(300, 300))
        assertEquals(Animazioni.Limit.OVER, Animazioni.limit(301, 300))
        assertEquals(0.5f, Animazioni.fill(150, 300), 0.001f)
        assertEquals(1f, Animazioni.fill(400, 300), 0.001f)
    }

    // 5: i puntini attorno all'immagine di adesso.
    @Test fun dotsWindow() {
        assertEquals(0 until 5, Animazioni.dots(5, 2))
        assertEquals(0 until 7, Animazioni.dots(12, 1))
        assertEquals(3 until 10, Animazioni.dots(12, 6))
        assertEquals(5 until 12, Animazioni.dots(12, 11))
        assertTrue(Animazioni.dots(0, 0).isEmpty())
    }

    // 3: bordi della pila.
    @Test fun stack() {
        assertEquals(0, Animazioni.stackEdges(1))
        assertEquals(1, Animazioni.stackEdges(2))
        assertEquals(2, Animazioni.stackEdges(5))
        assertEquals(0, Animazioni.stackEdges(0))
    }

    // 6: il suggerimento in grigio solo a campo vuoto.
    @Test fun ghostSuggestion() {
        assertEquals("build della seconda proposta", Animazioni.ghost(listOf(" build della seconda proposta ", "porta le 8"), ""))
        assertNull(Animazioni.ghost(listOf("build"), "b"))
        assertNull(Animazioni.ghost(emptyList(), ""))
        assertEquals("porta le 8", Animazioni.ghost(listOf(" ", "porta le 8"), ""))
    }

    // 1: la pillola dei nuovi.
    @Test fun newMessagesPill() {
        assertTrue(Animazioni.newPill(atBottom = false, unseen = 1))
        assertFalse(Animazioni.newPill(atBottom = true, unseen = 3))
        assertFalse(Animazioni.newPill(atBottom = false, unseen = 0))
    }
}
