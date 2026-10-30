package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class WeekArticleTest {
    @Test fun elidedOnlyBeforeAVowelSound() {
        listOf(1, 8, 11, 80, 89).forEach { assertEquals("$it", true, PhoneOverview.elidesArticle(it)) }
        listOf(0, 2, 7, 10, 12, 18, 21, 79, 90, 100).forEach { assertEquals("$it", false, PhoneOverview.elidesArticle(it)) }
    }
}
