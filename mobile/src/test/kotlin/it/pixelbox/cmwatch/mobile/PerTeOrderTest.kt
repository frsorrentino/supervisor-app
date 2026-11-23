package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.WaitAction
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.mobile.ui.Cm
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.PerTeGroupsBody
import it.pixelbox.cmwatch.rules.PerTe
import org.junit.Rule
import org.junit.Test

/**
 * app-per-te-ordine-23-11 (Franz 23/11 19:00 e 19:01; contratto 1.160), sulla segnalazione delle 19:01: in alto, aperte, le carte che
 * bloccano un lavoro, ognuna con la sua sessione; sotto albo-crotone con le cinque pause di Albo Chiaro, chiuso, col numero e da
 * quanto, e la freccia. A 360 dp col testo normale e grande.
 */
abstract class PerTeOrderShots(scale: Float) {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt(), screenHeight = (800 * 2.75).toInt(), fontScale = scale), theme = "android:Theme.Material.NoActionBar")
    private val now = 1795436100L
    private val day = 86_400L
    private fun w(id: String, session: String, title: String, text: String, at: Long, kind: String, label: String, main: String, blocking: Boolean? = null) = WaitItem(
        id, type = "carta", kind = kind, label = label, labelText = "", session = session, task = id, text = text, at = at, state = "open", title = title, blocking = blocking,
        actions = listOf(WaitAction(main, style = "primary", cmd = kotlinx.serialization.json.JsonObject(emptyMap()))),
    )
    private val waits = listOf(
        w("s1", "storefront", "Carrello sul telefono", "Dove stanno il totale e il tasto Paga", now - 540, "choice", "choose", "Scegli", true),
        w("s2", "atlas-shop", "Totali del carrello", "Apri gli esiti di oggi", now - 3600, "prova", "verify", "Va", true),
    ) + listOf("Albo Chiaro fase 3", "Albo Chiaro fase 2", "Albo Chiaro, Regione", "Albo Chiaro fase 1", "Albo Chiaro, Provincia").mapIndexed { i, t ->
        w("a$i", "albo-crotone", t, "Albo Chiaro sospeso dal 18/10", now - 36 * day + i, "pausa", "decide", "Rispondi")
    }

    @Test fun ordered() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.fillMaxSize().background(Cm.bg).padding(12.dp)) {
                PerTeGroupsBody(PerTe.groupsOf(waits, emptyList()), now, emptySet(), null, onSession = {}, onOpen = {}, onMain = {})
            }
        }
    }
}

class PerTeOrderTest : PerTeOrderShots(1f)
class PerTeOrderLargeTextTest : PerTeOrderShots(1.3f)
