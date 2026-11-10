package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.MonitorCard
import it.pixelbox.cmwatch.contract.MonitorRefresh
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LocalMonitorRefresh
import it.pixelbox.cmwatch.mobile.ui.LocalPageBg
import it.pixelbox.cmwatch.mobile.ui.MonitorPage
import it.pixelbox.cmwatch.mobile.ui.MonitorRefreshUi
import it.pixelbox.cmwatch.mobile.ui.MonitorsCard
import it.pixelbox.cmwatch.mobile.ui.TiAspettaColumn
import it.pixelbox.cmwatch.mobile.ui.V3
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * «Aggiorna adesso» (app-monitoraggi-aggiorna-10-11, contratto 1.133): il tasto nelle righe e nella scheda con i suoi stati. Nelle righe,
 * dall'alto: fermo; gira; riuscito con l'ora; errore in una riga col valore di prima; un monitoraggio che non si aggiorna, senza tasto.
 * Golden da CI; la regola del tasto è in core (MonitorsTest).
 */
class MonitorRefreshTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = phone(360), theme = "android:Theme.Material.NoActionBar")
    private val items = ContractJson.decodeMonitors(File("../core/src/test/resources/monitors-mockup.json").readText()).items
    private val now = 1_800_000_000L
    private fun phone(dp: Int, font: Float = 1f) = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), fontScale = font, screenHeight = 1800)
    private fun MonitorCard.with(state: String, message: String? = null, rc: Int? = null, retry: Long? = null) =
        copy(refreshable = true, refresh = MonitorRefresh(state, started = now - 30, ended = if (state == "running" || state == "idle") null else now - 20, rc = rc, message = message, retryAt = retry, timeoutS = 60))
    private val states = listOf(
        items[0].with("idle"),
        items[1].with("running"),
        items[2].with("ok", retry = now - 1),
        items[3].with("error", message = "curl: (6) Could not resolve host: api.example.com", rc = 6),
        items[4].copy(refreshable = false, refresh = null),
    )
    private val ui = MonitorRefreshUi(listOf("monitors", "monitor_refresh"), now)

    private fun rows() = paparazzi.snapshot { CmPhoneTheme(still = true) { CompositionLocalProvider(LocalMonitorRefresh provides ui) { MonitorsCard(states, {}, {}) } } }
    private fun page(m: MonitorCard, wide: Boolean = false) = paparazzi.snapshot {
        CmPhoneTheme(still = true) { CompositionLocalProvider(LocalMonitorRefresh provides ui) {
            if (wide) CompositionLocalProvider(LocalPageBg provides V3.S1) { TiAspettaColumn { MonitorPage(m, wide = true, onBack = {}, onStop = {}, onAsk = {}, onEditAlerts = {}) } }
            else MonitorPage(m, wide = false, onBack = {}, onStop = {}, onAsk = {}, onEditAlerts = {})
        } }
    }

    @Test fun rowStates() = rows()
    @Test fun rowStates320Font13() { paparazzi.unsafeUpdateConfig(deviceConfig = phone(320, 1.3f)); rows() }
    @Test fun pageRunning() = page(states[1])
    @Test fun pageDone() = page(states[2])
    @Test fun pageError() = page(states[3])
    @Test fun pageTabletIdle() { paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 1640, screenHeight = 1000, density = com.android.resources.Density.XHIGH)); page(states[0], wide = true) }
    // Senza l'op nel relay (relay vecchio) nessun tasto, anche se il monitoraggio si aggiornerebbe.
    @Test fun noOpNoButton() = paparazzi.snapshot { CmPhoneTheme(still = true) { CompositionLocalProvider(LocalMonitorRefresh provides ui.copy(ops = listOf("monitors"))) { MonitorsCard(states.take(1), {}, {}) } } }
}
