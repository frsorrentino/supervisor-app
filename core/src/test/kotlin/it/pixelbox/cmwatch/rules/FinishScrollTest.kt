package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinishScrollTest {
    private val ok = FinishScroll.Scene(false, false, 2_000, true, false, false, 800f, 800f, 10_000)

    @Test fun allConditionsMetScrolls() = assertTrue(FinishScroll.allowed(ok))
    @Test fun fingerOnListStops() = assertFalse(FinishScroll.allowed(ok.copy(fingerDown = true)))
    @Test fun movingListStops() = assertFalse(FinishScroll.allowed(ok.copy(listMoving = true)))
    @Test fun recentTouchStops() = assertFalse(FinishScroll.allowed(ok.copy(sinceTouchMs = 1_999)))
    @Test fun offScreenItemStops() = assertFalse(FinishScroll.allowed(ok.copy(wasOnScreen = false)))
    @Test fun writingStops() = assertFalse(FinishScroll.allowed(ok.copy(writing = true)))
    @Test fun openSheetStops() = assertFalse(FinishScroll.allowed(ok.copy(sheetOpen = true)))
    @Test fun moreThanOneScreenStops() = assertFalse(FinishScroll.allowed(ok.copy(distancePx = -801f)))
    @Test fun atMostOneEveryTenSeconds() = assertFalse(FinishScroll.allowed(ok.copy(sinceLastMs = 9_999)))
}
