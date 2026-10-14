package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.SessionState
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

/** Contratti 1.61-1.64 sulle fixture del relay 0.8.21. */
class NewProjectTest {
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)
    private val root = kotlinx.serialization.json.Json.parseToJsonElement(Fixtures.cmdResult).let { it as kotlinx.serialization.json.JsonObject }
    private val cmds = (root["cmd"] as kotlinx.serialization.json.JsonArray).map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
    private val results = (root["result"] as kotlinx.serialization.json.JsonArray).map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }

    @Test fun aNewProjectNeedsTwoTaps() {
        assertEquals(listOf("/home/demo/workspaces/personal", "/home/demo/workspaces/work/clients", "/home/demo/workspaces/work/own"), st.projectParents)
        assertTrue("project_new" in st.ops.orEmpty())
        val (first, second) = cmds.filter { it.op == CmdOp.PROJECT_NEW }
        assertNull(first.confirm); assertEquals(true, second.confirm); assertEquals("set up a README", second.prompt)
        val r1 = results.single { it.id == first.id }
        assertFalse(r1.ok); assertEquals("/home/demo/workspaces/personal/brand-new", NewProject.confirmPath(r1.text))
        val r2 = results.single { it.id == second.id }
        assertTrue(r2.ok); assertEquals("brand-new", r2.session); assertNull(NewProject.confirmPath(r2.text))
        assertTrue(NewProject.validName("brand-new")); assertTrue(NewProject.validName("a.b_c-1"))
        assertFalse(NewProject.validName(".hidden")); assertFalse(NewProject.validName("a b")); assertFalse(NewProject.validName("x".repeat(65)))
        assertFalse(ContractJson.encode(cmds[0]).contains("\"confirm\""))
    }

    @Test fun sendAtTodayOrTomorrow() {
        val z = ZoneId.of("Europe/Rome")
        val now = ZonedDateTime.of(2026, 10, 14, 21, 30, 0, 0, z)
        assertEquals(ZonedDateTime.of(2026, 10, 14, 22, 15, 0, 0, z).toEpochSecond(), SendAt.epoch(LocalTime.of(22, 15), now))
        assertEquals(ZonedDateTime.of(2026, 10, 15, 8, 0, 0, 0, z).toEpochSecond(), SendAt.epoch(LocalTime.of(8, 0), now))
        val p = cmds.single { it.at != null }
        assertEquals(CmdOp.PROMPT, p.op)
        assertTrue(SendAt.scheduled(results.single { it.id == p.id }.text))
    }

    @Test fun aFailedTurnOffersContinue() {
        val ev = ContractJson.decodeEvents(Fixtures.events)
        val f = ev.single { it.kind == EventKind.TURN_FAILED }
        assertEquals("carry on from where you stopped", f.continueText); assertEquals("api_error: 529 overloaded_error", f.reason)
        val atlas = st.sessions.single { it.name == "atlas-shop" }.copy(state = SessionState.IDLE, turnStarted = f.ts - 100)
        assertEquals(f, AlertEvents.openFailure(ev, atlas, f.ts + 60))
        assertNull(AlertEvents.openFailure(ev, atlas.copy(turnStarted = f.ts + 10), f.ts + 60))
        assertNull(AlertEvents.openFailure(ev, atlas, f.ts + 7 * 3600))
    }

    @Test fun quietWhenFranzIsAtThePc() {
        val ev = ContractJson.decodeEvents(Fixtures.events)
        assertTrue(AlertEvents.quiet(ev.single { it.key.endsWith("_024") }, null))
        assertTrue(AlertEvents.quiet(null, st.sessions.single { it.name == "atlas-shop" }))
        assertFalse(AlertEvents.quiet(ev.first { it.kind == EventKind.QUESTION }, st.sessions.single { it.name == "ledger-api" }))
    }
}
