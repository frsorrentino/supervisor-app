package it.pixelbox.cmwatch.rules

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
}
