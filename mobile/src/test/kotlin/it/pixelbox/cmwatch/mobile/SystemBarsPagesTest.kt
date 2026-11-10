package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CinqueMinutiScreen
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.MonitorPage
import it.pixelbox.cmwatch.mobile.ui.TiAspettaPage
import it.pixelbox.cmwatch.rules.TiAspetta
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Le pagine a tutto schermo del telefono lasciano lo spazio delle barre di sistema, sopra e sotto (app-monitor-scheda-barre-10-11, Franz
 * 10/11 20:08: nella scheda di un monitoraggio il titolo stava sotto l'ora e «Ferma» e «Chiedi alla master» sotto la linea dei gesti;
 * prima ancora «Ti aspetta», app-ti-aspetta-titolo-320-10-11). Paparazzi disegna senza barre: qui la vista riceve una barra di stato e una
 * barra dei gesti da 24 dp, e nelle due fasce non deve esserci niente oltre al fondo. Il fotogramma è anche un golden, registrato e
 * confrontato come quelli del volo (FlySwipeShotsTest).
 */
class SystemBarsPagesTest {
    private val frames = mutableListOf<Any>()
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 2000), theme = "android:Theme.Material.NoActionBar", snapshotHandler = proxy(SnapshotHandler::class.java) { name, _ ->
        if (name == "newFrameHandler") proxy(SnapshotHandler.FrameHandler::class.java) { n, a -> if (n == "handle") frames += a!![0]!!; null } else null
    })
    private fun <T> proxy(type: Class<T>, on: (String, Array<Any?>?) -> Any?): T = type.cast(
        java.lang.reflect.Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, a ->
            when (m.name) { "hashCode" -> 0; "equals" -> false; "toString" -> "keep"; else -> on(m.name, a) }
        },
    )
    private val monitors = ContractJson.decodeMonitors(File("../core/src/test/resources/monitors-mockup.json").readText()).items
    private val fake = it.pixelbox.cmwatch.transport.FakeTransport(load = { File("../contract/$it.json").readText() })

    /** La pagina con le due barre; poi le fasce in alto e in basso, che devono avere solo il fondo, e il golden. */
    private fun check(name: String, page: @Composable () -> Unit) {
        val bar = (24 * 2.75).toInt()
        val view = ComposeView(paparazzi.context).apply { setContent {
            val v = LocalView.current
            LaunchedEffect(Unit) {
                androidx.core.view.ViewCompat.dispatchApplyWindowInsets(v, androidx.core.view.WindowInsetsCompat.Builder()
                    .setInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars(), androidx.core.graphics.Insets.of(0, bar, 0, 0))
                    .setInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars(), androidx.core.graphics.Insets.of(0, 0, 0, bar)).build())
            }
            CmPhoneTheme(still = true) { page() }
        } }
        paparazzi.gif(view, end = 300, fps = 30)
        val img = frames.last()
        val c = img.javaClass
        val rgb = c.getMethod("getRGB", Int::class.java, Int::class.java)
        val w = c.getMethod("getWidth").invoke(img) as Int
        val h = c.getMethod("getHeight").invoke(img) as Int
        // I fotogrammi di `gif` sono rimpiccioliti: le fasce si misurano sulla loro larghezza.
        val band = bar * w / (DeviceConfig.PIXEL_5.screenWidth)
        val bg = rgb.invoke(img, 2, 2) as Int
        fun lit(ys: IntRange) = ys.sumOf { y -> (0 until w).count { x -> rgb.invoke(img, x, y) as Int != bg } }
        val top = lit(0 until band)
        val bottom = lit(h - band until h)
        assertTrue("$name: nella fascia della barra di stato ci sono $top pixel della pagina", top == 0)
        assertTrue("$name: nella fascia della barra dei gesti ci sono $bottom pixel della pagina", bottom == 0)
        assertTrue("$name: sotto la barra di stato la testata non c'è", lit(band until band * 3) > 0)
        golden(name, img)
    }

    private fun golden(name: String, shot: Any) {
        val io = Class.forName("javax.imageio.ImageIO")
        val ri = Class.forName("java.awt.image.RenderedImage")
        val file = File(System.getProperty("paparazzi.snapshot.dir") ?: return, "images/it.pixelbox.cmwatch.mobile_SystemBarsPagesTest_$name.png")
        when {
            System.getProperty("paparazzi.test.record") == "true" -> { file.parentFile.mkdirs(); io.getMethod("write", ri, String::class.java, File::class.java).invoke(null, shot, "png", file) }
            System.getProperty("paparazzi.test.verify") == "true" -> {
                assertTrue("$name: manca il golden ${file.name}", file.exists())
                val want = io.getMethod("read", File::class.java).invoke(null, file)!!
                val a = shot.javaClass; val b = want.javaClass
                val w = a.getMethod("getWidth").invoke(shot) as Int; val h = a.getMethod("getHeight").invoke(shot) as Int
                val same = w == b.getMethod("getWidth").invoke(want) as Int && h == b.getMethod("getHeight").invoke(want) as Int
                val ra = a.getMethod("getRGB", Int::class.java, Int::class.java); val rb = b.getMethod("getRGB", Int::class.java, Int::class.java)
                var n = 0
                if (same) for (y in 0 until h) for (x in 0 until w) if (ra.invoke(shot, x, y) != rb.invoke(want, x, y)) n++
                assertTrue("$name: diverso dal golden", same && n.toFloat() / (w * h) < 0.002f)
            }
        }
    }

    @Test fun monitorPage() = check("monitorPage") { MonitorPage(monitors.first(), wide = false, onBack = {}, onStop = {}, onAsk = {}, onEditAlerts = {}) }
    @Test fun waitPage() = check("waitPage") {
        TiAspettaPage(TiAspetta.Page("k", TiAspetta.Kind.DECIDE, "storefront", 0, null), 600, wide = false, onBack = {}, onJump = {}, onOther = {}) {}
    }
    // Decidi (e con lui Verifica e Prima raccolta, che passano da DecidePage): già a posto, resta di guardia.
    @Test fun decidePage() = check("decidePage") {
        CinqueMinutiScreen(ContractJson.decodeDecisions(fake.sampleText("decisioni")), onBack = {}, onApprove = { _, _ -> }, onDone = {}, onNotNow = {}, onDrop = { _, _ -> })
    }
}
