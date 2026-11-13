package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.ContractJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Contratto 1.145; gli stessi casi di web/src/lib/maxSessions.test.ts. */
class MaxSessionsTest {
    @Test fun onlyWhenTheRelayOffersTheOp() {
        assertEquals("max_sessions_set", MaxSessions.OP)
        assertEquals(listOf(true, false, false), listOf(MaxSessions.canSet(listOf("kpi", "max_sessions_set")), MaxSessions.canSet(listOf("kpi")), MaxSessions.canSet(null)))
    }

    @Test fun fromOneToTwenty() {
        assertEquals(1 to 20, MaxSessions.MIN to MaxSessions.MAX)
        assertEquals(listOf(false, true, true, true, false), listOf(0, 1, 7, 20, 21).map(MaxSessions::valid))
    }

    @Test fun choicesKeepAValueOutOfRange() {
        assertEquals((1..20).toList(), MaxSessions.choices(5))
        assertEquals(20, MaxSessions.choices(null).size)
        assertEquals(30, MaxSessions.choices(30).last())
        assertEquals(0, MaxSessions.choices(0).first())
    }

    @Test fun theRelaysAnswer() {
        assertEquals(7, MaxSessions.answer("{\"max_sessions\": 7}"))
        assertNull(MaxSessions.answer("{}")); assertNull(MaxSessions.answer("ok")); assertNull(MaxSessions.answer("{\"max_sessions\": \"7\"}"))
    }

    // L'op sul filo ha il nome del contratto.
    @Test fun theOpOnTheWire() {
        assertEquals("\"max_sessions_set\"", ContractJson.json.encodeToString(CmdOp.serializer(), CmdOp.MAX_SESSIONS_SET))
    }
}
