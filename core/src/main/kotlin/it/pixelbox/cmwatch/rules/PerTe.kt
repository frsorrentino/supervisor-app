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

    /**
     * Il tasto sulla voce: `index` nei tasti della richiesta; `direct` false = chiede un testo, quindi apre la scheda; `recommended`: è
     * la scelta consigliata, e la riga lo mostra in ambra (app-riall-2-viste-16-11). `tap` (app-tieni-premuto-28-11): un ok o un Va a un
     * tocco, col finale del cerchio verde.
     */
    data class Main(val index: Int, val label: String, val direct: Boolean, val recommended: Boolean = false, val tap: Boolean = false)

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
        return Main(i, short(if (num != null) "$num · ${a.label}" else a.label), a.textField == null, a.recommended)
    }

    /**
     * Il consiglio di chi chiede, sulla riga (app-riall-2-viste-16-11, direzione A: «consigliata: approvare»; contratto 1.149): approvare
     * o no su un ok; su una scelta l'opzione consigliata, col suo numero come sul tasto quando il tasto è lei. Senza consiglio, niente.
     */
    data class Advice(val approve: Boolean?, val option: String?)
    fun adviceOf(w: WaitItem, main: Main?): Advice? {
        val a = w.advice ?: return null
        a.approve?.let { x -> return Advice(x, null) }
        if (main?.recommended == true) return Advice(null, main.label)
        val o = one(a.option)
        return if (o.isEmpty()) null else Advice(null, short(o))
    }

    /** Il risultato da guardare di una verifica, col tipo e la versione per la riga di contesto. */
    data class Result(val id: String, val kind: Guardare.Kind, val version: Int?)

    /** `wait`: la richiesta (con `main` il suo tasto); `result`: il risultato da segnare visto. */
    data class Voce(
        val key: String, val group: Group, val title: String, val context: String?, val since: Long,
        val wait: WaitItem? = null, val main: Main? = null, val result: Result? = null, val advice: Advice? = null,
    )

    private fun one(s: String?): String = s.orEmpty().replace(Regex("\\s+"), " ").trim()

    /** Le voci della sessione: le richieste ancora aperte e i lavori con l'ultima versione non vista. */
    /** app-riquadri-chiusi-16-11: le voci che aspettano la persona (non i risultati da guardare); «Per te» si apre da solo per queste. */
    fun waitingKeys(items: List<Voce>): List<String> = items.filter { it.group != Group.VERIFICA }.map { it.key }

    /** La voce di una richiesta: titolo, contesto, gruppo e tasto principale. La stessa in «Per te» e in Dalla master (app-riall-1-dati-16-11). */
    fun voceOf(w: WaitItem): Voce {
        val title = one(w.title).ifEmpty { one(w.text) }
        val context = listOf(one(w.text), one(w.description)).firstOrNull { it.isNotEmpty() && it != title }
        // app-tieni-premuto-28-11: l'ok che pubblica si tiene premuto nella sua scheda, quindi la riga la apre; ogni altro ok e ogni Va
        // restano un tocco sulla riga ([Main.tap]), col finale del cerchio verde (correzione della master del 28/11).
        val m = mainOf(w.actions)
        val place = if (m != null) TieniPremuto.placeOf(w, m.index) else null
        val main = if (m != null && place == TieniPremuto.Place.PUBBLICA) m.copy(direct = false)
            else if (m != null && place != null) m.copy(tap = true)
            else m
        return Voce(w.id, groupOf(w.label), title, context, w.at, wait = w, main = main, advice = adviceOf(w, main))
    }

    fun voci(waits: List<WaitItem>, results: List<ResultGroup>, session: String): List<Voce> {
        val out = mutableListOf<Voce>()
        for (w in waits) {
            if (w.session != session || (w.state ?: "open") != "open") continue
            out += voceOf(w)
        }
        for (g in results) {
            val top = g.items.firstOrNull() ?: continue
            if (g.session != session || !g.unseen) continue
            val l = Guardare.line(top)
            out += Voce(top.id, Group.VERIFICA, l.title, null, top.at, result = Result(top.id, Guardare.kind(top.kind), l.version))
        }
        return out.sortedWith(byOrder)
    }

    private val byOrder = compareBy<Voce> { it.group.ordinal }.thenBy { it.since }

    /** Un gruppo di Dalla master: le voci di una sessione, o quelle senza sessione (`session` null). */
    data class Gruppo(val session: String?, val voci: List<Voce>)

    /**
     * Per te in ordine (app-per-te-ordine-23-11, Franz 23/11 19:00 e 19:01; contratto 1.160): in alto, aperte, le carte che bloccano
     * un compito in corso (`blocking`), dalla più vecchia, ognuna con la sua sessione; sotto i gruppi per sessione col resto (pause,
     * domande, verifiche), chiusi di base. Un gruppo che resta senza voci non c'è. La stessa regola di splitBlocking in perTe.ts.
     */
    data class Bloccante(val session: String?, val voce: Voce)
    data class Ordine(val top: List<Bloccante>, val groups: List<Gruppo>)
    fun splitBlocking(groups: List<Gruppo>): Ordine {
        val top = groups.flatMap { g -> g.voci.filter { v -> v.wait?.blocking == true }.map { v -> Bloccante(g.session, v) } }.sortedBy { it.voce.since }
        val rest = groups.mapNotNull { g -> g.voci.filter { v -> v.wait?.blocking != true }.takeIf { it.isNotEmpty() }?.let { Gruppo(g.session, it) } }
        return Ordine(top, rest)
    }

    /**
     * Dalla master (app-riall-2-viste-16-11, direzione A scelta da Franz il 16/11): le stesse voci di «Per te» di ogni sessione, una
     * sessione per gruppo; prima quella che aspetta da più tempo, in fondo le voci senza sessione (la prima raccolta). Le voci già
     * risposte ([answered]) restano fuori, e un gruppo senza voci non c'è. La stessa regola di groupsOf in perTe.ts.
     */
    fun groupsOf(waits: List<WaitItem>, results: List<ResultGroup>, answered: Set<String> = emptySet()): List<Gruppo> {
        fun open(w: WaitItem) = (w.state ?: "open") == "open"
        val names = LinkedHashSet<String>()
        waits.forEach { w -> w.session?.takeIf { s -> s.isNotEmpty() && open(w) }?.let { s -> names += s } }
        results.forEach { g -> g.session?.takeIf { s -> s.isNotEmpty() && g.unseen && g.items.isNotEmpty() }?.let { s -> names += s } }
        val out = names.map { s -> Gruppo(s, voci(waits, results, s).filter { v -> v.key !in answered }) }.filter { g -> g.voci.isNotEmpty() }
            .sortedWith(compareBy<Gruppo> { g -> g.voci.minOf { v -> v.since } }.thenBy { g -> g.session.orEmpty() })
        val loose = waits.filter { w -> w.session.isNullOrEmpty() && open(w) }.map(::voceOf).filter { v -> v.key !in answered }.sortedWith(byOrder)
        return if (loose.isEmpty()) out else out + Gruppo(null, loose)
    }
}
