package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** La sezione «Plugin e skill» (contratto 1.95, mockup approvato il 29/10) a 320 e 412 dp, sui dati di prova del relay. */
class PluginsSectionTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val summary = ContractJson.decodeState(File("../contract/state-3-stale.json").readText()).plugins!!
    // L'elenco come lo manda l'op `plugins` (la forma di cmd-result-sample.json), scritto qui: i test del telefono non hanno
    // kotlinx.serialization per leggere il campione intero.
    private val page = PluginsPage(
        checkedAt = summary.checkedAt,
        accounts = listOf(
            PluginsAccount(
                "personal",
                listOf(
                    Plugin("chrisbanes-skills@chrisbanes-skills", "chrisbanes-skills", "chrisbanes-skills", "user", "2026.9.22.01", "2026.10.26", true),
                    Plugin("frontend-design@claude-plugins-official", "frontend-design", "claude-plugins-official", "user", "465f09344424", null, null),
                    Plugin("supervisor@supervisor-dev", "supervisor", "supervisor-dev", "user", "0.8.81", "0.8.81", false),
                ),
                listOf(Skill("animate", "1.2", "account"), Skill("model-bench", null, "local")),
            ),
            PluginsAccount("work", listOf(Plugin("supervisor@supervisor-dev", "supervisor", "supervisor-dev", "user", "0.8.81", "0.8.81", false)), emptyList()),
        ),
    )

    private fun at(dp: Int) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 2400))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.fillMaxSize().background(it.pixelbox.cmwatch.mobile.ui.V3.Bg).padding(12.dp)) {
                    PluginsCard(summary, { page }, { _, _ -> null }, initial = page)
                }
            }
        }
    }

    // Dopo la conferma: lo stato in corso al posto dei tasti.
    @Test fun plugins_updatingAll() {
        val r = it.pixelbox.cmwatch.rules.PluginsRun(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined), { page }, { _, _ -> kotlinx.coroutines.awaitCancellation() }, page)
        r.updateAll(listOf("personale"))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.fillMaxSize().background(it.pixelbox.cmwatch.mobile.ui.V3.Bg).padding(12.dp)) {
                    PluginsCard(summary, { page }, { _, _ -> null }, initial = page, runner = r)
                }
            }
        }
    }

    @Test fun plugins_state320() = at(320)
    @Test fun plugins_state412() = at(412)
}
