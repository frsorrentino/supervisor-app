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
 * La home di partenza (app-home-voci-chiuse-10-11, Franz 10/11 21:28: «Di base, le voci della home devono apparire chiuse, sia da app che
 * da PWA»): senza scelte salvate le categorie e le sezioni sono chiuse col loro riassunto, aperte restano le sessioni. Golden da CI, a
 * 360 dp.
 */
class HomeStartsClosedTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = 3200), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-3-stale.json").readText())
    private val monitors = ContractJson.decodeMonitors(File("../core/src/test/resources/monitors-mockup.json").readText()).items

    @Test fun phone() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SummaryList(
                Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts,
                usage = { Text("") }, kpiState = st, monitors = monitors,
            )
        }
    }
}
