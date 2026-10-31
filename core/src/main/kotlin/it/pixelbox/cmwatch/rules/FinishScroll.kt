package it.pixelbox.cmwatch.rules

/**
 * Quando la home può scorrere da sola fino alla voce di un lavoro finito (docs/plans/2026-10-31-animazioni-app.md):
 * non deve mai rubare lo scorrimento. Se una sola condizione manca, la voce diventa verde dove si trova e basta.
 */
object FinishScroll {
    const val STILL_MS = 2_000L
    const val GAP_MS = 10_000L

    data class Scene(
        val fingerDown: Boolean, val listMoving: Boolean, val sinceTouchMs: Long, val wasOnScreen: Boolean,
        val writing: Boolean, val sheetOpen: Boolean, val distancePx: Float, val viewportPx: Float, val sinceLastMs: Long,
    )

    fun allowed(s: Scene): Boolean =
        !s.fingerDown && !s.listMoving && s.sinceTouchMs >= STILL_MS && s.wasOnScreen && !s.writing && !s.sheetOpen &&
            kotlin.math.abs(s.distancePx) <= s.viewportPx && s.sinceLastMs >= GAP_MS
}
