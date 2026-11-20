package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.PagellaRow
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.PagellaBody
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.mobile.ui.pagellaOneLine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * app-pagella-sovrapposta-19-11: sul web a 360 px si leggeva «correz14nicasi», il nome della prova sotto il numero dei casi. Sul
 * telefono stretto (320 dp, testo grande) nome e casi non si toccano: i casi vanno sotto, allineati a destra, quando non ci stanno.
 */
class PagellaNarrowTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), screenHeight = 900, fontScale = 1.3f), theme = "android:Theme.Material.NoActionBar")

    private val base = it.pixelbox.cmwatch.transport.FakeTransport(load = { n -> File("../contract/$n.json").readText() }).sampleText("conti").let(ContractJson::decodeConti).pagella!!
    private val pagella = base.copy(rows = base.rows + PagellaRow("claude-sonnet-5-5", "refactor", 11, true, 3, 0.273))

    @Test fun narrow() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { Column(Modifier.fillMaxSize().background(V3.S1)) { PagellaBody(pagella, ZoneId.of("Europe/Rome")) } }
    }

    @Test fun oneLineOnlyWhenBothFitWhole() {
        assertTrue(pagellaOneLine(lead = 150, cases = 120, gap = 10, max = 280))
        assertFalse("un pixel in meno: i casi vanno sotto", pagellaOneLine(lead = 150, cases = 121, gap = 10, max = 280))
        assertFalse("il caso del web a 360 px", pagellaOneLine(lead = 150, cases = 230, gap = 10, max = 332))
    }
}
