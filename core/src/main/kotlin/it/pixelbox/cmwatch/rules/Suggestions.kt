package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.Suggestion

/**
 * La scheda «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup; contratto 1.84): al massimo tre, dal più
 * recente, senza quelli già toccati da questo dispositivo né quelli scaduti. Stessa regola di suggestions.ts.
 */
object Suggestions {
    const val MAX_SHOWN = 3
    /** I quattro del 1.84 e i sei della coda (documento suggerimenti-coda, §8.1); un tipo fuori lista si mostra neutro. */
    val KINDS = listOf("recurring", "cleanup", "deadline", "quota", "choice", "followup", "unsaved", "waiting", "idea", "proposal")
    fun known(kind: String) = kind in KINDS
    data class Visible(val shown: List<Suggestion>, val total: Int)

    fun visible(state: State, done: Set<String>, now: Long): Visible {
        val all = state.suggestions.orEmpty().filter { it.id !in done && (it.expiresAt == null || it.expiresAt > now) }.sortedByDescending { it.at }
        return Visible(all.take(MAX_SHOWN), all.size)
    }
}
