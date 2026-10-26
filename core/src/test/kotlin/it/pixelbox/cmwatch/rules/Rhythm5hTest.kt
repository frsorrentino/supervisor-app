package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.QuotaHistory.Sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Gli stessi casi di rhythm5h.test.ts. La finestra del mockup 1A/1B: 08:40-13:40, ora 11:20, quota al 62%.
class Rhythm5hTest {
    private val reset = 50_000L
    private val start = reset - QuotaHistory.WINDOW_S
    private val now = start + 160 * 60

    @Test fun elapsedAndAheadAgainstTheWindow() {
        val v = Rhythm5h.of(emptyList(), 62, reset, now)
        assertEquals(53, v.elapsed)
        assertEquals(9, v.ahead)
        assertEquals(-3, Rhythm5h.of(emptyList(), 50, reset, now).ahead)
        assertEquals(100, Rhythm5h.of(emptyList(), 10, reset, reset + 60).elapsed)
    }

    // Il ritmo dell'ultima ora: 42% un'ora fa, 62% adesso = 20 punti l'ora; il 100% fra 114 minuti, prima del reset.
    @Test fun lastHourRateGivesTheTimeOf100() {
        val samples = listOf(Sample(start, 0), Sample(now - 7200, 30), Sample(now - 3600, 42), Sample(now, 62))
        val v = Rhythm5h.of(samples, 62, reset, now)
        assertEquals(now + 114 * 60, v.fullAt)
        assertEquals(26, v.idleMin)
        assertEquals(100, v.projected)
    }

    @Test fun slowPaceProjectsAtTheReset() {
        val samples = listOf(Sample(now - 3600, 50), Sample(now, 56))
        val v = Rhythm5h.of(samples, 56, reset, now)
        assertNull(v.fullAt)
        assertNull(v.idleMin)
        // 6 punti l'ora per 140 minuti: 56 + 14 = 70.
        assertEquals(70, v.projected)
    }

    @Test fun noRateWithoutTenMinutesOrWithoutGrowth() {
        assertNull(Rhythm5h.of(listOf(Sample(now - 300, 50), Sample(now, 60)), 60, reset, now).projected)
        assertNull(Rhythm5h.of(listOf(Sample(now - 3600, 60), Sample(now, 60)), 60, reset, now).projected)
        assertNull(Rhythm5h.of(listOf(Sample(now, 60)), 60, reset, now).projected)
        // Campioni di una finestra passata non contano.
        assertNull(Rhythm5h.of(listOf(Sample(start - 600, 10), Sample(now, 60)), 60, reset, now).projected)
    }

    @Test fun alreadyFullHasNoTimeOf100() {
        val v = Rhythm5h.of(listOf(Sample(now - 3600, 80), Sample(now, 100)), 100, reset, now)
        assertNull(v.fullAt)
        assertEquals(100, v.projected)
    }
}
