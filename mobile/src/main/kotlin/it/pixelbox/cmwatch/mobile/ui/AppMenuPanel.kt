package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.platform.testTag
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Una voce dei menu a pannello: icona su fondo tonale, titolo e una riga che spiega cosa fa. */
data class MenuEntry(
    val icon: ImageVector, val title: String, val sub: String, val accent: Boolean = false,
    /** Un'azione che chiude o cancella: in fondo, separata, in rosso (osservazioni del 03/10). */
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Il menu a pannello (design 03/10, tavola 5), condiviso dal menu dell'app e da quello della sessione: un pannello largo
 * sotto la barra sopra lo sfondo oscurato; in testa una riga con ✕; voci grandi; in fondo, separata, una voce di servizio.
 * Tocco fuori, ✕ o Indietro lo chiudono; ogni voce lo chiude prima di agire.
 */
@Composable
fun MenuPanel(
    onDismiss: () -> Unit, entries: List<MenuEntry>, footer: MenuEntry? = null, tag: String? = null,
    origin: androidx.compose.ui.graphics.TransformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f),
    header: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = PanelShell(onDismiss, header, origin, tag) { close ->
    entries.forEachIndexed { i, e -> MenuItem(e.icon, e.title, e.sub, e.accent, i > 0) { close(); e.onClick() } }
    footer?.let { f -> MenuItem(f.icon, f.title, f.sub, false, true, f.destructive) { close(); f.onClick() } }
}

/**
 * Il guscio dei pannelli: sfondo oscurato a tutto schermo (tocco = chiudi), pannello largo sotto la barra con angoli da
 * 28 dp, in testa una riga con ✕; il contenuto scorre se è più alto dello schermo. Indietro chiude.
 *
 * Il pannello nasce dal punto toccato ([origin]: ≡ in alto a destra, il nome della sessione in alto a sinistra) e ci torna
 * (osservazioni del 03/10, transizione 1): sfondo in dissolvenza in 150 ms, pannello da 0,96 a 1 con dissolvenza in 200 ms
 * con la curva dell'app, uscita per la stessa strada in 150 ms; [onDismiss] arriva a uscita finita. Con le animazioni
 * spente compare e sparisce subito. Il contenuto riceve `close`, che chiude con l'uscita animata.
 */
@Composable
fun PanelShell(
    onDismiss: () -> Unit, header: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    origin: androidx.compose.ui.graphics.TransformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f), tag: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.(close: () -> Unit) -> Unit,
) {
    val off = animationsOff()
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    // L'apertura si chiede una volta sola: impostata a ogni ricomposizione rimetteva `true` subito dopo la chiusura e il
    // pannello non si chiudeva più (segnalazione del 03/10 20:24).
    val shown = remember { androidx.compose.animation.core.MutableTransitionState(off).apply { targetState = true } }
    var closing by remember { androidx.compose.runtime.mutableStateOf(false) }
    val close: () -> Unit = { if (off) onDismiss() else { closing = true; shown.targetState = false } }
    androidx.compose.runtime.LaunchedEffect(shown.isIdle, shown.currentState, closing) { if (closing && shown.isIdle && !shown.currentState) onDismiss() }
    val enter = androidx.compose.animation.core.tween<Float>(200, easing = CmMotion.easing)
    val exit = androidx.compose.animation.core.tween<Float>(150, easing = CmMotion.easing)
    // Compose misura il popup una volta sola, sulla parte visibile della finestra: aperto con la tastiera su (scrivendo alla
    // master) prendeva l'altezza sopra la tastiera, che il popup stesso chiude, e il menu si fermava a metà e scorreva con
    // lo spazio vuoto sotto (Franz, 12/10 22:11). Prima si chiude la tastiera, poi si apre il pannello; se la tastiera non
    // si chiude, il pannello si apre lo stesso dopo mezzo secondo.
    val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    var ready by remember { androidx.compose.runtime.mutableStateOf(imeBottom == 0) }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    androidx.compose.runtime.LaunchedEffect(Unit) { if (!ready) { keyboard?.hide(); kotlinx.coroutines.delay(500); ready = true } }
    androidx.compose.runtime.LaunchedEffect(imeBottom) { if (imeBottom == 0) ready = true }
    if (!ready) return
    Popup(onDismissRequest = close, properties = PopupProperties(focusable = true)) {
        Box(Modifier.fillMaxSize()) {
            androidx.compose.animation.AnimatedVisibility(
                shown, enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)),
            ) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                        .clickable(remember { MutableInteractionSource() }, indication = null, onClick = close),
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                shown,
                enter = androidx.compose.animation.fadeIn(enter) + androidx.compose.animation.scaleIn(enter, initialScale = 0.96f, transformOrigin = origin),
                exit = androidx.compose.animation.fadeOut(exit) + androidx.compose.animation.scaleOut(exit, targetScale = 0.96f, transformOrigin = origin),
                // Su tablet e desktop un menu largo 380 dp dalla parte del tasto che lo apre, non a tutta larghezza (Franz, 05/10 13:10).
                modifier = if (wide) Modifier.align(if (origin.pivotFractionX > 0.5f) Alignment.TopEnd else Alignment.TopStart)
                    .statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 56.dp, bottom = 24.dp).width(380.dp)
                else Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 56.dp, bottom = 24.dp).fillMaxWidth(),
            ) {
                Surface(
                    shape = V3.Shape, color = V3.S1, shadowElevation = 8.dp, border = androidx.compose.foundation.BorderStroke(1.dp, V3.Line),
                    // Il tocco dentro il pannello non lo chiude.
                    modifier = Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier).clickable(remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    V3Theme { Column {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            header()
                            IconButton(onClick = close) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = CmColors.text2) }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
                        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) { content(close) }
                    } }
                }
            }
        }
    }
}

/** Il menu dell'app: in testa lo stato del collegamento; Lancia, Lavori, Notte, Quadro, Cerca; in fondo Impostazioni. */
@Composable
fun AppMenuPanel(
    host: String?, updated: String, stale: Boolean,
    onLaunch: () -> Unit, onLavori: () -> Unit, onQuadro: () -> Unit, onSearch: () -> Unit, onSettings: () -> Unit,
    onDismiss: () -> Unit,
    /** La pagina Notte (specifica del 07/10); null = la voce non c'è. */
    onNight: (() -> Unit)? = null,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val live by it.pixelbox.cmwatch.mobile.live.LiveService.running.collectAsState()
    MenuPanel(
        onDismiss,
        entries = listOf(
            MenuEntry(Icons.Outlined.RocketLaunch, stringResource(R.string.menu_launch), stringResource(R.string.menu_launch_sub), accent = true, onClick = onLaunch),
            // La pagina unica «Lavori» (22/10): al posto di Registro e Recap.
            MenuEntry(Icons.Outlined.Checklist, stringResource(R.string.menu_lavori), stringResource(R.string.menu_lavori_sub), onClick = onLavori),
        ) + listOfNotNull(
            onNight?.let { n -> MenuEntry(Icons.Outlined.Bedtime, stringResource(R.string.night_menu), stringResource(R.string.night_menu_sub), onClick = n) },
        ) + listOf(
            MenuEntry(Icons.Outlined.DataUsage, stringResource(R.string.menu_quadro), stringResource(R.string.menu_quadro_sub), onClick = onQuadro),
            MenuEntry(Icons.Outlined.Search, stringResource(R.string.menu_search), stringResource(R.string.menu_search_sub), onClick = onSearch),
            // Modalità live (specifica 06/10): notiziario in cuffia, comandi dal watch; lo stesso tocco la spegne.
            MenuEntry(
                Icons.Outlined.Headphones, stringResource(R.string.live_menu),
                stringResource(if (live) R.string.live_menu_on else R.string.live_menu_sub),
                onClick = { it.pixelbox.cmwatch.mobile.live.LiveService.toggle(ctx); onDismiss() },
            ),
        ),
        footer = MenuEntry(Icons.Outlined.Settings, stringResource(R.string.settings), "", onClick = onSettings), tag = "menu-v3",
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (stale) V3.Wait else V3.Good))
        Text(
            stringResource(R.string.menu_connected, host ?: stringResource(R.string.menu_pc), updated), style = V3.Small,
            color = V3.Tx2, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f),
        )
    }
}

/** Una voce nella forma della v3: icona grigia a contorno nel binario, senza cerchio; la voce d'azione è l'unico pieno, bianco. */
@Composable
private fun MenuItem(icon: ImageVector, title: String, sub: String, accent: Boolean, line: Boolean, destructive: Boolean = false, onClick: () -> Unit) {
    if (accent) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth().heightIn(min = 60.dp).clip(V3.Shape).background(V3.Act).handCursor().clickable(onClick = onClick).padding(top = 9.dp, bottom = 9.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = V3.OnAct, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, style = V3.Name.copy(color = V3.OnAct))
                if (sub.isNotEmpty()) Text(sub, style = V3.Small.copy(color = Color(0xFF41454C)))
            }
        }
        return
    }
    val tone = if (destructive) V3.Red else V3.Tx2
    V3Row(!line, onClick, { Icon(icon, null, tint = tone, modifier = Modifier.size(22.dp)) }) {
        Text(title, style = V3.Name.copy(color = if (destructive) V3.Red else V3.Tx))
        if (sub.isNotEmpty()) Text(sub, style = V3.Small)
    }
}
