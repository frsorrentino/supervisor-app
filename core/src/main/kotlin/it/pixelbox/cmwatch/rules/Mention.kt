package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * «@nome» nella chat della master (Franz, 14/10 21:55, voce 5): scrivendo «@» il campo propone le sessioni, scelta una il
 * destinatario resta scritto nel campo e il messaggio va a quella sessione invece che alla master.
 */
object Mention {
    /** Le sessioni da proporre mentre si scrive «@…» all'inizio del campo, prima dello spazio; vuoto altrimenti. */
    fun suggest(draft: String, sessions: List<Session>, master: String): List<Session> {
        if (!draft.startsWith("@") || draft.contains(' ') || draft.contains('\n')) return emptyList()
        val q = draft.removePrefix("@")
        return sessions.filter { it.name != master && it.state != SessionState.GONE && it.name.startsWith(q, ignoreCase = true) }
    }

    /** Destinatario e testo di un messaggio che comincia con «@nome »; null se non c'è o se la sessione non è viva. */
    fun parse(draft: String, sessions: List<Session>, master: String): Pair<String, String>? {
        if (!draft.startsWith("@")) return null
        val name = draft.removePrefix("@").substringBefore(' ').substringBefore('\n')
        val text = draft.removePrefix("@$name").trim()
        if (text.isEmpty() || name == master) return null
        return sessions.firstOrNull { it.name == name && it.state != SessionState.GONE }?.let { it.name to text }
    }
}
