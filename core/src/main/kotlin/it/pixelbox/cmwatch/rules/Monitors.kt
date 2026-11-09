package it.pixelbox.cmwatch.rules

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * I Monitoraggi (app-monitoraggi-09-11, mockup approvati da Franz il 09/11 alle 08:32, sezione in «Dalla master» dalle 09:08): cose
 * esterne che la master tiene d'occhio (prezzi, adozione dei plugin) restituite come grafici. Una riga per monitoraggio nella home
 * e una scheda col grafico nella forma scelta da un catalogo chiuso. monitors.ts nella web, con la stessa fixture.
 *
 * Il modello è provvisorio: segue i dati dei mockup (core/src/test/resources/monitors-mockup.json) finché non esce il contratto
 * `monitors` del relay (supervisor-monitoraggi-09-11); allora i nomi dei campi si allineano a quello.
 */
object Monitors {
    @Serializable data class Page(val items: List<Monitor> = emptyList())

    /** `status`: `ok` (pallino verde) o `look` (da guardare: una soglia è scattata, l'unico caso in ambra). */
    @Serializable data class Monitor(
        val id: String, val name: String, val status: String = "ok", val text: String = "", val value: String? = null,
        val spark: List<Double> = emptyList(), val source: String? = null, val at: Long? = null, val summary: String? = null,
        val alerts: String? = null, val example: String? = null, val notes: List<String> = emptyList(), val charts: List<Chart> = emptyList(),
    ) { val look get() = status == "look" }

    /**
     * Un grafico: `form` dal catalogo chiuso, `days` l'asse dei giorni (le serie hanno un valore per giorno, null dove non c'è
     * ancora), `marks` le linee tratteggiate di un giorno (il Black Friday), `step` i prezzi che cambiano a gradini.
     */
    @Serializable data class Chart(
        val form: String, val title: String? = null, val sub: String? = null, val unit: String? = null, val step: Boolean = false,
        val days: List<String> = emptyList(), val marks: List<Mark> = emptyList(), val series: List<Series> = emptyList(),
        val value: String? = null, val delta: String? = null, val limit: Double? = null, val rows: List<Row> = emptyList(),
    )
    @Serializable data class Series(
        val name: String? = null, val sub: String? = null, val values: List<Double?> = emptyList(), val value: String? = null,
        val note: String? = null, val delta: String? = null, val look: Boolean = false,
    )
    @Serializable data class Mark(val day: String, val label: String)
    @Serializable data class Row(val name: String, val value: String, val note: String? = null, val look: Boolean = false)

    private val json = Json { ignoreUnknownKeys = true }
    fun decode(text: String): Page = json.decodeFromString(Page.serializer(), text)

    /**
     * Il catalogo chiuso delle forme (references/choosing-a-form.md della skill dataviz): un valore con variazione e sparkline, una
     * serie nel tempo, più serie con gli stessi giorni (un grafico piccolo per serie), conteggi per giorno, un valore contro un
     * limite, righe, sì/no coi giorni. Una forma che l'app non conosce non si disegna.
     */
    enum class Form(val key: String) { TILE("tile"), LINE("line"), MULTIPLES("multiples"), BARS("bars"), METER("meter"), TABLE("table"), STATUS("status") }
    fun formOf(c: Chart): Form? = Form.entries.firstOrNull { it.key == c.form }

    /** Quanti sono da guardare: la riga chiusa di «Dalla master» nomina i Monitoraggi solo allora, in ambra; altrimenti non contano. */
    fun looking(items: List<Monitor>): Int = items.count { it.look }

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

    /** Il punto più basso e l'ultimo di una serie (gli indici dei giorni), dove il grafico mette il pallino; null senza valori. */
    fun minIndex(values: List<Double?>): Int? = values.withIndex().filter { it.value != null }.minByOrNull { it.value!! }?.index
    fun lastIndex(values: List<Double?>): Int? = values.indexOfLast { it != null }.takeIf { it >= 0 }
    /** Il massimo, segnato sulla linea come nel mockup C («max 135»). */
    fun maxIndex(values: List<Double?>): Int? = values.withIndex().filter { it.value != null }.maxByOrNull { it.value!! }?.index

    /** Al tocco, il giorno più vicino al dito: `x` da 0 a 1 sulla larghezza del grafico, `n` giorni. */
    fun dayAt(x: Float, n: Int): Int = if (n <= 1) 0 else kotlin.math.round(x.coerceIn(0f, 1f) * (n - 1)).toInt()

    /** Quanto è pieno il metro, da 0 a 1, e se il valore ha passato il limite. */
    fun meter(value: Double, limit: Double): Pair<Float, Boolean> =
        (if (limit <= 0) 1f else (value / limit / 1.25).toFloat().coerceIn(0f, 1f)) to (value >= limit)
}
