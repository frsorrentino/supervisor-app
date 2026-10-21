package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.publishedTs
import it.pixelbox.cmwatch.rules.Commitments.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contratto 1.76 (P2, P4): gli impegni del registro in cima alla home. */
class CommitmentsTest {
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)
    private val stale = ContractJson.decodeState(Fixtures.stateStale)
    private val night = ContractJson.decodeNightReport(Fixtures.read("night-report-sample.json"))

    @Test fun theFixturesCarryTheCommitmentsOf176() {
        assertEquals(listOf("ledger-migrations-011", "atlas-seo-pages"), idle.commitments!!.map { it.task })
        val led = idle.commitments!![0]
        assertEquals("running", led.state); assertTrue(led.stalled); assertTrue(led.approved)
        assertEquals(13800L, led.idleS); assertEquals(1789200600L, led.approvedAt); assertEquals("maintainer", led.approvedBy)
        assertEquals("approval", led.lastStep!!.kind); assertEquals("migrations on staging", led.lastStep!!.text)
        assertEquals("person", led.waits!!.who); assertEquals("ledger-api", led.waits!!.on)
        assertEquals("red", led.check!!.outcome); assertEquals("deploy", led.outcomeKind); assertNull(led.paused)
        assertEquals(emptyList<Commitment>(), ContractJson.decodeState(Fixtures.stateQuestion).commitments)
        val paused = stale.commitments!!.single()
        assertEquals("after the client's review", paused.paused!!.reason); assertEquals("master", paused.paused!!.by); assertNull(paused.expected)
        val stalled = ContractJson.decodeEvents(Fixtures.events).single { it.kind == EventKind.TASK_STALLED }
        assertEquals("ledger-migrations-011", stalled.ref); assertEquals("ledger-api", stalled.session)
        assertTrue(StrongAlert.strong(EventKind.TASK_STALLED))
        assertEquals(listOf("ledger-release-14", "notes-gantt-apk"), night.commitments.map { it.task })
    }

    @Test fun rowsKeepTheRelayOrderWithTheAgeOfNow() {
        val rows = Commitments.rows(idle, idle.ts)
        assertEquals(listOf("ledger-migrations-011", "atlas-seo-pages"), rows.map { it.c.task })
        assertEquals(listOf(13800L, 600L), rows.map { it.idle })
        assertEquals(listOf(Tone.WARN, Tone.NORMAL), rows.map { it.tone })
        assertEquals(listOf("ledger-api", "atlas-shop"), rows.map { it.turn })
        assertTrue(rows.none { it.yourTurn }); assertTrue(rows.none { it.approval != null })
        // Cinque ore dopo la pubblicazione (published_at, non ts), senza uno stato nuovo: l'attesa cresce da sola e i colori con lei.
        val later = Commitments.rows(idle, idle.publishedTs + 5 * 3600)
        assertEquals(listOf(13800L + 18000, 600L + 18000), later.map { it.idle })
        assertEquals(listOf(Tone.ALERT, Tone.WARN), later.map { it.tone })
        assertEquals(13800L + 18000, later[0].age)
    }

    @Test fun aPausedCommitmentHasNoColourAndGoesLast() {
        val row = Commitments.rows(stale, stale.ts).single()
        assertEquals(Tone.NORMAL, row.tone); assertEquals("orbit-docs", row.turn); assertFalse(row.yourTurn)
        val mixed = idle.copy(commitments = stale.commitments!! + idle.commitments!!)
        assertEquals(listOf("ledger-migrations-011", "atlas-seo-pages", "orbit-docs-site"), Commitments.rows(mixed, idle.ts).map { it.c.task })
    }

    @Test fun anOkOfTheRegistryBecomesTheApproveButtonWhenItIsYourTurn() {
        val ok = Approval("ledger-release-14", "Release 1.4 di ledger-api", "tag v1.4 e push", "github.com/example/ledger-api", requestedAt = 1791332460)
        val st = idle.copy(commitments = night.commitments, approvals = listOf(ok))
        val rows = Commitments.rows(st, idle.ts)
        val release = rows.single { it.c.task == "ledger-release-14" }
        assertTrue(release.yourTurn); assertNull(release.turn); assertEquals(ok, release.approval)
        val gantt = rows.single { it.c.task == "notes-gantt-apk" }
        assertFalse(gantt.yourTurn); assertEquals("field-notes", gantt.turn); assertNull(gantt.approval)
        assertEquals(1, Commitments.yours(st))
        assertEquals(0, Commitments.yours(idle))
    }

    @Test fun anOlderRelayHasNoRows() {
        assertEquals(emptyList<Commitments.Row>(), Commitments.rows(idle.copy(commitments = null), idle.ts))
        assertEquals(0, Commitments.yours(idle.copy(commitments = null)))
    }
}
