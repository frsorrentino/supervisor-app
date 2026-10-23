package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * La coda «Ti aspettano» (piano 30/09, Task 1): le domande aperte di tutte le sessioni, da rispondere in fila.
 * Contratto 1.68: chi è ferma per Franz conta come una domanda, come nel banner (`Elsewhere`) e nella home; la web dal
 * 15/10, il telefono dal 23/10 (il banner diceva «3 sessioni ti aspettano» con tre needs-you, la coda «Nessuno ti
 * aspetta»). `questionId` di una ferma è una chiave sua, mai l'id di una domanda. Le sessioni chiuse mai, come nel banner.
 */
object AttentionQueue {
    enum class Kind { QUESTION, STOP }
    data class Item(val session: String, val questionId: String, val askedAt: Long, val kind: Kind = Kind.QUESTION, val text: String = "")

    /** Le domande aperte e le ferme per Franz, dalla più vecchia. */
    fun items(state: State): List<Item> = state.sessions
        .filter { it.state != SessionState.GONE }
        .mapNotNull { s ->
            s.question?.let { Item(s.name, it.id, it.askedAt, Kind.QUESTION, it.text) }
                ?: StrongAlert.stop(s)?.let { Item(s.name, "stop:${s.name}@${it.at}", it.at, Kind.STOP, it.text) }
        }
        .sortedBy { it.askedAt }

    /**
     * La domanda dopo `current`; dopo l'ultima si riparte dalla più vecchia. Se `current` non c'è più (risposta al PC
     * mentre la coda era aperta) si va alla più vecchia rimasta: mai una risposta a una domanda sparita.
     */
    fun next(state: State, current: String?): Item? {
        val all = items(state)
        val i = all.indexOfFirst { it.questionId == current }
        return if (i < 0) all.firstOrNull() else all.getOrNull(i + 1) ?: all.firstOrNull()
    }

    /** La pagina della domanda `questionId` nella fila di adesso; null se non c'è più. */
    fun page(items: List<Item>, questionId: String?): Int? = items.indexOfFirst { it.questionId == questionId }.takeIf { it >= 0 }
}
