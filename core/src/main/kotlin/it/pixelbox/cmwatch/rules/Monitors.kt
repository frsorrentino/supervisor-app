package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.MonitorCard
import it.pixelbox.cmwatch.contract.MonitorSeries
import it.pixelbox.cmwatch.contract.MonitorsSummary

/**
 * I Monitoraggi (app-monitoraggi-09-11, mockup approvati da Franz il 09/11 alle 08:32, sezione in «Dalla master» dalle 09:08): cose
 * esterne che la master tiene d'occhio restituite come grafici. Contratto 1.132: `/state.monitors` col conto, l'op `monitors` con
 * le schede, una forma per scheda da un catalogo chiuso. monitors.ts nella web, con gli stessi casi.
 */
object Monitors {
    /**
     * Il catalogo chiuso delle forme (references/choosing-a-form.md della skill dataviz): un valore con variazione e sparkline, una
     * serie nel tempo, più serie con gli stessi giorni (un grafico piccolo per serie), conteggi per giorno, un valore contro un
     * limite, righe, sì/no coi giorni. Una forma che l'app non conosce non si disegna.
     */
    enum class Form(val key: String) { TILE("tile"), LINE("line"), MULTIPLES("multiples"), BARS("bars"), METER("meter"), TABLE("table"), STATUS("status") }
    fun formOf(c: MonitorCard): Form? = Form.entries.firstOrNull { it.key == c.form }

    /** Da guardare: una soglia è scattata adesso (`status: watch`), l'unico caso in ambra. */
    fun look(c: MonitorCard): Boolean = c.status == "watch"

    /** Quanti sono da guardare: la riga chiusa di «Dalla master» nomina i Monitoraggi solo allora; dallo stato se c'è, se no dalle schede. */
    fun looking(summary: MonitorsSummary?, items: List<MonitorCard>?): Int = summary?.watch ?: items.orEmpty().count(::look)

    /** I valori di una serie, nell'ordine dei punti. */
    fun values(s: MonitorSeries): List<Double?> = s.points.map { p -> p.getOrNull(1) }
    /** Gli istanti dei punti (secondi). */
    fun times(s: MonitorSeries): List<Long> = s.points.map { p -> p.getOrNull(0)?.toLong() ?: 0L }

    /** La sparkline di una riga: i valori della prima serie. */
    fun spark(c: MonitorCard): List<Double> = c.series.firstOrNull()?.let(::values).orEmpty().filterNotNull()

    /** La frase sotto il titolo: quella della master se c'è, altrimenti la variazione. */
    fun line(c: MonitorCard): String = c.verdict?.takeIf { it.isNotBlank() } ?: c.delta.orEmpty()

    /**
     * Una scala per grafico, mai due assi: da un multiplo del passo sotto il minimo a uno sopra il massimo, con circa tre tacche; le
     * barre partono da zero. Il passo è 1, 2, 2,5 o 5 per una potenza di dieci.
     */
    data class Scale(val lo: Double, val hi: Double, val ticks: List<Double>) {
        /** Da 0 (in basso) a 1 (in alto). */
        fun y(v: Double): Float = if (hi <= lo) 0.5f else ((v - lo) / (hi - lo)).toFloat()
    }
    fun scale(values: List<Double?>, fromZero: Boolean = false): Scale {
        val vs = values.filterNotNull()
        if (vs.isEmpty()) return Scale(0.0, 1.0, listOf(0.0, 1.0))
        val min = if (fromZero) minOf(0.0, vs.min()) else vs.min()
        val max = vs.max()
        if (max == min) return Scale(min - 1, max + 1, listOf(min))
        val step = nice((max - min) / 2)
        val lo = kotlin.math.floor(min / step) * step
        val hi = kotlin.math.ceil(max / step) * step
        val n = kotlin.math.round((hi - lo) / step).toInt()
        return Scale(lo, hi, (0..n).map { lo + it * step })
    }
    private fun nice(raw: Double): Double {
        val p = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(raw)))
        return listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * p >= raw - 1e-9 } * p
    }

    /** Il punto più basso, il più alto e l'ultimo di una serie (indici), dove il grafico mette il pallino; null senza valori. */
    fun minIndex(values: List<Double?>): Int? = values.withIndex().filter { it.value != null }.minByOrNull { it.value!! }?.index
    fun maxIndex(values: List<Double?>): Int? = values.withIndex().filter { it.value != null }.maxByOrNull { it.value!! }?.index
    fun lastIndex(values: List<Double?>): Int? = values.indexOfLast { it != null }.takeIf { it >= 0 }

    /**
     * L'asse del tempo di un grafico: da `from` a `to` (secondi), dove sta un istante, da 0 a 1. Con più serie (multiples) l'asse è
     * lo stesso per tutte: i giorni uguali.
     */
    fun span(c: MonitorCard): Pair<Long, Long> {
        val all = c.series.flatMap(::times)
        return (all.minOrNull() ?: 0L) to (all.maxOrNull() ?: 0L)
    }
    fun x(t: Long, span: Pair<Long, Long>): Float = if (span.second <= span.first) 1f else ((t - span.first).toDouble() / (span.second - span.first)).toFloat()

    /** Al tocco, il punto più vicino al dito: `x` da 0 a 1 sull'asse del tempo. */
    fun nearest(times: List<Long>, x: Float, span: Pair<Long, Long>): Int? =
        times.indices.minByOrNull { i -> kotlin.math.abs(x(times[i], span) - x.coerceIn(0f, 1f)) }

    /** Quanto è pieno il metro, da 0 a 1 (`pct` del relay, se no l'ultimo valore su `max`); la soglia, se c'è, nella stessa scala. */
    fun meter(c: MonitorCard): Pair<Float, Float?> {
        val last = c.series.firstOrNull()?.last
        val fill = (c.pct?.div(100) ?: if (last != null && (c.max ?: 0.0) > 0) last / c.max!! else 0.0).toFloat().coerceIn(0f, 1f)
        val mark = c.alerts.firstOrNull { it.kind == "above" || it.kind == "below" }?.value?.let { v -> c.max?.takeIf { it > 0 }?.let { (v / it).toFloat().coerceIn(0f, 1f) } }
        return fill to mark
    }

    /** Status: sì (1) o no (0) per ogni giorno. */
    fun up(v: Double?): Boolean = (v ?: 0.0) > 0

    private val LINK = Regex("#/monitor/([A-Za-z0-9][A-Za-z0-9_-]{0,63})$")
    /** Contratto 1.132: l'id di un monitoraggio da un link `<web_app_url>/#/monitor/<id>`; null per ogni altro indirizzo. */
    fun idOf(link: String): String? = LINK.find(link.trim())?.groupValues?.get(1)

    /**
     * Il tasto «Aggiorna adesso» di un monitoraggio (contratto 1.133, app-monitoraggi-aggiorna-10-11, Franz 10/11 13:03: «un tasto per ogni
     * monitoraggio che effettui l'aggiornamento immediato»). Lo stesso su telefono, tablet e web.
     */
    sealed interface Refresh {
        /** Si può toccare. */
        data object Ready : Refresh
        /** Gira: l'icona ruota e il tasto non si ritocca. */
        data object Running : Refresh
        /** Troppo presto: spento fino a [until] (epoca in secondi). */
        data class Wait(val until: Long) : Refresh
    }

    /**
     * Il tasto adesso ([now] in secondi), o null se non c'è: monitoraggio non aggiornabile, o relay senza l'op `monitor_refresh` in
     * [ops]. [asked]: il tocco è partito e la risposta non è ancora arrivata, gira già.
     */
    fun refresh(c: MonitorCard, ops: List<String>?, now: Long, asked: Boolean = false): Refresh? {
        if (!c.refreshable || ops?.contains("monitor_refresh") != true) return null
        val r = c.refresh
        if (asked || r?.state == "running") return Refresh.Running
        val until = r?.retryAt
        return if (until != null && until > now) Refresh.Wait(until) else Refresh.Ready
    }

    /** Com'è andato l'ultimo aggiornamento, da dire in una riga sotto il tasto. */
    sealed interface Outcome {
        /** Riuscito alle [at]. */
        data class Done(val at: Long) : Outcome
        /** Non riuscito: [message] la riga del relay o di supervisor, null se non ne ha (la frase la mette l'app; [timeout] tempo scaduto); il valore di prima resta. */
        data class Failed(val message: String?, val timeout: Boolean = false) : Outcome
    }

    /** L'esito dell'ultimo aggiornamento, o null se non è mai girato o sta girando. [failed]: il rifiuto del relay al tocco, che vince. */
    fun outcome(c: MonitorCard, failed: String? = null): Outcome? {
        failed?.takeIf { it.isNotBlank() }?.let { return Outcome.Failed(it.lines().first().take(200)) }
        val r = c.refresh ?: return null
        return when (r.state) {
            "ok" -> Outcome.Done(r.ended ?: c.at ?: return null)
            "error" -> Outcome.Failed(r.message?.takeIf { it.isNotBlank() }?.lines()?.first()?.take(200), timeout = r.rc == 124)
            else -> null
        }
    }

    /** Ogni quanti secondi rileggere la scheda mentre gira, e fino a quando al più (`timeout_s`, o due minuti). */
    const val POLL_S = 3L
    fun pollUntil(c: MonitorCard, now: Long): Long = (c.refresh?.started ?: now) + (c.refresh?.timeoutS ?: 120) + POLL_S

    /**
     * La sezione Monitoraggi nella home (app-monitoraggi-attesa-10-11, Franz 10/11 15:12: «Perché da app non vedo la scheda monitoraggi?»):
     * c'è appena `/state.monitors` dice n > 0, col numero del riassunto e le righe «in arrivo» finché le schede non arrivano. Prima
     * spariva quando la prima lettura tardava più di 30 s (il comando era rimasto in fila 23-51 s sul relay).
     */
    data class Section(val count: Int, val loading: Boolean)
    fun section(summary: MonitorsSummary?, items: List<MonitorCard>?): Section? = when {
        !items.isNullOrEmpty() -> Section(items.size, false)
        (summary?.n ?: 0) > 0 -> Section(summary!!.n, true)
        else -> null
    }

    /** Dopo quanti secondi riprovare la lettura delle schede al tentativo [attempt] (0 il primo): 5, 10, 20, 40, poi ogni 60. */
    fun retryAfter(attempt: Int): Long = minOf(60L, 5L shl attempt.coerceIn(0, 4))
}
