package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartRouteTest {
    @Test fun freshLaunchOpensOverview() = assertEquals(StartRoute.Tab.OVERVIEW, StartRoute.tab(restored = null))
    @Test fun rotationKeepsTheTab() = assertEquals(StartRoute.Tab.LAVORI, StartRoute.tab(restored = StartRoute.Tab.LAVORI))
    @Test fun freshLaunchClosesAnyOpenSheet() = assertNull(StartRoute.openSheet(freshLaunch = true, restored = "kb"))
    @Test fun rotationKeepsTheOpenSheet() = assertEquals("kb", StartRoute.openSheet(freshLaunch = false, restored = "kb"))

    @Test fun shortTripAwayKeepsTheSheet() = assertFalse(StartRoute.resetOnReturn(awayMs = StartRoute.AWAY_RESET_MS - 1))
    @Test fun longAbsenceReopensOnOverview() = assertTrue(StartRoute.resetOnReturn(awayMs = StartRoute.AWAY_RESET_MS))
    // Design 03/10: il riepilogo unico prende il posto di Master e Sessioni; resta una pagina sola dal menu: Lavori (22/10, prima il Registro).
    @Test fun onlyTheSummaryAndLavori() = assertEquals(listOf(StartRoute.Tab.OVERVIEW, StartRoute.Tab.LAVORI), StartRoute.Tab.entries.toList())
    // Franz, 03/10 16:30: la master non è una pagina come le altre; aprirla porta la home sulla sua chat.
    @Test fun theMasterOpensAtHome() {
        assertTrue(StartRoute.masterAtHome("master"))
        assertFalse(StartRoute.masterAtHome("atlas-shop"))
        assertFalse(StartRoute.masterAtHome(null))
    }
}
