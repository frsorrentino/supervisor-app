package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.NeedsYou
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.TurnBlocking
import it.pixelbox.cmwatch.rules.Wake.Action
import it.pixelbox.cmwatch.rules.Wake.NotifyKind
import org.junit.Assert.*
import org.junit.Test

class WakeTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    // Dalla 1.84 state-2 ha un needs_you (atlas-shop): qui si parte da una sessione senza.
    private val idle = ContractJson.decodeState(Fixtures.stateIdle).let { st -> st.copy(sessions = st.sessions.map { it.copy(needsYou = null) }) }

    @Test fun newQuestionNotifies() {
        val a = Wake.plan(idle, q)
        assertTrue(a.contains(Action.Notify(NotifyKind.QUESTION, "ledger-api")))
        assertTrue(a.last() == Action.RefreshComplications && a[a.size - 2] == Action.RefreshTile)
    }

    @Test fun sameQuestionDoesNotNotifyTwice() {
        assertTrue(Wake.plan(q, q).none { it is Action.Notify })
    }

    @Test fun firstStateNotifiesOpenQuestions() {
        assertTrue(Wake.plan(null, q).contains(Action.Notify(NotifyKind.QUESTION, "ledger-api")))
    }

    @Test fun newOutcomeOnlyForTheFollowedSession() {
        // atlas-shop non è seguita in fixture 1: il suo esito nuovo non notifica
        val prev = q.copy(sessions = q.sessions.map { if (it.name == "atlas-shop") it.copy(outcome = null) else it })
        assertTrue(Wake.plan(prev, q).none { it == Action.Notify(NotifyKind.OUTCOME, "atlas-shop") })
        val followed = q.copy(sessions = q.sessions.map { if (it.name == "atlas-shop") it.copy(followed = true) else it })
        assertTrue(Wake.plan(prev, followed).contains(Action.Notify(NotifyKind.OUTCOME, "atlas-shop")))
    }

    @Test fun goneNotifiesOnce() {
        val prev = q.copy(sessions = q.sessions.map { if (it.name == "orbit-docs") it.copy(state = SessionState.IDLE) else it })
        assertTrue(Wake.plan(prev, q).contains(Action.Notify(NotifyKind.GONE, "orbit-docs")))
        assertTrue(Wake.plan(q, q).none { it == Action.Notify(NotifyKind.GONE, "orbit-docs") })
        // sparita anche se non c'è più nella lista
        val vanished = q.copy(sessions = q.sessions.filter { it.name != "field-notes" })
        assertTrue(Wake.plan(q, vanished).contains(Action.Notify(NotifyKind.GONE, "field-notes")))
    }

    // Risposta data dal terminale o dal telefono (Franz, 14/09 22:10): la domanda sparisce dallo stato e la sua
    // notifica sull'orologio si chiude, anche con l'app in primo piano.
    @Test fun aQuestionAnsweredElsewhereClosesItsNotification() {
        val answered = q.copy(sessions = q.sessions.map { if (it.name == "ledger-api") it.copy(state = SessionState.BUSY, question = null) else it })
        assertTrue(Wake.plan(q, answered).contains(Action.CloseQuestion("ledger-api")))
        assertTrue(Wake.plan(q, q).none { it is Action.CloseQuestion })
        assertTrue(Wake.plan(idle, answered).none { it is Action.CloseQuestion })
    }

    @Test fun quotaThresholdCrossedOnce() {
        val hot = idle.copy(quota = idle.quota + ("personale" to QuotaAccount(h5 = 96, w7 = 38, resetW7 = 1L)))
        assertTrue(Wake.plan(idle, hot).contains(Action.Notify(NotifyKind.QUOTA, "personale")))
        assertTrue(Wake.plan(hot, hot).none { it is Action.Notify })
    }

    // Contratto 1.68: ferma per Franz (needs_you o turno bloccante), un avviso quando compare o cambia.
    @Test fun aSessionStoppedForFranzNotifiesOnce() {
        val name = idle.sessions.first().name
        fun with(f: (Session) -> Session) =
            idle.copy(sessions = idle.sessions.map { if (it.name == name) f(it) else it })
        val asked = with { it.copy(needsYou = NeedsYou("login su win", 100)) }
        assertTrue(Wake.plan(idle, asked).contains(Action.Notify(NotifyKind.STOPPED, name)))
        assertTrue(Wake.plan(asked, asked).none { it is Action.Notify })
        val again = with { it.copy(needsYou = NeedsYou("il codice", 200)) }
        assertTrue(Wake.plan(asked, again).contains(Action.Notify(NotifyKind.STOPPED, name)))
        val blocking = with { it.copy(turnBlocking = TurnBlocking("ok al deploy", 300)) }
        assertTrue(Wake.plan(idle, blocking).contains(Action.Notify(NotifyKind.STOPPED, name)))
        // All'avvio dell'app non si risuona per chi era già ferma: ci pensano i promemoria del relay.
        assertTrue(Wake.plan(null, asked).none { it == Action.Notify(NotifyKind.STOPPED, name) })
    }
}
