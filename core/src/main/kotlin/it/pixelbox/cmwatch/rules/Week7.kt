package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.WeekPoints

/**
 * La settimana ora per ora (proposte 2A e 2B del 25/10, contratto 1.86): la linea cumulata contro il ritmo pari, la
 * proiezione al ritmo delle ultime 48 ore, la spesa di ogni giorno contro il budget di 100/7. Una voce di `steps` vale
 * dall'ora in cui è letta fino alla voce dopo; prima della prima voce si sa solo lo 0 del rinnovo. I campioni partono dal
 * 26/10: un giorno senza il valore del suo inizio non ha barra (niente numeri inventati).
 */
object Week7 {
    const val WEEK_S = 7 * 86400L
    const val HOURS = 168
    /** Il budget di un giorno: la settimana in parti uguali. */
    const val BUDGET = 100.0 / 7
    /** Sotto le 12 ore di campioni il ritmo delle ultime 48 ore è rumore: si usa la media della settimana. */
    private const val MIN_SPAN_H = 12

    /** `points` la linea a gradini fino ad adesso (x da 0 a 1); `fullAt` l'ora del 100% prima del rinnovo; `projected` al rinnovo, o 100. */
    data class Chart(val start: Long, val resetAt: Long, val pct: Int, val points: List<Pair<Float, Int>>, val nowX: Float, val fullAt: Long?, val projected: Int?) {
        fun x(t: Long): Float = ((t - start).toFloat() / WEEK_S).coerceIn(0f, 1f)
    }

    /** Un giorno di 24 ore dal rinnovo: `spent` null se non si sa (prima dei campioni, o `partial` quando partono a metà). */
    data class Day(val index: Int, val start: Long, val spent: Int?, val partial: Boolean, val today: Boolean, val future: Boolean)

    private fun steps(p: WeekPoints) = p.steps.mapNotNull { s -> if (s.size >= 2) s[0] to s[1] else null }.sortedBy { it.first }

    /** Il valore prima dell'ora `h`: l'ultima voce letta in un'ora precedente; null se prima ci sono solo ore senza campioni. */
    private fun before(steps: List<Pair<Int, Int>>, h: Int): Int? {
        if (h <= 0) return 0
        val first = steps.firstOrNull()?.first ?: return null
        if (first > 0 && h <= first) return null
        return steps.lastOrNull { it.first < h }?.second ?: 0
    }

    fun chart(p: WeekPoints, pct: Int, resetAt: Long, now: Long): Chart {
        val st = steps(p)
        val nowH = ((now - p.start) / 3600).toInt().coerceIn(0, HOURS)
        val x = { h: Int -> h.toFloat() / HOURS }
        val pts = mutableListOf<Pair<Float, Int>>()
        st.forEachIndexed { i, (h, v) ->
            if (i > 0) pts += x(h) to st[i - 1].second
            pts += x(h) to v
        }
        if (pts.isNotEmpty()) pts += ((now - p.start).toFloat() / WEEK_S).coerceIn(0f, 1f) to pct
        val rate = rate(st, pct, nowH)
        val left = (resetAt - now).coerceAtLeast(0)
        val fullAt = if (rate == null || pct >= 100) null else (now + Math.ceil((100 - pct) / rate * 3600).toLong()).takeIf { it < resetAt }
        val projected = when {
            pct >= 100 -> 100
            rate == null -> null
            fullAt != null -> 100
            else -> (pct + rate * left / 3600).toInt().coerceIn(0, 100)
        }
        return Chart(p.start, resetAt, pct, pts, ((now - p.start).toFloat() / WEEK_S).coerceIn(0f, 1f), fullAt, projected)
    }

    /** Punti l'ora: dalle ultime 48 ore (o dalla prima voce, se più tarda) con almeno 12 ore, altrimenti la media della settimana. */
    private fun rate(st: List<Pair<Int, Int>>, pct: Int, nowH: Int): Double? {
        val from = nowH - 48
        val base = st.firstOrNull()?.let { first -> if (first.first >= from) first else from to (before(st, from + 1) ?: first.second) }
        if (base != null && nowH - base.first >= MIN_SPAN_H) {
            val r = (pct - base.second).toDouble() / (nowH - base.first)
            return r.takeIf { it > 0 }
        }
        if (nowH < 1 || pct <= 0) return null
        return pct.toDouble() / nowH
    }

    fun days(p: WeekPoints, pct: Int, now: Long): List<Day> {
        val st = steps(p)
        val nowH = ((now - p.start) / 3600).toInt()
        return (0 until 7).map { i ->
            val from = i * 24
            val to = from + 24
            val today = nowH in from until to
            val future = nowH < from
            val first = st.firstOrNull()?.first
            val partial = first != null && first > 0 && first > from && first < to
            val startV = before(st, from)
            val endV = if (today) pct else before(st, to)
            val spent = if (future || startV == null || endV == null) null else (endV - startV).coerceAtLeast(0)
            Day(i, p.start + from * 3600L, spent, partial, today, future)
        }
    }

    /** Da quando ci sono i campioni, se partono dopo il rinnovo; null se coprono la settimana. */
    fun samplesFrom(p: WeekPoints): Long? = steps(p).firstOrNull()?.first?.takeIf { it > 0 }?.let { p.start + it * 3600L }
}
