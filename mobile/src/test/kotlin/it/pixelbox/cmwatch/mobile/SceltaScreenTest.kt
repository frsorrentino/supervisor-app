package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.WaitAnswered
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalPageBg
import it.pixelbox.cmwatch.mobile.ui.LocalWaitCards
import it.pixelbox.cmwatch.mobile.ui.LocalWaitFiles
import it.pixelbox.cmwatch.mobile.ui.SceltaAperta
import it.pixelbox.cmwatch.mobile.ui.SceltaCompatte
import it.pixelbox.cmwatch.mobile.ui.TiAspettaPage
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.mobile.ui.WaitCardBody
import it.pixelbox.cmwatch.mobile.ui.WaitCardRead
import it.pixelbox.cmwatch.mobile.ui.WaitCardsHost
import it.pixelbox.cmwatch.mobile.ui.WaitFilesHost
import it.pixelbox.cmwatch.mobile.ui.waitRows
import it.pixelbox.cmwatch.rules.Scelta
import it.pixelbox.cmwatch.rules.TiAspetta
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * La scheda di scelta (app-scheda-scelta-12-11, contratto 1.137) sul telefono e sul tablet: aperta con le immagini delle opzioni, la
 * riga di pro e contro e i tasti, il giro scartato sotto; dall'archivio con com'è finita; chiusa in chat (scelta A di Franz, 12/11 17:46);
 * la verifica con le immagini di cosa guardare. I dati della fixture del contratto; le immagini sono quelle della demo web.
 */
class SceltaScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 3000), theme = "android:Theme.Material.NoActionBar")
    private val fake = it.pixelbox.cmwatch.transport.FakeTransport(load = { File("../contract/$it.json").readText() })
    private val open = ContractJson.decodeWait(fake.sampleResult("attesa", "wd2f87e85e2").second)
    private val archived = ContractJson.decodeWait(fake.sampleResult("attesa", "w3a91c04e7b").second)
    private val all = ContractJson.decodeWaits(fake.sampleText("attese")).items
    private val prova = all.single { it.kind == "prova" }
    private val now = open.at + 540

    // I file della fixture sul telefono: le schermate del carrello della demo web al posto dei mockup di atlas-shop.
    private val demo = File("../web/public/demo-files")
    private val shots = mapOf("home-a.png" to "cart-a.png", "home-b.png" to "cart-b.png", "round1-a.png" to "cart-round1.png", "checkout-before.png" to "checkout-before.png")
    private val files = WaitFilesHost(
        local = (open.files.orEmpty() + open.rounds.orEmpty().flatMap { it.files.orEmpty() } + prova.files.orEmpty())
            .mapNotNull { f -> shots[f.name]?.let { Scelta.fileKey(f) to File(demo, it).absolutePath } }.toMap(),
        decodeNow = true,
    )
    private fun cards(vararg items: Pair<String, WaitItem>) = WaitCardsHost(read = { id -> items.toMap()[id]?.let { WaitCardRead.Got(it) } }, ask = {}, open = {})

    @After fun night() { V3.day = false }

    @Composable private fun page(item: WaitItem, wide: Boolean, title: String? = null, sub: String? = null) =
        CompositionLocalProvider(LocalWaitFiles provides files) {
            TiAspettaPage(TiAspetta.pageOf(item, all), now, wide = wide, onBack = {}, onJump = {}, onOther = {}, stack = true, title = title, sub = sub) {
                SceltaAperta(item, waitRows(item)) { _, _ -> }
            }
        }

    @Test fun phone() = paparazzi.snapshot { CmPhoneTheme(still = true) { page(open, wide = false) } }
    @Test fun phoneDay() = paparazzi.snapshot { CmPhoneTheme(still = true, day = true) { page(open, wide = false) } }
    // Dall'archivio: «Scelta fatta», la A col bordo verde e la B spenta, nessun tasto.
    @Test fun phoneArchive() = paparazzi.snapshot { CmPhoneTheme(still = true) { page(archived, wide = false, title = "Scelta fatta", sub = "risposta oggi alle 16:55 · phone-pixel8") } }
    // Nella colonna del tablet: le opzioni sono colonne, coi tasti allineati in fondo; «Da dove viene» sotto.
    @Test fun tablet() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 1640, screenHeight = 1600, density = com.android.resources.Density.XHIGH))
        paparazzi.snapshot { CmPhoneTheme(still = true) { CompositionLocalProvider(LocalPageBg provides V3.S1) { page(open, wide = true) } } }
    }

    // In chat: la scheda chiusa da scegliere, la stessa dopo la scelta (A col bordo verde) e una verifica con la sua immagine.
    @Test fun compact() {
        val chosen = open.copy(id = "w0000000002", state = "chosen", answered = WaitAnswered(by = "phone", at = now, choice = "Tomorrow morning"), actions = emptyList())
        paparazzi.snapshot { CmPhoneTheme(still = true) {
            CompositionLocalProvider(LocalWaitFiles provides files, LocalWaitCards provides cards(open.id to open, chosen.id to chosen, prova.id to prova)) {
                Column(Modifier.fillMaxSize().background(V3.Bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SceltaCompatte(listOf(open.id, chosen.id, prova.id))
                }
            }
        } }
    }

    // La verifica aperta: le immagini di cosa guardare e il documento sotto il testo, coi suoi tasti.
    @Test fun verification() = paparazzi.snapshot { CmPhoneTheme(still = true) {
        CompositionLocalProvider(LocalWaitFiles provides files) {
            TiAspettaPage(TiAspetta.pageOf(prova, all), now, wide = false, onBack = {}, onJump = {}, onOther = {}) { WaitCardBody(prova) { _, _ -> } }
        }
    } }
}
