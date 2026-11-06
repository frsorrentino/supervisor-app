package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MasterLanding.Rect

/**
 * Il blocco della master che cambia forma mentre se ne cambia l'altezza col dito (app-master-morph-m3-06-11, Franz 06/11
 * 01:12-01:14, «ok m3»), come lib/morphBlob.ts nella web: posizione, misure e raggi vanno dal posto di partenza a quello
 * d'arrivo con l'avanzamento del dito; il bordo verso il dito si allunga a goccia; il contenuto sfuma mentre compare la presa.
 * Al rilascio la molla del cambio di lato completa la forma; annullato, torna indietro. Misure in dp.
 */
object MorphBlob {
    data class Shape(val x: Float, val y: Float, val w: Float, val h: Float, val rTop: Float, val rBottom: Float)

    /** Quanto si allunga al massimo il bordo verso il dito, e quanto lo segue. */
    const val STRETCH_MAX = 44f
    private const val STRETCH_K = 0.4f
    /** Il raggio degli angoli in mezzo al cambio. */
    const val RADIUS = 14f

    /** Quanto è avanti il dito fra partenza e arrivo, da 0 a 1, misurato sul bordo alto (o sull'altezza, se il bordo non si sposta). */
    fun progress(from: Rect, to: Rect, dy: Float): Float {
        val span = (to.y - from.y).takeIf { it != 0f } ?: (from.h - to.h)
        if (span == 0f) return if (dy == 0f) 0f else 1f
        return (dy / span).coerceIn(0f, 1f) + 0f   // + 0f: niente -0.0 con un dito fermo
    }

    /**
     * La forma all'avanzamento [p], tirata verso il dito [fx], [fy]: il bordo alto si allunga verso il dito fino a [STRETCH_MAX] e
     * si arrotonda; il centro si sposta appena verso il dito.
     */
    fun shape(from: Rect, to: Rect, p: Float, fx: Float, fy: Float): Shape {
        val e = 1f - (1f - p) * (1f - p)
        fun l(a: Float, b: Float) = a + (b - a) * e
        val x = l(from.x, to.x); val w = l(from.w, to.w)
        var y = l(from.y, to.y); var h = l(from.h, to.h)
        val stretch = minOf(STRETCH_MAX, maxOf(0f, y - fy) * STRETCH_K)
        y -= stretch; h += stretch
        val nudge = ((fx - (x + w / 2f)) * 0.06f).coerceIn(-24f, 24f)
        return Shape(x + nudge, y, w, h, RADIUS + stretch * 0.5f, RADIUS * (1f - kotlin.math.abs(2f * e - 1f)))
    }

    /** Il posto fermo, con gli angoli dritti. */
    fun still(r: Rect): Shape = Shape(r.x, r.y, r.w, r.h, 0f, 0f)

    /** Fra due forme, per la molla del rilascio. */
    fun mix(a: Shape, b: Shape, q: Float): Shape {
        fun l(u: Float, v: Float) = u + (v - u) * q
        return Shape(l(a.x, b.x), l(a.y, b.y), l(a.w, b.w), l(a.h, b.h), l(a.rTop, b.rTop), l(a.rBottom, b.rBottom))
    }

    /** Il contenuto della master di partenza sfuma presto; la presa della testata compare a metà. */
    fun contentAlpha(p: Float): Float = (1f - p * 1.6f).coerceIn(0f, 1f)
    fun gripAlpha(p: Float): Float = (p * 1.4f - 0.4f).coerceIn(0f, 1f)
}
