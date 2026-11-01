package it.pixelbox.cmwatch.mobile

import android.graphics.Bitmap
import android.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.share.ImageShrink
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** La stessa tabella sui pixel veri: rosso in alto a sinistra di una foto larga 2 e alta 1. */
class FotoRuotataBitmapTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private fun photo() = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888).apply { setPixel(0, 0, Color.RED); setPixel(1, 0, Color.BLUE) }

    @Test fun foto_ruotata_seiMetteIlRossoInAlto() {
        val b = ImageShrink.turned(photo(), 6)
        assertEquals(1, b.width); assertEquals(2, b.height)
        assertEquals(Color.RED, b.getPixel(0, 0)); assertEquals(Color.BLUE, b.getPixel(0, 1))
    }

    @Test fun foto_ruotata_ottoMetteIlRossoInBasso() {
        val b = ImageShrink.turned(photo(), 8)
        assertEquals(1, b.width); assertEquals(2, b.height)
        assertEquals(Color.BLUE, b.getPixel(0, 0)); assertEquals(Color.RED, b.getPixel(0, 1))
    }

    @Test fun foto_ruotata_dueSpecchiaEUnoNonToccaNiente() {
        val m = ImageShrink.turned(photo(), 2)
        assertEquals(Color.BLUE, m.getPixel(0, 0)); assertEquals(Color.RED, m.getPixel(1, 0))
        val p = photo(); assertEquals(true, ImageShrink.turned(p, 1) === p)
    }
}
