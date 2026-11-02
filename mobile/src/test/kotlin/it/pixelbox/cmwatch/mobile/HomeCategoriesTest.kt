package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.SystemStatus
import it.pixelbox.cmwatch.contract.TimelineEvent
import it.pixelbox.cmwatch.contract.TimelinePage
import it.pixelbox.cmwatch.contract.TimelineSession
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.rules.Summary
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * La home per categorie (Franz 02/11 16:44, app-home-categorie-02-11; mockup home-categorie-telefono.png): Richiede te e Attività
 * aperte, Esiti e Numeri chiuse con il segnale a destra, in ambra quando c'è qualcosa.
 */
class HomeCategoriesTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 5000), theme = "android:Theme.Material.NoActionBar")
    private val full = ContractJson.decodeState(File("../contract/state-3-stale.json").readText())
    private val system = SystemStatus(at = full.ts, cores = 8, load1 = 2.4, loadPerCore = 0.3, memTotalMb = 16000, memAvailableMb = 4600, level = "green", loadLevel = "green", memLevel = "green")
    private fun page(failed: Boolean) = TimelinePage(full.ts - 3600, listOf(TimelineSession("atlas-shop", true, events = listOfNotNull(
        TimelineEvent(full.ts - 60, "outcome", "Migrations applied", type = "done"),
        TimelineEvent(full.ts - 120, "outcome", "SEO pages online", type = "shipped"),
        if (failed) TimelineEvent(full.ts - 30, "task", "Release 1.4", ok = false, ref = "ledger-release-14") else null,
    ))))

    private fun shot(st: State = full, failed: Boolean = false, open: Set<String> = emptySet()) = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SummaryList(
                Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts,
                usage = { Text("") }, system = system, kpiState = st, kpiPage = page(failed), startOpen = open,
                plugins = st.plugins, onPluginsLoad = { null }, onPluginCmd = { _, _ -> null },
            )
        }
    }

    @Test fun homeByCategories() = shot()

    /** Un fallito oggi: la riga di Esiti, chiusa, lo dice in ambra; niente cambia posto. */
    @Test fun homeClosedCategorySignal() = shot(failed = true)

    /** Esiti in vista per scelta (Riordina): l'etichetta della categoria e le sue sezioni al posto della riga. */
    @Test fun homeCategoryKeptOpen() = shot(full.copy(homeOrder = listOf("shut:tools")))

    @Test fun homeCategoriesFont13At320() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), fontScale = 1.3f, screenHeight = 5000))
        shot(failed = true)
    }

    /** Riordina (mockup, riquadro 4): una card per categoria con maniglia e interruttore, le sezioni sotto con la loro maniglia. */
    @Test fun reorderByCategories() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 2700))
        paparazzi.snapshot { CmPhoneTheme(still = true) { it.pixelbox.cmwatch.mobile.ui.HomeOrderScreen(null, {}, {}) } }
    }

    /**
     * Lo spazio in cima (app-home-in-cima-02-11): Esiti di oggi e Sessioni sopra le categorie, senza testata; Esiti, chiusa, non le
     * conta più nel segnale e Attività non ripete Sessioni.
     */
    @Test fun homeWithTopItems() = shot(full.copy(homeOrder = listOf("top:esiti", "top:sessions")), failed = true)

    /** Riordina con lo spazio in cima: la prima card, con «Togli dalla cima»; le sezioni delle categorie hanno «Metti in cima». */
    /**
     * app-tabellone-aperte-02-11 (Franz 02/11 21:41): le voci rilasciate sul tabellone compaiono aperte, con la riga dello slot
     * (Utilizzo con la barra della settimana, Sistema col carico), subito sotto i tre riquadri; Ti aspettano viene dopo.
     */
    @Test fun homeBoardItemsWithExtra() {
        val asking = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
        shot(asking.copy(homeOrder = listOf("top:usage", "top:system")), open = setOf("usage"))
    }

    @Test fun reorderWithTop() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 3000))
        paparazzi.snapshot { CmPhoneTheme(still = true) { it.pixelbox.cmwatch.mobile.ui.HomeOrderScreen(listOf("top:esiti", "top:sessions"), {}, {}) } }
    }
}
