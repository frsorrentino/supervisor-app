package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Un blocco ```text è testo da copiare (una bozza di email): la voce lo legge per intero, non dice «Codice» (Franz,
 * 25/11 22:53: «il testo da copiare viene letto dalla voce come "codice"»). Il codice vero, o senza lingua, si annuncia.
 */
class TestoDaCopiareVoceTest {
    private val bozza = "Ecco la bozza:\n\n```text\nBuongiorno,\nvi scrivo per il preventivo.\n1. Due licenze\n2. Il rinnovo\nGrazie\n```\n\nFatto."

    @Test fun textBlockIsReadInFull() {
        val t = SpeechText.forPhone(bozza, "Codice", "Esito")
        assertEquals("Ecco la bozza:\nBuongiorno.\nvi scrivo per il preventivo.\n1. Due licenze.\n2. Il rinnovo.\nGrazie.\nFatto.", t)
        assertFalse(t.contains("Codice"))
    }

    @Test fun textBlockIsItsOwnKindAndKeepsItsLines() {
        val b = AnswerText.blocks(bozza)
        assertEquals(listOf(AnswerText.Kind.PARA, AnswerText.Kind.TEXT, AnswerText.Kind.PARA), b.map { it.kind })
        assertEquals("Buongiorno,\nvi scrivo per il preventivo.\n1. Due licenze\n2. Il rinnovo\nGrazie", b[1].text)
        val voce = SpeechText.forBlock(b[1].kind, b[1].text, "Codice")
        assertEquals("Buongiorno.\nvi scrivo per il preventivo.\n1. Due licenze.\n2. Il rinnovo.\nGrazie.", voce)
    }

    @Test fun codeAndBareBlocksStayAnnounced() {
        val t = "Patch:\n```kotlin\nval x = 1\n```\nSenza lingua:\n```\nrm -rf build\n```"
        val v = SpeechText.clean(t, "Codice")
        assertEquals("Patch:\nCodice.\nSenza lingua:\nCodice.", v)
        assertEquals(listOf(AnswerText.Kind.CODE, AnswerText.Kind.CODE), AnswerText.blocks(t).filter { it.kind != AnswerText.Kind.PARA }.map { it.kind })
        assertEquals("Codice.", SpeechText.forBlock(AnswerText.Kind.CODE, "val x = 1", "Codice"))
    }

    @Test fun copyTextLanguages() {
        assertTrue(AnswerText.isCopyText("```text"))
        assertTrue(AnswerText.isCopyText("```TXT "))
        assertTrue(AnswerText.isCopyText("```plaintext"))
        assertFalse(AnswerText.isCopyText("```"))
        assertFalse(AnswerText.isCopyText("```bash"))
    }
}
