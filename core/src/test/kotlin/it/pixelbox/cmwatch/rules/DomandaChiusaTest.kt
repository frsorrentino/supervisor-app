package it.pixelbox.cmwatch.rules

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** app-domanda-chiusa-25-11: una domanda chiusa, risposta o sostituita non è un errore; gli errori veri sì. */
class DomandaChiusaTest {
    @Test fun ilRifiutoDelContrattoEUnaDomandaChiusa() {
        val text = File("../contract/cmd-result-sample.json").readText()
        assertTrue(text.contains("this question is no longer waiting"))
        assertTrue(WaitGone.gone("w0000000000: this question is no longer waiting (closed or already answered)"))
    }

    @Test fun ilTestoItalianoDelRelayAncheVaInSilenzio() {
        // Il banner di Franz alle 16:55, sul link vecchio di una carta di wesud sostituita alle 16:05.
        assertTrue(WaitGone.gone("waf1a079167: questa domanda non aspetta più (chiusa o già risposta)"))
    }

    @Test fun gliErroriVeriRestanoErrori() {
        assertFalse(WaitGone.gone("offline"))
        assertFalse(WaitGone.gone("permission denied: device not paired"))
        assertFalse(WaitGone.gone("unknown op: attesa"))
        assertFalse(WaitGone.gone(null))
    }
}
