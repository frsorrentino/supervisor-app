package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ResultGroup
import it.pixelbox.cmwatch.contract.WaitAction
import it.pixelbox.cmwatch.contract.WaitItem

/**
 * Il formato unico (app-formato-unico-12-11, variante A scelta da Franz il 12/11 alle 07:29; mockup in
 * docs/mockup/2026-11-12-formato-unico): tutto ciò che aspetta la persona in una sessione è una voce della stessa forma, nell'ordine
 * Ti aspetta, Decidi, Prova, Verifica. Ogni voce ha titolo, una riga di contesto, da quanto aspetta e il suo tasto principale; il
 * tocco apre la scheda. Le domande, le decisioni e le prove vengono da `attese` (contratti 1.130-1.137), le verifiche dai risultati
 * non visti (1.106). La stessa regola di web/src/lib/perTe.ts, coi casi di core/src/test/resources/per-te-cases.json.
 */
object PerTe {
    /** Nell'ordine in cui stanno nell'elenco. */
    enum class Group { ASPETTA, DECIDI, PROVA, VERIFICA }

    /** Il gruppo di una richiesta dalla sua etichetta: reply, verify, e ogni altra (decide, choose) è una decisione. */
    fun groupOf(label: String): Group = when (label) { "reply" -> Group.ASPETTA; "verify" -> Group.PROVA; else -> Group.DECIDI }

    /** Il tasto sulla voce: `index` nei tasti della richiesta; `direct` false = chiede un testo, quindi apre la scheda. */
    data class Main(val index: Int, val label: String, val direct: Boolean)

    /** Il tasto sta sulla voce accanto al titolo: al massimo questi caratteri, tagliato a una parola intera; l'etichetta intera è nella scheda. */
    const val MAIN_MAX = 16

    private fun short(label: String): String {
        val l = label.replace(Regex("\\s+"), " ").trim()
        if (l.length <= MAIN_MAX) return l
        val cut = l.take(MAIN_MAX + 1)
        val at = cut.lastIndexOf(' ')
        return (if (at >= MAIN_MAX / 2) cut.take(at) else l.take(MAIN_MAX)).replace(Regex("[\\s·,;:]+$"), "")
    }

    /**
     * Il tasto principale: la scelta consigliata, altrimenti il tasto pieno, altrimenti la prima scelta numerata; solo tasti secondari,
     * nessuno. Una scelta numerata porta il suo numero («1 · sì»), come nella scheda.
     */
    fun mainOf(actions: List<WaitAction>?): Main? {
        val acts = actions.orEmpty()
        var i = acts.indexOfFirst { it.recommended && it.style != "secondary" }
        if (i < 0) i = acts.indexOfFirst { it.style == "primary" }
        if (i < 0) i = acts.indexOfFirst { it.style == "option" }
        if (i < 0) return null
        val a = acts[i]
        val num = if (TiAspetta.lookOf(a) == TiAspetta.Look.OPTION) acts.take(i + 1).count { TiAspetta.lookOf(it) == TiAspetta.Look.OPTION } else null
        return Main(i, short(if (num != null) "$num · ${a.label}" else a.label), a.textField == null)
    }

    /** Il risultato da guardare di una verifica, col tipo e la versione per la riga di contesto. */
    data class Result(val id: String, val kind: Guardare.Kind, val version: Int?)

    /** `wait`: la richiesta (con `main` il suo tasto); `result`: il risultato da segnare visto. */
    data class Voce(
        val key: String, val group: Group, val title: String, val context: String?, val since: Long,
        val wait: WaitItem? = null, val main: Main? = null, val result: Result? = null,
    )

    private fun one(s: String?): String = s.orEmpty().replace(Regex("\\s+"), " ").trim()

    /** Le voci della sessione: le richieste ancora aperte e i lavori con l'ultima versione non vista. */
    fun voci(waits: List<WaitItem>, results: List<ResultGroup>, session: String): List<Voce> {
        val out = mutableListOf<Voce>()
        for (w in waits) {
            if (w.session != session || (w.state ?: "open") != "open") continue
            val title = one(w.title).ifEmpty { one(w.text) }
            val context = listOf(one(w.text), one(w.description)).firstOrNull { it.isNotEmpty() && it != title }
            out += Voce(w.id, groupOf(w.label), title, context, w.at, wait = w, main = mainOf(w.actions))
        }
        for (g in results) {
            val top = g.items.firstOrNull() ?: continue
            if (g.session != session || !g.unseen) continue
            val l = Guardare.line(top)
            out += Voce(top.id, Group.VERIFICA, l.title, null, top.at, result = Result(top.id, Guardare.kind(top.kind), l.version))
        }
        return out.sortedWith(compareBy<Voce> { it.group.ordinal }.thenBy { it.since })
    }
}
