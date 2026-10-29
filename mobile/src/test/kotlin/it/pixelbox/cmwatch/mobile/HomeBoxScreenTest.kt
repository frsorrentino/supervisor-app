package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * Le pagine dei tre box in alto della home (compito app-box-cliccabili-29-10) a 412 dp, come il mockup approvato, e a
 * 320 dp per i testi lunghi: chi aspetta (una domanda e una ferma che si chiude da sola), chi lavora e le ferme, gli esiti
 * di oggi coi rossi in cima.
 */
class HomeBoxScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val zone = ZoneId.of("Europe/Rome")
    private val q = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val now = q.ts
    private val st = q.copy(sessions = q.sessions.map {
        if (it.name == "field-notes") it.copy(needsYou = NeedsYou("Fai il login su Search Console e mandami il codice di verifica", now - 180, until = true)) else it
    } + Session(id = "w", name = "watchface", account = "personal", project = "watchface", state = SessionState.IDLE, since = 0, outcome = Outcome("Quadrante notturno pubblicato su Play", "", now - 3600)))
    private val page = TimelinePage(now - 86400, listOf(
        TimelineSession("chrome-bridge", false, events = listOf(TimelineEvent(now - 1200, "task", "il file del benchmark non c'è in docs/", ok = false, ref = "benchmark-mcp-29-10"))),
        TimelineSession("atlas-shop", true, events = listOf(
            TimelineEvent(now - 900, "outcome", "Migrations 008-011 applied, tests green"),
            TimelineEvent(now - 600, "task", "la release non è sul ramo tab-emoji/22-10", ok = false, ref = "app-kpi-dati-relay-25-10"),
            TimelineEvent(now - 300, "outcome", "Contratto 1.97 adottato, pre-release kpi-dati-2026-10-29"),
        )),
        TimelineSession("watchface", true, events = listOf(TimelineEvent(now - 3600, "outcome", "Quadrante notturno pubblicato su Play"))),
    ))

    private fun shot(box: HomeBox, dp: Int, state: State = st, p: TimelinePage? = page) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 2400))
        paparazzi.snapshot { CmPhoneTheme(still = true) { HomeBoxScreen(box, state, p, now, onBack = {}, onSession = {}, zone = zone) } }
    }

    @Test fun waiting412() = shot(HomeBox.WAITING, 412)
    @Test fun waiting320() = shot(HomeBox.WAITING, 320)
    @Test fun waitingNone412() = shot(HomeBox.WAITING, 412, state = st.copy(sessions = emptyList()))
    @Test fun busy412() = shot(HomeBox.BUSY, 412)
    @Test fun busy320() = shot(HomeBox.BUSY, 320)
    @Test fun outcomes412() = shot(HomeBox.OUTCOMES, 412)
    @Test fun outcomes320() = shot(HomeBox.OUTCOMES, 320)
    @Test fun outcomesLoading412() = shot(HomeBox.OUTCOMES, 412, p = null)
}
