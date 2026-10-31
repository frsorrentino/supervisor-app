package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.mobile.ui.V3Section
import org.junit.Rule
import org.junit.Test

/** Le otto voci della home con la stessa struttura e i riepiloghi più lunghi (Franz 31/10 17:04): su una riga a 360 e a 412 dp. */
class HomeRowsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"))

    private fun at(dp: Int) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 1400))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.fillMaxSize().background(V3.Bg).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    V3Section("Sessioni", Icons.Outlined.PlayCircle, "12", "10 al lavoro · 2 finite", open = false, onToggle = {})
                    V3Section("Suggerimenti", Icons.Outlined.AutoAwesome, "3", "ultimo 16:05", open = false, onToggle = {})
                    V3Section("Utilizzo", Icons.Outlined.DataUsage, right = "pers. 20% · prof. 39%", open = false, onToggle = {})
                    V3Section("Sistema", Icons.Outlined.Memory, right = "carico 0,1 · 4,5 GB", open = false, dot = Color(0xFF34D399), onToggle = {})
                    V3Section("Plugin e skill", Icons.Outlined.Extension, right = "aggiornati", open = false, onToggle = {})
                    V3Section("Esiti di oggi", Icons.Outlined.SportsScore, right = "1 fallito · 68 · 16:49", open = false, onToggle = {})
                    V3Section("Lavori", Icons.Outlined.Checklist, right = "7 attivi · 2 notte · 19 oggi", open = false, onToggle = {})
                    V3Section("Chiuse", Icons.Outlined.Inventory2, "46", nav = true, onToggle = {})
                }
            }
        }
    }

    @Test fun rows360() = at(360)
    @Test fun rows412() = at(412)
}
