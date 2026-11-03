package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Il riassunto a destra su una riga (app-riassunti-una-riga-03-11); gli stessi casi in oneLine.test.ts. */
class OneLineTest {
    // Ogni carattere largo 1: la larghezza di k parti è quella del testo unito coi separatori.
    private val p = listOf("22 da decidere", "50 da provare", "5 suggerimenti")
    private fun w(k: Int) = p.take(k).joinToString(OneLine.SEP).length.toFloat()

    @Test fun allPartsWhenTheyFit() = assertEquals(3, OneLine.count(p.size, 100f, ::w))

    @Test fun thePartsFallWholeFromTheLast() {
        assertEquals(2, OneLine.count(p.size, w(2), ::w))
        assertEquals(2, OneLine.count(p.size, w(3) - 1, ::w))
        assertEquals(1, OneLine.count(p.size, w(2) - 1, ::w))
    }

    @Test fun zeroWhenNotEvenTheFirstFits() = assertEquals(0, OneLine.count(p.size, w(1) - 1, ::w))

    @Test fun nothingToFit() = assertEquals(0, OneLine.count(0, 100f, ::w))

    @Test fun theFirstPartShrinksToItsNumber() {
        assertEquals("22", OneLine.short("22 da decidere"))
        assertEquals("0,2", OneLine.short("carico 0,2"))
        assertEquals("47%", OneLine.short("47%"))
        assertEquals("3", OneLine.short("3 ti aspettano"))
        assertEquals("2", OneLine.short("Da decidere 2"))
        // Senza un numero resta la parte intera: la riga la taglia, ma una parola non va a capo.
        assertEquals("niente oggi", OneLine.short("niente oggi"))
    }

    @Test fun theSummaryIsSplitOnTheDots() {
        assertEquals(listOf("1 al lavoro", "1 finita"), OneLine.parts("1 al lavoro · 1 finita"))
        assertEquals(listOf("30 oggi"), OneLine.parts("30 oggi"))
        assertEquals(emptyList<String>(), OneLine.parts(" "))
    }
}
