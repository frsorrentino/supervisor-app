package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * Voce 3, la parte Fan cards dell'originale (app-animazioni-fedeli-26-11; Franz 26/11 20:38: «Ventaglio con pressione lunga»): una
 * pressione lunga sulla pila chiusa di «Per te» apre le carte a ventaglio dal loro bordo in basso, con una vibrazione breve: ognuna va
 * di lato di 78 dp per livello, ruota di 12° e le esterne scendono di 12 dp (molla 220 18); la carta sotto il dito sale di 22 dp e
 * scala a 1,06 (molle 420 20 e 420 22) e passa davanti. Al rilascio tornano nella pila. Da ferme le carte stanno come nella pila
 * (Notification stack: 10 dp più giù e il 5 % più piccole per livello). Animazioni spente: niente ventaglio, la pressione lunga apre
 * la lista ([onPlain]).
 */
@Composable
internal fun FanPile(titles: List<String>, fill: Color, line: Color, off: Boolean, onPlain: () -> Unit, front: @Composable (fanning: Boolean) -> Unit) {
    val n = minOf(titles.size, 2)
    val f = remember { Animatable(0f) }
    val lifts = remember { List(3) { Animatable(0f) } }
    val scales = remember { List(3) { Animatable(1f) } }
    var hot by remember { mutableIntStateOf(-1) }
    var w by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val plain by rememberUpdatedState(onPlain)
    val d = LocalDensity.current
    // La carta davanti è al centro (d = 0); la prima sotto va a sinistra (d = -1), la seconda a destra (d = +1).
    val dOf = { i: Int -> when (i) { 1 -> -1; 2 -> 1; else -> 0 } }
    fun setHot(i: Int) {
        if (i == hot) return
        hot = i
        for (k in 0..n) {
            scope.launch { lifts[k].animateTo(if (k == i) -22f else 0f, Molla.of(420f, 20f)) }
            scope.launch { scales[k].animateTo(if (k == i) 1.06f else 1f, Molla.of(420f, 22f)) }
        }
    }
    val gesture = Modifier.pointerInput(n, off) {
        if (n == 0) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val early = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                    if (!c.pressed || (c.position - down.position).getDistance() > viewConfiguration.touchSlop) return@withTimeoutOrNull true
                }
            }
            if (early != null) return@awaitEachGesture
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (off) {
                plain()
                while (true) { val e = awaitPointerEvent(PointerEventPass.Initial); e.changes.forEach { it.consume() }; if (e.changes.none { it.pressed }) break }
                return@awaitEachGesture
            }
            scope.launch { f.animateTo(1f, Molla.of(220f, 18f)) }
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                e.changes.forEach { it.consume() }
                val c = e.changes.firstOrNull { it.id == down.id } ?: break
                if (!c.pressed) break
                // La carta sotto il dito: quella col centro più vicino, a 78 dp per livello dal centro della pila.
                val dx = (c.position.x - size.width / 2f) / with(d) { 78.dp.toPx() }
                val near = (0..n).minByOrNull { k -> abs(dOf(k) - dx) } ?: -1
                setHot(near)
            }
            setHot(-1)
            scope.launch { f.animateTo(0f, Molla.of(220f, 18f)) }
        }
    }
    val fanning = f.value > 0.001f || f.isRunning
    Box(gesture, contentAlignment = Alignment.TopCenter) {
        if (fanning) for (k in n downTo 1) {
            val dd = dOf(k)
            Box(
                Modifier.zIndex(if (k == hot) 5f else 2f).graphicsLayer {
                    val x = f.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    translationX = dd * x * 78.dp.toPx()
                    rotationZ = dd * x * 12f
                    translationY = (1f - x) * 10.dp.toPx() * k + x * abs(dd) * 12.dp.toPx() + lifts[k].value.dp.toPx()
                    val s = ((1f - x) * (1f - 0.05f * k) + x) * scales[k].value
                    scaleX = s; scaleY = s
                }.then(if (w > 0) Modifier.size(with(d) { w.toDp() }, 40.dp) else Modifier.heightIn(min = 40.dp))
                    .clip(CircleShape).background(fill).border(1.dp, line, CircleShape).padding(horizontal = 13.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(titles[k - 1], style = V3.Small.copy(fontSize = 13.sp), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) }
        }
        Box(
            Modifier.zIndex(if (hot == 0) 5f else 3f).onSizeChanged { w = it.width }.graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                translationY = lifts[0].value.dp.toPx()
                scaleX = scales[0].value; scaleY = scales[0].value
            },
        ) { front(fanning) }
    }
}
