package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

class LaunchSheetTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())

    @Test fun launchSheet() = paparazzi.snapshot { CmPhoneTheme(still = true) { LaunchSheet(st) { _, _ -> } } }

    // Contratto 1.49 (Franz, 09/10 21:27, scelta A): nel foglio della notte le schede del Recap da spuntare, prima quelle
    // che può fare Claude; le tue chiuse sotto la loro riga. Due spuntate: il tasto tonale «Metti stanotte · 2».
    @Test fun nightSheetWithRecapCards() = paparazzi.snapshot {
        val rows = it.pixelbox.cmwatch.rules.RecapAgenda.night(it.pixelbox.cmwatch.contract.AgendaPage(listOf(
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "agenzia", "franz", "Confirm the 6 client ids with a candidate", ".claude/to-decide-client-id.md", key = "k1"),
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "personale", "claude", "Move the docs site to the new host", "orbit-docs", key = "k2"),
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "postazione", "claude", "Clear the orphan plugin caches", "after restarting every session", key = "k3"),
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "agenzia", "terzi", "Client answer on the domain", "", key = "k4"),
        )))
        CmPhoneTheme(still = true) {
            LaunchSheet(st, action = R.string.night_add, top = { NightAgendaPicker(rows, initial = listOf("k2", "k3")) {} }) { _, _ -> }
        }
    }

    // Quinto giro della notte (app-bug-notte-03-11): la stessa schermata a 320 dp col testo grande, in inglese e in orizzontale.
    private fun sweep(kind: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        val base = DeviceConfig.PIXEL_5
        val cfg = when (kind) {
            "320" -> base.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), fontScale = 1.3f)
            "en" -> base.copy(locale = "en")
            else -> base.copy(locale = "it", screenWidth = base.screenHeight, screenHeight = base.screenWidth)
        }
        paparazzi.unsafeUpdateConfig(deviceConfig = cfg)
        paparazzi.snapshot { content() }
    }
    @Test fun giro5_avvio_320Grande() = sweep("320") { CmPhoneTheme(still = true) { LaunchSheet(st) { _, _ -> } } }
    @Test fun giro5_avvio_en() = sweep("en") { CmPhoneTheme(still = true) { LaunchSheet(st) { _, _ -> } } }
    @Test fun giro5_avvio_orizzontale() = sweep("land") { CmPhoneTheme(still = true) { LaunchSheet(st) { _, _ -> } } }
}
