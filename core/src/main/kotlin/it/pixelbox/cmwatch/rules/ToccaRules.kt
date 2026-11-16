package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ProveItem
import it.pixelbox.cmwatch.contract.ProvePage
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.ToccaToday

/**
 * Tocca a te con un conto solo (app-pulizia-tocca-03-11, contratto 1.113): home, menu e registro danno lo stesso numero perché lo
 * calcola il relay. Con un relay precedente restano i conti delle pagine (`decisions.n`, `prove.n`).
 */
object ToccaRules {
    data class Counts(val decidi: Int, val prove: Int)

    fun counts(s: State): Counts = s.tocca?.let { Counts(it.decidi, it.prove) } ?: Counts(s.decisions?.n ?: 0, s.prove?.n ?: 0)

    /**
     * app-riall-1-dati-16-11 (contratto 1.151): lo stato coi conti delle voci come si vedono (le risposte da qui già fuori), così Decidi e
     * Verifica contano quello che elencano. Senza la lista unica (relay precedente) lo stato resta com'è.
     */
    fun withItems(s: State, items: List<it.pixelbox.cmwatch.contract.WaitItem>?): State = if (items == null) s else Attese.counts(items).let { c ->
        s.copy(tocca = (s.tocca ?: it.pixelbox.cmwatch.contract.ToccaState()).copy(n = c.n, prove = c.prove, decidi = c.decidi, rispondi = c.rispondi))
    }

    /** Una sessione aperta con le sue prove: la riga di Verifica che porta alla sua scheda. */
    data class InSession(val session: String, val n: Int)

    /**
     * app-verifica-vuota-16-11 (Franz 16/11 18:07: «Verifica 5», aperta «Niente da provare»), porta della regola del web del 12/11
     * (app-verifica-conto-elenco-12-11, stesso sintomo segnalato allora sul web): le prove di una sessione aperta stanno nella sua scheda
     * (app-sessioni-compatte-06-11), ma il numero di Verifica è il conto unico del relay e le conta tutte. In Verifica restano le prove
     * fuori dalle sessioni aperte ([Verifica.outside]) e una riga per ognuna di quelle sessioni col suo numero ([Verifica.inSessions]):
     * elenco e numero dicono la stessa cosa. [sessionOf] dà la sessione di una prova (la sua, o quella del suo compito).
     */
    data class Verifica(val outside: ProvePage, val inSessions: List<InSession>) {
        /** Niente fuori dalle sessioni aperte: «Niente da provare» si dice solo se nemmeno le sessioni ne hanno. */
        val outsideEmpty: Boolean get() = outside.groups.isEmpty() && outside.ko.isEmpty() && outside.red.isEmpty()
    }

    fun verifica(page: ProvePage, sessionOf: (ProveItem) -> String?, live: Set<String>): Verifica {
        val mine: (ProveItem) -> String? = { p -> sessionOf(p)?.takeIf { it in live } }
        val gs = page.groups.map { g -> g.copy(prove = g.prove.filter { p -> mine(p) == null }) }.filter { g -> g.prove.isNotEmpty() }
        val bySession = page.groups.flatMap { g -> g.prove }.mapNotNull(mine).groupingBy { it }.eachCount()
        return Verifica(page.copy(groups = gs, n = gs.sumOf { g -> g.prove.size }), proveInSessions(bySession.mapValues { (_, n) -> 0 to n }))
    }

    /** Le righe per sessione da `todo` (decisioni, prove) di ogni sessione aperta: solo quelle con prove, la più piena prima. Come toccaRules.ts. */
    fun proveInSessions(todo: Map<String, Pair<Int, Int>>): List<InSession> =
        todo.filter { (_, c) -> c.second > 0 }.map { (s, c) -> InSession(s, c.second) }.sortedWith(compareByDescending<InSession> { it.n }.thenBy { it.session })

    /** Le voci uscite da sole oggi; null se nessuna o con un relay precedente. */
    fun closedToday(s: State): ToccaToday? = s.tocca?.today?.takeIf { it.n > 0 }
}
