package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Voce 5 del terzo giro (gelatina b): l'elastico, il collo calcolato e le molle che si fermano sulla meta. */
class JellySimTest {
    @Test fun rubberStaysInsideAndNeverPasses18Outside() {
        assertEquals(50f, JellySim.rubber(50f, 12f, 100f, 18f), 0f)
        val far = JellySim.rubber(500f, 12f, 100f, 18f)
        assertTrue(far > 100f && far < 118f)
    }

    @Test fun neckOnlyBetweenNearCircles() {
        assertNotNull(JellySim.neck(Offset(0f, 30f), 12f, Offset(0f, -8f), 18f, 46f))
        assertNull(JellySim.neck(Offset(0f, 30f), 12f, Offset(0f, -200f), 18f, 46f))
    }

    @Test fun springsSettleOnTheirTargets() {
        val s = JellySim()
        s.X.t = 100f; s.DY.t = -38f; s.DS.t = 1f
        repeat(200) { s.step(1f / 60f) }
        assertEquals(100f, s.X.x, 0.1f); assertEquals(100f, s.DX.x, 0.5f); assertEquals(-38f, s.DY.x, 0.1f)
        assertTrue(s.still)
    }
}
