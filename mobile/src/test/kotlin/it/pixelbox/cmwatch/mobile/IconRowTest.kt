package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.IconRow
import it.pixelbox.cmwatch.mobile.ui.IconTab
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.IconRowRules.Tone
import org.junit.Rule
import org.junit.Test

/**
 * La riga di icone con la pillola elastica (app-posizioni-sistema-27-11, gruppo 1, e app-dock-elastico-27-11), quella della sessione,
 * a 360 dp e ferma: niente golden del movimento della pillola. Tre stati: una scheda aperta col nome; i numeri nei tre colori (ambra
 * Per te, verde Risultati, grigio Modifiche); un conteggio a zero, che toglie il numero e lascia l'icona al suo posto. Golden da CI.
 */
class IconRowTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (120 * 2.75).toInt()),
        theme = "android:Theme.Material.NoActionBar",
    )

    private fun tabs(perTe: Int, risultati: Int, modifiche: Int) = listOf(
        IconTab("chat", Icons.Rounded.ChatBubbleOutline, "Chat"),
        IconTab("perte", Icons.Outlined.PanTool, "Per te", perTe, Tone.WAIT),
        IconTab("risultati", Icons.Outlined.Visibility, "Risultati", risultati, Tone.NEW),
        IconTab("modifiche", Icons.Outlined.Description, "Modifiche", modifiche, Tone.COUNT),
        IconTab("dettagli", Icons.Outlined.Info, "Dettagli"),
    )

    @Composable private fun row(tabs: List<IconTab>, active: String) {
        Column(Modifier.fillMaxWidth().background(V3.Bg).padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconRow(tabs, active, onPick = {})
        }
    }

    /** La conversazione aperta: il nome «Chat» accanto all'icona, la pillola sotto; le altre schede solo icona e numero. */
    @Test fun openWithName() = paparazzi.snapshot { CmPhoneTheme(still = true) { row(tabs(perTe = 3, risultati = 1, modifiche = 4), "chat") } }

    /** I numeri nei tre colori con Risultati aperta: ambra chi aspetta Franz, verde il risultato nuovo, grigio il conteggio dei file. */
    @Test fun threeTones() = paparazzi.snapshot { CmPhoneTheme(still = true) { row(tabs(perTe = 7, risultati = 2, modifiche = 12), "risultati") } }

    /** Per te aperta a zero: niente numero e niente posto per lui, l'icona e il nome restano; Modifiche a zero solo con l'icona. */
    @Test fun zeroCount() = paparazzi.snapshot { CmPhoneTheme(still = true) { row(tabs(perTe = 0, risultati = 1, modifiche = 0), "perte") } }
}
