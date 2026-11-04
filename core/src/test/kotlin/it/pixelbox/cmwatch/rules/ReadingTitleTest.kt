package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Il titolo della lettura nel player delle notifiche (app-player-lettura-03-11). */
class ReadingTitleTest {
    @Test fun firstSentenceOfTheFirstLine() {
        assertEquals("Il rilascio 2.4 è pronto.", ReadingTitle.of("Il rilascio 2.4 è pronto. La suite è verde.\nAltro."))
    }

    @Test fun markdownMarksGo() {
        assertEquals("Esito: release candidate tagged", ReadingTitle.of("\n## **Esito**: release `candidate` tagged"))
        assertEquals("vedi il piano", ReadingTitle.of("- vedi [il piano](https://x.y/z)"))
    }

    @Test fun longSentenceCutAtAWordWithoutEllipsis() {
        val t = ReadingTitle.of("Seguito di ledger-api il controllo delle diciotto ha trovato tre file cambiati senza test e la pipeline del deploy non è partita")
        assertTrue(t.length <= ReadingTitle.MAX)
        assertFalse(t.endsWith(" "))
        assertFalse(t.contains("…"))
        assertTrue("Seguito di ledger-api il controllo delle diciotto ha trovato tre file cambiati senza test e la pipeline".startsWith(t))
    }

    @Test fun emptyTextEmptyTitle() = assertEquals("", ReadingTitle.of("  \n "))
}
