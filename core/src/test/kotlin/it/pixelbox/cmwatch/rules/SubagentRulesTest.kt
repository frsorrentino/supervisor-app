package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Subagents
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** app-sottoagenti-23-11 (contratto 1.158): gli stessi casi di subagents.test.ts, sulle fixture vere del relay. */
class SubagentRulesTest {
    private val root: JsonObject = ContractJson.json.parseToJsonElement(File("../contract/subagents-sample.json").readText()).jsonObject
    private fun subs(k: String) = ContractJson.json.decodeFromJsonElement(Subagents.serializer(), root[k]!!)

    @Test fun summaryCountsTheOthersAsDone() {
        assertEquals(SubagentRules.Summary(0, 3), SubagentRules.summary(subs("win")))
        assertEquals(SubagentRules.Summary(1, 6), SubagentRules.summary(subs("workflow")))
    }

    @Test fun rowsOfAWinSession() {
        val r = SubagentRules.rows(subs("win"), now = 1795436000)
        assertEquals(listOf("Fixture sottoagente in corso", "Fixture sottoagente background", "Fixture sottoagente primo piano"), r.map { it.title })
        assertEquals(SubagentRules.Row("a76eb4828a2262868", "Fixture sottoagente primo piano", false, "Explore", "Haiku 5.5", true, null, 69142L to 589L, 5), r[2])
        assertNull(SubagentRules.more(subs("win")))
    }

    @Test fun aWorkflowAgentAtWorkAndAForkWithoutTranscript() {
        val r = SubagentRules.rows(subs("workflow"), now = 1795436100)
        // Al lavoro: da quanto lavora, niente token finché non ha finito, la fase del Workflow.
        assertEquals(SubagentRules.Row("b000000000000000c", "fixture wf 3", true, "workflow-subagent", "Haiku 5.5", false, "Review", null, 178), r[0])
        // Un fork senza trascrizione: finito anche con ended null, senza durata; l'alias resta com'è.
        assertEquals(SubagentRules.Row("a1f3685a786f72627", "fixture 1", false, "fork", "inherit", true, null, null, null), r[1])
        assertEquals(7, SubagentRules.more(subs("workflow")))
    }

    @Test fun theOpAnswer() {
        // Come la legge l'app: il testo del risultato dell'op.
        assertEquals(3, ContractJson.decodeSubagents(root["op"]!!.toString())!!.items.size)
        assertNull(ContractJson.decodeSubagents(root["op_none"]!!.toString()))
    }

    @Test fun shortNumbers() {
        assertEquals(listOf("69k", "211k", "0,6k", "0,2k", "1,2M", "6,9k", "1k"), listOf(69142L, 211219, 589, 200, 1_234_567, 6_900, 1_000).map { SubagentRules.tokens(it) })
        assertEquals("0.6k", SubagentRules.tokens(589, en = true))
        assertEquals(listOf("18 s", "4 m", "1 h 05"), listOf(18L, 240, 3900).map(SubagentRules::duration))
    }
}
