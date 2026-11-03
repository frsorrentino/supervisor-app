package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.Summary
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** Il riepilogo unico (design 03/10): i gruppi del bisogno, una riga aperta, molte sessioni ferme. */
class SummaryListTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private fun model(state: State) = Summary.build(state, emptyList(), emptyList(), state.ts, ZoneId.of("Europe/Rome"), emptySet())

    @Composable private fun page(state: State, open: String? = null) = CmPhoneTheme(still = true) {
        Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
            SummaryList(model(state), {}, { _, _ -> }, { _, _ -> }, {}, {}, initiallyOpen = open)
        }
    }

    @Test fun summaryClosed() = paparazzi.snapshot { page(st) }
    @Test fun summaryOpen() = paparazzi.snapshot { page(st, open = "WAITING:ledger-api") }
    // app-attesa-dettagli-02-11: aperta, una ferma dice la proposta e mette tutte le scelte come tasti (secondo giro della notte).
    @Test fun summaryOpenWaitingProposal() = paparazzi.snapshot {
        val full = "Ho riletto il post e sistemato i link.\nLo pubblico adesso o domattina, quando parte la newsletter?\nEsito: post pronto, manca la pubblicazione\nProssimi: !ok alla pubblicazione · programmalo per domani · mandami l'anteprima"
        val w = st.sessions.first { s -> s.name == "field-notes" }.let { s -> s.copy(
            state = it.pixelbox.cmwatch.contract.SessionState.IDLE,
            outcome = it.pixelbox.cmwatch.contract.Outcome("post pronto, manca la pubblicazione", full, st.ts - 240),
            nextSteps = listOf(it.pixelbox.cmwatch.contract.NextStep("ok alla pubblicazione", true), it.pixelbox.cmwatch.contract.NextStep("programmalo per domani"), it.pixelbox.cmwatch.contract.NextStep("mandami l'anteprima")),
            turnBlocking = it.pixelbox.cmwatch.contract.TurnBlocking("ok alla pubblicazione", st.ts - 240),
        ) }
        page(st.copy(sessions = st.sessions.map { s -> if (s.name == "field-notes") w else s }), open = "WAITING:field-notes")
    }

    // La sezione «Lavori» (22/10): al posto di Da approvare, Impegni e Recap, le righe con un tasto e «Tutti i lavori».
    @Test fun summaryWithLavori() = paparazzi.snapshot {
        val idle = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
        val night = ContractJson.decodeNightReport(File("../contract/night-report-sample.json").readText())
        val ok = it.pixelbox.cmwatch.contract.Approval("ledger-release-14", "Release 1.4 di ledger-api", "tag v1.4 e push", "github.com/example/ledger-api", requestedAt = idle.ts - 600)
        val st2 = idle.copy(commitments = night.commitments.take(1) + idle.commitments!!, approvals = listOf(ok))
        val agenda = it.pixelbox.cmwatch.contract.AgendaPage(listOf(
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "personale", "franz", "Revocare la chiave API Anthropic trovata in chiaro", "console.anthropic.com → API Keys"),
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "agenzia", "claude", "Migrare al nuovo host i 4 siti ancora attivi", "alfa.example beta.example"),
        ), owner = "franz")
        val today = java.time.LocalDate.of(2026, 9, 12)
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
                SummaryList(model(st2), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st2.ts, foryou = it.pixelbox.cmwatch.rules.Lavori.forYou(st2, agenda, st2.ts, today), lavoriLine = "Tutti i lavori · in corso 3 · stanotte 1 · fatto oggi 0")
            }
        }
    }

    // Contratto 1.81 (23/10): un ok che chiede una scelta mostra un tasto per opzione, nella scheda della sessione e nella lista di Lavori.
    @Test fun summaryWithChoices() = paparazzi.snapshot {
        val idle = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
        val live = idle.sessions.first { it.state != SessionState.GONE }.name
        val inCard = Approval("ledger-release-14", "Layout del checkout", "scegli il layout", "atlas-shop", requestedAt = idle.ts - 600, options = listOf("Strada A", "Strada B"))
        val loose = Approval("atlas-checkout-layout", "Checkout layout of atlas-shop", "pick the checkout layout", "atlas-shop", requestedAt = idle.ts - 300, options = listOf("Strada A", "Strada B"))
        val st2 = idle.copy(approvals = listOf(inCard, loose), commitments = listOf(Commitment("ledger-release-14", "Layout del checkout", session = live, state = "awaiting_ok")))
        val today = java.time.LocalDate.of(2026, 9, 12)
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
                SummaryList(model(st2), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st2.ts, foryou = it.pixelbox.cmwatch.rules.Lavori.forYou(st2, null, st2.ts, today), lavoriLine = "Tutti i lavori")
            }
        }
    }

    // Contratto 1.83 (24/10): le domande dei server MCP in cima alla home, il modulo con i quattro tipi (progetto scelto,
    // titolo troppo corto) e il link da aprire.
    @Test fun summaryWithElicitations() = paparazzi.snapshot {
        val idle = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
        val now = idle.elicitations[0].askedAt + 18
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) { SummaryList(model(idle), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = now, elicitations = idle.elicitations) }
        }
    }

    // Franz 02/11 07:20: le righe del programma sono testate come le altre, nell'ordine scelto; una sta in mezzo alle sezioni di sempre.
    @Test fun voci_nuove_trascinabili_home() = paparazzi.snapshot {
        val idle = ContractJson.decodeState(File("../contract/state-3-stale.json").readText()).copy(
            prove = ProveSummary(2, 1, 0, 0), decisions = DecisionsSummary(2, 1, 3, 0), seguiti = SeguitiSummary(2, 1, 1),
            primaRaccolta = PrimaRaccoltaSummary("ask", 7, 8, true, null),
            homeOrder = listOf("seguiti", "prima_raccolta", "esiti", "decisioni", "prove", "sessions", "suggestions", "lavori", "usage", "system", "plugins", "closed"),
        )
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
                SummaryList(model(idle), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = idle.ts, kpiState = idle, onHomeOrder = {}, startOpen = setOf("seguiti"),
                    sectionBodies = mapOf("prima_raccolta" to { }, "prove" to { }, "decisioni" to { }, "seguiti" to { androidx.compose.material3.Text("Corpo di Da seguire, aperto sul posto") }))
            }
        }
    }

    // Contratto 1.84 (scheda A, Franz 24/10 19:46): i suggerimenti della master dopo chi ti aspetta, tre di quattro.
    @Test fun summaryWithSuggestions() = paparazzi.snapshot {
        val sg = listOf(
            it.pixelbox.cmwatch.contract.Suggestion("sg-1", "recurring", "r", "Chiedi spesso «com'è andata la CI di atlas-shop»: la aggiungo ai Ricorrenti?", "p", st.ts - 900),
            it.pixelbox.cmwatch.contract.Suggestion("sg-2", "cleanup", "c", "Due compiti in pausa da 2 giorni: li archivio?", "p", st.ts - 900),
            it.pixelbox.cmwatch.contract.Suggestion("sg-3", "deadline", "d", "Il cron notturno di orbit-docs non è mai partito: lo attivo stasera?", "p", st.ts - 900),
            it.pixelbox.cmwatch.contract.Suggestion("sg-4", "quota", "q", "Settimana al 62 %: sposto i lavori pesanti alla notte?", "p", st.ts - 960),
        )
        val withSg = st.copy(suggestions = sg)
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) { SummaryList(model(withSg), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts, suggestions = it.pixelbox.cmwatch.rules.Suggestions.visible(withSg, emptySet(), st.ts)) }
        }
    }

    // L'ordine della home (Franz 30/10 16:44): Ti aspettano, Sessioni, suggerimenti, poi il resto; alta per vederlo intero.
    @Test fun homeOrderSessionsBeforeSuggestions() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 5000))
        val withSg = st.copy(suggestions = listOf(it.pixelbox.cmwatch.contract.Suggestion("sg-1", "recurring", "r", "Chiedi spesso «com'è andata la CI di atlas-shop»: la aggiungo ai Ricorrenti?", "p", st.ts - 900)))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) { SummaryList(model(withSg), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts, suggestions = it.pixelbox.cmwatch.rules.Suggestions.visible(withSg, emptySet(), st.ts)) }
            }
        }
    }

    // Contratto 1.88 (la coda): una scelta con un tasto per opzione, un lavoro da salvare e un tipo che l'app non conosce, neutro.
    @Test fun summaryWithSuggestionQueue() = paparazzi.snapshot {
        val sg = listOf(
            it.pixelbox.cmwatch.contract.Suggestion("sq-1", "choice", "choice:privacy", "Informativa privacy di orbit-docs: versione breve o completa?", at = st.ts - 900, rank = 2, source = "master", options = listOf("Breve", "Completa")),
            it.pixelbox.cmwatch.contract.Suggestion("sq-2", "unsaved", "unsaved:atlas-shop:1", "atlas-shop si è chiusa con 2 file non committati: la riapro per salvarli?", at = st.ts - 600, rank = 4, source = "script"),
            it.pixelbox.cmwatch.contract.Suggestion("sq-3", "zzz", "zzz:1", "Un tipo che quest'app non conosce ancora.", at = st.ts - 300, rank = 6, source = "master"),
        )
        val withSg = st.copy(suggestions = sg)
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) { SummaryList(model(withSg), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts, suggestions = it.pixelbox.cmwatch.rules.Suggestions.visible(withSg, emptySet(), st.ts)) }
        }
    }

    // Contratto 1.90 (Franz 28/10 13:19): la sezione «Sistema», le altre chiuse perché si veda per intero.
    private val red = ContractJson.decodeState(File("../contract/state-3-stale.json").readText())
    @Composable private fun systemPage(sys: SystemStatus) = CmPhoneTheme(still = true) {
        Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
            SummaryList(model(red), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = red.ts, startOpen = emptySet(), usage = { Text("quota") }, system = sys, ops = red.ops, onSystem = { _, _ -> })
        }
    }
    // Rosso: aperta da sola, carico e memoria, chi consuma, quattro consigli; «Chiudi» solo dove c'è l'azione.
    @Test fun system_advice_red() = paparazzi.snapshot { systemPage(red.system!!) }
    // Verde: compatta, il pallino e la riga del carico nel titolo.
    @Test fun system_green_compact() = paparazzi.snapshot {
        systemPage(red.system!!.copy(level = "green", loadLevel = "green", memLevel = "green", loadPerCore = 0.4, memAvailableMb = 3200, advice = emptyList()))
    }
    // Giallo dallo stato tagliato a 8 KB: senza consumatori, il livello e i consigli restano.
    @Test fun system_yellow_noConsumers() = paparazzi.snapshot {
        systemPage(red.system!!.copy(level = "yellow", loadLevel = "yellow", memLevel = "green", loadPerCore = 1.3, memAvailableMb = 2900, consumers = emptyList(), advice = red.system!!.advice.take(2).map { it.copy(level = "yellow") }))
    }

    // P6 (21/10): in cima alla home la card «Aggiornamento disponibile · Installa» per una release nuova su GitHub.
    @Test fun summaryWithUpdate() = paparazzi.snapshot {
        val r = it.pixelbox.cmwatch.rules.AppUpdate.Release("impegni-2026-10-22", "Notte 22/10", st.ts - 2 * 3600 - 300, true, "https://example.com/mobile-release.apk", 68813846, null, "")
        val u = it.pixelbox.cmwatch.mobile.update.AppUpdater.Ui(phase = it.pixelbox.cmwatch.mobile.update.AppUpdater.Phase.Available(r))
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) { SummaryList(model(st), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts, update = u) }
        }
    }

    // Attenzione n. 5 del piano: con dieci sessioni ferme «Ferme» resta una riga breve ciascuna, «Chiuse» una riga sola.
    @Test fun summaryMany() = paparazzi.snapshot {
        val idle = st.sessions.first { it.state == SessionState.IDLE }
        val many = (1..10).map { i -> idle.copy(id = "idle-$i", name = "progetto-$i", since = st.ts - i * 1800L) }
        page(st.copy(sessions = st.sessions + many))
    }

    // La master agganciata sopra il campo (design 03/10), e «Riapri la master» quando non c'è.
    // «Fuori dalle sessioni» (mockup A, Franz 03/10 22:34): tutte le categorie, dalla più urgente, sotto una sola sessione.
    @Test fun summaryOutside() = paparazzi.snapshot {
        val one = st.copy(sessions = st.sessions.filter { it.state == SessionState.IDLE }.take(1))
        val row = { k: MasterHome.Kind, t: String, d: String?, n: Int? -> MasterHome.Row(k, t, d, number = n, at = st.ts + 3600) }
        val m = model(one).copy(
            service = listOf(
                row(MasterHome.Kind.NIGHT, "", null, 0), row(MasterHome.Kind.NEXT_STEP, "atlas-shop", "Tag the release and open the PR", null),
                row(MasterHome.Kind.CONTEXT, "field-notes", null, 86), row(MasterHome.Kind.SCHEDULED, "", null, 1),
                row(MasterHome.Kind.NEXT_STEP, "orbit-docs", "Review the onboarding chapter", null), row(MasterHome.Kind.NIGHT_REPORT, "Notte: 3 lavori, 2 riusciti", "Import, backup e report fatti", null),
            ),
            closed = st.sessions.map { it.copy(state = SessionState.GONE) },
        )
        CmPhoneTheme(still = true) { Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) { SummaryList(m, {}, { _, _ -> }, { _, _ -> }, {}, {}, startOpen = setOf("sessions", "other")) } }
    }

    // Mockup approvato da Franz l'08/10 alle 12:57: sezioni richiudibili e in testa il riquadro Notte.
    @Test fun summaryWithNight() = paparazzi.snapshot {
        val rep = ContractJson.decodeNightReport(File("../contract/night-report-sample.json").readText())
        val night = it.pixelbox.cmwatch.rules.NightPage.of(rep, java.time.ZoneId.of("Europe/Rome"))
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
                SummaryList(model(st), {}, { _, _ -> }, { _, _ -> }, {}, {}, nightDate = night.day, nightPage = night, usage = { Text("quota") })
            }
        }
    }

    // La scheda Stanotte (mockup approvato l'08/10 alle 20:43): i lavori della coda, uno in corso, e l'ultima notte.
    @Test fun summaryTonight() = paparazzi.snapshot {
        val rep = ContractJson.decodeNightReport(File("../contract/night-report-sample.json").readText())
        val night = it.pixelbox.cmwatch.rules.NightPage.of(rep, java.time.ZoneId.of("Europe/Rome"))
        val one = st.copy(sessions = st.sessions.filter { it.state == SessionState.IDLE }.take(1))
        val items = st.night.items!!.let { l -> listOf(l[0].copy(started = st.ts - 23 * 60), l[1]) }
        val m = model(one).copy(service = listOf(MasterHome.Row(MasterHome.Kind.NIGHT, "", null, number = 1, at = st.ts)))
        CmPhoneTheme(still = true) {
            Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
                SummaryList(m, {}, { _, _ -> }, { _, _ -> }, {}, {}, startOpen = setOf("other"), nightPage = night,
                    tonight = it.pixelbox.cmwatch.rules.TonightCard.of(st.night.copy(items = items), st.ts))
            }
        }
    }

    @Test fun dockMaster() = paparazzi.snapshot {
        val m = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null)
        val reply = TranscriptEntry("a1", "assistant", "Lanciata claude-master sulla fase 2.2.\n\nEsito: Fase 2.2 avviata su claude-master\nProssimi: distilla il confronto nella kb", st.ts - 600)
        CmPhoneTheme(still = true) { Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) { MasterDock(m, it.pixelbox.cmwatch.rules.MasterHome.hero(listOf(reply), m), {}, {}) } }
    }
    // La master espansa a tutta pagina (Franz, 03/10 16:44): la stessa barra in cima, con ▼ per ridurla.
    @Test fun dockMasterExpanded() = paparazzi.snapshot {
        val m = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null)
        val reply = TranscriptEntry("a1", "assistant", "Lanciata claude-master sulla fase 2.2.\n\nEsito: Fase 2.2 avviata su claude-master", st.ts - 600)
        CmPhoneTheme(still = true) { Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) { MasterDock(m, it.pixelbox.cmwatch.rules.MasterHome.hero(listOf(reply), m), {}, {}, expanded = true) } }
    }
    @Test fun dockAbsent() = paparazzi.snapshot { CmPhoneTheme(still = true) { Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg).padding(16.dp)) { MasterAbsent {} } } }
    // La pressione lunga: la copia del titolo che segue il dito.
    @Test fun homeSectionDrag() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { it.pixelbox.cmwatch.mobile.ui.HomeDragGhost("lavori") }
    }

    // Quinto giro della notte (app-bug-notte-03-11): la home a 320 dp col testo grande, in inglese e in orizzontale.
    private fun sweep(kind: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        val base = DeviceConfig.PIXEL_5
        val cfg = when (kind) {
            "320" -> base.copy(locale = "it", screenWidth = (320 * 2.75).toInt(), fontScale = 1.3f)
            "en" -> base.copy(locale = "en")
            else -> base.copy(locale = "it", screenWidth = base.screenHeight, screenHeight = base.screenWidth)
        }
        paparazzi.unsafeUpdateConfig(deviceConfig = cfg)
        paparazzi.snapshot { content() }
    }
    @Test fun giro5_home_320Grande() = sweep("320") { page(st, open = "WAITING:ledger-api") }
    @Test fun giro5_home_en() = sweep("en") { page(st, open = "WAITING:ledger-api") }
    @Test fun giro5_home_orizzontale() = sweep("land") { page(st, open = "WAITING:ledger-api") }

    // Il riassunto a destra su una riga (app-riassunti-una-riga-03-11, variante 1): categorie chiuse con i numeri di stamattina,
    // 22 da decidere, 50 da provare, 5 suggerimenti; a 360 e a 320 dp col testo grande, in italiano e in inglese.
    private fun unaRiga(locale: String, dp: Int, font: Float) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = locale, screenWidth = (dp * 2.75).toInt(), fontScale = font))
        val shut = it.pixelbox.cmwatch.rules.HomeCats.encode(it.pixelbox.cmwatch.rules.HomeCats.decode(null).map { c -> c.copy(open = false) }, null)
        val sg = (1..5).map { i -> it.pixelbox.cmwatch.contract.Suggestion("sg-$i", "cleanup", "c$i", "Suggerimento $i", "p", st.ts - 900) }
        val x = st.copy(decisions = DecisionsSummary(22, 4, 9, 0), prove = ProveSummary(50, 0, 0, 0), suggestions = sg, homeOrder = shut)
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
                    SummaryList(model(x), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = x.ts, kpiState = x, suggestions = it.pixelbox.cmwatch.rules.Suggestions.visible(x, emptySet(), x.ts))
                }
            }
        }
    }
    @Test fun unaRiga_360() = unaRiga("it", 360, 1f)
    @Test fun unaRiga_320Grande() = unaRiga("it", 320, 1.3f)
    @Test fun unaRiga_360_en() = unaRiga("en", 360, 1f)
    @Test fun unaRiga_320Grande_en() = unaRiga("en", 320, 1.3f)

    // In cima ogni voce ha la stessa scheda col bordo acceso (Franz 03/11 08:25, app-in-cima-uguali-03-11): Sessioni insieme alle
    // voci che l'avevano già, alta per vederle tutte.
    private fun inCima(dp: Int, font: Float) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 4000, fontScale = font))
        val top = listOf("sessions", "suggestions", "decisioni", "prove", "seguiti", "prima_raccolta")
        val order = it.pixelbox.cmwatch.rules.HomeCats.encode(it.pixelbox.cmwatch.rules.HomeCats.decode(null), null, top)
        val sg = (1..3).map { i -> it.pixelbox.cmwatch.contract.Suggestion("sg-$i", "cleanup", "c$i", "Suggerimento $i", "p", st.ts - 900) }
        val x = st.copy(decisions = DecisionsSummary(12, 8, 32, 0), prove = ProveSummary(50, 9, 0, 0), seguiti = SeguitiSummary(2, 1, 1),
            primaRaccolta = PrimaRaccoltaSummary("ask", 7, 8, true, null), suggestions = sg, homeOrder = order)
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
                    SummaryList(model(x), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = x.ts, kpiState = x, onHomeOrder = {}, suggestions = it.pixelbox.cmwatch.rules.Suggestions.visible(x, emptySet(), x.ts),
                        sectionBodies = mapOf("prima_raccolta" to { }, "prove" to { }, "decisioni" to { }, "seguiti" to { }))
                }
            }
        }
    }
    // Trascinando una voce di In cima, sotto il bordo un solo bersaglio (app-cima-rilascio-reset-03-11); l'etichetta col tasto Reset.
    @Test fun cimaRilascio() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 1400))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                androidx.compose.foundation.layout.Box(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg).padding(12.dp)) {
                    Column {
                        BoardLabel(onReset = {})
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(200.dp))
                    }
                    OffTopDrop(140f, true)
                }
            }
        }
    }
    @Test fun inCima_360() = inCima(360, 1f)
    @Test fun inCima_320Grande() = inCima(320, 1.3f)
}
