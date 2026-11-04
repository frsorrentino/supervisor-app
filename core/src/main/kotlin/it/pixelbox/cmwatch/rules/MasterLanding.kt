package it.pixelbox.cmwatch.rules

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * La master del tablet si sposta e si misura coi gesti come nella PWA larga (`web/src/lib/masterLanding.ts`, Franz 03/11
 * 22:06-23:01, app-master-trascina-03-11; sul tablet app-tablet-come-pwa-03-11): la testata trascinata cambia solo il lato;
 * il bordo verso le sessioni allarga e stringe fra due posti fissi (colonna e metà schermo); il bordo superiore cambia
 * l'altezza a scatti. Mentre si trascina compaiono i segni dove si può lasciare: quello di arrivo è
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

    /** La colonna stretta della master, in px come nella PWA. */
    const val COLUMN = 420f
    /** Il posto di adesso della larghezza tiene finché il dito non passa il punto di mezzo di tanto. */
    const val WIDTH_MARGIN = 28f
    /** Intorno al centro il lato resta quello di adesso. */
    const val SIDE_MARGIN = 48f
    /** La banda d'altezza di adesso tiene finché il dito non la supera di tanto. */
    const val HEIGHT_MARGIN = 28f

    fun sideAt(screenW: Float, x: Float): Side = if (x < screenW / 2) Side.LEFT else Side.RIGHT
    fun sideDragged(screenW: Float, x: Float, now: Side): Side = if (abs(x - screenW / 2) < SIDE_MARGIN) now else sideAt(screenW, x)

    /**
     * I posti fissi della larghezza (Franz 04/11 10:29-10:30, app-trascina-rifinitura-04-11: «meglio posizioni fisse», «così
     * la calamita ha più senso e può essere più forte»): la colonna e la metà schermo, se è più larga della colonna.
     */
    fun widthStops(screenW: Float): List<Float> {
        val half = (screenW / 2).roundToInt().toFloat()
        return if (half > COLUMN) listOf(COLUMN, half) else listOf(COLUMN)
    }
    /** La calamita della larghezza: sempre il posto fisso più vicino, mai una misura nel mezzo; `now` tiene entro WIDTH_MARGIN. */
    fun widthAt(px: Float, screenW: Float, now: Float? = null): Float {
        val stops = widthStops(screenW)
        val near = stops.minBy { abs(px - it) }
        return if (now != null && now in stops && abs(px - now) - abs(px - near) < WIDTH_MARGIN) now else near
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
    fun widthMarks(screenW: Float, screenH: Float, left: Boolean, on: Float): List<Mark<WidthStop>> = widthStops(screenW).map { s ->
        if (s == COLUMN) Mark(WidthStop.COLUMN, if (left) COLUMN else screenW - COLUMN, screenH / 2, vertical = true, on = on == s)
        else Mark(WidthStop.HALF_SCREEN, screenW / 2, screenH / 2, vertical = true, on = on == s)
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

    /** La riga in basso e la compatta, prima di misurarle; sotto la home colonna, maniglia e master hanno 24 fra l'una e l'altra. */
    const val BAR_H = 64f
    const val COMPACT_H = 180f
    const val STACK_GAP = 24f

    /**
     * Il posto della master nell'area della pagina, per la sagoma dell'anteprima (scelta A di Franz, 04/11 17:21,
     * app-trascina-rifinitura-04-11; sul tablet app-tablet-scelta-a-04-11; come `placeRect` di masterLanding.ts): la colonna sul
     * lato `left` larga `w`; sotto la home a metà o compatta, in fondo a quella colonna; la riga in basso a tutta larghezza.
     */
    fun placeRect(area: Rect, left: Boolean, h: Height, w: Float, bar: Float = BAR_H, compact: Float = COMPACT_H): Rect {
        val bottom = area.y + area.h
        if (h == Height.BAR) return Rect(area.x, bottom - bar, area.w, bar)
        val x = if (left) area.x else area.x + area.w - w
        if (h == Height.COLUMN) return Rect(x, area.y, w, area.h)
        val hh = if (h == Height.HALF) (area.h - STACK_GAP) / 2 else compact
        return Rect(x, bottom - hh, w, hh)
    }
    /** Lo stesso posto dall'altro lato dell'area. */
    fun mirrorRect(area: Rect, r: Rect): Rect = r.copy(x = 2 * area.x + area.w - r.x - r.w)
    /** La colonna della master larga `w`, ferma sul suo lato: il bordo verso le sessioni si sposta. */
    fun widthRect(col: Rect, left: Boolean, w: Float): Rect = col.copy(x = if (left) col.x else col.x + col.w - w, w = w)

    /** La linea a metà schermo del gesto del lato: passata quando il dito è dall'altra parte rispetto a dove stava la master. */
    fun crossed(screenW: Float, x: Float, fromLeft: Boolean): Boolean = (x < screenW / 2) != fromLeft

    /** Quanto deve restare sotto il dito un bersaglio nuovo prima di prenderlo (app-trascina-rifinitura-04-11). */
    const val DWELL_MS = 140L

    /**
     * La sosta (Franz 04/11 08:20: «dalla riga in basso c'è sfarfallio»; `steady` di masterLanding.ts): il bersaglio di adesso
     * tiene; uno nuovo vince solo se resta sotto il dito per `dwell` ms di fila. `feed` dice il bersaglio da mostrare e, se uno
     * nuovo aspetta, fra quanti ms riguardare anche senza altri movimenti del dito.
     */
    class Steady<K>(start: K, private val dwell: Long = DWELL_MS) {
        var current: K = start
            private set
        private var cand: K? = null
        private var since = 0L
        fun feed(key: K, now: Long): Pair<K, Long?> {
            if (key == current) { cand = null; return current to null }
            if (cand != key) { cand = key; since = now }
            val left = dwell - (now - since)
            if (left <= 0) { current = key; cand = null; return current to null }
            return current to left
        }
    }
}
