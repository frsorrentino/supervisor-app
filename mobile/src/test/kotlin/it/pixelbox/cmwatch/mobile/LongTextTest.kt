package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalSpeakText
import it.pixelbox.cmwatch.mobile.ui.LocalSpeaking
import it.pixelbox.cmwatch.mobile.ui.LongText
import it.pixelbox.cmwatch.mobile.ui.V3
import org.junit.Rule
import org.junit.Test

/** Il tasto ▶ accanto ai testi oltre due righe delle pagine nuove (app-tasto-play-03-11): il corto no, il lungo sì, ■ mentre legge. */
class LongTextTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val short = "Approva il rilascio 2.4 di atlas-shop."
    private val long = "Il rilascio 2.4 di atlas-shop è pronto: la suite di checkout è verde, il pagamento ha ancora due errori nel rimborso. " +
        "Se approvi parte in produzione stasera; se aspetti, il cliente lo vede domani mattina con la correzione dei rimborsi."
    private val other = "Seguito di ledger-api: il controllo delle 18 ha trovato tre file cambiati senza test, e la pipeline del deploy non è partita per il token scaduto."

    @Test fun playOnlyBesideLongTexts() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            CompositionLocalProvider(LocalSpeakText provides {}, LocalSpeaking provides other) {
                Column(Modifier.fillMaxWidth().background(V3.Bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LongText(short, style = V3.Body)
                    LongText(long, style = V3.Body)
                    LongText(other, style = V3.Small)
                }
            }
        }
    }
}
