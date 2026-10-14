package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Gli eventi che chiedono una riga o un tasto (Franz: sul telefono una riga e i tasti, mai paragrafi). Contratto 1.57: il
 * controllo di un task allo stop, verde o rosso. Contratto 1.58: quota finita, con le strade che il relay propone.
 */
object AlertEvents {
    /** 1.57: «✓ <task>: check green» o «✗ <task>: check red». */
    fun green(e: Event): Boolean = e.shareTitle.trimStart().startsWith("✓")

    /** Le parole dei tasti: «continua su %s», «riprendi alle %s», «passa a un'altra sessione». */
    data class Words(val otherAccount: String, val atReset: String, val pass: String)

    /** Una strada: la parola del tasto e l'`arg` di `quota_route`; null per «pass», dove la sessione la sceglie l'utente. */
    data class Way(val action: String, val label: String, val arg: String?)

    /** 1.58: le strade in ordine, solo quelle che il relay ha proposto e che hanno i dati per il tasto. */
    fun ways(e: Event, w: Words, hhmm: (Long) -> String): List<Way> = e.actions.orEmpty().mapNotNull { a ->
        when (a) {
            OTHER_ACCOUNT -> e.otherAccount?.let { Way(a, w.otherAccount.format(it), OTHER_ACCOUNT) }
            AT_RESET -> e.resetAt?.let { Way(a, w.atReset.format(hhmm(it)), AT_RESET) }
            PASS -> Way(a, w.pass, null)
            else -> null
        }
    }

    fun passArg(session: String) = "pass:$session"

    /** A chi passare: le sessioni vive, non quella ferma. */
    fun passTargets(sessions: List<Session>, stopped: String): List<String> =
        sessions.filter { it.state != SessionState.GONE && it.name != stopped }.map { it.name }

    /**
     * Il `quota_limit` ancora da decidere per una sessione: l'ultimo, finché la quota non si azzera (5 ore senza `reset_at`)
     * e finché la sessione non ha ripreso a lavorare dopo l'evento.
     */
    fun openLimit(events: List<Event>, s: Session, nowS: Long): Event? =
        events.filter { it.kind == EventKind.QUOTA_LIMIT && it.ref == s.name }.maxByOrNull { it.ts }
            ?.takeIf { e -> (e.resetAt ?: (e.ts + 5 * 3600)) > nowS && (s.turnStarted ?: 0) <= e.ts && s.state != SessionState.GONE }

    const val OTHER_ACCOUNT = "other_account"
    const val AT_RESET = "at_reset"
    const val PASS = "pass"
}
