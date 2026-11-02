package it.pixelbox.cmwatch.rules

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

/** Gli stessi casi di leave.test.ts: segno subito, comando subito, uscita dopo la conferma, ritorno se il PC dice no. */
class LeaveTest {
    private class Rec : Leave.Hooks {
        val log = mutableListOf<String>()
        override fun pick(ids: List<String>, choice: String?) { log += "pick ${ids.joinToString(",")} $choice" }
        override fun hide(ids: List<String>, hidden: Boolean) { log += "hide ${ids.joinToString(",")} $hidden" }
    }

    @Test fun signNowSendNowLeaveAfterTheConfirmWithoutWaitingForThePc() = runTest {
        val r = Rec(); val pc = CompletableDeferred<Boolean>()
        val done = async { Leave.run(listOf("7"), "ok", { r.log += "send"; pc.await() }, r) { yield() } }
        repeat(5) { yield() }
        assertEquals(listOf("pick 7 ok", "send", "hide 7 true"), r.log)
        pc.complete(true)
        assertEquals(true, done.await())
        assertEquals(listOf("pick 7 ok", "send", "hide 7 true"), r.log)
    }

    @Test fun thePcSaysNoTheCardComesBack() = runTest {
        val r = Rec()
        assertEquals(false, Leave.run(listOf("7", "8"), "ok", { repeat(4) { yield() }; false }, r) { yield() })
        assertEquals(listOf("hide 7,8 false", "pick 7,8 null"), r.log.takeLast(2))
    }

    @Test fun aNoDuringTheConfirmNeverHides() = runTest {
        val r = Rec()
        assertEquals(false, Leave.run(listOf("7"), "ok", { false }, r) { yield(); yield() })
        assertEquals(listOf("pick 7 ok", "pick 7 null"), r.log)
    }
}
