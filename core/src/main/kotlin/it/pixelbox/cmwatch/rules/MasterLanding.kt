package it.pixelbox.cmwatch.rules

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * La master del tablet si sposta e si misura coi gesti come nella PWA larga (`web/src/lib/masterLanding.ts`, Franz 03/11
 * 22:06-23:01, app-master-trascina-03-11; sul tablet app-tablet-come-pwa-03-11): la testata trascinata cambia solo il lato;
 * il bordo verso le sessioni allarga e stringe, con due scatti (colonna e metà schermo) e la misura libera fra i due; il
 * bordo superiore cambia l'altezza a scatti. Mentre si trascina compaiono i segni dove si può lasciare: quello di arrivo è
 * grande, e un segno vicino al dito si allunga e gli va incontro (la calamita). Misure in px dello schermo, senza la pagina.
 */
object MasterLanding {
    enum class Side { LEFT, RIGHT }
    enum class Height { COLUMN, HALF, COMPACT, BAR }
    enum class WidthStop { COLUMN, HALF_SCREEN }
    data class Rect(val x: Float, val y: Float, val w: Float, val h: Float)
    data class Zone<K>(val key: K, val rect: Rect)
    /** Un segno di arrivo: verticale sui lati e sulla larghezza, orizzontale sull'altezza. */
    data class Mark<K>(val key: K, val x: Float, val y: Float, val vertical: Boolean, val on: Boolean)

    /** La colonna stretta della master e il minimo della misura libera, in px come nella PWA. */
    const val COLUMN = 420f
    const val MIN_W = 360f
    /** Entro questa distanza da uno scatto la larghezza ci si ferma. */
    const val SNAP = 32f
    /** Intorno al centro il lato resta quello di adesso. */
    const val SIDE_MARGIN = 48f
    /** La banda d'altezza di adesso tiene finché il dito non la supera di tanto. */
    const val HEIGHT_MARGIN = 28f

    fun sideAt(screenW: Float, x: Float): Side = if (x < screenW / 2) Side.LEFT else Side.RIGHT
    fun sideDragged(screenW: Float, x: Float, now: Side): Side = if (abs(x - screenW / 2) < SIDE_MARGIN) now else sideAt(screenW, x)

    /** La larghezza mentre si trascina il bordo: fra MIN_W e il 60 % dello schermo. */
    fun clampWidth(px: Float, screenW: Float): Float = max(MIN_W, min(screenW * 0.6f, px)).roundToInt().toFloat()
    /** Al rilascio: vicina a uno scatto (colonna, metà schermo) ci si ferma, altrimenti resta dove la si lascia. */
    fun snapWidth(px: Float, screenW: Float): Float {
        val w = clampWidth(px, screenW)
        for (s in listOf(COLUMN, (screenW / 2).roundToInt().toFloat())) if (abs(w - s) <= SNAP) return s
        return w
    }
    /** La misura lasciata conta come «stretta» sotto il punto di mezzo fra colonna e metà schermo. */
    fun narrow(w: Float, screenW: Float): Boolean = w < (COLUMN + screenW / 2) / 2

    /**
     * Le bande del bordo superiore sulla colonna della master, dall'alto: colonna intera, sotto la home a metà, compatta, e
     * in fondo la riga. `under` false (nessuna sessione aperta, la master non sta sotto la home): solo colonna e riga.
     */
    fun heightZones(col: Rect, under: Boolean): List<Zone<Height>> {
        val cuts = if (under) listOf(Height.COLUMN to 0.15f, Height.HALF to 0.6f, Height.COMPACT to 0.88f, Height.BAR to 1f)
        else listOf(Height.COLUMN to 0.85f, Height.BAR to 1f)
        var y = col.y
        return cuts.map { (key, f) -> val end = col.y + col.h * f; Zone(key, Rect(col.x, y, col.w, end - y)).also { y = end } }
    }
    fun heightAt(zs: List<Zone<Height>>, y: Float): Height = zs.firstOrNull { y < it.rect.y + it.rect.h }?.key ?: zs.last().key
    fun heightDragged(zs: List<Zone<Height>>, y: Float, now: Height): Height {
        val near = listOf(heightAt(zs, y - HEIGHT_MARGIN), heightAt(zs, y), heightAt(zs, y + HEIGHT_MARGIN))
        return if (now in near && near[1] != now && (near[0] == now || near[2] == now)) now else near[1]
    }

    fun sideMarks(screenW: Float, screenH: Float, on: Side): List<Mark<Side>> = listOf(
        Mark(Side.LEFT, 14f, screenH / 2, vertical = true, on = on == Side.LEFT),
        Mark(Side.RIGHT, screenW - 14f, screenH / 2, vertical = true, on = on == Side.RIGHT),
    )
    fun widthMarks(screenW: Float, screenH: Float, left: Boolean, snapped: Float): List<Mark<WidthStop>> {
        val at = { w: Float -> if (left) w else screenW - w }
        return listOf(
            Mark(WidthStop.COLUMN, at(COLUMN), screenH / 2, vertical = true, on = snapped == COLUMN),
            Mark(WidthStop.HALF_SCREEN, screenW / 2, screenH / 2, vertical = true, on = snapped == (screenW / 2).roundToInt().toFloat()),
        )
    }
    fun heightMarks(col: Rect, under: Boolean, on: Height): List<Mark<Height>> {
        val h = col.y + col.h
        val ys = if (under) listOf(Height.COLUMN to 24f, Height.HALF to col.y + col.h / 2, Height.COMPACT to h - 150f, Height.BAR to h - 36f)
        else listOf(Height.COLUMN to 24f, Height.BAR to h - 36f)
        val cx = col.x + col.w / 2
        return ys.map { (k, y) -> Mark(k, cx, y, vertical = false, on = k == on) }
    }

    /** La calamita: 1 col dito sul segno, 0 da 240 in là. */
    fun pull(m: Mark<*>, px: Float, py: Float): Float = max(0f, 1f - hypot(m.x - px, m.y - py) / 240f)
    private fun pullShown(m: Mark<*>, px: Float, py: Float) = if (m.on) max(pull(m, px, py), 0.6f) else pull(m, px, py)
    /** La lunghezza del segno: 48 a riposo, fino a 136. */
    fun length(m: Mark<*>, px: Float, py: Float): Float = 48f + pullShown(m, px, py) * 88f
    fun alpha(m: Mark<*>, px: Float, py: Float): Float = 0.45f + pullShown(m, px, py) * 0.55f
    /** Lo scivolamento verso il dito lungo il bordo del segno. */
    fun shift(m: Mark<*>, px: Float, py: Float): Pair<Float, Float> {
        val p = pullShown(m, px, py)
        return if (m.vertical) 0f to (py - m.y) * p * 0.3f else (px - m.x) * p * 0.3f to 0f
    }
}
