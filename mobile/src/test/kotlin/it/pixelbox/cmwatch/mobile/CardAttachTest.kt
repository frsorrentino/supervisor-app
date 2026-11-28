package it.pixelbox.cmwatch.mobile

import android.net.Uri
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CardAttach
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.TiAspettaPage
import it.pixelbox.cmwatch.mobile.ui.WaitCardBody
import it.pixelbox.cmwatch.rules.TiAspetta
import kotlinx.serialization.json.jsonObject
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * app-allegati-carta-27-11 (Franz 27/11 20:47): la carta di prova della fixture delle attese del relay col campo del «Non va» aperto,
 * il + del campo delle sessioni dentro il campo, a sinistra, e uno screenshot nell'anteprima sopra, con la sua ×. L'immagine è quella
 * della demo web, come in SceltaScreenTest.
 */
class CardAttachTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val list = ContractJson.decodeWaits(
        ContractJson.json.parseToJsonElement(File("../contract/attese-rd-sample.json").readText()).jsonObject.getValue("attese").toString(),
    ).items
    private val prova = list.first { w -> w.kind == "prova" }
    private val shot = Uri.fromFile(File("../web/public/demo-files/checkout-before.png").absoluteFile)

    // I tasti della carta di prova sono «Va» e «Non va»: il secondo chiede il testo, e il suo campo è già aperto con l'allegato.
    @Test fun nonVaConUnAllegato() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            TiAspettaPage(TiAspetta.pageOf(prova, list), prova.at + 600, wide = false, onBack = {}, onJump = {}, onOther = {}) {
                WaitCardBody(prova, attach = CardAttach(files = true) { null }, writingAt = 1, filesAt = listOf(shot)) { _, _ -> }
            }
        }
    }
}
