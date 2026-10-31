package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.Kpi7
import it.pixelbox.cmwatch.mobile.ui.TabletQuotaPanel
import it.pixelbox.cmwatch.mobile.ui.UtilizzoV3
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.rules.PhoneOverview
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** quota-senza-cinque-ore: il dato five_hour sparisce per account. Tabellone, Utilizzo e pannello, un account solo e due con uno senza dato, notte e giorno B. */
class NoFiveHourTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val zone = ZoneId.of("Europe/Rome")
    private val base = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
    private val now = base.ts
    private val personal = base.quota.getValue("personal").copy(h5 = null, resetH5 = null, w7 = 41, resetW7 = now + 5 * 86400, stale = false)
    private val work = base.quota.getValue("work").copy(h5 = 34, resetH5 = now + 3 * 3600, w7 = 60, resetW7 = now + 2 * 86400, stale = false)
    private val one = base.copy(quota = mapOf("personal" to personal))
    private val two = base.copy(quota = mapOf("personal" to personal, "work" to work))
    private val head = Kpi.Head(2, 300, 3, 7, 0)

    private fun rings(st: State) = PhoneOverview.build(st, emptyList(), emptyList(), now, zone, stale = false).rings

    @After fun night() { V3.day = false }

    private fun shot(day: Boolean = false, content: @Composable () -> Unit) = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = day) { Column(Modifier.fillMaxWidth().background(V3.Bg).padding(12.dp)) { content() } }
    }

    private fun bigFont() = paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), fontScale = 1.3f))

    @Test fun boardOneAccount() = shot { Kpi7(head, one, now, {}) }
    @Test fun boardTwoAccountsOneWithout() = shot { Kpi7(head, two, now, {}) }
    @Test fun usageClosed() = shot { UtilizzoV3(rings(one), one, null, null, now, {}, zone = zone) }
    @Test fun usageWeekOpen() = shot { UtilizzoV3(rings(one), one, null, null, now, {}, startOpen = "w7:personal", zone = zone) }
    @Test fun usageTwoAccountsOneWithout() = shot { UtilizzoV3(rings(two), two, null, null, now, {}, zone = zone) }
    @Test fun panel() = shot { TabletQuotaPanel(rings(one).first(), now) }

    @Test fun boardOneAccountDay() = shot(day = true) { Kpi7(head, one, now, {}) }
    @Test fun boardTwoAccountsOneWithoutDay() = shot(day = true) { Kpi7(head, two, now, {}) }
    @Test fun usageWeekOpenDay() = shot(day = true) { UtilizzoV3(rings(one), one, null, null, now, {}, startOpen = "w7:personal", zone = zone) }
    @Test fun panelDay() = shot(day = true) { TabletQuotaPanel(rings(one).first(), now) }

    @Test fun boardTwoAccounts360LargeFont() { bigFont(); shot { Kpi7(head, two, now, {}) } }
}
