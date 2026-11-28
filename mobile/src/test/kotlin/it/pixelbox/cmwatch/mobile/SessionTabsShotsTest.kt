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
 * La sessione con la riga di icone e la tendina (app-posizioni-sistema-27-11, gruppo 2), a 360 dp e ferma: niente golden del movimento.
 * Due stati, con la testata vera della pagina (←, il nome, il ⋮ e il ≡): la riga con Chat aperta e i numeri dei tre colori (Per te 3 in
 * ambra, Risultati 1 in verde, Modifiche 4 in grigio), la conversazione sotto; la tendina di Dettagli giù, con modello ed effort, la
 * quota delle 5 ore, il contesto e le note, il campo in vista sotto. Golden da CI.
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
    @Test fun tendinaDettagli() = paparazzi.snapshot { sheet(SessionTabs.Key.DETTAGLI) }
}
