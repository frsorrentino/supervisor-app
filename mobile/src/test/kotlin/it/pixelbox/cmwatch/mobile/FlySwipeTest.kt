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
import it.pixelbox.cmwatch.mobile.ui.land
import it.pixelbox.cmwatch.rules.Summary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
internal object Swipe {
    val st: State = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    const val NAME = "field-notes"

    /** Il dito: dopo [wait] fotogrammi, le frazioni dello schermo che percorre, una per fotogramma, poi il rilascio. */
    class Finger(val path: List<Float>, val opening: Boolean, val release: Boolean = true, val wait: Int = 0, val letGo: Boolean = false)

    /**
     * La home e la pagina della sessione, col fondo della pagina [pageBg] (nei test che misurano, una tinta che la home non ha);
     * il dito parte dopo dieci fotogrammi fermi.
     */
    @Composable fun Stage(finger: Finger, pageBg: Color? = null, masterHome: String? = null) = Stage(listOf(finger), pageBg, masterHome)

    /** I gesti uno dopo l'altro, come Franz nel video: aprire, richiudere, riaprire. */
    @Composable fun Stage(fingers: List<Finger>, pageBg: Color? = null, masterHome: String? = null) = CmPhoneTheme(day = false) { CompositionLocalProvider(it.pixelbox.cmwatch.mobile.ui.LocalPageBg provides pageBg) {
        val cards = remember { mutableStateMapOf<String, Int>() }
        var open by remember { mutableStateOf(if (fingers.first().opening) null else NAME) }
        var opening by remember { mutableStateOf(true) }
        var flyTarget by remember { mutableStateOf(open) }
        var flyByCard by remember { mutableStateOf(true) }
        val seek = remember { SeekableTransitionState(flyTarget) }
        var pulled by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(open) {
            if (open != null && flyTarget == null) flyByCard = cards.containsKey(open)
            flyTarget = open
        }
        LaunchedEffect(flyTarget) { seek.land(flyTarget) }
        LaunchedEffect(seek) {
            snapshotFlow { pulled }.collectLatest { d -> if (d > 0f) seek.follow(d, if (opening) NAME else null) { pulled > 0f } }
        }
        val flight = rememberTransition(seek, label = "fly")
        CompositionLocalProvider(LocalCardsOnScreen provides cards) {
            SharedTransitionLayout(Modifier.fillMaxSize().background(V3.Bg)) {
                flight.AnimatedContent(transitionSpec = { it.pixelbox.cmwatch.mobile.ui.flySpec(targetState != null, flyByCard, false) }, contentKey = { it != null }) { name ->
                    CompositionLocalProvider(LocalFly provides Fly(this@SharedTransitionLayout, this@AnimatedContent, enabled = flyByCard)) {
                        val list: @Composable () -> Unit = { SummaryList(Summary.build(st, emptyList(), emptyList(), st.ts, ZoneId.of("Europe/Rome"), emptySet()), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts) }
                        // Con la master la home del telefono è la sua scheda, con la lista dentro (MainActivity, `sessionPage(master)`), col fondo suo e non con la tinta della pagina.
                        if (name == null && masterHome != null) CompositionLocalProvider(it.pixelbox.cmwatch.mobile.ui.LocalPageBg provides null) { SessionSheet(st.sessions.first { it.name == masterHome }, st.ts, emptyList(), 120, none, home = { _, _ -> list() }, header = false) }
                        else if (name == null) Column(Modifier.fillMaxSize()) { list() } else SessionSheet(st.sessions.first { it.name == name }, st.ts, emptyList(), 120, none)
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            repeat(10) { withFrameNanos {} }
            for (finger in fingers) {
                repeat(finger.wait) { withFrameNanos {} }
                opening = finger.opening
                if (finger.opening) flyByCard = cards.containsKey(NAME)
                for (x in finger.path) { pulled = x; withFrameNanos {} }
                if (!finger.release) return@LaunchedEffect
                pulled = 0f
                // Lasciato prima del terzo il volo torna da dove era partito; se no va a destinazione da qui, come in MainActivity.
                if (finger.letGo) launch { seek.land(seek.currentState) }
                else { open = if (finger.opening) NAME else null; flyTarget = open; launch { seek.land(open) } }
            }
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
    private fun shares(finger: Swipe.Finger): Pair<List<Float>, List<Float>> = shares(listOf(finger)).let { (p, c, _) -> p to c }

    /** Come sopra per più gesti di fila, con in più la parte trasparente: la pagina sopra la home, né l'una né l'altra. */
    private fun shares(fingers: List<Swipe.Finger>, masterHome: String? = null): Triple<List<Float>, List<Float>, List<Float>> {
        lit.clear()
        frames.clear()
        val view = ComposeView(paparazzi.context).apply { setContent { Swipe.Stage(fingers, pageBg, masterHome) } }
        paparazzi.gif(view, end = 1400, fps = 60)
        val page = pageBg.toArgb()
        val card = V3.S2.toArgb()
        val out = frames.drop(9).map { img ->
            val rgb = img.javaClass.getMethod("getRGB", Int::class.java, Int::class.java)
            val w = img.javaClass.getMethod("getWidth").invoke(img) as Int
            val h = img.javaClass.getMethod("getHeight").invoke(img) as Int
            var p = 0; var c = 0; var t = 0; var l = 0; var all = 0
            for (y in 0 until h step 4) for (x in 0 until w step 4) {
                all++
                val px = rgb.invoke(img, x, y) as Int
                when (px) { page -> p++; card -> c++ }
                if (between(px)) t++
                if (px != page && luma(px) > 60) l++
            }
            lit += l.toFloat() / all
            Triple(p.toFloat() / all, c.toFloat() / all, t.toFloat() / all)
        }
        return Triple(out.map { it.first }, out.map { it.second }, out.map { it.third })
    }

    /** Per ogni fotogramma dell'ultima misura, la parte accesa fuori dalla pagina: testi, bordi e tasti della home che si vede. */
    private val lit = mutableListOf<Float>()
    private fun luma(px: Int) = (((px shr 16) and 0xFF) * 3 + ((px shr 8) and 0xFF) * 6 + (px and 0xFF)) / 10

    /** Un colore fra il fondo scuro della home e la tinta della pagina, verde e blu a metà strada: la pagina in dissolvenza. */
    private fun between(px: Int): Boolean {
        val r = (px shr 16) and 0xFF; val g = (px shr 8) and 0xFF; val b = px and 0xFF
        val p = pageBg.toArgb()
        return r <= ((p shr 16) and 0xFF) && g in 0x10 until ((p shr 8) and 0xFF) - 2 && b in 0x14 until (p and 0xFF) - 3 && b - r > 6
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

    // Il ritorno col dito verso destra, a specchio dell'apertura (Franz, 09/11 21:37): la pagina si richiude nel riquadro che torna
    // card, con la home dietro. Prima la pagina spariva al primo passo e la card volava da sola dalla cima della home, un lampo.
    // Scoperta, la home non sbiadisce sotto un fondo che svanisce (la home «in dissolvenza» del primo video, 2,20-2,60 s).
    @Test fun closingFlickKeepsTheHomeBehind() {
        val (s, cards) = shares(Swipe.Finger(listOf(0.02f, 0.35f), opening = false))
        assertTrue("chiusura: al primo passo la pagina sparisce invece di richiudersi (quote $s)", s[1] > 0.3f)
        assertTrue("chiusura: la pagina resta (${s.last()})", s.last() < 0.05f)
        val top = cards.indexOf(cards.max())
        assertTrue("chiusura: la home in dissolvenza dopo il volo (card ${cards})", cards.drop(top).min() > cards.max() * 0.7f)
    }

    // La chiusura con la master che aspetta Franz (video di Franz del 10/11 09:37, 0.2.2074): la home del telefono è la scheda della
    // master, con la chiave di volo della card della master che sta nella sua lista. Col dito che richiude, la home volava intera
    // verso quella card: dietro la pagina restava solo la sua cima nel riquadro della card, il resto nero, e al rilascio la home
    // compariva d'un colpo. Succede dopo un'apertura, come nel video: aperta col dito e richiusa a metà, la home dietro si vede intera.
    // Un gesto per test: un secondo `gif` nello stesso test non dà un filmato nuovo.
    @Test fun closingWithTheMasterHomeKeepsTheHomeBehind() {
        val path = listOf(0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f, 0.35f, 0.4f)
        val gestures = listOf(Swipe.Finger(path, opening = true), Swipe.Finger(path + List(10) { 0.4f }, opening = false, release = false, wait = 40))
        shares(gestures, masterHome = "ledger-api"); val master = lit.takeLast(8)
        // Accesa: 0,025 con la home intera dietro, 0,006 con la sola cima nel riquadro della card; la home senza master fa 0,024.
        assertTrue("con la master la home dietro la pagina sparisce (accesa $master)", master.min() > 0.015f)
    }

    // Il primo gesto dopo un ritorno (secondo video di Franz, 09/11 18:18, 4,65-5,01 s): richiusa la sessione col dito, la riapre
    // un terzo di secondo dopo, col volo del ritorno ancora in corso. La card non sparisce dal suo posto e la pagina non entra
    // trasparente sopra la home: si scopre piena nella scheda che cresce, come al primo gesto.
    @Test fun reopeningRightAfterAReturnUncoversAFullPage() {
        val path = listOf(0.03f, 0.06f, 0.1f, 0.15f, 0.2f, 0.25f)
        val (s, _, faded) = shares(listOf(Swipe.Finger(path, opening = false), Swipe.Finger(path, opening = true, wait = 17)))
        assertTrue("riapertura: la pagina entra trasparente sopra la home (al massimo ${faded.max()}, quote $faded)", faded.max() < 0.05f)
        assertTrue("riapertura: la pagina non arriva intera (${s.last()})", s.last() > 0.6f)
    }

    // Il dito che apre la sessione e la lascia prima del terzo (registrazione nell'emulatore del 09/11 21:07): il volo torna alla
    // card abbassando la frazione. `animateTo` verso la home scambiava i due stati e la pagina compariva piena per un fotogramma.
    @Test fun openingLetGoEarlyGoesBackWithoutAFlash() {
        val (s, _, _) = shares(listOf(Swipe.Finger(listOf(0.03f, 0.06f, 0.1f), opening = true, letGo = true)))
        assertTrue("dito lasciato: la pagina compare piena (quote $s)", s.max() < 0.5f)
        assertTrue("dito lasciato: la pagina resta (${s.last()})", s.last() < 0.02f)
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

    private fun at(name: String, opening: Boolean) = at(name, end = 400, frame = 18) { Swipe.Stage(Swipe.Finger(listOf(0.12f), opening, release = false)) }

    private fun at(name: String, end: Long, frame: Int, stage: @Composable () -> Unit) {
        val view = ComposeView(paparazzi.context).apply { setContent { stage() } }
        paparazzi.gif(view, end = end, fps = 60)
        val shot = frames[frame]
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

    // La chiusura a metà con la home della master (video di Franz del 10/11 09:37): aperta col dito e richiusa fino a quattro decimi,
    // la home intera dietro la pagina che torna card, non la sua cima nel riquadro della card della master.
    @Test fun closingHalfwayMasterHome() = at("closingHalfwayMasterHome", end = 1400, frame = 72) {
        val path = listOf(0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f, 0.35f, 0.4f)
        Swipe.Stage(listOf(Swipe.Finger(path, opening = true), Swipe.Finger(path + List(10) { 0.4f }, opening = false, release = false, wait = 40)), null, "ledger-api")
    }

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
