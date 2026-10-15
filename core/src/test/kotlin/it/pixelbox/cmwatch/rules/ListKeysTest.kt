package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** 15/10 23:41: due sessioni «claude-plugins» con la stessa chiave chiudevano l'app a ogni apertura. */
class ListKeysTest {
    private data class S(val id: String, val name: String)

    @Test fun dueSessioniConLoStessoNomeHannoChiaviDiverse() {
        val rows = listOf(S("16776", "claude-plugins"), S("1ff97204", "claude-plugins"))
        val keys = uniqueKeys(rows) { "s-" + it.id }
        assertEquals(listOf("s-16776", "s-1ff97204"), keys)
    }

    @Test fun ancheConLoStessoIdNonSiRipetono() {
        val rows = listOf(S("a", "x"), S("a", "x"), S("b", "y"), S("a", "x"))
        val keys = uniqueKeys(rows) { "s-" + it.id }
        assertEquals(listOf("s-a", "s-b", "s-a#2", "s-a#3").sorted(), keys.sorted())
        assertEquals(keys.size, keys.toSet().size)
    }
}
