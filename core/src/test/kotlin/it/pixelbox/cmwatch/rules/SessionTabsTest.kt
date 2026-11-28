package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.IconRowRules.Tone
import it.pixelbox.cmwatch.rules.SessionTabs.Counts
import it.pixelbox.cmwatch.rules.SessionTabs.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La testata della sessione con la riga di icone e la tendina (app-posizioni-sistema-27-11, gruppo 2). Gli stessi casi di
 * web/src/lib/sessionTabs.test.ts.
 */
class SessionTabsTest {
    @Test fun `le schede sono cinque e stanno sempre nello stesso ordine`() {
        assertEquals(listOf("chat", "perte", "risultati", "modifiche", "dettagli"), SessionTabs.ORDER.map { it.id })
        assertEquals(Key.MODIFICHE, SessionTabs.keyOf("modifiche"))
        assertNull(SessionTabs.keyOf("altro"))
    }

    @Test fun `Per te conta le voci che aspettano e gli avvisi, Risultati i nuovi, Modifiche i file`() {
        assertEquals(Counts(perTe = 4, risultati = 1, modifiche = 4), SessionTabs.counts(3, 1, 1, 4))
        assertEquals(Counts(perTe = 2, risultati = 0, modifiche = 0), SessionTabs.counts(0, 2, 0, null))
        assertEquals(Counts(perTe = 0, risultati = 0, modifiche = 0), SessionTabs.counts(0, 0, 0, null))
    }

    @Test fun `i numeri non sono mai negativi`() {
        assertEquals(Counts(0, 0, 0), SessionTabs.counts(-1, -3, -2, -5))
    }

    @Test fun `gli avvisi sono quelli della pillola Avvisi, la sessione --bg e la nota di Claude Code`() {
        assertEquals(0, SessionTabs.notes(bg = false, headsUp = false))
        assertEquals(1, SessionTabs.notes(bg = true, headsUp = false))
        assertEquals(1, SessionTabs.notes(bg = false, headsUp = true))
        assertEquals(2, SessionTabs.notes(bg = true, headsUp = true))
    }

    @Test fun `un risultato è nuovo se la sua ultima versione non è vista`() {
        assertEquals(2, SessionTabs.newResults(listOf(true, false, true, false)))
        assertEquals(0, SessionTabs.newResults(emptyList()))
    }

    @Test fun `il colore del numero dice lo stato, ambra Per te, verde Risultati, grigio Modifiche`() {
        assertEquals(Tone.WAIT, SessionTabs.tone(Key.PERTE))
        assertEquals(Tone.NEW, SessionTabs.tone(Key.RISULTATI))
        assertEquals(Tone.COUNT, SessionTabs.tone(Key.MODIFICHE))
    }

    @Test fun `Chat e Dettagli non hanno numero`() {
        val c = SessionTabs.counts(3, 1, 2, 4)
        assertEquals(0, SessionTabs.count(Key.CHAT, c))
        assertEquals(0, SessionTabs.count(Key.DETTAGLI, c))
        assertEquals(4, SessionTabs.count(Key.PERTE, c))
        assertEquals(2, SessionTabs.count(Key.RISULTATI, c))
        assertEquals(4, SessionTabs.count(Key.MODIFICHE, c))
    }

    @Test fun `un tocco su una scheda apre la tendina, sulla scheda aperta o su Chat la chiude`() {
        assertEquals(Key.PERTE, SessionTabs.pick(Key.CHAT, "perte"))
        assertEquals(Key.CHAT, SessionTabs.pick(Key.PERTE, "perte"))
        assertEquals(Key.CHAT, SessionTabs.pick(Key.PERTE, "chat"))
        assertEquals(Key.CHAT, SessionTabs.pick(Key.CHAT, "chat"))
    }

    @Test fun `una scheda diversa prende il posto di quella aperta`() {
        assertEquals(Key.DETTAGLI, SessionTabs.pick(Key.PERTE, "dettagli"))
        assertEquals(Key.RISULTATI, SessionTabs.pick(Key.MODIFICHE, "risultati"))
    }

    @Test fun `una chiave sconosciuta lascia la scheda aperta`() {
        assertEquals(Key.DETTAGLI, SessionTabs.pick(Key.DETTAGLI, "altro"))
    }

    @Test fun `la tendina è aperta su ogni scheda tranne Chat`() {
        assertFalse(SessionTabs.shadeOpen(Key.CHAT))
        SessionTabs.ORDER.drop(1).forEach { assertTrue(SessionTabs.shadeOpen(it)) }
    }

    @Test fun `i tempi sono quelli del centro notifiche, 360 ms e 32 ms fra le righe`() {
        assertEquals(360, SessionTabs.SHADE_MS)
        assertEquals(32, SessionTabs.ROW_MS)
        assertEquals(0, SessionTabs.rowDelay(0))
        assertEquals(32, SessionTabs.rowDelay(1))
        assertEquals(96, SessionTabs.rowDelay(3))
    }

    @Test fun `dopo otto righe il ritardo non cresce più`() {
        assertEquals(256, SessionTabs.rowDelay(SessionTabs.MAX_STAGGER))
        assertEquals(256, SessionTabs.rowDelay(SessionTabs.MAX_STAGGER + 5))
        assertEquals(0, SessionTabs.rowDelay(-2))
    }

    @Test fun `col dito la tendina sale soltanto, mai oltre la sua altezza`() {
        assertEquals(0f, SessionTabs.dragOffset(40f, 500f), 0f)
        assertEquals(-120f, SessionTabs.dragOffset(-120f, 500f), 0f)
        assertEquals(-500f, SessionTabs.dragOffset(-900f, 500f), 0f)
    }

    @Test fun `tirata su oltre un quarto della sua altezza, o con uno slancio, si chiude`() {
        assertTrue(SessionTabs.closesOnDrag(-125f, 0f, 500f))
        assertFalse(SessionTabs.closesOnDrag(-100f, 0f, 500f))
        assertTrue(SessionTabs.closesOnDrag(-30f, -900f, 500f))
        assertFalse(SessionTabs.closesOnDrag(-30f, -500f, 500f))
    }

    @Test fun `tirata in giù non si chiude mai`() {
        assertFalse(SessionTabs.closesOnDrag(0f, -2000f, 500f))
        assertFalse(SessionTabs.closesOnDrag(80f, -2000f, 500f))
    }
}
