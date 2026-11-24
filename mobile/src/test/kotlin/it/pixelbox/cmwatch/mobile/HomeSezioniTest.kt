package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.rules.Summary
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * Il titolo «Sessioni» (app-titolo-sessioni-24-11, Franz 24/11 09:41: «qui sopra sessioni metterei un titolino sessioni come per dalla
 * master, in modo che se uno volesse potrebbe chiuderle, di base solo la sezione sessioni deve essere aperta»): sopra la lista la stessa
 * testata di «Dalla master». Di partenza Sessioni è aperta e le categorie chiuse; chiusa, la testata dice il riassunto accanto. Golden
 * registrati su Linux nella CI, a 360 dp.
 */
class HomeSezioniTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = 3200), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-3-stale.json").readText())

    private fun home(startOpen: Set<String>) = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SummaryList(
                Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts,
                usage = { Text("") }, kpiState = st, startOpen = startOpen,
            )
        }
    }

    @Test fun aperta() = home(setOf("sessions"))
    @Test fun chiusa() = home(emptySet())
}
