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

    // ---- Proposte piccole (compito app-kpi-piccole-25-10) ----
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)

    @Test fun kpi4_theFiveHourQuotaSplitBySessionByTodaysShare() {
        val sp = Kpi.split(idle, "personal")!!
        assertEquals(24, sp.h5)
        assertEquals(listOf(Kpi.Slice("atlas-shop", 15), Kpi.Slice("master", 9)), sp.slices)
        assertEquals(0, sp.others)
        assertNull(Kpi.split(idle, "nobody"))
    }

    @Test fun kpi5_todaysShareAndTheHandoffMark() {
        assertEquals(62, Kpi.todayShare(idle, "atlas-shop"))
        assertNull(Kpi.todayShare(idle, "nobody"))
        assertEquals(60, Kpi.HANDOFF)
    }

    @Test fun kpi7_threeFiguresOnTopOfTheHome() {
        val h = Kpi.head(idle, page, now, zone)
        // atlas-shop è ferma per Franz (needs_you): una attesa, nessuna al lavoro.
        assertEquals(1, h.waiting); assertEquals(0, h.busy)
        assertEquals(1, h.outcomes); assertEquals(0, h.red)
        // Senza cronologia gli esiti non si sanno: null, non zero.
        assertNull(Kpi.head(idle, null, now, zone).outcomes)
    }

    // ---- I box della home aperti (compito app-box-cliccabili-29-10) ----

    @Test fun boxWaiting_sameSessionsAsTheFigure_oldestFirst_secretsMasked() {
        val q = ContractJson.decodeState(Fixtures.stateQuestion)
        val rows = Kpi.waitingRows(q)
        assertEquals(listOf(Kpi.Waiting("ledger-api", Kpi.WaitKind.QUESTION, "Deploy ready with DEPLOY_TOKEN=\u2022\u2022\u2022, waiting for the client's ok. Deploy now?", 1_789_210_500L, false)), rows)
        assertEquals(Kpi.head(q, null, q.ts, zone).waiting, rows.size)
        // needs_you dice se si chiude da sé; la voce bloccante viene dopo; la più vecchia in cima.
        val st = State(v = 1, ts = now, host = "pc", sessions = listOf(
            s("a").copy(turnBlocking = TurnBlocking("ok to publish", now - 60)),
            s("b").copy(needsYou = NeedsYou("type the code", now - 600, until = true)),
            s("c", SessionState.GONE).copy(needsYou = NeedsYou("gone", now - 900)),
        ), quota = emptyMap())
        assertEquals(listOf("b" to Kpi.WaitKind.NEEDS_YOU, "a" to Kpi.WaitKind.BLOCKING), Kpi.waitingRows(st).map { it.session to it.kind })
        assertTrue(Kpi.waitingRows(st).first().closesItself)
        assertEquals(Kpi.head(st, null, now, zone).waiting, Kpi.waitingRows(st).size)
    }

    @Test fun boxWorking_busySessionsWithGoalToolAndTurn_thenTheStillOnes() {
        val q = ContractJson.decodeState(Fixtures.stateQuestion)
        assertEquals(listOf(Kpi.Working("atlas-shop", "All checkout tests green and the release tagged", "Bash pytest -q tests", "Run the test suite", 100L, "Sonnet 5", 18)), Kpi.workingRows(q, q.ts))
        assertEquals(Kpi.head(q, null, q.ts, zone).busy, Kpi.workingRows(q, q.ts).size)
        // Ferme senza niente per Franz: non chi aspetta (ledger-api), non le sparite.
        assertEquals(listOf(Kpi.Still("field-notes", "README rewritten", 1_789_121_000L)), Kpi.stillRows(q))
    }

    /** app-home-riordino-29-10: chi lavora e intanto aspetta Franz sta solo in «ti aspettano», non in due box. */
    @Test fun boxWorking_busyAndWaiting_countsOnlyAsWaiting() {
        val q = ContractJson.decodeState(Fixtures.stateQuestion)
        val st = q.copy(sessions = q.sessions.map { if (it.name == "atlas-shop") it.copy(needsYou = NeedsYou("type the code", q.ts - 60)) else it })
        val h = Kpi.head(st, null, st.ts, zone)
        assertEquals(Kpi.head(q, null, q.ts, zone).waiting + 1, h.waiting)
        assertEquals(0, h.busy)
        assertEquals(emptyList<Kpi.Working>(), Kpi.workingRows(st, st.ts))
    }

    @Test fun boxOutcomes_redsOnTop_newestFirst_onlyToday() {
        val p = TimelinePage(since = 0, sessions = listOf(
            TimelineSession("a", true, events = listOf(TimelineEvent(now - 300, "outcome", "first"), TimelineEvent(now - 200, "task", "check red", ok = false, ref = "t1"), TimelineEvent(now - 100, "task", "check green", ok = true, ref = "t2"))),
            TimelineSession("b", true, events = listOf(TimelineEvent(now - 50, "outcome", "second"), TimelineEvent(now - 30 * 3600, "outcome", "yesterday"))),
        ))
        val o = Kpi.outcomes(p, now, zone)!!
        assertEquals(listOf(Kpi.Done("a", "check red", now - 200, "t1")), o.red)
        assertEquals(listOf(Kpi.Done("b", "second", now - 50, null), Kpi.Done("a", "first", now - 300, null)), o.done)
        assertFalse(o.atLeast)
        val h = Kpi.head(State(v = 1, ts = now, host = "pc", sessions = emptyList(), quota = emptyMap()), p, now, zone)
        assertEquals(h.outcomes, o.done.size); assertEquals(h.red, o.red.size)
        assertNull(Kpi.outcomes(null, now, zone))
        assertTrue(Kpi.outcomes(p.copy(more = true), now, zone)!!.atLeast)
    }

    @Test fun kpi14_ringAndKpi16_pinned() {
        assertEquals(Kpi.Ring(24, 1), Kpi.ring(idle, "personal"))
        val p = Kpi.pinned(idle, idle.ts)!!
        assertEquals(1, p.waiting); assertEquals(24, p.quota); assertEquals(1_789_228_800L, p.resetAt)
        assertNull(Kpi.pinned(null, now))
    }

    // ---- Contratto 1.96: l'op `kpi` (proposte 8, 9, 10, 12, 13, 17) ----

    private val data: KpiData = run {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val id = cmds.single { it.op == CmdOp.KPI }.id
        val r = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }.single { it.id == id }
        ContractJson.decodeKpi(r.text)
    }

    @Test fun kpiOp_decodesEveryBlockOfTheSample() {
        assertEquals(1_789_214_700L, data.at)
        val t = data.tasks!!
        assertEquals(listOf(3, 1, 1, 2), listOf(t.open, t.waitingYou, t.stalled, t.closedToday))
        assertEquals(KpiTask("atlas-release-2-4", "Release 2.4 of atlas-shop", "running", 9000, 2, 3, true, "master"), t.tasks.first())
        assertEquals(7, data.checksWeek!!.size)
        assertEquals(KpiTurn(1_789_201_000, 1_789_201_420, "fail", 26_100, 260_000), data.turnsToday!!.single().turns[1])
        val h = data.health!!
        assertEquals(2.1, h.latency!!.p50S!!, 0.0); assertEquals(4, h.sessionsHere); assertEquals(5, h.sessionsMax)
        assertEquals(KpiHost("win", true, 0.12, 71, null), h.hosts.single())
        assertEquals(KpiSuggestions(7, 3, 2, 0, 2), data.suggestions7d)
        assertEquals(KpiWaitNow("ledger-api", "question", 1_789_210_500), data.waits!!.now.single())
        assertNull(data.waits!!.week.first().medianMin)
        // Un blocco che il relay non legge arriva null, il resto resta.
        val partial = ContractJson.decodeKpi("""{"at": 1, "kpi_tasks": null, "kpi_health": null}""")
        assertNull(partial.tasks); assertNull(partial.health); assertNull(partial.waits)
    }

    @Test fun kpi8_ageBarFullFromADay_stalledFirst() {
        assertEquals(0.5f, Kpi.ageFraction(43_200), 0.001f)
        assertEquals(1f, Kpi.ageFraction(172_800), 0f)
        val t = data.tasks!!
        assertEquals(listOf("atlas-release-2-4", "ledger-vendor-ticket"), Kpi.tasksOrder(t).map { it.task })
        // Senza fermi conta l'età: il più vecchio prima.
        assertEquals(listOf("ledger-vendor-ticket", "atlas-release-2-4"), Kpi.tasksOrder(t.copy(tasks = t.tasks.map { it.copy(stalled = false) })).map { it.task })
    }

    @Test fun kpi9_weekTotals() {
        assertEquals(33 to 4, Kpi.checksTotals(data.checksWeek!!))
        assertEquals(0 to 0, Kpi.checksTotals(emptyList()))
    }

    @Test fun kpi10_turnsOfASession_andTheirSummary() {
        val turns = Kpi.turnsOf(data, "atlas-shop")
        assertEquals(3, turns.size)
        assertTrue(Kpi.turnsOf(data, "ledger-api").isEmpty())
        assertTrue(Kpi.turnsOf(null, "atlas-shop").isEmpty())
        // 240 + 420 + 700 s = 22 min 40, arrotondati a 23; un fallito; il costo dal primo all'ultimo turno.
        assertEquals(Kpi.TurnsSummary(3, 23, 1, 18_200, 41_000, 41_000), Kpi.turnsSummary(turns))
    }

    @Test fun kpi13_acceptRate() {
        assertEquals(50, Kpi.acceptRate(data.suggestions7d!!))
        assertEquals(14, Kpi.suggestionsTotal(data.suggestions7d!!))
        assertNull(Kpi.acceptRate(KpiSuggestions()))
    }

    @Test fun kpi17_closedWaitsAndScale() {
        assertEquals(5, Kpi.waitsClosed(data.waits!!))
        assertEquals(11.5, Kpi.waitsTop(data.waits!!), 0.0)
        assertEquals(0.0, Kpi.waitsTop(KpiWaits()), 0.0)
    }
}
