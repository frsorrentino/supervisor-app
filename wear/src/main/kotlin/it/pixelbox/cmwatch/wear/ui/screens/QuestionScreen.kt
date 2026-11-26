package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.wear.ui.components.SpeakButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.drawText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.wear.ui.theme.rememberRoundSpec
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.Tier
import it.pixelbox.cmwatch.data.PendingStatus
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.rules.QuestionRules
import it.pixelbox.cmwatch.wear.ui.components.SessionHeader
import it.pixelbox.cmwatch.wear.ui.components.StaleChip
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.ui.tokens.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.morph
import kotlinx.coroutines.delay

/**
 * Domanda a schermo intero: testo intero, un bottone largo per opzione uno sotto l'altro, tier high con
 * pressione lunga, testo libero con la tastiera di sistema. Se la domanda sparisce (risposta altrove) lo dice e chiude.
 */
@Composable
fun QuestionScreen(
    snapshot: Snapshot,
    name: String,
    now: Long,
    sentId: String?,
    onAnswer: (Int) -> Unit,
    onFreeText: () -> Unit,
    onAllowAll: () -> Unit,
    onRetry: (String) -> Unit,
    onAnsweredElsewhere: () -> Unit,
    onDone: () -> Unit,
    speaking: Boolean = false,
    onSpeak: () -> Unit = {},
    onChat: () -> Unit = {},
    listState: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
) {
    val spec = rememberRoundSpec()
    val s: Session? = snapshot.state?.sessions?.firstOrNull { it.name == name }
    val q = s?.question
    val enabled = snapshot.freshness !is Freshness.Stale
    val pending = snapshot.pending.firstOrNull { it.cmd.id == sentId }
    var holdHint by rememberSaveable { mutableStateOf(false) }
    // Il bordo rosso del rischio alto fa un respiro solo all'arrivo della domanda, poi resta fermo (proposta 20, fase 1):
    // si nota senza restare ad animare addosso alla batteria. Con «riduci animazioni» il bordo è subito quello fermo.
    val respiro = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    val fermeAnimazioni = it.pixelbox.cmwatch.wear.ui.ambient.animationsOff()
    LaunchedEffect(q, fermeAnimazioni) {
        if (q?.tier == Tier.HIGH && !fermeAnimazioni) {
            respiro.snapTo(1f)
            respiro.animateTo(0f, androidx.compose.animation.core.tween(durationMillis = 900))
        } else {
            respiro.snapTo(0f)
        }
    }

    // Voce 8 di app-animazioni-seconde-26-11 (Domanda che arriva al polso): all'arrivo chi chiede, la domanda e i tasti salgono di 14 dp
    // e si posano con un piccolo sforamento, a 40 ms l'uno dall'altro; una volta sola per domanda. Con «riduci animazioni» niente.
    // Solo all'arrivo: una domanda di più di 20 s fa (aperta più tardi, o nelle immagini di prova) è già al suo posto.
    val appena = q != null && System.currentTimeMillis() / 1000 - q.askedAt <= 20
    val ferma = fermeAnimazioni || !appena || androidx.compose.ui.platform.LocalInspectionMode.current
    val arrivo = androidx.compose.runtime.remember(q?.text) { androidx.compose.animation.core.Animatable(if (ferma) 1f else 0f) }
    LaunchedEffect(q?.text, ferma) { if (!ferma && q != null) arrivo.animateTo(1f, androidx.compose.animation.core.tween(620 + 6 * it.pixelbox.cmwatch.rules.Animazioni.STAGGER_MS, easing = androidx.compose.animation.core.LinearEasing)) }
    val sale = androidx.compose.animation.core.CubicBezierEasing(0.34f, 1.3f, 0.64f, 1f)
    fun Modifier.arriva(i: Int): Modifier = graphicsLayer {
        val total = 620f + 6 * it.pixelbox.cmwatch.rules.Animazioni.STAGGER_MS
        val local = ((arrivo.value * total - i * it.pixelbox.cmwatch.rules.Animazioni.STAGGER_MS) / 620f).coerceIn(0f, 1f)
        translationY = (1f - sale.transform(local)) * 14.dp.toPx()
        alpha = (local / 0.3f).coerceAtMost(1f)
    }

    // Domanda sparita: se l'abbiamo mandata noi si chiude al risultato; altrimenti «già risposta» e si chiude.
    if (q == null) {
        LaunchedEffect(sentId, pending?.status) {
            if (sentId == null) { onAnsweredElsewhere(); delay(1500); onDone() }
            else if (pending == null) { delay(600); onDone() }
        }
    }

    ScreenScaffold(
        scrollState = listState, contentPadding = roundListPadding(),
    ) { padding ->
        // La corona muove la Domanda un'opzione per volta, con lo scatto aptico del sistema (proposta 17, fase 1):
        // si sceglie senza coprire il testo con il dito.
        TransformingLazyColumn(
            state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize(),
            rotaryScrollableBehavior = androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults.snapBehavior(listState),
        ) {
            if (s == null || q == null) {
                item {
                    val text = when {
                        sentId != null && pending?.status == PendingStatus.FAILED -> stringResource(R.string.question_not_delivered)
                        sentId != null -> stringResource(R.string.question_sent)
                        else -> stringResource(R.string.question_answered_elsewhere)
                    }
                    Column(Modifier.fillMaxWidth().morph(this, spec), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        // Voce 8 di app-animazioni-otto-26-11 (Ok al polso): l'arco gira finché il PC non ha preso la risposta, poi la spunta.
                        val mark = sentId != null && pending?.status != PendingStatus.FAILED
                        if (mark) AnswerMark(done = pending == null, label = text)
                        if (!mark) Text(text, style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (sentId != null && pending?.status == PendingStatus.FAILED) {
                    item { WideButton(stringResource(R.string.question_retry), onClick = { onRetry(sentId) }, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                }
                return@TransformingLazyColumn
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st -> item { StaleChip(st.minutes, Modifier.morph(this, spec)) } }
            item { SessionHeader(s, now, enabled, modifier = Modifier.morph(this, spec).arriva(0), trailing = { SpeakButton(speaking, onToggle = onSpeak) }) }
            // La domanda a tutta larghezza; il ▶ che la legge con le opzioni numerate sta in testata (Franz, 15/09 16:13).
            item { QuestionText(QuestionRules.shownText(q.text, stringResource(R.string.question_no_text)), modifier = Modifier.fillMaxWidth().morph(this, spec).arriva(1)) }
            // Voce 8: mandata la risposta, mentre la domanda è ancora qui, l'arco dice che sta viaggiando.
            if (pending != null && pending.status != PendingStatus.FAILED) item { Box(Modifier.fillMaxWidth().morph(this, spec), contentAlignment = androidx.compose.ui.Alignment.Center) { AnswerMark(done = false) } }
            if (holdHint) {
                item { Text(stringResource(R.string.question_hold), style = MaterialTheme.typography.bodyMedium, color = CmColors.waiting, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
            }
            q.options.forEachIndexed { i, opt ->
                item {
                    // Contratto 1.53: piena l'opzione consigliata, con la sua riga di spiegazione.
                    val primary = i == QuestionRules.primaryIndex(q.options)
                    val long = QuestionRules.needsLongPress(q.tier)
                    WideButton(
                        QuestionRules.optionLabel(opt),
                        note = QuestionRules.optionNote(opt, stringResource(R.string.option_recommended)),
                        onClick = { if (long) holdHint = true else onAnswer(opt.n) },
                        onLongClick = if (long) ({ onAnswer(opt.n) }) else null,
                        primary = primary, enabled = enabled && pending == null,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec).arriva(2 + i),
                        // Il rischio alto si vede anche col colore, non solo con la pressione lunga (Franz, 15/09 18:14).
                        // A riposo resta il bordo di prima (2 dp pieno); il respiro lo ingrossa una volta sola all'arrivo.
                        border = if (q.tier == Tier.HIGH) androidx.compose.foundation.BorderStroke((2 + 2 * respiro.value).dp, CmColors.gone) else null,
                    )
                }
            }
            // «Chat about this» come nel terminale: si parla prima di scegliere (Franz, 15/09 18:14; contratto 1.10).
            // Contratto 1.161 (issue #4): «Chat about this» e «Scrivi» solo se il dialogo li ha davvero.
            if (it.pixelbox.cmwatch.rules.VociAssenti.chat(q)) item {
                WideButton(
                    stringResource(R.string.question_chat), onClick = onChat, enabled = enabled && pending == null,
                    transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                )
            }
            if (QuestionRules.allowAllVisible(q)) {
                item { WideButton(stringResource(R.string.question_allow_all), onClick = onAllowAll, enabled = enabled && pending == null, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
            }
            // Le opzioni restano bottoni in lista, perché sono contenuto; l'azione della schermata è «Scrivi», ultima voce e
            // curva sul bordo: nello slot fisso copriva le opzioni mentre si scorreva (video 16/09 18:48).
            if (it.pixelbox.cmwatch.rules.VociAssenti.type(q)) item { CmEdgeButton(stringResource(R.string.question_write), onClick = onFreeText, enabled = enabled && pending == null) }
        }
    }
}

/** Corpo della domanda a 18 sp; se l'ultima riga resterebbe con una parola sola, scende di uno scatto fino a 15 sp (master, 12/09). */
@Composable
private fun QuestionText(text: String, modifier: Modifier = Modifier) {
    var size by rememberSaveable(text) { mutableStateOf(18) }
    Text(
        text, color = CmColors.text, modifier = modifier,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = size.sp, lineHeight = (size + 5).sp),
        onTextLayout = { r ->
            if (r.lineCount > 1 && size > 15) {
                val last = text.substring(r.getLineStart(r.lineCount - 1), r.getLineEnd(r.lineCount - 1)).trim()
                if (!last.contains(' ')) size -= 1
            }
        },
    )
}

/**
 * Voce 8, fedele all'originale (designbookmark.com/motion, Loading to success; app-animazioni-fedeli-26-11). Mentre la risposta
 * viaggia, un cerchio di 56 dp col velo ambra e la cometa che gira a 504°/s, entrata da 0,6 (molla 300 28). Preso dal PC: la cometa
 * esce, il cerchio si apre in una pillola (molla 300 22) che diventa verde (200 30), la spunta si disegna dopo 150 ms (120 20) e
 * [label] sale di 12 dp (220 20). Con «riduci animazioni» niente giro e tutto al posto.
 */
@Composable
private fun AnswerMark(done: Boolean, label: String? = null) {
    val off = it.pixelbox.cmwatch.wear.ui.ambient.animationsOff()
    fun m(k: Float, c: Float) = androidx.compose.animation.core.spring<Float>(stiffness = k, dampingRatio = c / (2f * kotlin.math.sqrt(k)))
    @Composable fun sp(t: Float, k: Float, c: Float, l: String) =
        androidx.compose.animation.core.animateFloatAsState(t, if (off) androidx.compose.animation.core.snap() else m(k, c), label = l).value
    val d = if (done) 1f else 0f
    val pIn = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (off) 1f else 0f) }
    LaunchedEffect(done) { if (done) pIn.animateTo(0f, if (off) androidx.compose.animation.core.snap() else m(300f, 28f)) else if (!off) pIn.animateTo(1f, m(300f, 28f)) }
    val w = sp(d, 300f, 22f, "ok-larghezza")
    val g = sp(d, 200f, 30f, "ok-verde")
    val dd = sp(d, 220f, 20f, "ok-scritta")
    val k = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (done && off) 1f else 0f) }
    LaunchedEffect(done) { if (done) { if (off) k.snapTo(1f) else { kotlinx.coroutines.delay(150); k.animateTo(1f, m(120f, 20f)) } } }
    val turn = if (off || pIn.value <= 0.001f && done) 0f else {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "cometa")
        t.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(714, easing = androidx.compose.animation.core.LinearEasing)), label = "giro").value
    }
    val amber = CmColors.waiting
    val veil = androidx.compose.ui.graphics.lerp(androidx.compose.ui.graphics.Color.Black, amber, 0.28f)
    val green = CmColors.idle
    val ink = androidx.compose.ui.graphics.Color.Black
    val density = androidx.compose.ui.platform.LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge.copy(color = ink)
    val text = label?.let { measurer.measure(it, style, maxLines = 1) }
    val h = 56.dp
    val openW = with(density) { (22.dp.toPx() + 8.dp.toPx() + (text?.size?.width ?: 0) + 36.dp.toPx()).toDp() }
    val width = h + (openW - h) * w.coerceAtLeast(0f)
    androidx.compose.foundation.Canvas(Modifier.padding(bottom = 6.dp).size(width, h)) {
        val bg = androidx.compose.ui.graphics.lerp(veil, green, g.coerceIn(0f, 1f))
        drawRoundRect(bg, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        val pv = pIn.value.coerceIn(0f, 1.2f)
        if (pv > 0.001f) {
            val r = 12.dp.toPx() * (0.6f + 0.4f * pv)
            val sw = r * 0.28f
            androidx.compose.ui.graphics.drawscope.rotate(turn - 90f, center) {
                drawCircle(
                    androidx.compose.ui.graphics.Brush.sweepGradient(
                        0f to androidx.compose.ui.graphics.Color.Transparent, 0.10f to androidx.compose.ui.graphics.Color.Transparent,
                        0.85f to amber, 0.85f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color.Transparent,
                        center = center,
                    ),
                    radius = r - sw / 2, center = center, alpha = pIn.value.coerceIn(0f, 1f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(sw),
                )
            }
        }
        val a = dd.coerceIn(0f, 1f)
        if (a > 0f) {
            val dy = (1f - dd) * 12.dp.toPx()
            val u = 22.dp.toPx() / 24f
            val total = 22.dp.toPx() + (if (text != null) 8.dp.toPx() + text.size.width else 0f)
            val x0 = (size.width - total) / 2f
            val y0 = (size.height - 22.dp.toPx()) / 2f + dy
            val path = androidx.compose.ui.graphics.Path().apply { moveTo(x0 + 5f * u, y0 + 12.5f * u); lineTo(x0 + 9.5f * u, y0 + 17f * u); lineTo(x0 + 19f * u, y0 + 7.5f * u) }
            val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
            val part = androidx.compose.ui.graphics.Path()
            pm.getSegment(0f, pm.length * k.value.coerceIn(0f, 1f), part, true)
            drawPath(part, ink, alpha = a, style = androidx.compose.ui.graphics.drawscope.Stroke(2.6f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            if (text != null) drawText(text, topLeft = androidx.compose.ui.geometry.Offset(x0 + 22.dp.toPx() + 8.dp.toPx(), (size.height - text.size.height) / 2f + dy), alpha = a)
        }
    }
}
