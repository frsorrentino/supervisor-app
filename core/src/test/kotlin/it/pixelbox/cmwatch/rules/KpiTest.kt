package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.Kpi.Band.STILL
import it.pixelbox.cmwatch.rules.Kpi.Band.WAIT
import it.pixelbox.cmwatch.rules.Kpi.Band.WORK
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

/** I KPI coi dati già nel relay (proposte 6, 11, 15, 18 del 25/10). */
class KpiTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val page: TimelinePage = run {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val id = cmds.single { it.op == CmdOp.TIMELINE }.id
        val r = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }.single { it.id == id }
        ContractJson.decodeTimeline(r.text)
    }
    private val now = 1_789_210_000L
    private fun ev(at: Long, kind: String = "prompt") = TimelineEvent(at, kind, "x")
    private fun s(name: String, st: SessionState = SessionState.IDLE) = Session(id = name, name = name, account = "personale", project = name, state = st, since = 0)

    @Test fun kpi6_hourlyCountsTheLastSixHoursOldestFirst() {
        assertEquals(listOf(0, 0, 0, 0, 2, 4), Kpi.hourly(page.sessions.single { it.session == "ledger-api" }.events, now))
        assertEquals(listOf(0, 0, 0, 0, 0, 6), Kpi.hourly(page.sessions.single { it.session == "field-notes" }.events, now))
        // Fuori dalle sei ore o nel futuro non conta.
        assertEquals(listOf(0, 0, 0, 0, 0, 0), Kpi.hourly(listOf(ev(now - 7 * 3600), ev(now + 60)), now))
    }

    @Test fun kpi11_workWhereEventsAreClose_stillBetween_waitOnlyForTheCurrentStretch() {
        val from = now - 4 * 3600
        val events = listOf(ev(from + 600), ev(from + 900), ev(from + 1500), ev(from + 3 * 3600))
        val spans = Kpi.day(events, from, now, null)
        assertEquals(listOf(STILL, WORK, STILL, WORK, STILL), spans.map { it.band })
        assertEquals(from + 600, spans[1].from); assertEquals(from + 1500, spans[1].to)
        // Un evento isolato vale 10 minuti.
        assertEquals(600L, spans[3].to - spans[3].from)
        assertEquals(900L + 600L, Kpi.workSeconds(spans))
        // Ti aspetta da mezz'ora: l'ultimo tratto diventa ambra, il resto resta.
        val waiting = Kpi.day(events, from, now, now - 1800)
        assertEquals(WAIT, waiting.last().band); assertEquals(now - 1800, waiting.last().from); assertEquals(now, waiting.last().to)
        assertEquals(listOf(STILL, WORK, STILL, WORK, STILL, WAIT), waiting.map { it.band })
        // Le fasce coprono tutto, senza buchi.
        waiting.zipWithNext().forEach { (a, b) -> assertEquals(a.to, b.from) }
    }

    @Test fun kpi15_cellsAndTheThreeMostActiveLiveSessions() {
        assertEquals(listOf(STILL, STILL, STILL, STILL, WORK, WAIT), Kpi.cells(listOf(ev(now - 4000)), now, waitingNow = true))
        val asked = Question(id = "q", kind = QuestionKind.ASK, text = "?", options = emptyList(), tier = Tier.LOW, askedAt = now - 300)
        val state = State(v = 1, ts = now, host = "h", sessions = listOf(s("field-notes"), s("ledger-api"), s("idle-one"), s("asking").copy(question = asked)))
        val rows = Kpi.tileRows(page, state, now)
        // ledger-api non è viva nella cronologia: resta fuori; chi aspetta adesso entra anche senza eventi.
        assertEquals(listOf("field-notes", "asking"), rows.map { it.name })
        assertEquals(WAIT, rows[1].cells.last())
    }

    @Test fun kpi18_dayFigures() {
        val st = ContractJson.decodeState(Fixtures.stateIdle)
        val f = Kpi.dayFigures(page, st, now, zone)
        assertEquals(7, f.prompts); assertEquals(1, f.commits); assertFalse(f.atLeast)
        assertEquals(st.usageToday!!.sumOf { it.tokens }, f.tokens)
        assertEquals("atlas-shop", f.topSession)
        assertEquals(f.prompts, f.perHour.sumOf { it.first }); assertEquals(f.commits, f.perHour.sumOf { it.second })
        // Al tetto di 40 eventi per sessione la cifra è un minimo.
        val full = page.copy(sessions = listOf(page.sessions[0].copy(events = List(40) { ev(now - 100) })))
        assertTrue(Kpi.dayFigures(full, st, now, zone).atLeast)
    }
}
