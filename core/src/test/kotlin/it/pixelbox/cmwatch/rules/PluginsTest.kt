package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

/** La sezione «Plugin e skill» (contratto 1.95). */
class PluginsTest {
    private val stale = ContractJson.decodeState(Fixtures.stateStale)
    private val page: PluginsPage = run {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val id = cmds.single { it.op == CmdOp.PLUGINS }.id
        ContractJson.decodePlugins(root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }.single { it.id == id }.text)
    }

    @Test fun plugins_state_summaryCountsTheUpdates() {
        assertEquals(1, Plugins.pending(stale))
        assertEquals(listOf("personal"), Plugins.accountsWithUpdates(stale))
        assertNull(Plugins.pending(ContractJson.decodeState(Fixtures.stateIdle)))
    }

    @Test fun theListPutsUpdatesFirst() {
        val a = Plugins.account(page.accounts.first { it.account == "personal" })
        assertEquals(listOf("chrisbanes-skills"), a.updates.map { it.name })
        assertEquals("2026.10.26", a.updates.single().latest)
        // Senza ultima versione nota (`update` null) resta fra gli altri, non fra gli aggiornamenti.
        assertTrue(a.rest.any { it.name == "frontend-design" && it.update == null })
        assertEquals("personal:chrisbanes-skills@chrisbanes-skills", Plugins.updateArg("personal", a.updates.single()))
        assertTrue(a.skills > 0)
    }
}
