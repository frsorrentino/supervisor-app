package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.Attention
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.Outcome
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
    private val w = AlertEvents.Words("continua su %s", "riprendi alle %s", "passa a un'altra sessione", "continua con %s")

    @Test fun taskCheckIsGreenOrRed() {
        val e = Event("k", EventKind.TASK_CHECK, "atlas-shop", null, 1, "✓ t-1: check green", "title", "t-1")
        assertTrue(AlertEvents.green(e))
        assertFalse(AlertEvents.green(e.copy(title = "✗ t-1: check red")))
        // Contratto 1.78: l'esito sta in `ok` e il titolo non porta più il segno; `ok` vince sul titolo.
        assertTrue(AlertEvents.green(e.copy(title = "t-1: check green", ok = true)))
        assertFalse(AlertEvents.green(e.copy(title = "t-1: check red", ok = false)))
        assertFalse(AlertEvents.green(e.copy(ok = false)))
    }

    // Franz, 22/10 13:36: niente emoji nelle notifiche: titolo e corpo da mostrare passano dal filtro, il titolo grezzo resta.
    @Test fun theTitleAndBodyToShowCarryNoSymbols() {
        assertEquals("atlas-shop: limite 5h finito, reset alle 18:00", limit.shareTitle)
        assertEquals("⛔ atlas-shop: limite 5h finito, reset alle 18:00", limit.title)
        val e = limit.copy(body = "🚀 deploy fatto", preview = it.pixelbox.cmwatch.contract.EventPreview("✓ x", "token ••• 🎉"))
        assertEquals("x", e.shareTitle); assertEquals("token •••", e.shareBody)
    }

    @Test fun quotaLimitOffersTheRelaysWays() {
        val ways = AlertEvents.ways(limit, w) { "18:00" }
        assertEquals(listOf("continua su work", "riprendi alle 18:00", "passa a un'altra sessione"), ways.map { it.label })
        assertEquals(listOf("other_account", "at_reset", null), ways.map { it.arg })
        // Senza l'altro account o l'ora del reset quel tasto non c'è; un'azione sconosciuta si salta.
        assertEquals(listOf("pass"), AlertEvents.ways(limit.copy(otherAccount = null, resetAt = null, actions = listOf("other_account", "at_reset", "pass", "x")), w) { "" }.map { it.action })
        // Limite di un modello: il tasto «continua con Opus 5.5», arg other_model; senza il campo il tasto non c'è.
        val model = limit.copy(window = "model", otherAccount = null, otherModel = "Opus 5.5", actions = listOf("other_model", "at_reset"))
        assertEquals(listOf("continua con Opus 5.5" to "other_model", "riprendi alle 18:00" to "at_reset"), AlertEvents.ways(model, w) { "18:00" }.map { it.label to it.arg })
        assertEquals(listOf("at_reset"), AlertEvents.ways(model.copy(otherModel = null), w) { "18:00" }.map { it.action })
        assertEquals("pass:ledger-api", AlertEvents.passArg("ledger-api"))
        assertFalse("atlas-shop" in AlertEvents.passTargets(st.sessions, "atlas-shop"))
    }

    @Test fun theLimitStaysUntilTheResetOrANewTurn() {
        val idle = atlas.copy(state = SessionState.IDLE, turnStarted = 900, outcome = null)
        assertEquals(limit, AlertEvents.openLimit(listOf(limit), idle, 2000))
        assertNull(AlertEvents.openLimit(listOf(limit), idle, 5000))
        assertNull(AlertEvents.openLimit(listOf(limit), idle.copy(state = SessionState.BUSY, turnStarted = 1200), 2000))
        assertNull(AlertEvents.openLimit(listOf(limit), idle.copy(name = "other"), 2000))
    }

    // 16/10 (Franz alle 16:03, «va corretto ora»): «master: turno fallito» con Continua tornava sotto risposte riuscite,
    // perché a turno finito turn_started è null. Si chiude anche con un esito dopo l'evento, senza la voce in attention,
    // con settled_at.
    private val failed = Event("k", EventKind.TURN_FAILED, "atlas-shop", null, 1000, "⚠ atlas-shop: turn failed — 529", ref = "atlas-shop",
        reason = "529", continueText = "carry on")
    private val rested = atlas.copy(state = SessionState.IDLE, turnStarted = null, outcome = null)
    private fun done(at: Long) = rested.copy(outcome = Outcome("ok", "ok", at))
    private val voice = listOf(Attention("turn_failed", "atlas-shop", "atlas-shop", "⚠", 1000))

    @Test fun `turno fallito chiuso da un esito dopo l'evento`() {
        assertEquals(failed, AlertEvents.openFailure(listOf(failed), done(900), 1060))
        assertNull(AlertEvents.openFailure(listOf(failed), done(1100), 1160))
    }

    @Test fun `turno fallito chiuso quando attention non ha piu la voce`() {
        assertEquals(failed, AlertEvents.openFailure(listOf(failed), rested, 1060, voice))
        assertNull(AlertEvents.openFailure(listOf(failed), rested, 1060, emptyList()))
        assertNull(AlertEvents.openFailure(listOf(failed), rested, 1060, listOf(voice[0].copy(ref = "ledger-api"))))
    }

    @Test fun `turno fallito chiuso con settled_at o senza continue_text`() {
        assertNull(AlertEvents.openFailure(listOf(failed.copy(settledAt = 1100)), rested, 1160))
        assertNull(AlertEvents.openFailure(listOf(failed.copy(continueText = null)), rested, 1160))
    }

    @Test fun `turno fallito chiuso, quota finita chiusa allo stesso modo`() {
        assertEquals(limit, AlertEvents.openLimit(listOf(limit), done(900), 2000))
        assertNull(AlertEvents.openLimit(listOf(limit), done(1100), 2000))
        assertEquals(limit, AlertEvents.openLimit(listOf(limit), rested, 2000, listOf(voice[0].copy(kind = "quota_limit"))))
        assertNull(AlertEvents.openLimit(listOf(limit), rested, 2000, voice))
        assertNull(AlertEvents.openLimit(listOf(limit.copy(settledAt = 1500)), rested, 2000))
    }
}
