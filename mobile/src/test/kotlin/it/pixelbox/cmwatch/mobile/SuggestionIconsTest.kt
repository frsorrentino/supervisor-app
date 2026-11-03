package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.ui.Modifier
import it.pixelbox.cmwatch.contract.Suggestion
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.ItemRowView
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.mobile.ui.itemRight
import it.pixelbox.cmwatch.rules.ItemRows
import org.junit.Rule
import org.junit.Test

/** L'icona del tipo al posto del pallino, sulla riga del titolo (Franz 03/11 10:06, app-suggerimenti-icone-03-11): ogni tipo, poi una riga col pallino. */
class SuggestionIconsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 3600), theme = "android:Theme.Material.NoActionBar")

    private val kinds = listOf("unsaved", "followup", "observe", "choice", "idea", "proposal", "reminder", "recurring", "cleanup", "deadline", "quota", "waiting", "zzz")

    private fun shot(font: Float) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 3600, fontScale = font))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.background(V3.S1)) {
                    kinds.forEachIndexed { i, k ->
                        val r = ItemRows.suggestion(Suggestion("s-$k", k, k, if (i % 2 == 0) "my.pixelbox.it ha 10 file fuori da git: li smisto?" else "International Young Club: guardiamo in Site Tools se la chiave è ancora attiva?", "p", 0))
                        ItemRowView(r, itemRight(r), i == 0, false, onClick = {}) {}
                    }
                    val dot = ItemRows.Row("d", "Una riga col pallino", "sotto, cosa c'è", ItemRows.Tone.WAIT, emptyList())
                    ItemRowView(dot, "", false, false, onClick = {}) {}
                }
            }
        }
    }
    @Test fun icone() = shot(1f)
    @Test fun icone_testoGrande() = shot(1.3f)
}
