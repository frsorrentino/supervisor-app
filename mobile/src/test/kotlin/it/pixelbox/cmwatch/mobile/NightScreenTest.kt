package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.contract.NightIndex
import it.pixelbox.cmwatch.mobile.ui.GanttViews
import it.pixelbox.cmwatch.mobile.ui.NightScreen
import it.pixelbox.cmwatch.rules.NightGantt
import it.pixelbox.cmwatch.rules.NightPage
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** La pagina Notte (specifica del 07/10, approvata alle 21:50) sulla fixture inventata del contratto 1.44, v2 dalla 1.74. */
class NightScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 3400), theme = "android:Theme.Material.NoActionBar")

    private val zone = ZoneId.of("Europe/Rome")
    private val report = ContractJson.decodeNightReport(File("../contract/night-report-sample.json").readText())
    private val page = NightPage.of(report, zone)
    private val gantt = GanttViews(NightGantt.projects(report, zone), NightGantt.goals(report, zone))
    private val earlier = listOf(NightIndex("2026-10-06", "2 progetti: 2 fatti", 1791247020, 2), NightIndex("2026-10-05", null, 1791160620, 1))

    /** Bozza A (Franz, 21/10 10:29): una riga per progetto, poi dove è arrivato ognuno; sotto le notti precedenti. */
    @Test fun ganttByProject() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            NightScreen(page, null, false, onBack = {}, onRefresh = {}, onChat = {}, onAnswer = { _, _ -> }, onApprove = {}, onSend = { _, _ -> }, zone = zone,
                gantt = gantt, earlier = earlier)
        }
    }

    /** Bozza B: il totale dei punti, poi un segmento per obiettivo con i suoi punti dentro. */
    @Test fun ganttByGoal() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            NightScreen(page, null, false, onBack = {}, onRefresh = {}, onChat = {}, onAnswer = { _, _ -> }, onApprove = {}, onSend = { _, _ -> }, zone = zone,
                gantt = gantt, byGoal = true, earlier = earlier)
        }
    }

    @Test fun closedCards() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            NightScreen(page, null, false, onBack = {}, onRefresh = {}, onChat = {}, onAnswer = { _, _ -> }, onApprove = {}, onSend = { _, _ -> }, zone = zone)
        }
    }

    /** Il tocco apre la card sul posto: esito intero, conteggi, passi con l'ora, «Conversazione» per una sessione viva. */
    @Test fun anOpenCard() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            NightScreen(page, null, false, onBack = {}, onRefresh = {}, onChat = {}, onAnswer = { _, _ -> }, onApprove = {}, onSend = { _, _ -> }, zone = zone, opened = "ledger-api")
        }
    }

    /** Un relay che non conosce l'op `night`: la pagina lo dice. */
    @Test fun anOldRelay() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            NightScreen(null, "Il PC non sa ancora leggere il rapporto della notte: aggiorna supervisor", false,
                onBack = {}, onRefresh = {}, onChat = {}, onAnswer = { _, _ -> }, onApprove = {}, onSend = { _, _ -> }, zone = zone)
        }
    }
}
