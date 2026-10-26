package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.TabletQuotaPanel
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.QuotaHistory
import it.pixelbox.cmwatch.rules.QuotaHistory.Sample
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Rule
import org.junit.Test

/**
 * Il ritmo delle cinque ore (1A e 1B, 25/10) nel pannello di Utilizzo e limiti, a 320, 360 e 412 dp: regola di Franz
 * (21:33), nessun testo esce da card e pagina. Due casi: il 100% arriva 64 minuti prima del reset (la fascia rossa con
 * «fermi 64 min») e la quota indietro sul tempo a ritmo lento.
 */
class RhythmPanelTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val reset = 1_793_050_800L
    private val start = reset - QuotaHistory.WINDOW_S
    private val now = start + 160 * 60

    // 30 punti nell'ultima ora: dal 62% il 100% arriva in 76 minuti, 64 minuti prima del reset.
    private val fast = PhoneOverview.Ring(
        "personal", true, 62, 41, reset,
        QuotaHistory.Pace(listOf(Sample(start + 600, 2), Sample(now - 7200, 18), Sample(now - 3600, 32), Sample(now - 1800, 47), Sample(now, 62)), null, null),
        weekResetAt = reset + 3 * 86400,
    )
    private val slow = PhoneOverview.Ring(
        "work", false, 21, 12, reset,
        QuotaHistory.Pace(listOf(Sample(start + 900, 3), Sample(now - 3600, 15), Sample(now, 21)), null, null),
        weekResetAt = reset + 3 * 86400,
    )

    // Un pannello per scatto: due insieme a 320 dp superano l'altezza dello schermo del test, che non scorre.
    private fun at(dp: Int, ring: PhoneOverview.Ring) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt()))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.fillMaxSize().background(CmColors.bg).padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    TabletQuotaPanel(ring, now)
                }
            }
        }
    }

    @Test fun rhythmFull320() = at(320, fast)
    @Test fun rhythmFull360() = at(360, fast)
    @Test fun rhythmFull412() = at(412, fast)
    @Test fun rhythmBehind320() = at(320, slow)
    @Test fun rhythmBehind360() = at(360, slow)
    @Test fun rhythmBehind412() = at(412, slow)
}
