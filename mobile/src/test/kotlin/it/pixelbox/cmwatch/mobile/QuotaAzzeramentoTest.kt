package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.Kpi7
import it.pixelbox.cmwatch.mobile.ui.TabletQuotaPanel
import it.pixelbox.cmwatch.mobile.ui.UsageCell
import it.pixelbox.cmwatch.mobile.ui.UtilizzoV3
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.mobile.ui.resetText
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.UtilizzoRules
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * app-quota-azzeramento-17-11: l'ora in cui si azzera sotto ogni percentuale. Il tabellone in Home, il blocco Utilizzo aperto con due
 * account (uno fresco, uno con la lettura vecchia) e i pannelli di «Utilizzo e limiti» su tablet (5 ore passate, settimana col suo
 * giorno). Notte e giorno. Le ore sono quelle vere della settimana del 17/11: la settimana personale si azzera gio 19/11 alle 3.
 */
class QuotaAzzeramentoTest {
    // I golden con l'anello e i grafici della quota escono in due versioni a seconda della macchina della CI, diverse solo
    // nell'antialiasing (0,03-0,05% dei pixel, 27/11): la tolleranza dello 0,1% le accetta tutte e due.
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 2000), theme = "android:Theme.Material.NoActionBar", maxPercentDifference = 0.1)
    @After fun night() { V3.day = false }

    private val zone = ZoneId.of("Europe/Rome")
    private val week = 1795053600L
    private val now = week - 40 * 3600
    private val base = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
    private val st = base.copy(ts = now, publishedAt = null, quota = mapOf(
        "personal" to base.quota.getValue("personal").copy(h5 = 9, w7 = 91, stale = false, resetH5 = now + 2 * 3600 + 30 * 60, resetW7 = week),
        "work" to base.quota.getValue("work").copy(h5 = 1, w7 = 12, stale = true, resetH5 = now + 3600, resetW7 = week + 5 * 86400),
    ))
    private fun ring(a: String) = PhoneOverview.build(st, emptyList(), emptyMap(), now, zone, stale = false).rings.first { it.account == a }

    private fun board(day: Boolean) = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = day) {
            Column(Modifier.fillMaxSize().background(V3.Bg).padding(12.dp)) {
                val q = st.quota.getValue("personal")
                Kpi7(Kpi.Head(1, 48 * 60, 0, 3, null), st, now, {}, usage = UsageCell(91, UtilizzoRules.elapsedPct(week, UtilizzoRules.W7_S, now)?.toFloat(), V3.Red, "della settimana", false,
                    resetText(UtilizzoRules.resetLine(q.resetW7, now, q.stale)!!, zone)) {})
            }
        }
    }

    private fun opened(day: Boolean) = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = day) {
            // Tutte e due le righe dell'account vecchio restano: senza la fresca accanto si vedrebbe solo la personale.
            Column(Modifier.fillMaxWidth().background(V3.S1)) { UtilizzoV3(listOf(ring("personal"), ring("work").copy(stale = false)), st, null, null, now, {}, zone = zone) }
        }
    }

    private fun tablet(day: Boolean) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (840 * 2.75).toInt(), screenHeight = 3000))
        paparazzi.snapshot {
            CmPhoneTheme(still = true, day = day) {
                Column(Modifier.fillMaxSize().background(V3.Bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Le 5 ore personali già passate: «azzerata» invece di un'ora che non vale.
                    TabletQuotaPanel(ring("personal").copy(readResetH5 = now - 60, resetAt = null, h5 = 9), now)
                    Column(Modifier.width(420.dp)) { TabletQuotaPanel(ring("work").copy(stale = false), now, dataAgeS = 2 * 3600 + 5 * 60L) }
                }
            }
        }
    }

    @Test fun boardNight() = board(false)
    @Test fun boardDay() = board(true)
    @Test fun openNight() = opened(false)
    @Test fun openDay() = opened(true)
    @Test fun tabletNight() = tablet(false)
    @Test fun tabletDay() = tablet(true)
}
