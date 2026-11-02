package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.transport.FakeTransport
import org.junit.Rule
import org.junit.Test

/** Contratto 1.102, la pagina sui dati della fixture: chi è indietro e chi non si può sapere, il giro parziale e la card, i promemoria. */
class SeguitiScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val page = ContractJson.decodeSeguiti(FakeTransport({ java.io.File("../contract/$it.json").readText() }).sampleText("seguiti"))
    private val item = page.rimasto.single()
    private val run = page.altrove.single()
    private val rimasto = page.copy(rimasto = listOf(item, item.copy(key = "readme", title = "README", stale = null, why = "no_git", alignedVersion = null, alignedAt = null), item.copy(key = "privacy", title = "Privacy", stale = false, why = null), item.copy(key = "poster", title = "Poster", why = "check", task = "allinea-poster-02-11")), altrove = emptyList(), promemoria = emptyList(), promemoriaScattati = emptyList())
    private val altrove = page.copy(rimasto = emptyList(), altrove = listOf(run.copy(task = "altrove-a81c55e20b"), run.copy(id = "b", text = "Debug log left on", complete = false, done = 5, positive = emptyList(), errors = listOf("personal/orbit-docs"), running = true), run.copy(id = "c", text = "Old key in the config", positive = emptyList())), promemoria = emptyList(), promemoriaScattati = emptyList())
    private val promemoria = page.copy(rimasto = emptyList(), altrove = emptyList(), promemoria = page.promemoria + page.promemoria.single().copy(id = "2", text = "Renew the domain", atTime = 1789473600, expiresAt = 1790000000))
    private fun shot(p: SeguitiPage, dp: Int = 412, scale: Float = 1f) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), fontScale = scale))
        paparazzi.snapshot { CmPhoneTheme(still = true) { SeguitiScreen(p, false, {}, { _, _, _ -> }, {}, {}) } }
    }
    @Test fun rimasto_indietro412() = shot(rimasto)
    @Test fun rimasto_indietro320_grande() = shot(rimasto, 320, 1.3f)
    @Test fun stesso_difetto412() = shot(altrove)
    @Test fun promemoria412() = shot(promemoria)
    @Test fun seguiti_vuota() = shot(SeguitiPage())
}
