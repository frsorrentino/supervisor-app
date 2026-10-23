package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Badge
import it.pixelbox.cmwatch.rules.Lavori
import it.pixelbox.cmwatch.rules.RecapAgenda
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il badge della sessione come sul polso e sul desktop (Franz, 30/09 20:31): forma = account (cerchio personale, quadrato
 * lavoro), riempimento = colore della sessione, glifo di stato dentro, dalla regola `Badge` di `core`. Respira mentre la
 * sessione lavora, fermo con le animazioni spente.
 */
@Composable
fun SessionBadge(s: Session, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val spec = Badge.of(s.account, s.color, s.state, s.icon, s.accountKind)
    // Il respiro in un livello suo, letto solo lì (Franz, 05/10 12:30: «l'app è sempre molto lenta»): letto qui ricomponeva
    // il badge a ogni fotogramma e ridisegnava tutta la lista che lo contiene.
    val breath = if (Badge.breathes(s.state) && !animationsOff())
        rememberInfiniteTransition(label = "respiro").animateFloat(1f, 0.55f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "a")
    else null
    val labels = Badge.Labels(
        waiting = stringResource(R.string.state_waiting), busy = stringResource(R.string.state_busy),
        idle = stringResource(R.string.state_idle), gone = stringResource(R.string.state_closed),
        awaiting = stringResource(R.string.state_awaiting), personal = stringResource(R.string.badge_personal),
        work = stringResource(R.string.badge_work),
    )
    val description = Badge.description(s.account, s.accountKind, s.state, labels)
    Canvas(modifier.size(size).graphicsLayer { alpha = breath?.value ?: 1f }.semantics { contentDescription = description }) { drawBadge(spec, this.size.minDimension, 1f) }
}

/** Solo la forma dell'account, vuota: dove c'è l'account ma nessuna sessione (quota, «Lancia», filtro). */
@Composable
fun AccountMark(personal: Boolean, modifier: Modifier = Modifier, size: Dp = 14.dp, color: Color = CmColors.text2) {
    val description = stringResource(if (personal) R.string.badge_personal else R.string.badge_work)
    Canvas(modifier.size(size).semantics { contentDescription = description }) {
        val d = this.size.minDimension
        val stroke = Stroke(width = d * 0.14f)
        if (personal) drawCircle(color, radius = d / 2f - d * 0.07f, center = Offset(d / 2f, d / 2f), style = stroke)
        else drawRoundRect(color, topLeft = Offset(d * 0.07f, d * 0.07f), size = Size(d * 0.86f, d * 0.86f), cornerRadius = CornerRadius(d * 0.23f), style = stroke)
    }
}

/** Le stesse proporzioni del badge dell'orologio (`drawBadge` in `wear`), così polso e telefono si riconoscono. */
private fun DrawScope.drawBadge(spec: Badge.Spec, d: Float, alpha: Float) {
    val fill = Color(spec.fill).copy(alpha = alpha)
    val ink = Color(spec.glyphColor).copy(alpha = alpha)
    when (spec.shape) {
        Badge.Shape.CIRCLE -> drawCircle(fill, radius = d / 2f, center = Offset(d / 2f, d / 2f))
        Badge.Shape.SQUARE -> drawRoundRect(fill, size = Size(d, d), cornerRadius = CornerRadius(d * 0.23f))
    }
    glyph(spec.glyph, d, ink)
}

/**
 * Il glifo di stato dal tracciato di `Badge.paths` (Franz, 03/10 09:01: icone uniche; mano, fulmine, pausa, croce), a
 * tratto in un riquadro di 0,6 del diametro: lo stesso disegno del badge dell'orologio e delle sue notifiche.
 */
private fun DrawScope.glyph(g: Badge.Glyph, d: Float, ink: Color) = glyph(Badge.paths(g), d, ink)

private fun DrawScope.glyph(paths: List<String>, d: Float, ink: Color) {
    val box = d * 0.6f; val o = (d - box) / 2f
    withTransform({ translate(o, o); scale(box / 24f, box / 24f, pivot = Offset.Zero) }) {
        paths.forEach { p ->
            drawPath(androidx.compose.ui.graphics.vector.PathParser().parsePathString(p).toPath(), ink,
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

/**
 * Il segno di un lavoro (strada B, Franz 23/10 23:08): la stessa famiglia del badge delle sessioni: cerchio = account
 * personale, quadrato = agenzia; il glifo dice cosa chiede: mano = aspetta te, triangolo = può partire, sigillo = un ok.
 */
@Composable
fun WorkBadge(kind: Lavori.Kind, mark: RecapAgenda.Mark, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val description = stringResource(when (kind) { Lavori.Kind.OK -> R.string.lavori_ok; Lavori.Kind.OUTSIDE -> R.string.lavori_outside; Lavori.Kind.CLAUDE -> R.string.lavori_claude })
    val square = mark == RecapAgenda.Mark.AGENCY || mark == RecapAgenda.Mark.DESK
    Canvas(modifier.size(size).semantics { contentDescription = description }) {
        val d = this.size.minDimension
        if (square) drawRoundRect(CmColors.briefTrack, size = Size(d, d), cornerRadius = CornerRadius(d * 0.23f))
        else drawCircle(CmColors.briefTrack, radius = d / 2f, center = Offset(d / 2f, d / 2f))
        glyph(workPaths(kind), d, Color.White)
    }
}

/** Solo il glifo di un lavoro, a tratto nel colore del gruppo: nella scheda di una sessione, accanto agli ok. */
@Composable
fun WorkGlyph(kind: Lavori.Kind, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val d = this.size.minDimension
        withTransform({ scale(d / 24f, d / 24f, pivot = Offset.Zero) }) {
            workPaths(kind).forEach { p ->
                drawPath(androidx.compose.ui.graphics.vector.PathParser().parsePathString(p).toPath(), tint,
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}

/** I tracciati dei glifi dei lavori, nel riquadro 24×24 di `Badge.paths`: la mano del badge, un triangolo, il sigillo dell'ok. */
internal fun workPaths(kind: Lavori.Kind): List<String> = when (kind) {
    Lavori.Kind.OUTSIDE -> Badge.paths(Badge.Glyph.HAND)
    Lavori.Kind.CLAUDE -> listOf("M7 4v16l13-8z")
    Lavori.Kind.OK -> listOf("M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76z", "M9 12l2 2 4-4")
}
