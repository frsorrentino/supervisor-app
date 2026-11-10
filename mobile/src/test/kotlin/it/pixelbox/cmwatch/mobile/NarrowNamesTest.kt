package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalNameWhole
import it.pixelbox.cmwatch.mobile.ui.MenuActions
import it.pixelbox.cmwatch.mobile.ui.PageHeader
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.mobile.ui.TiAspettaPage
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.rules.TiAspetta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * Un telefono largo 320 dp col testo grande (app-ti-aspetta-titolo-320-10-11, scelta di Franz «Tutti e due»): il nome della sessione
 * si legge intero nella card della home, nella testata della sessione e nella scheda «Ti aspetta». La regola è una: il nome prima di
 * tutto, quello che gli sta accanto va sotto o cade. Prima, con un nome come quelli di Franz, la card della home restava senza nome
 * («Scegli» e «ctx» si prendevano la riga), la testata diceva «win:superviso» e «da 49 min» andava a capo a metà.
 */
internal object Narrow {
    const val NAME = "win:supervisor-app"
    private val raw = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    /** Lo stato della fixture con la sessione che aspetta chiamata come una di Franz. */
    val st = raw.copy(sessions = raw.sessions.map { s -> if (s.name == "ledger-api") s.copy(name = NAME) else s })
    fun config() = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), screenHeight = 1800, fontScale = 1.3f)
    val page = TiAspetta.Page("k", TiAspetta.Kind.DECIDE, NAME, st.ts - 2940, null, emptyList())
}

class NarrowNamesTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = Narrow.config(), theme = "android:Theme.Material.NoActionBar")
    private val whole = mutableMapOf<String, Boolean>()

    private fun shot(content: @androidx.compose.runtime.Composable () -> Unit) = paparazzi.snapshot {
        CmPhoneTheme(still = true) { CompositionLocalProvider(LocalNameWhole provides { n, w -> whole[n] = w }) { content() } }
    }

    @Test fun homeCard() {
        shot { SummaryList(Summary.build(Narrow.st, emptyList(), emptyList(), Narrow.st.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = Narrow.st.ts) }
        assertEquals("card della home: il nome tagliato ($whole)", true, whole[Narrow.NAME])
    }

    @Test fun sessionHeader() {
        shot { PageHeader(Narrow.NAME, Narrow.st.sessions, 4, Narrow.st.ts, {}, {}, MenuActions(null, "", false, {}, {}, {}, null, {})) }
        assertEquals("testata della sessione: il nome tagliato ($whole)", true, whole[Narrow.NAME])
    }

    @Test fun waitPage() {
        shot { TiAspettaPage(Narrow.page, Narrow.st.ts, wide = false, onBack = {}, onJump = {}, onOther = {}) {} }
        assertEquals("scheda Ti aspetta: il nome tagliato ($whole)", true, whole[Narrow.NAME])
    }
}

/**
 * La scheda «Ti aspetta» sul telefono lascia il posto alla barra di stato (app-ti-aspetta-titolo-320-10-11, screenshot di Franz
 * dell'08/11 21:38: «Ti aspetta» e la freccia sopra l'ora). Paparazzi disegna senza barra di stato: qui la si dà alla vista, alta
 * 24 dp, e nella fascia in alto non deve esserci niente oltre al fondo.
 */
class WaitPageStatusBarTest {
    private val frames = mutableListOf<Any>()
    @get:Rule val paparazzi = Paparazzi(deviceConfig = Narrow.config(), theme = "android:Theme.Material.NoActionBar", snapshotHandler = proxy(SnapshotHandler::class.java) { name, _ ->
        if (name == "newFrameHandler") proxy(SnapshotHandler.FrameHandler::class.java) { n, a -> if (n == "handle") frames += a!![0]!!; null } else null
    })
    private fun <T> proxy(type: Class<T>, on: (String, Array<Any?>?) -> Any?): T = type.cast(
        java.lang.reflect.Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, a ->
            when (m.name) { "hashCode" -> 0; "equals" -> false; "toString" -> "keep"; else -> on(m.name, a) }
        },
    )

    @Test fun titleBelowTheStatusBar() {
        val bar = (24 * 2.75).toInt()
        val view = ComposeView(paparazzi.context).apply { setContent {
            val v = LocalView.current
            LaunchedEffect(Unit) {
                val insets = androidx.core.view.WindowInsetsCompat.Builder()
                    .setInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars(), androidx.core.graphics.Insets.of(0, bar, 0, 0)).build()
                androidx.core.view.ViewCompat.dispatchApplyWindowInsets(v, insets)
            }
            CmPhoneTheme(still = true) { TiAspettaPage(Narrow.page, Narrow.st.ts, wide = false, onBack = {}, onJump = {}, onOther = {}) {} }
        } }
        paparazzi.gif(view, end = 300, fps = 30)
        val img = frames.last()
        val rgb = img.javaClass.getMethod("getRGB", Int::class.java, Int::class.java)
        val w = img.javaClass.getMethod("getWidth").invoke(img) as Int
        val bg = rgb.invoke(img, 2, 2) as Int
        // I fotogrammi di `gif` sono rimpiccioliti: la fascia della barra si misura sulla loro larghezza.
        val band = bar * w / (320 * 2.75).toInt()
        var lit = 0
        for (y in 0 until band) for (x in 0 until w) if (rgb.invoke(img, x, y) as Int != bg) lit++
        assertTrue("nella fascia della barra di stato ci sono $lit pixel della scheda (il titolo sotto l'ora)", lit == 0)
        // E la scheda c'è: sotto la fascia il titolo e la freccia.
        var below = 0
        for (y in band until band * 3) for (x in 0 until w) if (rgb.invoke(img, x, y) as Int != bg) below++
        assertTrue("sotto la barra di stato la testata della scheda non c'è", below > 0)
    }
}
