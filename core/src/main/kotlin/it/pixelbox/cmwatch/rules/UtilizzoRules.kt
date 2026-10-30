package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.KpiWaitDay

/** Il blocco Utilizzo aperto (scelta D di Franz, 30/10 20:11, compito app-utilizzo-d-30-10; utilizzo.ts nella web). */
object UtilizzoRules {
    const val H5_S = 5 * 3600L
    const val W7_S = 7 * 86400L

    /** Quanto è passato della finestra, da 0 a 100; null senza ora di ripartenza o a finestra finita. La tacca e «adesso» usano questo. */
    fun elapsedPct(resetAt: Long?, windowS: Long, now: Long): Float? {
        if (resetAt == null || resetAt <= now) return null
        return ((1f - (resetAt - now).toFloat() / windowS) * 100f).coerceIn(0f, 100f)
    }

    /** Punti sopra (positivi) o sotto il ritmo pari della settimana; null senza dati. */
    fun weekDelta(w7: Int?, resetAt: Long?, now: Long): Int? {
        val e = elapsedPct(resetAt, W7_S, now) ?: return null
        return Math.round((w7 ?: return null) - e)
    }

    /** L'ora con più prompt e commit, null in una giornata vuota. */
    fun peakHour(perHour: List<Pair<Int, Int>>, firstHour: Int): Int? {
        val i = perHour.indices.maxByOrNull { perHour[it].first + perHour[it].second } ?: return null
        return if (perHour[i].first + perHour[i].second > 0) (firstHour + i) % 24 else null
    }

    /** L'ultima mediana delle attese e quante chiuse in tutto; null senza attese chiuse. */
    data class Waits(val medianMin: Int, val closed: Int)
    fun waits(week: List<KpiWaitDay>): Waits? {
        val last = week.lastOrNull { it.medianMin != null } ?: return null
        return Waits(Math.round(last.medianMin!!).toInt(), week.sumOf { it.n })
    }
}
