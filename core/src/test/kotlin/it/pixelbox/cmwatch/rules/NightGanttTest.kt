package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.NightGoal
import it.pixelbox.cmwatch.contract.NightPoint
import it.pixelbox.cmwatch.rules.NightGantt.GoalState
import it.pixelbox.cmwatch.rules.NightGantt.Where
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * Il Gantt della notte (bozze A e B approvate da Franz il 21/10 alle 10:29) sul rapporto v2 della fixture: notte del 07/10
 * dalle 00:33 alle 02:36, ledger-api fermo su una domanda, orbit-docs in parte su win, field-notes fermo senza lavoro.
 */
class NightGanttTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val report = ContractJson.decodeNightReport(Fixtures.read("night-report-sample.json"))
    private val a = NightGantt.projects(report, zone)
    private val b = NightGantt.goals(report, zone)
    private fun at(h: Int, m: Int) = ((h * 60 + m) * 60).toDouble() / (3 * 3600)

    @Test fun theV2ReportHasTheGantt() {
        assertTrue(NightGantt.has(report))
        assertFalse(NightGantt.has(report.copy(v = 1)))
        assertFalse(NightGantt.has(report.copy(projects = report.projects.map { it.copy(spans = emptyList(), goals = emptyList()) })))
    }

    @Test fun theAxisGoesFromTheWholeHourBeforeToTheWholeHourAfter() {
        assertEquals(listOf(0, 1, 2, 3), a.axis.ticks.map { it.hour })
        assertEquals(0.0, a.axis.ticks.first().at, 1e-9); assertEquals(1.0, a.axis.ticks.last().at, 1e-9)
        assertEquals(10800L, a.axis.end - a.axis.start)
        assertEquals(listOf(0, 3), a.axis.every(3).map { it.hour })
    }

    @Test fun theDashedLineIsTheTimeOfAReportMadeDuringTheNight() {
        assertEquals(at(2, 36), a.axis.mark, 1e-9)
        assertEquals(NightGantt.Mark.NOW, a.axis.markKind)
        val morning = report.copy(window = report.window.copy(endSource = "morning_message"))
        assertEquals(NightGantt.Mark.END, NightGantt.axis(morning, zone).markKind)
    }

    @Test fun rowsInTheOrderTheyStartedThenTheIdleOnes() =
        assertEquals(listOf("ledger-api", "orbit-docs", "field-notes"), a.rows.map { it.name })

    @Test fun barsAreTheWorkSpans() {
        val ledger = a.rows[0]
        assertEquals(2, ledger.bars.size)
        assertEquals(at(0, 36), ledger.bars[0].from, 1e-9); assertEquals(at(0, 45), ledger.bars[0].to, 1e-9)
        assertEquals(at(0, 50), ledger.bars[1].from, 1e-9); assertEquals(at(0, 56), ledger.bars[1].to, 1e-9)
        assertTrue(a.rows[2].bars.isEmpty())
    }

    @Test fun diamondsArePushesAndReleasesNotCommits() {
        assertTrue(a.rows[0].pubs.isEmpty())
        assertEquals(at(1, 25), a.rows[1].pubs.single(), 1e-9)
    }

    @Test fun pointsDoneOnTheTotalBesideTheName() {
        assertEquals(3 to 3, a.rows[0].done to a.rows[0].total)
        // Un obiettivo senza punti (il messaggio che ha assegnato il lavoro) vale un punto.
        assertEquals(0 to 1, a.rows[1].done to a.rows[1].total)
        assertEquals(0 to 0, a.rows[2].done to a.rows[2].total)
    }

    @Test fun whereEachProjectGotAndWhetherItNeedsFranz() {
        assertEquals(listOf(Where.STOPPED, Where.PARTIAL, Where.STOPPED), a.rows.map { it.where })
        assertEquals(listOf(true, false, true), a.rows.map { it.needsYou })
        assertEquals("docs: redirect dei vecchi indirizzi", a.rows[1].line)
        // Il prefisso del relay («domanda:», «!:») non si mostra: lo stato lo dice già.
        assertEquals("Pubblico la release 1.4 adesso?", a.rows[0].line)
        assertEquals(mapOf(Where.PARTIAL to 1, Where.STOPPED to 2), a.counts)
    }

    @Test fun aShortSpanStillShows() {
        val tiny = report.copy(projects = report.projects.map { p -> p.copy(spans = p.spans.map { it.copy(end = it.start + 5) }) })
        NightGantt.projects(tiny, zone).rows[0].bars.forEach { assertTrue(it.to - it.from >= NightGantt.MIN_BAR - 1e-9) }
    }

    @Test fun theGoalTotalsCountPoints() {
        assertEquals(NightGantt.Totals(done = 3, running = 0, partial = 1, stopped = 0, todo = 0), b.totals)
        assertEquals(4, b.totals.total)
    }

    @Test fun oneSegmentPerGoalFromTheFirstWorkToItsEnd() {
        val ledger = b.groups[0].goals.single()
        assertEquals(GoalState.DONE, ledger.state)
        // La proposta è delle 00:30, il lavoro comincia alle 00:36; l'obiettivo è chiuso dal FINE delle 02:27.
        assertEquals(at(0, 36), ledger.bar!!.from, 1e-9); assertEquals(at(2, 27), ledger.bar!!.to, 1e-9)
        assertEquals("02:27", NightGantt.hm(ledger.doneAt!!, zone))
        assertEquals(listOf("Migrazione del registro", "Pagina mensile", "Release 1.4"), ledger.points)
    }

    @Test fun anOpenGoalReachesTheDashedLine() {
        val orbit = b.groups[1].goals.single()
        assertEquals(GoalState.PARTIAL, orbit.state)
        assertEquals(at(1, 0), orbit.bar!!.from, 1e-9); assertEquals(b.axis.mark, orbit.bar!!.to, 1e-9)
        assertTrue(orbit.points.isEmpty())
        assertTrue(b.groups[2].goals.isEmpty()); assertEquals("/clear", b.groups[2].line)
    }

    @Test fun aGoalNotStartedHasNoBar() {
        val todo = report.copy(projects = report.projects.mapIndexed { i, p ->
            if (i == 0) p.copy(goals = p.goals + NightGoal(title = "Esportazione", at = p.goals[0].at, state = "todo", points = listOf(NightPoint("CSV"), NightPoint("PDF")))) else p
        })
        val g = NightGantt.goals(todo, zone)
        assertNull(g.groups[0].goals[1].bar)
        assertEquals(2, g.totals.todo)
        assertEquals(3 to 5, NightGantt.points(todo.projects[0].goals))
    }

    @Test fun anUnknownStateIsNotInvented() {
        assertEquals(Where.UNKNOWN, NightGantt.where(report.projects[0].copy(where = null)))
        assertEquals(GoalState.TODO, NightGantt.goalState("blocked-by-robots"))
    }
}
