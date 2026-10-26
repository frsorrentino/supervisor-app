package it.pixelbox.cmwatch.mobile

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.SnapshotHandler
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.SessionSheet
import it.pixelbox.cmwatch.mobile.ui.SheetActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * sugg-focus (Franz, 26/10 19:56): un tocco per entrare nel campo vuoto trasformava il suggerimento attenuato in testo.
 * Il suggerimento resta un'indicazione; lo mette nel campo solo «Usa». Paparazzi fa da motore dei fotogrammi, come in
 * ChatScrollTest: il tocco è un MotionEvent vero sul campo, nel punto in cui sta il suggerimento.
 */
class SuggFocusTest {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar",
        snapshotHandler = silent(SnapshotHandler::class.java),
    )
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

    @Test fun suggFocusATapInTheEmptyFieldLeavesItEmpty() {
        val sug = "Committa le modifiche, apri la PR su GitHub e aspetta che la CI torni verde"
        val s = st.sessions.first { it.state == SessionState.IDLE && it.question == null }.copy(suggestion = sug)
        val draft = mutableStateOf("")
        var focused = false
        var tapped = false
        lateinit var view: ComposeView
        view = ComposeView(paparazzi.context).apply {
            setContent {
                CmPhoneTheme(still = true) { SessionSheet(s, st.ts, emptyList(), 120, none, draftState = draft) }
                LaunchedEffect(Unit) {
                    repeat(10) { withFrameNanos {} }
                    val root = view.getChildAt(0)
                    val field = field((root as ViewRootForTest).semanticsOwner.unmergedRootSemanticsNode)
                    val b = field.boundsInRoot
                    // A un terzo della larghezza: sul testo del suggerimento, lontano dal + e da «Usa».
                    tap(root, b.left + b.width / 3, b.center.y)
                    tapped = true
                    repeat(10) { withFrameNanos {} }
                    focused = field(root.semanticsOwner.unmergedRootSemanticsNode).config.getOrNull(SemanticsProperties.Focused) == true
                }
            }
        }
        paparazzi.gif(view, end = 1000, fps = 60)
        assertTrue("sugg-focus: il tocco non è partito", tapped)
        assertTrue("sugg-focus: il tocco non è arrivato al campo", focused)
        assertEquals("sugg-focus: il tocco nel campo vuoto ha messo il suggerimento nel campo", "", draft.value)
    }

    private fun field(n: SemanticsNode): SemanticsNode =
        if (SemanticsProperties.EditableText in n.config) n else n.children.firstNotNullOf { runCatching { field(it) }.getOrNull() }

    private fun tap(v: View, x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        v.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0))
        v.dispatchTouchEvent(MotionEvent.obtain(t, t + 40, MotionEvent.ACTION_UP, x, y, 0))
    }

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
