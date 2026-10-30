package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Rail

/**
 * Lo stile «Studio e Diretta», terza versione (approvato da Franz il 30/10 alle 09:49, compito app-home-riordino-29-10), solo
 * sulla superficie home e solo di notte; gli stessi valori di web/src/v3.css. Il colore appartiene allo stato: ambra aspetta
 * te, blu lavora, verde fatto, rosso fallito; struttura e azioni neutre, l'unico pieno è bianco. Caratteri OFL in res/font.
 */
internal object V3 {
    val Bg = Color(0xFF0B0C0E); val S1 = Color(0xFF121316); val S2 = Color(0xFF1B1D22); val S3 = Color(0xFF272A31); val Line = Color(0xFF25272D)
    val Tx = Color(0xFFEDEEF0); val Tx2 = Color(0xFF969BA4); val Tx3 = Color(0xFF858A93); val Act = Color.White; val OnAct = Bg
    val Wait = Color(0xFFFFB23F); val Busy = Color(0xFF5AA2FF); val Good = Color(0xFF34D399); val Red = Color(0xFFFF5A5F)
    val Sans = FontFamily(Font(R.font.v3_barlow_regular, FontWeight.Normal), Font(R.font.v3_barlow_medium, FontWeight.Medium))
    val Display = FontFamily(Font(R.font.v3_barlow_sc_medium, FontWeight.Medium), Font(R.font.v3_barlow_sc_semibold, FontWeight.SemiBold), Font(R.font.v3_barlow_sc_bold, FontWeight.Bold))
    val Mono = FontFamily(Font(R.font.v3_geist_mono_regular, FontWeight.Normal), Font(R.font.v3_geist_mono_medium, FontWeight.Medium))
    val Section = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, color = Tx)
    val Name = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 20.sp, color = Tx)
    val Body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.5.sp, lineHeight = 21.sp, color = Tx)
    val Small = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp, color = Tx2)
    val Meter = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp, color = Tx2)
    val RailText = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 16.sp)
    val Rail = 52.dp
    val Shape = RoundedCornerShape(14.dp)

    fun tone(g: it.pixelbox.cmwatch.rules.Rail.Glyph) = when (g) {
        it.pixelbox.cmwatch.rules.Rail.Glyph.WAITING -> Wait
        it.pixelbox.cmwatch.rules.Rail.Glyph.BUSY -> Busy
        it.pixelbox.cmwatch.rules.Rail.Glyph.DONE -> Good
        it.pixelbox.cmwatch.rules.Rail.Glyph.RED -> Red
        it.pixelbox.cmwatch.rules.Rail.Glyph.STILL -> Tx3
    }
}

/** I glifi di stato a contorno, con i significati di Telegram (StateGlyph.svelte): ▶ con l'anello del contesto, tratteggio chi è fermo; il quadrato è l'account di lavoro. */
@Composable
internal fun StateGlyph(kind: Rail.Glyph, ctx: Int? = null, size: Dp = 20.dp, square: Boolean = false, label: String? = null) {
    val col = V3.tone(kind)
    val m = if (label != null) Modifier.semantics { contentDescription = label } else Modifier
    Canvas(m.size(size)) {
        val u = this.size.minDimension / 20f
        fun outline(width: Float, color: Color = col, effect: PathEffect? = null, sweep: Float = 360f) {
            val st = Stroke(width * u, cap = if (sweep < 360f) StrokeCap.Round else StrokeCap.Butt, pathEffect = effect)
            val tl = Offset(1.8f * u, 1.8f * u); val sz = Size(16.4f * u, 16.4f * u)
            if (square) {
                // L'arco del contesto su un quadrato: la parte in proporzione del perimetro, da mezzogiorno.
                val e = if (sweep < 360f) PathEffect.dashPathEffect(floatArrayOf(sweep / 360f * 65.6f * u, 200f * u)) else effect
                drawRoundRect(color, tl, sz, CornerRadius(4.5f * u), style = Stroke(width * u, cap = st.cap, pathEffect = e))
            } else drawArc(color, -90f, sweep, false, tl, sz, style = st)
        }
        when (kind) {
            Rail.Glyph.BUSY -> {
                outline(2f, col.copy(alpha = 0.28f))
                val pct = (ctx ?: 0).coerceIn(0, 100)
                if (pct > 0) outline(2f, sweep = pct * 3.6f)
                val p = Path().apply { moveTo(8.3f * u, 6.7f * u); lineTo(13.6f * u, 10f * u); lineTo(8.3f * u, 13.3f * u); close() }
                drawPath(p, col); drawPath(p, col, style = Stroke(1.2f * u))
            }
            Rail.Glyph.STILL -> outline(1.8f, effect = PathEffect.dashPathEffect(floatArrayOf(2.6f * u, 2.4f * u)))
            Rail.Glyph.WAITING -> { outline(1.8f); waitingMark(u, col) }
            Rail.Glyph.DONE -> { outline(1.8f); polyline(u, col, 6.4f to 10.2f, 8.8f to 12.6f, 13.6f to 7.6f) }
            Rail.Glyph.RED -> { outline(1.8f); polyline(u, col, 7.3f to 7.3f, 12.7f to 12.7f); polyline(u, col, 12.7f to 7.3f, 7.3f to 12.7f) }
        }
    }
}

private fun DrawScope.polyline(u: Float, col: Color, vararg pts: Pair<Float, Float>) {
    val p = Path().apply { pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) } }
    drawPath(p, col, style = Stroke(1.8f * u, cap = StrokeCap.Round))
}

private fun DrawScope.waitingMark(u: Float, col: Color) {
    drawArc(col, 170f, 270f, false, Offset(7.7f * u, 5.7f * u), Size(4.8f * u, 4.8f * u), style = Stroke(1.7f * u, cap = StrokeCap.Round))
    drawLine(col, Offset(10.6f * u, 10.2f * u), Offset(10f * u, 12f * u), 1.7f * u, StrokeCap.Round)
    drawCircle(col, 1.05f * u, Offset(10f * u, 14.7f * u))
}

/** Il titolo col binario: l'icona grigia allineata ai glifi, il titolo ai nomi, il numero accanto; a destra il riepilogo e l'apertura. */
@Composable
internal fun V3Section(title: String, icon: ImageVector, count: String? = null, right: String? = null, tag: String? = null, open: Boolean? = null, onToggle: (() -> Unit)? = null) {
    var m = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)
    if (onToggle != null) m = m.handCursor().clickable(onClick = onToggle)
    if (tag != null) m = m.testTag(tag)
    Row(m.heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(V3.Rail), contentAlignment = Alignment.Center) { Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(8.dp))
        Text(title, style = V3.Section, maxLines = 1)
        count?.let { Text(it, style = V3.Section.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = V3.Tx2), modifier = Modifier.padding(start = 8.dp)) }
        Spacer(Modifier.weight(1f))
        right?.let { Text(it, style = V3.Small, maxLines = 1, overflow = TextOverflow.Clip) }
        if (open != null) Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.padding(start = 6.dp).size(20.dp))
    }
}

/** La lista a filo: un fondo solo, con le righe separate dal filo. */
@Composable
internal fun V3List(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1)) { content() }
}

/** Una riga della lista col binario: icona o glifo a sinistra, poi il testo, in fondo quello che serve. */
@Composable
internal fun V3Row(first: Boolean = false, onClick: (() -> Unit)? = null, rail: @Composable () -> Unit, trailing: @Composable RowScope.() -> Unit = {}, body: @Composable () -> Unit) {
    if (!first) Box(Modifier.fillMaxWidth().padding(start = 0.dp).heightIn(min = 1.dp, max = 1.dp).background(V3.Line))
    var m = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    if (onClick != null) m = m.handCursor().clickable(onClick = onClick)
    Row(m.padding(top = 10.dp, bottom = 10.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(V3.Rail), contentAlignment = Alignment.Center) { rail() }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { body() }
        trailing()
    }
}

/** La pillola tonale delle righe (Fatto, Avvia, Aggiungi): mai piena, salvo l'unico bottone pieno della schermata. */
@Composable
internal fun V3Pill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    androidx.compose.material3.Surface(onClick = onClick, shape = androidx.compose.foundation.shape.CircleShape, color = if (filled) V3.Act else V3.S3, contentColor = if (filled) V3.OnAct else V3.Tx) {
        Text(text, style = V3.Name.copy(fontSize = 15.sp, color = if (filled) V3.OnAct else V3.Tx), maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
    }
}
