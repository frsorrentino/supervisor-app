package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.WeekOrigins
import it.pixelbox.cmwatch.contract.WeekPoints
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.TabletQuotaPanel
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.Week7
import org.junit.Rule
import org.junit.Test

/**
 * La settimana ora per ora (2A e 2B, 25/10, contratto 1.86) nel pannello di Utilizzo e limiti, a 320, 360 e 412 dp: nessun
 * testo esce da card e pagina. Due casi: i campioni solo da stasera (come dal vivo il 26/10: una voce all'ora 113) e una
 * settimana intera dal rinnovo che arriva al 100% prima del rinnovo.
 */
class WeekPanelTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val start = 1_789_005_600L
    private val reset = start + Week7.WEEK_S

    private fun ring(points: WeekPoints, w7: Int, origins: WeekOrigins?, nowH: Int) = PhoneOverview.Ring(
        "personal", true, null, w7, start + nowH * 3600L + 5 * 3600, null, weekResetAt = reset, weekPoints = points, weekOrigins = origins,
    )

    private val tonight = ring(WeekPoints(start, listOf(listOf(113, 68))), 68, WeekOrigins(39.8, 4.5, 23.8), 114)
    private val full = ring(
        WeekPoints(start, listOf(listOf(0, 2), listOf(10, 8), listOf(30, 18), listOf(50, 24), listOf(72, 30), listOf(80, 40), listOf(96, 52), listOf(110, 65))),
        65, WeekOrigins(34.2, 19.1, 11.7), 114,
    )

    // Un pannello per scatto, come in RhythmPanelTest: lo schermo del test non scorre.
    private fun at(dp: Int, r: PhoneOverview.Ring) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 2800))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.fillMaxSize().background(it.pixelbox.cmwatch.mobile.ui.V3.Bg).padding(16.dp)) { TabletQuotaPanel(r, start + 114 * 3600L) }
            }
        }
    }

    @Test fun weekTonight320() = at(320, tonight)
    @Test fun weekTonight360() = at(360, tonight)
    @Test fun weekTonight412() = at(412, tonight)
    @Test fun weekFull320() = at(320, full)
    @Test fun weekFull360() = at(360, full)
    @Test fun weekFull412() = at(412, full)
}
