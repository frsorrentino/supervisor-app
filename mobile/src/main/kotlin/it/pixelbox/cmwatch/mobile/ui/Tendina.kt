package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SessionTabs

/** Quanto dura la cascata delle righe: l'ultima riga che aspetta più tempo, più la sua comparsa. */
private val ROWS_MS = (SessionTabs.rowDelay(SessionTabs.MAX_STAGGER) + SessionTabs.SHADE_MS).toFloat()

/** L'orologio delle righe della tendina aperta, in ms dall'apertura; fuori dalla tendina non c'è, e le righe stanno ferme. */
internal val LocalTendinaClock = compositionLocalOf<(() -> Float)?> { null }

/**
 * La tendina della sessione (app-posizioni-sistema-27-11, gruppo 2), come Tendina.svelte: scende dalla riga di icone sopra la chat,
 * fino al campo, che resta sempre in vista e usabile. È solo il contenitore: il contenuto lo passa chi la usa (che si occupa anche di
 * scorrerlo), e il centro notifiche (app-centro-notifiche-26-11) cambierà il contenuto, non il posto. Scende e sale in 360 ms con
 * FastOutSlowInEasing; le righe del contenuto segnate con [tendinaRow] compaiono a 32 ms l'una dall'altra, a ogni apertura e a ogni
 * cambio di sezione ([key]). Si chiude tirandola su dalla maniglia in fondo (oltre un quarto o con uno slancio,
 * SessionTabs.closesOnDrag) o col tocco sulla maniglia. Con le animazioni spente, e nelle immagini di prova, compare al suo posto, ferma.
 */
@Composable
fun Tendina(open: Boolean, key: Any, onClose: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val off = animationsOff()
    val p = remember { Animatable(if (open) 1f else 0f) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(open, off) {
        val to = if (open) 1f else 0f
        if (off) p.snapTo(to) else p.animateTo(to, tween(SessionTabs.SHADE_MS, easing = FastOutSlowInEasing))
    }
    // Da 0 a ROWS_MS a ogni apertura e a ogni cambio di sezione; fermo in fondo, tutte le righe ci sono.
    val clock = remember { Animatable(ROWS_MS) }
    LaunchedEffect(open, key, off) {
        if (!open || off) { clock.snapTo(ROWS_MS); return@LaunchedEffect }
        clock.snapTo(0f)
        clock.animateTo(ROWS_MS, tween(ROWS_MS.toInt(), easing = LinearEasing))
    }
    val down by remember { derivedStateOf { p.value > 0f } }
    if (!open && !down) return
    val density = LocalDensity.current
    val shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
    Box(modifier.clipToBounds().onSizeChanged { heightPx = it.height }) {
        Column(
            Modifier.fillMaxSize()
                .graphicsLayer { translationY = (p.value - 1f) * size.height + dragPx }
                .shadow(10.dp, shape, clip = false).clip(shape).background(V3.Bg, shape)
                // I tocchi sul fondo della tendina restano suoi: non arrivano alla chat sotto.
                .pointerInput(Unit) { detectTapGestures { } }
                .testTag("tendina"),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CompositionLocalProvider(LocalTendinaClock provides { clock.value }) { content() }
            }
            val closeLabel = stringResource(R.string.shade_close)
            Box(
                Modifier.fillMaxWidth().height(26.dp)
                    .draggable(
                        rememberDraggableState { d -> dragPx = SessionTabs.dragOffset(dragPx + d, heightPx.toFloat()) },
                        Orientation.Vertical,
                        onDragStopped = { v ->
                            // La velocità in dp al secondo, come la soglia dello slancio.
                            val vDp = with(density) { v.toDp().value }
                            if (SessionTabs.closesOnDrag(dragPx, vDp, heightPx.toFloat())) {
                                // Si riparte da dove il dito l'ha lasciata: la salita continua da lì.
                                p.snapTo((1f + dragPx / heightPx.coerceAtLeast(1)).coerceIn(0f, 1f))
                                dragPx = 0f
                                onClose()
                            } else {
                                animate(dragPx, 0f, animationSpec = tween(SessionTabs.SHADE_MS, easing = FastOutSlowInEasing)) { x, _ -> dragPx = x }
                            }
                        },
                    )
                    .handCursor().clickable(onClickLabel = closeLabel, role = Role.Button) { onClose() }
                    .testTag("tendina-maniglia"),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3)) }
        }
    }
}

/**
 * Una riga del contenuto della tendina, la [i]-esima da 0: compare SessionTabs.rowDelay(i) dopo l'apertura, in 360 ms con
 * FastOutSlowInEasing, scendendo di 6 dp. Si muove solo il disegno. Fuori dalla tendina non fa niente.
 */
@Composable
fun Modifier.tendinaRow(i: Int): Modifier {
    val clock = LocalTendinaClock.current ?: return this
    val lift = with(LocalDensity.current) { 6.dp.toPx() }
    return graphicsLayer {
        val f = FastOutSlowInEasing.transform(((clock() - SessionTabs.rowDelay(i)) / SessionTabs.SHADE_MS).coerceIn(0f, 1f))
        alpha = f
        translationY = (f - 1f) * lift
    }
}
