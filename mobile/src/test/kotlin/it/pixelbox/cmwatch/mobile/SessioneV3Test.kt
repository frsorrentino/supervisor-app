package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.ChatFeed
import it.pixelbox.cmwatch.rules.Elsewhere
import org.junit.Rule
import org.junit.Test
import java.io.File

/** I blocchi della schermata di sessione nello stile v3 (Franz 30/10 21:19): file modificati nel loro contenitore, domanda, avviso, chip. */
class SessioneV3Test {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

    private fun shot(open: Boolean = false, failed: Boolean = false) {
        val s = st.sessions.first { it.question != null }
        val t = st.ts - 600
        val v = ContractJson.decodeChanges(File("../contract/changes-sample.json").readText())
        val d = StepDiff("/home/dev/storefront/docs/2026-10-30-studio-v3-restyling-piano.md", "edit", 1, 1, v.files[0].hunks)
        val entries = List(4) { TranscriptEntry("a$it", "tool", text = d.path, at = t + it * 10L, tool = "Edit", diff = d, error = failed && it == 3) }
        CmPhoneTheme(still = true) {
            SessionSheet(s, st.ts, emptyList(), 120, none, elsewhere = Elsewhere.Finished("master", st.ts - 30), feed = ChatFeed.merge(entries, emptyList()))
        }
    }

    private fun at360() = paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (360 * 2.75).toInt()))

    @Test fun blocks() = paparazzi.snapshot { shot() }
    @Test fun blocks360() { at360(); paparazzi.snapshot { shot() } }
    @Test fun failed360() { at360(); paparazzi.snapshot { shot(failed = true) } }
}
