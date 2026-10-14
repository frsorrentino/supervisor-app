package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test

/** Contratti 1.57 e 1.58, con i testi della specifica di supervisor del 14/10 (le fixture arrivano con la 0.8.20). */
class AlertEventsTest {
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)
    private val atlas = st.sessions.single { it.name == "atlas-shop" }
    private val limit = ContractJson.json.decodeFromString(Event.serializer(), """{"key":"k1","kind":"quota_limit","session":"atlas-shop",
        "account":"personal","ts":1000,"title":"⛔ atlas-shop: limite 5h finito, reset alle 18:00","body":"","ref":"atlas-shop",
        "reset_at":5000,"window":"5h","other_account":"work","actions":["other_account","at_reset","pass"]}""")
    private val w = AlertEvents.Words("continua su %s", "riprendi alle %s", "passa a un'altra sessione")

    @Test fun taskCheckIsGreenOrRed() {
        val e = Event("k", EventKind.TASK_CHECK, "atlas-shop", null, 1, "✓ t-1: check green", "title", "t-1")
        assertTrue(AlertEvents.green(e))
        assertFalse(AlertEvents.green(e.copy(title = "✗ t-1: check red")))
    }

    @Test fun quotaLimitOffersTheRelaysWays() {
        val ways = AlertEvents.ways(limit, w) { "18:00" }
        assertEquals(listOf("continua su work", "riprendi alle 18:00", "passa a un'altra sessione"), ways.map { it.label })
        assertEquals(listOf("other_account", "at_reset", null), ways.map { it.arg })
        // Senza l'altro account o l'ora del reset quel tasto non c'è; un'azione sconosciuta si salta.
        assertEquals(listOf("pass"), AlertEvents.ways(limit.copy(otherAccount = null, resetAt = null, actions = listOf("other_account", "at_reset", "pass", "x")), w) { "" }.map { it.action })
        assertEquals("pass:ledger-api", AlertEvents.passArg("ledger-api"))
        assertFalse("atlas-shop" in AlertEvents.passTargets(st.sessions, "atlas-shop"))
    }

    @Test fun theLimitStaysUntilTheResetOrANewTurn() {
        val idle = atlas.copy(state = SessionState.IDLE, turnStarted = 900)
        assertEquals(limit, AlertEvents.openLimit(listOf(limit), idle, 2000))
        assertNull(AlertEvents.openLimit(listOf(limit), idle, 5000))
        assertNull(AlertEvents.openLimit(listOf(limit), idle.copy(state = SessionState.BUSY, turnStarted = 1200), 2000))
        assertNull(AlertEvents.openLimit(listOf(limit), idle.copy(name = "other"), 2000))
    }
}
