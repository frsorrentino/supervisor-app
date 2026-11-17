package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.PagellaRow
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.PagellaBody
import it.pixelbox.cmwatch.mobile.ui.RendicontiBody
import it.pixelbox.cmwatch.mobile.ui.V3
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * app-rendiconti-2-02-11 (contratto 1.103): Rendiconti col mese, i clienti e il rendiconto aperto di uno, col tasto «Chiedi la bozza»;
 * la Pagella con le sole righe da 10 casi in su (quella di Sonnet su 6 casi non c'è). Dati della fixture del relay, notte e giorno.
 */
class ContiUiTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 2000), theme = "android:Theme.Material.NoActionBar")
    @After fun night() { V3.day = false }

    private val zone = ZoneId.of("Europe/Rome")
    private val base = it.pixelbox.cmwatch.transport.FakeTransport(load = { n -> File("../contract/$n.json").readText() }).sampleText("conti").let(ContractJson::decodeConti)
    // Un secondo cliente e una seconda riga piena, per vedere la lista e una percentuale alta in ambra.
    private val page = base.copy(
        clienti = base.clienti + it.pixelbox.cmwatch.contract.ContiCliente("work/ledger-api", "ledger-api", 3, base.at - 2 * 86400),
        pagella = base.pagella!!.copy(rows = base.pagella!!.rows + PagellaRow("claude-sonnet-5-5", "feature", 11, true, 3, 0.273)),
    )

    private fun shot(day: Boolean) = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = day) {
            Column(Modifier.fillMaxSize().background(V3.S1), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RendicontiBody(page, failed = false, onLoad = { _, _ -> }, onAsk = { _, _ -> null }, zone = zone)
                PagellaBody(page.pagella!!, zone)
            }
        }
    }

    @Test fun contiNight() = shot(false)
    @Test fun contiDay() = shot(true)
}
