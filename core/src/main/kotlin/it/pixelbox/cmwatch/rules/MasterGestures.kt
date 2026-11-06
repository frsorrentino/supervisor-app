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

    /**
     * Il cambio di lato che continua il gesto (app-lato-animazione-06-11): una molla smorzata senza rimbalzo, rigidità 300
     * (ω ≈ 17): da ferma arriva al 95 % in circa 270 ms, partendo dalla velocità del dito. Le stesse in gesture.ts.
     */
    const val SIDE_SPRING_DAMPING = 1f
    const val SIDE_SPRING_STIFFNESS = 300f
    /** La molla più rigida che si usa per un colpo veloce: ω = 100 al secondo, al 95 % in circa 50 ms. */
    const val LAND_MAX_STIFFNESS = 10000f

    /** La molla d'arrivo di un rilascio: la rigidità e la velocità di partenza da usare. */
    data class Land(val stiffness: Float, val velocity: Float)

    /**
     * Ogni rilascio della master arriva senza rimbalzo (app-altezza-senza-rimbalzo-06-11, Franz 06/11 10:59: «dopo il
     * ridimensionamento della master c'è ancora il rimbalzo»): la molla smorzata in modo critico x(t) = (x0 + (v + ω·x0)·t)·e^(−ωt)
     * passa l'arrivo e torna quando il dito va verso l'arrivo più veloce di ω·|x0|. Allora la molla si fa più rigida, ω = |v| / |x0|,
     * e parte con la velocità del dito senza superare l'arrivo; oltre LAND_MAX_STIFFNESS resta quella e la velocità si limita.
     * `from` è la distanza dall'arrivo (arrivo = 0), `velocity` nelle stesse unità al secondo. Le stesse in gesture.ts (landSpring).
     */
    fun land(from: Float, velocity: Float): Land {
        val w = kotlin.math.sqrt(SIDE_SPRING_STIFFNESS)
        if (from == 0f || from * velocity >= 0f || abs(velocity) <= w * abs(from)) return Land(SIDE_SPRING_STIFFNESS, velocity)
        val wMax = kotlin.math.sqrt(LAND_MAX_STIFFNESS)
        val need = abs(velocity) / abs(from)
        return if (need <= wMax) Land(need * need, velocity) else Land(LAND_MAX_STIFFNESS, -wMax * from)
    }

    /** La posizione al tempo `t` (s) della molla d'arrivo da `from` con `velocity`: per le prove. */
    fun landAt(from: Float, velocity: Float, t: Float): Float {
        val l = land(from, velocity)
        val w = kotlin.math.sqrt(l.stiffness)
        return (from + (l.velocity + w * from) * t) * kotlin.math.exp(-w * t)
    }
}
