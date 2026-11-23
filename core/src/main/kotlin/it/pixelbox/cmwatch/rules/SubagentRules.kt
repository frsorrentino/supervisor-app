package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Subagent
import it.pixelbox.cmwatch.contract.Subagents

/**
 * app-sottoagenti-23-11 (Franz 23/11 13:06: «sul terminale si possono vedere, su supervisor non ce n'è traccia»; 13:26 «procedi con la
 * consigliata», la variante A): i sottoagenti di una sessione come righe sotto la sessione in «Al lavoro adesso», in una sezione che
 * nasce chiusa e si apre col toggle delle altre. Contratto 1.158: `sessions[].subagents`, tutti con l'op `subagents`. Come subagents.ts,
 * coi casi di contract/subagents-sample.json.
 */
object SubagentRules {
    /** Il riassunto della sezione: «2 al lavoro, 5 finiti». [done] sono tutti gli altri lanciati, anche quelli di giorni fa. */
    data class Summary(val running: Int, val done: Int)
    fun summary(s: Subagents): Summary = Summary(s.running.coerceAtLeast(0), (s.n - s.running).coerceAtLeast(0))

    /**
     * Una riga: [title] la descrizione data al lancio (il tipo se è vuota); [running] col pallino blu, altrimenti la spunta;
     * [meta] tipo, modello, «in background», fase del Workflow; [tokens] solo da finito; [seconds] da quanto lavora, o quanto ha
     * lavorato (null se non si sa: un fork senza trascrizione).
     */
    data class Row(val id: String, val title: String, val running: Boolean, val type: String, val model: String?, val bg: Boolean, val phase: String?, val tokens: Pair<Long, Long>?, val seconds: Long?)
    fun row(a: Subagent, now: Long): Row {
        val running = !a.finished
        val secs = when {
            running -> a.started?.let { (now - it).coerceAtLeast(0) }
            a.started != null && a.ended != null -> (a.ended - a.started).coerceAtLeast(0)
            else -> null
        }
        return Row(
            a.id, a.desc.trim().ifEmpty { a.type }, running, a.type, a.model?.let(::model), a.bg, a.phase,
            a.tokens?.takeIf { !running }?.let { it.input to it.out }, secs,
        )
    }
    fun rows(s: Subagents, now: Long): List<Row> = s.items.map { row(it, now) }

    /** «Tutti e 7»: quando la sessione ne ha lanciati più di quelli nelle righe. */
    fun more(s: Subagents): Int? = s.n.takeIf { it > s.items.size }

    /** «Haiku 5.5» da «claude-haiku-5-5»; un alias chiesto senza trascrizione resta com'è (`inherit`, `haiku`). */
    fun model(id: String): String = ContiRules.modelName(id)

    /** «69k», «211k», «0,6k», «1,2M»: i token corti, con la virgola in italiano. */
    fun tokens(n: Long, en: Boolean = false): String {
        val sep = if (en) "." else ","
        fun one(x: Double) = String.format(java.util.Locale.ROOT, "%.1f", x).replace(".", sep).removeSuffix("${sep}0")
        return when {
            n >= 1_000_000 -> "${one(n / 1_000_000.0)}M"
            n >= 10_000 -> "${Math.round(n / 1000.0)}k"
            else -> "${one(n / 1000.0)}k"
        }
    }

    /** «18 s», «4 m», «1 h 05»: quanto ha lavorato o da quanto lavora. Fra numero e unità uno spazio non separabile. */
    fun duration(s: Long): String {
        val nb = ' '
        return when {
            s < 60 -> "$s${nb}s"
            s < 3600 -> "${s / 60}${nb}m"
            else -> "${s / 3600}${nb}h$nb${(s % 3600 / 60).toString().padStart(2, '0')}"
        }
    }
}
