package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
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
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * In basso nella sessione (app-posizioni-sistema-27-11 gruppo 3), a 360 dp e ferma, con la testata vera della pagina e la riga di icone del
 * gruppo 2. Tre stati: la riga delle Azioni con due chip sopra il campo e l'anellino al 18 % nel campo; il campo scritto, con la riga
 * nascosta e l'anellino che resta; il contesto al 72 %, col chip ambra «Contesto 72% · Handoff» per primo e l'anellino ambra. Golden da CI.
 */
class SessionBottomShotsTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (780 * 2.75).toInt()),
        theme = "android:Theme.Material.NoActionBar",
    )
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val menu = MenuActions(null, "", false, {}, {}, {}, {}, {})

    @Composable private fun sheet(context: Int, typed: String = "") {
        // La sessione ferma della fixture, senza il suggerito del terminale: nel campo vuoto resta «Scrivi a field-notes».
        val s = st.sessions.first { it.state == SessionState.IDLE }.copy(context = context, suggestion = null)
        val t = st.ts - 600
        val entries = listOf(
            TranscriptEntry("u1.0", "user", text = "Il carrello sul telefono: quale delle due costruisco?", at = t, origin = "phone"),
            TranscriptEntry(
                "a1.0", "assistant",
                text = "Il carrello sul telefono è rifatto dopo la tua nota sul primo giro. La A tiene il riepilogo in alto, la B mette totale e Paga in una barra in fondo.\n\nProssimi: scegli B e prosegui · mostrami le due varianti",
                at = t + 40, turn = TranscriptTurn(t, t + 45, 4020, 130),
            ),
        )
        CmPhoneTheme(still = true) {
            SessionSheet(
                s, st.ts, emptyList(), 120, none, choices = st.choices, ops = st.ops, canAttach = true,
                feed = ChatFeed.merge(entries, emptyList()),
                appBar = { PageHeader(s.name, st.sessions, 3, st.ts, {}, {}, menu) },
                draftState = mutableStateOf(typed), perTeCount = 2, newResults = 1,
            )
        }
    }

    @Test fun azioniEAnellino() = paparazzi.snapshot { sheet(18) }
    @Test fun campoScritto() = paparazzi.snapshot { sheet(18, typed = "scegli B e prosegui") }
    @Test fun contesto72() = paparazzi.snapshot { sheet(72) }
}
