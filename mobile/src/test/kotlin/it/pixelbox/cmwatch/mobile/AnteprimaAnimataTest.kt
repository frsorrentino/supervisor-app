package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.WaitFile
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalWaitFiles
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.mobile.ui.WaitFilesBlock
import it.pixelbox.cmwatch.mobile.ui.WaitFilesHost
import it.pixelbox.cmwatch.mobile.ui.movesFile
import it.pixelbox.cmwatch.rules.Scelta
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * app-anteprime-webp-27-11: i file da guardare di una carta con le animazioni ridotte. Il WebP animato dell'entrata della home resta al
 * primo fotogramma col ▶ sopra; la schermata PNG accanto è ferma e non ha il ▶; sotto la bozza HTML con lo stesso nome del WebP (il tocco
 * sul WebP la apre dal vivo). I file sono quelli della demo web.
 */
class AnteprimaAnimataTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val demo = File("../web/public/demo-files")
    private fun file(name: String, mime: String, size: Long, kind: String) =
        WaitFile(path = "/home/demo/workspaces/work/storefront/docs/mockups/$name", name = name, mime = mime, size = size, kind = kind, session = null)
    private val webp = file("entrata-home.webp", "image/webp", 128_816, "image")
    private val png = file("checkout-before.png", "image/png", 35_449, "image")
    private val html = file("entrata-home.html", "text/html", 2_744, "document")
    private val host = WaitFilesHost(
        local = mapOf(Scelta.fileKey(webp) to File(demo, "entrata-home.webp").absolutePath, Scelta.fileKey(png) to File(demo, "checkout-before.png").absolutePath),
        decodeNow = true,
    )

    @Test fun ridotteIlPrimoFotogrammaColPlay() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            CompositionLocalProvider(LocalWaitFiles provides host) {
                Box(Modifier.fillMaxSize().background(V3.Bg).padding(16.dp)) { WaitFilesBlock(listOf(webp, png, html)) }
            }
        }
    }

    @Test fun `si muove il WebP animato, non la schermata PNG`() {
        assertTrue(movesFile(File(demo, "entrata-home.webp").path))
        assertFalse(movesFile(File(demo, "checkout-before.png").path))
        assertFalse(movesFile(File(demo, "non-c-e.webp").path))
    }
}
