package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Le novità della scheda dell'aggiornamento (app-changelog-aggiornamento-26-11, Franz 26/11 14:00). */
class NovitaTest {
    private fun rel(tag: String, at: Long, notes: String) = AppUpdate.Release(tag, "0.2.$at · tab-emoji/22-10 · $tag", at, true, "u", 1, null, notes)
    private fun body(vararg lines: String) = "## Novità\n" + lines.joinToString("\n") { "- $it" } + "\n\n- fix(phone): commit subject\n"

    @Test fun sectionReadsOnlyTheNovitaList() {
        assertEquals(listOf("La copia risponde", "La voce segue la parola"), Novita.section(body("La copia risponde", "La voce segue la parola")))
        assertEquals(emptyList<String>(), Novita.section("- fix(phone): only commit subjects\n- test: goldens"))
        assertEquals(emptyList<String>(), Novita.section(""))
    }

    @Test fun sumsTheSkippedPreReleasesNewestFirstWithoutDuplicates() {
        val a = rel("a", 100, body("Copia che risponde", "Anello del tabellone"))
        val b = rel("b", 200, "- only subjects")
        val c = rel("c", 300, body("Annulla dopo 5 secondi", "Copia che risponde"))
        val got = Novita.lines(listOf(c, b, a), installedAt = 50, offer = c)
        assertEquals(listOf("Annulla dopo 5 secondi", "Copia che risponde", "Anello del tabellone"), got.lines)
        assertEquals(0, got.more)
        // Le pre-release già installate non contano.
        assertEquals(listOf("Annulla dopo 5 secondi", "Copia che risponde"), Novita.lines(listOf(c, b, a), installedAt = 150, offer = c).lines)
    }

    @Test fun capsAtSixLines() {
        val r = rel("x", 10, body("1", "2", "3", "4", "5", "6", "7", "8"))
        val got = Novita.lines(listOf(r), installedAt = 0, offer = r)
        assertEquals(listOf("1", "2", "3", "4", "5"), got.lines)
        assertEquals(3, got.more)
    }

    @Test fun nothingWritten() {
        val r = rel("x", 10, "- fix(phone): subject")
        assertEquals(Novita.Lines(emptyList(), 0), Novita.lines(listOf(r), installedAt = 0, offer = r))
    }
}
