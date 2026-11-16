package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.WaitItem

/**
 * La lista unica di ciò che aspetta la persona (app-riall-1-dati-16-11, contratto 1.151, Franz 16/11 20:39: passo 1 del
 * riallineamento, docs/valutazioni/2026-11-16-chiarezza-sessione.md): Per te nella sessione, Decidi e Verifica in Dalla master, le chip
 * delle sessioni e i conti leggono le stesse voci, così il numero sono le righe. Una voce risposta qui, o chiusa altrove
 * (`state.attese.closed`), esce da ogni vista. Come attese.ts, coi casi di contract/attese-rd-sample.json.
 */
object Attese {
    fun isOpen(w: WaitItem): Boolean = (w.state ?: "open") == "open"
    /** Decidi: le etichette decide e choose; Verifica: verify; Ti aspetta: reply. */
    fun isDecide(w: WaitItem): Boolean = w.label == "decide" || w.label == "choose"
    fun isVerify(w: WaitItem): Boolean = w.label == "verify"
    fun isReply(w: WaitItem): Boolean = w.label == "reply"

    /** Le voci da mostrare: aperte e non ancora risposte da qui né chiuse altrove. */
    fun visible(items: List<WaitItem>, answered: Set<String>): List<WaitItem> = items.filter { w -> isOpen(w) && w.id !in answered }

    data class Counts(val n: Int, val decidi: Int, val prove: Int, val rispondi: Int)
    /** I conti delle voci, di tutte ([all] vero) o di una sessione: gli stessi di `state.tocca` e `sessions[].tocca`. */
    fun counts(items: List<WaitItem>, session: String? = null, all: Boolean = true): Counts {
        val mine = if (all) items else items.filter { w -> w.session == session }
        return Counts(mine.size, mine.count(::isDecide), mine.count(::isVerify), mine.count(::isReply))
    }

    /** Per le chip delle sessioni: per ognuna, le decisioni e le verifiche. */
    fun perSession(items: List<WaitItem>): Map<String, Pair<Int, Int>> = items.filter { w -> w.session != null }.groupBy { w -> w.session!! }
        .mapValues { (_, l) -> l.count(::isDecide) to l.count(::isVerify) }
        .filterValues { (d, v) -> d > 0 || v > 0 }

    /** Gli id delle voci chiuse da poco, da togliere subito da ogni vista. */
    fun closedIds(s: State): List<String> = s.attese?.closed.orEmpty().map { c -> c.id }
}
