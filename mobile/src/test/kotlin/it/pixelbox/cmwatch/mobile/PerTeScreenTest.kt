package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ResultGroup
import it.pixelbox.cmwatch.contract.ResultItem
import it.pixelbox.cmwatch.mobile.ui.BlockIcon
import it.pixelbox.cmwatch.mobile.ui.ChatBlock
import it.pixelbox.cmwatch.mobile.ui.ChatBlocks
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.PerTeBlock
import it.pixelbox.cmwatch.mobile.ui.ResultsChatRow
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.Guardare
import it.pixelbox.cmwatch.rules.PerTe
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * Il formato unico (app-formato-unico-12-11, Franz 12/11 07:29: formato A e blocchi B): «Per te» in cima alla chat con le voci della
 * sessione del mockup (una domanda, una decisione, una prova, un risultato da guardare), aperto e ridotto, accanto a «Risultati» e
 * «Azioni» ridotti; telefono e colonna del tablet, giorno e notte, schermo stretto coi caratteri grandi. Golden da CI.
 */
class PerTeScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 1500), theme = "android:Theme.Material.NoActionBar")
    private val now = 1_789_214_700L
    // Le richieste arrivano come dal relay, con `attese`.
    private val waits = it.pixelbox.cmwatch.contract.ContractJson.decodeWaits("""{"at": $now, "items": [
        {"id": "w1", "label": "reply", "session": "storefront", "title": "Pubblico la 2.4 in produzione?", "text": "Pubblico la 2.4 in produzione?", "at": ${now - 420},
         "actions": [{"label": "sì", "style": "option", "recommended": true, "cmd": {"op": "answer"}}, {"label": "no", "style": "option", "cmd": {"op": "answer"}}]},
        {"id": "w2", "label": "decide", "session": "storefront", "title": "Logo ambra sulla scheda Play", "text": "procedo quando Google approva", "at": ${now - 7200},
         "actions": [{"label": "Procedi", "style": "primary", "cmd": {"op": "approve"}}, {"label": "Non ora", "style": "secondary", "cmd": {"op": "decisione"}}]},
        {"id": "w3", "label": "verify", "session": "storefront", "title": "Prova #569 · link diretto in Verifica", "text": "tocca «prova #550» in un messaggio della master", "at": ${now - 1200},
         "actions": [{"label": "Ok", "style": "primary", "cmd": {"op": "prova"}}, {"label": "Non va", "style": "secondary", "cmd": {"op": "prova"}, "text_field": "text"}]}
    ]}""").items
    private val results = listOf(ResultGroup(
        title = "Rapporto dei test dei pagamenti", session = "storefront", kind = "document", at = now - 3600, versions = 2, unseen = true,
        items = listOf(ResultItem(id = "r1", session = "storefront", title = "Rapporto dei test dei pagamenti", kind = "document", path = "out/report.md", at = now - 3600, version = 2, versions = 2)),
    ))
    private val items = PerTe.voci(waits, results, "storefront")

    @After fun night() { V3.day = false }

    @Composable private fun blocks(open: Boolean) {
        val closed = remember { ChatBlocks(false, false) { _, _ -> } }
        Column(Modifier.fillMaxSize().background(V3.Bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PerTeBlock(items, now, open, {}, {}, {})
            ResultsChatRow(Guardare.SessionLine(results, results[0].items[0]), onClose = {}, blocks = remember { ChatBlocks(false, open) { _, _ -> } }) {}
            ChatBlock(BlockIcon.BOLT, "Azioni", 2, "prova il checkout dal vivo", false, {}) {}
            if (open) ResultsChatRow(Guardare.SessionLine(results, results[0].items[0]), blocks = closed) {}
        }
    }

    @Test fun reduced() = paparazzi.snapshot { CmPhoneTheme(still = true) { blocks(open = false) } }
    @Test fun reducedDay() = paparazzi.snapshot { CmPhoneTheme(still = true, day = true) { blocks(open = false) } }
    @Test fun open() = paparazzi.snapshot { CmPhoneTheme(still = true) { blocks(open = true) } }
    @Test fun openDay() = paparazzi.snapshot { CmPhoneTheme(still = true, day = true) { blocks(open = true) } }
    @Test fun open320Font13() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), fontScale = 1.3f, screenHeight = 1800))
        paparazzi.snapshot { CmPhoneTheme(still = true) { blocks(open = true) } }
    }
    // Nella colonna larga del tablet.
    @Test fun tablet() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 1640, screenHeight = 1200, density = com.android.resources.Density.XHIGH))
        paparazzi.snapshot { CmPhoneTheme(still = true) { blocks(open = true) } }
    }
    @Test fun tabletDay() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 1640, screenHeight = 1200, density = com.android.resources.Density.XHIGH))
        paparazzi.snapshot { CmPhoneTheme(still = true, day = true) { blocks(open = true) } }
    }
}
