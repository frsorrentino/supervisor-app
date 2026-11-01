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

    /** Solo dati demo: tutti i tipi, un esito vecchio senza tipo e due in corso, che restano chiusi in fondo. */
    private val typed = TimelinePage(now - 86400, listOf(
        TimelineSession("atlas-shop", true, events = listOf(
            TimelineEvent(now - 300, "outcome", "Release 2.4 published, checkout green", type = "shipped"),
            TimelineEvent(now - 900, "outcome", "Migrations 008-011 applied, tests green", type = "done"),
            TimelineEvent(now - 1500, "outcome", "Payment suite red on the refund case", type = "problem"),
            TimelineEvent(now - 200, "outcome", "Build running on the release branch", type = "running"),
        )),
        TimelineSession("ledger-api", false, events = listOf(
            TimelineEvent(now - 600, "outcome", "Invoices endpoint ready: approve the plan?", type = "waiting"),
            TimelineEvent(now - 1800, "outcome", "The rate limit is per account, not per key", type = "answer"),
            TimelineEvent(now - 2400, "outcome", "Old outcome written before the types"),
            TimelineEvent(now - 100, "outcome", "Tests running on the new endpoint", type = "running"),
        )),
    ))

    private fun shot(box: HomeBox, dp: Int, state: State = st, p: TimelinePage? = page, byType: Boolean? = null) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 2400))
        paparazzi.snapshot { CmPhoneTheme(still = true) { HomeBoxScreen(box, state, p, now, onBack = {}, onSession = {}, zone = zone, startByType = byType) } }
    }

    @Test fun waiting412() = shot(HomeBox.WAITING, 412)
    @Test fun waiting320() = shot(HomeBox.WAITING, 320)
    @Test fun waitingNone412() = shot(HomeBox.WAITING, 412, state = st.copy(sessions = emptyList()))
    @Test fun busy412() = shot(HomeBox.BUSY, 412)
    @Test fun busy320() = shot(HomeBox.BUSY, 320)
    @Test fun outcomes412() = shot(HomeBox.OUTCOMES, 412)
    @Test fun outcomes320() = shot(HomeBox.OUTCOMES, 320)
    @Test fun esiti_tipo_perTipo412() = shot(HomeBox.OUTCOMES, 412, p = typed, byType = true)
    @Test fun esiti_tipo_perSessione412() = shot(HomeBox.OUTCOMES, 412, p = typed, byType = false)
    @Test fun esiti_tipo_perTipo320() = shot(HomeBox.OUTCOMES, 320, p = typed, byType = true)
    /** Il testo grande del telefono di Franz: dal suo screenshot del 01/11 19:39, circa 1,3 volte. */
    private fun big(dp: Int, byType: Boolean, scale: Float) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 1000, fontScale = scale))
        paparazzi.snapshot { CmPhoneTheme(still = true) { HomeBoxScreen(HomeBox.OUTCOMES, st, typed, now, onBack = {}, onSession = {}, zone = zone, startByType = byType) } }
    }
    @Test fun esiti_interruttore_tipo412() = big(412, true, 1f)
    @Test fun esiti_interruttore_sessione412() = big(412, false, 1f)
    @Test fun esiti_interruttore_tipo320() = big(320, true, 1f)
    @Test fun esiti_interruttore_sessione320() = big(320, false, 1f)
    @Test fun esiti_interruttore_tipo412Grande() = big(412, true, 1.3f)
    @Test fun esiti_interruttore_sessione412Grande() = big(412, false, 1.3f)
    @Test fun esiti_interruttore_sessione320Grande() = big(320, false, 1.3f)
    @Test fun esiti_interruttore_sessione412Enorme() = big(412, false, 2f)
    private fun features(locale: String, dp: Int, scale: Float = 1f) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = locale, screenWidth = (dp * 2.75).toInt(), screenHeight = 2400, fontScale = scale))
        val c = it.pixelbox.cmwatch.rules.Features.decode(File("src/main/assets/features.json").readText())
        paparazzi.snapshot { CmPhoneTheme(still = true) { FeaturesScreen(c.copy(features = c.features.filter { f -> f.group in setOf("you", "next") }), onBack = {}, onRoute = {}) } }
    }
    private fun page(key: String, locale: String = "it", dp: Int = 412) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = locale, screenWidth = (dp * 2.75).toInt(), screenHeight = 1400))
        val c = it.pixelbox.cmwatch.rules.Features.decode(File("src/main/assets/features.json").readText())
        paparazzi.snapshot { CmPhoneTheme(still = true) { FeaturesScreen(c, onBack = {}, startOn = key, onRoute = {}) } }
    }
    @Test fun panoramica_demo_paginaConPortamiLi() = page("esiti")
    @Test fun panoramica_demo_paginaSenzaPortamiLi() = page("orologio")
    @Test fun panoramica_demo_paginaInArrivo() = page("cinque_minuti", "en", 320)
    @Test fun panoramica_italiano412() = features("it", 412)
    @Test fun panoramica_inglese320Grande() = features("en", 320, 1.3f)
    @Test fun outcomesLoading412() = shot(HomeBox.OUTCOMES, 412, p = null)
}
