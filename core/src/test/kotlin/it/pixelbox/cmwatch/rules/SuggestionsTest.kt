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

    @Test fun knownKinds() {
        assertEquals(10, Suggestions.KINDS.size)
        assertEquals(true, Suggestions.known("unsaved"))
        assertEquals(false, Suggestions.known("zzz"))
    }

    // Contratto 1.88: la coda per rank (assente o fuori 0-9 = 5), poi dalla più recente; al massimo due dello stesso tipo.
    private fun q(id: String, rank: Int?, at: Long, kind: String = "cleanup", options: List<String>? = null) = Suggestion(id, kind, "k-$id", id, at = at, rank = rank, options = options)

    @Test fun rankBeforeAt() {
        val v = Suggestions.visible(st(listOf(q("a", 6, 50), q("b", 2, 10, "idea"), q("c", 4, 40, "unsaved"), q("d", null, 60, "waiting"))), emptySet(), 0)
        assertEquals(listOf("b", "c", "d"), v.shown.map { it.id })
        assertEquals(4, v.total)
    }

    @Test fun missingRankIsFive() {
        val v = Suggestions.visible(st(listOf(q("a", 6, 90, "idea"), q("x", 12, 80, "waiting"), q("d", null, 60, "unsaved"), q("c", 4, 10, "choice"))), emptySet(), 0)
        assertEquals(listOf("c", "x", "d"), v.shown.map { it.id })
    }

    @Test fun atMostTwoOfAKind() {
        val v = Suggestions.visible(st(listOf(q("u1", 4, 30, "unsaved"), q("u2", 4, 20, "unsaved"), q("u3", 4, 10, "unsaved"), q("i", 6, 5, "idea"))), emptySet(), 0)
        assertEquals(listOf("u1", "u2", "i"), v.shown.map { it.id })
        assertEquals(4, v.total)
    }

    @Test fun fillsWhenOnlyOneKind() {
        val v = Suggestions.visible(st((1..5).map { q("u$it", 4, 100L - it, "unsaved") }), emptySet(), 0)
        assertEquals(listOf("u1", "u2", "u3"), v.shown.map { it.id })
        assertEquals(5, v.total)
    }

    @Test fun optionsOnlyForChoice() {
        assertEquals(listOf("A", "B"), Suggestions.options(q("c", 2, 0, "choice", listOf("A", "B"))))
        assertEquals(emptyList<String>(), Suggestions.options(q("c", 2, 0, "choice", listOf("A"))))
        assertEquals(emptyList<String>(), Suggestions.options(q("c", 2, 0, "choice", listOf("A", "B", "C", "D", "E"))))
        assertEquals(emptyList<String>(), Suggestions.options(q("i", 2, 0, "idea", listOf("A", "B"))))
    }
}
