package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R

/**
 * Il Genie dei menu (app-genie-menu-09-11, approvato da Franz il 09/11 22:14; riferimento in D:\tmp\menu-x): dal tasto premuto
 * cade una goccia che si gonfia fino al pannello in circa 0,3 s, e le voci passano da sfocate a nitide; chiudendo, lo stesso
 * all'indietro. Il pannello è sempre misurato alla sua misura: la goccia è la sua forma ritagliata, non una scala, così il testo
 * non si deforma. La sfocatura c'è da Android 12 (API 31); prima le voci arrivano solo in dissolvenza.
 */
object Genie {
    /** La durata dell'apertura; la chiusura è più svelta. */
    const val OPEN_MS = 320
    const val CLOSE_MS = 220
    /** Fin qui la goccia esce dal tasto; poi si gonfia. */
    const val DROP = .3f
    /** Da qui le voci arrivano, sfocate, e diventano nitide alla fine. */
    const val CONTENT = .55f

    /**
     * Il riquadro della goccia dentro il pannello [w]×[h] all'avanzamento [p], col centro orizzontale [cx] sotto il tasto: tonda
     * e un po' allungata mentre esce, poi si allarga fino al pannello con gli angoli che passano dal tondo a [r].
     */
    fun rect(p: Float, w: Float, h: Float, cx: Float, drop: Float, r: Float): RoundRect {
        val q = p.coerceIn(0f, 1f)
        if (q <= DROP) {
            val t = q / DROP
            val dw = drop * (.55f + .45f * t)
            val dh = drop * (.55f + .75f * t)
            val x = (cx - dw / 2).coerceIn(0f, (w - dw).coerceAtLeast(0f))
            return RoundRect(x, 0f, x + dw, dh, CornerRadius(dw / 2))
        }
        val t = ease((q - DROP) / (1f - DROP))
        val dw = drop; val dh = drop * 1.3f
        val x0 = (cx - dw / 2).coerceIn(0f, (w - dw).coerceAtLeast(0f))
        val left = x0 * (1 - t)
        val right = (x0 + dw) + (w - x0 - dw) * t
        val bottom = dh + (h - dh) * t
        val rad = dw / 2 + (r - dw / 2) * t
        return RoundRect(left, 0f, right, bottom, CornerRadius(rad))
    }

    /** Quanto si vedono le voci all'avanzamento [p]: niente fino a [CONTENT], poi a salire. */
    fun content(p: Float): Float = ((p - CONTENT) / (1f - CONTENT)).coerceIn(0f, 1f)

    private fun ease(t: Float): Float { val u = t.coerceIn(0f, 1f); return 1f - (1f - u) * (1f - u) * (1f - u) }
}

/** Solo per i provini: il Genie fermo a questo avanzamento (0 la goccia appena uscita, 1 il pannello aperto). */
internal val LocalGenieAt = staticCompositionLocalOf<Float?> { null }

private class GenieShape(private val rr: RoundRect) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = Outline.Rounded(rr)
}

/**
 * La forma del pannello mentre il Genie corre: ritagliata nella goccia, con l'ombra della goccia. [origin] dice da che parte sta il
 * tasto quando non si sa dove si è toccato (≡ a destra, il nome della sessione a sinistra).
 */
fun Modifier.genieBlob(origin: TransformOrigin, progress: () -> Float): Modifier = composed {
    val press = remember { LastPress.at }
    val at = remember { floatArrayOf(Float.NaN, Float.NaN) }
    this
        .onGloballyPositioned { c -> val o = c.positionOnScreen(); at[0] = o.x; at[1] = o.y }
        .graphicsLayer {
            val p = progress()
            val w = size.width; val h = size.height
            val cx = if (press != null && !at[0].isNaN()) press.x - at[0] else origin.pivotFractionX * w
            val rr = Genie.rect(p, w, h, cx, 44.dp.toPx(), 14.dp.toPx())
            clip = true
            shape = GenieShape(rr)
            shadowElevation = 8.dp.toPx() * p.coerceIn(0f, 1f)
            alpha = (p / .05f).coerceIn(0f, 1f)
        }
}

/** Le voci del pannello mentre il Genie corre: trasparenti e sfocate, poi nitide. */
fun Modifier.genieContent(progress: () -> Float): Modifier = graphicsLayer {
    val k = Genie.content(progress())
    alpha = k
    val blur = 10.dp.toPx() * (1f - k)
    renderEffect = if (blur > .5f && android.os.Build.VERSION.SDK_INT >= 31) androidx.compose.ui.graphics.BlurEffect(blur, blur) else null
}

/**
 * Il tasto che ha aperto il menu diventa ×: sopra lo sfondo oscurato, dove si è toccato, un tondo con la × che gira al suo posto
 * e chiude. Senza un tocco recente (un menu aperto da un test o da un altro gesto) non c'è, e chiude la × della testata.
 */
@Composable
fun GenieClose(press: Offset, progress: () -> Float, onClose: () -> Unit) {
    val origin = remember { floatArrayOf(Float.NaN, Float.NaN) }
    Box(Modifier.onGloballyPositioned { c -> val o = c.positionOnScreen(); origin[0] = o.x; origin[1] = o.y }) {
        Box(
            Modifier
                .offsetFrom(press, origin)
                .size(40.dp)
                .graphicsLayer { val k = (progress() / Genie.DROP).coerceIn(0f, 1f); alpha = k; rotationZ = 90f * (1f - k) }
                .clip(CircleShape).background(V3.S2)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .testTag("genie-chiudi"),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = V3.Tx, modifier = Modifier.size(22.dp)) }
    }
}

/** Il tondo centrato sul punto toccato, nelle coordinate della finestra del pannello. */
private fun Modifier.offsetFrom(press: Offset, origin: FloatArray): Modifier = this.layout { m, c ->
    val p = m.measure(c)
    layout(p.width, p.height) {
        val ox = if (origin[0].isNaN()) 0f else origin[0]
        val oy = if (origin[1].isNaN()) 0f else origin[1]
        p.place(IntOffset((press.x - ox - p.width / 2f).toInt(), (press.y - oy - p.height / 2f).toInt()))
    }
}
