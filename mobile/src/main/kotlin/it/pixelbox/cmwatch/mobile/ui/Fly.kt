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
 * Il dito guida il volo (app-scorrimento-prima-sessione-09-11, video di Franz del 09/11 15:42): la frazione va verso quella del
 * dito un fotogramma alla volta, mai più svelta del tocco (0 → 1 in [FLY_MS]). Un salto lungo in un fotogramma solo, come uno
 * slancio, rompeva i bordi condivisi: la scheda spariva per qualche fotogramma e la pagina compariva piena di colpo, coi titoli
 * della scheda sopra la sua testata. Annullata da un valore nuovo del dito, riparte dalla frazione raggiunta; [go] falso (il dito
 * lasciato) la ferma prima del passo dopo, che toglierebbe il volo al rilascio.
 *
 * Un volo ancora in corso verso un'altra meta finisce lì prima che il dito lo prenda (secondo video di Franz, 09/11 18:18,
 * 4,65-5,01 s): richiusa la sessione col dito, il volo del ritorno dura ancora mezzo secondo dopo che la card è quasi al suo
 * posto, e il dito che la riapriva lo interrompeva a metà. La pagina che usciva rientrava dal suo stato di uscita: la card
 * spariva per due fotogrammi e la pagina entrava trasparente sopra la home.
 */
suspend fun androidx.compose.animation.core.SeekableTransitionState<String?>.follow(to: Float, target: String?, go: () -> Boolean = { true }) {
    if (currentState != targetState && targetState != target) {
        snapTo(targetState)
        repeat(2) { androidx.compose.runtime.withFrameNanos { } }
    }
    while (go()) {
        val from = if (targetState == target && currentState != target) fraction else 0f
        val next = if (to > from) minOf(to, from + FLY_STEP) else maxOf(to, from - FLY_STEP)
        seekTo(next, target)
        if (next == to) return
        androidx.compose.runtime.withFrameNanos { }
    }
}

/**
 * Il volo fino a [target] col passo del tocco: da fermo tutto [FLY_MS], dopo il dito il tempo che manca dalla frazione raggiunta
 * (app-scorrimento-prima-sessione-09-11). Tornando al punto di partenza, il volo resta quello della transizione.
 */
suspend fun androidx.compose.animation.core.SeekableTransitionState<String?>.land(target: String?) {
    val left = when {
        fraction == 0f -> 1f
        target == targetState -> 1f - fraction
        else -> null
    }
    animateTo(target, left?.let { l -> tween((l * FLY_MS).toInt().coerceAtLeast(1), easing = androidx.compose.animation.core.LinearEasing) })
}

/** Il passo più lungo del dito in un fotogramma: quello del tocco a 60 Hz. */
const val FLY_STEP = 1000f / 60f / FLY_MS

/** Il cambio fra la home e la sessione del telefono (transizione C, Franz 09/10 20:15). */
fun flySpec(opening: Boolean, flyByCard: Boolean, sideOff: Boolean): ContentTransform {
    val ease = tween<androidx.compose.ui.unit.IntOffset>(FLY_MS, easing = CmMotion.easing)
    return when {
        // La card che vola: lo fanno i bordi condivisi, il resto non si muove.
        flyByCard || sideOff -> EnterTransition.None togetherWith ExitTransition.None
        // Senza card la pagina entra da destra sopra la home, che arretra un poco; esce dallo stesso lato.
        opening -> (slideInHorizontally(CmMotion.bounce) { w -> w / 3 } + fadeIn(tween(FLY_MS))) togetherWith
            (slideOutHorizontally(ease) { w -> -w / 10 } + fadeOut(tween(FLY_MS / 2)))
        else -> ((slideInHorizontally(CmMotion.bounce) { w -> -w / 10 } + fadeIn(tween(FLY_MS))) togetherWith
            (slideOutHorizontally(ease) { w -> w / 3 } + fadeOut(tween(FLY_MS)))).apply { targetContentZIndex = -1f }
    }
}

/**
 * La scheda che diventa pagina o colonna e il ritorno (app-volo-titolo-telefono-05-11, regola Morph): mai due testi uno sopra
 * l'altro e mai un riquadro vuoto (app-morph-senza-stacco-06-11, scelta A): niente dissolvenze, dal primo fotogramma quello che
 * entra è pieno (Morph.inAlpha) e quello che esce non si vede (Morph.outAlpha); sotto, il fondo pieno (fullUnder); i bordi
 * restano sulla loro molla.
 */
internal fun morphEnter(off: Boolean): EnterTransition = EnterTransition.None

internal fun morphExit(off: Boolean): ExitTransition = if (off) ExitTransition.None else fadeOut(snap())

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

/**
 * Il contenuto che vola si scopre dall'angolo invece di reimpaginarsi (app-telefono-volo-pagina-06-11, scelta A di Franz 06/11
 * 16:03, come `::view-transition-new(morph)` della web): misurato subito alla sua misura d'arrivo, non scalato, fermo in alto a
 * sinistra e ritagliato dal riquadro che cresce, con gli angoli della scheda. Prima `RemeasureToBounds` lo reimpaginava a ogni
 * fotogramma dentro i bordi della scheda, con le righe che si stringevano e si riallargavano.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private val UNCOVER = SharedTransitionScope.ResizeMode.scaleToBounds(androidx.compose.ui.layout.ContentScale.None, androidx.compose.ui.Alignment.TopStart)

/** La card si allarga fino a diventare la scheda, e i due contenuti si danno il cambio (morphEnter, morphExit); con le animazioni spente, subito. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.fly(key: String): Modifier {
    val f = LocalFly.current?.takeIf { it.enabled } ?: return this
    val off = animationsOff()
    // La scheda si allarga nella pagina con la molla breve senza rimbalzo (app-telefono-volo-pagina-06-11, come --spring-land).
    val bounds = BoundsTransform { _, _ -> if (off) snap() else Fx.land() }
    return with(f.shared) {
        // Il fondo vola dentro la scheda (app-scorrimento-prima-sessione-09-11, video di Franz del 09/11 15:42), non sotto di lei
        // come per il tablet (fullUnder): quello aveva la misura ferma del suo contenuto e non volava, copriva di nero la home
        // dietro la scheda che cresceva, svaniva sopra di lei al ritorno e lasciava vuoto il posto della card. Una card di «Le
        // altre» ha il fondo del suo gruppo, non uno suo: tornando volava trasparente, col suo nome sopra quello delle altre.
        val card = rememberSharedContentState(key)
        val fill = Cm.surface
        this@fly.sharedBounds(
            card, f.anim,
            enter = morphEnter(off), exit = morphExit(off),
            boundsTransform = bounds,
            resizeMode = UNCOVER, zIndexInOverlay = 1f,
            clipInOverlayDuringTransition = OverlayClip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
        ).drawBehind { if (f.shared.isTransitionActive && card.isMatchFound) drawRect(fill) }
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
        // Le schede che solo scivolano restano piene (Franz 04/11) e si rimisurano senza deformarsi; la scheda della plancia che
        // diventa colonna si dà il cambio con lei e si scopre dall'angolo (UNCOVER).
        if (card) (if (!off) this@reflow.fullUnder(f, key, bounds) else this@reflow).sharedBounds(
            rememberSharedContentState(key), f.anim,
            enter = morphEnter(off), exit = morphExit(off),
            boundsTransform = bounds, resizeMode = UNCOVER, zIndexInOverlay = 1f,
            clipInOverlayDuringTransition = OverlayClip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
        ) else this@reflow.sharedBounds(
            rememberSharedContentState(key), f.anim,
            enter = EnterTransition.None, exit = ExitTransition.None,
            boundsTransform = bounds, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds, zIndexInOverlay = 0f,
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
            resizeMode = UNCOVER, zIndexInOverlay = 1f,
            clipInOverlayDuringTransition = OverlayClip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
        )
    }
}

/** Le colonne che quel contenuto mostrava: uscendo, le colonne restano quelle di prima finché la coreografia non finisce. */
val LocalDeskCols = compositionLocalOf<List<String>?> { null }

/** Lo stato di una delle due viste del tablet: la plancia, o le colonne aperte. */
data class DeskView(val dash: Boolean, val cols: List<String>)
