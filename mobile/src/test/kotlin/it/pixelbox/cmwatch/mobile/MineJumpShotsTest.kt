package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.MineJumpBar
import it.pixelbox.cmwatch.mobile.ui.MineJumpKey
import it.pixelbox.cmwatch.mobile.ui.V3
import org.junit.Rule
import org.junit.Test

/**
 * app-trova-messaggi-20-11 (Franz 21:12, «ok A»): il tasto dei miei messaggi e la barra del salto al posto del campo, a 360 dp col testo
 * normale e grande: «11 di 12» con le due frecce; al più nuovo la freccia giù spenta.
 */
abstract class MineJumpShots(scale: Float) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (300 * 2.75).toInt(), fontScale = scale), theme = "android:Theme.Material.NoActionBar")

    @Test fun bar() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Column(Modifier.background(V3.Bg).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().padding(end = 16.dp), contentAlignment = Alignment.CenterEnd) { MineJumpKey {} }
                MineJumpBar(11 to 12, canUp = true, canDown = true, onUp = {}, onDown = {}, onClose = {})
                MineJumpBar(12 to 12, canUp = true, canDown = false, onUp = {}, onDown = {}, onClose = {})
            }
        }
    }
}

class MineJumpShotsTest : MineJumpShots(1f)
class MineJumpShotsLargeTextTest : MineJumpShots(1.3f)
