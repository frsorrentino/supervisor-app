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

    private fun prova(b: BancoProva.Build) = paparazzi.snapshot { CmPhoneTheme(still = true) { DaProvareScreen(prove, { b }, onBack = {}, onOk = {}, onKo = { _, _, _ -> }, onScreen = {}) } }
    @Test fun banco_prova_installata() = prova(BancoProva.Build.INSTALLED)
    @Test fun banco_prova_daInstallare() = prova(BancoProva.Build.TO_INSTALL)
    @Test fun cinque_minuti_quante() = paparazzi.snapshot { CmPhoneTheme(still = true) { CinqueMinutiScreen(dec, onBack = {}, onApprove = { _, _ -> }, onDone = {}, onNotNow = {}, onDrop = { _, _ -> }) } }
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
