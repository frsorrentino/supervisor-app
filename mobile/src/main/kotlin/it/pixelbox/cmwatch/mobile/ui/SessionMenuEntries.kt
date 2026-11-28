package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SessionMenu
import it.pixelbox.cmwatch.rules.SessionMenu.Kind
import it.pixelbox.cmwatch.rules.TieniPremuto
import it.pixelbox.cmwatch.rules.VociAssenti

/** Cosa fa ogni voce del menu della sessione; null = la voce non si può fare da qui e non compare. */
class SessionMenuActions(
    val changes: (() -> Unit)? = null,
    val changesTitle: String? = null,
    val changesSub: String? = null,
    val prove: (() -> Unit)? = null,
    val decidi: (() -> Unit)? = null,
    val guarda: (() -> Unit)? = null,
    val seguiti: (() -> Unit)? = null,
    val follow: (Boolean) -> Unit,
    val askMaster: (() -> Unit)? = null,
    val openInClaude: (() -> Unit)? = null,
    val terminal: (() -> Unit)? = null,
    val restartClean: (() -> Unit)? = null,
    /** app-tieni-premuto-28-11: quello che fa il tasto rosso da tenere premuto (salva e chiude, o /exit). */
    val exit: (() -> Unit)? = null,
    /** «Chiudi senza salvare», una voce col suo Annulla; null dove non c'è il salvataggio (la master, un relay senza «close»). */
    val exitNoSave: (() -> Unit)? = null,
)

/**
 * Le voci del menu unico della sessione (app-menu-sessione-03-11), nell'ordine di [SessionMenu]: le stesse da home, chat e
 * plancia. app-tieni-premuto-28-11 (Franz 28/11 16:22): «Riparti pulito» e «Chiudi la sessione» non sono più voci ma i tasti da tenere
 * premuto in fondo ([SessionMenuHolds]); al posto di Chiudi resta la voce «Chiudi senza salvare», dove c'è il salvataggio.
 */
@Composable
fun sessionMenuEntries(s: Session, items: List<SessionMenu.Item>, a: SessionMenuActions): List<MenuEntry> {
    val counted: @Composable (Int, Int) -> String = { title, n -> stringResource(R.string.smenu_count, stringResource(title), n) }
    return items.mapNotNull { i ->
        when (i.kind) {
            Kind.CHANGES -> a.changes?.let { MenuEntry(Icons.Outlined.Add, a.changesTitle.orEmpty(), a.changesSub.orEmpty(), onClick = it) }
            Kind.PROVE -> a.prove?.let { MenuEntry(Icons.Outlined.Science, counted(R.string.prove_title, i.n), stringResource(R.string.smenu_prove_sub), onClick = it) }
            Kind.DECIDI -> a.decidi?.let { MenuEntry(Icons.Outlined.Timer, counted(R.string.decisions_title, i.n), stringResource(R.string.smenu_decidi_sub), onClick = it) }
            Kind.GUARDA -> a.guarda?.let { MenuEntry(Icons.Outlined.Visibility, counted(R.string.home_sec_watch, i.n), stringResource(R.string.smenu_guarda_sub), onClick = it) }
            Kind.SEGUITI -> a.seguiti?.let { MenuEntry(Icons.Outlined.Update, counted(R.string.seguiti_title, i.n), stringResource(R.string.smenu_seguiti_sub), onClick = it) }
            Kind.FOLLOW -> MenuEntry(
                if (s.followed) Icons.Outlined.NotificationsOff else Icons.Outlined.NotificationsActive,
                stringResource(if (s.followed) R.string.unfollow else R.string.follow),
                stringResource(if (s.followed) R.string.unfollow_sub else R.string.follow_sub),
            ) { a.follow(!s.followed) }
            Kind.ASK_MASTER -> a.askMaster?.let { MenuEntry(Icons.Outlined.SupervisorAccount, stringResource(R.string.ask_master), stringResource(R.string.ask_master_sub), onClick = it) }
            Kind.OPEN_IN_CLAUDE -> a.openInClaude?.let { MenuEntry(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.open_in_claude), stringResource(R.string.open_in_claude_sub), onClick = it) }
            Kind.TERMINAL -> a.terminal?.let { MenuEntry(Icons.Outlined.Terminal, stringResource(R.string.terminal), stringResource(R.string.terminal_sub), onClick = it) }
            Kind.RESTART_CLEAN -> null
            Kind.EXIT -> a.exitNoSave?.takeIf { VociAssenti.close(s) }?.let { MenuEntry(Icons.AutoMirrored.Outlined.Logout, stringResource(R.string.close_no_save), stringResource(R.string.menu_no_save_sub), onClick = it) }
        }
    }
}

/**
 * app-tieni-premuto-28-11 (Franz 28/11 16:22, mockup tieni-premuto-28-11.html): in fondo al menu della sessione i tasti da tenere premuto, al
 * posto delle voci e della carta «Chiudere X?» di prima. Ripartire 1 s col colore del testo; chiudere 1,2 s in rosso, che salva e chiude
 * dove si può. Con Franz al PC su questa sessione niente chiusura: il motivo, come nella carta ([CloseBlockedNote]). [close] chiude il
 * menu, poi parte l'azione.
 */
@Composable
fun SessionMenuHolds(s: Session, items: List<SessionMenu.Item>, a: SessionMenuActions, close: () -> Unit) {
    val restart = a.restartClean?.takeIf { items.any { i -> i.kind == Kind.RESTART_CLEAN } }
    val exit = a.exit?.takeIf { items.any { i -> i.kind == Kind.EXIT } }
    if (restart == null && exit == null) return
    Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (restart != null) HoldButton(
            stringResource(R.string.hold_restart), onDone = { close(); restart() }, holdMs = TieniPremuto.holdMs(TieniPremuto.Place.RIPARTI),
            tone = HoldTone.INK, icon = Icons.Outlined.RestartAlt, said = stringResource(R.string.hold_said_restart, s.name), tag = "tieni-ripartire",
        )
        if (exit != null) {
            if (VociAssenti.close(s)) HoldButton(
                stringResource(R.string.hold_close), onDone = { close(); exit() }, holdMs = TieniPremuto.holdMs(TieniPremuto.Place.CHIUDI),
                tone = HoldTone.RED, icon = Icons.Outlined.PowerSettingsNew, said = stringResource(R.string.hold_said_close, s.name), tag = "tieni-chiudere",
            )
            else CloseBlockedNote("at_pc")
        }
    }
}
