package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Feedback dal menù (app-feedback-menu-04-11): gli stessi casi di web/src/lib/feedback.test.ts.
class FeedbackTest {
    private val labels = Feedback.Labels("Suggerimento", "Bug", "Dati tecnici", "Foto della pagina allegata")
    private val tech = Feedback.Tech("0.2.1890", "abc1234", "Google Pixel 10 · Android 16 · 412×915 dp", "schermata: Lavori", "lingua: it")

    @Test fun theDraftIsAnonymous() = assertEquals(
        "scrivi a <email>, file in C:\\Users\\<utente>\\x e /home/<utente>/y, token <chiave> da <ip>",
        Feedback.anonymize("scrivi a fr.x@gmail.com, file in C:\\Users\\Utente\\x e /home/franz/y, token ghp_abcdefghijklmnopqrstuvwxyz0123 da 192.168.1.20"),
    )

    @Test fun theTitleIsTheFirstLineCutAtAWord() {
        assertEquals("La home sbiadisce", Feedback.titleOf("La home sbiadisce\naltro"))
        val t = Feedback.titleOf("parola ".repeat(20))
        assertTrue(t.length <= 72)
        assertTrue(t.endsWith("parola"))
    }

    @Test fun theDraftCarriesKindTextTechAndThePhotoOnlyWhenOn() {
        val d = Feedback.draft(Feedback.Kind.BUG, "Il tasto non risponde\nsu Lavori", tech, false, labels)
        assertEquals("[Bug] Il tasto non risponde", d.title)
        assertTrue("- app: 0.2.1890 (abc1234)" in d.body)
        assertTrue("schermata: Lavori" in d.body)
        assertFalse("Foto" in d.body)
        assertTrue("_Foto della pagina allegata_" in Feedback.draft(Feedback.Kind.IDEA, "x", tech, true, labels).body)
    }
}
