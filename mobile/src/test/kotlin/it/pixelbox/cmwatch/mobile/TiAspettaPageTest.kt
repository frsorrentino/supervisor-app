package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalPageBg
import it.pixelbox.cmwatch.mobile.ui.SessionDecisions
import it.pixelbox.cmwatch.mobile.ui.TiAspettaPage
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.rules.TiAspetta
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * La scheda «Ti aspetta» come nel mockup approvato (app-scheda-ti-aspetta-07-11): la carta in alto, «Da dove viene» con la frase
 * della domanda accesa e il salto, le altre che aspettano. I dati del contratto non ci sono ancora: la pagina e una decisione della
 * fixture, solo dati demo.
 */
class TiAspettaPageTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 2400), theme = "android:Theme.Material.NoActionBar")
    private val fake = it.pixelbox.cmwatch.transport.FakeTransport(load = { File("../contract/$it.json").readText() })
    private val dec = ContractJson.decodeDecisions(fake.sampleText("decisioni"))
    private val now = 1_800_000_000L
    private val page = TiAspetta.Page(
        "storefront-release-2-4", TiAspetta.Kind.DECIDE, "storefront", now - 1500,
        TiAspetta.Source(
            "storefront", now - 1560,
            "Staging è verde: rimborsi, webhook e checkout passano. Il carrello tiene anche con dieci articoli.\n\nPubblico la 2.4 in produzione solo con il tuo ok: l'unica cosa che non ho provato è il pagamento da un telefono vero.",
            "Pubblico la 2.4 in produzione solo con il tuo ok", "e-42",
        ),
        listOf(
            TiAspetta.Other("branch", TiAspetta.Kind.CHOOSE, "Scegli il nome del branch", "payments-api", now - 720),
            TiAspetta.Other("carrello", TiAspetta.Kind.VERIFY, "Verifica i totali del carrello", "storefront", now - 3 * 3600),
        ),
    )
    private val card = @androidx.compose.runtime.Composable { SessionDecisions(listOf(dec.groups.first()), { _, _ -> }, {}, {}, { _, _ -> }, {}) }

    @After fun night() { V3.day = false }

    @Test fun phone() = paparazzi.snapshot { CmPhoneTheme(still = true) { TiAspettaPage(page, now, wide = false, onBack = {}, onJump = {}, onOther = {}, card = card) } }
    @Test fun phoneDay() = paparazzi.snapshot { CmPhoneTheme(still = true, day = true) { TiAspettaPage(page, now, wide = false, onBack = {}, onJump = {}, onOther = {}, card = card) } }
    // Nella colonna del tablet: la carta e «Da dove viene» affiancate, sul grigio delle schede.
    @Test fun column() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 1640, screenHeight = 1000, density = com.android.resources.Density.XHIGH))
        paparazzi.snapshot { CmPhoneTheme(still = true) { androidx.compose.runtime.CompositionLocalProvider(LocalPageBg provides V3.S1) { TiAspettaPage(page, now, wide = true, onBack = {}, onJump = {}, onOther = {}, card = card) } } }
    }
}
