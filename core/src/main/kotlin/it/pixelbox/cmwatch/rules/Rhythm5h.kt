package it.pixelbox.cmwatch.rules

/**
 * Il ritmo delle cinque ore (proposte 1A e 1B del 25/10): quanto della finestra è passato contro quanta quota è usata, e
 * la proiezione al ritmo dell'ultima ora fino al 100% o al reset. Vale la regola di QuotaHistory: niente numeri
 * inventati, senza un ritmo misurato la proiezione non c'è.
 */
object Rhythm5h {
    /** Sotto i 10 minuti fra due campioni il ritmo è rumore. */
    private const val MIN_SPAN_S = 600L
    private const val HOUR_S = 3600L

    /**
     * `elapsed` e `ahead` in punti; `points` la linea della finestra (x da 0 a 1); `fullAt` l'ora del 100% se cade prima
     * del reset, `idleMin` i minuti fermi fra il 100% e il reset; `projected` dove si arriva al reset, o 100.
     */
    data class View(
        val start: Long, val resetAt: Long, val pct: Int, val elapsed: Int, val ahead: Int,
        val points: List<Pair<Float, Int>>, val nowX: Float, val fullAt: Long?, val idleMin: Int?, val projected: Int?,
    ) {
        fun x(t: Long): Float = ((t - start).toFloat() / QuotaHistory.WINDOW_S).coerceIn(0f, 1f)
    }

    fun of(samples: List<QuotaHistory.Sample>, pct: Int, resetAt: Long, now: Long): View {
        val start = resetAt - QuotaHistory.WINDOW_S
        val elapsed = ((now - start) * 100 / QuotaHistory.WINDOW_S).toInt().coerceIn(0, 100)
        val points = QuotaHistory.window(samples, resetAt)
        val x = { t: Long -> ((t - start).toFloat() / QuotaHistory.WINDOW_S).coerceIn(0f, 1f) }
        val rate = rate(points)
        val left = (resetAt - now).coerceAtLeast(0)
        val fullAt = if (rate == null || pct >= 100) null else (now + Math.ceil((100 - pct) / rate).toLong()).takeIf { it < resetAt }
        val projected = when {
            pct >= 100 -> 100
            rate == null -> null
            fullAt != null -> 100
            else -> (pct + rate * left).toInt().coerceIn(0, 100)
        }
        return View(
            start, resetAt, pct, elapsed, pct - elapsed, points.map { x(it.ts) to it.pct }, x(now),
            fullAt, fullAt?.let { ((resetAt - it) / 60).toInt() }, projected,
        )
    }

    /** Punti al secondo dal primo campione nell'ora prima dell'ultimo fino all'ultimo; null senza salita misurabile. */
    private fun rate(points: List<QuotaHistory.Sample>): Double? {
        val last = points.lastOrNull() ?: return null
        val base = points.first { it.ts >= last.ts - HOUR_S }
        val span = last.ts - base.ts
        if (span < MIN_SPAN_S || last.pct <= base.pct) return null
        return (last.pct - base.pct).toDouble() / span
    }
}
