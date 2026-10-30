package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.Kpi
import it.pixelbox.cmwatch.rules.Summary
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * I KPI coi dati già nel relay (proposte 6, 11, 18, compito app-kpi-medie-25-10; 4 e 7, app-kpi-piccole-25-10) a 320, 360 e 412 dp: nessun testo esce
 * da card e pagina (Franz 25/10 21:33). Una giornata di prova dalle 8 alle 16, una sessione che ti aspetta da 25 minuti.
 */
class KpiViewsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val zone = ZoneId.of("Europe/Rome")
    private val st = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
    private val from = Kpi.midnight(st.ts, zone)
    private val now = from + 16 * 3600 + 20 * 60
    private val events = buildList {
        for (h in listOf(8, 9, 10, 11, 14, 15, 16)) for (m in listOf(2, 9, 17, 31, 44)) add(TimelineEvent(from + h * 3600 + m * 60, if (m == 44) "commit" else "prompt", "x"))
    }
    private val page = TimelinePage(from, listOf(TimelineSession("atlas-shop", true, "atlas-shop", events)))
    private val spans = Kpi.day(events, from, now, now - 25 * 60)
    private val figures = Kpi.dayFigures(page, st.copy(sessions = st.sessions), now, zone)

    private fun at(dp: Int) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 2400))
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.fillMaxSize().background(it.pixelbox.cmwatch.mobile.ui.V3.Bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Proposte piccole (compito app-kpi-piccole-25-10): le tre cifre e la quota per sessione.
                    Kpi7(Kpi.head(st, page, now, zone), st, now, {})
                    Kpi.split(st, "personal")?.let { Kpi4(it) }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(it.pixelbox.cmwatch.mobile.ui.V3.S1).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("atlas-shop", color = it.pixelbox.cmwatch.mobile.ui.V3.Tx, modifier = Modifier.weight(1f)); Kpi6(Kpi.hourly(events, now)) }
                        Kpi11(spans, from, now)
                    }
                    Kpi18(figures)
                }
            }
        }
    }

    @Test fun kpi320() = at(320)
    @Test fun kpi360() = at(360)
    @Test fun kpi412() = at(412)

    // La home a 320 dp con la cronologia: le colonnine sulla riga del contesto, il nome intero.
    @Test fun kpiHome320() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 880, screenHeight = 2400))
        val m = Summary.build(st, emptyList(), emptyList(), st.ts, zone, emptySet())
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                Column(Modifier.background(it.pixelbox.cmwatch.mobile.ui.V3.Bg)) {
                    SummaryList(m, {}, { _, _ -> }, { _, _ -> }, {}, {}, now = st.ts, kpiPage = page.copy(sessions = page.sessions.map { it.copy(events = it.events.map { e -> e.copy(at = e.at - now + st.ts) }) }), kpiState = st)
                }
            }
        }
    }
}
