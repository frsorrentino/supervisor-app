package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.CommitmentWaits
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.DonePage
import it.pixelbox.cmwatch.rules.Lavori.Bar
import it.pixelbox.cmwatch.rules.Lavori.Kind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** La pagina unica «Lavori» (mockup approvato da Franz il 22/10 alle 12:43): i quattro blocchi dal contratto 1.76, dall'agenda e dagli eventi. */
class LavoriTest {
    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)
    private val stale = ContractJson.decodeState(Fixtures.stateStale)
    private val night = ContractJson.decodeNightReport(Fixtures.read("night-report-sample.json"))
    private val events = ContractJson.decodeEvents(Fixtures.events)
    private val agenda: AgendaPage = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray
        .first { it.jsonObject.getValue("id").jsonPrimitive.content.endsWith("0386") }
        .let { ContractJson.decodeAgenda(it.jsonObject.getValue("text").jsonPrimitive.content) }
    /** Il giorno dopo l'`until` della riga sospesa dell'agenda (2026-10-12): conta come aperta. */
    private val today: LocalDate = LocalDate.of(2026, 10, 13)
    private val okRelease = Approval("ledger-release-14", "Release 1.4 di ledger-api", "tag v1.4 e push", "github.com/example/ledger-api", requestedAt = 1791332460)
    private val okLoose = Approval("loose-task", "Loose ok", "push", "master", requestedAt = 1789214000)
    private val st = idle.copy(commitments = night.commitments + idle.commitments!!, approvals = listOf(okRelease, okLoose))
    private val eventDay: LocalDate = Instant.ofEpochSecond(1789199700).atZone(zone).toLocalDate()

    @Test fun theFixturesGiveTheFourBlocks() {
        val fy = Lavori.forYou(st, agenda, idle.ts, today)
        assertEquals(listOf("Loose ok", "Release 1.4 di ledger-api"), fy.ok.map { it.title })
        assertEquals(okLoose, fy.ok[0].approval); assertNull(fy.ok[0].commitment); assertEquals("master", fy.ok[0].who)
        assertEquals("ledger-release-14", fy.ok[1].commitment!!.task); assertEquals(okRelease, fy.ok[1].approval); assertEquals("ledger-api", fy.ok[1].who)
        assertEquals(listOf("Confirm the 6 client ids with a candidate"), fy.outside.map { it.title })
        assertEquals(agenda.rows[0].ref, fy.outside[0].who); assertEquals(Kind.OUTSIDE, fy.outside[0].kind); assertEquals(agenda.rows[0], fy.outside[0].agenda)
        assertEquals(listOf("Move the docs site to the new host"), fy.claude.map { it.title }); assertEquals(Kind.CLAUDE, fy.claude[0].kind)
        assertEquals(4, fy.total)
        val ip = Lavori.inProgress(st, agenda, idle.ts, zone, today)
        assertEquals(listOf("notes-gantt-apk", "ledger-migrations-011", "atlas-seo-pages"), ip.active.map { it.row!!.c.task })
        assertTrue(ip.active.all { it.bars.size == 1 && !it.bars[0].dashed })
        assertTrue(ip.paused.isEmpty())
        assertEquals(listOf("Staging of atlas-shop not reachable from the CLI"), ip.others.map { it.agenda!!.title })
        val done = Lavori.done(events, zone, eventDay)
        assertEquals(1, done.size); assertEquals(eventDay, done[0].date)
        val row = done[0].rows.single()
        assertEquals("atlas-release-2-4", row.task); assertEquals("Release 2.4 of atlas-shop", row.title)
        assertEquals("atlas-shop", row.session); assertEquals(1789199700L, row.at); assertNull(row.proof)
    }

    @Test fun anOkIsNeverListedTwice() {
        val fy = Lavori.forYou(idle.copy(commitments = night.commitments, approvals = listOf(okRelease)), null, idle.ts, today)
        assertEquals(1, fy.ok.size); assertEquals(okRelease, fy.ok[0].approval); assertEquals("ledger-release-14", fy.ok[0].commitment!!.task)
    }

    @Test fun aPauseWithoutPersonStaysInProgress() {
        val s = stale.copy(commitments = listOf(stale.commitments!!.single().copy(waits = CommitmentWaits("person", "orbit-docs"))))   // relay 1.76
        assertEquals(0, Lavori.forYou(s, null, stale.ts, today).total)
        val ip = Lavori.inProgress(s, null, stale.ts, zone, today)
        assertTrue(ip.active.isEmpty()); assertEquals(listOf("orbit-docs-site"), ip.paused.map { it.row!!.c.task })
    }

    @Test fun aPauseThatWaitsThePersonIsForYou() {
        val c = stale.commitments!!.single()   // 1.77: waits.on = person, paused.at = l'inizio vero
        val fy = Lavori.forYou(stale, null, stale.ts, today)
        assertTrue(fy.ok.isEmpty())
        val row = fy.outside.single()
        assertEquals("after the client's review", row.title); assertEquals("orbit-docs", row.who); assertEquals(1789100000L, row.at); assertEquals(c, row.commitment)
        assertEquals(listOf("orbit-docs-site"), Lavori.inProgress(stale, null, stale.ts, zone, today).paused.map { it.row!!.c.task })
    }

    @Test fun theAxisCoversSevenDaysEndingTomorrow() {
        val ax = Lavori.axis(idle.ts, zone)   // 12/09/2026 14:00 a Roma
        assertEquals(LocalDate.of(2026, 9, 6).atStartOfDay(zone).toEpochSecond(), ax.start)
        assertEquals(LocalDate.of(2026, 9, 13).atStartOfDay(zone).toEpochSecond(), ax.end)
        assertEquals(listOf("6", "7", "8", "9", "10", "11", "12"), ax.days.map { it.first })
        assertEquals(1.0 / 7, ax.days[1].second, 1e-9)
        assertEquals((6 * 24 + 14).toDouble() / (7 * 24), ax.now, 1e-9)
    }

    @Test fun aBarOlderThanTheAxisStartsAtTheEdge() {
        val ax = Lavori.axis(idle.ts, zone)
        val c = idle.commitments!![1].copy(approvedAt = ax.start - 86400)
        assertEquals(listOf(Bar(0.0, ax.now, false)), Lavori.bars(c, ax, idle.ts))
    }

    @Test fun aPausedBarIsSolidThenDashed() {
        val ax = Lavori.axis(stale.ts, zone)
        val f = { t: Long -> (t - ax.start).toDouble() / (ax.end - ax.start) }
        val fixture = stale.commitments!!.single()   // ok 1789120000, pausa dal 1789100000: in pausa da prima dell'ok, tutta tratteggiata
        assertEquals(listOf(Bar(f(1789120000), f(stale.ts), true)), Lavori.bars(fixture, ax, stale.ts))
        val c = fixture.copy(paused = fixture.paused!!.copy(at = 1789190000))
        assertEquals(listOf(Bar(f(1789120000), f(1789190000), false), Bar(f(1789190000), f(stale.ts), true)), Lavori.bars(c, ax, stale.ts))
        val old = c.copy(approvedAt = ax.start - 2 * 86400, paused = c.paused!!.copy(at = ax.start - 86400))
        assertEquals(listOf(Bar(0.0, f(stale.ts), true)), Lavori.bars(old, ax, stale.ts))
    }

    @Test fun aFreshBarHasAMinimumWidth() {
        val ax = Lavori.axis(idle.ts, zone)
        val b = Lavori.bars(idle.commitments!![1].copy(approvedAt = idle.ts - 10), ax, idle.ts).single()
        assertEquals(Lavori.MIN_BAR, b.to - b.from, 1e-9); assertTrue(b.to <= 1.0)
    }

    @Test fun doneKeepsTheLastCheckOfATask() {
        val later = Event(key = "x_1", kind = EventKind.TASK_CHECK, session = "ledger-api", ts = 1789200360, title = "✓ ledger-migrations-011: check green", body = "Migrations 008-011 on staging\nall green", ref = "ledger-migrations-011")
        val earlier = Event(key = "x_0", kind = EventKind.TASK_CHECK, session = "atlas-shop", ts = 1789199000, title = "✓ atlas-release-2-4: check green", body = "Release 2.4 of atlas-shop", ref = "atlas-release-2-4")
        val day = Lavori.done(events + later + earlier, zone, eventDay).single()
        assertEquals(listOf("ledger-migrations-011" to 1789200360L, "atlas-release-2-4" to 1789199700L), day.rows.map { it.task to it.at })
        assertEquals("Migrations 008-011 on staging", day.rows[0].title)
        // Fuori dai sette giorni non si vede; un giorno dopo l'evento è ancora dentro.
        assertTrue(Lavori.done(events, zone, eventDay.plusDays(7)).isEmpty())
        assertEquals(1, Lavori.done(events, zone, eventDay.plusDays(1)).size)
    }

    /** Contratto 1.77: l'op `done` porta la prova e i giorni prima; i giorni senza righe sono richiusi col conteggio. */
    @Test fun theDoneOpGivesTheProofAndTheDaysBefore() {
        val raw = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray
            .first { it.jsonObject.getValue("id").jsonPrimitive.content.endsWith("0760") }
            .jsonObject.getValue("text").jsonPrimitive.content
        val page: DonePage = ContractJson.decodeDone(raw)
        assertEquals("2026-02-10", page.date); assertEquals(4, page.rows.size); assertEquals(listOf("2026-02-09" to 3, "2026-02-07" to 1), page.days.map { it.date to it.n })
        val days = Lavori.doneFromPages(mapOf(page.date to page), zone)
        assertEquals(listOf("2026-02-10", "2026-02-09", "2026-02-07"), days.map { it.date.toString() })
        assertEquals(listOf(4, 3, 1), days.map { it.n }); assertEquals(listOf(4, 0, 0), days.map { it.rows.size })
        val rows = days[0].rows
        assertEquals(listOf("atlas-release-2-3", "ledger-migrations-010", "notes-night-report", "orbit-docs-lint"), rows.map { it.task })
        assertEquals(listOf("release v2.3", "commit 9f8e7d6", "docs/notte/2026-02-10-field-notes.md", "npm run lint"), rows.map { it.proof })
        assertEquals("https://github.com/example/atlas-shop/releases/tag/v2.3", rows[0].url); assertNull(rows[2].url)
        assertEquals(listOf("green", "green", "cancelled", "failed"), rows.map { it.outcome })
        assertEquals("Release 2.3 of atlas-shop", rows[0].title); assertEquals("atlas-shop", rows[0].session); assertNull(rows[2].session); assertEquals(1789211400L, rows[0].at)
        // Un giorno prima caricato dopo si unisce; il più recente resta primo.
        val older = page.copy(date = "2026-02-09", rows = page.rows.take(3), days = emptyList())
        val merged = Lavori.doneFromPages(mapOf(page.date to page, older.date to older), zone)
        assertEquals(listOf(4, 3, 1), merged.map { it.n }); assertEquals(3, merged[1].rows.size)
    }

    @Test fun oldRelayHasNoRows() {
        val s = idle.copy(commitments = null)
        assertEquals(0, Lavori.forYou(s, null, idle.ts, today).total)
        val ip = Lavori.inProgress(s, null, idle.ts, zone, today)
        assertTrue(ip.active.isEmpty() && ip.paused.isEmpty() && ip.others.isEmpty())
        assertEquals(1, Lavori.done(events, zone, eventDay).size)
    }
}
