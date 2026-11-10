package it.pixelbox.cmwatch.mobile

import androidx.compose.ui.text.LinkAnnotation
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.ProvaPage
import it.pixelbox.cmwatch.mobile.ui.linked
import it.pixelbox.cmwatch.rules.ProvaLinks
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * La scheda di una prova (app-prova-link-10-11, contratto 1.134) nei suoi stati, e i link «prova #N» nel testo della chat. Golden da CI.
 */
class ProvaPageTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 1800), theme = "android:Theme.Material.NoActionBar")
    private val at = 1_789_214_700L
    private fun prova(state: String, text: String? = null) = ProvaOne(
        518, "app-scorrimento-prima-sessione-09-11", "Scorrimento della prima sessione", "Aprendo e chiudendo la prima sessione col dito: niente nero, niente titoli doppi",
        "win:supervisor-app", "supervisor-app", "0.2.2075", "telefono, home", "Apri e chiudi la prima sessione col dito, tre volte", "Il volo segue il dito, la home resta dietro", state, text, at,
    )
    private fun page(read: ProvaLinks.Read?) = paparazzi.snapshot { CmPhoneTheme(still = true) { ProvaPage(518, read, wide = false, onBack = {}, onVerify = {}) } }

    @Test fun todo() = page(ProvaLinks.Read.Found(prova("todo")))
    @Test fun ok() = page(ProvaLinks.Read.Found(prova("ok")))
    @Test fun ko() = page(ProvaLinks.Read.Found(prova("ko", "c'è ancora un passaggio spigoloso alla chiusura")))
    @Test fun red() = page(ProvaLinks.Read.Found(prova("red", "il controllo dei golden è rosso")))
    @Test fun loading() = page(null)
    @Test fun unsupported() = page(ProvaLinks.Read.Unsupported)
    @Test fun refused() = page(ProvaLinks.Read.Refused("prova 999: non esiste"))
    @Test fun todo320Font13() { paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), fontScale = 1.3f, screenHeight = 2400)); page(ProvaLinks.Read.Found(prova("todo"))) }

    // Nel testo di un messaggio ogni prova citata è un link `#/prova/<n>`; un link già scritto resta com'è.
    @Test fun theChatLinksTheProveItCites() {
        val text = "La prova #518 è passata; le prove #527 e #543 aspettano. Dettagli: https://example.com/prova/1"
        val s = linked(text)
        val urls = s.getLinkAnnotations(0, s.length).map { (it.item as LinkAnnotation.Url).url to s.substring(it.start, it.end) }
        assertEquals(
            listOf("#/prova/518" to "prova #518", "#/prova/527" to "prove #527", "#/prova/543" to "#543", "https://example.com/prova/1" to "https://example.com/prova/1").sortedBy { it.second },
            urls.sortedBy { it.second },
        )
    }
}
