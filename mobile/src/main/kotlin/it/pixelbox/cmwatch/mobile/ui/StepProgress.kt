package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.rules.Animazioni
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Step progress, fedele all'originale (designbookmark.com/motion, step-progress: steps.js e steps.css; app-chiusura-step-progress-27-11,
 * Franz 27/11 12:00): una linea da 4 dp dal primo all'ultimo pallino, col verde che va al passo attuale sulla molla 160 22, e un pallino
 * da 32 dp col numero per passo. Un passo fatto si riempie di verde (260 24) mentre il numero sfuma, e la spunta si disegna 120 ms dopo
 * (140 20); il passo che diventa attuale fa un pop da 0,7 (500 14, spinta 6) e si accende col colore di lavoro (300 26), col numero
 * scuro. Sotto i nomi, in chiaro fino all'attuale (300 ms). Colori v3: verde [V3.Good], attuale [V3.Busy], pallini [V3.S3], linea al
 * 14% del testo come la fill-3 dell'originale. L'attuale è fatto solo a lavoro finito ([finished]): nella chiusura «Chiusa» raggiunta
 * aspetta la conferma del PC (scelta a della master). Animazioni spente: i passi accesi, fermi.
 */
@Composable
internal fun StepProgress(names: List<String>, at: Int, modifier: Modifier = Modifier, finished: Boolean = false) {
    val n = names.size
    if (n == 0) return
    val off = animationsOff()
    val i = at.coerceIn(0, n - 1)
    val states = List(n) { k -> Animazioni.step(k, i, finished) }
    // Come l'originale tutto parte da zero e va ai passi di adesso; con le animazioni spente parte già lì.
    val line = remember { Animatable(if (off) Animazioni.stepLine(i, n) else 0f) }
    val fs = remember(n) { List(n) { k -> Animatable(if (off && states[k] == Animazioni.Step.DONE) 1f else 0f) } }
    val cs = remember(n) { List(n) { k -> Animatable(if (off && states[k] == Animazioni.Step.DONE) 1f else 0f) } }
    val os = remember(n) { List(n) { k -> Animatable(if (off && states[k] == Animazioni.Step.CURRENT) 1f else 0f) } }
    val ps = remember(n) { List(n) { Animatable(1f) } }
    val cur = remember(n) { intArrayOf(if (off) i else -1) }
    LaunchedEffect(i, finished, n, off) {
        if (off) {
            line.snapTo(Animazioni.stepLine(i, n))
            states.forEachIndexed { k, s ->
                val d = if (s == Animazioni.Step.DONE) 1f else 0f
                fs[k].snapTo(d); cs[k].snapTo(d); ps[k].snapTo(1f); os[k].snapTo(if (s == Animazioni.Step.CURRENT) 1f else 0f)
            }
            cur[0] = i
            return@LaunchedEffect
        }
        launch { line.animateTo(Animazioni.stepLine(i, n), Molla.of(160f, 22f)) }
        states.forEachIndexed { k, s ->
            val done = s == Animazioni.Step.DONE
            launch { fs[k].animateTo(if (done) 1f else 0f, Molla.of(260f, 24f)) }
            launch { os[k].animateTo(if (s == Animazioni.Step.CURRENT) 1f else 0f, Molla.of(300f, 26f)) }
            launch { if (done) delay(120); cs[k].animateTo(if (done) 1f else 0f, Molla.of(140f, 20f)) }
        }
        if (i != cur[0]) {
            cur[0] = i
            launch { ps[i].snapTo(0.7f); ps[i].animateTo(1f, Molla.of(500f, 14f), initialVelocity = 6f) }
        }
    }
    val measurer = rememberTextMeasurer()
    val numStyle = V3.Small.copy(fontSize = 13.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = CM_TNUM)
    val track = V3.Tx.copy(alpha = 0.14f); val good = V3.Good; val busy = V3.Busy; val dot = V3.S3; val muted = V3.Tx2; val dark = V3.Bg
    Column(modifier.fillMaxWidth().testTag("passi-chiusura")) {
        Canvas(Modifier.fillMaxWidth().height(32.dp)) {
            val x0 = 14.dp.toPx(); val x1 = size.width - 14.dp.toPx()
            fun xAt(k: Int) = if (n <= 1) size.width / 2 else x0 + (x1 - x0) * k / (n - 1)
            val cy = 16.dp.toPx(); val th = 4.dp.toPx()
            drawRoundRect(track, Offset(x0, cy - th / 2), Size(x1 - x0, th), CornerRadius(th / 2))
            val lw = (x1 - x0) * line.value.coerceIn(0f, 1f)
            if (lw > 0f) drawRoundRect(good, Offset(x0, cy - th / 2), Size(lw, th), CornerRadius(th / 2))
            val r = 16.dp.toPx()
            for (k in 0 until n) {
                val c = Offset(xAt(k), cy)
                val f = fs[k].value.coerceIn(0f, 1f)
                val o = os[k].value.coerceIn(0f, 1f)
                scale(ps[k].value, c) {
                    drawCircle(if (f > 0.01f) lerp(dot, good, f) else if (o > 0.01f) lerp(dot, busy, o) else dot, r, c)
                    // Il numero sfuma mentre il pallino diventa verde; sul colore di lavoro è scuro.
                    if (f < 1f) {
                        val ink = if (o > 0.5f) dark else muted
                        val l = measurer.measure("${k + 1}", numStyle.copy(color = ink.copy(alpha = 1f - f)))
                        drawText(l, topLeft = Offset(c.x - l.size.width / 2f, c.y - l.size.height / 2f))
                    }
                    // La spunta si disegna da sinistra: il tracciato di steps.js in un riquadro da 16 dp, tratto 2,8.
                    val ck = cs[k].value.coerceIn(0f, 1f)
                    if (ck > 0f) {
                        val u = 16.dp.toPx() / 24f
                        val o0 = c - Offset(8.dp.toPx(), 8.dp.toPx())
                        val path = Path().apply { moveTo(o0.x + 5f * u, o0.y + 12.5f * u); lineTo(o0.x + 9.5f * u, o0.y + 17f * u); lineTo(o0.x + 19f * u, o0.y + 7.5f * u) }
                        val pm = PathMeasure().apply { setPath(path, false) }
                        val part = Path()
                        pm.getSegment(0f, pm.length * ck, part, true)
                        drawPath(part, dark, style = Stroke(2.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }
        // I nomi sotto i pallini, centrati su di loro come nell'originale (anche oltre il margine della linea), in chiaro fino all'attuale.
        Layout(
            content = {
                names.forEachIndexed { k, name ->
                    val col by animateColorAsState(if (k <= i) V3.Tx else muted, if (off) snap() else tween(300), label = "nome")
                    Text(name, style = V3.Small.copy(fontSize = 12.5.sp, lineHeight = 18.sp, color = col), maxLines = 1, softWrap = false)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) { ms, c ->
            val w = c.maxWidth
            val inset = 14.dp.roundToPx()
            // Ogni nome al più largo quanto lo spazio fra due pallini: non tocca il vicino, e un nome troppo lungo per il suo posto si vede.
            val slot = if (n <= 1) w else (w - 2 * inset) / (n - 1)
            val placeables = ms.map { it.measure(Constraints(maxWidth = slot.coerceAtLeast(0))) }
            layout(w, placeables.maxOf { it.height }) {
                placeables.forEachIndexed { k, p ->
                    val x = if (n <= 1) w / 2 else inset + (w - 2 * inset) * k / (n - 1)
                    p.place(x - p.width / 2, 0)
                }
            }
        }
    }
}
