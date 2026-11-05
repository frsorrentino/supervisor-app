package it.pixelbox.cmwatch.rules

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Un solo riconoscitore per le maniglie della master (app-master-gesti-senza-conflitti-04-11, come web/src/lib/gesture.ts): oltre
 * DRAG_DP è un trascinamento, che annulla i tocchi in sospeso; fermo, al rilascio è un tocco; due tocchi fermi entro DOUBLE_MS
 * sullo stesso posto sono un gesto solo e non due; se la seconda pressione diventa un trascinamento vince il trascinamento.
 */
object MasterGestures {
    const val DRAG_DP = 6f
    const val DOUBLE_MS = 300L

    fun isDrag(dx: Float, dy: Float): Boolean = hypot(dx, dy) > DRAG_DP

    /**
     * I tocchi delle maniglie, senza attese: il tocco agisce subito e un secondo tocco fermo entro DOUBLE_MS sullo stesso posto
     * non ripete l'azione. `dragged`: è partito un trascinamento, il tocco di prima non conta più per il doppio.
     */
    class Taps {
        private var key: String? = null
        private var at = 0L
        /** true se il tocco agisce. */
        fun tap(k: String, now: Long): Boolean {
            if (key == k && now - at <= DOUBLE_MS) { key = null; return false }
            key = k; at = now
            return true
        }
        fun dragged() { key = null }
    }

    /**
     * La corsa breve (app-master-corsa-breve-05-11, Franz 05/11 11:23: «serve che si possa rilasciare e passare alla posizione
     * della direzione presa, molto prima nella corsa»). Fra posizioni fisse al rilascio conta la direzione: oltre COMMIT_DP nella
     * direzione presa, o con un colpo oltre FLICK_DP_S, la master va alla posizione dopo in quella direzione; sotto torna dov'era.
     * Come i fogli di iOS la velocità si proietta (PROJECT_S: dove arriverebbe il dito rallentando): un tratto o un colpo
     * lunghi saltano più posizioni. Le stesse soglie sulla web (gesture.ts), in px CSS.
     */
    const val COMMIT_DP = 24f
    const val FLICK_DP_S = 500f
    /** La proiezione di iOS, decelerazione 0,998 per millisecondo: v · 0,998 / (1 − 0,998) / 1000 secondi. */
    const val PROJECT_S = 0.499f

    /**
     * La posizione al rilascio fra `stops` (le posizioni lungo l'asse del gesto, in ordine crescente), partendo da `from`, con il
     * dito spostato di `delta` e la velocità `velocity` al rilascio, nelle stesse unità al secondo.
     */
    fun step(stops: List<Float>, from: Int, delta: Float, velocity: Float = 0f): Int {
        if (stops.size < 2) return from
        val dir = when {
            abs(velocity) >= FLICK_DP_S -> if (velocity > 0f) 1 else -1
            abs(delta) >= COMMIT_DP -> if (delta > 0f) 1 else -1
            else -> return from
        }
        val next = (from + dir).coerceIn(0, stops.lastIndex)
        val at = stops[from] + delta + velocity * PROJECT_S
        val near = stops.indices.minBy { abs(stops[it] - at) }
        return if (dir > 0) maxOf(next, near) else minOf(next, near)
    }
}
