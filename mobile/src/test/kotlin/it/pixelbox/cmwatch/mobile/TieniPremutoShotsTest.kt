package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.HoldButton
import it.pixelbox.cmwatch.mobile.ui.HoldForm
import it.pixelbox.cmwatch.mobile.ui.HoldTone
import it.pixelbox.cmwatch.mobile.ui.MenuPanel
import it.pixelbox.cmwatch.mobile.ui.SessionMenuActions
import it.pixelbox.cmwatch.mobile.ui.SessionMenuHolds
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.mobile.ui.WaitCardBody
import it.pixelbox.cmwatch.mobile.ui.sessionMenuEntries
import it.pixelbox.cmwatch.rules.SessionMenu
import it.pixelbox.cmwatch.rules.TieniPremuto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Il tasto «tieni premuto» (app-tieni-premuto-28-11, Franz 28/11 16:22; mockup tieni-premuto-28-11.html), a 360 dp, fermo: la pillola rossa
 * in fondo al menu della sessione, la pillola ambra sulla carta di una pubblicazione, una pillola piena a metà (il pieno fermato con
 * `pinned`), e il «Va» a un tocco della correzione della master del 28/11, fermo e arrivato alla spunta. Golden da CI.
 */
class TieniPremutoShotsTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (800 * 2.75).toInt()),
        theme = "android:Theme.Material.NoActionBar",
    )
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val cards = Json.parseToJsonElement(File("../core/src/test/resources/tieni-premuto-cases.json").readText()).jsonObject["cards"]!!.jsonArray
    private fun card(id: String): WaitItem = cards.map { c -> ContractJson.json.decodeFromJsonElement(WaitItem.serializer(), c.jsonObject["item"]!!) }.first { w -> w.id == id }

    private fun page(content: @Composable () -> Unit) = paparazzi.snapshot {
        CmPhoneTheme(still = true) { Box(Modifier.fillMaxSize().background(V3.Bg).padding(12.dp)) { content() } }
    }

    // Il menu ⋮ di ledger-api: le voci, «Chiudi senza salvare», poi «Tieni premuto per ripartire» e la pillola rossa, fermi.
    @Test fun menuRedIdle() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            val s = st.sessions.first { x -> x.name == "ledger-api" }
            val items = SessionMenu.entries(s, st, canExit = true)
            val acts = SessionMenuActions(follow = {}, askMaster = {}, openInClaude = {}, restartClean = {}, exit = {}, exitNoSave = {})
            Box(Modifier.fillMaxSize().background(V3.Bg)) {
                MenuPanel(onDismiss = {}, entries = sessionMenuEntries(s, items, acts), bottom = { close -> SessionMenuHolds(s, items, acts, close) }) {
                    Text(s.name, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // La carta del deploy del mockup: la scelta consigliata lascia l'elenco e diventa la pillola ambra da tenere premuta, in fondo.
    @Test fun releaseCardAmber() = page { WaitCardBody(card("w1")) { _, _ -> } }

    // Il pieno fermo a metà: la scritta scura sotto il pieno, quella ambra fuori.
    @Test fun halfFilled() = page {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HoldButton(
                stringResource(R.string.hold_publish), onDone = {}, holdMs = TieniPremuto.holdMs(TieniPremuto.Place.PUBBLICA), tone = HoldTone.AMBER,
                icon = Icons.Outlined.RocketLaunch, pinned = 0.5f,
            )
        }
    }

    // La carta di una verifica: il «Va» resta un tocco, nella forma di prima.
    @Test fun vaIdle() = page { WaitCardBody(card("w5")) { _, _ -> } }

    // Il «Va» toccato, fermo alla spunta: il cerchio verde con la spunta disegnata.
    @Test fun vaAtCheck() = page {
        HoldButton(
            "Va", onDone = {}, mode = TieniPremuto.Mode.TAP, tone = HoldTone.AMBER, form = HoldForm.KEY, pinned = 1f,
        )
    }
}
