package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.transport.FakeTransport
import org.junit.Rule
import org.junit.Test

/** Contratto 1.100, la pagina sui dati della fixture: il consenso e la sezione vuota che dice perché; a raccolta finita il costo del primo giro. */
class PrimaRaccoltaScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val page = ContractJson.decodePrimaRaccolta(FakeTransport({ java.io.File("../contract/$it.json").readText() }).sampleText("prima_raccolta"))
    private fun shot(p: PrimaRaccoltaPage, dp: Int = 412, scale: Float = 1f) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), fontScale = scale))
        paparazzi.snapshot { CmPhoneTheme(still = true) { PrimaRaccoltaScreen(p, {}, { _, _ -> }, {}, {}) } }
    }
    @Test fun prima_raccolta_consenso412() = shot(page)
    @Test fun prima_raccolta_finita320_grande() = shot(page.copy(status = "done", consent = false, askText = null), 320, 1.3f)
}
