package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In basso nella sessione (app-posizioni-sistema-27-11 gruppo 3): la riga delle Azioni, il chip del contesto e l'anellino nel campo.
 * Gli stessi casi di web/src/lib/sessionBottom.test.ts.
 */
class SessionBottomTest {
    @Test fun `l'anellino mostra la percentuale del contesto e senza contesto non c'è`() {
        assertEquals(18, SessionBottom.ringPct(18))
        assertEquals(72, SessionBottom.ringPct(72))
        assertEquals(104, SessionBottom.ringPct(104))
        assertEquals(0, SessionBottom.ringPct(-3))
        assertNull(SessionBottom.ringPct(null))
    }

    @Test fun `l'arco dell'anellino resta fra vuoto e pieno`() {
        assertEquals(0.18f, SessionBottom.ringFraction(18), 0.0001f)
        assertEquals(0f, SessionBottom.ringFraction(0), 0.0001f)
        assertEquals(1f, SessionBottom.ringFraction(104), 0.0001f)
        assertEquals(0f, SessionBottom.ringFraction(-5), 0.0001f)
    }

    @Test fun `dal 70 per cento l'anellino è ambra`() {
        assertEquals(70, SessionBottom.CHIP_FROM)
        assertFalse(SessionBottom.ringHigh(18))
        assertFalse(SessionBottom.ringHigh(69))
        assertTrue(SessionBottom.ringHigh(70))
        assertTrue(SessionBottom.ringHigh(72))
        assertFalse(SessionBottom.ringHigh(null))
    }

    @Test fun `il chip del contesto c'è dal 70 per cento, a sessione ferma e con un comando di handoff`() {
        assertEquals(72, SessionBottom.ctxChip(72, idle = true, canHandoff = true))
        assertEquals(70, SessionBottom.ctxChip(70, idle = true, canHandoff = true))
        assertNull(SessionBottom.ctxChip(69, idle = true, canHandoff = true))
        assertNull(SessionBottom.ctxChip(72, idle = false, canHandoff = true))
        assertNull(SessionBottom.ctxChip(72, idle = true, canHandoff = false))
        assertNull(SessionBottom.ctxChip(null, idle = true, canHandoff = true))
    }

    @Test fun `la riga delle Azioni c'è con un'azione o col chip, solo a campo vuoto`() {
        assertTrue(SessionBottom.rowShown(2, chip = false, draft = ""))
        assertTrue(SessionBottom.rowShown(0, chip = true, draft = ""))
        assertTrue(SessionBottom.rowShown(1, chip = true, draft = "  \n"))
        assertFalse(SessionBottom.rowShown(0, chip = false, draft = ""))
        assertFalse(SessionBottom.rowShown(2, chip = true, draft = "scegli"))
        assertFalse(SessionBottom.rowShown(2, chip = false, draft = "a"))
    }
}
