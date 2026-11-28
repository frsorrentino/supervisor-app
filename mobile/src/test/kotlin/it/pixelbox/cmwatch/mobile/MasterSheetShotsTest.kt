package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.TranscriptEntry
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalBordoFermo
import it.pixelbox.cmwatch.mobile.ui.MasterSheetHead
import it.pixelbox.cmwatch.mobile.ui.MenuActions
import it.pixelbox.cmwatch.mobile.ui.PageHeader
import it.pixelbox.cmwatch.mobile.ui.SessionSheet
import it.pixelbox.cmwatch.mobile.ui.SheetActions
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.rules.ChatFeed
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.MasterSheet
import it.pixelbox.cmwatch.rules.Summary
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * Il foglio della master sul telefono (app-posizioni-sistema-27-11 gruppo 4), a 360 dp e fermo, con la testata vera della home: la home con
 * il foglio alla riga (maniglia, la riga della master col suo stato e ▶, e il campo) al posto della barra di prima; il foglio a metà sopra la
 * home, con la conversazione fra la sua riga e il campo. Golden da CI; niente golden del movimento.
 */
class MasterSheetShotsTest {
    // La home dietro al foglio è rimpicciolita (0,96-0,98): il ricampionamento cambia l'antialiasing da una macchina della CI all'altra
    // (0,011-0,017 % dei pixel, run di cd711890, 28/11); la tolleranza dello 0,1 % lo accetta, come per i pannelli della quota.
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (780 * 2.75).toInt()),
        theme = "android:Theme.Material.NoActionBar",
        maxPercentDifference = 0.1,
    )
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val menu = MenuActions(null, "", false, {}, {}, {}, {}, {})
    private val master = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null, suggestion = null)
    private val entries = listOf(
        TranscriptEntry("u1.0", "user", text = "Come va la fase 2.2 di atlas-shop?", at = st.ts - 600, origin = "phone"),
        TranscriptEntry(
            "a1.0", "assistant",
            text = "Lanciata la fase 2.2 su atlas-shop: test verdi, 40 su 40. Solo commit locali: push e release con il tuo ok.\n\nEsito: fase 2.2 avviata su atlas-shop\nProssimi: fai il push · prova dal vivo",
            at = st.ts - 560,
        ),
    )

    @Composable private fun home(stop: MasterSheet.Stop) {
        val summary = Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet())
        CmPhoneTheme(still = true) {
            SessionSheet(
                master, st.ts, emptyList(), 120, none, grid = true, canAttach = true,
                home = { _, _ -> Column { SummaryList(summary, {}, { _, _ -> }, { _, _ -> }, {}, {}, startOpen = setOf("sessions")) } },
                homeOpen = stop == MasterSheet.Stop.ROW, sheetAt = stop,
                appBar = { PageHeader(null, st.sessions, 3, st.ts, {}, {}, menu) },
                dock = { MasterSheetHead(master, MasterHome.hero(entries, master), {}, closed = stop == MasterSheet.Stop.ROW) },
                feed = ChatFeed.merge(entries, emptyList()),
            )
        }
    }

    @Test fun riga() = paparazzi.snapshot { home(MasterSheet.Stop.ROW) }
    @Test fun meta() = paparazzi.snapshot { home(MasterSheet.Stop.HALF) }

    // app-bordo-master-27-11: il campo della master in una striscia di 360 × 220 dp, col foglio alla riga sopra una home vuota. Da ferma
    // l'anello di 1,5 dp; al lavoro il raggio fermo a 12 s del giro (LocalBordoFermo: un quinto del bordo, a metà del lato di sopra), così
    // l'immagine non dipende dall'ora. Il giro vero si vede dal vivo.
    private fun campo(lavora: Boolean) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (220 * 2.75).toInt()))
        val m = if (lavora) master.copy(state = SessionState.BUSY) else master
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                CompositionLocalProvider(LocalBordoFermo provides 12_000L) {
                    SessionSheet(
                        m, st.ts, emptyList(), 120, none, grid = true, canAttach = true,
                        home = { _, _ -> }, homeOpen = true, sheetAt = MasterSheet.Stop.ROW,
                        dock = { MasterSheetHead(m, MasterHome.hero(entries, m), {}, closed = true) },
                        feed = ChatFeed.merge(entries, emptyList()),
                    )
                }
            }
        }
    }
    @Test fun campoAnello() = campo(lavora = false)
    @Test fun campoRaggio() = campo(lavora = true)
}
