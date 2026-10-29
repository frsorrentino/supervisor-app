package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Le viste coi dati nuovi del relay (proposte 8, 9, 10, 12, 13, 17; contratto 1.96, compito app-kpi-dati-relay-25-10) a
 * 320, 360 e 412 dp, col `kpi` del campione del contratto: nessun testo esce da card e pagina (Franz 25/10 21:33).
 * Due giri: le viste da sole e, a 320 dp, i casi lunghi (nomi lunghi, una macchina spenta, nessun suggerimento).
 */
class KpiDataViewsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    // Il `kpi` di cmd-result-sample.json preso dal file: il modulo del telefono non ha kotlinx-json nei test, il testo si
    // toglie dalle virgolette a mano (non ha altri escape) e lo legge ContractJson del core.
    private val data: KpiData = run {
        val line = File("../contract/cmd-result-sample.json").readLines().single { "\"text\": \"{\\\"at\\\": " in it && "kpi_tasks" in it }
        ContractJson.decodeKpi(line.substringAfter("\"text\": \"").substringBeforeLast("\"").replace("\\\"", "\""))
    }
    private val now = data.at

    private fun shot(dp: Int, d: KpiData) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 3400))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.fillMaxSize().background(CmColors.bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    d.tasks?.let { Kpi8(it) }
                    d.checksWeek?.let { Kpi9(it) }
                    // La 10 sta nella card aperta della sessione: la stessa cornice.
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surface).padding(14.dp)) { Kpi10(Kpi.turnsOf(d, "atlas-shop")) }
                    d.health?.let { Kpi12(it) }
                    d.suggestions7d?.let { Kpi13(it) }
                    d.waits?.let { Kpi17(it, now) }
                }
            }
        }
    }

    @Test fun kpiData320() = shot(320, data)
    @Test fun kpiData360() = shot(360, data)
    @Test fun kpiData412() = shot(412, data)

    @Test fun kpiDataLong320() {
        val t = data.tasks!!
        val long = data.copy(
            tasks = t.copy(tasks = t.tasks + KpiTask("supervisor-app-pulizia-nomi-29-10", "La pulizia dei nomi nel repository pubblico prima della release", "assigned", 40 * 60, 3, 3, false, "supervisor-app-molto-lungo")),
            turnsToday = listOf(KpiSessionTurns("atlas-shop", (0 until 30).map { i -> KpiTurn(now - 30_000 + i * 1000L, now - 30_000 + i * 1000L + (60 + i * 37 % 400), if (i % 7 == 3) "fail" else if (i == 29) "running" else "ok", 12_000L + i * 4_100, 0) })),
            health = data.health!!.copy(latency = KpiLatency(null, null, 0), hosts = data.health!!.hosts + KpiHost("chromebook-di-franz", false)),
            suggestions7d = KpiSuggestions(),
            waits = data.waits!!.copy(now = data.waits!!.now + KpiWaitNow("supervisor-app-molto-lungo", "needs_you", now - 3 * 3600)),
        )
        shot(320, long)
    }
}
