package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.AppShell
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.Genie
import it.pixelbox.cmwatch.mobile.ui.LocalGenieAt
import it.pixelbox.cmwatch.rules.StartRoute.Tab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Il Genie dei menu (app-genie-menu-09-11): il menu ≡ e il menu delle sessioni fermati a metà apertura. Con la goccia appena uscita dal
 * tasto, a metà del gonfiaggio (il pannello vuoto, le voci non ancora arrivate) e con le voci che arrivano sfocate. Golden da CI.
 */
class GenieMenuTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())

    private fun appMenu(at: Float) = paparazzi.snapshot { CmPhoneTheme(still = true) { CompositionLocalProvider(LocalGenieAt provides at) {
        AppShell(Tab.OVERVIEW, demo = false, {}, {}, onSearch = {}, host = "penguin", updated = "aggiornato ora", openCount = 3, menuStartOpen = true) { Text("contenuto") }
    } } }
    private fun sessionMenu(at: Float) = paparazzi.snapshot { CmPhoneTheme(still = true) { CompositionLocalProvider(LocalGenieAt provides at) {
        AppShell(Tab.OVERVIEW, demo = false, {}, {}, sessions = st.sessions, current = "atlas-shop", now = st.ts, sessionMenuStartOpen = true) { Text("contenuto") }
    } } }

    @Test fun appMenuDrop() = appMenu(.2f)
    @Test fun appMenuSwelling() = appMenu(.5f)
    @Test fun appMenuArriving() = appMenu(.8f)
    @Test fun sessionMenuSwelling() = sessionMenu(.5f)
    @Test fun sessionMenuArriving() = sessionMenu(.8f)

    // La forma: la goccia sotto il tasto, tonda e piccola; a metà corsa più grande ma non ancora il pannello; alla fine il pannello.
    @Test fun shapeGoesFromDropToPanel() {
        val w = 1000f; val h = 1500f; val cx = 900f; val drop = 120f; val r = 38f
        val start = Genie.rect(0f, w, h, cx, drop, r)
        assertTrue("la goccia nasce sotto il tasto (${start})", start.left < cx && start.right > cx && start.top == 0f && start.width < drop)
        assertEquals("la goccia è tonda", start.width / 2, start.topLeftCornerRadius.x, .01f)
        val mid = Genie.rect(.6f, w, h, cx, drop, r)
        assertTrue("a metà più grande della goccia e più piccola del pannello ($mid)", mid.width > drop && mid.width < w && mid.height < h)
        val end = Genie.rect(1f, w, h, cx, drop, r)
        assertEquals(0f, end.left, .01f); assertEquals(w, end.right, .01f); assertEquals(h, end.bottom, .01f); assertEquals(r, end.topLeftCornerRadius.x, .01f)
        assertEquals("le voci non ci sono mentre la goccia si gonfia", 0f, Genie.content(.5f), 0f)
        assertEquals(1f, Genie.content(1f), 0f)
    }
}
