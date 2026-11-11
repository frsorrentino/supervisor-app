package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Automatico, Giorno, Notte (app-tema-giorno-notte-11-11). */
class AppThemeTest {
    @Test fun autoFollowsTheSystem() {
        assertEquals(true, AppTheme.day(AppTheme.Choice.AUTO, systemDark = false))
        assertEquals(false, AppTheme.day(AppTheme.Choice.AUTO, systemDark = true))
    }

    @Test fun dayAndNightIgnoreTheSystem() {
        for (dark in listOf(true, false)) {
            assertEquals(true, AppTheme.day(AppTheme.Choice.DAY, dark))
            assertEquals(false, AppTheme.day(AppTheme.Choice.NIGHT, dark))
        }
    }

    @Test fun anUnknownOrMissingKeyIsAuto() {
        assertEquals(AppTheme.Choice.AUTO, AppTheme.fromKey(null))
        assertEquals(AppTheme.Choice.AUTO, AppTheme.fromKey(""))
        assertEquals(AppTheme.Choice.AUTO, AppTheme.fromKey("sepia"))
        assertEquals(AppTheme.Choice.DAY, AppTheme.fromKey("day"))
        assertEquals(AppTheme.Choice.NIGHT, AppTheme.fromKey("night"))
    }
}
