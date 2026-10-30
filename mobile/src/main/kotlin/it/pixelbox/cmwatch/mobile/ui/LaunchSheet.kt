package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Project
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.LaunchSuggest
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.CreateNewFolder

/**
 * Il contenuto del foglio «Lancia» (design 29/09, schermata 4), usato anche per «Aggiungi alla notte» (`action`). Sotto
 * il campo una lista (segnalazione 01/10 23:20, prima era una tendina): a campo vuoto i recenti, scrivendo le sessioni
 * con quel nome e poi i progetti di tutti e due gli account in ordine `LaunchSuggest.ranked`, la parte trovata in
 * grassetto; gli account in testa sono un filtro facoltativo.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LaunchSheet(
    state: State, action: Int = R.string.launch,
    /** Una sessione trovata per nome: aperta = la sua scheda, chiusa = «Riapri». Null = solo progetti (la notte). */
    onSession: ((name: String, reopen: Boolean) -> Unit)? = null,
    /** Contratto 1.26: tutti i progetti chiesti al PC all'apertura; finché non arrivano, quelli di /state. */
    projects: List<Project> = state.projects,
    /** Sotto il titolo, prima dei progetti: nella notte le schede del Recap da spuntare (contratto 1.49). */
    top: (@Composable ColumnScope.() -> Unit)? = null,
    /**
     * Contratto 1.64, «Nuovo progetto»: la cartella madre, il nome, il primo messaggio e la conferma; torna il risultato del
     * PC, null senza risposta. Null = il relay non lo sa fare (nessuna `project_parents`).
     */
    onNewProject: (suspend (parent: String, name: String, first: String, confirm: Boolean) -> it.pixelbox.cmwatch.contract.CmdResult?)? = null,
    onLaunch: (project: Project, firstMessage: String) -> Unit,
) {
    val parents = state.projectParents.orEmpty()
    var making by rememberSaveable { mutableStateOf(false) }
    var parent by rememberSaveable { mutableStateOf(parents.firstOrNull()) }
    var confirmPath by rememberSaveable { mutableStateOf<String?>(null) }
    var refusal by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val accounts = remember(state, projects) {
        (state.quota.keys + projects.map { it.account }).distinct()
            .sortedWith(compareBy<String> { a -> !(state.quota[a]?.let { Accounts.isPersonalQuota(a, it) } ?: Accounts.personal(a, null)) }.thenBy { it })
    }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var typed by rememberSaveable { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Project?>(null) }
    var first by rememberSaveable { mutableStateOf("") }
    // A campo vuoto i recenti; scrivendo, prima le sessioni con quel nome e poi i progetti (segnalazione 01/10 23:20).
    val found = if (typed.isBlank()) LaunchSuggest.recent(state, filter, pool = projects) else LaunchSuggest.ranked(state, typed, filter, pool = projects)
    val sessions = if (onSession == null || chosen != null) emptyList() else LaunchSuggest.sessions(state, typed)
    Column(
        Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(action), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = Cm.text)
        top?.invoke(this)
        if (accounts.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { a ->
                val personal = state.quota[a]?.let { Accounts.isPersonalQuota(a, it) } ?: Accounts.personal(a, null)
                FilterChip(
                    selected = filter == a, onClick = { filter = if (filter == a) null else a; chosen = null },
                    label = { Text(a) }, leadingIcon = { AccountMark(personal, size = 12.dp) },
                )
            }
        }
        // Una lista sotto il campo, non una tendina: dentro il foglio, con la tastiera aperta, la tendina finiva fuori
        // schermo e sembrava che la ricerca non trovasse nulla.
        OutlinedTextField(
            typed, { typed = it; chosen = null; making = false; confirmPath = null }, label = { Text(stringResource(R.string.launch_project)) }, singleLine = true,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
        )
        // Contratto 1.64: un nome che non è un progetto diventa un progetto nuovo, in una delle cartelle madri del PC.
        val newName = typed.trim()
        if (onNewProject != null && parents.isNotEmpty() && chosen == null && !making && it.pixelbox.cmwatch.rules.NewProject.validName(newName) &&
            projects.none { p -> p.name.equals(newName, ignoreCase = true) }
        ) Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).handCursor().clickable { making = true; confirmPath = null; refusal = null }.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.CreateNewFolder, null, tint = Cm.actionIcon)
            Text(stringResource(R.string.project_new, newName), style = MaterialTheme.typography.bodyLarge, color = Cm.actionIcon, modifier = Modifier.weight(1f))
        }
        if (making) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.project_new_parent), style = MaterialTheme.typography.labelLarge, color = Cm.text2)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                parents.forEach { p ->
                    FilterChip(selected = parent == p, onClick = { parent = p; confirmPath = null }, label = { Text(p.trimEnd('/').substringAfterLast('/').ifEmpty { p }) })
                }
            }
            confirmPath?.let { Text(stringResource(R.string.project_new_confirm, it), style = MaterialTheme.typography.bodyMedium, color = Cm.text) }
            refusal?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.gone) }
        }
        if (chosen == null && !making) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (typed.isBlank() && found.isNotEmpty()) Text(stringResource(R.string.launch_recent), style = MaterialTheme.typography.labelLarge, color = Cm.text2)
            sessions.forEach { s ->
                val closed = s.state == it.pixelbox.cmwatch.contract.SessionState.GONE
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).handCursor().clickable { onSession?.invoke(s.name, closed) }.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SessionBadge(s, size = 18.dp)
                    Text(highlight(s.name, typed), style = MaterialTheme.typography.bodyLarge, color = Cm.text, modifier = Modifier.weight(1f))
                    Text(stringResource(if (closed) R.string.reopen else R.string.launch_open), style = MaterialTheme.typography.labelLarge, color = Cm.actionIcon)
                }
            }
            found.forEach { p ->
                val personal = state.quota[p.account]?.let { Accounts.isPersonalQuota(p.account, it) } ?: Accounts.personal(p.account, null)
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).handCursor().clickable { chosen = p; typed = p.name }.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AccountMark(personal, size = 14.dp)
                    Column {
                        Text(highlight(p.name, typed), style = MaterialTheme.typography.bodyLarge, color = Cm.text)
                        Text(p.path.substringBeforeLast('/').substringAfterLast('/'), style = MaterialTheme.typography.bodySmall, color = Cm.text2)
                    }
                }
            }
        }
        OutlinedTextField(first, { first = it }, label = { Text(stringResource(R.string.launch_first)) }, minLines = 3, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        if (making) Button(
            // Primo tocco: il PC dice il percorso e non crea niente; secondo tocco, con la conferma, crea e lancia.
            onClick = {
                val p = parent ?: return@Button
                val create = onNewProject ?: return@Button
                busy = true; refusal = null
                scope.launch {
                    val r = create(p, newName, first.trim(), confirmPath != null)
                    busy = false
                    val path = r?.let { res -> it.pixelbox.cmwatch.rules.NewProject.confirmPath(res.text) }
                    when {
                        r == null -> refusal = null
                        path != null -> confirmPath = path
                        !r.ok -> { refusal = r.text; confirmPath = null }
                    }
                }
            },
            enabled = parent != null && !busy,
            colors = ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(if (confirmPath == null) R.string.project_new_go else R.string.project_new_create, newName)) }
        else Button(
            onClick = { chosen?.let { onLaunch(it, first.trim()) } }, enabled = chosen != null,
            colors = ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(action)) }
    }
}

/** Il nome con la parte che corrisponde al testo scritto in grassetto (maiuscole e spazi ignorati, come la ricerca). */
private fun highlight(name: String, typed: String): androidx.compose.ui.text.AnnotatedString {
    val t = typed.trim()
    val i = if (t.isEmpty()) -1 else name.indexOf(t, ignoreCase = true)
    return androidx.compose.ui.text.buildAnnotatedString {
        append(name)
        if (i >= 0) addStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Cm.actionIcon), i, i + t.length)
    }
}
