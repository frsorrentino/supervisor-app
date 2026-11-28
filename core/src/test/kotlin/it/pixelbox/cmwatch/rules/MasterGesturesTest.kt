package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Le molle d'arrivo della master (app-altezza-senza-rimbalzo-06-11), anche in gesture.test.ts; il riconoscitore delle maniglie e la corsa
// breve sono usciti con app-posizioni-sistema-27-11 gruppo 6, insieme alle posizioni di prima.
class MasterGesturesTest {
    // app-altezza-senza-rimbalzo-06-11: ogni rilascio arriva e si ferma, mai oltre l'arrivo, con qualunque velocità del dito.
    @Test fun `ogni rilascio arriva senza passare oltre, a qualunque velocità del dito`() {
        for (from in listOf(-400f, -40f, -3f, 3f, 40f, 400f)) for (v in listOf(-6000f, -3000f, -800f, 0f, 800f, 3000f, 6000f)) {
            var t = 0f
            while (t < 1.5f) { assertTrue("da $from a $v al tempo $t", MasterGestures.landAt(from, v, t) * from >= 0f); t += 0.001f }
        }
    }

    @Test fun `la molla tiene la velocità del dito quando può`() {
        // Da 40 px a 3000 px/s verso l'arrivo: prima passava di circa 36 px; ora parte a 3000 px/s con una molla più rigida.
        val l = MasterGestures.land(40f, -3000f)
        assertEquals(-3000f, l.velocity, 0f)
        assertEquals(75f * 75f, l.stiffness, 1f)
        // Lento o in senso contrario resta la molla di sempre.
        assertEquals(MasterGestures.SIDE_SPRING_STIFFNESS, MasterGestures.land(400f, -2000f).stiffness, 0f)
        assertEquals(MasterGestures.SIDE_SPRING_STIFFNESS, MasterGestures.land(-100f, -1500f).stiffness, 0f)
        // Troppo veloce per la molla più rigida: la velocità si limita.
        assertEquals(-100f * 3f, MasterGestures.land(3f, -6000f).velocity, 0.01f)
    }
}
