package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.platform.LocalContext

/**
 * Il puntatore del mouse su tablet e Chromebook (Franz, 05/10 10:13: restava sempre la freccia): la mano su ciò che si
 * tocca, le frecce sui bordi che si trascinano, la presa sulle testate che si spostano.
 */
fun Modifier.handCursor(): Modifier = pointerHoverIcon(PointerIcon.Hand)

fun Modifier.resizeCursor(): Modifier = systemCursor(android.view.PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW)

fun Modifier.grabCursor(): Modifier = systemCursor(android.view.PointerIcon.TYPE_GRAB)

private fun Modifier.systemCursor(type: Int): Modifier = composed {
    val ctx = LocalContext.current
    pointerHoverIcon(remember(type) { PointerIcon(android.view.PointerIcon.getSystemIcon(ctx, type)) })
}

/** Il tasto destro del mouse (tablet e Chromebook): apre il menu contestuale come la pressione lunga (Franz, 14/10 20:58). */
fun Modifier.contextClick(onContext: () -> Unit): Modifier = pointerInput(onContext) {
    awaitPointerEventScope {
        while (true) {
            val e = awaitPointerEvent()
            if (e.type == androidx.compose.ui.input.pointer.PointerEventType.Press && e.buttons.isSecondaryPressed) {
                e.changes.forEach { it.consume() }
                onContext()
            }
        }
    }
}
