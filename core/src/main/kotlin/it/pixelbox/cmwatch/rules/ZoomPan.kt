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
        // Sotto 1x si arriva solo dal pizzico sull'anteprima (handOff): da lì si può crescere senza scatto.
        val s = (v.scale * zoom).coerceIn(minOf(1f, v.scale), max)
        val k = s / v.scale
        // Il centro dello strato è il centro della vista: un punto a distanza d dal centro, con lo spostamento v.x, sta a
        // centro + v.x + d·scale. Tenere fermo il punto sotto le dita dà x' = c - (c - x)·k, con c misurato dal centro.
        val dx = cx - viewW / 2f
        val dy = cy - viewH / 2f
        val nx = dx - (dx - v.x) * k + panX
        val ny = dy - (dy - v.y) * k + panY
        // Fuori dai bordi (dopo handOff) l'immagine può solo rientrare: niente scatto al primo tocco; il resto lo fa [settle].
        return View(s, toward(v.x, nx, limit(contentW, viewW, s)), toward(v.y, ny, limit(contentH, viewH, s)))
    }

    private fun toward(prev: Float, next: Float, lim: Float): Float = when {
        prev > lim -> next.coerceIn(-lim, prev)
        prev < -lim -> next.coerceIn(prev, lim)
        else -> next.coerceIn(-lim, lim)
    }

    /** Lo stato valido più vicino: almeno 1x (attorno al centro della vista) e dentro i bordi; a fine gesto ci si arriva animando. */
    fun settle(v: View, contentW: Float, contentH: Float, viewW: Float, viewH: Float, max: Float = MAX): View {
        val s = v.scale.coerceIn(1f, max)
        val k = s / v.scale
        return clamp(View(s, v.x * k, v.y * k), contentW, contentH, viewW, viewH)
    }

    /** Un rettangolo in pixel dello schermo. */
    data class Box(val left: Float, val top: Float, val width: Float, val height: Float)

    /** Dove il visore mostra l'immagine intera, in pixel dello schermo; `boxLeft, boxTop`: l'angolo del visore sullo schermo. */
    fun shown(v: View, contentW: Float, contentH: Float, boxLeft: Float, boxTop: Float, viewW: Float, viewH: Float): Box =
        Box(boxLeft + viewW / 2f + v.x - contentW * v.scale / 2f, boxTop + viewH / 2f + v.y - contentH * v.scale / 2f, contentW * v.scale, contentH * v.scale)

    /** L'immagine intera dietro un'anteprima che la ritaglia al centro (ContentScale.Crop): sporge dal riquadro sul lato lungo. */
    fun cropped(thumb: Box, imageW: Float, imageH: Float): Box {
        val k = maxOf(thumb.width / imageW, thumb.height / imageH)
        return Box(thumb.left + (thumb.width - imageW * k) / 2f, thumb.top + (thumb.height - imageH * k) / 2f, imageW * k, imageH * k)
    }

    /**
     * Il pizzico sull'anteprima (app-zoom-testo-02-11, Franz 03/11 00:41: «il punto centrale dello zoom non corrisponde a quello
     * del pinch»): ogni punto `r` dell'anteprima, ingrandita di `scale` attorno a `origin` (dove le dita sono cominciate),
     * si sposta con le dita: il punto che era sotto le dita ci resta. Tutto in pixel dello schermo.
     */
    fun pinched(r: Box, originX: Float, originY: Float, scale: Float, fingerX: Float, fingerY: Float): Box =
        Box(fingerX + (r.left - originX) * scale, fingerY + (r.top - originY) * scale, r.width * scale, r.height * scale)

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
     * Il passaggio dal pizzico sull'anteprima al visore (Franz 03/11 00:41: «al rilascio l'immagine si posiziona diversamente
     * spostandosi un po'»): il visore riprende l'immagine esattamente dove e quanto grande la si vedeva (`image`, in pixel
     * dello schermo), anche sotto 1x o oltre i bordi; ci pensa [settle] a fine del gesto dopo, animando.
     */
    fun handOff(image: Box, boxLeft: Float, boxTop: Float, contentW: Float, contentH: Float, viewW: Float, viewH: Float): View {
        if (contentW <= 0f) return View()
        return View(image.width / contentW, image.left + image.width / 2f - boxLeft - viewW / 2f, image.top + image.height / 2f - boxTop - viewH / 2f)
    }

    /** La misura dell'immagine adattata alla vista (ContentScale.Fit). */
    fun fit(imageW: Float, imageH: Float, viewW: Float, viewH: Float): Pair<Float, Float> {
        if (imageW <= 0f || imageH <= 0f) return viewW to viewH
        val r = minOf(viewW / imageW, viewH / imageH)
        return imageW * r to imageH * r
    }
}
