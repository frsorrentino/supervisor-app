package it.pixelbox.cmwatch.rules

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
}
