package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.rules.MasterLaunch.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La card «Avvia la master» (app-master-avvia-19-11). */
class MasterLaunchTest {
    private val now = 10_000L

    @Test fun withAFreshStateOneTapLaunchesIt() {
        val c = MasterLaunch.card(Freshness.Fresh, null, null, now)
        assertEquals(Kind.LAUNCH, c.kind); assertTrue(c.button)
        assertEquals(Kind.LAUNCH, MasterLaunch.card(Freshness.Slow(40), null, null, now).kind)
    }

    @Test fun withAnOldStateThePcIsOffAndThereIsNoButton() {
        val c = MasterLaunch.card(Freshness.Stale(4), null, null, now)
        assertEquals(Kind.PC_OFF, c.kind); assertFalse(c.button)
        // Anche dopo il tocco: nessuno l'ha ricevuto.
        assertEquals(Kind.PC_OFF, MasterLaunch.card(Freshness.Stale(4), now - 5, null, now).kind)
        assertEquals(Kind.PC_OFF, MasterLaunch.card(null, null, null, now).kind)
    }

    @Test fun afterTheTapItIsStartingUntilTheWindowEnds() {
        val c = MasterLaunch.card(Freshness.Fresh, now - 10, null, now)
        assertEquals(Kind.STARTING, c.kind); assertFalse(c.button)
        assertEquals(Kind.LAUNCH, MasterLaunch.card(Freshness.Fresh, now - MasterLaunch.STARTING_S, null, now).kind)
    }

    @Test fun aRefusalShowsTheRelayTextAndLetsYouRetry() {
        val c = MasterLaunch.card(Freshness.Fresh, now - 10, "master is not a closed session", now)
        assertEquals(Kind.FAILED, c.kind); assertTrue(c.button)
        assertEquals("master is not a closed session", c.error)
    }
}
