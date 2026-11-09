package it.pixelbox.cmwatch.mobile

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import it.pixelbox.cmwatch.mobile.ui.OverlayAtOrigin
import it.pixelbox.cmwatch.rules.ZoomPan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * app-pinch-primo-tocco-09-11 (Franz 09/11 08:19: «il pinch to zoom non è centrato al primo tocco»). La finestra sovrapposta del
 * pizzico nasce all'origine della finestra dell'app (Compose, PopupLayout.createLayoutParams: gravity START|TOP, x e y a 0) e lì
 * misura `layerAt`; poi si sposta dove dice il provider, e uno spostamento della sola finestra non la rimisura. Il punto
 * dell'immagine che era sotto le dita deve restarci a ogni altezza della chat: lo stesso conto di AttachmentThumb.
 */
class ThumbOverlayTest {
    // Il telefono del video di Franz (1080x2404) e un tablet.
    private val windows = listOf(IntSize(1080, 2404), IntSize(2560, 1600))
    private val born = IntOffset.Zero

    /** Dove finisce sullo schermo il punto dell'immagine che, al primo tocco, era fra le dita; e dove sono le dita. */
    private fun landing(windowAt: IntOffset, thumb: ZoomPan.Box, ox: Float, oy: Float, fx: Float, fy: Float): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        val r = ZoomPan.pinched(thumb, ox, oy, 2.5f, fx, fy)
        // L'immagine sta a (r - layerAt) dentro la finestra sovrapposta, che sta a `windowAt`.
        val left = windowAt.x + (r.left - born.x)
        val top = windowAt.y + (r.top - born.y)
        val u = (ox - thumb.left) / thumb.width
        val v = (oy - thumb.top) / thumb.height
        return (left + u * r.width to top + v * r.height) to (fx to fy)
    }

    private fun cases(w: IntSize) = listOf(0.08f, 0.45f, 0.8f).map { at ->
        // Un'anteprima di 720x540 dentro la sua bolla, a varie altezze; le dita partono a destra del centro e si spostano.
        val thumb = ZoomPan.Box(48f, w.height * at, 720f, 540f)
        val anchor = IntRect(32, (thumb.top - 40f).toInt(), 900, (thumb.top + 600f).toInt())
        Triple(anchor, thumb, listOf(thumb.left + 500f, thumb.top + 260f, thumb.left + 380f, thumb.top + 180f))
    }

    @Test fun theFingersKeepTheirPointAtAnyHeight() {
        for (w in windows) for ((anchor, thumb, f) in cases(w)) {
            val at = OverlayAtOrigin.calculatePosition(anchor, w, LayoutDirection.Ltr, w)
            val (point, fingers) = landing(at, thumb, f[0], f[1], f[2], f[3])
            assertEquals("x, finestra $w, anteprima a ${thumb.top}", fingers.first, point.first, 0.5f)
            assertEquals("y, finestra $w, anteprima a ${thumb.top}", fingers.second, point.second, 0.5f)
        }
    }

    // Com'era: la finestra sovrapposta messa sotto la bolla (il provider di serie del Popup) si sposta dopo aver misurato
    // `layerAt`, e a metà chat il punto finisce lontano dalle dita di quanto la bolla dista dall'alto.
    @Test fun underTheBubbleItDrifts() {
        val w = windows.first()
        val (anchor, thumb, f) = cases(w)[1]
        val (point, fingers) = landing(IntOffset(anchor.left, anchor.top), thumb, f[0], f[1], f[2], f[3])
        assertFalse(kotlin.math.abs(point.second - fingers.second) < 0.5f)
        assertEquals(anchor.top.toFloat(), point.second - fingers.second, 0.5f)
    }
}
