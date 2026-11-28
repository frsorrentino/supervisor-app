package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.rules.BordoMaster

/**
 * Solo per i provini: il raggio fermo in questo punto del giro (il tempo in ms di [BordoMaster.posizione]), anche con le animazioni spente;
 * così l'immagine non dipende dall'ora. Null nell'app.
 */
internal val LocalBordoFermo = staticCompositionLocalOf<Long?> { null }

private val BordoColore = Color(BordoMaster.COLORE)
private val BordoCurva = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
/** La sfumatura del raggio da davanti a dietro: il colore del bordo con le opacità di [BordoMaster.LUCE], mai il nero trasparente. */
private val BordoLuce: Array<Pair<Float, Color>> = BordoMaster.LUCE.map { (x, a) -> x to BordoColore.copy(alpha = a) }.toTypedArray()

/**
 * app-bordo-master-27-11: il bordo della master, come nel mockup (dalla-master/bordo-master-2026-11-27/bordo-master.html; le regole in
 * [BordoMaster], le stesse del web). Un anello fermo di 1,5 dp, sempre; mentre la master lavora ([lavora]: il suo glifo «al lavoro») un
 * raggio corre lungo il bordo verso antiorario, un giro ogni 15 s, e si accende e si spegne in 240 ms con la curva cubic-bezier(.23, 1, .32,
 * 1); spegnendosi gira ancora. Il raggio è quello del Border Beam del web: una fascia diritta di 320 × 24 dp lungo la tangente del bordo nel
 * punto del giro, con la sfumatura lineare dal davanti al dietro, che colora solo l'anello. Un solo valore che si muove, il tempo nel giro,
 * letto nel disegno: la transizione infinita c'è solo mentre il raggio gira e le animazioni non sono spente, così da ferma la master non
 * tiene lo schermo a 120 Hz. Con le animazioni spente niente giro: mentre lavora l'anello sale al 35 %.
 * app-bordo-foglio-28-11 (Franz, variante B di dalla-master/bordo-foglio-28-11.html): non più sul campo ma attorno a tutto il foglio della
 * master ([MasterSheetBox]), alla riga, a metà e intero: gli angoli in alto tondi di [angolo] px (letto qui, così il percorso segue gli
 * angoli che si squadrano all'intera), il lato di sotto diritto sul bordo dello schermo.
 */
@Composable
internal fun Modifier.bordoMaster(lavora: Boolean, angolo: () -> Float): Modifier {
    val fermo = LocalBordoFermo.current
    // Nei provini col raggio fermo ([LocalBordoFermo]) il raggio si disegna anche con le animazioni spente, fermo in quel punto.
    val ridotto = animationsOff() && fermo == null
    val gira = BordoMaster.gira(lavora, ridotto)
    val anello = BordoMaster.anello(lavora, ridotto)
    // La luce del raggio; il giro c'è finché è accesa, anche mentre si spegne.
    val luce = remember { Animatable(if (gira) 1f else 0f) }
    var corre by remember { mutableStateOf(gira && fermo == null) }
    LaunchedEffect(gira, fermo) {
        if (fermo != null) return@LaunchedEffect
        if (gira) { corre = true; luce.animateTo(1f, tween(BordoMaster.SFUMA_MS, easing = BordoCurva)) }
        else { luce.animateTo(0f, tween(BordoMaster.SFUMA_MS, easing = BordoCurva)); corre = false }
    }
    // Il punto del giro viene dall'orologio, come sul web: web e telefono mostrano il raggio nello stesso punto.
    val inizio = remember(corre) { System.currentTimeMillis() }
    val giro = if (corre) rememberInfiniteTransition(label = "bordo-master").animateFloat(
        0f, BordoMaster.GIRO_MS.toFloat(), infiniteRepeatable(tween(BordoMaster.GIRO_MS.toInt(), easing = LinearEasing)), label = "giro",
    ) else null
    return drawWithCache {
        val w = BordoMaster.SPESSORE_DP.dp.toPx()
        val bordo = foglio(size, w / 2f, angolo())
        val misura = PathMeasure().apply { setPath(bordo, false) }
        val tratto = Stroke(width = w)
        val lungo = BordoMaster.RAGGIO_DP.dp.toPx()
        val mezza = BordoMaster.FASCIA_DP.dp.toPx() / 2f
        val fascia = Path()
        onDrawWithContent {
            drawContent()
            drawPath(bordo, BordoColore.copy(alpha = anello), style = tratto)
            val ms = when {
                fermo != null -> fermo.takeIf { lavora }
                giro != null -> inizio + giro.value.toLong()
                else -> null
            } ?: return@onDrawWithContent
            val accesa = if (fermo != null) 1f else luce.value
            if (accesa <= 0f || misura.length <= 0f) return@onDrawWithContent
            // Il punto del giro sul bordo, la direzione del bordo lì (in senso orario) e la sua normale: davanti, verso cui il raggio va
            // girando al contrario, il primo terzo della luce; dietro gli altri due.
            val d = BordoMaster.posizione(ms) * misura.length
            val p = misura.getPosition(d)
            val t = misura.getTangent(d)
            val n = Offset(-t.y, t.x) * mezza
            val davanti = p - t * (lungo * BordoMaster.CAPO)
            val dietro = p + t * (lungo * (1f - BordoMaster.CAPO))
            fascia.rewind()
            fascia.moveTo(davanti.x + n.x, davanti.y + n.y)
            fascia.lineTo(dietro.x + n.x, dietro.y + n.y)
            fascia.lineTo(dietro.x - n.x, dietro.y - n.y)
            fascia.lineTo(davanti.x - n.x, davanti.y - n.y)
            fascia.close()
            clipPath(fascia) {
                drawPath(bordo, Brush.linearGradient(*BordoLuce, start = davanti, end = dietro), alpha = accesa, style = tratto)
            }
        }
    }
}

/**
 * Il contorno del foglio come percorso chiuso, sulla linea a metà dell'anello ([i] dal bordo): gli angoli in alto tondi di [raggio]
 * all'esterno, quelli in basso diritti. Parte dall'estremo sinistro del lato di sopra e gira in senso orario, come `rect()` in CSS, così
 * [BordoMaster.posizione] è lo stesso punto sul web e qui. Con [raggio] 0 (il foglio intero) è un rettangolo.
 */
private fun foglio(s: Size, i: Float, raggio: Float): Path {
    val r = minOf(raggio - i, (s.height - 2f * i) / 2f, (s.width - 2f * i) / 2f).coerceAtLeast(0f)
    val sx = i
    val su = i
    val dx = s.width - i
    val giu = s.height - i
    return Path().apply {
        moveTo(sx + r, su)
        lineTo(dx - r, su)
        if (r > 0f) arcTo(Rect(dx - 2f * r, su, dx, su + 2f * r), -90f, 90f, false)
        lineTo(dx, giu)
        lineTo(sx, giu)
        lineTo(sx, su + r)
        if (r > 0f) arcTo(Rect(sx, su, sx + 2f * r, su + 2f * r), 180f, 90f, false)
        close()
    }
}
