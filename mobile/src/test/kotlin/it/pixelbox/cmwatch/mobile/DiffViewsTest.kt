package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.DiffRules
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Il diff dal telefono (contratto 1.72): riga chiusa, diff aperto, «Mostra tutto», sulle fixture del contratto. */
class DiffViewsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val view = ContractJson.decodeChanges(File("../contract/changes-sample.json").readText())
    private val edit = StepDiff("/home/dev/storefront/src/checkout/totals.ts", "edit", 2, 1, view.files[0].hunks)

    private fun step(d: StepDiff, open: Boolean, opener: DiffOpener = DiffOpener()) = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            CompositionLocalProvider(LocalDiffOpener provides opener) {
                Column(Modifier.background(CmColors.bg).padding(16.dp)) { StepDiffRow("a8.0", d, startOpen = open) }
            }
        }
    }

    @Test fun stepDiffClosed() = step(edit, open = false)
    @Test fun stepDiffOpen() = step(edit, open = true)
    // Caso 1: una riga di 50.000 lettere senza spazi non va a capo e non allarga la pagina; si ferma a 2000 col conto del resto.
    @Test fun stepDiffLongLine() = step(edit.copy(hunks = listOf(Hunk(1, 1, 1, 1, listOf("-" + "a".repeat(80), "+" + "x".repeat(50_000))))), open = true)
    // Caso 3: un passaggio tagliato finisce con «Mostra tutto» quando il relay ha l'op `diff`.
    @Test fun stepDiffCut() = step(
        StepDiff("/w/blog/bench/results.csv", "create", 261, 0, listOf(Hunk(0, 0, 1, 261, List(8) { "+home,$it,400,36" })), cut = true),
        open = true, opener = DiffOpener(full = { DiffRules.Load.Failed(DiffRules.Why.DOWN) }),
    )
    // Caso 5: in orizzontale, dopo una rotazione, il diff resta aperto e largo quanto lo schermo.
    @Test fun stepDiffOpenLandscape() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", orientation = ScreenOrientation.LANDSCAPE, screenWidth = 2340, screenHeight = 1080))
        step(edit, open = true)
    }

    private val shop = ContractJson.decodeState(File("../contract/state-1-question.json").readText()).sessions.single { it.changes != null }
    private fun panel(start: DiffRules.Load<ChangesView>, open: Map<String, DiffRules.Load<ChangesView>> = emptyMap()) = paparazzi.snapshot {
        CmPhoneTheme(still = true) { ChangesPanel(shop, load = { start }, onClose = {}, start = start, startOpen = open) }
    }

    @Test fun changesList() = panel(DiffRules.Load.Ok(view))
    @Test fun changesFileOpen() = panel(DiffRules.Load.Ok(view), mapOf("src/checkout/totals.ts" to DiffRules.Load.Ok(view.copy(files = view.files.take(1)))))
    // I file che arrivano senza righe lo dicono: il nome sensibile e il binario.
    @Test fun changesHiddenFiles() = panel(DiffRules.Load.Ok(view), mapOf(".env.local" to DiffRules.Load.Ok(view), "assets/logo.png" to DiffRules.Load.Ok(view)))
    @Test fun changesOutsideGit() = panel(DiffRules.Load.Ok(view.copy(source = "transcript", repo = null, base = null)))
    @Test fun changesGitTimeout() = panel(DiffRules.Load.Failed(DiffRules.Why.GIT, "git did not answer in 5 s"))
    @Test fun changesPcDown() = panel(DiffRules.Load.Failed(DiffRules.Why.DOWN))
    @Test fun changesNone() = panel(DiffRules.Load.Ok(view.copy(files = emptyList(), totals = ChangesTotals())))
    // Caso 3: la vista tagliata a 50 file e un file tagliato aperto con `file=`, anche lui oltre le 5000 righe.
    @Test fun changesCutView() {
        val cutFile = view.files[0].copy(cut = true)
        panel(
            DiffRules.Load.Ok(view.copy(files = listOf(cutFile), totals = ChangesTotals(51, 900, 120), cut = true)),
            mapOf(cutFile.path to DiffRules.Load.Ok(view.copy(files = listOf(cutFile)))),
        )
    }
}
