package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// La scheda che diventa pagina o colonna (app-volo-titolo-telefono-05-11).
class MorphTest {
    // In nessun momento i due contenuti si vedono tutti e due: quando uno è visibile l'altro è quasi trasparente.
    @Test fun neverTwoFullTexts() {
        for (ms in 0..400) assertTrue("a $ms ms", minOf(Morph.outAlpha(ms), Morph.inAlpha(ms)) < 0.05f)
    }

    @Test fun oldLeavesFirstNewArrivesAfter() {
        assertEquals(1f, Morph.outAlpha(0), 0f)
        assertEquals(0f, Morph.outAlpha(Morph.OUT_MS), 0f)
        assertEquals(0f, Morph.inAlpha(Morph.IN_DELAY_MS), 0f)
        assertEquals(1f, Morph.inAlpha(Morph.IN_DELAY_MS + Morph.IN_MS), 0f)
    }
}
