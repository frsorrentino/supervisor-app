package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ProveGroup
import it.pixelbox.cmwatch.contract.ProveItem
import it.pixelbox.cmwatch.contract.ProvePage
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.DaProvareScreen
import it.pixelbox.cmwatch.rules.BancoProva
import org.junit.Rule
import org.junit.Test

/**
 * Il link di una prova da fare apre Verifica sulla sua scheda (app-prova-link-diretta-10-11, Franz 10/11 22:57: «sarebbe preferibile
 * che si apra direttamente la scheda in verifica»): la quinta prova del gruppo, che starebbe fra le «altre N», è aperta coi suoi tasti
 * e la lista è intera. Golden da CI; con `still` le animazioni sono spente e l'evidenza, che si spegne da sola, non c'è.
 */
class ProvaDirectTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 2000), theme = "android:Theme.Material.NoActionBar")
    private val page = ProvePage(n = 5, builds = 1, groups = listOf(ProveGroup("0.2.2092", n = 5, prove = listOf(
        ProveItem(546, "app-home-voci-chiuse-10-11", title = "Home con le voci chiuse", dove = "telefono, home", fai = "Apri l'app da zero", atteso = "Categorie e sezioni chiuse, le sessioni aperte"),
        ProveItem(547, "app-monitoraggi-testata-10-11", title = "Testata dei monitoraggi", dove = "telefono, home", fai = "Guarda la testata dei monitoraggi", atteso = "Il numero conta solo ciò che aspetta te"),
        ProveItem(548, "app-monitoraggi-aggiorna-10-11", title = "Aggiorna adesso", dove = "telefono, monitoraggi", fai = "Tocca «Aggiorna adesso»", atteso = "La scheda gira e poi mostra l'ora nuova"),
        ProveItem(549, "app-genie-menu-09-11", title = "Menu che si aprono dal tasto", dove = "telefono, home", fai = "Apri e chiudi il menu dell'app", atteso = "Il menu esce dal tasto e ci rientra"),
        ProveItem(550, "app-prova-link-diretta-10-11", title = "Il link apre la prova in Verifica", dove = "telefono, chat della master", fai = "Tocca «prova #550» in un messaggio", atteso = "Si apre Verifica con questa scheda aperta, Ok e Non va"),
    ))))

    @Test fun theLinkedProvaIsOpenInVerify() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { DaProvareScreen(page, { BancoProva.Build.INSTALLED }, onBack = {}, onOk = { true }, onKo = { _, _, _ -> true }, onScreen = {}, focus = "550") }
    }
}
