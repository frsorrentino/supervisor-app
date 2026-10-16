package it.pixelbox.cmwatch.mobile

import it.pixelbox.cmwatch.contract.ChangesCount
import it.pixelbox.cmwatch.rules.DiffRules
import org.junit.Assert.*
import org.junit.Test

class ChangesMenuTest {
    // Franz, 16/10 13:08: «l'header su 2 righe non va bene, da mobile spostare diff in menu». Sotto 600 dp la pillola diventa
    // la prima voce del menu ⋮, con un puntino sul ⋮; tablet e finestre larghe la tengono.
    @Test fun `Modifiche nel menu sul telefono, pillola sul tablet`() {
        assertTrue(DiffRules.inMenu(411))
        assertTrue(DiffRules.inMenu(599))
        assertFalse(DiffRules.inMenu(600))
        assertFalse(DiffRules.inMenu(1280))
        assertEquals("Modifiche · 22 file +2440 \u22129", DiffRules.menuLine("Modifiche", "22 file", ChangesCount(22, 2440, 9, 0)))
    }
}
