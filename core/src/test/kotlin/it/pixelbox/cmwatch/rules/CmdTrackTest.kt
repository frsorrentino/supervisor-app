package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.CmdTrack.Phase
import it.pixelbox.cmwatch.transport.FakeTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * app-tasti-riscontro-20-11, i comandi appesi (Franz 20/11 09:46): a che punto è un comando mandato da un tasto. Un caso per riga di
 * quelle che l'app dice; gli stessi casi in web/src/lib/cmdTrack.test.ts. In fondo il comando appeso della demo sul bus finto.
 */
class CmdTrackTest {
    private val ok = CmdResult("c", true, "interrupted", 0)
    private val queued = CmdResult("c", true, "queued: kb is working, it reads it when the turn ends", 0)
    private fun s(state: SessionState, started: Long? = null) = Session(id = "kb", name = "kb", account = "personale", project = "kb", state = state, since = 0, turnStarted = started)

    @Test fun firstSecondsSaySending() = assertEquals(Phase.SENDING, CmdTrack.phase(2, null, null))
    @Test fun stillOnTheBusIsNotTaken() = assertEquals(Phase.NOT_TAKEN, CmdTrack.phase(6, null, taken = false))
    @Test fun takenWithoutResultIsWorking() = assertEquals(Phase.WORKING, CmdTrack.phase(25, null, taken = true))
    @Test fun unknownStaysSending() = assertEquals(Phase.SENDING, CmdTrack.phase(25, null, taken = null))
    @Test fun lateResultIsDone() = assertEquals(Phase.DONE, CmdTrack.phase(40, ok, taken = true))
    @Test fun refusedSaysNo() = assertEquals(Phase.REFUSED, CmdTrack.phase(3, CmdResult("c", false, "kb is not running", 0), null))
    @Test fun pastTheMaximumNoAnswer() {
        assertEquals(Phase.WORKING, CmdTrack.phase(CmdTrack.MAX_S - 1, null, taken = true))
        assertEquals(Phase.NO_ANSWER, CmdTrack.phase(CmdTrack.MAX_S, null, taken = true))
        assertTrue(CmdTrack.final(Phase.NO_ANSWER))
    }

    @Test fun queuedUntilTheSessionTakesIt() {
        val sentAt = 1_000L
        assertEquals("il turno di prima", Phase.QUEUED, CmdTrack.phase(5, queued, true, s(SessionState.BUSY, started = 900), sentAt))
        assertEquals("un turno nuovo dopo l'invio", Phase.DELIVERED, CmdTrack.phase(60, queued, true, s(SessionState.BUSY, started = 1_050), sentAt))
        assertEquals("ferma: la coda è stata letta", Phase.DELIVERED, CmdTrack.phase(60, queued, true, s(SessionState.IDLE), sentAt))
        assertEquals("con una domanda aperta resta in coda", Phase.QUEUED, CmdTrack.phase(60, queued, true, s(SessionState.WAITING, started = 900), sentAt))
        assertFalse(CmdTrack.final(Phase.QUEUED))
    }

    /** Il comando appeso della demo sul bus finto (Interrompi su atlas-shop, al lavoro nella fixture): in /cmd per 8 s, poi preso, l'esito dopo 30 s, oltre l'attesa normale di 20 s. */
    @Test fun demoHangingCommand() = runTest {
        var clock = 1_000L
        val bus = FakeTransport({ Fixtures.read("$it.json") }, { clock })
        val cmd = Cmd("hang-1", CmdOp.INTERRUPT, "atlas-shop", null, clock, "phone")
        val sending = async { bus.send(cmd) }
        testScheduler.runCurrent()
        clock += 5
        assertEquals(false, bus.taken("hang-1")); assertNull(bus.peekResult("hang-1"))
        assertEquals(Phase.NOT_TAKEN, CmdTrack.phase(5, bus.peekResult("hang-1"), bus.taken("hang-1")))
        clock += 5
        assertEquals(true, bus.taken("hang-1"))
        assertEquals(Phase.WORKING, CmdTrack.phase(10, bus.peekResult("hang-1"), bus.taken("hang-1")))
        clock += 25
        assertEquals(Phase.DONE, CmdTrack.phase(35, bus.peekResult("hang-1"), bus.taken("hang-1")))
        assertEquals("atlas-shop: stopped", bus.peekResult("hang-1")?.text)
        assertEquals(SessionState.IDLE, bus.fetchState().sessions.first { it.name == "atlas-shop" }.state)
        sending.cancel()
    }
}
