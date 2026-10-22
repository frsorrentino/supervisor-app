package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Night
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.LavoriHeader
import it.pixelbox.cmwatch.mobile.ui.LavoriPart
import it.pixelbox.cmwatch.mobile.ui.LavoriScreen
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
        AgendaRow("aperto", "agenzia", "claude", "Migrare a pixel i 4 siti di produzione con idemedia attivo", "pinsacesare.it rehalife.it tgcal24.it"),
        AgendaRow("aperto", "postazione", "claude", "Cancellare le cache orfane dei plugin", "~/.claude*/plugins/cache/"),
        AgendaRow("aperto", "postazione", "terzi", "Attesa revisione Chrome Web Store per chrome-bridge 1.12.0", "blocca la prova delle schede"),
        AgendaRow("fatto", "postazione", "nessuno", "Backups of the workstation every night", "crontab"),
    ), owner = "franz")

    @Composable private fun page(state: State, agenda: AgendaPage?, part: LavoriPart = LavoriPart.ALL, header: LavoriHeader = LavoriHeader.BAR, open: Set<String> = emptySet()) = CmPhoneTheme(still = true) {
        LavoriScreen(state, agenda, if (agenda == null) emptyList() else events, idle.ts, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}, {}, header = header, part = part, today = today, zone = zone, startOpen = open)
    }

    @Test fun lavoriFull() = paparazzi.snapshot { page(st, agenda) }
    @Test fun lavoriProgressAndDone() = paparazzi.snapshot { page(st, agenda, part = LavoriPart.RIGHT, header = LavoriHeader.DESK, open = setOf("paused", "others")) }
    @Test fun lavoriOldRelay() = paparazzi.snapshot { page(idle.copy(commitments = null, approvals = emptyList(), night = Night()), null) }
}
