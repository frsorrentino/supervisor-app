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
}
