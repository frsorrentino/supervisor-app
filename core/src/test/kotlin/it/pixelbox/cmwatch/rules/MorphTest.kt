package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// La scheda che diventa pagina o colonna (app-volo-titolo-telefono-05-11).
class MorphTest {
    // In nessun momento i due contenuti si vedono tutti e due: sovrapposizione 0.
    @Test fun neverTwoTexts() {
        for (ms in 0..400) assertEquals("a $ms ms", 0f, minOf(Morph.outAlpha(ms), Morph.inAlpha(ms)), 0f)
    }

    // In nessun momento il riquadro è vuoto (app-morph-senza-stacco-06-11): vuoto 0, quello che entra è pieno dal primo fotogramma.
    @Test fun neverEmpty() {
        for (ms in 0..400) assertEquals("a $ms ms", 1f, maxOf(Morph.outAlpha(ms), Morph.inAlpha(ms)), 0f)
        assertEquals(1f, Morph.inAlpha(0), 0f)
        assertEquals(0f, Morph.outAlpha(0), 0f)
    }

    // Il collo è breve e basso: il volo parte subito.
    @Test fun neckIsShortAndLow() {
        assertTrue(Morph.NECK_PRESS_MS < Morph.NECK_MS)
        assertTrue(Morph.NECK_MS <= 140)
        assertTrue(Morph.NECK_LIFT_DP <= 10f)
    }
}
