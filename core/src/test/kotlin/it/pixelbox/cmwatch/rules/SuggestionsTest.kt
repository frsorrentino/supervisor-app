package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.Suggestion
import org.junit.Assert.assertEquals
import org.junit.Test

/** Gli stessi casi di suggestions.test.ts (contratto 1.84, scheda A). */
class SuggestionsTest {
    private fun sg(id: String, at: Long, exp: Long? = null) = Suggestion(id, "cleanup", "k-$id", id, "p $id", at, exp)
    private fun st(xs: List<Suggestion>?) = State(v = 1, ts = 100, host = "pc", sessions = emptyList(), suggestions = xs)

    @Test fun threeAtMostNewestFirst() {
        val v = Suggestions.visible(st(listOf(sg("a", 10), sg("b", 40), sg("c", 30), sg("d", 20), sg("e", 50, 90), sg("f", 60))), setOf("f"), 100)
        assertEquals(listOf("b", "c", "d"), v.shown.map { it.id })
        assertEquals(4, v.total)
        assertEquals(Suggestions.Visible(emptyList(), 0), Suggestions.visible(st(null), emptySet(), 100))
    }
}
