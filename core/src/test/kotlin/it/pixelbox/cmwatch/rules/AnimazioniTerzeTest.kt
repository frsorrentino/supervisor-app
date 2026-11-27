package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Il terzo giro di animazioni (app-animazioni-terzo-c-26-11), dopo la prova 1391: la barra dei paragrafi. Gli stessi casi di
 * web/src/lib/animazioniTerze.test.ts.
 */
class AnimazioniTerzeTest {
    // Il punto sta all'inizio del paragrafo che la voce legge.
    @Test fun scrubRestsAtTheStartOfTheParagraph() {
        assertEquals(0f, Animazioni.scrubAt(0, 4), 1e-6f)
        assertEquals(0.5f, Animazioni.scrubAt(2, 4), 1e-6f)
        assertEquals(0.75f, Animazioni.scrubAt(9, 4), 1e-6f)
        assertEquals(0f, Animazioni.scrubAt(0, 1), 1e-6f)
    }

    // Al rilascio si aggancia al paragrafo che comincia più vicino: il dito a metà fra due segni va a quello dopo.
    @Test fun scrubSnapsToTheNearestStart() {
        assertEquals(0, Animazioni.scrubNearest(0.1f, 4))
        assertEquals(1, Animazioni.scrubNearest(0.2f, 4))
        assertEquals(1, Animazioni.scrubNearest(0.125f, 4))
        assertEquals(2, Animazioni.scrubNearest(0.6f, 4))
        assertEquals(3, Animazioni.scrubNearest(1f, 4))
        assertEquals(0, Animazioni.scrubNearest(-0.3f, 4))
        assertEquals(0, Animazioni.scrubNearest(0.9f, 1))
        // Il punto agganciato è l'inizio di quel paragrafo.
        for (n in 2..9) for (k in 0 until n) assertEquals(k, Animazioni.scrubNearest(Animazioni.scrubAt(k, n), n))
    }
}
