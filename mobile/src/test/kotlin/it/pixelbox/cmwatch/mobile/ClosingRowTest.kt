package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Outcome
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.SummaryList
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * chiusura-testo (Franz, 27/10 13:23, screenshot): nella card di una sessione finita che si sta chiudendo, il segno
 * «studio-example-com si sta chiudendo» stava accanto a «● aperta · ha finito il compito alle 13:07» e lo stato andava a
 * capo ogni due-tre lettere. Il segno va su una riga sua, sotto lo stato.
 */
private object Closing {
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    const val NAME = "studio-example-com"
    val state: State = st.copy(sessions = st.sessions.map {
        if (it.name != "field-notes") it
        else it.copy(name = NAME, state = SessionState.IDLE, finished = true, since = st.ts - 3600,
            outcome = Outcome("Main pushato, repo e server allineati", "Main pushato, repo e server allineati", st.ts - 3600))
    })

    @Composable fun Page() = CmPhoneTheme(still = true) {
        Column(Modifier.background(CmColors.bg)) {
            SummaryList(
                Summary.build(state, emptyList(), emptyList(), state.ts, ZoneId.of("Europe/Rome"), emptySet()),
                {}, { _, _ -> }, { _, _ -> }, {}, {}, canExit = true, closing = NAME, now = state.ts,
            )
        }
    }

    fun config(dp: Int) = DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = (dp * 2.75).toInt(), screenHeight = 2800)
}

class ClosingRowTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = Closing.config(320), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )

    // Alla larghezza più stretta: il segno comincia sotto la fine dello stato, mai accanto.
    @Test fun closingSignSitsBelowTheStatusLine() {
        var status: Rect? = null
        var sign: Rect? = null
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                Closing.Page()
                LaunchedEffect(Unit) {
                    repeat(10) { withFrameNanos {} }
                    val root = (view.getChildAt(0) as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode
                    status = find(root, "ha finito il compito alle")?.let(::box)
                    sign = find(root, "${Closing.NAME} si sta chiudendo")?.let(::box)
                }
            }
        }
        paparazzi.gif(view, end = 500, fps = 60)
        assertNotNull("chiusura-testo: la riga di stato non c'è", status)
        assertNotNull("chiusura-testo: il segno «si sta chiudendo» non c'è", sign)
        assertTrue("chiusura-testo: il segno sta accanto allo stato ($status, $sign)", sign!!.top >= status!!.bottom)
    }

    private fun find(n: SemanticsNode, text: String): SemanticsNode? =
        all(n).firstOrNull { x -> x.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text in it.text } }

    // Da posizione e misura: lo stato schiacciato è largo 0, e `boundsInRoot` di un nodo largo 0 vale tutto zero.
    private fun box(n: SemanticsNode) = Rect(n.positionInRoot, androidx.compose.ui.geometry.Size(n.size.width.toFloat(), n.size.height.toFloat()))

    private fun all(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap { all(it) }

    // Fotogrammi buttati, come in ChatScrollTest: un `SnapshotHandler` scritto qui nominerebbe `BufferedImage`.
    private fun <T> silent(type: Class<T>): T = type.cast(
        java.lang.reflect.Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, _ ->
            when (m.name) {
                "newFrameHandler" -> silent(SnapshotHandler.FrameHandler::class.java)
                "hashCode" -> 0
                "equals" -> false
                "toString" -> "silent"
                else -> null
            }
        },
    )
}

/** Le stesse card in immagine, a 320, 360 e 412 dp come per Utilizzo (golden da CI). */
class ClosingRowShotsTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = Closing.config(360), theme = "android:Theme.Material.NoActionBar")

    private fun at(dp: Int) {
        paparazzi.unsafeUpdateConfig(deviceConfig = Closing.config(dp))
        paparazzi.snapshot { Closing.Page() }
    }

    @Test fun closing320() = at(320)
    @Test fun closing360() = at(360)
    @Test fun closing412() = at(412)
}
