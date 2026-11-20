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

    // Relay 0.8.196 (app-chiudi-scheda-20-11): l'esito della chiusura in italiano; l'inglese resta per i relay di prima.
    @Test fun readsTheItalianRefusalsOfTheRelay() {
        assertEquals(
            listOf(
                CloseSplash.Refusal("atlas-shop", "busy", "sta lavorando"), CloseSplash.Refusal("kb", "question", "c'è una domanda in attesa"),
                CloseSplash.Refusal("notes", "background", "c'è un lavoro in sottofondo"), CloseSplash.Refusal("old", "gone", "non è in esecuzione"),
                CloseSplash.Refusal("stuck", null, "non si riesce a fermarla"),
            ),
            CloseSplash.refusals(
                "chiusa: field-notes\natlas-shop non chiusa: sta lavorando\nkb non chiusa: c'è una domanda in attesa\n" +
                    "notes non chiusa: c'è un lavoro in sottofondo\nold non chiusa: non è in esecuzione\nstuck non chiusa: non si riesce a fermarla",
            ),
        )
        assertEquals(listOf(true, true, true), CloseSplash.refusals("a non chiusa: sta lavorando\nb non chiusa: c'è una domanda in attesa\nc non chiusa: c'è un lavoro in sottofondo").map { it.armable })
        assertEquals("gone", CloseSplash.refusals("old not closed: it is not running").single().kind)
        assertEquals(emptyList<CloseSplash.Refusal>(), CloseSplash.refusals("kb si chiude appena finisce"))
    }

    @Test fun aForcedCloseIsNotSlowBeforeIts180s() {
        val f = sent.copy(force = true)
        assertEquals(false, (CloseSplash.of("kb", listOf(s("kb", SessionState.BUSY)), f, null, now = 1_000 + CloseSplash.FORCE_S) as CloseSplash.Phase.Closing).slow)
        assertEquals(true, (CloseSplash.of("kb", listOf(s("kb", SessionState.BUSY)), f, null, now = 1_000 + CloseSplash.FORCE_S + CloseSplash.SLOW_S) as CloseSplash.Phase.Closing).slow)
    }

    // Contratto 1.155 (app-chiudi-salva-19-11): «Salva e chiudi» segue i passi del relay e dice com'è finita. Come la web.
    private val saving = sent.copy(save = true)
    private fun armed(mode: String) = s("kb", SessionState.IDLE).copy(closing = it.pixelbox.cmwatch.contract.Closing(mode, at = 1_001, deadline = if (mode == "save") 1_181 else null))

    @Test fun closingModeSaveIsWritingTheHandoffExitIsClosing() {
        assertEquals(null, (CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), saving, null, 1_002) as CloseSplash.Phase.Closing).step)
        assertEquals("save", (CloseSplash.of("kb", listOf(armed("save")), saving, null, 1_010) as CloseSplash.Phase.Closing).step)
        assertEquals("exit", (CloseSplash.of("kb", listOf(armed("exit")), saving, null, 1_020) as CloseSplash.Phase.Closing).step)
        assertNull((CloseSplash.of("kb", listOf(armed("save")), sent, null, 1_010) as CloseSplash.Phase.Closing).step)
    }

    @Test fun aSaveIsNotSlowBeforeIts180Seconds() {
        assertEquals(false, (CloseSplash.of("kb", listOf(armed("save")), saving, null, 1_000 + CloseSplash.FORCE_S) as CloseSplash.Phase.Closing).slow)
        assertEquals(true, (CloseSplash.of("kb", listOf(armed("save")), saving, null, 1_000 + CloseSplash.FORCE_S + CloseSplash.SLOW_S) as CloseSplash.Phase.Closing).slow)
    }

    @Test fun goneWithAHandoffSaysWhereItIs() {
        val g = s("kb", SessionState.GONE, 1_050).copy(closed = it.pixelbox.cmwatch.contract.ClosedState(at = 1_050, handoff = true, file = "docs/handoff-2026-11-20.md"))
        assertEquals(CloseSplash.Phase.Closed("kb", 1_050, byMe = true, save = true, handoff = true, file = "docs/handoff-2026-11-20.md"), CloseSplash.of("kb", listOf(g), saving, null, 1_060))
    }

    @Test fun goneAfterTheSaveExpiredCarriesTheReason() {
        val g = s("kb", SessionState.GONE, 1_190).copy(closed = it.pixelbox.cmwatch.contract.ClosedState(at = 1_190, reason = "salvataggio scaduto"))
        assertEquals(CloseSplash.Phase.Closed("kb", 1_190, byMe = true, save = true, reason = "salvataggio scaduto"), CloseSplash.of("kb", listOf(g), saving, null, 1_200))
    }
}
