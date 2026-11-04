package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

// Il riposizionamento a onda (app-effetti-movimenti-04-11, punto 6).
class HomeReflowTest {
    // 18 ms a gradino di 48 dp verso il basso, un terzo verso destra, al massimo 160.
    @Test fun waveDelaysGrowDownAndRight() {
        assertEquals(0, HomeReflow.waveDelay(0f, 0f))
        assertEquals(36, HomeReflow.waveDelay(100f, 0f))
        assertEquals(54, HomeReflow.waveDelay(100f, 144f))
        assertEquals(160, HomeReflow.waveDelay(2000f, 0f))
    }
}
