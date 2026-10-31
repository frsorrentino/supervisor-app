package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.PluginsPage
import it.pixelbox.cmwatch.contract.PluginsSummary
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Plugins
import it.pixelbox.cmwatch.rules.PluginsRun

/**
 * La scheda aperta di «Plugin e skill» (contratto 1.95, mockup approvato da Franz il 29/10 alle 10:39): per account i plugin
 * da aggiornare con «Aggiorna», gli altri e le skill dietro «Mostra»; in fondo «Controlla ora» e «Aggiorna tutto» con la
 * conferma. L'elenco si chiede al relay quando la sezione si apre.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun PluginsCard(
    summary: PluginsSummary, load: suspend () -> PluginsPage?, run: suspend (CmdOp, String?) -> CmdResult?,
    initial: PluginsPage? = null, runner: PluginsRun? = null,
) {
    val scope = rememberCoroutineScope()
    val r = runner ?: remember { PluginsRun(scope, load, run, initial) }
    val st by r.state.collectAsState()
    val page = st.page
    val loading = st.loading
    val busy = st.busy
    val notes = st.notes
    var shown by remember { mutableStateOf(setOf<String>()) }
    var asking by remember { mutableStateOf(false) }
    // All'apertura si rilegge l'elenco e sparisce l'esito di un giro vecchio.
    if (initial == null) LaunchedEffect(Unit) { r.opened() }
    // Il riassunto dello stato arriva con la push dopo: appena c'è l'elenco, contano i plugin dell'elenco.
    val count = Plugins.pending(page, summary)
    val accountsToUpdate = Plugins.accountsToUpdate(page, summary)
    val hm = remember { java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()) }
    Column(Modifier.testTag("plugins_state").fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (page == null) Text(stringResource(if (loading) R.string.plugins_loading else R.string.plugins_none), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(horizontal = 4.dp))
        page?.accounts.orEmpty().forEach { raw ->
            val a = Plugins.account(raw)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.account, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Cm.text, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.plugins_meta, raw.plugins.size, a.skills), style = MonoSmall, color = Cm.text2)
                }
                a.updates.forEach { p ->
                    Line()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, color = Cm.text)
                            Text(buildAnnotatedString { append("${p.installed ?: "?"} → "); withStyle(SpanStyle(color = Cm.briefWarn)) { append(p.latest ?: "") } }, style = MonoSmall, color = Cm.text2)
                        }
                        if (p.scope == "user") Surface(
                            onClick = { r.act(p.id, CmdOp.PLUGIN_UPDATE, Plugins.updateArg(a.account, p), a.account) },
                            shape = CircleShape, color = Cm.surfaceHigh, contentColor = if (p.id in busy) Cm.text2 else Cm.text,
                        ) { Text(stringResource(if (p.id in busy) R.string.plugins_updating else R.string.plugins_update), style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
                    }
                }
                if (a.account in shown) {
                    a.rest.forEach { p ->
                        Line()
                        Column {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, color = Cm.text)
                            Text(
                                buildAnnotatedString {
                                    append(p.installed ?: "?")
                                    when (p.update) { false -> withStyle(SpanStyle(color = Cm.briefGood)) { append(" · " + stringResource(R.string.plugins_latest)) }; null -> append(" · " + stringResource(R.string.plugins_unknown)); else -> {} }
                                },
                                style = MonoSmall, color = Cm.text2,
                            )
                        }
                    }
                    if (a.skillNames.isNotEmpty()) Text(stringResource(R.string.plugins_skills) + ": " + a.skillNames.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Cm.text2)
                }
                if (a.rest.isNotEmpty() || a.skills > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (a.account in shown) "" else when { a.skills == 0 -> stringResource(R.string.plugins_others_p, a.rest.size); a.rest.isEmpty() -> stringResource(R.string.plugins_others_s, a.skills); else -> stringResource(R.string.plugins_others, a.rest.size, a.skills) }, style = MonoSmall, color = Cm.text2, modifier = Modifier.weight(1f))
                    TextButton(onClick = { shown = if (a.account in shown) shown - a.account else shown + a.account }) {
                        Text(stringResource(if (a.account in shown) R.string.plugins_hide else R.string.plugins_show), color = Cm.actionIcon)
                    }
                }
                notes[a.account]?.let { n -> Note(n.text?.let { t -> t + if (n.ok) " · " + stringResource(R.string.plugins_new_sessions) else "" } ?: stringResource(R.string.plugins_no_answer), n.ok) }
            }
        }
        if (st.all) Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp).testTag("plugins_updating_all"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), color = Cm.actionIcon, strokeWidth = 2.dp)
            Text(stringResource(R.string.plugins_updating_all), style = MaterialTheme.typography.bodyMedium, color = Cm.text)
        } else if (asking) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(pluralString(R.plurals.plugins_confirm, count), style = MaterialTheme.typography.bodySmall, color = Cm.text2, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { asking = false }) { Text(stringResource(R.string.plugins_keep), color = Cm.actionIcon) }
            Button(
                onClick = { asking = false; r.updateAll(accountsToUpdate) },
                colors = ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary),
            ) { Text(stringResource(R.string.plugins_update_all, count)) }
        } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            val checked = page?.checkedAt ?: summary.checkedAt
            Text(checked?.let { stringResource(R.string.plugins_checked, hm.format(java.time.Instant.ofEpochSecond(it))) } ?: stringResource(R.string.plugins_never_checked), style = MonoSmall, color = Cm.text2)
            TextButton(onClick = { r.act("check", CmdOp.PLUGIN_CHECK, null, "_check") }) { Text(stringResource(R.string.plugins_check), color = Cm.actionIcon) }
            if (count > 0) Button(onClick = { asking = true }, colors = ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary)) { Text(stringResource(R.string.plugins_update_all, count)) }
        }
        notes["_check"]?.let { n -> Note(n.text ?: stringResource(R.string.plugins_no_answer), n.ok) }
    }
}

@Composable
private fun pluralString(id: Int, n: Int): String = androidx.compose.ui.res.pluralStringResource(id, n, n)

@Composable
private fun Line() = Box(Modifier.fillMaxWidth().height(1.dp).background(Cm.line))

@Composable
private fun Note(text: String, ok: Boolean) = Text(
    text, style = MaterialTheme.typography.bodySmall, color = Cm.text,
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background((if (ok) Cm.briefGood else Cm.briefWarn).copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 8.dp),
)
