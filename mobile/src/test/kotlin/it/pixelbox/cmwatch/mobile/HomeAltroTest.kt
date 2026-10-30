package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SystemStatus
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.HomeOrderScreen
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.rules.Summary
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** La riga Sistema chiusa in «Altro» (Franz 30/10 16:03): il titolo su una riga, il riepilogo a destra va a capo. */
class HomeAltroTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val st = ContractJson.decodeState(File("../contract/state-2-idle.json").readText()).let { it.copy(sessions = emptyList(), approvals = emptyList()) }
    private val system = SystemStatus(at = st.ts, cores = 8, load1 = 2.0, loadPerCore = 0.2, memTotalMb = 16000, memAvailableMb = 4600, level = "green", loadLevel = "green", memLevel = "green")

    private fun shot() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SummaryList(Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts, usage = { Text("") }, system = system, kpiState = st)
        }
    }

    /** «Riordina la home» (app-riordino-sezioni-30-10): due voci fisse, le sezioni con maniglia e frecce, un solo tasto pieno. */
    @Test fun reorderScreen() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { HomeOrderScreen(listOf("esiti", "sessions"), {}, {}) }
    }

    @Test fun reorderScreen360LargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), fontScale = 1.3f))
        paparazzi.snapshot { CmPhoneTheme(still = true) { HomeOrderScreen(null, {}, {}) } }
    }

    @Test fun altroSystemClosed() = shot()

    /** Utilizzo con due account (state-2-idle): il nome intero, mai tagliato in un'altra parola (master 30/10 16:42). */
    @Test fun altroUsageTwoAccounts() = shot()

    @Test fun altroUsageTwoAccounts360LargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), fontScale = 1.3f))
        shot()
    }

    @Test fun altroSystemClosed360LargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), fontScale = 1.3f))
        shot()
    }
}
