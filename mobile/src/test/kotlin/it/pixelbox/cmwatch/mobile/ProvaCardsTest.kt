package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.ProvaCard
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.ProvaLinks
import org.junit.Rule
import org.junit.Test

/**
 * Le prove citate nelle chat come schede compatte (app-prova-scheda-in-chat-11-11): da fare coi tasti Non va e Ok, con l'esito al
 * posto dei tasti, in arrivo; un numero che non esiste non ha scheda. Golden da CI.
 */
class ProvaCardsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 1800), theme = "android:Theme.Material.NoActionBar")
    private fun prova(n: Long, state: String, text: String? = null) = ProvaOne(
        n, "app-prova-link-diretta-10-11", "Link diretto in Verifica", "Il link apre la prova in Verifica", "win:supervisor-app", "supervisor-app", "0.2.2101",
        "telefono, chat della master", "Tocca «prova #550» in un messaggio della master e guarda dove arrivi", "Verifica con la scheda aperta", state, text, 1_789_214_700L,
    )
    private fun cards() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Column(Modifier.background(V3.Bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProvaCard(569, ProvaLinks.Read.Found(prova(569, "todo")), {}, { true }, { _, _ -> true })
                ProvaCard(558, ProvaLinks.Read.Found(prova(558, "ok")), {}, { true }, { _, _ -> true })
                ProvaCard(551, ProvaLinks.Read.Found(prova(551, "ko", "si apre la pagina di dettaglio, non Verifica")), {}, { true }, { _, _ -> true })
                ProvaCard(570, null, {}, { true }, { _, _ -> true })
                ProvaCard(999, ProvaLinks.Read.Refused("prova 999: non esiste"), {}, { true }, { _, _ -> true })
            }
        }
    }

    @Test fun compactCards() = cards()
    @Test fun compactCards320Font13() { paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), fontScale = 1.3f, screenHeight = 2400)); cards() }
}
