package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ChangesCount
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.TranscriptEntry
import it.pixelbox.cmwatch.contract.TranscriptTurn
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.MenuActions
import it.pixelbox.cmwatch.mobile.ui.PageHeader
import it.pixelbox.cmwatch.mobile.ui.SessionSheet
import it.pixelbox.cmwatch.mobile.ui.SheetActions
import it.pixelbox.cmwatch.rules.ChatFeed
import it.pixelbox.cmwatch.rules.SessionTabs
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * La sessione con la barra ibrida H1 e la tendina (app-barra-ibrida-28-11: la capsula della riga di icone con la pillola elastica e, dopo un
 * separatore, lo stato; prima la barra C sottile), a 360 dp e ferma: niente golden del movimento. Con la testata vera della pagina (←, il
 * nome, il ⋮ e il ≡): nella capsula i numeri dei tre colori (Per te 3 in ambra, Risultati 1 in verde, Modifiche 4 in grigio) e lo stato,
 * l'orologio con «5h 11%» e l'anellino del contesto (42 %), ridotto finché la riga ci sta (StatusBar.step); la conversazione sotto. Poi
 * Risultati aperta, col suo nome nella pillola e la sua tendina giù; la tendina di Dettagli, alta quanto il suo contenuto; la stessa
 * sessione in una colonna stretta da 300 dp col testo grande (1,3): lo stato si stringe ancora, il testo non va a capo e non si taglia.
 * Golden da CI.
 */
class SessionTabsShotsTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (780 * 2.75).toInt()),
        theme = "android:Theme.Material.NoActionBar",
    )
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val menu = MenuActions(null, "", false, {}, {}, {}, {}, {})

    @Composable private fun sheet(tab: SessionTabs.Key?) {
        val s = st.sessions.first { it.state == SessionState.IDLE }.copy(context = 42, changes = ChangesCount(4, 8, 2, st.ts))
        val q = st.quota.getValue(s.account).copy(h5 = 11, resetH5 = st.ts + 3 * 3600, stale = false)
        val t = st.ts - 600
        val entries = listOf(
            TranscriptEntry("u1.0", "user", text = "Il carrello sul telefono: quale delle due costruisco?", at = t, origin = "phone"),
            TranscriptEntry(
                "a1.0", "assistant", text = "La A tiene il riepilogo in alto, la B mette totale e Paga in una barra in fondo. Consiglio la B: sta sotto il pollice.",
                at = t + 40, turn = TranscriptTurn(t, t + 45, 4020, 130),
            ),
        )
        CmPhoneTheme(still = true) {
            SessionSheet(
                s, st.ts, emptyList(), 120, none, choices = st.choices, ops = st.ops, canAttach = true, accountQuota = q,
                feed = ChatFeed.merge(entries, emptyList()),
                appBar = { PageHeader(s.name, st.sessions, 3, st.ts, {}, {}, menu) },
                perTeCount = 3, newResults = 1, tabStart = tab,
            )
        }
    }

    @Test fun rigaChatAperta() = paparazzi.snapshot { sheet(null) }
    // app-barra-ibrida-28-11: una scheda aperta, col suo nome nella pillola della capsula.
    @Test fun rigaRisultatiAperta() = paparazzi.snapshot { sheet(SessionTabs.Key.RISULTATI) }
    @Test fun tendinaDettagli() = paparazzi.snapshot { sheet(SessionTabs.Key.DETTAGLI) }

    // app-barra-ibrida-28-11: una colonna stretta del tablet, 300 dp col testo grande; lo stato prende un passo più stretto.
    @Test fun colonnaStretta() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (300 * 2.75).toInt(), screenHeight = (640 * 2.75).toInt(), fontScale = 1.3f))
        paparazzi.snapshot { sheet(null) }
    }
}
