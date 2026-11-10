package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La lettura che non si arrende (app-monitoraggi-attesa-10-11): dal log del relay del 10/11 il comando `monitors` restava in fila 23-51 s;
 * la lettura di prima aspettava 30 s e si arrendeva, e la sezione Monitoraggi spariva. Tempo finto di runTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatientReadTest {
    private class Relay {
        val results = MutableStateFlow<Map<String, CmdResult>>(emptyMap())
        val sent = mutableListOf<String>()
        val forgotten = mutableListOf<String>()
        fun send(): String = "c${sent.size}".also { sent += it }
        fun answer(id: String, ok: Boolean, text: String) { results.value = results.value + (id to CmdResult(id, ok, text, 0)) }
    }

    @Test fun anAnswerAfterFortyFiveSecondsIsUsed() = runTest {
        val r = Relay()
        val read = async { PatientRead.read({ r.send() }, r.results, { r.forgotten += it }, giveUpOnRefusal = false) }
        runCurrent()
        advanceTimeBy(45_000); r.answer("c0", true, "{\"items\": []}"); runCurrent()
        assertEquals("la risposta al primo comando, arrivata dopo i 30 s", "{\"items\": []}", read.await())
        assertEquals("un secondo tentativo dopo i primi 30 s", listOf("c0", "c1"), r.sent)
        assertTrue("dimenticati tutti e due", r.forgotten.containsAll(listOf("c0", "c1")))
    }

    @Test fun itKeepsTryingUntilAnAnswerComes() = runTest {
        val r = Relay()
        val read = async { PatientRead.read({ r.send() }, r.results, { r.forgotten += it }, giveUpOnRefusal = false) }
        runCurrent()
        advanceTimeBy(3 * 60_000L); runCurrent()
        assertFalse("dopo tre minuti senza risposta non si è arreso", read.isCompleted)
        assertTrue("più tentativi (${r.sent})", r.sent.size >= 4)
        r.answer(r.sent.last(), true, "ok"); runCurrent()
        assertEquals("ok", read.await())
    }

    @Test fun aRefusedListIsTriedAgainAndAGoneCardStops() = runTest {
        val r = Relay()
        val list = async { PatientRead.read({ r.send() }, r.results, { r.forgotten += it }, giveUpOnRefusal = false) }
        runCurrent(); r.answer("c0", false, "relay busy"); runCurrent()
        assertFalse(list.isCompleted)
        advanceTimeBy(6_000); runCurrent()
        r.answer("c1", true, "ok"); runCurrent()
        assertEquals("ok", list.await())
        val r2 = Relay()
        val one = async { PatientRead.read({ r2.send() }, r2.results, {}, giveUpOnRefusal = true) }
        runCurrent(); r2.answer("c0", false, "vecchio: questo monitoraggio non c'è più"); runCurrent()
        assertNull(one.await())
    }
}
