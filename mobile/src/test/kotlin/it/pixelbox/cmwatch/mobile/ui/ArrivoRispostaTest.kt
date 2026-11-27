package it.pixelbox.cmwatch.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** app-messaggio-master-effetto-27-11 (opzione E, prima B): quali risposte arrivano e come si chiude una corsa. */
class ArrivoRispostaTest {
    // La chat è ferma al suo posto e la sessione è stata vista lavorare dal turno cominciato a [from].
    private fun open(from: Long = 1_000) = StreamGate().apply { settle(); working(from) }

    @Test fun gateLetsThroughOnlyRepliesArrivedAfterOpening() {
        val g = open()
        assertFalse("before priming nothing arrives", g.claim("a", 1_005))
        g.prime(listOf("b"))
        assertFalse("already there at opening", g.claim("b", 1_005))
        assertTrue(g.claim("c", 1_005))
        assertFalse("once only", g.claim("c", 1_005))
    }

    @Test fun aReplyBornBeforeTheTurnSeenOpenDoesNotArrive() {
        val g = open(from = 1_000)
        g.prime(emptyList())
        assertFalse("nata prima del turno visto aperto, in una pagina che si carica dopo", g.claim("old", 990))
        assertFalse("senza ora non è nuova", g.claim("noat", null))
        assertTrue(g.claim("new", 1_005))
    }

    // Franz 27/11 15:17: aprendo la sessione di un compito appena finito la risposta passava per nuova. Prima che la chat sia ferma, o
    // senza lavoro visto a chat ferma, niente arriva con un'animazione; una risposta vista una volta resta di prima.
    @Test fun nothingArrivesBeforeTheChatSettlesOrWithoutWork() {
        val g = StreamGate().apply { prime(emptyList()) }
        g.working(1_000)
        assertFalse("la chat va ancora al suo posto", g.claim("a", 1_005))
        g.settle()
        assertFalse(g.live)
        assertFalse("la sessione non è stata vista lavorare a chat ferma", g.claim("b", 1_005))
        g.working(1_000)
        assertTrue(g.live)
        assertFalse("vista prima, resta di prima", g.claim("a", 1_005))
        assertTrue(g.claim("c", 1_005))
    }

    @Test fun theFirstTurnSeenIsTheBoundary() {
        val g = StreamGate().apply { prime(emptyList()); settle(); working(null) }
        assertFalse("un turno senza inizio non conta", g.live)
        g.working(1_000)
        g.working(2_000)
        assertFalse("nata prima del primo turno visto", g.claim("x", 999))
        assertTrue(g.claim("y", 1_500))
    }

    // Finché la corsa non parte la risposta è trasparente; una risposta nuova chiude subito quella in corsa, tutta intera.
    @Test fun aNewArrivalFinishesTheRunningOne() {
        val g = StreamGate()
        val a = Arrival("a")
        g.publish(a)
        assertTrue(a.running)
        assertEquals(0f, a.alpha, 0f)
        assertEquals(Arrival.CHECK_FROM, a.check, 0f)
        val b = Arrival("b")
        g.publish(b)
        assertFalse(a.running)
        assertEquals(1f, a.alpha, 0f)
        assertSame(b, g.current)
        assertTrue(b.running)
    }

    // Un dito, o la fine della corsa: tutto intero e fermo, lampo spento, spunta a grandezza piena.
    @Test fun finishShowsEverything() {
        val a = Arrival("a")
        a.alpha = .3f
        a.flash = .7f
        a.check = .85f
        a.finish()
        assertEquals(1f, a.alpha, 0f)
        assertEquals(0f, a.flash, 0f)
        assertEquals(1f, a.check, 0f)
        assertFalse(a.running)
    }
}
