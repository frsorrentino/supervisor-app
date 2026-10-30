package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.rules.SettingsDevices
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Le Impostazioni del mockup A (Franz, 03/10 21:59): sezioni per argomento e lo schema dei dispositivi da toccare. */
class SettingsScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private fun devices(pending: Boolean = false, near: Boolean? = true, fresh: Freshness = Freshness.Fresh) =
        SettingsDevices.build("penguin", st, fresh, st.ts, "Pixel 9", "0.2", true, "Pixel Watch 5", pending, near)

    // Franz 30/10 21:46: all'apertura tutte chiuse, il riassunto a destra.
    @Test fun settingsSectionsClosed() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SettingsScreen("penguin", "Pixel 9", "Pixel Watch 5", false, false, "0.2", {}, {}, {}, {}, devices = devices(), linked = SettingsDevices.linked(st, "phoneUid00000000000000000000", st.ts))
        }
    }

    @Test fun settingsPaired() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { SettingsScreen("penguin", "Pixel 9", "Pixel Watch 5", false, false, "0.2", {}, {}, {}, {}, initialOpen = ALL_SECTIONS, devices = devices()) }
    }

    // Contratto 1.32, variante B (Franz, 04/10 17:11): il PC sopra e i dispositivi veri in fila sotto, «questo» è il telefono;
    // il Chromebook letto ore fa in arancio, il Pixel 7 mai arrivato tratteggiato.
    @Test fun settingsDevicesB() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SettingsScreen(
                "penguin", "Pixel 9", "Pixel Watch 5", false, false, "0.2", {}, {}, {}, {}, initialOpen = ALL_SECTIONS, devices = devices(),
                linked = SettingsDevices.linked(st, "phoneUid00000000000000000000", st.ts),
            )
        }
    }

    // Contratto 1.89 (Franz, 28/10): l'elenco con l'ultimo segno di vita di ognuno e Revoca accanto, tranne che su questo
    // telefono; il browser conta il comando delle 10 s dopo la lettura, il Pixel 7 l'arrivo tenuto dal relay.
    @Test fun device_last_seen_list() = paparazzi.snapshot {
        val stale = ContractJson.decodeState(File("../contract/state-3-stale.json").readText())
        CmPhoneTheme(still = true) {
            Box(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg).padding(16.dp)) {
                LinkedList(SettingsDevices.linked(stale, "phoneUid00000000000000000000", 1789211000)!!, 1789211000, onRevoke = {})
            }
        }
    }

    // L'orologio senza chiave: il suo filo e il suo punto in arancio, la scheda dice cosa manca.
    @Test fun settingsWatchPending() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SettingsScreen("penguin", "Pixel 9", "Pixel Watch 5", true, false, "0.2", {}, {}, {}, {}, initialOpen = ALL_SECTIONS, devices = devices(pending = true, near = null, fresh = Freshness.Stale(12)), initialDevice = DeviceNode.WATCH)
        }
    }

    @Test fun settingsDemo() = paparazzi.snapshot { CmPhoneTheme(still = true) { SettingsScreen(null, "Pixel 9", null, false, true, "0.2", {}, {}, {}, {}, initialOpen = ALL_SECTIONS) } }
}

private val ALL_SECTIONS = setOf("link", "reading", "app", "info")
