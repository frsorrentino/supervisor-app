package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.DecisionsSummary
import it.pixelbox.cmwatch.contract.ProveSummary
import org.junit.Assert.*
import org.junit.Test

/** Contratto 1.113 (app-pulizia-tocca-03-11): un conto solo per Tocca a te, dal relay, e le voci uscite da sole oggi. */
class ToccaRulesTest {
    private val s = ContractJson.decodeState(Fixtures.stateIdle)

    @Test fun theCountComesFromTheRelay() {
        val t = s.tocca!!
        assertEquals(t.n, t.prove + t.decidi)
        assertEquals(ToccaRules.Counts(t.decidi, t.prove), ToccaRules.counts(s.copy(decisions = DecisionsSummary(n = 99), prove = ProveSummary(n = 98))))
    }

    @Test fun anOlderRelayKeepsThePageCounts() =
        assertEquals(ToccaRules.Counts(3, 2), ToccaRules.counts(s.copy(tocca = null, decisions = DecisionsSummary(n = 3), prove = ProveSummary(n = 2))))

    @Test fun todaysClosingsAreReadOnlyRows() {
        val td = ToccaRules.closedToday(s)!!
        assertTrue(td.n >= td.items.size)
        assertEquals(setOf("pause", "prova"), td.items.map { it.kind }.toSet())
        assertEquals(setOf(1, 2), td.items.map { it.rule }.toSet())
        assertTrue(td.items.all { it.task.isNotBlank() && it.at > 0 })
        assertNull(ToccaRules.closedToday(s.copy(tocca = s.tocca!!.copy(today = null))))
        assertNull(ToccaRules.closedToday(s.copy(tocca = null)))
    }
}
