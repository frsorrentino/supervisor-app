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

    /** Il giorno della settimana di quota, da 1 a 7; null senza rinnovo o a settimana finita. */
    fun weekDay(resetAt: Long?, now: Long): Int? {
        if (resetAt == null || resetAt <= now) return null
        return (((now - (resetAt - W7_S)) / 86400).toInt() + 1).coerceIn(1, 7)
    }

    /**
     * Il dato delle 5 ore che vale ancora: una lettura vecchia a finestra finita non dice più niente («Passata, non si mostra»,
     * PhoneBoard), e l'account conta come senza 5 ore. Una lettura fresca resta com'è. Stessa regola di `fiveHourLive` nel web.
     */
    fun fiveHourLive(h5: Int?, resetH5: Long?, stale: Boolean, now: Long): Int? = h5?.takeUnless { stale && (resetH5 == null || resetH5 <= now) }

    /** Gli account da mostrare: i freschi e, se non ce n'è nessuno, tutti (regola del 05/10, come `PhoneOverview.lineRings`). */
    fun shownQuota(quota: Map<String, it.pixelbox.cmwatch.contract.QuotaAccount>): List<Map.Entry<String, it.pixelbox.cmwatch.contract.QuotaAccount>> =
        quota.entries.filter { !it.value.stale }.ifEmpty { quota.entries.toList() }

    data class Bar(val week: Boolean, val pct: Int, val resetAt: Long?, val windowS: Long, val delta: Int?, val day: Int?)

    /**
     * quota-senza-cinque-ore: la barra di un account. Anthropic toglie il limite di 5 ore a tappe e per account: conta il dato
     * assente, mai il nome del piano. Senza `h5` la settimana, col giorno e il ritmo; senza nessuno dei due null, mai uno 0 %.
     */
    fun bar(h5: Int?, w7: Int?, resetH5: Long?, resetW7: Long?, now: Long, stale: Boolean = false): Bar? = when (val live = fiveHourLive(h5, resetH5, stale, now)) {
        null -> weekBar(w7, resetW7, now)
        else -> Bar(false, live.coerceIn(0, 100), resetH5, H5_S, null, null)
    }

    private fun weekBar(w7: Int?, resetW7: Long?, now: Long): Bar? = when {
        w7 != null -> Bar(true, w7.coerceIn(0, 100), resetW7, W7_S, weekDelta(w7, resetW7, now), weekDay(resetW7, now))
        else -> null
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
