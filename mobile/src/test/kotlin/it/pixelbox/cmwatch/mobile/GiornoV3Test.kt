package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.HomeBox
import it.pixelbox.cmwatch.mobile.ui.HomeBoxScreen
import it.pixelbox.cmwatch.mobile.ui.SettingsScreen
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.Summary
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** La versione giorno B «Attese in ambra» (Franz 30/10 21:56): fondo grigio-azzurro, scatole bianche col filetto, attese in ambra chiaro. */
class GiornoV3Test {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.Light.NoActionBar")
    private val zone = ZoneId.of("Europe/Rome")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())

    @After fun night() { V3.day = false }

    @Test fun home() = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = true) {
            Column(Modifier.background(V3.Bg)) { SummaryList(Summary.build(st, emptyList(), emptyList(), st.ts, zone, emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}) }
        }
    }

    @Test fun settings() = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = true) {
            SettingsScreen("penguin", "Pixel 9", "Pixel Watch 5", false, false, "0.2", {}, {}, {}, {}, initialOpen = setOf("app", "info"))
        }
    }

    @Test fun boxWaiting() = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = true) { HomeBoxScreen(HomeBox.WAITING, st, null, st.ts, onBack = {}, onSession = {}, zone = zone) }
    }
}
