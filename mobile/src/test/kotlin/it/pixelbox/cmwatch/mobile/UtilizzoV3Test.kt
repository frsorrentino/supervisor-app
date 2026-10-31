package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.TimelinePage
import it.pixelbox.cmwatch.contract.TimelineSession
import it.pixelbox.cmwatch.contract.TimelineEvent
import it.pixelbox.cmwatch.contract.UsageToday
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.UtilizzoV3
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.QuotaHistory
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** Il blocco Utilizzo (scelta D, 30/10 20:11): chiuso e con ogni riga aperta, a 412 dp e a 360 dp con caratteri grandi e un nome lungo. */
class UtilizzoV3Test {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val zone = ZoneId.of("Europe/Rome")
    private val base = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
    private val now = base.ts
    private val account = base.quota.keys.first()
    private val st = base.copy(quota = mapOf(account to base.quota.getValue(account).copy(h5 = 34, w7 = 41, stale = false, resetH5 = now + 3 * 3600, resetW7 = now + 3 * 86400)))
        .copy(usageToday = listOf(UsageToday(session = "internationalyoungclub-com", account = account, share = 0.46), UsageToday(session = "supervisor-app", account = account, share = 0.27)))
    private val pts = List(5) { QuotaHistory.Sample(now - 7200 + it * 1800L, 10 + it * 6) }
    private val ring = PhoneOverview.Ring(account, true, 34, 41, now + 3 * 3600, QuotaHistory.Pace(pts, 62, now + 3 * 3600), now + 3 * 86400)
    private val page = TimelinePage(now - 86400, listOf(TimelineSession("supervisor-app", true, "supervisor-app", List(12) { TimelineEvent(now - it * 1500L, "prompt", "x") })))

    private fun shot(open: String?) = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Column(Modifier.fillMaxWidth().background(it.pixelbox.cmwatch.mobile.ui.V3.S1)) { UtilizzoV3(listOf(ring), st, page, null, now, {}, startOpen = open, zone = zone) }
        }
    }

    @Test fun closed() = shot(null)
    @Test fun fiveHoursOpen() = shot("h5:$account")
    @Test fun weekOpen() = shot("w7:$account")
    @Test fun sessionsOpen() = shot("sessions")
    @Test fun todayOpen() = shot("today")

    // Il nome lungo a 360 dp col carattere grande: sotto l'etichetta, mai spezzato a metà parola.
    @Test fun longNames360LargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), fontScale = 1.3f))
        shot("sessions")
    }

    @Test fun longName360LargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), fontScale = 1.3f))
        shot("h5:$account")
    }
}
