package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test

/** Contratto 1.68: un solo avviso forte per ogni interruzione, finché la voce resta in `state.attention`. */
class StrongAlertTest {
    private fun s(name: String, st: SessionState = SessionState.IDLE) =
        Session(id = name, name = name, account = "personale", project = name, state = st, since = 0)
    private fun st(vararg ss: Session, attention: List<Attention>? = null) = State(v = 1, ts = 0, host = "pc", sessions = ss.toList(), attention = attention)
    private fun ev(kind: EventKind, ref: String?, attentionKind: String? = null) =
        Event(key = "k", kind = kind, session = ref, ts = 0, title = "t", ref = ref, attentionKind = attentionKind)

    @Test fun theFiveInterruptionsAndTheReminderAreStrong() {
        val strong = EventKind.entries.filter { StrongAlert.strong(it) }.toSet()
        // Contratto 1.76: anche gli impegni fermi (task_stalled).
        assertEquals(setOf(EventKind.QUESTION, EventKind.NEEDS_YOU, EventKind.TURN_BLOCKING, EventKind.TURN_FAILED, EventKind.QUOTA_LIMIT, EventKind.ATTENTION_REMINDER, EventKind.TASK_STALLED), strong)
    }

    @Test fun needsYouComesBeforeTheBlockingStep() {
        val both = s("kb").copy(needsYou = NeedsYou("login su win", 20), turnBlocking = TurnBlocking("ok al deploy", 10))
        assertEquals(StrongAlert.Stop("login su win", 20), StrongAlert.stop(both))
        assertEquals(StrongAlert.Stop("ok al deploy", 10), StrongAlert.stop(s("kb").copy(turnBlocking = TurnBlocking("ok al deploy", 10))))
        assertNull(StrongAlert.stop(s("kb").copy(needsYou = NeedsYou("  ", 5))))
        assertNull(StrongAlert.stop(s("kb")))
    }

    @Test fun aClosedSessionIsNotStoppedForFranz() =
        assertNull(StrongAlert.stop(s("kb", SessionState.GONE).copy(needsYou = NeedsYou("login", 1))))

    @Test fun aReminderRingsOnlyWhileItsItemIsOpen() {
        val open = st(attention = listOf(Attention("needs_you", "kb", "kb", "login", 1)))
        assertTrue(StrongAlert.open(open, ev(EventKind.ATTENTION_REMINDER, "kb", "needs_you")))
        assertFalse(StrongAlert.open(st(attention = null), ev(EventKind.ATTENTION_REMINDER, "kb", "needs_you")))
        assertFalse(StrongAlert.open(open, ev(EventKind.ATTENTION_REMINDER, "kb", "turn_blocking")))
        // Senza stato (non ancora letto) si avvisa: meglio un promemoria in più che uno perso.
        assertTrue(StrongAlert.open(null, ev(EventKind.ATTENTION_REMINDER, "kb", "needs_you")))
    }

    @Test fun closedItemsAreTheOnesThatLeftAttention() {
        val a = Attention("needs_you", "kb", "kb", "login", 1)
        val b = Attention("turn_failed", "atlas", "atlas", "errore", 2)
        assertEquals(listOf(a), StrongAlert.closed(st(attention = listOf(a, b)), st(attention = listOf(b))))
        assertEquals(emptyList<Attention>(), StrongAlert.closed(null, st(attention = listOf(b))))
        // `attention` assente = niente aperto: con un relay 1.66+ la voce è uscita davvero.
        assertEquals(listOf(a), StrongAlert.closed(st(attention = listOf(a)), st(attention = null)))
    }

    /** Il banner della master: le voci delle altre sessioni, non le sue. */
    @Test fun theMasterBannerCountsOnlyTheOthers() {
        val items = listOf(
            Attention("question", "kb", "q1", "?", 1), Attention("needs_you", ContextActions.MASTER, ContextActions.MASTER, "login", 2),
            Attention("task_red", null, "t1", "rosso", 3),
        )
        assertEquals(2, StrongAlert.forMaster(st(attention = items)).size)
        assertEquals(0, StrongAlert.forMaster(st()).size)
    }

    @Test fun elsewhereCountsAStoppedSessionAsWaiting() {
        val state = st(s("kb").copy(needsYou = NeedsYou("login", 5)), s("open"))
        assertEquals(Elsewhere.Waiting(listOf("kb"), listOf("w:kb@5")), Elsewhere.alert(state, "open", 10, emptySet(), emptySet()))
    }
}
