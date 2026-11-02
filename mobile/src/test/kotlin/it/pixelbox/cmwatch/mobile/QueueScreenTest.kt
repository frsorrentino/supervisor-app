package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

/** La coda «Ti aspettano»: due domande in fila (la seconda chiesta più tardi, rischio alto) e la coda vuota. */
class QueueScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val q = st.sessions.first { it.question != null }
    private val two = st.copy(sessions = st.sessions + q.copy(
        id = "s-second", name = "atlas-review", state = SessionState.WAITING,
        question = q.question!!.copy(id = "q-second", tier = Tier.HIGH, askedAt = q.question!!.askedAt + 60),
    ))

    @Test fun queueTwo() = paparazzi.snapshot { CmPhoneTheme(still = true) { QueueScreen(two, st.ts + 300, { none }, {}) } }

    // Contratto 1.101 (Franz, 02/11 01:22): la card di un permesso dice cosa si approva; a 320 dp e col testo grande, comando lungo piegato.
    private val longCmd = "Run the migration on the staging database: psql -h db.staging.example -f migrations/2026-11-02-orders.sql --single-transaction --set ON_ERROR_STOP=1"
    private fun permesso(dp: Int, scale: Float, what: String?, body: String? = null) {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), fontScale = scale))
        val p = q.question!!.copy(kind = QuestionKind.PERMISSION, text = "Do you want to proceed?", what = what, body = body, options = listOf(Option(1, "Yes"), Option(2, "No")))
        paparazzi.snapshot { CmPhoneTheme(still = true) { QueueScreen(st.copy(sessions = listOf(q.copy(question = p))), st.ts + 60, { none }, {}) } }
    }
    @Test fun permesso_esplicito_412() = permesso(412, 1f, "Publish the docs: git push origin main")
    @Test fun permesso_esplicito_320_grande() = permesso(320, 1.3f, longCmd)
    @Test fun permesso_esplicito_relayVecchio() = permesso(412, 1f, null)

    @Test fun queueEmpty() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { QueueScreen(st.copy(sessions = st.sessions.map { it.copy(question = null) }), st.ts, { none }, {}) }
    }
}
