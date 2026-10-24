package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.Suggestion

/**
 * La scheda «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup; contratto 1.84): al massimo tre, dal più
 * recente, senza quelli già toccati da questo dispositivo né quelli scaduti. Stessa regola di suggestions.ts.
 */
object Suggestions {
    const val MAX_SHOWN = 3
    data class Visible(val shown: List<Suggestion>, val total: Int)

    fun visible(state: State, done: Set<String>, now: Long): Visible {
        val all = state.suggestions.orEmpty().filter { it.id !in done && (it.expiresAt == null || it.expiresAt > now) }.sortedByDescending { it.at }
        return Visible(all.take(MAX_SHOWN), all.size)
    }
}
