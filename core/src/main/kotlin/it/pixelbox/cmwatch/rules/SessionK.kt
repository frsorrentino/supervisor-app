package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.KpiData
import it.pixelbox.cmwatch.contract.KpiResult
import it.pixelbox.cmwatch.contract.KpiTurn
import it.pixelbox.cmwatch.contract.TimelinePage

/**
 * La scheda aperta di una sessione nella forma K (app-scheda-sessione-semplice-19-11, scelta di Franz il 19/11 alle 18:42; bozza in
 * docs/mockups/scheda-sessione-19-11/k1.html): dall'alto in ordine di tempo come la chat — Prima (i turni di oggi), Risultati,
 * Adesso (compito, esito, contesto, finestra), Prossimo (passo, ciò che aspetta Franz col tasto in basso). Qui la parte che non
 * dipende dall'interfaccia. Lo stesso in web/src/lib/sessionK.ts.
 */
object SessionK {
    /** Un turno di oggi: inizio, minuti (almeno 1), riuscito o no, il testo dell'esito se il relay lo manda. */
    data class Turn(val start: Long, val minutes: Long, val ok: Boolean, val text: String?, val entry: String? = null)

    /** Prima: gli ultimi [shown] turni dal più vecchio al più nuovo, quanti ne restano sopra, il totale e i minuti di oggi. */
    data class Before(val turns: List<Turn>, val hidden: Int, val total: Int, val minutes: Long, val failed: Int)

    const val SHOWN = 3

    fun before(turns: List<KpiTurn>?, all: Boolean = false): Before? {
        val t = turns.orEmpty().sortedBy { it.start }
        if (t.isEmpty()) return null
        val shown = if (all) t else t.takeLast(SHOWN)
        val sum = Kpi.turnsSummary(t)
        return Before(
            shown.map { Turn(it.start, ((it.end - it.start).coerceAtLeast(0) + 30) / 60, it.outcome != "fail", it.text?.trim()?.takeIf { x -> x.isNotEmpty() }, it.entry?.trim()?.takeIf { x -> x.isNotEmpty() }) }.map { it.copy(minutes = it.minutes.coerceAtLeast(1)) },
            t.size - shown.size, t.size, sum.minutes, sum.failed,
        )
    }

    /** Contratto 1.156: un risultato da mostrare — il tocco apre il link, o la voce della chat che ha mandato il file. */
    data class Result(val kind: String, val label: String, val url: String?, val entry: String?, val at: Long)
    data class Results(val shown: List<Result>, val hidden: Int, val total: Int)

    const val RESULTS_SHOWN = 4
    private val KINDS = setOf("image", "file", "link", "release")
    private val HTTP = Regex("^https?://")

    /** Risultati: i più nuovi per primi, al massimo 20; fuori quelli senza nome o che non si possono aprire; null quando non ce n'è. */
    fun results(list: List<KpiResult>?, all: Boolean = false): Results? {
        val l = list.orEmpty().mapNotNull { x ->
            val label = x.label.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            if (x.kind !in KINDS) return@mapNotNull null
            val web = x.kind == "link" || x.kind == "release"
            val url = x.url?.takeIf { HTTP.containsMatchIn(it) }
            val entry = x.entry?.trim()?.takeIf { it.isNotEmpty() }
            if (if (web) url == null else entry == null) return@mapNotNull null
            Result(x.kind, label, if (web) url else null, entry, x.at)
        }.sortedByDescending { it.at }.take(20)
        if (l.isEmpty()) return null
        val shown = if (all || l.size <= RESULTS_SHOWN + 1) l else l.take(RESULTS_SHOWN)
        return Results(shown, l.size - shown.size, l.size)
    }

    /** Il compito del registro su cui lavora la sessione: il più recente fra quelli aperti col suo nome. */
    fun task(commitments: List<Commitment>?, session: String): Commitment? =
        commitments.orEmpty().filter { it.session == session && it.state !in CLOSED }.minByOrNull { it.idleS }

    private val CLOSED = setOf("done", "closed", "dropped", "abandoned")

    /** Il prossimo passo: quello del registro, altrimenti il primo dei «Prossimi:» della sessione. */
    fun nextStep(task: Commitment?, steps: List<String>): String? =
        task?.next?.trim()?.takeIf { it.isNotEmpty() } ?: steps.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * La pagina Utilizzo dopo la K: barra della giornata, attività per ora, token per turno e «oggi Y%» per ogni sessione che ha
     * lavorato oggi, la più recente in cima. Una sessione conta se ha un evento da [from] o un turno di oggi.
     */
    fun todaySessions(page: TimelinePage?, data: KpiData?, from: Long): List<String> {
        val last = HashMap<String, Long>()
        fun seen(name: String, at: Long) { last[name] = maxOf(last[name] ?: 0L, at) }
        page?.sessions.orEmpty().forEach { s -> s.events.forEach { e -> if (e.at >= from) seen(s.session, e.at) } }
        data?.turnsToday.orEmpty().forEach { x -> x.turns.forEach { seen(x.session, it.end) } }
        return last.entries.sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key }).map { it.key }
    }
}
