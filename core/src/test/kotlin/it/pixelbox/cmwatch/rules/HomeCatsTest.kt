package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La home per categorie (compito app-home-categorie-02-11); gli stessi casi in homeCats.test.ts. */
class HomeCatsTest {
    private fun ids(c: List<HomeCats.Cat>) = c.map { it.id }

    @Test fun theDefaultHasFourCategoriesTwoOpen() {
        val d = HomeCats.decode(null)
        assertEquals(listOf("you", "work", "out", "tools"), ids(d))
        assertEquals(listOf("prima_raccolta", "decisioni", "prove", "suggestions"), d[0].sections)
        assertEquals(listOf("sessions", "seguiti", "lavori"), d[1].sections)
        assertEquals(listOf("esiti", "da_guardare", "rendiconti"), d[2].sections)
        assertEquals(listOf("usage", "pagella", "system"), d[3].sections)
        assertEquals(listOf(true, true, false, false), d.map { it.open })
        assertEquals(HomeCats.DEFAULT, HomeCats.decode(emptyList()))
    }

    @Test fun theDefaultIsSavedAsNull() {
        assertNull(HomeCats.encode(HomeCats.DEFAULT, null))
    }

    @Test fun aSavedOrderComesBackTheSame() {
        val cats = HomeCats.toggle(HomeCats.moveCat(HomeCats.DEFAULT, 1, 0), "you")
        val saved = HomeCats.encode(cats, "usage")!!
        assertEquals(cats, HomeCats.decode(saved))
        assertEquals("usage", HomeOrder.pinned(saved))
        assertEquals("shut:you,out,tools", saved.single { it.startsWith("shut:") })
    }

    @Test fun nothingClosedIsSavedToo() {
        val all = HomeCats.DEFAULT.map { it.copy(open = true) }
        assertEquals(all, HomeCats.decode(HomeCats.encode(all, null)))
    }

    @Test fun yesterdaysFlatOrderFallsIntoCategories() {
        val d = HomeCats.decode(listOf("sessions", "closed", "usage", "prove", "plugins", "pin:usage"))
        assertEquals(listOf("work", "tools", "you", "out"), ids(d))
        assertEquals(listOf("prove", "prima_raccolta", "decisioni", "suggestions"), d.single { it.id == "you" }.sections)
        assertTrue(d.flatMap { it.sections }.none { it == "closed" || it == "plugins" })
        assertEquals(listOf(true, false, true, false), d.map { it.open })
    }

    @Test fun theSavedListFitsTheRelay() {
        // Contratto 1.97: al massimo 16 chiavi, ognuna al massimo 32 caratteri.
        val worst = HomeCats.DEFAULT.reversed().map { it.copy(open = false, sections = it.sections.reversed()) }
        val saved = HomeCats.encode(worst, "prima_raccolta")!!
        assertTrue(saved.size <= 16); assertTrue(saved.all { it.length <= 32 })
    }

    @Test fun aSectionNeverLeavesItsCategory() {
        assertEquals(HomeCats.DEFAULT, HomeCats.moveSection(HomeCats.DEFAULT, "you", 1, 9))
        assertEquals(listOf("prove", "prima_raccolta", "decisioni", "suggestions"), HomeCats.moveSection(HomeCats.DEFAULT, "you", 2, 0)[0].sections)
        assertEquals("out", HomeCats.catOf("rendiconti")); assertNull(HomeCats.catOf("closed"))
    }
}
