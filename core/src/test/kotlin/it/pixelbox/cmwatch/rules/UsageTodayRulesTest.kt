package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.UsageToday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Gli stessi casi di usageToday.test.ts (contratto 1.84, B5). */
class UsageTodayRulesTest {
    @Test fun fromTheBiggestWithTheSmallTogether() {
        val r = UsageTodayRules.rows(listOf(
            UsageToday("master", "personale", 140, 0.14), UsageToday("supervisor-app", "personale", 340, 0.34),
            UsageToday("legnaia", "personale", 20, 0.02), UsageToday("curriculum", "personale", 10, 0.01),
        ))
        assertEquals(UsageTodayRules.Rows(listOf(UsageTodayRules.Row("supervisor-app", 34), UsageTodayRules.Row("master", 14)), UsageTodayRules.Others(2, 3)), r)
        assertNull(UsageTodayRules.rows(emptyList()))
        assertNull(UsageTodayRules.rows(null))
    }

    @Test fun aSingleSmallOneKeepsItsName() {
        val r = UsageTodayRules.rows(listOf(UsageToday("master", "p", 9, 0.9), UsageToday("console", "p", 1, 0.029)))
        assertEquals(UsageTodayRules.Rows(listOf(UsageTodayRules.Row("master", 90), UsageTodayRules.Row("console", 3)), null), r)
    }
}
