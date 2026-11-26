package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.WaitCard
import it.pixelbox.cmwatch.contract.WaitItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le otto animazioni scelte da Franz il 26/11 (app-animazioni-otto-26-11): le regole che non sono disegno. Gli stessi casi di
 * web/src/lib/animazioniOtto.test.ts.
 */
class AnimazioniOttoTest {
    @Test fun commonTimes() {
        assertEquals(1800L, Animazioni.COPY_HOLD_MS)
        assertEquals(1200L, Animazioni.HOLD_MS)
        assertEquals(1600, Animazioni.CYCLE_MS)
    }

    // 6: tenere premuto solo per la produzione.
    @Test fun holdOnlyForProduction() {
        assertTrue(Animazioni.needsHold(Approval(task = "t", title = "Release 5 su Play", deploy = true)))
        assertFalse(Animazioni.needsHold(Approval(task = "t", title = "Pre-release")))
        assertFalse(Animazioni.needsHold(null as Approval?))
        assertTrue(Animazioni.needsHold(WaitItem(id = "w", card = WaitCard(deploy = true))))
        assertFalse(Animazioni.needsHold(WaitItem(id = "w")))
    }

    // 4: la cifra scorre solo per un gesto di Franz.
    @Test fun digitRollsOnlyForAGesture() {
        assertTrue(Animazioni.rolls(3, 2, byGesture = true))
        assertFalse(Animazioni.rolls(3, 2, byGesture = false))
        assertFalse(Animazioni.rolls(2, 2, byGesture = true))
        assertFalse(Animazioni.rolls(null, 2, byGesture = true))
    }

    // 2: la parola detta, contata nel testo pulito e accesa nel testo mostrato.
    @Test fun wordFollowsTheVoice() {
        val shown = "I blocchi di **codice** veri restano annunciati"
        val spoken = "I blocchi di codice veri restano annunciati."
        val n = Animazioni.words(shown).size
        assertEquals(7, n)
        assertEquals(0, Animazioni.wordIndex(spoken, 0, 0, n))
        assertEquals(3, Animazioni.wordIndex(spoken, spoken.indexOf("codice"), 0, n))
        assertEquals(3, Animazioni.wordIndex(spoken, spoken.indexOf("codice") + 2, 0, n))
        assertEquals(6, Animazioni.wordIndex(spoken, spoken.length + 5, 0, n))
        // Il secondo pezzo dello stesso paragrafo parte dalle parole già lette.
        assertEquals(5, Animazioni.wordIndex("restano annunciati.", 0, 5, n))
        assertEquals(6, Animazioni.wordIndex("restano annunciati.", 0, 40, n))
        assertNull(Animazioni.wordIndex(spoken, 0, 0, 0))
        assertEquals(3, Animazioni.wordCount("uno due  tre"))
    }
}
