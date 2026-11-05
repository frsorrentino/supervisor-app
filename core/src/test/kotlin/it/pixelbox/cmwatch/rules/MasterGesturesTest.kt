package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// I quattro casi di app-master-gesti-senza-conflitti-04-11, come gesture.test.ts.
class MasterGesturesTest {
    @Test fun aDragStartsPast6dp() {
        assertFalse(MasterGestures.isDrag(4f, 4f))
        assertTrue(MasterGestures.isDrag(5f, 4f))
    }

    @Test fun aStillTapActs() = assertTrue(MasterGestures.Taps().tap("bar-left", 0))

    @Test fun aDoubleTapIsOneGesture() {
        val t = MasterGestures.Taps()
        assertTrue(t.tap("bar-left", 0))
        assertFalse(t.tap("bar-left", 250))
        // Dopo il doppio, un tocco nuovo agisce; anche oltre i 300 ms, o su un altro posto.
        assertTrue(t.tap("bar-left", 400))
        assertTrue(t.tap("bar-right", 500))
        assertTrue(t.tap("bar-right", 900))
    }

    @Test fun aDragCancelsThePendingTap() {
        val t = MasterGestures.Taps()
        assertTrue(t.tap("bar-left", 0))
        t.dragged()
        assertTrue(t.tap("bar-left", 100))
    }

    // La corsa breve (app-master-corsa-breve-05-11): conta la direzione, non la metà della corsa.
    @Test fun shortRunGoesToTheNextStopThatWay() {
        val stops = listOf(0f, 300f, 700f, 1000f)
        assertEquals(1, MasterGestures.step(stops, 1, 10f))
        assertEquals(1, MasterGestures.step(stops, 1, -23f))
        assertEquals(2, MasterGestures.step(stops, 1, 24f))
        assertEquals(0, MasterGestures.step(stops, 1, -30f))
        // Un tratto lungo arriva al posto più vicino, mai meno di uno.
        assertEquals(3, MasterGestures.step(stops, 1, 650f))
        // Ai bordi si resta.
        assertEquals(3, MasterGestures.step(stops, 3, 80f))
    }

    @Test fun flickCountsAndProjects() {
        val stops = listOf(0f, 300f, 700f, 1000f)
        // Fermo quasi subito ma con un colpo: va nella direzione del colpo.
        assertEquals(2, MasterGestures.step(stops, 1, 5f, 600f))
        assertEquals(0, MasterGestures.step(stops, 1, 30f, -800f))
        // Un colpo forte proietta più lontano.
        assertEquals(3, MasterGestures.step(stops, 1, 40f, 1500f))
        assertEquals(1, MasterGestures.step(stops, 1, 5f, 400f))
    }
}
