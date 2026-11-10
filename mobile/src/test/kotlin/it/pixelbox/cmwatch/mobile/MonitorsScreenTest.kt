package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalPageBg
import it.pixelbox.cmwatch.mobile.ui.MonitorPage
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.mobile.ui.TiAspettaColumn
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.Summary
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * I Monitoraggi coi dati dei mockup nella forma del contratto 1.132 (app-monitoraggi-09-11): la sezione in fondo a «Dalla master», la riga chiusa della categoria
 * che li nomina in ambra solo se uno è da guardare, e la scheda di ogni monitoraggio, che insieme coprono le sette forme del
 * catalogo: a 360 dp di notte e di giorno, a 320 dp col testo a 1,3 e nella colonna del tablet.
 */
class MonitorsScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = phone(360), theme = "android:Theme.Material.NoActionBar")
    private val items = ContractJson.decodeMonitors(File("../core/src/test/resources/monitors-mockup.json").readText()).items
    private val st = ContractJson.decodeState(File("../contract/state-3-stale.json").readText())

    private fun phone(dp: Int, font: Float = 1f, h: Int = 2600) = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), fontScale = font, screenHeight = h)

    @After fun night() { V3.day = false }

    // Con una sezione da mostrare la sua categoria è aperta: la home ora parte chiusa (app-home-voci-chiuse-10-11).
    private fun home(s0: State, open: Set<String>, day: Boolean = false, list: List<it.pixelbox.cmwatch.contract.MonitorCard>? = items) = paparazzi.snapshot {
        val s = if (s0.homeOrder == null && open.isNotEmpty()) s0.copy(homeOrder = homeAllOpen) else s0
        CmPhoneTheme(still = true, day = day) {
            SummaryList(
                Summary.build(s, emptyList(), emptyList(), s.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = s.ts,
                usage = { Text("") }, kpiState = s, startOpen = open, monitors = list,
            )
        }
    }

    // La sezione aperta in fondo a Dalla master: due righe in ambra, le altre verdi, «Nuovo monitoraggio» in fondo.
    // Le schede non ancora arrivate (app-monitoraggi-attesa-10-11): la sezione c'è col numero del riassunto e una riga «in arrivo»; prima
    // spariva finché la lettura non riusciva.
    @Test fun homeSectionLoading() { paparazzi.unsafeUpdateConfig(deviceConfig = phone(360, h = 5000)); home(st.copy(monitors = it.pixelbox.cmwatch.contract.MonitorsSummary(7, 2, st.ts)), setOf("monitors"), list = null) }
    @Test fun homeSection() { paparazzi.unsafeUpdateConfig(deviceConfig = phone(360, h = 5000)); home(st, setOf("monitors")) }
    @Test fun homeSectionDay() { paparazzi.unsafeUpdateConfig(deviceConfig = phone(360, h = 5000)); home(st, setOf("monitors"), day = true) }
    @Test fun homeSection320Font13() { paparazzi.unsafeUpdateConfig(deviceConfig = phone(320, 1.3f, 6000)); home(st, setOf("monitors")) }
    // Dalla master chiusa: la riga nomina i due monitoraggi da guardare, in ambra (senza Prima raccolta, che passerebbe prima).
    @Test fun homeMasterClosed() { paparazzi.unsafeUpdateConfig(deviceConfig = phone(360, h = 3000)); home(st.copy(homeOrder = listOf("shut:you,work,tools"), primaRaccolta = null), emptySet()) }

    private fun page(id: String, day: Boolean = false) = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = day) { MonitorPage(items.single { it.id == id }, wide = false, onBack = {}, onStop = {}, onAsk = {}, onEditAlerts = {}) }
    }
    private fun tablet(id: String, day: Boolean = false) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 1640, screenHeight = 1600, density = com.android.resources.Density.XHIGH))
        paparazzi.snapshot { CmPhoneTheme(still = true, day = day) { androidx.compose.runtime.CompositionLocalProvider(LocalPageBg provides V3.S1) { TiAspettaColumn { MonitorPage(items.single { it.id == id }, wide = true, onBack = {}, onStop = {}, onAsk = {}, onEditAlerts = {}) } } } }
    }
    private fun small(id: String) { paparazzi.unsafeUpdateConfig(deviceConfig = phone(320, 1.3f, 3400)); page(id) }

    // Prezzi (mockup B): multiples, il pallino ambra sull'ultimo valore dello Yoga che ha fatto scattare la soglia, la nota Esempio.
    @Test fun pagePrices() = page("prezzi-portatili")
    @Test fun pagePricesDay() = page("prezzi-portatili", day = true)
    @Test fun pagePrices320Font13() = small("prezzi-portatili")
    @Test fun pagePricesTablet() = tablet("prezzi-portatili")
    @Test fun pagePricesTabletDay() = tablet("prezzi-portatili", day = true)
    // Adozione (mockup C), una scheda per forma come vuole il contratto: line (utenti nello Store), bars (download npm), table (stelle).
    @Test fun pageLine() = page("utenti-store")
    @Test fun pageLineDay() = page("utenti-store", day = true)
    @Test fun pageLine320Font13() = small("utenti-store")
    @Test fun pageLineTablet() = tablet("utenti-store")
    @Test fun pageBars() = page("download-npm")
    @Test fun pageBarsDay() = page("download-npm", day = true)
    @Test fun pageBars320Font13() = small("download-npm")
    @Test fun pageBarsTablet() = tablet("download-npm")
    @Test fun pageTable() = page("stelle-github")
    @Test fun pageTableDay() = page("stelle-github", day = true)
    @Test fun pageTable320Font13() = small("stelle-github")
    @Test fun pageTableTablet() = tablet("stelle-github")
    // Le forme che i mockup non mostrano: tile, meter (soglia scattata, in ambra) e status.
    @Test fun pageTile() = page("adozione")
    @Test fun pageTileDay() = page("adozione", day = true)
    @Test fun pageTile320Font13() = small("adozione")
    @Test fun pageTileTablet() = tablet("adozione")
    @Test fun pageMeter() = page("quota-gemini")
    @Test fun pageMeterDay() = page("quota-gemini", day = true)
    @Test fun pageMeter320Font13() = small("quota-gemini")
    @Test fun pageMeterTablet() = tablet("quota-gemini")
    @Test fun pageStatus() = page("sito-online")
    @Test fun pageStatusDay() = page("sito-online", day = true)
    @Test fun pageStatus320Font13() = small("sito-online")
    @Test fun pageStatusTablet() = tablet("sito-online")
}
