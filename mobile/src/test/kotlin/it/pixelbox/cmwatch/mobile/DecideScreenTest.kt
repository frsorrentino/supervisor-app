package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.BancoProva
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Le schermate della fase A con le pagine della fixture del contratto, solo dati demo. */
class DecideScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenHeight = 2000), theme = "android:Theme.Material.NoActionBar")

    private val fake = it.pixelbox.cmwatch.transport.FakeTransport(load = { File("../contract/$it.json").readText() })
    private fun textOf(op: String) = fake.sampleText(op)
    private val prove = ContractJson.decodeProve(textOf("prove"))
    private val dec = ContractJson.decodeDecisions(textOf("decisioni"))

    // app-prove-segnalate-android-13-11 (Pixel di Franz, 13/11 18:02): tre versioni che l'app non riconosce, senza prove a schermo, e una
    // prova segnalata. Nessuna nota «Non so se questa versione…» da sola sopra SEGNALATE; la segnalata aperta ha «Ora va».
    @Test fun da_provare_segnalate() = paparazzi.snapshot {
        val page = it.pixelbox.cmwatch.contract.ProvePage(
            groups = listOf("auto-2026-11-13-1017-79bf068a", "auto-2026-11-13-1204-267872b0", "auto-2026-11-13-1243-7838fa0b").map { b -> it.pixelbox.cmwatch.contract.ProveGroup(build = b) },
            ko = listOf(it.pixelbox.cmwatch.contract.ProveKo(id = 389, task = "app-verifica-conto-elenco-12-11", short = "Conto ed elenco in Verifica", text = "Il conto dice 3 ma l'elenco ne mostra 2.")),
        )
        CmPhoneTheme(still = true) { DaProvareScreen(page, { BancoProva.Build.UNKNOWN }, onBack = {}, onOk = { true }, onKo = { _, _, _ -> true }, onScreen = {}, focus = "ko-389") }
    }
    private fun prova(b: BancoProva.Build) = paparazzi.snapshot { CmPhoneTheme(still = true) { DaProvareScreen(prove, { b }, onBack = {}, onOk = { true }, onKo = { _, _, _ -> true }, onScreen = {}) } }
    @Test fun banco_prova_installata() = prova(BancoProva.Build.INSTALLED)
    @Test fun banco_prova_daInstallare() = prova(BancoProva.Build.TO_INSTALL)
    @Test fun cinque_minuti_quante() = paparazzi.snapshot { CmPhoneTheme(still = true) { CinqueMinutiScreen(dec, onBack = {}, onApprove = { _, _ -> }, onDone = {}, onNotNow = {}, onDrop = { _, _ -> }) } }
    // Contratto 1.104 (Franz 02/11 08:04): la card dice cosa si propone, con Procedi e Parliamone; il testo ripetuto compare una volta sola.
    private val proposta = "Proposta del 28/10 22:22: aspetta l'ok di Franz"
    @Test fun decisione_spiegata_card() = paparazzi.snapshot { CmPhoneTheme(still = true) { CinqueMinutiScreen(dec.copy(groups = listOf(DecisionGroup("k", title = proposta, weight = 1, where = "here", items = listOf(
        DecisionItem(kind = "pause", task = "logo-ambra-28-10", title = "Logo ambra sulla scheda Play di Supervisor", reason = proposta, asks = "ok",
            planStart = "Quando Google approva la scheda, si sostituisce l'icona corallo con quella ambra in tutte le dimensioni richieste da Play, si rigenera l'immagine in evidenza e si manda la scheda in revisione; nessuna modifica all'app, solo alla scheda dello store."),
    )))), onBack = {}, onApprove = { _, _ -> }, onDone = {}, onNotNow = {}, onDrop = { _, _ -> }, startHowMany = 1) } }
    @Test fun cinque_minuti_una() = paparazzi.snapshot { CmPhoneTheme(still = true) { CinqueMinutiScreen(dec, onBack = {}, onApprove = { _, _ -> }, onDone = {}, onNotNow = {}, onDrop = { _, _ -> }, startHowMany = 3) } }
    @Test fun ok_informato_card() {
        val st = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
        paparazzi.snapshot { CmPhoneTheme(still = true) { Column(Modifier.fillMaxWidth().background(V3.Bg).padding(12.dp)) {
            ApprovalCard(st.approvals.single(), st.ts) { _, _, _ -> }
            ApprovalCard(Approval("demo", "Rilascio demo", "tag e push", "produzione", true, st.ts - 60), st.ts) { _, _, _ -> }
        } } }
    }
    @Test fun heads_up_nota() {
        val s = Session(id = "s", name = "blog", account = "a", project = "p", state = SessionState.IDLE, since = 0, headsUp = HeadsUp("Questa conversazione si sta allungando: conviene un handoff", 900))
        paparazzi.snapshot { CmPhoneTheme(still = true) { androidx.compose.runtime.CompositionLocalProvider(LocalHeadsUp provides HeadsUpActions(emptySet(), {}, { _, _ -> })) { Column(Modifier.fillMaxWidth().background(V3.Bg).padding(12.dp)) { HeadsUpNoteRow(s, 1800) } } } }
    }
}
