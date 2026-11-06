package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp

/** Le scene del «volo» card → scheda (design 24/09, «Movimento»); assenti negli snapshot, dove il modificatore non fa nulla. */
@OptIn(ExperimentalSharedTransitionApi::class)
class Fly(
    val shared: SharedTransitionScope, val anim: AnimatedVisibilityScope,
    /** Il volo c'è solo se la card toccata era sullo schermo (transizione C, 09/10); altrimenti la pagina entra di lato. */
    val enabled: Boolean = true,
)

/**
 * Le card delle sessioni sullo schermo della home, per nome (transizione C, Franz 09/10 20:15): una sessione aperta da qui vola
 * dalla sua card; aperta da menu, avviso o ricerca, senza card in vista, entra di lato come una pagina.
 */
val LocalCardsOnScreen = compositionLocalOf<androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>?> { null }

val LocalFly = compositionLocalOf<Fly?> { null }

/** La durata del volo card ↔ scheda. */
const val FLY_MS = 320

/**
 * La scheda che diventa pagina o colonna e il ritorno (app-volo-titolo-telefono-05-11, regola Morph): quello che esce sparisce
 * presto, quello che entra arriva dopo, mai due testi pieni uno sopra l'altro; i bordi restano sulla loro molla.
 */
internal fun morphEnter(off: Boolean): EnterTransition = if (off) EnterTransition.None
    else fadeIn(tween(it.pixelbox.cmwatch.rules.Morph.IN_MS, delayMillis = it.pixelbox.cmwatch.rules.Morph.IN_DELAY_MS, easing = androidx.compose.animation.core.LinearEasing))

internal fun morphExit(off: Boolean): ExitTransition = if (off) ExitTransition.None
    else it.pixelbox.cmwatch.rules.Morph.OUT_EASE.let { e -> fadeOut(tween(it.pixelbox.cmwatch.rules.Morph.OUT_MS, easing = androidx.compose.animation.core.CubicBezierEasing(e[0], e[1], e[2], e[3]))) }

/**
 * Il riquadro resta pieno mentre i due contenuti si danno il cambio (app-morph-senza-stacco-06-11, Franz 06/11 08:56: «un piccolo
 * stacco di fade out e fade in»): sotto di loro un fondo col colore della pagina vola con gli stessi limiti e non sparisce, e si
 * disegna solo durante il volo. Prima i due contenuti sfumavano col loro fondo, e per un decimo di secondo si vedeva la pagina
 * dietro. Il fondo vecchio lascia il posto al nuovo per tutto il volo; i contenuti stanno sopra (zIndexInOverlay).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.fullUnder(f: Fly, key: String, bounds: BoundsTransform): Modifier = with(f.shared) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    val fill = Cm.bg
    this@fullUnder.sharedBounds(
        rememberSharedContentState("$key#fondo"), f.anim,
        enter = EnterTransition.None, exit = fadeOut(tween(FLY_MS)),
        boundsTransform = bounds, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds, zIndexInOverlay = 0f,
    ).drawBehind { if (f.shared.isTransitionActive) drawOutline(shape.createOutline(size, layoutDirection, this), fill) }
}

/** La card si allarga fino a diventare la scheda, e i due contenuti si danno il cambio (morphEnter, morphExit); con le animazioni spente, subito. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.fly(key: String): Modifier {
    val f = LocalFly.current?.takeIf { it.enabled } ?: return this
    val off = animationsOff()
    // La scheda si allarga nella pagina e gli angoli si assestano con la molla (app-effetti-movimenti-04-11, punto 1).
    val bounds = BoundsTransform { _, _ -> if (off) snap() else Fx.spring() }
    return with(f.shared) {
        (if (off) this@fly else this@fly.fullUnder(f, key, bounds)).sharedBounds(
            rememberSharedContentState(key), f.anim,
            enter = morphEnter(off), exit = morphExit(off),
            boundsTransform = bounds,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds, zIndexInOverlay = 1f,
        )
    }
}

/**
 * La coreografia del tablet fra la plancia e le colonne (app-home-riposiziona-animata-04-11, Franz 04/11 18:03-18:04, come
 * `html.reflow` della web): aprendo la prima sessione la sua scheda si allarga nella colonna, le altre scivolano nella colonna
 * della home e la master va al suo posto; chiudendo l'ultima l'inverso. Ogni scheda resta riconoscibile, il testo si
 * rimisura e non si deforma, niente dissolvenze incrociate; durata e curva di HomeReflow, le stesse della web.
 */
val LocalReflow = compositionLocalOf<Fly?> { null }

/** La sessione che nella home cede il suo nome alla colonna aperta (`oneName(focus)` della web). */
val LocalReflowFocus = compositionLocalOf<String?> { null }

/** La sessione che diventa colonna, o lo è stata per ultima: la sua scheda della plancia e la colonna si danno il cambio (Morph). */
val LocalReflowCard = compositionLocalOf<String?> { null }

/** La plancia o le colonne, per il contenuto che entra o esce: durante la coreografia ci sono tutte e due. */
val LocalDeskDash = compositionLocalOf<Boolean?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.reflow(key: String): Modifier {
    val f = LocalReflow.current ?: return this
    if (key == "r-" + LocalReflowFocus.current) return this
    val off = animationsOff()
    val card = LocalDeskDash.current == true && key == "r-" + LocalReflowCard.current
    val e = it.pixelbox.cmwatch.rules.HomeReflow.EASE
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    // A onda dall'angolo in alto a sinistra (punto 6): chi sta più in basso e più a destra parte poco dopo.
    val bounds = BoundsTransform { from, _ ->
        if (off) snap() else {
            val d = it.pixelbox.cmwatch.rules.HomeReflow.waveDelay(from.top / density, from.left / density)
            tween(it.pixelbox.cmwatch.rules.HomeReflow.DURATION_MS - d, delayMillis = d, easing = androidx.compose.animation.core.CubicBezierEasing(e[0], e[1], e[2], e[3]))
        }
    }
    return with(f.shared) {
        // La scheda della plancia che diventa colonna ha sotto il fondo pieno (fullUnder).
        (if (card && !off) this@reflow.fullUnder(f, key, bounds) else this@reflow).sharedBounds(
            rememberSharedContentState(key), f.anim,
            // Le schede che solo scivolano restano piene (Franz 04/11); la scheda della plancia che diventa colonna si dà il cambio con lei.
            enter = if (card) morphEnter(off) else EnterTransition.None, exit = if (card) morphExit(off) else ExitTransition.None,
            boundsTransform = bounds,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds, zIndexInOverlay = if (card) 1f else 0f,
        )
    }
}

/** La colonna aperta prende il nome della sua scheda (la focus), che nella home non lo ha. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.reflowColumn(name: String): Modifier {
    if (LocalReflowFocus.current != name) return this
    val f = LocalReflow.current ?: return this
    val off = animationsOff()
    val e = it.pixelbox.cmwatch.rules.HomeReflow.EASE
    // La scheda vola nella colonna e si posa con la molla breve senza rimbalzo (app-sessione-apertura-06-11).
    val bounds = BoundsTransform { _, _ -> if (off) snap() else Fx.land() }
    return with(f.shared) {
        (if (off) this@reflowColumn else this@reflowColumn.fullUnder(f, "r-$name", bounds)).sharedBounds(
            rememberSharedContentState("r-$name"), f.anim,
            enter = morphEnter(off), exit = morphExit(off),
            boundsTransform = bounds,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds, zIndexInOverlay = 1f,
        )
    }
}

/** Le colonne che quel contenuto mostrava: uscendo, le colonne restano quelle di prima finché la coreografia non finisce. */
val LocalDeskCols = compositionLocalOf<List<String>?> { null }

/** Lo stato di una delle due viste del tablet: la plancia, o le colonne aperte. */
data class DeskView(val dash: Boolean, val cols: List<String>)
