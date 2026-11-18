package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalFavicons
import it.pixelbox.cmwatch.mobile.ui.SessionBadge
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.Favicons
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.transport.DemoText
import it.pixelbox.cmwatch.transport.FakeTransport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * app-favicon-siti-18-11 (contratto 1.153, scelta B di Franz): nella home storefront e blog, che sono siti, hanno la favicon con
 * l'anello dello stato; payments-api, che non lo è, tiene il glifo di oggi. Sotto i badge delle intestazioni. Dati della demo,
 * le favicon lette con l'op `favicon` come fa l'app. Notte e giorno.
 */
class SiteAvatarTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 1500), theme = "android:Theme.Material.NoActionBar")
    @After fun night() { V3.day = false }

    private val demo = FakeTransport(load = { n -> DemoText.dress(File("../contract/$n.json").readText()) })
    private val st = runBlocking { demo.state.first() }
    private val pngs = runBlocking {
        Favicons.wanted(st).associateWith { h ->
            val r = demo.send(Cmd("f$h", CmdOp.FAVICON, arg = h, issued = st.ts, by = "test")) {}
            Favicons.png(h, r.ok, r.text)!!
        }
    }

    private fun shot(day: Boolean) = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = day) {
            // Le immagini si decodificano qui dentro: fuori dalla foto Paparazzi non ha ancora caricato la grafica nativa.
            val icons = androidx.compose.runtime.remember { pngs.mapValues { (_, b) -> android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size).asImageBitmap() } }
            CompositionLocalProvider(LocalFavicons provides icons) {
                Column(Modifier.fillMaxSize().background(V3.Bg).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryList(Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, startOpen = setOf("sessions"))
                    st.sessions.filter { s -> s.state != it.pixelbox.cmwatch.contract.SessionState.GONE }.forEach { s ->
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            SessionBadge(s, 24.dp); Text("  " + s.name, style = V3.Name)
                        }
                    }
                }
            }
        }
    }

    @Test fun siteAvatarNight() = shot(false)
    @Test fun siteAvatarDay() = shot(true)
}
