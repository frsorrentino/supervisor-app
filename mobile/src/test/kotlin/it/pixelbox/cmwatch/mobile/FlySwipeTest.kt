package it.pixelbox.cmwatch.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.Fly
import it.pixelbox.cmwatch.mobile.ui.LocalCardsOnScreen
import it.pixelbox.cmwatch.mobile.ui.LocalFly
import it.pixelbox.cmwatch.mobile.ui.SessionSheet
import it.pixelbox.cmwatch.mobile.ui.SheetActions
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.mobile.ui.V3
import it.pixelbox.cmwatch.mobile.ui.follow
import it.pixelbox.cmwatch.rules.Summary
import kotlinx.coroutines.flow.collectLatest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * Il dito che apre la prima sessione dalla home e la richiude (app-scorrimento-prima-sessione-09-11, video di Franz del 09/11
 * 15:42): il volo della card come nel telefono, coi bordi condivisi su una transizione cercata dal dito, la card vera della home
 * e la pagina vera della sessione. Il dito e il rilascio fanno quello che fanno in MainActivity: la frazione segue il dito con
 * `follow`, al rilascio `open` cambia e il volo va a destinazione da dove è arrivato.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private object Swipe {
    val st: State = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    const val NAME = "field-notes"

    /** Il dito: le frazioni dello schermo che percorre, una per fotogramma, poi il rilascio. */
    class Finger(val path: List<Float>, val opening: Boolean, val release: Boolean = true)

    /**
     * La home e la pagina della sessione, col fondo della pagina [pageBg] (nei test che misurano, una tinta che la home non ha);
     * il dito parte dopo dieci fotogrammi fermi.
     */
    @Composable fun Stage(finger: Finger, pageBg: Color? = null) = CmPhoneTheme(day = false) { CompositionLocalProvider(it.pixelbox.cmwatch.mobile.ui.LocalPageBg provides pageBg) {
        val cards = remember { mutableStateMapOf<String, Int>() }
        var open by remember { mutableStateOf(if (finger.opening) null else NAME) }
        var flyTarget by remember { mutableStateOf(open) }
        var flyByCard by remember { mutableStateOf(true) }
        val seek = remember { SeekableTransitionState(flyTarget) }
        var pulled by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(open) {
            if (open != null && flyTarget == null) flyByCard = cards.containsKey(open)
            flyTarget = open
        }
        LaunchedEffect(flyTarget) { seek.animateTo(flyTarget, if (seek.fraction == 0f) tween(it.pixelbox.cmwatch.mobile.ui.FLY_MS, easing = LinearEasing) else null) }
        LaunchedEffect(seek) {
            snapshotFlow { pulled }.collectLatest { d -> if (d > 0f) seek.follow(d, if (finger.opening) NAME else null) { pulled > 0f } }
        }
        val flight = rememberTransition(seek, label = "fly")
        CompositionLocalProvider(LocalCardsOnScreen provides cards) {
            SharedTransitionLayout(Modifier.fillMaxSize().background(V3.Bg)) {
                flight.AnimatedContent(transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }, contentKey = { it != null }) { name ->
                    CompositionLocalProvider(LocalFly provides Fly(this@SharedTransitionLayout, this@AnimatedContent, enabled = flyByCard)) {
                        if (name == null) Column(Modifier.fillMaxSize()) {
                            SummaryList(Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts)
                        } else SessionSheet(st.sessions.first { it.name == name }, st.ts, emptyList(), 120, none)
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            repeat(10) { withFrameNanos {} }
            if (finger.opening) flyByCard = cards.containsKey(NAME)
            for (x in finger.path) { pulled = x; withFrameNanos {} }
            if (!finger.release) return@LaunchedEffect
            pulled = 0f
            open = if (finger.opening) NAME else null
        }
    } }
}

class FlySwipeTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar", snapshotHandler = keep())

    private val pageBg = Color(0xFF0E2A3F)

    /**
     * Per ogni fotogramma, dopo i dieci fermi, la parte dello schermo con la tinta della pagina (la pagina che si scopre nella
     * scheda che vola: una tinta che la home non ha) e quella col fondo delle card della home (quanto della home si vede).
     */
    private fun shares(finger: Swipe.Finger): Pair<List<Float>, List<Float>> {
        frames.clear()
        val view = ComposeView(paparazzi.context).apply { setContent { Swipe.Stage(finger, pageBg) } }
        paparazzi.gif(view, end = 1400, fps = 60)
        val page = pageBg.toArgb()
        val card = V3.S2.toArgb()
        val out = frames.drop(9).map { img ->
            val rgb = img.javaClass.getMethod("getRGB", Int::class.java, Int::class.java)
            val w = img.javaClass.getMethod("getWidth").invoke(img) as Int
            val h = img.javaClass.getMethod("getHeight").invoke(img) as Int
            var p = 0; var c = 0; var all = 0
            for (y in 0 until h step 4) for (x in 0 until w step 4) {
                all++
                when (rgb.invoke(img, x, y) as Int) { page -> p++; card -> c++ }
            }
            p.toFloat() / all to c.toFloat() / all
        }
        return out.map { it.first } to out.map { it.second }
    }

    private fun steps(s: List<Float>) = s.zipWithNext { a, b -> b - a }

    // Lo slancio del video (0,90-1,15 s): il dito corre a un terzo dello schermo in due fotogrammi e lascia. La pagina cresce a
    // passi senza sparire per un fotogramma, e la home resta dietro: nessun fondo nero copre le sue card d'un colpo.
    @Test fun openingFlickUncoversThePageOverTheHome() {
        val (s, cards) = shares(Swipe.Finger(listOf(0.02f, 0.35f), opening = true))
        val d = steps(s)
        assertTrue("apertura: la pagina compare piena d'un colpo (salto massimo ${d.max()}, quote $s)", d.max() < 0.25f)
        assertTrue("apertura: la scheda sparisce per un fotogramma (calo massimo ${d.min()}, quote $s)", d.min() > -0.05f)
        assertTrue("apertura: la pagina non arriva intera (${s.last()})", s.last() > 0.6f)
        assertTrue("apertura: la home sparisce d'un colpo dietro la scheda (card ${cards})", steps(cards).min() > -cards.first() / 2)
    }

    // Il ritorno col dito verso destra, lo specchio: la card torna al suo posto sopra la home, che c'è tutta dal primo fotogramma
    // del gesto, senza un fondo che la copre o che svanisce sopra di lei (la home «in dissolvenza» del video, 2,20-2,60 s).
    @Test fun closingFlickKeepsTheHomeBehind() {
        val (s, cards) = shares(Swipe.Finger(listOf(0.02f, 0.35f), opening = false))
        assertTrue("chiusura: la pagina resta (${s.last()})", s.last() < 0.05f)
        val behind = cards.drop(1)
        assertTrue("chiusura: la home coperta o in dissolvenza durante il volo (card ${cards})", behind.min() > cards.last() * 0.7f)
    }

    // L'immagine dell'ultimo fotogramma di ogni passo: `BufferedImage` il classpath dei test Android non lo vede, si legge per riflessione.
    private val frames = mutableListOf<Any>()
    private fun keep(): SnapshotHandler = proxy(SnapshotHandler::class.java) { name, _ ->
        if (name == "newFrameHandler") proxy(SnapshotHandler.FrameHandler::class.java) { n, a -> if (n == "handle") frames += a!![0]!!; null } else null
    }
    private fun <T> proxy(type: Class<T>, on: (String, Array<Any?>?) -> Any?): T = type.cast(
        java.lang.reflect.Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, a ->
            when (m.name) { "hashCode" -> 0; "equals" -> false; "toString" -> "keep"; else -> on(m.name, a) }
        },
    )
}

/**
 * A metà gesto, col dito fermo: la home dietro e la pagina che si scopre dalla scheda in apertura; in chiusura la card che torna
 * al suo posto sopra la home. Golden da CI, come gli altri. `paparazzi.snapshot` disegna un fotogramma solo e il dito ne vuole
 * parecchi, così il fotogramma si prende dal filmato: registrato come golden con `recordPaparazzi`, confrontato con
 * `verifyPaparazzi`, ignorato dai test semplici.
 */
class FlySwipeShotsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar", snapshotHandler = keep())

    private fun at(name: String, opening: Boolean) {
        val view = ComposeView(paparazzi.context).apply { setContent { Swipe.Stage(Swipe.Finger(listOf(0.12f), opening, release = false)) } }
        paparazzi.gif(view, end = 400, fps = 60)
        val shot = frames[18]
        val io = Class.forName("javax.imageio.ImageIO")
        val ri = Class.forName("java.awt.image.RenderedImage")
        val golden = File(System.getProperty("paparazzi.snapshot.dir") ?: return, "images/it.pixelbox.cmwatch.mobile_FlySwipeShotsTest_$name.png")
        when {
            System.getProperty("paparazzi.test.record") == "true" -> {
                golden.parentFile.mkdirs()
                io.getMethod("write", ri, String::class.java, File::class.java).invoke(null, shot, "png", golden)
            }
            System.getProperty("paparazzi.test.verify") == "true" -> {
                assertTrue("$name: manca il golden ${golden.name}", golden.exists())
                val want = io.getMethod("read", File::class.java).invoke(null, golden)!!
                val off = differing(shot, want)
                assertTrue("$name: diverso dal golden in $off dei pixel", off < 0.002f)
            }
        }
    }

    private fun differing(a: Any, b: Any): Float {
        val c = a.javaClass
        val w = c.getMethod("getWidth").invoke(a) as Int
        val h = c.getMethod("getHeight").invoke(a) as Int
        if (w != b.javaClass.getMethod("getWidth").invoke(b) as Int || h != b.javaClass.getMethod("getHeight").invoke(b) as Int) return 1f
        val ra = c.getMethod("getRGB", Int::class.java, Int::class.java)
        val rb = b.javaClass.getMethod("getRGB", Int::class.java, Int::class.java)
        var n = 0
        for (y in 0 until h) for (x in 0 until w) if (ra.invoke(a, x, y) != rb.invoke(b, x, y)) n++
        return n.toFloat() / (w * h)
    }

    @Test fun openingHalfway() = at("openingHalfway", opening = true)
    @Test fun closingHalfway() = at("closingHalfway", opening = false)

    private val frames = mutableListOf<Any>()
    private fun keep(): SnapshotHandler = proxy(SnapshotHandler::class.java) { name, _ ->
        if (name == "newFrameHandler") proxy(SnapshotHandler.FrameHandler::class.java) { n, a -> if (n == "handle") frames += a!![0]!!; null } else null
    }
    private fun <T> proxy(type: Class<T>, on: (String, Array<Any?>?) -> Any?): T = type.cast(
        java.lang.reflect.Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, a ->
            when (m.name) { "hashCode" -> 0; "equals" -> false; "toString" -> "keep"; else -> on(m.name, a) }
        },
    )
}
