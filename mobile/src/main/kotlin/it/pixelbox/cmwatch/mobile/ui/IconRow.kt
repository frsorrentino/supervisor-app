package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.rules.IconRowRules
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Una scheda della riga di icone: [icon] e [count] sempre, il nome [label] solo sulla scheda aperta; [tone] è il colore del numero. */
data class IconTab(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val count: Int = 0,
    val tone: IconRowRules.Tone = IconRowRules.Tone.COUNT,
)

private val RowShape = RoundedCornerShape(25.dp)
private val NameStyle get() = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
private val CountStyle get() = TextStyle(fontFamily = V3.Mono, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, fontFeatureSettings = "tnum")

/**
 * La riga di icone (app-posizioni-sistema-27-11, gruppo 1) con la pillola elastica del Rubber dock (app-dock-elastico-27-11, Franz
 * 27/11 20:46), come IconRow.svelte e riga-elastica.html della master. Ogni scheda ha icona e numero; quella aperta anche il nome, che
 * si apre con la molla 300/26 mentre quello della lasciata si chiude. La pillola ha due bordi su due molle (IconRowRules.edges):
 * davanti rigido (--dock-lead 640/38), dietro pigro (--dock-trail 150/16), così si allunga fra le schede; col dito (1100/56 e 320/26) si
 * trascina, e oltre la prima e l'ultima scheda tira come un elastico (rubber di dock.js) con la riga che segue al 35 % (300/18).
 * L'icona scelta fa un piccolo salto (--dock-pop 520/16, spinta -3,2). Posto e forma della pillola li decide solo IconRowRules.pill,
 * sulla meta di IconRowRules.target. Con le animazioni spente, e nelle immagini di prova, tutto al suo posto e niente salto.
 *
 * [onPick] arriva a ogni tocco, anche sulla scheda aperta (decide chi chiama: la apre o chiude la tendina), e al rilascio del dito su
 * un'altra scheda. [onEsc]: Esc dalla tastiera, torna alla conversazione. Le schede non cambiano mai ordine né posto.
 *
 * Stretta (spazi di 7 e icone da 20) quando la riga riceve al massimo 400 dp (IconRowRules.tight): la larghezza che le arriva, non lo
 * schermo, così sta anche nelle colonne strette del tablet (app-posizioni-sistema-27-11 gruppo 6, come la container query della web).
 */
@Composable
fun IconRow(
    tabs: List<IconTab>,
    active: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEsc: (() -> Unit)? = null,
    /** Alt+1…5 ed Esc della riga; false quando li gestisce chi la usa (app-barra-ibrida-28-11). */
    keys: Boolean = true,
    /**
     * app-barra-ibrida-28-11 (H1): dentro la capsula, dopo le schede, lo stato della sessione. Il dito che parte da qui non trascina la
     * pillola; i suoi tocchi sono suoi.
     */
    trailing: (@Composable () -> Unit)? = null,
) = BoxWithConstraints(modifier) {
    val off = animationsOff()
    val u = LocalDensity.current.density
    val tight = IconRowRules.tight(maxWidth.value)
    // La scheda sotto il dito mentre trascina: pillola e nome vanno lì, la scelta arriva al rilascio.
    var drag by remember { mutableStateOf<String?>(null) }
    val shown = drag ?: active
    // Il tratto di ogni scheda nella riga, in px, scritto a ogni posa: la meta della pillola segue la scheda anche mentre si allarga.
    val spans = remember { mutableStateMapOf<String, IconRowRules.Span>() }
    var extra by remember { mutableFloatStateOf(0f) }
    var tune by remember { mutableStateOf(IconRowRules.edges(0, drag = false)) }
    // Quando la pillola cambia scheda guida il bordo verso cui va: lo scrive la composizione insieme alla scheda, in un passo solo.
    val last = remember { arrayOfNulls<String>(1) }
    if (last[0] != shown) {
        if (last[0] != null) {
            val from = tabs.indexOfFirst { it.key == last[0] }
            tune = IconRowRules.edges(IconRowRules.direction(from, tabs.indexOfFirst { it.key == shown }), drag = drag != null)
        }
        last[0] = shown
    }
    val shownNow = rememberUpdatedState(shown)
    val tabsNow by rememberUpdatedState(tabs)
    val activeNow by rememberUpdatedState(active)
    val pickNow by rememberUpdatedState(onPick)
    val edgeL = remember { Animatable(0f) }
    val edgeR = remember { Animatable(0f) }
    val nudge = remember { Animatable(0f) }
    var ready by remember { mutableStateOf(false) }

    // I due bordi verso la meta, ognuno con la sua molla; a ogni passo della meta ripartono da dove sono, con la loro velocità.
    LaunchedEffect(off) {
        if (off) { ready = false; return@LaunchedEffect }
        snapshotFlow { IconRowRules.target(shownNow.value?.let { spans[it] }, extra) to tune }.collectLatest { (goal, springs) ->
            if (goal == null) return@collectLatest
            if (!ready) { edgeL.snapTo(goal.left); edgeR.snapTo(goal.right); ready = true; return@collectLatest }
            coroutineScope {
                launch { edgeL.animateTo(goal.left, springs.first.spec()) }
                launch { edgeR.animateTo(goal.right, springs.second.spec()) }
            }
        }
    }
    LaunchedEffect(off) {
        snapshotFlow { extra * IconRowRules.FOLLOW }.collectLatest { t ->
            if (off) nudge.snapTo(t) else nudge.animateTo(t, IconRowRules.ROW.spec())
        }
    }

    // Al rilascio la pillola si posa da dov'è; se chi chiama non prende la scheda, la composizione la riporta su quella aperta.
    fun release(commit: Boolean) {
        val back = IconRowRules.settle(extra)
        extra = 0f
        val k = drag
        drag = null
        if (commit && k != null && k != activeNow) pickNow(k)
        tune = IconRowRules.edges(back, drag = false)
    }

    val pillColor = V3.S3
    val rimColor = V3.Tx.copy(alpha = 0.12f)
    Row(
        Modifier
            .graphicsLayer { translationX = nudge.value }
            .background(V3.S2, RowShape)
            .border(1.dp, V3.Line, RowShape)
            .padding(5.dp)
            .drawBehind {
                val goal = IconRowRules.target(shownNow.value?.let { spans[it] }, extra) ?: return@drawBehind
                val moving = !off && ready
                val p = IconRowRules.pill(
                    left = if (moving) edgeL.value else goal.left, right = if (moving) edgeR.value else goal.right, goal = goal,
                    height = IconRowRules.PILL_H * u, minWidth = IconRowRules.PILL_MIN_W * u,
                )
                val top = (size.height - IconRowRules.PILL_H * u) / 2f + p.top
                drawRoundRect(color = pillColor, topLeft = Offset(p.x, top), size = Size(p.width, p.height), cornerRadius = CornerRadius(p.height / 2f))
                val sw = 1.dp.toPx()
                drawRoundRect(
                    color = rimColor, topLeft = Offset(p.x + sw / 2f, top + sw / 2f), size = Size(p.width - sw, p.height - sw),
                    cornerRadius = CornerRadius((p.height - sw) / 2f), style = Stroke(sw),
                )
            }
            .pointerInput(Unit) {
                // Solo il dito che parte sulle schede trascina la pillola; dallo stato (trailing) no.
                var fromTabs = true
                detectHorizontalDragGestures(
                    onDragStart = { o -> fromTabs = tabsNow.mapNotNull { t -> spans[t.key] }.maxOfOrNull { s -> s.right }?.let { r -> o.x <= r } ?: true },
                    onDragEnd = { if (fromTabs) release(commit = true) },
                    onDragCancel = { if (fromTabs) release(commit = false) },
                    onHorizontalDrag = move@{ change, dx ->
                        if (!fromTabs) return@move
                        change.consume()
                        val list = tabsNow
                        val ss = list.map { spans[it.key] ?: return@move }
                        // Il punto nella riga com'è nel layout, senza lo spostamento dell'elastico.
                        val x = change.position.x + nudge.value
                        extra = IconRowRules.stretch(x, ss, IconRowRules.BAND * u)
                        val cur = list.indexOfFirst { it.key == (drag ?: activeNow) }
                        val j = IconRowRules.under(ss, x, cur, IconRowRules.SLACK * u)
                        if (j >= 0 && j != cur) drag = list[j].key
                        if (abs(dx) > 0.5f) tune = IconRowRules.edges(if (dx > 0f) 1 else -1, drag = true)
                    },
                )
            }
            .then(if (keys) Modifier.iconRowKeys(tabs.map { it.key }, onPick, onEsc) else Modifier)
            .selectableGroup()
            .testTag("riga-icone"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            androidx.compose.runtime.key(tab.key) {
                IconRowTab(
                    tab = tab, selected = tab.key == active, shown = tab.key == shown, off = off, tight = tight,
                    onClick = { pickNow(tab.key) },
                    onSpan = { s -> if (spans[tab.key] != s) spans[tab.key] = s },
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * app-barra-ibrida-28-11: quanto sono larghe le schede a riposo, in dp, col nome più lungo aperto (padding della capsula escluso): le
 * parti fisse di ogni scheda (icona, margini, numero), il nome più largo con lo spazio davanti, gli spazi fra le schede. Serve a ridurre lo
 * stato che sta nella stessa capsula (StatusBar.step) senza che cambi aprendo e chiudendo le schede. [tight]: come la riga, sotto i 400 dp.
 */
@Composable
internal fun iconRowRest(tabs: List<IconTab>, tight: Boolean): Float {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = LocalDensity.current
    val pad = if (tight) 7f else 11f
    val icon = if (tight) 20f else 22f
    return with(density) {
        var fixed = 0f
        var name = 0f
        tabs.forEach { t ->
            val count = IconRowRules.badge(t.count, t.tone)?.let { b -> 5f + measurer.measure(b.text, CountStyle).size.width.toDp().value } ?: 0f
            fixed += pad * 2f + icon + count
            name = maxOf(name, measurer.measure(t.label, NameStyle).size.width.toDp().value)
        }
        fixed + name + IconRowRules.NAME_GAP + 2f * (tabs.size - 1).coerceAtLeast(0)
    }
}

@Composable
private fun IconRowTab(tab: IconTab, selected: Boolean, shown: Boolean, off: Boolean, tight: Boolean, onClick: () -> Unit, onSpan: (IconRowRules.Span) -> Unit) {
    // Il nome, da 0 chiuso a 1 aperto, con la molla 300/26.
    val open = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(shown, off) {
        val t = if (shown) 1f else 0f
        if (off) open.snapTo(t) else open.animateTo(t, IconRowRules.NAME.spec())
    }
    // Il salto dell'icona quando la pillola arriva: da ferma, con la spinta di dock.js sulla molla 520/16.
    val pop = remember { Animatable(1f) }
    val was = remember { booleanArrayOf(shown) }
    LaunchedEffect(shown) {
        val jump = shown && !was[0] && !off
        was[0] = shown
        if (jump) pop.animateTo(1f, IconRowRules.POP.spec(), initialVelocity = pop.velocity + IconRowRules.PUSH)
    }
    val color by animateColorAsState(if (shown) V3.Tx else V3.Tx2, if (off) snap() else tween(250), label = "riga-icone-colore")
    val badge = IconRowRules.badge(tab.count, tab.tone)
    val gap = with(LocalDensity.current) { IconRowRules.NAME_GAP.dp.toPx() }
    Row(
        Modifier
            .onPlaced { c -> val x = c.positionInParent().x; onSpan(IconRowRules.Span(x, x + c.size.width)) }
            .height(IconRowRules.PILL_H.dp)
            .selectable(selected = selected, interactionSource = null, indication = null, role = Role.Tab, onClick = onClick)
            .handCursor()
            .testTag("riga-icone-${tab.key}")
            .padding(horizontal = if (tight) 7.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            tab.icon, contentDescription = null, tint = color,
            modifier = Modifier.size(if (tight) 20.dp else 22.dp).graphicsLayer { val s = pop.value; scaleX = s; scaleY = s },
        )
        Text(tab.label, style = NameStyle, color = color, maxLines = 1, softWrap = false, modifier = Modifier.reveal(gap) { open.value })
        if (badge != null) {
            Text(badge.text, style = CountStyle, color = toneColor(badge.tone), maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 5.dp))
        }
    }
}

/** Una molla della master in Compose: spring(dampingRatio = c / 2√k, stiffness = k), la stessa equazione con massa 1. */
private fun IconRowRules.Spring.spec(): FiniteAnimationSpec<Float> = spring(dampingRatio = ratio(), stiffness = k)

private fun toneColor(t: IconRowRules.Tone) = when (t) {
    IconRowRules.Tone.WAIT -> V3.Wait
    IconRowRules.Tone.NEW -> V3.Good
    IconRowRules.Tone.COUNT -> V3.Tx2
}

/** Il nome che si apre: largo quanto la sua parte aperta [f] più lo spazio [gap] davanti, ritagliato; compare dopo un terzo della corsa. */
private fun Modifier.reveal(gap: Float, f: () -> Float): Modifier = clipToBounds().layout { m, c ->
    val p = m.measure(c.copy(minWidth = 0, maxWidth = Constraints.Infinity))
    val v = f()
    val w = ((p.width + gap) * v).roundToInt().coerceIn(c.minWidth, c.maxWidth)
    layout(w, p.height) { p.placeWithLayer((IconRowRules.nameGap(v) * gap).roundToInt(), 0) { alpha = IconRowRules.nameAlpha(v) } }
}

/**
 * Alt+1…5 sceglie la scheda ed Esc torna alla conversazione (proposta, «Tastiera fisica»), sul tablet e sul telefono con la tastiera.
 * La riga lo fa da sé col fuoco su una scheda; chi la ospita lo mette anche sullo schermo intorno, perché il fuoco di solito è nel campo.
 */
fun Modifier.iconRowKeys(keys: List<String>, onPick: (String) -> Unit, onEsc: (() -> Unit)? = null): Modifier = onPreviewKeyEvent { e ->
    if (e.type != KeyEventType.KeyDown || e.isCtrlPressed || e.isMetaPressed) return@onPreviewKeyEvent false
    if (e.key == Key.Escape) {
        if (onEsc == null || e.isAltPressed || e.isShiftPressed) return@onPreviewKeyEvent false
        onEsc()
        return@onPreviewKeyEvent true
    }
    if (!e.isAltPressed) return@onPreviewKeyEvent false
    val digit = when (e.key) {
        Key.One -> 1
        Key.Two -> 2
        Key.Three -> 3
        Key.Four -> 4
        Key.Five -> 5
        else -> 0
    }
    val i = IconRowRules.altDigit(digit, keys.size) ?: return@onPreviewKeyEvent false
    onPick(keys[i])
    true
}
