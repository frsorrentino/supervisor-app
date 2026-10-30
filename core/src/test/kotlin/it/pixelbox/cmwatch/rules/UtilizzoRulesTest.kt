package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.KpiWaitDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilizzoRulesTest {
    @Test fun elapsedIsThePartOfTheWindowGone() {
        assertEquals(60f, UtilizzoRules.elapsedPct(3000, 5000, 1000)!!, 0.01f)
        assertNull(UtilizzoRules.elapsedPct(1000, 5000, 1000))
        assertNull(UtilizzoRules.elapsedPct(null, 5000, 1000))
    }

    @Test fun weekDeltaIsAgainstTheEvenPace() {
        val half = UtilizzoRules.W7_S / 2
        assertEquals(-9, UtilizzoRules.weekDelta(41, 1000 + half, 1000))
        assertEquals(10, UtilizzoRules.weekDelta(60, 1000 + half, 1000))
        assertNull(UtilizzoRules.weekDelta(null, 1000 + half, 1000))
    }

    @Test fun peakHourWrapsAndIgnoresAnEmptyDay() {
        assertEquals(0, UtilizzoRules.peakHour(listOf(1 to 0, 2 to 3, 1 to 1), 23))
        assertNull(UtilizzoRules.peakHour(listOf(0 to 0), 8))
    }

    @Test fun waitsTakeTheLastMedianAndSumTheClosed() {
        assertEquals(UtilizzoRules.Waits(4, 12), UtilizzoRules.waits(listOf(KpiWaitDay("a", 9.0, 5), KpiWaitDay("b", 4.2, 7), KpiWaitDay("c", null, 0))))
        assertNull(UtilizzoRules.waits(emptyList()))
    }
}
