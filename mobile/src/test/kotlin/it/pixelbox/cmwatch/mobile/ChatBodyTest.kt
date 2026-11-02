package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.*
import org.junit.Rule
import org.junit.Test

/** Una risposta in chat con «Da te», «Non provato» e il nome di un compito, solo dati demo; poi un messaggio senza etichette. */
class ChatBodyTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private fun shot(text: String, big: Boolean = false, type: String? = null) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 1400, fontScale = if (big) 1.3f else 1f))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                CompositionLocalProvider(LocalTaskLinks provides TaskLinks(mapOf("demo-cover" to "Immagine di copertina")) {}) {
                    Column(Modifier.fillMaxWidth().background(V3.Bg).padding(12.dp)) { ChatBody(text, type) }
                }
            }
        }
    }

    private val full = "demo-cover è pronto per te; `git log demo-cover` resta com'è.\n\n**Da te**\n1. Manda l'immagine al blog.\n2. Fai il login nella sandbox.\n\n**Non provato:** il checkout su un telefono vero, con il testo intero leggibile su più righe, senza pieghe.\n\nEsito: due cose aspettano te."

    private val long = "Fatto.\n\nEsito: fase B uscita; fase C passata a supervisor; 42 prove su `seguiti-2026-11-02`, e questa riga va avanti per tre righe di testo sul telefono."
    @Test fun chat_esito_allineata_treRighe() = shot(long, type = "shipped")
    @Test fun chat_esito_allineata_treRigheGrande() = shot(long, big = true, type = "shipped")
    @Test fun chat_esito_allineata_aspetta() = shot("Fatto.\n\nEsito: serve la tua scelta sul brano.", type = "waiting")
    @Test fun chat_esito_problema() = shot(full, type = "problem")
    @Test fun chat_esito_pubblicato() = shot(full, type = "shipped")
    @Test fun chat_etichette_riquadroENonProvato() = shot(full)
    @Test fun chat_etichette_testoGrande() = shot(full, big = true)
    @Test fun chat_etichette_senzaEtichette() = shot("Da te ci vuole poco, e Non provato resta a metà frase.\n\n- una voce\n- un'altra\n\nEsito: niente cambia.")
}
