package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.Rail

/**
 * Le favicon arrivate dal PC (contratto 1.153), per impronta: le mette MainActivity, le leggono badge e righe. Vuota nei test che non
 * la danno e con un relay che non ha l'op: lì resta l'icona di oggi.
 */
val LocalFavicons = staticCompositionLocalOf<Map<String, ImageBitmap>> { emptyMap() }

/** La favicon di questa sessione, se il PC l'ha mandata. */
@Composable
fun siteFavicon(s: Session): ImageBitmap? = s.favicon?.let { h -> LocalFavicons.current[h] }

/**
 * app-favicon-siti-18-11, scelta B di Franz (18/11 17:11): la favicon del sito su un riquadro chiaro arrotondato, così si legge anche
 * scura di notte, con intorno l'anello del colore dello stato. Al lavoro l'anello è un arco che gira, come lo spinner di oggi; fermo
 * con le animazioni spente. Fra riquadro e anello uno spazio del colore di fondo.
 */
@Composable
fun SiteAvatar(icon: ImageBitmap, ring: Color, busy: Boolean, size: Dp = 22.dp, label: String? = null, modifier: Modifier = Modifier) {
    val turn = if (busy && !animationsOff())
        rememberInfiniteTransition(label = "anello").animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "giro")
    else null
    val tile = if (V3.day) Color.White else Color(0xFFF5F5F2)
    val edge = if (V3.day) Color(0x1F000000) else Color.Transparent
    val m = if (label != null) modifier.semantics { contentDescription = label } else modifier
    Canvas(m.size(size)) {
        val d = this.size.minDimension
        val ringW = d * 0.075f
        val gap = d * 0.065f
        val inset = ringW + gap
        val inner = d - 2 * inset
        // L'anello: intero per chi aspetta o è fermo, un arco di tre quarti che gira per chi lavora.
        val half = ringW / 2f
        val r = CornerRadius(d * 0.3f)
        if (turn == null) drawRoundRect(ring, Offset(half, half), Size(d - ringW, d - ringW), r, style = Stroke(ringW))
        else {
            drawRoundRect(ring.copy(alpha = ring.alpha * 0.28f), Offset(half, half), Size(d - ringW, d - ringW), r, style = Stroke(ringW))
            drawArc(ring, -90f + turn.value, 270f, false, Offset(half, half), Size(d - ringW, d - ringW), style = Stroke(ringW, cap = StrokeCap.Round))
        }
        drawRoundRect(tile, Offset(inset, inset), Size(inner, inner), CornerRadius(inner * 0.27f))
        if (edge.alpha > 0f) drawRoundRect(edge, Offset(inset, inset), Size(inner, inner), CornerRadius(inner * 0.27f), style = Stroke(d * 0.02f))
        val pad = inner * 0.1f
        val px = (inner - 2 * pad).toInt().coerceAtLeast(1)
        drawImage(icon, IntOffset.Zero, IntSize(icon.width, icon.height), IntOffset((inset + pad).toInt(), (inset + pad).toInt()), IntSize(px, px), filterQuality = FilterQuality.High)
    }
}

/** Il colore dell'anello per lo stato della sessione, come i glifi di oggi: ambra chi aspetta, blu chi lavora, grigio chi è fermo. */
internal fun stateRing(s: Session): Color = when (s.state) {
    SessionState.WAITING, SessionState.AWAITING -> V3.Wait
    SessionState.BUSY -> V3.Busy
    else -> V3.Tx3
}

/**
 * Il binario delle righe (home, menu delle sessioni): la favicon con l'anello del colore del glifo, o il glifo di oggi senza favicon.
 * Stessa misura del glifo, così le righe con e senza sito restano allineate.
 */
@Composable
internal fun SessionRailIcon(s: Session, glyph: Rail.Glyph, ctx: Int?, square: Boolean, size: Dp = 20.dp) {
    val icon = siteFavicon(s)
    if (icon == null) StateGlyph(glyph, ctx, size = size, square = square)
    else SiteAvatar(icon, V3.tone(glyph), busy = glyph == Rail.Glyph.BUSY, size = size + 8.dp, label = s.name)
}
