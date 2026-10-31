package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** quota-senza-cinque-ore: Anthropic toglie il limite a tappe, per account. Conta il dato assente, mai il nome del piano. */
class QuotaNoFiveHourTest {
    private val now = 1_000_000L
    private val resetW7 = now + 5 * 86400 // passati 2 giorni su 7: ritmo pari al 29 %

    @Test fun withoutH5TheBarIsTheWeekWithDayAndPace() {
        assertEquals(UtilizzoRules.Bar(true, 41, resetW7, UtilizzoRules.W7_S, 12, 3), UtilizzoRules.bar(null, 41, null, resetW7, now))
    }

    @Test fun withH5TheBarStaysFiveHoursEvenAtZero() {
        assertEquals(UtilizzoRules.Bar(false, 0, now + 3600, UtilizzoRules.H5_S, null, null), UtilizzoRules.bar(0, 41, now + 3600, resetW7, now))
    }

    @Test fun twoAccountsTheDataIsMissingOnOneOnly() {
        assertFalse(UtilizzoRules.bar(34, 50, now + 3600, resetW7, now)!!.week)
        assertTrue(UtilizzoRules.bar(null, 50, null, resetW7, now)!!.week)
    }

    @Test fun withoutRenewalTheWeekStaysWithoutPace() {
        assertEquals(UtilizzoRules.Bar(true, 41, null, UtilizzoRules.W7_S, null, null), UtilizzoRules.bar(null, 41, null, null, now))
    }

    @Test fun withoutBothNoBarNeverAnInventedZero() {
        assertNull(UtilizzoRules.bar(null, null, null, null, now))
    }

    @Test fun theDayStaysBetweenOneAndSeven() {
        assertEquals(1, UtilizzoRules.weekDay(now + 7 * 86400, now))
        assertEquals(7, UtilizzoRules.weekDay(now + 1, now))
        assertNull(UtilizzoRules.weekDay(now, now))
    }
}
