package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.CommitmentWaits
import it.pixelbox.cmwatch.contract.State
import org.junit.Assert.*
import org.junit.Test

/** Approva ottimistica (Franz 23/10 21:24: «dopo aver approvato non deve apparire nulla del genere»). */
class ApprovalsTest {
    private val base = State(v = 1, ts = 1_000_000, host = "pc", sessions = emptyList())
    private fun ok(task: String) = Approval(task, task, requestedAt = 10)
    private fun c(task: String, state: String, waitsOn: String? = "person") =
        Commitment(task, task, state = state, approved = state == "approved", waits = waitsOn?.let { CommitmentWaits(on = it) })

    @Test fun onTapTheEntryDisappearsAtOnce() {
        val st = base.copy(approvals = listOf(ok("a"), ok("b")), commitments = listOf(c("a", "awaiting_ok"), c("b", "awaiting_ok"), c("x", "running", null)))
        val v = Approvals.withoutApproved(st, setOf("a"))
        assertEquals(listOf("b"), v.approvals.map { it.task })
        val a = v.commitments!!.first { it.task == "a" }
        assertTrue(a.approved); assertEquals("approved", a.state); assertNull(a.waits)
        val b = v.commitments!!.first { it.task == "b" }
        assertFalse(b.approved); assertEquals("awaiting_ok", b.state)
        assertSame(st, Approvals.withoutApproved(st, emptySet()))
    }

    @Test fun settledTasksAreForgottenTheOthersStayHidden() {
        val st = base.copy(approvals = listOf(ok("b")), commitments = listOf(c("a", "approved", null), c("b", "awaiting_ok")))
        assertEquals(setOf("a"), Approvals.settled(st, setOf("a", "b")))
    }
}
