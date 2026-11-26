package it.pixelbox.cmwatch.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Voce 1 (app-animazioni-fedeli-26-11): i tempi delle parole che scorrono, scelta b della master del 26/11. */
class WordStreamTest {
    @Test fun seventyMsUpToTwentyWords() {
        assertEquals(0.070, WordStream(20).gapS, 1e-9)
        assertEquals(0.070, WordStream(5).gapS, 1e-9)
    }

    @Test fun longRepliesComeOutIn1400Ms() {
        assertEquals(1.4 / 200, WordStream(200).gapS, 1e-9)
        assertEquals(1.4, WordStream(200).gapS * 200, 1e-9)
    }

    @Test fun springStartsAtZeroAndSettlesAtOne() {
        assertEquals(0.0, WordStream.x(0.0), 1e-9)
        assertEquals(1.0, WordStream.x(2.0), 1e-3)
        assertTrue("340 26 overshoots", (1..100).any { WordStream.x(it / 100.0) > 1.0 })
    }

    @Test fun gateLetsThroughOnlyRepliesArrivedAfterOpening() {
        val g = StreamGate()
        assertFalse("before priming nothing streams", g.claim("a"))
        g.prime(listOf("b"))
        assertFalse("already there at opening", g.claim("b"))
        assertTrue(g.claim("c"))
        assertFalse("once only", g.claim("c"))
    }

    @Test fun countsWords() = assertEquals(3, WordStream.count(" Sì: run  verde\n"))
}
