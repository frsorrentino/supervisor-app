package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import kotlin.math.sqrt
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import it.pixelbox.cmwatch.rules.TieniPremuto
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Animazioni
import it.pixelbox.cmwatch.rules.PassiProva
import kotlinx.coroutines.launch

/**
 * Le molle di designbookmark.com/motion (shared/motion.js: massa 1, a = -k(x - t) - c·v), per le animazioni fedeli
 * (app-animazioni-fedeli-26-11): in Compose `spring(stiffness = k, dampingRatio = c / 2√k)`, la stessa equazione.
 * Con le animazioni spente (e nelle immagini di prova) salta alla meta.
 */
internal object Molla {
    fun of(k: Float, c: Float): FiniteAnimationSpec<Float> = spring(stiffness = k, dampingRatio = c / (2f * sqrt(k)))
}

@Composable
private fun molla(target: Float, k: Float, c: Float, label: String): Float {
    val off = animationsOff()
    return animateFloatAsState(target, if (off) snap() else Molla.of(k, c), label = label).value
}

/**
 * Copia fedele all'originale (Copy Button): al tocco l'icona copia si rimpicciolisce a metà e gira di -40°, la spunta entra da 0,4
 * con un piccolo rimbalzo, la parola sale da «Copia» a «Copiato» e il tasto si allarga alla parola nuova; dopo
 * [Animazioni.COPY_HOLD_MS] torna. Molle dell'originale: rullo 260 20, icone 420 18, larghezza 300 24. Ogni tocco riparte da capo.
 */
@Composable
private fun rememberCopied(): Pair<Boolean, () -> Unit> {
    var taps by remember { mutableIntStateOf(0) }
    var on by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        on = true
        kotlinx.coroutines.delay(Animazioni.COPY_HOLD_MS)
        on = false
    }
    return on to { taps++ }
}

/** Le due icone sovrapposte: [i] è la molla 420 18, da 0 (copia) a 1 (spunta), libera fra -0,2 e 1,2 come nell'originale. */
@Composable
private fun CopyIcons(i0: Float, tint: Color, size: Dp) {
    val i = i0.coerceIn(-0.2f, 1.2f)
    val ic = i.coerceIn(0f, 1f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.ContentCopy, null, tint = tint,
            modifier = Modifier.size(size).graphicsLayer {
                alpha = (1f - i * 1.6f).coerceIn(0f, 1f); scaleX = 1f - 0.5f * ic; scaleY = 1f - 0.5f * ic; rotationZ = -40f * ic
            },
        )
        Icon(
            Icons.Rounded.Check, null, tint = V3.Good,
            modifier = Modifier.size(size).graphicsLayer { alpha = (i * 1.6f - 0.4f).coerceIn(0f, 1f); scaleX = 0.4f + 0.6f * i; scaleY = 0.4f + 0.6f * i },
        )
    }
}

/** Il tasto Copia a icona, sotto una risposta o un messaggio. */
@Composable
internal fun CopyIconAction(text: String) {
    val clip = LocalClipboardManager.current
    val (on, tap) = rememberCopied()
    val label = stringResource(if (on) R.string.copied_word else R.string.copy)
    val i = molla(if (on) 1f else 0f, 420f, 18f, "copia-icone")
    IconButton(
        onClick = { clip.setText(AnnotatedString(text)); tap() },
        modifier = Modifier.size(36.dp).testTag("copia").semantics { contentDescription = label },
    ) { CopyIcons(i, Cm.text2, 18.dp) }
}

/** Il tasto Copia a parole, dentro un testo da mandare o un comando: icone 16 dp e la parola che sale, larga quanto serve. */
@Composable
internal fun CopyPill(copy: () -> String) {
    val clip = LocalClipboardManager.current
    val (on, tap) = rememberCopied()
    val t = if (on) 1f else 0f
    val r = molla(t, 260f, 20f, "copia-rullo")
    val i = molla(t, 420f, 18f, "copia-icone")
    val w = molla(t, 300f, 24f, "copia-larghezza")
    val style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val label = stringResource(if (on) R.string.copied_word else R.string.copy)
    Row(
        Modifier.clip(CircleShape).border(1.dp, V3.Line, CircleShape).handCursor()
            .clickable { clip.setText(AnnotatedString(copy())); tap() }
            .padding(start = 10.dp, end = 12.dp, top = 4.dp, bottom = 4.dp).testTag("copia").semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CopyIcons(i, V3.Tx, 16.dp)
        androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
        Layout(
            content = {
                Text(stringResource(R.string.copy), style = style, color = V3.Tx, maxLines = 1, softWrap = false)
                Text(stringResource(R.string.copied_word), style = style, color = V3.Good, maxLines = 1, softWrap = false)
            },
            modifier = Modifier.clipToBounds(),
        ) { ms, cs ->
            val a = ms[0].measure(cs.copy(minWidth = 0, maxWidth = androidx.compose.ui.unit.Constraints.Infinity))
            val b = ms[1].measure(cs.copy(minWidth = 0, maxWidth = androidx.compose.ui.unit.Constraints.Infinity))
            val h = a.height
            val width = (a.width + (b.width - a.width) * w).roundToInt().coerceAtLeast(0)
            layout(width, h) {
                val dy = (-h * r).roundToInt()
                a.placeRelative(0, dy)
                b.placeRelative(0, h + dy)
            }
        }
    }
}

/**
 * Voce 3, fedele all'originale (Kokonut Thinking text; app-animazioni-fedeli-26-11): un'onda passa sul tempo di chi lavora, lettera per
 * lettera: opacità fino a 0,3 a metà di 1,6 s, ease-in-out sulle due metà, 70 ms di scarto per lettera. Con le animazioni spente, fermo.
 */
@Composable
internal fun WaveText(text: String, style: TextStyle, color: androidx.compose.ui.graphics.Color, step: Int = 70) {
    if (animationsOff()) { Text(text, style = style, color = color, maxLines = 1); return }
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "al-lavoro")
    val clock by t.animateFloat(
        0f, 1600f,
        androidx.compose.animation.core.infiniteRepeatable(tween(1600, easing = androidx.compose.animation.core.LinearEasing)),
        label = "onda",
    )
    val ease = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val shown = androidx.compose.ui.text.buildAnnotatedString {
        text.forEachIndexed { k, ch ->
            val ph = (((clock - k * step) % 1600f) + 1600f) % 1600f / 1600f
            val half = if (ph < 0.5f) ph * 2f else (1f - ph) * 2f
            pushStyle(androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = 1f - 0.7f * ease.transform(half))))
            append(ch)
            pop()
        }
    }
    Text(shown, style = style, color = color, maxLines = 1)
}

/**
 * Voce 4 (Cifra che scorre): l'ora dell'ultima risposta data da qui. La cifra del tabellone scorre solo se cambia entro [WINDOW_MS]
 * da un gesto; per un aggiornamento dal PC cambia e basta.
 */
object Gesture {
    const val WINDOW_MS = 4000L
    @Volatile private var at = 0L
    fun mark() { at = android.os.SystemClock.uptimeMillis() }
    fun recent(): Boolean = at > 0 && android.os.SystemClock.uptimeMillis() - at < WINDOW_MS
}

/**
 * Voce 4, fedele all'originale (Word roll; app-animazioni-fedeli-26-11): la cifra grande del tabellone, con [roll], cambia lettera per
 * lettera dopo un gesto. Le cifre vecchie salgono via a scalare di 22 ms, le nuove entrano da sotto dopo 60 ms a scalare di 28 ms, tutte
 * con la molla 420 26 (spostamento del 105 % dell'altezza, opacità 1 - 0,9·|y|); la larghezza va alla cifra nuova con la 320 30. Il
 * taglio è solo sopra e sotto. Senza gesto o con le animazioni spente, al posto.
 */
@Composable
internal fun RollNumber(value: String, roll: Boolean, content: @Composable (String) -> Unit) {
    if (!roll) { content(value); return }
    val off = animationsOff()
    class Word(val text: String, val id: Int) { val ys = text.map { androidx.compose.animation.core.Animatable(0f) } }
    var words by remember { androidx.compose.runtime.mutableStateOf(listOf(Word(value, 0))) }
    val wp = remember { androidx.compose.animation.core.Animatable(1f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(value) {
        val cur = words.last()
        if (value == cur.text) return@LaunchedEffect
        val go = !off && Animazioni.rolls(cur.text.toIntOrNull(), value.toIntOrNull() ?: 0, Gesture.recent())
        val new = Word(value, cur.id + 1)
        if (!go) { words = listOf(new); wp.snapTo(1f); return@LaunchedEffect }
        new.ys.forEach { it.snapTo(1f) }
        words = listOf(cur, new)
        wp.snapTo(0f)
        scope.launch { wp.animateTo(1f, Molla.of(320f, 30f)) }
        cur.ys.forEachIndexed { k, a -> scope.launch { kotlinx.coroutines.delay(k * 22L); a.animateTo(-1f, Molla.of(420f, 26f)) } }
        new.ys.forEachIndexed { k, a -> scope.launch { kotlinx.coroutines.delay(60L + k * 28L); a.animateTo(0f, Molla.of(420f, 26f)) } }
        kotlinx.coroutines.delay(1000)
        if (words.lastOrNull() === new) words = listOf(new)
    }
    Layout(
        content = {
            words.forEach { w ->
                androidx.compose.runtime.key(w.id) {
                    Row {
                        w.text.forEachIndexed { k, ch ->
                            Box(Modifier.graphicsLayer {
                                val y = w.ys[k].value
                                translationY = y * 1.05f * size.height
                                alpha = (1f - kotlin.math.abs(y) * 0.9f).coerceIn(0f, 1f)
                            }) { content(ch.toString()) }
                        }
                    }
                }
            }
        },
        modifier = Modifier.drawWithContent { clipRect(left = -size.width, right = size.width * 2f) { this@drawWithContent.drawContent() } },
    ) { ms, cs ->
        val ps = ms.map { it.measure(cs.copy(minWidth = 0)) }
        val to = ps.last()
        val from = ps.first()
        val w = (from.width + (to.width - from.width) * wp.value).roundToInt().coerceAtLeast(0)
        val h = ps.maxOf { it.height }
        val base = to[androidx.compose.ui.layout.FirstBaseline]
        layout(w, h, if (base != androidx.compose.ui.layout.AlignmentLine.Unspecified) mapOf(androidx.compose.ui.layout.FirstBaseline to base) else emptyMap()) {
            ps.forEach { it.placeRelative(0, 0) }
        }
    }
}

/** Il colore del tasto «tieni premuto» (app-tieni-premuto-28-11): rosso per chiudere, il colore del testo, l'ambra dei tasti principali, un tonale grigio. */
internal enum class HoldTone { RED, INK, AMBER, GREY }

/** La forma del tasto: la pillola da 52 larga quanto il posto, un tasto della carta, il tasto di una riga. */
internal enum class HoldForm { PILL, KEY, MINI }

/**
 * Il tasto «tieni premuto» (app-tieni-premuto-28-11, Franz 28/11 16:22, «Tutti e tre»; mockup tieni-premuto-28-11.html), fedele al Hold to
 * publish di designbookmark (hold.js e le molle di motion.js) coi colori, i font e i raggi dell'app; gemello di HoldButton.svelte. Le regole
 * (il tempo di ogni posto, il pieno, lasciato prima o fatto, la scossa) stanno in [TieniPremuto].
 *
 * Tenere premuto ([mode] HOLD): il dito, o Spazio e Invio da una tastiera, schiaccia la pillola a 0,96 (molla 520 20) e la riempie da
 * sinistra, lineare in [holdMs] ([TieniPremuto.progress] del tempo tenuto); la scritta si inverte sotto il pieno. Lasciata prima: il pieno
 * torna con la molla 260 14, appena oltre lo zero, e la pillola fa «no» (molla 700 9, spinta [TieniPremuto.kick]); non parte niente. In
 * fondo: la pillola diventa un cerchio senza cambiare il posto che occupa (molla 320 20), verde, la scritta sfuma e si sfoca, la spunta si
 * disegna (450 ms con (.65, 0, .35, 1) dopo 160 ms); poi parte [onDone].
 *
 * Toccare ([mode] TAP, correzione della master del 28/11): ogni altro «Approva» e ogni «Va» restano a un tocco col finale dello stesso tasto;
 * [onDone] parte al tocco e chi ospita il tasto fa uscire la carta o la riga come prima. Con [on] è lui a dire se il tasto è fatto: se il PC
 * rifiuta lo spegne e la pillola torna.
 *
 * TalkBack legge [a11y] («Tieni premuto per …», se no [label]) e poi l'esito: [said], o «lasciato prima». Animazioni spente: niente
 * schiacciata né scossa, il cerchio senza rimbalzo; il pieno cresce lo stesso, perché dice quanto manca, e la spunta compare senza
 * disegnarsi. [pinned] ferma il pieno (da 0 a 1; 1 = fatto) per le immagini di prova.
 */
@Composable
internal fun HoldButton(
    label: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    mode: TieniPremuto.Mode = TieniPremuto.Mode.HOLD,
    holdMs: Long = TieniPremuto.holdMs(TieniPremuto.Place.CHIUDI),
    tone: HoldTone = HoldTone.AMBER,
    form: HoldForm = HoldForm.PILL,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    a11y: String? = null,
    said: String? = null,
    on: Boolean? = null,
    pinned: Float? = null,
    enabled: Boolean = true,
    tag: String = "tieni-premuto",
) {
    val off = animationsOff()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val fire by androidx.compose.runtime.rememberUpdatedState(onDone)
    val onNow by androidx.compose.runtime.rememberUpdatedState(on)
    val p = remember { androidx.compose.animation.core.Animatable(0f) }
    val sc = remember { androidx.compose.animation.core.Animatable(1f) }
    val shake = remember { androidx.compose.animation.core.Animatable(0f) }
    val morph = remember { androidx.compose.animation.core.Animatable(0f) }
    var mine by remember { androidx.compose.runtime.mutableStateOf(false) }
    var news by remember { androidx.compose.runtime.mutableStateOf("") }
    val holding = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    val touch = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val done = if (pinned != null) pinned >= 1f else on ?: mine
    val early = stringResource(R.string.hold_early)
    // Il cerchio segue lo stato «fatto», di qui o di chi ospita il tasto: avanti con la molla 320 20, indietro con 0,8 della rigidità.
    LaunchedEffect(done, off) {
        val k = if (done) TieniPremuto.MORPH_K else TieniPremuto.MORPH_K * 0.8f
        if (!done && holding[0]?.isActive != true) launch { p.animateTo(0f, Molla.of(TieniPremuto.RELEASE_K, TieniPremuto.damping(TieniPremuto.RELEASE_K, TieniPremuto.RELEASE_C, off))) }
        if (off) morph.snapTo(if (done) 1f else 0f) else morph.animateTo(if (done) 1f else 0f, Molla.of(k, TieniPremuto.damping(k, TieniPremuto.MORPH_C, false)))
    }
    fun finish() {
        mine = true
        news = said.orEmpty()
        if (!off) scope.launch { sc.animateTo(1f, Molla.of(TieniPremuto.PRESS_K, TieniPremuto.PRESS_C), initialVelocity = sc.velocity + 2.5f) }
        scope.launch {
            kotlinx.coroutines.delay(TieniPremuto.FIRE_MS)
            fire()
            kotlinx.coroutines.delay(TieniPremuto.DONE_MS - TieniPremuto.FIRE_MS)
            // Il tasto torna pillola se nessuno l'ha tolto (l'originale dopo 1,7 s).
            if (onNow == null) mine = false
        }
    }
    fun press() {
        if (mode != TieniPremuto.Mode.HOLD || done || !enabled || pinned != null || holding[0]?.isActive == true) return
        news = ""
        if (!off) scope.launch { sc.animateTo(TieniPremuto.SQUASH, Molla.of(TieniPremuto.PRESS_K, TieniPremuto.PRESS_C)) }
        holding[0] = scope.launch {
            // Lineare mentre si tiene, da dove era il pieno: è una barra di avanzamento; la molla riprende quando si lascia.
            val from = p.value.coerceIn(0f, 1f)
            val need = ((1f - from) * holdMs).toLong()
            val t0 = android.os.SystemClock.uptimeMillis()
            while (true) {
                val e = android.os.SystemClock.uptimeMillis() - t0
                p.snapTo(from + (1f - from) * TieniPremuto.progress(e, need))
                if (TieniPremuto.release(e, need) == TieniPremuto.Release.COMPLETE) break
                androidx.compose.runtime.withFrameMillis { }
            }
            holding[0] = null
            finish()
        }
    }
    fun lift() {
        val j = holding[0] ?: return
        j.cancel()
        holding[0] = null
        val kick = TieniPremuto.kick(p.value, off)
        news = early
        scope.launch { if (off) sc.snapTo(1f) else sc.animateTo(1f, Molla.of(TieniPremuto.PRESS_K, TieniPremuto.PRESS_C)) }
        scope.launch { p.animateTo(0f, Molla.of(TieniPremuto.RELEASE_K, TieniPremuto.damping(TieniPremuto.RELEASE_K, TieniPremuto.RELEASE_C, off))) }
        if (kick > 0f) scope.launch { shake.animateTo(0f, Molla.of(TieniPremuto.SHAKE_K, TieniPremuto.SHAKE_C), initialVelocity = shake.velocity + kick) }
    }
    fun tap() {
        if (mode != TieniPremuto.Mode.TAP || done || !enabled || pinned != null) return
        mine = true
        news = said.orEmpty()
        if (!off) scope.launch { sc.animateTo(1f, Molla.of(TieniPremuto.PRESS_K, TieniPremuto.PRESS_C), initialVelocity = sc.velocity + 2.5f) }
        fire()
        scope.launch { kotlinx.coroutines.delay(TieniPremuto.DONE_MS); if (onNow == null) mine = false }
    }
    val labelA0 = animateFloatAsState(if (done) 0f else 1f, if (off) snap() else tween(200), label = "tieni-scritta").value
    val checkA0 = animateFloatAsState(if (done) 1f else 0f, if (off || !done) snap() else tween(100, delayMillis = 120), label = "tieni-spunta").value
    val draw0 = animateFloatAsState(
        if (done) 1f else 0f,
        if (off || !done) snap() else tween(TieniPremuto.CHECK_DRAW_MS.toInt(), delayMillis = TieniPremuto.CHECK_DELAY_MS.toInt(), easing = androidx.compose.animation.core.CubicBezierEasing(0.65f, 0f, 0.35f, 1f)),
        label = "tieni-traccia",
    ).value
    val still = pinned != null
    val pv = pinned?.coerceIn(0f, 1f) ?: p.value
    val mv = if (still) (if (done) 1f else 0f) else morph.value
    val labelA = if (still) (if (done) 0f else 1f) else labelA0
    val checkA = if (still) (if (done) 1f else 0f) else checkA0
    val draw = if (still) (if (done) 1f else 0f) else draw0
    val fg = when (tone) { HoldTone.RED -> V3.Red; HoldTone.INK -> V3.Tx; HoldTone.AMBER -> V3.Wait; HoldTone.GREY -> V3.Tx }
    val fillTone = when (tone) { HoldTone.RED -> V3.Red; HoldTone.AMBER -> V3.Wait; else -> V3.Tx }
    val bg = when (tone) { HoldTone.AMBER -> V3.Wait.copy(alpha = 0.12f); HoldTone.GREY -> V3.S3; else -> V3.S2 }
    val rim: Color? = when (tone) { HoldTone.AMBER -> V3.Wait.copy(alpha = 0.5f); HoldTone.GREY -> null; else -> V3.Line }
    val good = V3.Good
    val ink = V3.Bg
    val pad = when (form) { HoldForm.PILL -> 20.dp; HoldForm.KEY -> 16.dp; HoldForm.MINI -> 14.dp }
    val checkSize = if (form == HoldForm.MINI) 22.dp else 26.dp
    // Barlow Semi Condensed 600 15, anche nella forma della riga: la stessa del tasto di una voce (VoceButton).
    val style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val gestures = if (mode == TieniPremuto.Mode.HOLD) Modifier
        .pointerInput(enabled, done, pinned, holdMs) { detectTapGestures(onPress = { press(); tryAwaitRelease(); lift() }) }
        .onKeyEvent { e ->
            val k = e.key == Key.Spacebar || e.key == Key.Enter || e.key == Key.NumPadEnter
            if (!k) false
            else if (e.type == KeyEventType.KeyDown) { if (e.nativeKeyEvent.repeatCount == 0) press(); true }
            else if (e.type == KeyEventType.KeyUp) { lift(); true }
            else false
        }
        .focusable(enabled = enabled && !done)
    else Modifier.clickable(touch, indication = null, enabled = enabled && !done && !still) { tap() }
    Box(
        modifier
            .then(if (form == HoldForm.PILL) Modifier.fillMaxWidth().height(52.dp) else Modifier.heightIn(min = if (form == HoldForm.KEY) 42.dp else 36.dp))
            .graphicsLayer { translationX = shake.value * density; scaleX = sc.value; scaleY = sc.value; alpha = if (enabled || done) 1f else 0.45f }
            .drawWithContent {
                val w = size.width
                val h = size.height
                val r = h / 2f
                // La pillola diventa cerchio senza cambiare il posto che occupa: si taglia dai due lati verso il centro, coi bordi tondi.
                val cut = ((w - h) / 2f * mv).coerceIn(0f, (w / 2f - 1f).coerceAtLeast(0f))
                val shape = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(cut, 0f, w - cut, h, androidx.compose.ui.geometry.CornerRadius(r, r)))
                }
                clipPath(shape) {
                    drawRect(if (done) good else bg)
                    if (!done && mode == TieniPremuto.Mode.HOLD && pv > 0f) drawRect(fillTone, size = size.copy(width = w * pv))
                    val line = rim
                    if (!done && line != null) {
                        val sw = 1.dp.toPx()
                        drawRoundRect(
                            line, topLeft = androidx.compose.ui.geometry.Offset(cut + sw / 2f, sw / 2f), size = androidx.compose.ui.geometry.Size(w - 2f * cut - sw, h - sw),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r - sw / 2f, r - sw / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(sw),
                        )
                    }
                    this@drawWithContent.drawContent()
                }
                if (checkA > 0f) {
                    val u = checkSize.toPx() / 24f
                    val o = center - androidx.compose.ui.geometry.Offset(12f * u, 12f * u)
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(o.x + 5f * u, o.y + 12.5f * u); lineTo(o.x + 9.5f * u, o.y + 17f * u); lineTo(o.x + 19f * u, o.y + 7.5f * u)
                    }
                    val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
                    val part = androidx.compose.ui.graphics.Path()
                    pm.getSegment(0f, pm.length * draw, part, true)
                    drawPath(part, ink.copy(alpha = checkA), style = androidx.compose.ui.graphics.drawscope.Stroke(2.6f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
            }
            .handCursor()
            .then(gestures)
            .semantics {
                contentDescription = a11y ?: label
                role = Role.Button
                if (news.isNotEmpty()) { liveRegion = LiveRegionMode.Polite; stateDescription = news }
            }
            .padding(horizontal = pad)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.then(if (form == HoldForm.PILL) Modifier.fillMaxWidth() else Modifier).graphicsLayer { alpha = labelA }.blur(((1f - labelA) * 4f).dp), contentAlignment = Alignment.Center) {
            HoldLabel(label, icon, fg, style)
            // La scritta scura sopra il pieno, tagliata alla sua larghezza: si inverte esattamente sotto il pieno.
            if (mode == TieniPremuto.Mode.HOLD) Box(
                Modifier.matchParentSize().drawWithContent {
                    val right = pv * (size.width + 2f * pad.toPx()) - pad.toPx()
                    if (right > 0f) clipRect(right = right) { this@drawWithContent.drawContent() }
                },
                contentAlignment = Alignment.Center,
            ) { HoldLabel(label, icon, ink, style) }
        }
    }
}

@Composable
private fun HoldLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, color: Color, style: TextStyle) =
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = style, color = color, maxLines = 1, softWrap = false)
    }

/**
 * Voce 7 di app-animazioni-seconde-26-11 (Limite ad anello): un anello piccolo si riempie mentre scrivi; negli ultimi 20 caratteri
 * diventa ambra e mostra quanti ne restano, oltre il limite rosso. Non blocca la scrittura.
 */
@Composable
internal fun LimitRing(length: Int, max: Int) {
    // Fedele all'originale (Letter limit; app-animazioni-fedeli-26-11): l'anello va con la molla 280 22; a ogni carattere il numero fa
    // un pop (da 1,12, molla 520 16); ogni carattere oltre il limite dà una scossa (spinta 34 dp/s sulla molla 700 12, metà ampiezza)
    // e il pop da 1,25. Colori in 200 ms. Il testo oltre il limite non si taglia (regola dell'app).
    val state = Animazioni.limit(length, max)
    val off = animationsOff()
    val fillTo = Animazioni.fill(length, max)
    val F = remember { androidx.compose.animation.core.Animatable(fillTo) }
    val CP = remember { androidx.compose.animation.core.Animatable(1f) }
    val SH = remember { androidx.compose.animation.core.Animatable(0f) }
    var last by remember { mutableIntStateOf(length) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(length, off) {
        val before = last
        last = length
        if (off) { F.snapTo(fillTo); return@LaunchedEffect }
        scope.launch { F.animateTo(fillTo, Molla.of(280f, 22f)) }
        if (length == before) return@LaunchedEffect
        val over = length > max && length > before
        scope.launch { CP.snapTo(if (over) 1.25f else maxOf(CP.value, 1.12f)); CP.animateTo(1f, Molla.of(520f, 16f)) }
        if (over) scope.launch { SH.animateTo(0f, Molla.of(700f, 12f), initialVelocity = SH.velocity + 34f * (length - before)) }
    }
    val tone by androidx.compose.animation.animateColorAsState(
        when (state) { Animazioni.Limit.OK -> V3.Tx2; Animazioni.Limit.NEAR -> V3.Wait; Animazioni.Limit.OVER -> V3.Red }, if (off) snap() else tween(200), label = "limite-colore",
    )
    val track = V3.Tx.copy(alpha = 0.14f)
    val left = max - length
    val label = androidx.compose.ui.res.pluralStringResource(R.plurals.scelta_left, maxOf(0, left), maxOf(0, left))
    Row(Modifier.testTag("limite").semantics { contentDescription = label }.graphicsLayer { translationX = SH.value * 0.5f * density }, verticalAlignment = Alignment.CenterVertically) {
        if (state != Animazioni.Limit.OK) Text("$left", style = V3.Meter.copy(color = tone), modifier = Modifier.padding(end = 6.dp).graphicsLayer { scaleX = CP.value; scaleY = CP.value })
        androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
            val w = 3.dp.toPx()
            val st = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(track, 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = st)
            drawArc(tone, -90f, 360f * F.value.coerceIn(0f, 1f), false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = st)
        }
    }
}


/**
 * Voce 4 di app-animazioni-seconde-26-11 (Annulla col tempo, scelta di Franz del 26/11 alle 12:49): un gesto che toglie qualcosa (Non
 * ora, Mai, Visto, Chiudi senza salvare) cambia subito quello che si vede, ma il comando parte dopo [Animazioni.UNDO_MS]; fino ad
 * allora «Annulla» lo ferma e rimette com'era. Un avviso per volta: un gesto nuovo fa partire subito quello di prima.
 */
object Undo {
    data class Shown(val id: Int, val text: String)

    var shown by androidx.compose.runtime.mutableStateOf<Shown?>(null)
        private set
    private var seq = 0
    private val own = kotlinx.coroutines.MainScope()
    private var job: kotlinx.coroutines.Job? = null
    private var go: (() -> Unit)? = null
    private var back: (() -> Unit)? = null

    @Suppress("UNUSED_PARAMETER")
    fun later(scope: kotlinx.coroutines.CoroutineScope, text: String, go: () -> Unit, back: () -> Unit = {}) {
        flush()
        val id = ++seq
        this.go = go; this.back = back
        shown = Shown(id, text)
        // L'attesa non dipende da chi l'ha chiesta: se la sua schermata se ne va, il comando parte lo stesso.
        job = own.launch {
            kotlinx.coroutines.delay(Animazioni.UNDO_MS)
            if (shown?.id == id) flush()
        }
    }

    fun cancel() {
        val b = back
        job?.cancel(); job = null; go = null; back = null; shown = null
        b?.invoke()
    }

    fun flush() {
        val g = go
        job?.cancel(); job = null; go = null; back = null; shown = null
        g?.invoke()
    }
}

/** L'avviso in basso con «Annulla» e l'anello che si svuota in 5 s; sopra tutto, in un popup che non prende i tocchi fuori da sé. */
@Composable
internal fun UndoSnack() {
    val s = Undo.shown ?: return
    val off = animationsOff()
    val ring = remember(s.id) { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(s.id) { if (!off) ring.animateTo(0f, tween(Animazioni.UNDO_MS.toInt(), easing = androidx.compose.animation.core.LinearEasing)) }
    androidx.compose.ui.window.Popup(alignment = Alignment.BottomCenter, offset = androidx.compose.ui.unit.IntOffset(0, -48)) {
        Row(
            Modifier.padding(horizontal = 16.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(V3.S3)
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp).testTag("annulla"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.text, style = V3.Body.copy(fontSize = 14.5.sp), maxLines = 2, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
            // Fedele all'originale (Kokonut Undo button; app-animazioni-fedeli-26-11): il bordo del tasto si svuota in senso orario dal
            // centro in alto, lineare in [Animazioni.UNDO_MS], tratto 1,5 dp; accanto i secondi che restano salgono di 10 dp, 200 ms,
            // ease-out (0.23, 1, 0.32, 1). Con le animazioni spente il bordo resta pieno e i secondi cambiano e basta.
            val ease = androidx.compose.animation.core.CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
            val secs = kotlin.math.ceil(ring.value * Animazioni.UNDO_MS / 1000f).toInt().coerceAtLeast(1)
            val tone = V3.Tx
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            Row(
                Modifier.clip(CircleShape).handCursor().clickable { Undo.cancel() }
                    .drawWithContent {
                        drawContent()
                        val sw = 1.5.dp.toPx()
                        val h = size.height; val w = size.width; val st = sw / 2; val r = h / 2 - st
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w / 2, st); lineTo(w - st - r, st)
                            arcTo(androidx.compose.ui.geometry.Rect(w - st - 2 * r, st, w - st, h - st), -90f, 180f, false)
                            lineTo(st + r, h - st)
                            arcTo(androidx.compose.ui.geometry.Rect(st, st, st + 2 * r, h - st), 90f, 180f, false)
                            close()
                        }
                        val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
                        val part = androidx.compose.ui.graphics.Path()
                        pm.getSegment(0f, pm.length * (if (off) 1f else ring.value), part, true)
                        drawPath(part, tone, style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.undo), style = TextStyle(fontFamily = V3.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = V3.Tx)
                AnimatedContent(
                    secs,
                    transitionSpec = {
                        if (off) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (androidx.compose.animation.slideInVertically(tween(200, easing = ease)) { (10 * density).toInt() } + fadeIn(tween(200, easing = ease))) togetherWith
                            (androidx.compose.animation.slideOutVertically(tween(200, easing = ease)) { -(10 * density).toInt() } + fadeOut(tween(200, easing = ease)))
                    },
                    label = "annulla-secondi",
                    modifier = Modifier.padding(start = 8.dp),
                ) { n -> Text("$n", style = V3.Meter.copy(color = V3.Tx2)) }
            }
        }
    }
}

/**
 * Voce 5, fedele agli originali (Step progress e Agent steps; app-animazioni-fedeli-26-11): le tappe del compito in verifica su un filo.
 * Il filo va alla tappa raggiunta con la molla 160 22; una tappa passata si riempie di verde (260 24) e la spunta si disegna 120 ms dopo
 * (140 20); la tappa che diventa quella in corso fa un pop da 0,7 (molla 500 14, spinta 6). La tappa in corso ha la rotella dell'Agent
 * steps (un giro in 0,8 s) e il nome con l'onda (opacità fino a 0,3 in 1,6 s, 50 ms di scarto per lettera); una fallita è rossa. Sotto,
 * a parole, dove si è. Con le animazioni spente niente movimento.
 */
@Composable
internal fun ProofRail(steps: it.pixelbox.cmwatch.contract.ProofSteps, bleed: androidx.compose.ui.unit.Dp = 0.dp, inset: androidx.compose.ui.unit.Dp = 0.dp) {
    val items = steps.items
    if (items.isEmpty()) return
    val off = animationsOff()
    val looks = items.map { PassiProva.look(it) }
    val n = items.size
    val bar = remember { androidx.compose.animation.core.Animatable(PassiProva.fill(items)) }
    val fs = remember(n) { List(n) { k -> androidx.compose.animation.core.Animatable(if (looks[k] == PassiProva.Look.DONE) 1f else 0f) } }
    val cs = remember(n) { List(n) { k -> androidx.compose.animation.core.Animatable(if (looks[k] == PassiProva.Look.DONE) 1f else 0f) } }
    val ps = remember(n) { List(n) { androidx.compose.animation.core.Animatable(1f) } }
    var prev by remember(n) { androidx.compose.runtime.mutableStateOf(looks) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(looks, off) {
        val before = prev
        prev = looks
        if (off) {
            bar.snapTo(PassiProva.fill(items))
            looks.forEachIndexed { k, l -> val d = if (l == PassiProva.Look.DONE) 1f else 0f; fs[k].snapTo(d); cs[k].snapTo(d); ps[k].snapTo(1f) }
            return@LaunchedEffect
        }
        scope.launch { bar.animateTo(PassiProva.fill(items), Molla.of(160f, 22f)) }
        looks.forEachIndexed { k, l ->
            val done = l == PassiProva.Look.DONE
            scope.launch { fs[k].animateTo(if (done) 1f else 0f, Molla.of(260f, 24f)) }
            if (done && cs[k].targetValue < 1f) scope.launch { kotlinx.coroutines.delay(120); cs[k].animateTo(1f, Molla.of(140f, 20f)) }
            if (!done) scope.launch { cs[k].snapTo(0f) }
            if (l == PassiProva.Look.NOW && before.getOrNull(k) != PassiProva.Look.NOW) scope.launch { ps[k].snapTo(0.7f); ps[k].animateTo(1f, Molla.of(500f, 14f), initialVelocity = 6f) }
        }
    }
    val running = !off && looks.any { it == PassiProva.Look.NOW }
    val clock = if (!running) 0f else {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "tappa")
        t.animateFloat(0f, 4000f, androidx.compose.animation.core.infiniteRepeatable(tween(4000, easing = androidx.compose.animation.core.LinearEasing)), label = "tempo").value
    }
    val turn = (clock % 800f) / 800f * 360f
    val good = V3.Good; val red = V3.Red; val busy = V3.Busy; val line = V3.S3; val ink = V3.Tx3; val bg = V3.S1
    // app-passi-centrati-26-11 (Franz 26/11 15:02: «troppo a destra, serve ben centrato»): nella riga della sessione il filo esce a
    // sinistra della colonna del testo di [bleed] (la colonna del glifo) e rientra di [inset], lo stesso margine del lato destro: così
    // è centrato nella riga intera e non sotto il nome.
    val wide = Modifier.layout { m, c ->
        val extra = (bleed - inset).roundToPx()
        val w = (c.maxWidth + extra).coerceAtLeast(0)
        val p = m.measure(c.copy(minWidth = w, maxWidth = w))
        layout(c.maxWidth, p.height) { p.place(-extra, 0) }
    }
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().then(if (bleed > 0.dp) wide else Modifier).padding(top = 6.dp).testTag("passi-prova")) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val cw = size.width / n
            val cy = size.height / 2
            val x0 = cw / 2; val x1 = size.width - cw / 2
            drawLine(line, androidx.compose.ui.geometry.Offset(x0, cy), androidx.compose.ui.geometry.Offset(x1, cy), 2.dp.toPx())
            if (n > 1) drawLine(good, androidx.compose.ui.geometry.Offset(x0, cy), androidx.compose.ui.geometry.Offset(x0 + (x1 - x0) * bar.value.coerceIn(0f, 1f), cy), 2.dp.toPx())
            val r = 10.dp.toPx()
            val round = { w: Float -> androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round) }
            looks.forEachIndexed { i, look ->
                val c = androidx.compose.ui.geometry.Offset(x0 + i * cw, cy)
                val f = fs[i].value.coerceIn(0f, 1f)
                scale(ps[i].value, c) {
                    if (look == PassiProva.Look.FAILED) {
                        drawCircle(red, r, c)
                        val d = r * .35f
                        drawLine(androidx.compose.ui.graphics.Color.Black, c + androidx.compose.ui.geometry.Offset(-d, -d), c + androidx.compose.ui.geometry.Offset(d, d), 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(androidx.compose.ui.graphics.Color.Black, c + androidx.compose.ui.geometry.Offset(d, -d), c + androidx.compose.ui.geometry.Offset(-d, d), 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                    } else {
                        drawCircle(androidx.compose.ui.graphics.lerp(bg, good, f), r, c)
                        if (f < 1f) {
                            val rr = r - 1.dp.toPx()
                            if (look == PassiProva.Look.NOW) {
                                drawCircle(busy.copy(alpha = 0.35f * (1f - f)), rr, c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                                drawArc(busy.copy(alpha = 1f - f), turn - 90f, 90f, false, topLeft = c - androidx.compose.ui.geometry.Offset(rr, rr), size = androidx.compose.ui.geometry.Size(2 * rr, 2 * rr), style = round(2.dp.toPx()))
                            } else drawCircle(ink.copy(alpha = 1f - f), rr, c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        }
                        val k = cs[i].value.coerceIn(0f, 1f)
                        if (k > 0f) {
                            val u = 16.dp.toPx() / 24f
                            val o = c - androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx())
                            val path = androidx.compose.ui.graphics.Path().apply { moveTo(o.x + 5f * u, o.y + 12.5f * u); lineTo(o.x + 9.5f * u, o.y + 17f * u); lineTo(o.x + 19f * u, o.y + 7.5f * u) }
                            val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
                            val part = androidx.compose.ui.graphics.Path()
                            pm.getSegment(0f, pm.length * k, part, true)
                            drawPath(part, androidx.compose.ui.graphics.Color.Black, style = round(2.8f * u))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            items.forEachIndexed { i, s ->
                val look = looks[i]
                val color = when (look) { PassiProva.Look.DONE -> V3.Tx; PassiProva.Look.NOW -> V3.Busy; PassiProva.Look.FAILED -> V3.Red; else -> V3.Tx2 }
                val wave = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
                val label = if (look == PassiProva.Look.NOW && running) androidx.compose.ui.text.buildAnnotatedString {
                    s.label.forEachIndexed { k, ch ->
                        val ph = (((clock - k * 50f) % 1600f) + 1600f) % 1600f / 1600f
                        val half = if (ph < 0.5f) ph * 2f else (1f - ph) * 2f
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = 1f - 0.7f * wave.transform(half))))
                        append(ch)
                        pop()
                    }
                } else AnnotatedString(s.label)
                Text(
                    label, style = V3.Small.copy(fontSize = 13.sp, color = color),
                    maxLines = 1, softWrap = false, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        it.pixelbox.cmwatch.rules.PassiProva.line(steps)?.let { l ->
            val text = when (l.say) {
                it.pixelbox.cmwatch.rules.PassiProva.Say.RUNNING -> stringResource(R.string.proof_running, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.FAILED -> stringResource(R.string.proof_failed, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.TODO -> stringResource(R.string.proof_todo, l.label)
                it.pixelbox.cmwatch.rules.PassiProva.Say.ALL_DONE -> stringResource(R.string.proof_all_done)
            }
            Text(text, style = V3.Meter, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
        }
    }
}

/**
 * app-messaggio-master-effetto-27-11, opzione E (Franz 27/11 15:48, mockup 04-subito.html): l'ora del riquadro della master che scorre
 * quando arriva una risposta, da 04:05 a 04:08. Scorre solo la cifra che cambia: la vecchia sale via, la nuova entra da sotto, con la molla
 * di casa (CmMotion.bounceFloat, 620 ms); il taglio è solo sopra e sotto. Senza [roll], con le animazioni spente o con un'altra lunghezza,
 * l'ora cambia e basta.
 */
@Composable
internal fun RollTime(value: String, roll: Boolean, style: TextStyle, modifier: Modifier = Modifier) {
    val off = animationsOff()
    var shown by remember { androidx.compose.runtime.mutableStateOf(value) }
    var from by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val p = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(value) {
        if (value == shown) return@LaunchedEffect
        if (!roll || off || value.length != shown.length) { shown = value; from = null; return@LaunchedEffect }
        from = shown
        shown = value
        p.snapTo(0f)
        try { p.animateTo(1f, CmMotion.bounceFloat) } finally { from = null }
    }
    val old = from
    if (old == null) { Text(shown, style = style, maxLines = 1, modifier = modifier); return }
    Row(modifier) {
        shown.forEachIndexed { i, ch ->
            if (old.getOrNull(i) == ch) Text(ch.toString(), style = style, maxLines = 1)
            else Box(Modifier.clipToBounds()) {
                Text(old[i].toString(), style = style, maxLines = 1, modifier = Modifier.graphicsLayer { translationY = -p.value * size.height })
                Text(ch.toString(), style = style, maxLines = 1, modifier = Modifier.graphicsLayer { translationY = (1f - p.value) * size.height })
            }
        }
    }
}
