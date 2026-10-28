package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test

/** La sezione «Sistema» della home (contratto 1.90, Franz 28/10 13:19). */
class SystemRulesTest {
    private val red = ContractJson.decodeState(Fixtures.stateStale)

    @Test fun system_advice_redStateCarriesLevelConsumersAndFourAdvice() {
        val s = red.system!!
        assertEquals("red", s.level)
        assertEquals(listOf("orphan_proc", "heavy_job", "low_memory", "orphan"), s.advice.map { it.kind })
        assertEquals(SystemConsumer("claude", 5, 142.5, 2310), s.consumers.first())
        assertNull(s.advice[1].action)
    }

    @Test fun olderRelayHasNoSystem() {
        assertNull(ContractJson.decodeState(Fixtures.stateIdle).system)
        assertNull(ContractJson.decodeState(Fixtures.stateQuestion).system)
    }

    @Test fun openByItselfInYellowAndRed_tapHoldsUntilTheLevelChanges() {
        assertFalse(SystemRules.isOpen("green", null))
        assertTrue(SystemRules.isOpen("yellow", null))
        assertTrue(SystemRules.isOpen("red", null))
        assertFalse(SystemRules.isOpen("red", SystemRules.Manual("red", false)))
        assertTrue(SystemRules.isOpen("red", SystemRules.Manual("yellow", false)))
        assertTrue(SystemRules.isOpen("green", SystemRules.Manual("green", true)))
    }

    @Test fun buttonOnlyWithAnActionTheRelayRuns() {
        val (kill, job) = red.system!!.advice
        assertEquals(CmdOp.SYS_KILL, SystemRules.opOf(kill, red.ops))
        assertNull(SystemRules.opOf(job, red.ops))
        assertNull(SystemRules.opOf(kill, listOf("close")))
        assertNull(SystemRules.opOf(kill, null))
        val close = kill.copy(action = SystemAction("close", "a,b", "Chiudi"))
        assertEquals(CmdOp.CLOSE, SystemRules.opOf(close, listOf("close")))
        assertNull(SystemRules.opOf(kill.copy(action = SystemAction("reboot", "", "Riavvia")), listOf("reboot")))
    }

    @Test fun figuresWithOneDecimalAtMost_commaInItalian() {
        val s = red.system!!
        assertEquals("2,2", SystemRules.num(s.loadPerCore, comma = true))
        assertEquals("0,8", SystemRules.num(s.memAvailableMb / 1024.0, comma = true))
        assertEquals("6.5", SystemRules.num(s.memTotalMb / 1024.0, comma = false))
        assertEquals("1 GB", SystemRules.size(1061, comma = true))
        assertEquals("2,3 GB", SystemRules.size(2310, comma = true))
        assertEquals("60 MB", SystemRules.size(60, comma = true))
        assertEquals(143, SystemRules.cpu(142.5))
    }
}
