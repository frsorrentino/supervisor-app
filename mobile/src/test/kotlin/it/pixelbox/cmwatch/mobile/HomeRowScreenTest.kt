package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.HomeRowBar
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.HomeRow
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * La Home in riga (app-home-riga-c1-13-11, variante C1): la riga con chi ti aspetta fuori schermo e una novità, con una novità sola,
 * e vuota con la sola maniglia; sul tablet largo con cosa in breve, sul tablet stretto e sul telefono col nome e da quanto (sul
 * telefono le novità contate in una voce). Giorno e notte. Golden da CI.
 */
class HomeRowScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 900), theme = "android:Theme.Material.NoActionBar")
    private val now = 1_789_214_700L
    private val waits = listOf(
        HomeRow.Voice(HomeRow.Kind.WAIT, "storefront", "Fai il login nella sandbox dei pagamenti", now - 420, "wait:storefront"),
        HomeRow.Voice(HomeRow.Kind.WAIT, "blog", "ok alla pubblicazione del post", now - 240, "wait:blog"),
    )
    private val news = listOf(
        HomeRow.Voice(HomeRow.Kind.NEWS, "admin-panel", "232 test verdi", now - 60, "finished:admin-panel@1"),
        HomeRow.Voice(HomeRow.Kind.NEWS, "docs", "pagine pubblicate", now - 30, "finished:docs@2"),
    )

    @After fun night() { V3.day = false }

    @Composable private fun rows(tight: Boolean) {
        Column(Modifier.fillMaxSize().background(V3.S2).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HomeRowBar(waits + news.take(1), now, tight, {}, {})
            HomeRowBar(news, now, tight, {}, {})
            HomeRowBar(emptyList(), now, tight, {}, {})
        }
    }
    private fun wide(w: Int) = paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = w, screenHeight = 600, density = com.android.resources.Density.XHIGH, orientation = com.android.resources.ScreenOrientation.LANDSCAPE))

    @Test fun phone() = paparazzi.snapshot { CmPhoneTheme(still = true) { rows(tight = true) } }
    @Test fun phoneDay() = paparazzi.snapshot { CmPhoneTheme(still = true, day = true) { rows(tight = true) } }
    // La plancia larga 1280 dp: le voci dicono anche cosa.
    @Test fun tabletWide() { wide(2560); paparazzi.snapshot { CmPhoneTheme(still = true) { rows(tight = false) } } }
    @Test fun tabletWideDay() { wide(2560); paparazzi.snapshot { CmPhoneTheme(still = true, day = true) { rows(tight = false) } } }
    // Accanto alla master resta meno di 900 dp: il nome e da quanto.
    @Test fun tabletNarrow() { wide(1640); paparazzi.snapshot { CmPhoneTheme(still = true) { rows(tight = false) } } }
}
