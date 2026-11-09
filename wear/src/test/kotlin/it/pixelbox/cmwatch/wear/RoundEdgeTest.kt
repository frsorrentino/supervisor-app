package it.pixelbox.cmwatch.wear

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import com.android.resources.Density
import com.android.resources.ScreenRound
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.wear.ui.screens.LaunchScreen
import it.pixelbox.cmwatch.wear.ui.screens.PairingScreen
import it.pixelbox.cmwatch.wear.ui.screens.PairingStatus
import it.pixelbox.cmwatch.wear.ui.screens.QuestionScreen
import it.pixelbox.cmwatch.wear.ui.screens.QuotaScreen
import it.pixelbox.cmwatch.wear.ui.screens.SessionScreen
import it.pixelbox.cmwatch.wear.ui.screens.SessionsScreen
import it.pixelbox.cmwatch.wear.ui.screens.SettingsScreen
import it.pixelbox.cmwatch.wear.ui.theme.CmTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * app-wear-play-rifiuto-09-11: Google Play ha rifiutato l'app il 03/11 per la regola di qualità Wear «nessun testo o controllo
 * tagliato dai bordi dello schermo». Le prove di Google sono liste ferme in fondo: Abbinamento con «Jn your phone» tagliato a
 * sinistra e «Try the demo» tagliato in basso, Impostazioni con «Demo mode» tagliato in basso.
 *
 * Ogni schermata si disegna ferma in cima (lo stato con cui si apre) e ferma in fondo, su un orologio rotondo piccolo e grande, col
 * testo normale e grande, e si guarda la fascia di 4 px lungo il bordo del cerchio:
 * - in fondo, nella metà bassa, nessun pixel diverso dal fondo: l'ultima voce sta tutta dentro il cerchio, anche il fondo scuro
 *   di un tasto tonale;
 * - in cima e in fondo, nella metà alta, nessun pixel chiaro: niente testo o tasto ancora leggibile tagliato dal bordo. Le voci
 *   che escono dall'alto sfumano (rememberRoundSpec), e mezze trasparenti sul bordo sono il disegno delle liste Wear.
 * In cima la metà bassa non si guarda: lì la lista continua ed entra dal bordo, ed è il segno che scorre. A destra, a metà altezza,
 * l'indicatore di scorrimento sta sul bordo per disegno.
 */
class RoundEdgeTest {
    private var last: Any? = null
    private val now = 1789210800L
    private val state = ContractJson.decodeState(java.io.File("../contract/state-1-question.json").readText())
    private val snap = Snapshot(state, Freshness.Fresh)
    @get:Rule val paparazzi = Paparazzi(deviceConfig = round(456, 1f), theme = "android:Theme.DeviceDefault.NoActionBar", snapshotHandler = keep())

    private fun round(px: Int, font: Float) = DeviceConfig.WEAR_OS_SMALL_ROUND.copy(screenWidth = px, screenHeight = px, density = Density.XHIGH, screenRound = ScreenRound.ROUND, locale = "en", fontScale = font)

    // L'immagine dell'ultimo fotogramma: `BufferedImage` il classpath dei test Android non lo vede, si legge per riflessione.
    private fun keep(): SnapshotHandler = proxy(SnapshotHandler::class.java) { name, _ ->
        if (name == "newFrameHandler") proxy(SnapshotHandler.FrameHandler::class.java) { n, a -> if (n == "handle") last = a!![0]; null } else null
    }
    private fun <T> proxy(type: Class<T>, on: (String, Array<Any?>?) -> Any?): T = type.cast(
        java.lang.reflect.Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, a ->
            when (m.name) { "hashCode" -> 0; "equals" -> false; "toString" -> "keep"; else -> on(m.name, a) }
        },
    )

    /** La fascia lungo il bordo: per ogni pixel l'angolo (gradi, 0 a destra, in senso orario) e il colore. */
    private fun band(): List<Pair<Int, Int>> {
        val img = last!!
        val w = img.javaClass.getMethod("getWidth").invoke(img) as Int
        val rgb = img.javaClass.getMethod("getRGB", Int::class.java, Int::class.java)
        val r = w / 2.0
        val out = mutableListOf<Pair<Int, Int>>()
        for (tenth in 0 until 3600) {
            val deg = tenth / 10.0
            if (deg < 40 || deg > 320) continue
            for (d in listOf(1.5, 2.5, 3.5, 4.5)) {
                val x = (r + (r - d) * Math.cos(Math.toRadians(deg))).toInt().coerceIn(0, w - 1)
                val y = (r + (r - d) * Math.sin(Math.toRadians(deg))).toInt().coerceIn(0, w - 1)
                out += deg.toInt() to (rgb.invoke(img, x, y) as Int)
            }
        }
        return out
    }

    private fun ch(p: Int) = listOf((p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF)

    /** Gli angoli tagliati: testo o tasto ancora chiaro nella metà alta, e con `lower` qualunque cosa diversa dal fondo nella metà bassa. */
    private fun cuts(lower: Boolean): List<Int> {
        val b = band()
        // Il fondo è il colore più frequente della fascia: Paparazzi non annerisce fuori dal tondo e il fondo del tema non è nero.
        val bg = b.groupingBy { it.second }.eachCount().maxBy { it.value }.key
        val bright = b.filter { it.first >= 180 && ch(it.second).max() > 140 }
        val down = if (!lower) emptyList() else b.filter { it.first < 180 && ch(it.second).zip(ch(bg)).any { (a, c) -> kotlin.math.abs(a - c) > 10 } }
        return (bright + down).map { it.first }.distinct().sorted()
    }

    /** In cima lo stato di partenza; in fondo l'ultima voce come ancora, che la lista ferma al suo limite. */
    private fun check(name: String, screen: @androidx.compose.runtime.Composable (TransformingLazyColumnState) -> Unit) {
        val bad = mutableListOf<String>()
        for ((px, font) in listOf(384 to 1f, 456 to 1f, 384 to 1.24f, 456 to 1.24f)) {
            paparazzi.unsafeUpdateConfig(deviceConfig = round(px, font))
            val top = TransformingLazyColumnState()
            shot(name, px, font, "cima") { screen(top) }
            cuts(lower = false).takeIf { it.isNotEmpty() }?.let { h -> bad += "$name ${px}px font $font, in cima: tagliato a ${h.first()}°..${h.last()}° (${h.size} gradi)" }
            val end = TransformingLazyColumnState(initialAnchorItemIndex = top.layoutInfo.totalItemsCount - 1)
            shot(name, px, font, "fondo") { screen(end) }
            cuts(lower = true).takeIf { it.isNotEmpty() }?.let { h -> bad += "$name ${px}px font $font, in fondo: tagliato a ${h.first()}°..${h.last()}° (${h.size} gradi)" }
        }
        assertTrue(bad.joinToString(" | "), bad.isEmpty())
    }

    // Con EDGE_DUMP=<cartella> salva ogni fotogramma, per guardarlo.
    private fun shot(name: String, px: Int, font: Float, at: String, screen: @androidx.compose.runtime.Composable () -> Unit) {
        paparazzi.snapshot { CmTheme { screen() } }
        System.getenv("EDGE_DUMP")?.let { d ->
            Class.forName("javax.imageio.ImageIO").getMethod("write", Class.forName("java.awt.image.RenderedImage"), String::class.java, java.io.File::class.java)
                .invoke(null, last, "png", java.io.File(d, "$name-$px-$font-$at.png".replace(' ', '_')))
        }
    }

    @Test fun pairingNeverTouchesTheEdge() = check("Abbinamento") { s -> PairingScreen(PairingStatus.Idle, withCode = true, {}, {}, {}, listState = s) }
    @Test fun pairingPhoneOnlyNeverTouchesTheEdge() = check("Abbinamento dal telefono") { s -> PairingScreen(PairingStatus.Idle, withCode = false, {}, {}, {}, listState = s) }
    @Test fun pairingFailedNeverTouchesTheEdge() = check("Abbinamento fallito") { s -> PairingScreen(PairingStatus.Failed("x"), withCode = true, {}, {}, {}, listState = s) }
    @Test fun settingsNeverTouchTheEdge() = check("Impostazioni") { s -> SettingsScreen(Settings(), onChange = {}, onRepair = {}, listState = s) }
    @Test fun settingsNotificationsOffNeverTouchTheEdge() = check("Impostazioni senza notifiche") { s -> SettingsScreen(Settings(), onChange = {}, onRepair = {}, notificationsEnabled = false, listState = s) }
    @Test fun sessionsNeverTouchTheEdge() = check("Sessioni") { s -> SessionsScreen(snap, now, onOpen = {}, onSettings = {}, listState = s) }
    // Una sessione ferma: mentre lavora il bagliore in fondo tocca il bordo per disegno, ed è luce, non testo.
    @Test fun sessionNeverTouchesTheEdge() = check("Sessione") { s -> SessionScreen(snap, "field-notes", now, {}, {}, {}, {}, {}, listState = s) }
    @Test fun questionNeverTouchesTheEdge() = check("Domanda") { s -> QuestionScreen(snap, "ledger-api", now, null, {}, {}, {}, {}, {}, {}, listState = s) }
    @Test fun quotaNeverTouchesTheEdge() = check("Quota") { s -> QuotaScreen(state, Freshness.Fresh, now, animateOverride = false, listState = s) }
    @Test fun launchNeverTouchesTheEdge() = check("Nuova sessione") { s -> LaunchScreen(state.projects, enabled = true, onLaunch = {}, listState = s) }
}
