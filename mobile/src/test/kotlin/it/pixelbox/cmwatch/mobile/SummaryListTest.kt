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
import it.pixelbox.cmwatch.ui.tokens.CmColors
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
        Column(Modifier.background(CmColors.bg)) {
            SummaryList(model(state), {}, { _, _ -> }, { _, _ -> }, {}, {}, initiallyOpen = open)
        }
    }

    @Test fun summaryClosed() = paparazzi.snapshot { page(st) }
    @Test fun summaryOpen() = paparazzi.snapshot { page(st, open = "WAITING:ledger-api") }

    // La sezione «Lavori» (22/10): al posto di Da approvare, Impegni e Recap, le righe con un tasto e «Tutti i lavori».
    @Test fun summaryWithLavori() = paparazzi.snapshot {
        val idle = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
        val night = ContractJson.decodeNightReport(File("../contract/night-report-sample.json").readText())
        val ok = it.pixelbox.cmwatch.contract.Approval("ledger-release-14", "Release 1.4 di ledger-api", "tag v1.4 e push", "github.com/example/ledger-api", requestedAt = idle.ts - 600)
        val st2 = idle.copy(commitments = night.commitments.take(1) + idle.commitments!!, approvals = listOf(ok))
        val agenda = it.pixelbox.cmwatch.contract.AgendaPage(listOf(
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "personale", "franz", "Revocare la chiave API Anthropic trovata in chiaro", "console.anthropic.com → API Keys"),
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "agenzia", "claude", "Migrare a pixel i 4 siti con idemedia attivo", "pinsacesare.it rehalife.it"),
        ), owner = "franz")
        val today = java.time.LocalDate.of(2026, 9, 12)
        CmPhoneTheme(still = true) {
            Column(Modifier.background(CmColors.bg)) {
                SummaryList(model(st2), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st2.ts, foryou = it.pixelbox.cmwatch.rules.Lavori.forYou(st2, agenda, st2.ts, today), lavoriLine = "Tutti i lavori · in corso 3 · stanotte 1 · fatto oggi 0")
            }
        }
    }

    // P6 (21/10): in cima alla home la card «Aggiornamento disponibile · Installa» per una release nuova su GitHub.
    @Test fun summaryWithUpdate() = paparazzi.snapshot {
        val r = it.pixelbox.cmwatch.rules.AppUpdate.Release("impegni-2026-10-22", "Notte 22/10", st.ts - 2 * 3600 - 300, true, "https://example.com/mobile-release.apk", 68813846, null, "")
        val u = it.pixelbox.cmwatch.mobile.update.AppUpdater.Ui(phase = it.pixelbox.cmwatch.mobile.update.AppUpdater.Phase.Available(r))
        CmPhoneTheme(still = true) {
            Column(Modifier.background(CmColors.bg)) { SummaryList(model(st), {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts, update = u) }
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
        CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.bg)) { SummaryList(m, {}, { _, _ -> }, { _, _ -> }, {}, {}, startOpen = setOf("sessions", "other")) } }
    }

    // Mockup approvato da Franz l'08/10 alle 12:57: sezioni richiudibili e in testa il riquadro Notte.
    @Test fun summaryWithNight() = paparazzi.snapshot {
        val rep = ContractJson.decodeNightReport(File("../contract/night-report-sample.json").readText())
        val night = it.pixelbox.cmwatch.rules.NightPage.of(rep, java.time.ZoneId.of("Europe/Rome"))
        CmPhoneTheme(still = true) {
            Column(Modifier.background(CmColors.bg)) {
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
            Column(Modifier.background(CmColors.bg)) {
                SummaryList(m, {}, { _, _ -> }, { _, _ -> }, {}, {}, startOpen = setOf("other"), nightPage = night,
                    tonight = it.pixelbox.cmwatch.rules.TonightCard.of(st.night.copy(items = items), st.ts))
            }
        }
    }

    @Test fun dockMaster() = paparazzi.snapshot {
        val m = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null)
        val reply = TranscriptEntry("a1", "assistant", "Lanciata claude-master sulla fase 2.2.\n\nEsito: Fase 2.2 avviata su claude-master\nProssimi: distilla il confronto nella kb", st.ts - 600)
        CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.bg)) { MasterDock(m, it.pixelbox.cmwatch.rules.MasterHome.hero(listOf(reply), m), {}, {}) } }
    }
    // La master espansa a tutta pagina (Franz, 03/10 16:44): la stessa barra in cima, con ▼ per ridurla.
    @Test fun dockMasterExpanded() = paparazzi.snapshot {
        val m = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null)
        val reply = TranscriptEntry("a1", "assistant", "Lanciata claude-master sulla fase 2.2.\n\nEsito: Fase 2.2 avviata su claude-master", st.ts - 600)
        CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.bg)) { MasterDock(m, it.pixelbox.cmwatch.rules.MasterHome.hero(listOf(reply), m), {}, {}, expanded = true) } }
    }
    @Test fun dockAbsent() = paparazzi.snapshot { CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.bg).padding(16.dp)) { MasterAbsent {} } } }
}
