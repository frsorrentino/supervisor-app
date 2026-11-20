package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.mobile.ui.CloseAskBody
import it.pixelbox.cmwatch.mobile.ui.CloseCardFrame
import it.pixelbox.cmwatch.mobile.ui.CloseSplashOverlay
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.SheetActions
import it.pixelbox.cmwatch.mobile.ui.SessionSheet
import it.pixelbox.cmwatch.mobile.ui.closePairFits
import it.pixelbox.cmwatch.rules.CloseSplash
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * app-chiudi-scheda-20-11 (Franz 20/11 19:08, bozza C senza tasto bianco): la carta della chiusura in fondo alla pagina, a 360 dp,
 * col testo normale e grande. «Lascia aperta» e «Chiudi senza salvare» stanno a metà solo se ci stanno interi, altrimenti in colonna;
 * nessun tasto va a capo.
 */
abstract class CloseCardShots(scale: Float) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (800 * 2.75).toInt(), fontScale = scale), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val s = st.sessions.first { it.state == SessionState.IDLE }

    private fun page(card: @androidx.compose.runtime.Composable () -> Unit) = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.fillMaxSize()) {
                SessionSheet(s, st.ts, emptyList(), 120, SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}))
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { card() }
            }
        }
    }

    @Test fun ask() = page {
        CloseCardFrame(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) { CloseAskBody(s.copy(attached = true), save = true, onKeep = {}, onClose = {}) }
    }
    @Test fun refusedItalian() = page {
        CloseSplashOverlay(CloseSplash.Phase.Refused(s.name, "resta aperta: ha una domanda", armable = true), onHome = {}, onReopen = null, onArm = {})
    }
}

class CloseCardTest : CloseCardShots(1f) {
    @Test fun halvesOnlyWhenBothFitWhole() {
        assertTrue(closePairFits(a = 150, b = 160, gap = 8, max = 328))
        assertFalse("un pixel in più: in colonna", closePairFits(a = 150, b = 161, gap = 8, max = 328))
    }
}

class CloseCardLargeTextTest : CloseCardShots(1.3f)
