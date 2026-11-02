package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.HomeRight.Bit
import it.pixelbox.cmwatch.rules.HomeRight.Part
import org.junit.Assert.assertEquals
import org.junit.Test

/** Gli stessi casi di homeRight.test.ts. */
class HomeRightTest {
    @Test fun voci_nuove_testo_soloQuelloCheIlNumeroNonDice() {
        assertEquals(listOf(Bit(Part.BUILDS, 2), Bit(Part.KO, 1)), HomeRight.prove(ProveSummary(n = 5, builds = 2, red = 0, ko = 1)))
        assertEquals(listOf(Bit(Part.BUILDS, 1)), HomeRight.prove(ProveSummary(n = 3, builds = 1)))
        assertEquals(listOf(Bit(Part.HERE, 1), Bit(Part.UNBLOCKS, 3)), HomeRight.decisions(DecisionsSummary(n = 2, here = 1, tasks = 3)))
        assertEquals(listOf(Bit(Part.UNBLOCKS, 3)), HomeRight.decisions(DecisionsSummary(n = 2, here = 0, tasks = 3)))
        assertEquals(listOf(Bit(Part.BEHIND, 2), Bit(Part.REMINDERS, 1)), HomeRight.seguiti(SeguitiSummary(2, 0, 1)))
        assertEquals("ask", HomeRight.prima(PrimaRaccoltaSummary("ask", 7, 8, true, null)))
    }
}
