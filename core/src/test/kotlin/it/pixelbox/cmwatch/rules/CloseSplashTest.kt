package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Franz, 08/10 18:06: chiusa una sessione, la sua pagina spariva e compariva la home; «non voglio pagine che scompaiono».
// Variante A (18:29): un pannello sopra la pagina dice che si sta chiudendo, poi che è chiusa, e dopo 3 s si torna alla home.
class CloseSplashTest {
    private fun s(n: String, st: SessionState, since: Long = 100) = Session(id = n, name = n, account = "personale", project = n, state = st, since = since)
    private val sent = CloseSplash.Leaving("kb", sentAt = 1_000, cmd = "c1")

    @Test fun aLiveSessionWithoutExitShowsNothing() =
        assertNull(CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), leaving = null, lastSeen = null, now = 1_010))

    @Test fun afterExitTheSessionIsClosingUntilThePcConfirms() {
        val p = CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), sent, lastSeen = null, now = 1_002, delivered = false)
        assertEquals(CloseSplash.Phase.Closing("kb", sentAt = 1_000, delivered = false, slow = false), p)
        val d = CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), sent, lastSeen = null, now = 1_004, delivered = true)
        assertEquals(CloseSplash.Phase.Closing("kb", sentAt = 1_000, delivered = true, slow = false), d)
    }

    // Dal vivo 08/10 19:29: dopo /exit Claude Code chiedeva cosa fare dei lavori in background, e il pannello copriva la
    // domanda. Finché la sessione chiede, il pannello si fa da parte; risposto, torna.
    @Test fun aQuestionAfterExitShowsTheQuestionNotThePanel() {
        val q = it.pixelbox.cmwatch.contract.Question(
            id = "q1", kind = it.pixelbox.cmwatch.contract.QuestionKind.ASK, text = "Background tasks are running",
            options = listOf(it.pixelbox.cmwatch.contract.Option(3, "Stay")), tier = it.pixelbox.cmwatch.contract.Tier.LOW, askedAt = 1_010,
        )
        assertNull(CloseSplash.of("kb", listOf(s("kb", SessionState.WAITING).copy(question = q)), sent, lastSeen = null, now = 1_030, delivered = true))
    }

    @Test fun aClosingThatTakesLongerThanUsualSaysSo() {
        val p = CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), sent, lastSeen = null, now = 1_000 + CloseSplash.SLOW_S, delivered = true)
        assertEquals(true, (p as CloseSplash.Phase.Closing).slow)
    }

    @Test fun goneAfterMyExitIsClosedByMeAtItsLastSighting() =
        assertEquals(
            CloseSplash.Phase.Closed("kb", at = 1_003, byMe = true),
            CloseSplash.of("kb", listOf(s("kb", SessionState.GONE, since = 1_003)), sent, lastSeen = null, now = 1_005),
        )

    @Test fun vanishedAfterMyExitIsClosedByMe() =
        assertEquals(
            CloseSplash.Phase.Closed("kb", at = 1_003, byMe = true),
            CloseSplash.of("kb", emptyList(), sent, lastSeen = s("kb", SessionState.GONE, since = 1_003), now = 1_005),
        )

    // Chiusa per conto suo resta con «Riapri» (03/10 19:57); quando esce dallo stato, il pannello dice che non l'hai chiusa tu.
    @Test fun goneOnItsOwnStaysAsAPage() =
        assertNull(CloseSplash.of("kb", listOf(s("kb", SessionState.GONE)), leaving = null, lastSeen = null, now = 1_005))

    @Test fun vanishedOnItsOwnIsClosedNotByMe() =
        assertEquals(
            CloseSplash.Phase.Closed("kb", at = 1_005, byMe = false),
            CloseSplash.of("kb", emptyList(), leaving = null, lastSeen = s("kb", SessionState.IDLE, since = 900), now = 1_005),
        )

    @Test fun anotherSessionsExitDoesNotCount() =
        assertNull(CloseSplash.of("atlas", listOf(s("atlas", SessionState.IDLE)), sent, lastSeen = null, now = 1_005))

    @Test fun neverSeenAndMissingFallsBackToNow() =
        assertEquals(CloseSplash.Phase.Closed("kb", at = 1_005, byMe = false), CloseSplash.of("kb", emptyList(), null, lastSeen = null, now = 1_005))

    // app-chiudi-esito-04-11: il Chiudi rifiutato dice perché, e il pannello resta finché non lo togli. Gli stessi casi della web.
    @Test fun aRefusedExitShowsTheReasonInsteadOfClosing() =
        assertEquals(CloseSplash.Phase.Refused("kb", "resta aperta"), CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), sent.copy(refused = "resta aperta"), null, now = 1_002))

    @Test fun goneAnywayAfterARefusalIsClosed() =
        assertEquals(CloseSplash.Phase.Closed("kb", at = 1_003, byMe = true), CloseSplash.of("kb", listOf(s("kb", SessionState.GONE, 1_003)), sent.copy(refused = "x"), null, now = 1_005))

    @Test fun readsTheBackgroundRefusalTheRelaySendsToday() =
        assertEquals(listOf(CloseSplash.Refusal("kb", "background", "a job is running in the background")), CloseSplash.refusals("ERR kb not closed: a job is running in the background"))

    @Test fun readsOnlyTheRefusedLinesOfACloseWithNames() =
        assertEquals(listOf(CloseSplash.Refusal("atlas-shop", "busy", "it is working")), CloseSplash.refusals("closed: field-notes\natlas-shop not closed: it is working"))

    @Test fun anUnknownReasonHasNoKind() =
        assertEquals(listOf(CloseSplash.Refusal("kb", null, "something else")), CloseSplash.refusals("kb not closed: something else"))

    // Contratto 1.120: «Chiudi quando finisce» e «Chiudi comunque» solo dove il relay arma la chiusura.
    @Test fun aQuestionWaitingCanBeArmedAnAttachedSessionCannot() {
        assertEquals(true, CloseSplash.refusals("kb not closed: a question is waiting").single().armable)
        assertEquals(false, CloseSplash.refusals("kb not closed: it is attached to a terminal").single().armable)
    }

    @Test fun aForcedCloseIsNotSlowBeforeIts180s() {
        val f = sent.copy(force = true)
        assertEquals(false, (CloseSplash.of("kb", listOf(s("kb", SessionState.BUSY)), f, null, now = 1_000 + CloseSplash.FORCE_S) as CloseSplash.Phase.Closing).slow)
        assertEquals(true, (CloseSplash.of("kb", listOf(s("kb", SessionState.BUSY)), f, null, now = 1_000 + CloseSplash.FORCE_S + CloseSplash.SLOW_S) as CloseSplash.Phase.Closing).slow)
    }
}
