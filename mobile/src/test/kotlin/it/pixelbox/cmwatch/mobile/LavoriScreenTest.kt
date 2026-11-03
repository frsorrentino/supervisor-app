package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.CommitmentPause
import it.pixelbox.cmwatch.contract.CommitmentWaits
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.DoneDayRef
import it.pixelbox.cmwatch.contract.DonePage
import it.pixelbox.cmwatch.contract.DoneProof
import it.pixelbox.cmwatch.contract.DoneRow
import it.pixelbox.cmwatch.contract.Night
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LavoriHeader
import it.pixelbox.cmwatch.mobile.ui.LavoriPart
import it.pixelbox.cmwatch.mobile.ui.LavoriScreen
import it.pixelbox.cmwatch.mobile.ui.LavoriView
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneId

/** La pagina unica «Lavori» (mockup approvato il 22/10): i quattro blocchi con le fixture 1.76/1.77 e un'agenda. */
class LavoriScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val idle = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
    private val stale = ContractJson.decodeState(File("../contract/state-3-stale.json").readText())
    private val night = ContractJson.decodeNightReport(File("../contract/night-report-sample.json").readText())
    private val events = ContractJson.decodeEvents(File("../contract/events-sample.json").readText())
    private val ok = Approval("ledger-release-14", "Release 1.4 di ledger-api", "tag v1.4 e push", "github.com/example/ledger-api", requestedAt = idle.ts - 600)
    private val st = idle.copy(commitments = night.commitments + idle.commitments!! + stale.commitments!!, approvals = listOf(ok))
    private val today = Instant.ofEpochSecond(idle.ts).atZone(zone).toLocalDate()
    private val agenda = AgendaPage(listOf(
        AgendaRow("aperto", "personale", "franz", "Revocare la chiave API Anthropic trovata in chiaro", "console.anthropic.com → API Keys"),
        AgendaRow("aperto", "agenzia", "franz", "17 client_id residui: 6 con candidato da confermare", ".claude/DA-DECIDERE-client-id.md"),
        AgendaRow("aperto", "agenzia", "franz", "Svuotare i 46 container con Universal Analytics", ".claude/scan/tag-runtime.json"),
        AgendaRow("aperto", "agenzia", "claude", "Migrare al nuovo host i 4 siti di produzione ancora attivi", "alfa.example beta.example gamma.example"),
        AgendaRow("aperto", "postazione", "claude", "Cancellare le cache orfane dei plugin", "~/.claude*/plugins/cache/"),
        AgendaRow("aperto", "postazione", "terzi", "Attesa revisione Chrome Web Store per chrome-bridge 1.12.0", "blocca la prova delle schede"),
        AgendaRow("fatto", "postazione", "nessuno", "Backups of the workstation every night", "crontab"),
    ), owner = "franz")

    /** La pagina …0760 di `cmd-result-sample.json` (contratto 1.77), scritta a mano: il modulo del telefono non ha kotlinx-json nei test. */
    private val donePage = DonePage(
        "2026-02-10",
        rows = listOf(
            DoneRow("atlas-release-2-3", "Release 2.3 of atlas-shop", "personal/atlas-shop", "atlas-shop", 1789211400, "green", DoneProof("release", "v2.3", "https://github.com/example/atlas-shop/releases/tag/v2.3")),
            DoneRow("ledger-migrations-010", "Migrations 008-010 on staging", "work/clients/ledger-api", "ledger-api", 1789209000, "green", DoneProof("commit", "9f8e7d6", "https://github.com/example/ledger-api/commit/9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e")),
            DoneRow("notes-night-report", "Night report of field-notes", "personal/field-notes", null, 1789205000, "cancelled", DoneProof("file", "docs/notte/2026-02-10-field-notes.md", null)),
            DoneRow("orbit-docs-lint", "Lint of the orbit docs", "work/own/orbit-docs", "orbit-docs", 1789201000, "failed", DoneProof("check", "npm run lint", null)),
        ),
        days = listOf(DoneDayRef("2026-02-09", 3), DoneDayRef("2026-02-07", 1)),
    )

    @Composable private fun page(
        state: State, agenda: AgendaPage?, part: LavoriPart = LavoriPart.ALL, header: LavoriHeader = LavoriHeader.BAR, open: Set<String> = emptySet(),
        pages: Map<String, DonePage> = emptyMap(), view: LavoriView = LavoriView.LIST, now: Long = idle.ts, day: java.time.LocalDate = today,
    ) = CmPhoneTheme(still = true) {
        LavoriScreen(state, agenda, if (agenda == null) emptyList() else events, now, {}, { _, _, _ -> }, {}, {}, {}, {}, {}, {}, {}, header = header, part = part, today = day, zone = zone, donePages = pages, startOpen = open, startView = view)
    }

    /** Il caso reale della sera del 22/10 (mockup «In corso leggibile»): 2 al lavoro, 7 che aspettano Franz con Docs online ×5, 2 fermi su terzi, titoli lunghi. */
    private val eveningNow = 1792700400L
    private val evening: State by lazy {
        fun c(task: String, title: String, session: String, waitsOn: String, paused: CommitmentPause? = null, approvedAt: Long? = null, next: String? = null, short: String? = null) =
            Commitment(task, title, session = session, state = if (paused != null) "assigned" else "running", approved = true, approvedAt = approvedAt, ageS = 3600, idleS = 600,
                next = next, waits = CommitmentWaits(if (waitsOn == "person") session else "person", waitsOn), paused = paused, short = short)
        val docs = CommitmentPause("Docs online sospeso dal 18/10 a stato pulito: aspetta da Franz la casella di posta docs@, l'utente con password e le chiavi API e CDN", 1792274400, "master")
        idle.copy(ts = eveningNow, approvals = emptyList(), commitments = listOf(
            c("niente-emoji-22-10", "supervisor-app: niente emoji nell'app e nelle notifiche, solo icone vettoriali (SVG, vector drawable, Font Awesome)", "supervisor-app", "supervisor-app", approvedAt = eveningNow - 5400, short = "Niente emoji nell'app"),
            c("tab-suggerimento-22-10", "supervisor-app: con un suggerimento visibile nella casella di testo, Tab (web) lo copia nel campo come testo pronto per l'invio", "supervisor-app", "supervisor-app",
                paused = CommitmentPause("in coda dopo niente-emoji-22-10", eveningNow - 5300, "master"), approvedAt = eveningNow - 5400),
            c("docs-fase1-20261017", "Docs online fase 1 (task 1-8): repository e CI, verifiche, database, lettore automatico orario sul server", "docs-site", "person", paused = docs, approvedAt = 1792200000, short = "Docs online fase 1"),
            c("docs-fase2-20261017", "Docs online fase 2 (task 9-15): testo, immagini, pulizia dei dati, ricerca con controlli", "docs-site", "person", paused = docs, approvedAt = 1792200000, short = "Docs online fase 2"),
            c("docs-fase3-20261017", "Docs online fase 3 (task 16-19): esempi da API, guide e changelog", "docs-site", "person", paused = docs, approvedAt = 1792200000, short = "Docs online fase 3"),
            c("docs-api-20261017", "Docs online: pagine API come seconda sezione (colonna sezione, indice per sezione)", "docs-site", "person", paused = docs, approvedAt = 1792200000, short = "Docs online: API"),
            c("docs-guide-20261017", "Docs online: guide che citano le API (filtro sull'oggetto prima di PDF e indice)", "docs-site", "person", paused = docs, approvedAt = 1792200000, short = "Docs online: guide"),
            c("lavori-gerarchia-22-10", "supervisor-app: la sezione In corso di Lavori è un muro di testo: gruppi per chi deve muoversi, titolo breve, Elenco e Tempo", "supervisor-app", "person", next = "l'ok a: il mockup della master", short = "Lavori: In corso leggibile"),
            c("phone-fix-0310-sera", "App telefono: menu che si chiudono, volo card→sessione al tocco, «Avvia» solo senza sessione, quota 5h su card e testata", "claude-master-phone", "person",
                paused = CommitmentPause("compito del 03/10 dell'app quando si chiamava claude-master-phone; il controllo non gira più (exit 127): da confermare con Franz se è superato e va cancellato", 1792622164, "master"), approvedAt = 1791050000, short = "App telefono del 03/10"),
        ))
    }
    private val eveningAgenda = AgendaPage(listOf(
        AgendaRow("aperto", "postazione", "terzi", "Attesa revisione Chrome Web Store per chrome-bridge 1.12.0 con move_tab", "chrome-bridge 1.12.0 con move_tab", key = "a1", short = "Revisione Chrome Web Store"),
        AgendaRow("aperto", "personale", "terzi", "chrome-bridge, issue #2: risposta di un utente sul tab che non si muove", "", detail = "risposta di un utente\nseconda riga", until = "2026-10-25", key = "a2"),
    ), owner = "franz")
    private val eveningDay: java.time.LocalDate = Instant.ofEpochSecond(eveningNow).atZone(zone).toLocalDate()

    @Test fun lavoriFull() = paparazzi.snapshot { page(st, agenda) }
    @Test fun lavoriProgressAndDone() = paparazzi.snapshot { page(st, agenda, part = LavoriPart.RIGHT, header = LavoriHeader.DESK) }
    // Mockup «In corso leggibile» (22/10 21:16) col caso reale: la vista Elenco e la vista Tempo.
    @Test fun lavoriProgressList() = paparazzi.snapshot { page(evening, eveningAgenda, part = LavoriPart.RIGHT, header = LavoriHeader.NONE, now = eveningNow, day = eveningDay) }
    @Test fun lavoriProgressTime() = paparazzi.snapshot { page(evening, eveningAgenda, part = LavoriPart.RIGHT, header = LavoriHeader.NONE, view = LavoriView.TIME, now = eveningNow, day = eveningDay) }
    // Contratto 1.77: il blocco Fatto dall'op `done`, con la prova, gli esiti e i giorni prima richiusi col conteggio.
    @Test fun lavoriDoneFromTheOp() = paparazzi.snapshot { page(st, agenda, part = LavoriPart.RIGHT, header = LavoriHeader.NONE, pages = mapOf(donePage.date to donePage)) }
    @Test fun lavoriOldRelay() = paparazzi.snapshot { page(idle.copy(commitments = null, approvals = emptyList(), night = Night()), null) }

    // Quinto giro della notte (app-bug-notte-03-11): la stessa schermata a 320 dp col testo grande, in inglese e in orizzontale.
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
    @Test fun giro5_lavori_320Grande() = sweep("320") { page(st, agenda) }
    @Test fun giro5_lavori_en() = sweep("en") { page(st, agenda) }
    @Test fun giro5_lavori_orizzontale() = sweep("land") { page(st, agenda) }
}
