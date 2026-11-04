package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

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

/** La card si allarga fino a diventare la scheda, senza dissolvenza finale; con le animazioni spente, subito. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.fly(key: String): Modifier {
    val f = LocalFly.current?.takeIf { it.enabled } ?: return this
    val off = animationsOff()
    return with(f.shared) {
        this@fly.sharedBounds(
            rememberSharedContentState(key), f.anim,
            enter = EnterTransition.None, exit = ExitTransition.None,
            // La scheda si allarga nella pagina e gli angoli si assestano con la molla (app-effetti-movimenti-04-11, punto 1).
            boundsTransform = { _, _ -> if (off) snap() else Fx.spring() },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
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

/** La plancia o le colonne, per il contenuto che entra o esce: durante la coreografia ci sono tutte e due. */
val LocalDeskDash = compositionLocalOf<Boolean?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.reflow(key: String): Modifier {
    val f = LocalReflow.current ?: return this
    if (key == "r-" + LocalReflowFocus.current) return this
    val off = animationsOff()
    val e = it.pixelbox.cmwatch.rules.HomeReflow.EASE
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    return with(f.shared) {
        this@reflow.sharedBounds(
            rememberSharedContentState(key), f.anim,
            enter = EnterTransition.None, exit = ExitTransition.None,
            // A onda dall'angolo in alto a sinistra (punto 6): chi sta più in basso e più a destra parte poco dopo.
            boundsTransform = { from, _ ->
                if (off) snap() else {
                    val d = it.pixelbox.cmwatch.rules.HomeReflow.waveDelay(from.top / density, from.left / density)
                    tween(it.pixelbox.cmwatch.rules.HomeReflow.DURATION_MS - d, delayMillis = d, easing = androidx.compose.animation.core.CubicBezierEasing(e[0], e[1], e[2], e[3]))
                }
            },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
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
    return with(f.shared) {
        this@reflowColumn.sharedBounds(
            rememberSharedContentState("r-$name"), f.anim,
            enter = EnterTransition.None, exit = ExitTransition.None,
            // La scheda vola nella colonna con la molla che supera di poco la misura e torna (punto 1).
            boundsTransform = { _, _ -> if (off) snap() else Fx.spring() },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}

/** Le colonne che quel contenuto mostrava: uscendo, le colonne restano quelle di prima finché la coreografia non finisce. */
val LocalDeskCols = compositionLocalOf<List<String>?> { null }

/** Lo stato di una delle due viste del tablet: la plancia, o le colonne aperte. */
data class DeskView(val dash: Boolean, val cols: List<String>)
