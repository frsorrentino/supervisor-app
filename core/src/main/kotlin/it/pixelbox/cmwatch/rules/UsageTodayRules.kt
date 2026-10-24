package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.UsageToday
import kotlin.math.roundToInt

/**
 * «Oggi · quota per sessione» (Franz 24/10 19:46, B5 del mockup): al posto degli eventi per ora, quanta parte della
 * giornata dell'account ha usato ogni sessione. Le voci sotto il 3 % stanno insieme in «altre N». Stessa regola di usageToday.ts.
 */
object UsageTodayRules {
    const val SMALL = 0.03
    data class Row(val session: String, val pct: Int)
    data class Others(val n: Int, val pct: Int)
    data class Rows(val rows: List<Row>, val others: Others?)

    fun rows(list: List<UsageToday>?): Rows? {
        if (list.isNullOrEmpty()) return null
        val sorted = list.sortedByDescending { it.share }
        val (big, small) = sorted.partition { it.share >= SMALL }
        fun pct(x: Double) = (x * 100).roundToInt()
        return Rows(big.map { Row(it.session, pct(it.share)) }, small.takeIf { it.isNotEmpty() }?.let { Others(it.size, pct(it.sumOf { u -> u.share })) })
    }
}
