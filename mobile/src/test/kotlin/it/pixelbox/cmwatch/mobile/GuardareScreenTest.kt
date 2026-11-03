package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.Guardare
import it.pixelbox.cmwatch.transport.FakeTransport
import org.junit.Rule
import org.junit.Test

/**
 * Contratto 1.106, «Da guardare» sui dati della fixture (app-da-guardare-02-11, mockup della master delle 08:16): la voce in home
 * con video, mockup e documento; la pagina coi lavori e la versione precedente con la nota; il risultato aperto; la riga della chat.
 * Senza file (Paparazzi non ha il PC) le miniature restano tessere col segno del tipo.
 */
class GuardareScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val page = ContractJson.decodeResults(FakeTransport({ java.io.File("../contract/$it.json").readText() }).sampleText("results"))
    private val v3 = page.groups.single().items.first()
    private val mock = ResultItem("m", "master", "Riga Esito, quattro forme", "mockup", "text/html", "/home/dev/p/esito.html", 24000, v3.at - 3600)
    private val doc = ResultItem("d", "video-supervisor", "Effetti del video di Google", "document", "text/markdown", "/home/dev/v/effetti.md", 8000, v3.at - 7200)
    private val two = page.copy(groups = page.groups + ResultGroup("Riga Esito, quattro forme", "personal/master", "master", "mockup", mock.at, 1, true, listOf(mock)))
    private fun shot(dp: Int = 412, scale: Float = 1f, locale: String = "it", content: @androidx.compose.runtime.Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = locale, screenWidth = (dp * 2.75).toInt(), fontScale = scale))
        paparazzi.snapshot { CmPhoneTheme(still = true) { content() } }
    }
    @Test fun guardare_home412() = shot { Column(Modifier.background(Cm.bg).padding(12.dp)) { GuardareBody(listOf(v3, mock, doc), GuardareActions()) {} } }
    @Test fun guardare_home320_grande() = shot(320, 1.3f) { Column(Modifier.background(Cm.bg).padding(12.dp)) { GuardareBody(listOf(v3, mock, doc), GuardareActions()) {} } }
    @Test fun guardare_pagina412() = shot { GuardareScreen(two, false, null, GuardareActions()) {} }
    @Test fun guardare_sessione412() = shot { GuardareScreen(two, false, "orbit-docs", GuardareActions()) {} }
    @Test fun guardare_vuota() = shot { GuardareScreen(ResultsPage(), false, null, GuardareActions()) {} }
    @Test fun guardare_aperto412() = shot { ResultViewer(doc.copy(version = 2, versions = 2, changed = "aggiunto l'effetto zoom", notes = listOf(ResultNote(doc.at + 60, "Manca la fonte", null))), GuardareActions()) {} }
    // Secondo giro della notte (app-bug-notte-03-11): larghezze strette col testo grande, inglese, titolo lungo.
    private val long = doc.copy(id = "l", title = "Rendiconto trimestrale del cliente con le pagine dei costi e la pagella")
    @Test fun guardare_pagina320_grande() = shot(320, 1.3f) { GuardareScreen(two, false, null, GuardareActions()) {} }
    @Test fun guardare_aperto320_grande() = shot(320, 1.3f) { ResultViewer(long.copy(version = 2, versions = 3, changed = "aggiunte le pagine dei costi e tolto il riepilogo vecchio"), GuardareActions()) {} }
    @Test fun guardare_home_en() = shot(locale = "en") { Column(Modifier.background(Cm.bg).padding(12.dp)) { GuardareBody(listOf(v3, mock, long), GuardareActions()) {} } }
    @Test fun guardare_vuota_en() = shot(locale = "en") { GuardareScreen(ResultsPage(), false, null, GuardareActions()) {} }
    @Test fun guardare_riga_chat320_grande() = shot(320, 1.3f) { Column(Modifier.background(Cm.bg).padding(12.dp)) { ResultsChatRow(Guardare.ofSession(page, "orbit-docs")) {} } }
    // Terzo giro (app-bug-notte-03-11): il video che non arriva dal PC dice perché e offre Riprova, invece della rotella per sempre.
    @Test fun guardare_non_arrivato412() = shot { ResultViewer(v3, GuardareActions(file = { _, _, _, ready -> ready(null) })) {} }
    // Telefono in orizzontale (840 × 412 dp) e tablet in verticale (800 dp).
    private fun wide(w: Int, h: Int, content: @androidx.compose.runtime.Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (w * 2.75).toInt(), screenHeight = (h * 2.75).toInt()))
        paparazzi.snapshot { CmPhoneTheme(still = true) { content() } }
    }
    @Test fun guardare_orizzontale_home() = wide(840, 412) { Column(Modifier.background(Cm.bg).padding(12.dp)) { GuardareBody(listOf(v3, mock, doc), GuardareActions()) {} } }
    @Test fun guardare_orizzontale_aperto() = wide(840, 412) { ResultViewer(doc.copy(version = 2, versions = 2, changed = "aggiunto l'effetto zoom"), GuardareActions()) {} }
    @Test fun guardare_tablet_pagina() = wide(800, 1280) { GuardareScreen(two, false, null, GuardareActions()) {} }
    @Test fun guardare_riga_chat() = shot { Column(Modifier.background(Cm.bg).padding(12.dp)) { ResultsChatRow(Guardare.ofSession(page, "orbit-docs")) {} } }
}
