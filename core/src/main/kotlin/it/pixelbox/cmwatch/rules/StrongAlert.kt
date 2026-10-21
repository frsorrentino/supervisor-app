package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Attention
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * Contratto 1.68 (Franz, 14/10 22:45): un solo avviso forte per ogni interruzione, con lo stesso disegno di vibrazione su
 * telefono e orologio, finché la voce resta in `state.attention`. I promemoria del relay (10 e 30 minuti) sono lo stesso
 * avviso. Un evento quiet (1.62) non suona, ma la voce resta in cima.
 */
object StrongAlert {
    /** Lo stesso disegno sui due dispositivi: due colpi pieni, distinti da esiti e chiusure. */
    val VIBRATION = longArrayOf(0, 220, 120, 220)

    private val STRONG = setOf(
        EventKind.QUESTION, EventKind.NEEDS_YOU, EventKind.TURN_BLOCKING, EventKind.TURN_FAILED, EventKind.QUOTA_LIMIT,
        EventKind.ATTENTION_REMINDER, EventKind.TASK_STALLED,
    )

    fun strong(kind: EventKind): Boolean = kind in STRONG

    /** Perché la sessione è ferma per Franz: `needs_you` prima della voce bloccante; null se non lo è. */
    data class Stop(val text: String, val at: Long)

    fun stop(s: Session): Stop? {
        if (s.state == SessionState.GONE) return null
        s.needsYou?.takeIf { it.reason.isNotBlank() }?.let { return Stop(it.reason, it.at) }
        return s.turnBlocking?.takeIf { it.text.isNotBlank() }?.let { Stop(it.text, it.at) }
    }

    /** Un promemoria suona solo se la sua voce è ancora aperta; senza stato letto si avvisa. */
    fun open(state: State?, e: Event): Boolean = state == null ||
        state.attention.orEmpty().any { it.ref == e.ref && (e.attentionKind == null || it.kind == e.attentionKind) }

    /** Le voci uscite da `state.attention` fra due stati: le loro notifiche si chiudono. */
    fun closed(prev: State?, cur: State): List<Attention> {
        val now = cur.attention.orEmpty().map { it.kind to it.ref }.toSet()
        return prev?.attention.orEmpty().filter { (it.kind to it.ref) !in now }
    }

    /** Il banner nella casa della master: le voci delle altre sessioni, non le sue. */
    fun forMaster(state: State): List<Attention> = state.attention.orEmpty().filter { it.session != ContextActions.MASTER }
}
