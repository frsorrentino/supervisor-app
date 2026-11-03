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

    @Test fun anOldReadingOfAFinishedWindowIsNotShown() {
        assertNull(UtilizzoRules.fiveHourLive(40, 900, stale = true, now = 1000))
        assertNull(UtilizzoRules.fiveHourLive(40, null, stale = true, now = 1000))
        assertEquals(40, UtilizzoRules.fiveHourLive(40, 2000, stale = true, now = 1000))
        assertEquals(40, UtilizzoRules.fiveHourLive(40, 900, stale = false, now = 1000))
    }

    @Test fun anOldReadingOfAFinishedWindowFallsBackOnTheWeek() {
        val b = UtilizzoRules.bar(40, 25, 900, 5000, 1000, stale = true)!!
        assertEquals(true, b.week)
        assertEquals(25, b.pct)
    }

    @Test fun shownQuotaKeepsTheFreshOnesOrAllOfThem() {
        val fresh = it.pixelbox.cmwatch.contract.QuotaAccount(w7 = 20)
        val old = it.pixelbox.cmwatch.contract.QuotaAccount(w7 = 39, stale = true)
        assertEquals(listOf("a"), UtilizzoRules.shownQuota(mapOf("a" to fresh, "b" to old)).map { it.key })
        assertEquals(listOf("a", "b"), UtilizzoRules.shownQuota(mapOf("a" to old, "b" to old)).map { it.key })
    }

    private fun acc(w7: Int, stale: Boolean = false, reset: Long? = null) = it.pixelbox.cmwatch.contract.QuotaAccount(w7 = w7, stale = stale, resetW7 = reset)

    @Test fun theWeekAlarmStartsAt75() {
        assertEquals(false, UtilizzoRules.weekSlot(mapOf("a" to acc(74)), 0)!!.alarm)
        assertEquals(true, UtilizzoRules.weekSlot(mapOf("a" to acc(75)), 0)!!.alarm)
    }

    @Test fun theWeekAlarmStartsTenPointsAboveThePace() {
        // A metà settimana il ritmo pari è 50.
        val half = UtilizzoRules.W7_S / 2
        assertEquals(false, UtilizzoRules.weekSlot(mapOf("a" to acc(59, reset = half)), 0)!!.alarm)
        assertEquals(true, UtilizzoRules.weekSlot(mapOf("a" to acc(60, reset = half)), 0)!!.alarm)
    }

    @Test fun anOldReadingNeverRingsTheAlarm() {
        assertEquals(false, UtilizzoRules.weekSlot(mapOf("a" to acc(95, stale = true)), 0)!!.alarm)
    }

    @Test fun withTwoAccountsTheMostLoadedAmongTheFreshCounts() {
        val s = UtilizzoRules.weekSlot(mapOf("a" to acc(30), "b" to acc(82), "c" to acc(99, stale = true)), 0)!!
        assertEquals("b", s.account); assertEquals(82, s.pct); assertEquals(true, s.alarm); assertEquals(true, s.many)
        assertNull(UtilizzoRules.weekSlot(emptyMap(), 0))
    }

    // app-utilizzo-una-barra-03-11: la barra fissa delle 5 ore lascia il tabellone; una finestra vicina al limite ci entra da sola in ambra.
    private fun q5(h5: Int, reset: Long = 3600, stale: Boolean = false, w7: Int? = null) = it.pixelbox.cmwatch.contract.QuotaAccount(h5 = h5, resetH5 = reset, stale = stale, w7 = w7)

    @Test fun usageAlarm_leCinqueOreDal75() {
        assertNull(UtilizzoRules.usageAlarm(mapOf("a" to q5(74)), 0))
        assertEquals(UtilizzoRules.UsageAlarm("a", false, 75, 3600L), UtilizzoRules.usageAlarm(mapOf("a" to q5(75)), 0))
    }

    @Test fun usageAlarm_laSettimanaComeLoSlot() {
        assertEquals(UtilizzoRules.UsageAlarm("a", true, 80, null), UtilizzoRules.usageAlarm(mapOf("a" to acc(80)), 0))
        assertNull(UtilizzoRules.usageAlarm(mapOf("a" to acc(40)), 0))
    }

    @Test fun usageAlarm_maiDaUnaLetturaVecchia() {
        assertNull(UtilizzoRules.usageAlarm(mapOf("a" to q5(95, stale = true)), 0))
    }

    @Test fun usageAlarm_laFinestraPiuCaricaVince() {
        assertEquals(UtilizzoRules.UsageAlarm("b", false, 90, 3600L), UtilizzoRules.usageAlarm(mapOf("a" to acc(80), "b" to q5(90)), 0))
        assertEquals(UtilizzoRules.UsageAlarm("a", true, 92, null), UtilizzoRules.usageAlarm(mapOf("a" to q5(76, w7 = 92)), 0))
        assertEquals("a pari le 5 ore, che finiscono prima", false, UtilizzoRules.usageAlarm(mapOf("a" to q5(80, w7 = 80)), 0)!!.week)
    }
}
