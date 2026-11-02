package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Il visore delle immagini (Franz, 15/10 22:25): lo spostamento segue il dito anche ingranditi. */
class ZoomPanTest {
    // Una vista 1000×2000 con un'immagine adattata 1000×600.
    private val w = 1000f; private val h = 2000f; private val cw = 1000f; private val ch = 600f
    private fun step(v: ZoomPan.View, zoom: Float = 1f, px: Float = 0f, py: Float = 0f, cx: Float = 500f, cy: Float = 1000f) =
        ZoomPan.step(v, zoom, px, py, cx, cy, cw, ch, w, h)

    @Test fun atFourTimesThePanFollowsTheFingerOneToOne() {
        val v = step(ZoomPan.View(), zoom = 4f)
        // Prima l'immagine si muoveva di un quarto: 100 px di dito erano 25 px.
        assertEquals(v.x + 100f, step(v, px = 100f).x, 0.01f)
        assertEquals(v.y - 50f, step(v, py = -50f).y, 0.01f)
    }

    @Test fun theZoomStaysAroundTheFingers() {
        // Dita a 200 px dal centro: dopo lo zoom quel punto dell'immagine è ancora sotto le dita.
        val v = step(ZoomPan.View(), zoom = 2f, cx = 700f, cy = 1000f)
        val imageX = (700f - w / 2f - 0f) / 1f   // il punto, a 1x
        assertEquals(700f, w / 2f + v.x + imageX * v.scale, 0.01f)
    }

    @Test fun theImageDoesNotLeaveTheEdges() {
        val v = step(ZoomPan.View(), zoom = 2f)
        // A 2x l'immagine larga 2000 in una vista di 1000: si sposta di 500 al massimo per lato.
        assertEquals(500f, step(v, px = 5000f).x, 0.01f)
        assertEquals(-500f, step(v, px = -5000f).x, 0.01f)
        // In altezza 1200 in 2000: nessuno spostamento verticale.
        assertEquals(0f, step(v, py = 800f).y, 0.01f)
        // A 1x niente spostamento.
        assertEquals(0f, step(ZoomPan.View(), px = 300f).x, 0.01f)
    }

    @Test fun theZoomStaysBetweenOneAndSix() {
        assertEquals(1f, step(ZoomPan.View(), zoom = 0.3f).scale, 0.001f)
        assertEquals(6f, step(ZoomPan.View(), zoom = 20f).scale, 0.001f)
    }

    @Test fun doubleTapZoomsOnThePointAndBack() {
        val z = ZoomPan.doubleTap(ZoomPan.View(), 800f, 1000f, cw, ch, w, h)
        assertEquals(2.5f, z.scale, 0.001f)
        assertEquals(-450f, z.x, 0.01f)   // il punto toccato, a 300 dal centro, va verso il centro
        assertEquals(ZoomPan.View(), ZoomPan.doubleTap(z, 0f, 0f, cw, ch, w, h))
    }

    @Test fun theFittedSize() {
        assertEquals(1000f to 600f, ZoomPan.fit(2000f, 1200f, 1000f, 2000f))
        assertEquals(500f to 2000f, ZoomPan.fit(1000f, 4000f, 1000f, 2000f))
    }

    /**
     * app-chat-zoom-02-11 (Franz 02/11 22:24): un mockup alto 824×6652 su un telefono 1080×2400 sta adattato largo 297 px.
     * Il doppio tocco lo porta a tutta larghezza, e il pizzico va oltre fino al triplo di quella misura.
     */
    @Test fun aTallImageFillsTheWidthOnDoubleTapAndZoomsFurther() {
        val (tw, th) = ZoomPan.fit(824f, 6652f, 1080f, 2400f)
        val z = ZoomPan.doubleTap(ZoomPan.View(), 540f, 300f, tw, th, 1080f, 2400f)
        assertEquals(1080f / tw, z.scale, 0.001f)
        assertEquals(0f, z.x, 0.01f)
        val max = ZoomPan.maxScale(tw, 1080f)
        assertEquals(3f * 1080f / tw, max, 0.001f)
        assertEquals(max, ZoomPan.step(z, 20f, 0f, 0f, 540f, 300f, tw, th, 1080f, 2400f, max).scale, 0.001f)
    }

    @Test fun aWideImageKeepsTheOldLimits() {
        assertEquals(ZoomPan.MAX, ZoomPan.maxScale(cw, w), 0.001f)
    }

    /** Franz 03/11 00:41: il punto fra le dita resta sotto le dita mentre l'anteprima cresce e si sposta. */
    @Test fun thePinchKeepsThePointBetweenTheFingersUnderTheFingers() {
        // Anteprima 600×400 a (100, 1500) sullo schermo, immagine 1200×1200: ritagliata al centro, intera è 600×600 a (100, 1400).
        val thumb = ZoomPan.Box(100f, 1500f, 600f, 400f)
        val img = ZoomPan.cropped(thumb, 1200f, 1200f)
        assertEquals(ZoomPan.Box(100f, 1400f, 600f, 600f), img)
        // Le dita cominciano a (250, 1600): il punto (¼, ⅓) dell'immagine. Ingrandita 3 volte, le dita ora a (400, 900).
        val fx = (250f - img.left) / img.width; val fy = (1600f - img.top) / img.height
        val r = ZoomPan.pinched(img, 250f, 1600f, 3f, 400f, 900f)
        assertEquals(400f, r.left + fx * r.width, 0.01f)
        assertEquals(900f, r.top + fy * r.height, 0.01f)
        assertEquals(1800f, r.width, 0.01f)
        // A scala 1 e dita ferme non si muove niente.
        assertEquals(img, ZoomPan.pinched(img, 250f, 1600f, 1f, 250f, 1600f))
    }

    /** Franz 03/11 00:41: al rilascio il visore riprende esattamente posizione e scala del gesto, anche fuori dai bordi. */
    @Test fun theHandOffShowsTheImageExactlyWhereThePinchLeftIt() {
        // Visore 1000×2000 a (0, 80) sullo schermo (sotto la barra di stato), immagine adattata 1000×600.
        for (seen in listOf(ZoomPan.Box(-350f, 300f, 2000f, 1200f), ZoomPan.Box(120f, 1300f, 700f, 420f), ZoomPan.Box(-900f, -400f, 3000f, 1800f))) {
            val v = ZoomPan.handOff(seen, 0f, 80f, cw, ch, w, h)
            val back = ZoomPan.shown(v, cw, ch, 0f, 80f, w, h)
            assertEquals(seen.left, back.left, 0.01f); assertEquals(seen.top, back.top, 0.01f)
            assertEquals(seen.width, back.width, 0.01f); assertEquals(seen.height, back.height, 0.01f)
        }
    }

    /** Dal pizzico al visore, tutto il percorso: il punto sotto le dita all'inizio è sotto le dita nel visore, alla stessa larghezza. */
    @Test fun fromThePreviewToTheViewerThePointStaysUnderTheFingers() {
        val thumb = ZoomPan.Box(100f, 1500f, 600f, 400f)
        val img = ZoomPan.cropped(thumb, 1200f, 1200f)
        val fx = (250f - img.left) / img.width; val fy = (1600f - img.top) / img.height
        val seen = ZoomPan.pinched(img, 250f, 1600f, 2.5f, 520f, 1100f)
        // Nel visore l'immagine adattata è 1000×1000 (quadrata in 1000×2000).
        val v = ZoomPan.handOff(seen, 0f, 80f, 1000f, 1000f, w, h)
        val r = ZoomPan.shown(v, 1000f, 1000f, 0f, 80f, w, h)
        assertEquals(520f, r.left + fx * r.width, 0.01f)
        assertEquals(1100f, r.top + fy * r.height, 0.01f)
        assertEquals(1500f, r.width, 0.01f)
    }

    /** Dopo il passaggio fuori dai bordi il primo tocco non scatta; a fine gesto [ZoomPan.settle] riporta dentro. */
    @Test fun outOfBoundsOnlyComesBackAndSettles() {
        // Sotto 1x (0.7) e spostata di 300 in basso: un pizzico che cresce non torna di colpo a 1x né al centro.
        val v = ZoomPan.View(0.7f, 0f, 300f)
        val grown = step(v, zoom = 1.1f, cx = 500f, cy = 1300f)
        assertEquals(0.77f, grown.scale, 0.001f)
        assertEquals(300f, grown.y, 0.01f)
        // Uno spostamento verso fuori resta fermo, verso dentro passa.
        assertEquals(300f, step(v, py = 50f).y, 0.01f)
        assertEquals(250f, step(v, py = -50f).y, 0.01f)
        assertEquals(ZoomPan.View(), ZoomPan.settle(v, cw, ch, w, h))
        // Dentro i bordi settle non cambia niente.
        val inside = ZoomPan.View(2f, 100f, 0f)
        assertEquals(inside, ZoomPan.settle(inside, cw, ch, w, h))
    }
}
