package it.pixelbox.cmwatch.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** app-messaggio-master-effetto-27-11 (opzione B): quali risposte arrivano e come si chiude una corsa. */
class ArrivoRispostaTest {
    @Test fun gateLetsThroughOnlyRepliesArrivedAfterOpening() {
        val g = StreamGate()
        assertFalse("before priming nothing arrives", g.claim("a"))
        g.prime(listOf("b"))
        assertFalse("already there at opening", g.claim("b"))
        assertTrue(g.claim("c"))
        assertFalse("once only", g.claim("c"))
    }

    @Test fun aReplyBornBeforeTheOpeningDoesNotArrive() {
        val g = StreamGate(openedAt = 1_000)
        g.prime(emptyList())
        assertFalse("arrivata prima dell'apertura, in una lista che si carica dopo", g.claim("old", 990))
        assertTrue(g.claim("new", 1_005))
    }

    // Finché la corsa non parte la risposta è nascosta; una risposta nuova chiude subito quella in corsa, tutta scoperta.
    @Test fun aNewArrivalFinishesTheRunningOne() {
        val g = StreamGate()
        val a = Arrival("a")
        g.publish(a)
        assertTrue(a.running)
        assertTrue(a.k.isInfinite())
        val b = Arrival("b")
        g.publish(b)
        assertFalse(a.running)
        assertEquals(0f, a.k, 0f)
        assertSame(b, g.current)
        assertTrue(b.running)
    }

    @Test fun finishUncoversEverything() {
        val a = Arrival("a")
        a.k = 120f
        a.body = 0f
        a.finish()
        assertEquals(0f, a.k, 0f)
        assertEquals(1f, a.body, 0f)
        assertFalse(a.running)
    }
}
