package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.togetherWith
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.size
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private fun scheme(day: Boolean) = (if (day) lightColorScheme() else darkColorScheme()).copy(
    primary = V3.Act, onPrimary = V3.OnAct,
    background = V3.Bg, onBackground = V3.Tx,
    surface = V3.S1, onSurface = V3.Tx,
    surfaceVariant = V3.S2, onSurfaceVariant = V3.Tx2,
    surfaceContainer = V3.S2, surfaceContainerHigh = V3.S3,
    outline = V3.Line, error = V3.Red,
    secondaryContainer = V3.S3, onSecondaryContainer = V3.Tx,
    tertiary = V3.Wait,
    // Tutti i ruoli di superficie sui grigi neutri (app-audit-superfici-luci-04-11): lasciati ai valori di base del Material 3
    // davano ai fogli dal basso, ai dialoghi, ai menu e agli interruttori i grigi violacei della sua palette, e la tinta
    // d'elevazione li velava ancora.
    surfaceContainerLowest = V3.Bg, surfaceContainerLow = V3.S1, surfaceContainerHighest = V3.S3,
    surfaceBright = V3.S3, surfaceDim = V3.Bg, surfaceTint = androidx.compose.ui.graphics.Color.Transparent,
    outlineVariant = V3.Line, secondary = V3.Tx2, onSecondary = V3.Bg,
    primaryContainer = V3.S3, onPrimaryContainer = V3.Tx, tertiaryContainer = V3.S3, onTertiaryContainer = V3.Tx,
    inverseSurface = V3.Tx, inverseOnSurface = V3.Bg, inversePrimary = V3.Bg, scrim = androidx.compose.ui.graphics.Color.Black,
)

/** Un raggio per famiglia, come il web (audit grafica 04/11): card, liste e dialoghi 14, riquadri interni 12, fogli dal basso 28 in alto. */
private val cmShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(14.dp), extraLarge = RoundedCornerShape(14.dp),
)

/** Gerarchia forte: numeri grandi più pesanti, come quota e contatori sul polso; titoli un gradino sopra il corpo. */
/** Cifre a larghezza fissa: tabellone, quota, carico. */
const val CM_TNUM = "tnum"

private val cmType = Typography().run {
    copy(
        // Voce 15: le cifre grandi a larghezza fissa non tremolano quando cambiano.
        displayLarge = displayLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = CM_TNUM),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = CM_TNUM),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Medium),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
        // La scala unica con il web (docs/verifiche/2026-11-04-audit-grafica.md): Nome 17, Tasto 15, Corpo 15,5, Piccolo 14,
        // Misura 12,5, Minimo 11. bodyMedium è già Piccolo (14) e labelSmall già Minimo (11).
        titleSmall = titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = bodyLarge.copy(fontSize = 15.5.sp, lineHeight = 21.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 19.sp),
        bodySmall = bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
        labelLarge = labelLarge.copy(fontSize = 15.sp, lineHeight = 20.sp),
        labelMedium = labelMedium.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
    )
}

/** Negli snapshot tutto fermo allo stato finale: Paparazzi fotografa il primo fotogramma, prima di ogni animazione. */
val LocalStill = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Notte o giorno B dal sistema (app-giorno-b-30-10), tutto dallo stile v3; negli snapshot la notte, `day` per le prove del giorno, -Pv3.day=true per farle tutte di giorno. `still` per i test. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CmPhoneTheme(still: Boolean = false, day: Boolean = if (still) java.lang.Boolean.getBoolean("v3.day") else !isSystemInDarkTheme(), content: @Composable () -> Unit) {
    V3.day = day
    androidx.compose.runtime.CompositionLocalProvider(LocalStill provides still) {
        MaterialExpressiveTheme(colorScheme = scheme(day), motionScheme = MotionScheme.expressive(), shapes = cmShapes, typography = cmType) {
            // Negli snapshot del giorno la tela chiara: un componente senza fondo finirebbe sulla finestra scura del test.
            V3Theme { if (still && day) androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(V3.Bg)) { content() } else content() }
        }
    }
}

/**
 * Lo switch spento visibile sulle card (Franz, 02/10 15:49: «i tasti switch sono poco visibili»): Material lo disegna con
 * `outline`, che qui è il colore delle linee, quasi uguale al fondo della card.
 */
@Composable
fun cmSwitchColors() = SwitchDefaults.colors(
    uncheckedThumbColor = Cm.text2, uncheckedBorderColor = Cm.text2, uncheckedTrackColor = Cm.surfaceLow,
)

/** Tempi e curva del movimento: 250 ms e l'easing del sito; con le animazioni spente, subito lo stato finale. */
object CmMotion {
    val easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)
    fun <T> spec(off: Boolean): FiniteAnimationSpec<T> = if (off) snap() else tween(250, easing = easing)

    /**
     * Il pannello della master che sale dalla sua barra e ci torna (Franz, 03/10 17:12): una molla senza rimbalzo, che
     * riparte dal punto in cui si trova se il gesto cambia idea a metà.
     */
    val panel = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        visibilityThreshold = androidx.compose.ui.unit.IntOffset.VisibilityThreshold,
    )

    /** La stessa molla di [panel] per chi la guida in frazioni: il pannello della master quando riparte dal dito. */
    val panelFloat = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
    )

    /** Veloce, per pressione e tasti: svelta e senza rimbalzo (voce 14, docs/plans/2026-10-31-animazioni-app.md). */
    val fast = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
    )

    /**
     * Spaziale, per il posto di una voce (Franz, 31/10 21:15: «aggiungiamo anche un po' di rimbalzo sui movimenti»): circa
     * 5% di sforamento. Un solo stile: posizione e scala, mai colori, opacità, scorrimento automatico o dati dei push.
     */
    val bounce = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = 0.7f,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        visibilityThreshold = androidx.compose.ui.unit.IntOffset.VisibilityThreshold,
    )

    /** La stessa molla di [bounce] per la scala e per gli scostamenti in pixel. */
    val bounceFloat = androidx.compose.animation.core.spring<Float>(
        dampingRatio = 0.7f,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
    )

    /** La stessa molla di [bounce] per le altezze che si adattano. */
    val bounceSize = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = 0.7f,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        visibilityThreshold = androidx.compose.ui.unit.IntSize.VisibilityThreshold,
    )

    /** Lento, per colori e passaggi di sfondo: nessuno sforamento. */
    val slow = androidx.compose.animation.core.spring<androidx.compose.ui.graphics.Color>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
    )
}

/** Voce 7: una card nuova entra da altezza 0, così le altre non saltano sotto il dito. Alla prima composizione della home niente. */
@Composable
fun Modifier.growIn(isNew: Boolean): Modifier {
    val off = animationsOff()
    val f = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (isNew && !off) 0f else 1f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { f.animateTo(1f, tween(220, easing = CmMotion.easing)) }
    if (f.value >= 1f) return this
    return this.clipToBounds().layout { m, c ->
        val p = m.measure(c)
        layout(p.width, (p.height * f.value).toInt()) { p.place(0, 0) }
    }
}

/** Voce 10: una spunta verde che si disegna, una volta sola, quando non resta nessuno che aspetta. */
@Composable
fun AllDoneCheck(modifier: Modifier = Modifier) {
    val p = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { p.animateTo(1f, tween(400, easing = CmMotion.easing)) }
    val good = V3.Good
    androidx.compose.foundation.Canvas(modifier.size(32.dp)) {
        val a = androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .52f)
        val b = androidx.compose.ui.geometry.Offset(size.width * .42f, size.height * .74f)
        val c = androidx.compose.ui.geometry.Offset(size.width * .82f, size.height * .28f)
        val w = 3.dp.toPx(); val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val first = (p.value / .35f).coerceAtMost(1f)
        drawLine(good, a, a + (b - a) * first, w, cap)
        if (p.value > .35f) drawLine(good, b, b + (c - b) * ((p.value - .35f) / .65f), w, cap)
    }
}

/** Voce 4: un solo passaggio per tutti i tasti che lavorano: tasto, conferma, attesa, esito. Niente rimbalzo sulle dissolvenze. */
@Composable
fun <T> ActionPass(state: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val off = animationsOff()
    androidx.compose.animation.AnimatedContent(state, modifier, transitionSpec = {
        if (off) androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
        else (androidx.compose.animation.fadeIn(tween(200, easing = CmMotion.easing)) + androidx.compose.animation.scaleIn(tween(200, easing = CmMotion.easing), initialScale = 0.96f)) togetherWith androidx.compose.animation.fadeOut(tween(120))
    }, label = "action") { content(it) }
}

/** Voce 6: il tasto cede subito alla pressione, senza rimbalzo. Quasi invisibile, quindi regge l'uso frequente. */
@Composable
fun Modifier.pressScale(source: androidx.compose.foundation.interaction.MutableInteractionSource): Modifier {
    if (animationsOff()) return this
    val pressed by source.collectIsPressedAsState()
    val s by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.97f else 1f, CmMotion.fast, label = "press")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/**
 * La forma del tasto: la pillola, ferma anche al tocco (Franz 04/11 22:28, app-tasto-forma-tocco-04-11: «il tasto quadrato tondo al
 * clic non va bene»; prima l'effetto C la portava a un rettangolo di 9 dp). Al tocco resta solo la scala al 97 % ([pressScale]).
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun pressShape(source: androidx.compose.foundation.interaction.MutableInteractionSource): androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.CircleShape

/** Voce 13: le azioni decisive si sentono, una volta, alla conferma. */
fun android.view.View.confirmHaptic() { performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM) }

/**
 * Una voce che cambia posto in una colonna normale, dove `animateItem` non arriva: scivola al posto nuovo con [CmMotion.bounce].
 * Guarda l'indice e non la posizione: una card che si apre sopra sposta le altre senza farle rimbalzare.
 */
@Composable
fun Modifier.bounceOnMove(index: Int): Modifier {
    if (animationsOff()) return this
    val shift = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    val memo = androidx.compose.runtime.remember { floatArrayOf(Float.NaN, index.toFloat()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return this.then(
        Modifier.placedY { y ->
            val moved = !memo[0].isNaN() && memo[1] != index.toFloat() && memo[0] != y
            if (moved) { val from = memo[0] - y; scope.launch { shift.snapTo(shift.value + from); shift.animateTo(0f, CmMotion.bounceFloat) } }
            memo[0] = y; memo[1] = index.toFloat()
        },
    ).graphicsLayer { translationY = shift.value }
}

private fun Modifier.placedY(onY: (Float) -> Unit): Modifier =
    this.then(Modifier.onPlaced { c -> onY(c.positionInParent().y) })

/**
 * Un lavoro approvato in corso (voce 1): una linea blu indeterminata sul bordo basso. Non avanza come una percentuale,
 * perché il relay non la dà; con le animazioni spente resta ferma, così «in corso» si legge ancora.
 */
@Composable
fun WorkEdgeLine(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = V3.Busy) {
    val off = animationsOff()
    val fadeIn = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (off) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(off) { if (!off) fadeIn.animateTo(1f, tween(200, easing = CmMotion.easing)) }
    val at = if (off) 0f else androidx.compose.animation.core.rememberInfiniteTransition(label = "edge").animateFloat(
        0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(1600, easing = androidx.compose.animation.core.LinearEasing)), label = "edge-at",
    ).value
    Box(
        modifier.fillMaxWidth().height(2.dp).drawBehind {
            val a = fadeIn.value
            drawRect(color.copy(alpha = 0.22f * a))
            if (off) drawRect(color.copy(alpha = a))
            else {
                val w = size.width * 0.3f
                val x = -w + (size.width + w) * at
                drawRect(color.copy(alpha = a), topLeft = androidx.compose.ui.geometry.Offset(x, 0f), size = androidx.compose.ui.geometry.Size(w, size.height))
            }
        },
    )
}

/**
 * Le aperture sul posto (▼ delle card, giorni del Registro, gruppi di passaggi; osservazioni del 03/10, transizione 3):
 * l'altezza segue il contenuto in 200 ms con la curva dell'app, quasi impercettibile; con le animazioni spente, subito.
 */
@Composable
fun Modifier.smoothSize(): Modifier =
    this.animateContentSize(if (animationsOff()) snap() else tween(200, easing = CmMotion.easing))

/** «Riduci animazioni» o animazioni di sistema a zero; negli snapshot (LocalStill) sempre «spente»: stato finale. */
@Composable
fun animationsOff(): Boolean {
    if (LocalStill.current) return true
    if (LocalInspectionMode.current) return false
    val ctx = LocalContext.current
    return remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/**
 * Dove il dito ha toccato per ultimo, sullo schermo: da lì crescono i menu. La scrive `MainActivity`. Vale un secondo: un foglio
 * che si apre da solo, o dalla finestra di un altro menu, non nasce da un tocco vecchio.
 */
object LastPress {
    @Volatile private var where: androidx.compose.ui.geometry.Offset? = null
    @Volatile private var time = 0L
    fun set(at: androidx.compose.ui.geometry.Offset) { where = at; time = android.os.SystemClock.uptimeMillis() }
    val at: androidx.compose.ui.geometry.Offset? get() = where?.takeIf { android.os.SystemClock.uptimeMillis() - time < 1000 }
}

/**
 * Il cambio forma dei menu (Franz, 01/11: «l'animazione deve riguardare anche il contenuto»): pannello e contenuto crescono
 * insieme dal punto premuto, come un solo oggetto, da un quinto della grandezza e mai da zero. [progress] va da 0 a 1 e può
 * cambiare verso a metà. Senza un tocco recente cresce da [from], un punto dello schermo, o dall'angolo [origin] del menu.
 */
@Composable
fun Modifier.growFromPress(origin: androidx.compose.ui.graphics.TransformOrigin, from: androidx.compose.ui.geometry.Offset? = null, progress: () -> Float): Modifier {
    val press = androidx.compose.runtime.remember { LastPress.at ?: from }
    val at = androidx.compose.runtime.remember { floatArrayOf(Float.NaN, Float.NaN) }
    return this
        .onGloballyPositioned { c -> val o = c.positionOnScreen(); at[0] = o.x; at[1] = o.y }
        .graphicsLayer {
            // Sopra 1 è il rimbalzo della molla (Franz, 01/11: «troppo lineari… un piccolo rimbalzo»): la scala lo segue.
            val p = progress().coerceAtLeast(0f)
            val s = .2f + .8f * p
            scaleX = s; scaleY = s
            alpha = (p / .15f).coerceIn(0f, 1f)
            // La sfocatura leggera mentre appare (Franz, 01/11): sparisce a poco più di metà corsa. Android la dà da 12 in poi.
            val blur = 6.dp.toPx() * ((1f - p) / .6f).coerceIn(0f, 1f)
            renderEffect = if (blur > .5f && android.os.Build.VERSION.SDK_INT >= 31) androidx.compose.ui.graphics.BlurEffect(blur, blur, androidx.compose.ui.graphics.TileMode.Decal) else null
            transformOrigin = if (press != null && !at[0].isNaN() && size.width > 0f && size.height > 0f)
                androidx.compose.ui.graphics.TransformOrigin((press.x - at[0]) / size.width, (press.y - at[1]) / size.height) else origin
        }
}
