package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SessionMenu
import it.pixelbox.cmwatch.rules.SessionMenu.Kind

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
    val exit: (() -> Unit)? = null,
)

/**
 * Le voci del menu unico della sessione (app-menu-sessione-03-11), nell'ordine di [SessionMenu]: le stesse da home, chat e
 * plancia. Restituisce le voci e, a parte, Chiudi la sessione, che va in fondo in rosso.
 */
@Composable
fun sessionMenuEntries(s: Session, items: List<SessionMenu.Item>, a: SessionMenuActions): Pair<List<MenuEntry>, MenuEntry?> {
    val counted: @Composable (Int, Int) -> String = { title, n -> stringResource(R.string.smenu_count, stringResource(title), n) }
    var footer: MenuEntry? = null
    val entries = items.mapNotNull { i ->
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
            Kind.RESTART_CLEAN -> a.restartClean?.let { MenuEntry(Icons.Outlined.RestartAlt, stringResource(R.string.smenu_restart), stringResource(R.string.smenu_restart_sub), onClick = it) }
            Kind.EXIT -> { footer = a.exit?.let { MenuEntry(Icons.Outlined.PowerSettingsNew, stringResource(R.string.menu_exit), stringResource(R.string.menu_exit_sub), destructive = true, onClick = it) }; null }
        }
    }
    return entries to footer
}
