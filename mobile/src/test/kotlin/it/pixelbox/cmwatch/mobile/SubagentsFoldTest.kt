package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.Subagents
import it.pixelbox.cmwatch.mobile.ui.Cm
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.HomeBox
import it.pixelbox.cmwatch.mobile.ui.HomeBoxScreen
import it.pixelbox.cmwatch.mobile.ui.SubagentsFold
import kotlinx.serialization.json.jsonObject
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * app-sottoagenti-23-11 (variante A, Franz 23/11 13:26): in «Al lavoro adesso» la sessione con sottoagenti ha la sua sezione, chiusa
 * di base («1 al lavoro, 6 finiti»); aperta, le righe e «Tutti e 7». A 360 dp col testo normale e grande, sulla fixture del relay.
 */
abstract class SubagentsFoldShots(scale: Float) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (800 * 2.75).toInt(), fontScale = scale), theme = "android:Theme.Material.NoActionBar")
    private val st0 = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val subs: Subagents = ContractJson.json.decodeFromJsonElement(
        Subagents.serializer(), ContractJson.json.parseToJsonElement(File("../contract/subagents-sample.json").readText()).jsonObject["workflow"]!!,
    )
    private val now = 1795436100L
    private val st = st0.copy(ops = st0.ops.orEmpty() + "subagents", sessions = st0.sessions.map { if (it.state == SessionState.BUSY) it.copy(subagents = subs, turnStarted = now - 360) else it })

    @Test fun busyPageClosed() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { HomeBoxScreen(HomeBox.BUSY, st, null, now, onBack = {}, onSession = {}, onSubagents = { null }) }
    }

    @Test fun open() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.fillMaxSize().background(Cm.bg).padding(12.dp)) {
                Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Cm.surfaceLow).padding(horizontal = 14.dp, vertical = 4.dp)) {
                    SubagentsFold(subs, now, onAll = { null }, startOpen = true)
                }
            }
        }
    }
}

class SubagentsFoldTest : SubagentsFoldShots(1f)
class SubagentsFoldLargeTextTest : SubagentsFoldShots(1.3f)
