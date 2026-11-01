package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.SlotInfo
import org.junit.Rule
import org.junit.Test
import java.time.ZoneOffset

/** Le cinque voci nello slot, con soli dati demo (compito app-slot-info-01-11): acceso, macchina spenta, testo lungo, niente per Franz. */
class SlotInfoScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val now = 1_793_500_000L

    private fun shot(locale: String, dp: Int = 392, scale: Float = 1f) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = locale, screenWidth = (dp * 2.75).toInt(), fontScale = scale))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.fillMaxWidth().background(V3.S1)) {
                    SystemSlotRow(SlotInfo.SystemInfo(42, 42, "green", 37, listOf(SlotInfo.HostRam("win", 40)), 3), Modifier, false) {}
                    SystemSlotRow(SlotInfo.SystemInfo(160, 100, "red", 88, listOf(SlotInfo.HostRam("win", null)), 1), Modifier, false) {}
                    LavoriSlotRow(SlotInfo.LavoriInfo(21, 29, "Logo ambra", now - 7200), now, Modifier, false) {}
                    LavoriSlotRow(SlotInfo.LavoriInfo(0, 4, null, null), now, Modifier, false) {}
                    SlotInfoRow(Modifier, stringResource(R.string.sug_title), Icons.Outlined.AutoAwesome, "s", count = "5", names = listOf("Atlas Shop", "payments-api", "Field Notes", "Orbit Docs", "Storefront release"), open = false) {}
                    SlotInfoRow(Modifier, stringResource(R.string.sug_title), Icons.Outlined.AutoAwesome, "s2", count = "1", names = listOf("Un nome solo troppo lungo per stare su questa riga intera del telefono, mai"), open = false) {}
                    PluginsSlotRow(SlotInfo.PluginsInfo(listOf("alfa", "beta"), null), ZoneOffset.UTC, Modifier, false) {}
                    PluginsSlotRow(SlotInfo.PluginsInfo(emptyList(), now), ZoneOffset.UTC, Modifier, false) {}
                    SlotRow(Modifier, stringResource(R.string.cat_closed), Icons.Outlined.Inventory2, "4", tag = "c", nav = true) {}
                }
            }
        }
    }

    @Test fun slot_info_italiano() = shot("it")
    @Test fun slot_info_inglese() = shot("en")
    @Test fun slot_info_412Grande() = shot("it", 412, 1.3f)
    @Test fun slot_info_320Grande() = shot("it", 320, 1.3f)
}
