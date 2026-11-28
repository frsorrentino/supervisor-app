package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.Animazioni
import it.pixelbox.cmwatch.rules.MasterGestures
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.MasterSheet
import it.pixelbox.cmwatch.rules.Rail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val SHEET_HM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * La master è la superficie più alta dell'app (Celeste velato, Franz 06/10 21:15-22:27; dal 30/10 i neutri dello stile «Studio e
 * Diretta», HomeV3.kt): i suoi toni, gli stessi della web app. Stavano con la barra di prima, uscita con app-posizioni-sistema-27-11 gruppo 6.
 */
internal val MasterHighest get() = V3.S2
internal val MasterHigh get() = V3.S1
internal val MasterContainer get() = V3.S1

/**
 * Il posto della riga del foglio della master in fondo a una lista che gli sta sotto e non si sposta: le colonne della plancia del tablet
 * (app-posizioni-sistema-27-11 gruppo 6, come `--sheet-row` di Plancia.svelte). SummaryList lo aggiunge in fondo al suo scorrimento.
 */
internal val LocalSheetRoom = androidx.compose.runtime.compositionLocalOf { 0.dp }

/**
 * Chi vuole sapere dove comincia il foglio della master, in px dalla radice (null quando non c'è più): la corsia del tablet, che non
 * mette il + né il filo accanto alle schede della home coperte dal foglio (app-posizioni-sistema-27-11 gruppo 6, come SwapPills.svelte).
 */
internal val LocalSheetTop = androidx.compose.runtime.staticCompositionLocalOf<((Float?) -> Unit)?> { null }

/**
 * La riga della master nel suo foglio (app-posizioni-sistema-27-11 gruppo 4, al posto della barra di prima): il glifo dello stato, «Master»
 * col punto ambra quando arriva una risposta a foglio chiuso ([closed]), l'ora dell'ultima risposta che scorre e il suo titolo su una
 * riga, ▶ per ascoltarla (■ mentre legge, con la velocità accanto), e il ⋮ della master (LocalHeadMore). I gesti sono del foglio
 * ([MasterSheetBox]): qui toccano solo ▶ e ⋮. app-barra-stato-28-11 (Franz 14:01): l'ultimo esito sotto il titolo solo col foglio alla
 * riga ([closed]); aperto, al suo posto c'è la barra di stato, subito sotto, e «Master» va al centro della riga, che resta alta 52 dp.
 */
@Composable
fun MasterSheetHead(master: Session, hero: MasterHome.Hero?, onSpeak: (String) -> Unit, closed: Boolean = true) {
    val time = hero?.at?.let { at -> SHEET_HM.format(Instant.ofEpochSecond(at).atZone(ZoneId.systemDefault())) }
    val ctx = master.context?.let { c -> stringResource(R.string.ctx_short, c) }
    val label = listOfNotNull(stringResource(R.string.dock_master), time, ctx).joinToString(" · ")
    val off = animationsOff()
    // app-messaggio-master-effetto-27-11: l'ora scorre (RollTime) e il titolo nuovo entra in 150 ms, non nel primo secondo; il punto ambra
    // dice per DOCK_PEEK_MS che è arrivata una risposta, come la barra di prima.
    var roll by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(StreamGate.OPENING_MS); roll = true }
    var fresh by remember { mutableStateOf(false) }
    var seenAt by remember { mutableStateOf(hero?.at) }
    val headA = remember { Animatable(1f) }
    LaunchedEffect(hero?.at) {
        val at = hero?.at
        val isNew = at != null && seenAt != null && at != seenAt
        seenAt = at
        if (isNew && roll && !off) { headA.snapTo(0f); headA.animateTo(1f, tween(150)) }
        if (isNew) { fresh = true; delay(Animazioni.DOCK_PEEK_MS); fresh = false }
    }
    Row(
        // app-foglio-master-testata-28-11: il titolo e l'ultimo esito a 16 dp dal bordo, come le schede dell'app; a destra ▶ e ⋮ coi loro 44 dp.
        Modifier.fillMaxWidth().height(52.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StateGlyph(Rail.glyph(master), master.context, size = 20.dp, square = !Accounts.personal(master.account, master.accountKind), label = label)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.dock_master).lowercase().replaceFirstChar { c -> c.titlecase() }, style = V3.Name.copy(fontSize = 15.5.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                if (fresh && closed) Box(Modifier.size(8.dp).clip(CircleShape).background(V3.Wait).testTag("master-nuova"))
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = closed && (time != null || hero != null),
                enter = androidx.compose.animation.fadeIn(tween(180)) + androidx.compose.animation.expandVertically(tween(180)),
                exit = androidx.compose.animation.fadeOut(tween(180)) + androidx.compose.animation.shrinkVertically(tween(180)),
            ) { Row(verticalAlignment = Alignment.CenterVertically) {
                time?.let { tm -> RollTime(tm, roll, V3.Meter) }
                if (time != null && hero != null) Text(" · ", style = V3.Small, maxLines = 1)
                // Una riga sola: quello che non ci sta si taglia sul bordo, senza puntini.
                hero?.let { h -> Text(linked(h.headline), style = V3.Small, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.graphicsLayer { alpha = headA.value }) }
            } }
        }
        val spoken = hero?.let { h -> listOf(h.headline, h.body).filter { x -> x.isNotBlank() }.joinToString("\n") }
        val reading = spoken != null && LocalSpeaking.current == spoken
        if (reading) RatePill()
        if (spoken != null) IconButton(onClick = { onSpeak(spoken) }, modifier = Modifier.size(44.dp)) {
            Icon(if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(if (reading) R.string.stop_reading else R.string.read_aloud), tint = Cm.text)
        }
        // Il ⋮ della master (gruppo 4): quello della riga dei dati, che nel foglio non c'è; lo passa SessionSheet (MasterSheetMore).
        LocalHeadMore.current?.invoke()
    }
}

/**
 * La maniglia del foglio: 40 × 4 dp al centro, come i fogli del sistema. app-foglio-master-testata-28-11 (Franz 14:37, «la barra in alto e il
 * padding esiguo tra testo e bordo la rendono poco equilibrata»): una fascia sua, 8 dp sopra e 12 sotto, staccata dal titolo.
 */
@Composable
private fun SheetGrip() {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(V3.Tx3))
    }
}

/**
 * Il foglio della master sul telefono (app-posizioni-sistema-27-11 gruppo 4, B «Foglio»; le regole in [MasterSheet], le stesse di
 * web/src/lib/MasterSheet.svelte). Sta sopra la home ([under]), che non si sposta mai e lascia in fondo solo il posto della riga; il campo
 * della master sta sotto, fuori, sempre in vista (SessionSheet). Tre misure: la riga (maniglia e [head]), metà del posto, intera (tutto il
 * posto sotto la testata meno lo stacco). La maniglia e la riga si trascinano 1:1, oltre la riga e l'intera con l'elastico; al rilascio la
 * velocità si proietta come su iOS (0,998) e il foglio va alla misura più vicina al punto proiettato, con la molla senza rimbalzo della
 * master che parte dalla velocità del dito ([MasterGestures.land]: smorzamento critico, mai oltre la misura). Il tocco passa alla misura
 * dopo (riga → metà → intera → riga). Il foglio cambia altezza nella fase di misura e di disegno, la home sotto non si ricompone a ogni
 * fotogramma. Con le animazioni spente (e nei provini) va subito alla misura.
 * app-posizioni-sistema-27-11, prova 1447 (Franz: «sembra quasi che si sta alzando solo l'header», «si confonde con il testo della home»):
 * la riga e la conversazione ([body]) sono un blocco solo, opaco e rigido: la conversazione è appesa sotto la riga e sale con lei, e il
 * campo, fermo sotto il foglio, la taglia. Mentre il foglio si muove la sua altezza è ferma ([MasterSheet.freezeAt]: dalla riga quanta se
 * ne vedrà all'arrivo; scendendo resta rigida; salendo si allunga col foglio); da fermo torna alta quanto si vede e [onSettled] riporta
 * l'ultimo messaggio sopra il campo con una corsa breve. Dietro, la home si vela e arretra con l'avanzamento ([MasterSheet.cues]); sopra il
 * bordo un'ombra sfuma verso il nero. Con le animazioni spente la home non arretra, resta il velo; sulla plancia del tablet, dove sotto
 * c'è una lista vuota, si vela solo la colonna sotto il foglio.
 */
@Composable
internal fun MasterSheetBox(
    stop: MasterSheet.Stop, onStop: (MasterSheet.Stop) -> Unit, modifier: Modifier = Modifier,
    /** Solo per i provini: il foglio fermo a questa frazione fra la riga e l'intera, come a metà di un trascinamento. */
    pull: Float? = null,
    under: @Composable ColumnScope.() -> Unit,
    head: @Composable () -> Unit,
    body: @Composable ColumnScope.() -> Unit,
    /** A foglio fermo dopo un movimento: l'ultimo messaggio appena sopra il campo, con una corsa breve, se la si seguiva (prova 1447). */
    onSettled: () -> Unit = {},
    /** La master al lavoro (il suo glifo): il raggio del suo bordo gira attorno al foglio (app-bordo-foglio-28-11). */
    lavora: Boolean = false,
) {
    val density = LocalDensity.current
    val off = animationsOff()
    val sheetTop = LocalSheetTop.current
    androidx.compose.runtime.DisposableEffect(sheetTop) { onDispose { sheetTop?.invoke(null) } }
    BoxWithConstraints(modifier.clipToBounds()) {
        val boxPx = constraints.maxHeight.toFloat()
        var headPx by remember { mutableIntStateOf(0) }
        // Prima della misura, la riga di [MasterSheetHead]: maniglia 24 dp (app-foglio-master-testata-28-11) e riga 52.
        val rowPx = if (headPx > 0) headPx.toFloat() else with(density) { 76.dp.toPx() }
        val a = MasterSheet.anchors(boxPx, rowPx, with(density) { MasterSheet.GAP_DP.dp.toPx() })
        val target = a.of(stop)
        val height = remember { Animatable(target) }
        // Il dito tiene il foglio; la velocità del rilascio per la molla; un rilascio sulla stessa misura rifà comunque la corsa.
        val held = remember { booleanArrayOf(false) }
        val fling = remember { floatArrayOf(0f) }
        val ready = remember { booleanArrayOf(false) }
        var settled by remember { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()
        val aNow by rememberUpdatedState(a)
        val stopNow by rememberUpdatedState(stop)
        val onStopNow by rememberUpdatedState(onStop)
        val onSettledNow by rememberUpdatedState(onSettled)
        val boxNow by rememberUpdatedState(boxPx)
        val pullNow by rememberUpdatedState(pull)
        // Prova 1447: l'altezza ferma della conversazione durante il movimento, in px (-1 da fermo), e la misura da cui è partito. Si ferma
        // all'inizio ([MasterSheet.freezeAt]) e non si accorcia finché il foglio non si ferma; se il foglio la supera, si allunga con lui.
        // Le usano anche i gesti, che tengono le funzioni della prima composizione: dentro solo stati e cose ricordate.
        val frozen = remember { mutableFloatStateOf(-1f) }
        val base = remember { arrayOf(stop) }
        val freeze: (MasterSheet.Stop?) -> Unit = { to -> frozen.floatValue = maxOf(frozen.floatValue, MasterSheet.freezeAt(aNow, height.value, base[0], to)) }
        val grow: (Float) -> Unit = { h -> if (frozen.floatValue >= 0f) frozen.floatValue = maxOf(frozen.floatValue, h - aNow.row) }
        LaunchedEffect(target, off, settled, pull, headPx > 0) {
            if (held[0]) return@LaunchedEffect
            // Prima della misura vera della riga, con le animazioni spente e nei provini: subito alla misura.
            if (off || pull != null || !ready[0]) {
                height.snapTo(target); ready[0] = headPx > 0; fling[0] = 0f
            } else {
                val land = MasterGestures.land(height.value - target, fling[0])
                fling[0] = 0f
                if (kotlin.math.abs(height.value - target) > 0.5f) freeze(stop)
                height.animateTo(target, spring(dampingRatio = 1f, stiffness = land.stiffness), land.velocity) { grow(value) }
            }
            // Fermo: la conversazione torna alta quanto si vede e, se la si seguiva, l'ultimo messaggio torna sopra il campo.
            base[0] = stop
            if (frozen.floatValue >= 0f) { frozen.floatValue = -1f; onSettledNow() }
        }
        // L'altezza di adesso, letta solo dove si misura e si disegna.
        val px: () -> Float = { (pullNow?.let { p -> aNow.row + (aNow.full - aNow.row) * p.coerceIn(0f, 1f) } ?: height.value).coerceIn(0f, boxNow) }
        val release: (Float) -> Unit = { v ->
            held[0] = false
            val to = MasterSheet.snap(aNow, height.value, v)
            fling[0] = v
            if (to != stopNow) onStopNow(to)
            settled++
        }
        val drag = Modifier.pointerInput(Unit) {
            val tracker = VelocityTracker()
            var from = 0f
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { _ -> tracker.resetTracking(); total = 0f; from = height.value; held[0] = true; freeze(null) },
                onDragEnd = { release(-tracker.calculateVelocity().y) },
                onDragCancel = { release(0f) },
                onVerticalDrag = { change, dy ->
                    total += dy
                    tracker.addPosition(change.uptimeMillis, Offset(0f, total))
                    change.consume()
                    val h = MasterSheet.rubber(from - total, aNow.row, aNow.full, boxNow)
                    scope.launch { height.snapTo(h) }
                    grow(h)
                },
            )
        }
        val rowDp = with(density) { rowPx.toDp() }
        // Prova 1447: il velo, l'arretramento della home e l'ombra sopra il bordo, dall'avanzamento del foglio (MasterSheet.cues).
        val cuesNow: () -> MasterSheet.Cues = { MasterSheet.cues(MasterSheet.progress(aNow, px()), reduced = off) }
        val shadePx = with(density) { MasterSheet.SHADE_DP.dp.toPx() }
        // La home, ferma: in fondo lascia il posto della riga; mentre il foglio sale arretra dal centro in alto.
        Column(
            Modifier.fillMaxSize().padding(bottom = rowDp).graphicsLayer {
                val k = cuesNow(); scaleX = k.scale; scaleY = k.scale; transformOrigin = TransformOrigin(0.5f, 0f)
            },
            content = under,
        )
        // Il velo sulla home (sulla plancia del tablet, sulla colonna sotto il foglio) e l'ombra che sfuma verso il nero sopra il bordo del
        // foglio; non prendono tocchi.
        Box(
            Modifier.fillMaxSize().drawBehind {
                val k = cuesNow()
                if (k.dim > 0f) drawRect(Color.Black.copy(alpha = k.dim))
                val top = size.height - px()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = k.shade), startY = top - shadePx, endY = top),
                    topLeft = Offset(0f, top - shadePx), size = Size(size.width, shadePx),
                )
            },
        )
        val bodyOn by remember { derivedStateOf { pullNow != null || height.value > aNow.row + 0.5f } }
        // app-foglio-master-testata-28-11: all'intera il foglio tocca la barra dell'app e gli angoli in alto si squadrano (MasterSheet.corner).
        // Un valore derivato: chi lo legge nel disegno (la forma, il bordo della master) si ridisegna solo quando l'angolo cambia.
        val cornerPx = with(density) { MasterSheet.CORNER_DP.dp.toPx() }
        val cornerNow by remember(cornerPx) { derivedStateOf { MasterSheet.corner(aNow, px(), cornerPx) } }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .layout { m, c ->
                    val h = px().roundToInt().coerceIn(0, c.maxHeight)
                    val p = m.measure(c.copy(minHeight = h, maxHeight = h))
                    layout(p.width, h) { p.place(0, 0) }
                }
                .graphicsLayer {
                    val r = cornerNow
                    shape = RoundedCornerShape(topStart = r, topEnd = r)
                    clip = true
                }
                .background(MasterHigh)
                // app-bordo-foglio-28-11: l'anello e il raggio della master attorno a tutto il foglio, sopra il suo contenuto.
                .bordoMaster(lavora) { cornerNow }
                .testTag("foglio-master")
                .then(if (sheetTop != null) Modifier.onGloballyPositioned { c -> sheetTop(c.boundsInRoot().top) } else Modifier),
        ) {
            val tapLabel = stringResource(
                when (stop) { MasterSheet.Stop.ROW -> R.string.master_sheet_tap_row; MasterSheet.Stop.HALF -> R.string.master_sheet_tap_half; MasterSheet.Stop.FULL -> R.string.master_sheet_tap_full },
            )
            // La riga tiene sempre la sua altezza, anche col foglio tirato sotto la riga: la sua misura fa la misura «riga».
            Column(
                Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).onSizeChanged { s -> headPx = s.height }.then(drag).handCursor()
                    .clickable(onClickLabel = tapLabel, role = Role.Button) { onStopNow(MasterSheet.tap(stopNow)) }
                    .testTag("foglio-maniglia"),
            ) {
                SheetGrip()
                head()
            }
            // Prova 1447: appesa sotto la riga e opaca, alta quanto si vede o, durante il movimento, ferma; la taglia il bordo del campo.
            if (bodyOn) Column(
                Modifier.weight(1f).fillMaxWidth()
                    .graphicsLayer { clip = true }
                    .layout { m, c ->
                        val f = frozen.floatValue
                        val h = if (f >= 0f) maxOf(c.maxHeight, f.roundToInt()) else c.maxHeight
                        val p = m.measure(Constraints.fixed(c.maxWidth, h))
                        layout(c.maxWidth, c.maxHeight) { p.place(0, 0) }
                    },
                content = body,
            )
        }
    }
}
