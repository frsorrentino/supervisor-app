package it.pixelbox.cmwatch.rules

/**
 * Il visore delle immagini (Franz, 15/10 22:25: «se faccio uno zoom e poi provo a muoverla si muove molto lentamente»): il
 * gesto si legge fuori dallo strato ingrandito, così lo spostamento segue il dito 1:1 a ogni zoom; lo zoom resta attorno
 * alle dita; l'immagine non esce dai bordi; il doppio tocco ingrandisce sul punto toccato o torna a 1x. Coordinate in
 * pixel della vista, con l'immagine adattata (`content`) al centro e lo strato scalato attorno al centro.
 */
object ZoomPan {
    const val MAX = 6f
    const val DOUBLE_TAP = 2.5f

    data class View(val scale: Float = 1f, val x: Float = 0f, val y: Float = 0f)

    /** Quanto lo strato può spostarsi su un asse prima che l'immagine lasci un bordo vuoto. */
    fun limit(content: Float, viewport: Float, scale: Float): Float = ((content * scale - viewport) / 2f).coerceAtLeast(0f)

    fun clamp(v: View, contentW: Float, contentH: Float, viewW: Float, viewH: Float): View {
        val lx = limit(contentW, viewW, v.scale)
        val ly = limit(contentH, viewH, v.scale)
        return v.copy(x = v.x.coerceIn(-lx, lx), y = v.y.coerceIn(-ly, ly))
    }

    /**
     * Un passo del gesto: `zoom` attorno a `cx, cy` (le dita, in pixel della vista), poi lo spostamento delle dita, 1:1.
     * Il punto dell'immagine sotto le dita resta sotto le dita.
     */
    fun step(v: View, zoom: Float, panX: Float, panY: Float, cx: Float, cy: Float, contentW: Float, contentH: Float, viewW: Float, viewH: Float, max: Float = MAX): View {
        val s = (v.scale * zoom).coerceIn(1f, max)
        val k = s / v.scale
        // Il centro dello strato è il centro della vista: un punto a distanza d dal centro, con lo spostamento v.x, sta a
        // centro + v.x + d·scale. Tenere fermo il punto sotto le dita dà x' = c - (c - x)·k, con c misurato dal centro.
        val dx = cx - viewW / 2f
        val dy = cy - viewH / 2f
        val moved = View(s, dx - (dx - v.x) * k + panX, dy - (dy - v.y) * k + panY)
        return clamp(moved, contentW, contentH, viewW, viewH)
    }

    /**
     * La misura che porta l'immagine a tutta larghezza (app-chat-zoom-02-11): un mockup alto, adattato, è una striscia stretta;
     * il pizzico arriva fino al triplo di questa, e mai sotto [MAX].
     */
    fun fillWidth(contentW: Float, viewW: Float): Float = if (contentW <= 0f) 1f else (viewW / contentW).coerceAtLeast(1f)
    fun maxScale(contentW: Float, viewW: Float): Float = maxOf(MAX, 3f * fillWidth(contentW, viewW))

    /** Il doppio tocco: da 1x ingrandisce sul punto toccato, almeno fino a tutta larghezza; altrimenti torna a 1x. */
    fun doubleTap(v: View, tx: Float, ty: Float, contentW: Float, contentH: Float, viewW: Float, viewH: Float): View =
        if (v.scale > 1.01f) View()
        else maxOf(DOUBLE_TAP, fillWidth(contentW, viewW)).let { z -> step(View(), z, 0f, 0f, tx, ty, contentW, contentH, viewW, viewH, maxScale(contentW, viewW)) }

    /**
     * Il passaggio dal pizzico sull'anteprima al visore (app-zoom-continuo-02-11, Franz 02/11 23:31: «deve essere continuo»):
     * il punto dell'immagine che stava sotto le dite resta sotto le dita, e l'immagine resta larga quanto la si vedeva.
     * `fx, fy`: il punto in frazioni dell'immagine; `shownW`: la larghezza a cui la si vedeva; `px, py`: le dita nella vista.
     */
    fun handOff(fx: Float, fy: Float, shownW: Float, px: Float, py: Float, contentW: Float, contentH: Float, viewW: Float, viewH: Float): View {
        if (contentW <= 0f) return View()
        val s = (shownW / contentW).coerceIn(1f, maxScale(contentW, viewW))
        // A 1x il punto sta a centro + (f - ½)·content; a scala s, spostato di (x, y), a centro + x + (f - ½)·content·s.
        val x = px - viewW / 2f - (fx - 0.5f) * contentW * s
        val y = py - viewH / 2f - (fy - 0.5f) * contentH * s
        return clamp(View(s, x, y), contentW, contentH, viewW, viewH)
    }

    /** La misura dell'immagine adattata alla vista (ContentScale.Fit). */
    fun fit(imageW: Float, imageH: Float, viewW: Float, viewH: Float): Pair<Float, Float> {
        if (imageW <= 0f || imageH <= 0f) return viewW to viewH
        val r = minOf(viewW / imageW, viewH / imageH)
        return imageW * r to imageH * r
    }
}
