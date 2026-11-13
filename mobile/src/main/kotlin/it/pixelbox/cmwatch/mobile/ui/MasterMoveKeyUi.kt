package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterMove

/** L'anteprima dell'arrivo, data dal telaio della master (MasterFrame): la freccia scelta (null = nessuna), tutte le possibili, da dove. */
internal val LocalMasterKeyPreview = staticCompositionLocalOf<((MasterMove.Move?, List<MasterMove.Move>, MasterMove.Spot) -> Unit)?> { null }

/**
 * Il tasto tondo della master sul tablet (app-sposta-master-a-13-11; mockup giro E e giro F variante A): a riposo il chevron, che
 * apre e chiude. Col dito: tocco corto apre e chiude; tenuto premuto, il trascinamento sceglie la direzione, nel tasto compare la
 * freccia e il rilascio sposta; rilasciato sul tasto annulla. Col mouse: la freccia della direzione possibile dalla parte del tasto
 * dove sta il puntatore, il chevron al centro; il clic sposta. Tastiera: le frecce (anche due insieme), Invio, Esc. Mentre si
 * sceglie il telaio accende la zona d'arrivo col suo nome e vela gli altri posti possibili.
 */
@Composable
fun MasterMoveKey(now: MasterMove.Spot, sessionsOpen: Boolean, up: Boolean, onToggle: () -> Unit, onMove: (MasterMove.Move) -> Unit) {
    val all = remember(now, sessionsOpen) { MasterMove.moves(now, sessionsOpen) }
    val preview = LocalMasterKeyPreview.current
    var dir by remember { mutableStateOf<MasterMove.Dir?>(null) }
    val allNow by rememberUpdatedState(all); val nowNow by rememberUpdatedState(now)
    val toggle by rememberUpdatedState(onToggle); val move by rememberUpdatedState(onMove); val previewNow by rememberUpdatedState(preview)
    fun moveOf(d: MasterMove.Dir?) = allNow.firstOrNull { it.dir == d }
    fun show(d: MasterMove.Dir?) {
        val m = moveOf(d)
        if (m?.dir == dir) return
        dir = m?.dir
        previewNow?.invoke(m, allNow, nowNow)
    }
    fun done() { if (dir != null) { dir = null; previewNow?.invoke(null, allNow, nowNow) } }
    fun go(): Boolean { val m = moveOf(dir) ?: return false; dir = null; previewNow?.invoke(null, allNow, nowNow); move(m); return true }
    val haptic = LocalHapticFeedback.current
    val held = remember { mutableListOf<MasterMove.Dir>() }
    val label = stringResource(if (up) R.string.dock_conversation else R.string.dock_collapse)
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(if (dir != null) V3.Wait else MasterKey).testTag("master-tasto")
            .semantics { role = Role.Button; contentDescription = label; onClick { toggle(); true } }
            .onKeyEvent { e ->
                val k = when (e.key) { Key.DirectionUp -> MasterMove.Dir.N; Key.DirectionDown -> MasterMove.Dir.S; Key.DirectionLeft -> MasterMove.Dir.W; Key.DirectionRight -> MasterMove.Dir.E; else -> null }
                when {
                    k != null && e.type == KeyEventType.KeyDown -> { if (k !in held) held += k; show(MasterMove.combo(held)); true }
                    k != null -> { held -= k; true }
                    e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.NumPadEnter || e.key == Key.Spacebar) -> { if (!go()) toggle(); true }
                    e.type == KeyEventType.KeyDown && e.key == Key.Escape && dir != null -> { done(); true }
                    else -> false
                }
            }
            .focusable()
            // Il mouse: il passaggio dentro il tasto sceglie.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: continue
                        if (c.type != PointerType.Mouse) continue
                        when (ev.type) {
                            PointerEventType.Exit -> done()
                            PointerEventType.Move, PointerEventType.Enter ->
                                show(MasterMove.dirAt(c.position.x - size.width / 2f, c.position.y - size.height / 2f, MasterMove.DEAD_MOUSE.dp.toPx(), allNow.map { it.dir }))
                            else -> Unit
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    if (down.type == PointerType.Mouse) {
                        // Il clic: sposta se c'è la freccia, altrimenti apre e chiude.
                        val upc = waitForUpOrCancellation() ?: return@awaitEachGesture
                        upc.consume()
                        if (!go()) toggle()
                        return@awaitEachGesture
                    }
                    // Il dito: entro 450 ms è un tocco; oltre, il trascinamento sceglie.
                    var cancelled = false
                    val early = withTimeoutOrNull(450) { waitForUpOrCancellation().also { if (it == null) cancelled = true } }
                    if (cancelled) return@awaitEachGesture
                    if (early != null) { early.consume(); toggle(); return@awaitEachGesture }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) { c.consume(); if (!go()) done(); break }
                        show(MasterMove.dirAt(c.position.x - size.width / 2f, c.position.y - size.height / 2f, MasterMove.DEAD_FINGER.dp.toPx(), allNow.map { it.dir }))
                        c.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val d = dir
        if (d != null) Icon(Icons.Rounded.ArrowUpward, null, tint = V3.Bg, modifier = Modifier.size(26.dp).rotate(d.deg + 90f))
        else Icon(if (up) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Cm.text, modifier = Modifier.size(26.dp))
    }
}
