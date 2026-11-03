package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.ToccaToday

/**
 * Tocca a te con un conto solo (app-pulizia-tocca-03-11, contratto 1.113): home, menu e registro danno lo stesso numero perché lo
 * calcola il relay. Con un relay precedente restano i conti delle pagine (`decisions.n`, `prove.n`).
 */
object ToccaRules {
    data class Counts(val decidi: Int, val prove: Int)

    fun counts(s: State): Counts = s.tocca?.let { Counts(it.decidi, it.prove) } ?: Counts(s.decisions?.n ?: 0, s.prove?.n ?: 0)

    /** Le voci uscite da sole oggi; null se nessuna o con un relay precedente. */
    fun closedToday(s: State): ToccaToday? = s.tocca?.today?.takeIf { it.n > 0 }
}
