package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.SiteAvatar
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.Favicons
import it.pixelbox.cmwatch.transport.DemoText
import it.pixelbox.cmwatch.transport.FakeTransport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * L'indicatore di attività scelto da Franz (app-indicatore-scelto-24-11, 24/11 20:28: proposta A «traccia sul bordo»): il riquadro
 * della favicon sta fermo e un tratto blu ne percorre il contorno sopra l'anello tenue. Per riga la sessione al lavoro col tratto
 * in tre punti del giro, poi una che ti aspetta (anello ambra pieno), a 40 e a 28 dp. Notte e giorno; con «riduci movimento» il
 * tratto non c'è e l'anello è pieno e fermo. Golden registrati su Linux nella CI.
 */
class IndicatoreAttivitaTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 600), theme = "android:Theme.Material.NoActionBar")
    @After fun night() { V3.day = false }

    private val demo = FakeTransport(load = { n -> DemoText.dress(File("../contract/$n.json").readText()) })
    private val st = runBlocking { demo.state.first() }
    private val png = runBlocking {
        val h = Favicons.wanted(st).first()
        val r = demo.send(Cmd("f$h", CmdOp.FAVICON, arg = h, issued = st.ts, by = "test")) {}
        Favicons.png(h, r.ok, r.text)!!
    }

    private fun shot(day: Boolean, still: Boolean) = paparazzi.snapshot {
        CmPhoneTheme(still = true, day = day) {
            val icon = androidx.compose.runtime.remember { android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size).asImageBitmap() }
            Column(Modifier.fillMaxSize().background(V3.Bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf(40.dp, 28.dp).forEach { size ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        (if (still) listOf<Float?>(null) else listOf(0f, 0.3f, 0.6f)).forEach { p -> SiteAvatar(icon, V3.Busy, busy = true, size = size, phase = p) }
                        SiteAvatar(icon, V3.Wait, busy = false, size = size)
                    }
                }
            }
        }
    }

    @Test fun notte() = shot(day = false, still = false)
    @Test fun giorno() = shot(day = true, still = false)
    @Test fun riduciMovimento() = shot(day = false, still = true)
}
