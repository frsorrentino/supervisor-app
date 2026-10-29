package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.Suggestion

/**
 * La scheda «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup; contratto 1.84): al massimo tre, dal più
 * recente, senza quelli già toccati da questo dispositivo né quelli scaduti. Stessa regola di suggestions.ts.
 * Contratto 1.88 (la coda, Franz 26/10 20:44): prima per `rank` (assente o fuori 0-9 = 5), poi dalla più recente; fra le
 * tre al massimo due dello stesso tipo, ma se la coda non ha altro si riempie con le scartate.
 */
object Suggestions {
    const val MAX_SHOWN = 3
    /** I quattro del 1.84 e i sei della coda (documento suggerimenti-coda, §8.1); un tipo fuori lista si mostra neutro. */
    val KINDS = listOf("recurring", "cleanup", "deadline", "quota", "choice", "followup", "unsaved", "waiting", "idea", "proposal", "observe")
    fun known(kind: String) = kind in KINDS
    data class Visible(val shown: List<Suggestion>, val total: Int)

    const val MAX_PER_KIND = 2

    private fun rank(s: Suggestion) = s.rank?.takeIf { it in 0..9 } ?: 5

    fun visible(state: State, done: Set<String>, now: Long): Visible {
        val all = state.suggestions.orEmpty().filter { it.id !in done && (it.expiresAt == null || it.expiresAt > now) }
            .sortedWith(compareBy<Suggestion> { rank(it) }.thenByDescending { it.at })
        val shown = mutableListOf<Suggestion>()
        for (s in all) if (shown.size < MAX_SHOWN && shown.count { it.kind == s.kind } < MAX_PER_KIND) shown += s
        for (s in all) if (shown.size < MAX_SHOWN && s !in shown) shown += s
        return Visible(all.filter { it in shown }, all.size)
    }

    /** I tasti di una `choice`: le sue 2-4 etichette; altrimenti nessuno, e la voce ha Accetta come le altre. */
    fun options(s: Suggestion): List<String> = s.options?.takeIf { s.kind == "choice" && it.size in 2..4 }.orEmpty()
}
