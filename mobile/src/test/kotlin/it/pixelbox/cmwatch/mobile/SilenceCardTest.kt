package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.Cm
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.VoceItem
import it.pixelbox.cmwatch.mobile.ui.WaitCardBody
import it.pixelbox.cmwatch.rules.PerTe
import kotlinx.serialization.json.jsonObject
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * app-silenzio-assenso-23-11 (variante A, Franz 23/11 18:52): la carta di prova col tasto «Dai per risolto se tace» accanto agli
 * altri e quella di ok che pubblica, senza (silence_ok false, contratto 1.160), la carta con la scadenza messa (la riga blu con la data e «Annulla»), e la riga di Per te con «si chiude da
 * sola». A 360 dp col testo normale e grande, sulla fixture delle attese del relay. Registrati in UTC come in CI.
 */
abstract class SilenceCardShots(scale: Float) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (800 * 2.75).toInt(), fontScale = scale), theme = "android:Theme.Material.NoActionBar")
    private val list = ContractJson.decodeWaits(
        ContractJson.json.parseToJsonElement(File("../contract/attese-rd-sample.json").readText()).jsonObject.getValue("attese").toString(),
    ).items
    private fun item(kind: String) = list.first { it.kind == kind }
    private val ops = listOf("attese", "silence")
    private val now = 1795436100L
    private val until = now + 3 * 86_400

    @Test fun keys() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Column(Modifier.fillMaxSize().background(Cm.bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WaitCardBody(item("prova").copy(silenceOk = true), ops, now, { null }) { _, _ -> }
                // «pubblica 1.4 sul Play Store»: il registro dice silence_ok false (contratto 1.160), niente tasto.
                WaitCardBody(item("ok").copy(silenceOk = false), ops, now, { null }) { _, _ -> }
            }
        }
    }

    @Test fun set() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Column(Modifier.fillMaxSize().background(Cm.bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WaitCardBody(item("prova").copy(silenceUntil = until, silenceOk = true), ops, now, { null }) { _, _ -> }
                VoceItem(PerTe.voceOf(item("prova").copy(silenceUntil = until)), now, {}, {})
                VoceItem(PerTe.voceOf(item("ok").copy(silenceUntil = until)), now, {}, {})
            }
        }
    }
}

class SilenceCardTest : SilenceCardShots(1f)
class SilenceCardLargeTextTest : SilenceCardShots(1.3f)
