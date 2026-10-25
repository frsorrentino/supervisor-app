package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Attention
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Gli eventi che chiedono una riga o un tasto (Franz: sul telefono una riga e i tasti, mai paragrafi). Contratto 1.57: il
 * controllo di un task allo stop, verde o rosso. Contratto 1.58: quota finita, con le strade che il relay propone.
 */
object AlertEvents {
    /** 1.78: l'esito sta in `ok`; con un relay precedente lo dice il «✓» in testa al titolo grezzo (1.57). */
    fun green(e: Event): Boolean = e.ok ?: (e.preview?.title ?: e.title).trimStart().startsWith("✓")

    /** Le parole dei tasti: «continua su %s», «riprendi alle %s», «passa a un'altra sessione», «continua con %s» (il modello). */
    data class Words(val otherAccount: String, val atReset: String, val pass: String, val otherModel: String)

    /** Una strada: la parola del tasto e l'`arg` di `quota_route`; null per «pass», dove la sessione la sceglie l'utente. */
    data class Way(val action: String, val label: String, val arg: String?)

    /** 1.58: le strade in ordine, solo quelle che il relay ha proposto e che hanno i dati per il tasto. */
    fun ways(e: Event, w: Words, hhmm: (Long) -> String): List<Way> = e.actions.orEmpty().mapNotNull { a ->
        when (a) {
            OTHER_ACCOUNT -> e.otherAccount?.let { Way(a, w.otherAccount.format(it), OTHER_ACCOUNT) }
            AT_RESET -> e.resetAt?.let { Way(a, w.atReset.format(hhmm(it)), AT_RESET) }
            OTHER_MODEL -> e.otherModel?.let { Way(a, w.otherModel.format(it), OTHER_MODEL) }
            PASS -> Way(a, w.pass, null)
            else -> null
        }
    }

    fun passArg(session: String) = "pass:$session"

    /** A chi passare: le sessioni vive, non quella ferma. */
    fun passTargets(sessions: List<Session>, stopped: String): List<String> =
        sessions.filter { it.state != SessionState.GONE && it.name != stopped }.map { it.name }

    /**
     * Chiuso dopo l'evento (16/10: turn_started è null a turno finito, e da solo riapriva la riga): un turno partito, un
     * esito arrivato, la voce uscita da `state.attention` (passata solo quando lo stato c'è), `settled_at` del relay 0.8.30.
     */
    private fun closed(e: Event, s: Session, attention: List<Attention>?): Boolean =
        e.settledAt != null || (s.turnStarted ?: 0) > e.ts || (s.outcome?.at ?: 0) > e.ts ||
            attention != null && attention.none { it.kind == (if (e.kind == EventKind.QUOTA_LIMIT) "quota_limit" else "turn_failed") && it.ref == s.name }

    /**
     * Il `quota_limit` ancora da decidere per una sessione: l'ultimo, finché la quota non si azzera (5 ore senza `reset_at`)
     * e finché non è chiuso. `attention` = `state.attention.orEmpty()`, null senza stato.
     */
    fun openLimit(events: List<Event>, s: Session, nowS: Long, attention: List<Attention>? = null): Event? =
        events.filter { it.kind == EventKind.QUOTA_LIMIT && it.ref == s.name }.maxByOrNull { it.ts }
            ?.takeIf { e -> (e.resetAt ?: (e.ts + 5 * 3600)) > nowS && !closed(e, s, attention) && s.state != SessionState.GONE }

    /**
     * Contratto 1.61: il turno fallito ancora da riprendere per una sessione, l'ultimo, finché non è chiuso e per al massimo
     * 6 ore. Il tasto «Continua» manda `continue_text` come prompt; l'ultimo evento senza testo è già chiuso.
     */
    fun openFailure(events: List<Event>, s: Session, nowS: Long, attention: List<Attention>? = null): Event? =
        events.filter { it.kind == EventKind.TURN_FAILED && it.ref == s.name }.maxByOrNull { it.ts }
            ?.takeIf { e -> !e.continueText.isNullOrBlank() && nowS - e.ts < 6 * 3600 && !closed(e, s, attention) &&
                s.state != SessionState.GONE && s.state != SessionState.BUSY }

    /** Contratto 1.62: senza suono né vibrazione, perché Franz è al PC su quella sessione. Senza il campo si suona (fail open). */
    fun quiet(e: Event?, s: Session?): Boolean = e?.quiet == true || s?.atPc == true

    const val OTHER_ACCOUNT = "other_account"
    const val AT_RESET = "at_reset"
    const val PASS = "pass"
    const val OTHER_MODEL = "other_model"
}
