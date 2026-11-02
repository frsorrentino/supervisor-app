package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.AltroveRun
import it.pixelbox.cmwatch.contract.RimastoItem
import it.pixelbox.cmwatch.contract.SeguitiPage
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Seguiti

/**
 * «Fuori dal codice» (contratto 1.102, fase B del programma del 01/11): rimasto_indietro, stesso_difetto e promemoria in una pagina. `onAction` è il tocco
 * (`action`, `product`, `key`): allinea e ok su una voce, remind_remove su un promemoria. Quello che non si sa resta detto come non saputo.
 */
@Composable
fun SeguitiScreen(page: SeguitiPage?, failed: Boolean, onBack: () -> Unit, onAction: (String, String?, String) -> Unit, onLavori: () -> Unit, onMaster: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().verticalScroll(rememberScrollState()).testTag("seguiti").padding(start = 12.dp, end = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Cm.text) }
            Text(stringResource(R.string.seguiti_title), style = MaterialTheme.typography.titleLarge, color = Cm.text)
        }
        if (failed) Note(stringResource(R.string.seguiti_read_failed))
        if (page == null) { if (!failed) Note(stringResource(R.string.prove_loading)); return@Column }
        Heading(stringResource(R.string.seguiti_rimasto))
        if (page.rimasto.isEmpty()) Note(stringResource(R.string.seguiti_rimasto_empty))
        Seguiti.ordered(page.rimasto).forEach { RimastoCard(it, onAction) }
        Heading(stringResource(R.string.seguiti_altrove))
        if (page.altrove.isEmpty()) Note(stringResource(R.string.seguiti_altrove_empty))
        page.altrove.forEach { AltroveCard(it, onLavori) }
        Heading(stringResource(R.string.seguiti_promemoria))
        if (page.promemoria.isEmpty()) Note(stringResource(R.string.seguiti_promemoria_empty))
        page.promemoria.forEach { p ->
            Card {
                Text(p.text, style = MaterialTheme.typography.bodyLarge, color = Cm.text)
                Text(when {
                    p.atTime != null -> stringResource(R.string.seguiti_at_time, Seguiti.whenText(p.atTime!!))
                    else -> stringResource(R.string.seguiti_until)
                }, style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
                p.expiresAt?.let { Text(stringResource(R.string.seguiti_expires, Seguiti.whenText(it)), style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
                TextButton(onClick = { onAction("remind_remove", null, p.id) }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.seguiti_remove)) }
            }
        }
        Note(stringResource(R.string.seguiti_add_hint))
        TextButton(onClick = onMaster, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.seguiti_write_master)) }
        if (page.promemoriaScattati.isNotEmpty()) {
            Heading(stringResource(R.string.seguiti_fired_title))
            page.promemoriaScattati.forEach { p ->
                Card {
                    Text(p.text, style = MaterialTheme.typography.bodyLarge, color = Cm.text)
                    p.at?.let { Text(stringResource(if (Seguiti.fired(p)) R.string.seguiti_fired else R.string.seguiti_expired, Seguiti.whenText(it)), style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
                }
            }
        }
    }
}

@Composable private fun Heading(t: String) = Text(t, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Cm.text, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
@Composable private fun Note(t: String) = Text(t, style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(horizontal = 4.dp))
@Composable private fun Card(content: @Composable ColumnScope.() -> Unit) = Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RimastoCard(i: RimastoItem, onAction: (String, String?, String) -> Unit) = Card {
    Text(i.title.ifBlank { i.key }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text)
    Text(listOfNotNull(i.product, i.where?.takeIf { it.isNotBlank() }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
    Text(when (Seguiti.age(i)) {
        Seguiti.Age.ALIGNED -> stringResource(R.string.seguiti_aligned)
        Seguiti.Age.BEHIND -> when {
            Seguiti.changedPaths(i) != null -> stringResource(R.string.seguiti_behind_paths, Seguiti.changedPaths(i)!!)
            Seguiti.why(i) == Seguiti.Why.CHECK -> stringResource(R.string.seguiti_behind_check)
            else -> stringResource(R.string.seguiti_behind)
        }
        Seguiti.Age.UNKNOWN -> stringResource(when (Seguiti.why(i)) {
            Seguiti.Why.NO_GIT -> R.string.seguiti_unknown_no_git
            Seguiti.Why.NO_PROJECT -> R.string.seguiti_unknown_no_project
            Seguiti.Why.CHECK_TIMEOUT -> R.string.seguiti_unknown_timeout
            else -> R.string.seguiti_unknown
        })
    }, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
    Text(if (i.alignedVersion != null || i.alignedAt != null) stringResource(R.string.seguiti_last_aligned, listOfNotNull(i.alignedVersion, i.alignedAt?.let { Seguiti.whenText(it) }).joinToString(", ")) else stringResource(R.string.seguiti_never_aligned), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
    i.task?.let { Text(stringResource(R.string.seguiti_task, it), style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
    if (Seguiti.canAccept(i) || Seguiti.canAlign(i)) FlowRow(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (Seguiti.canAccept(i)) OutlinedButton(onClick = { onAction("ok", i.product, i.key) }) { Text(stringResource(R.string.seguiti_accept)) }
        if (Seguiti.canAlign(i)) FilledTonalButton(onClick = { onAction("allinea", i.product, i.key) }) { Text(stringResource(R.string.seguiti_align)) }
    }
}

@Composable
private fun AltroveCard(r: AltroveRun, onLavori: () -> Unit) = Card {
    Text(r.text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text)
    Text((if (Seguiti.partial(r)) stringResource(R.string.seguiti_partial, r.done, r.total) else stringResource(R.string.seguiti_checked, r.done, r.total)) + (r.at?.let { " · " + Seguiti.whenText(it) } ?: ""), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
    if (r.running) Text(stringResource(R.string.seguiti_running), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
    if (r.positive.isNotEmpty()) { Text(stringResource(R.string.seguiti_found_in), style = MaterialTheme.typography.bodyMedium, color = Cm.text2); r.positive.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text) } }
    if (r.errors.isNotEmpty()) { Text(stringResource(R.string.seguiti_errors), style = MaterialTheme.typography.bodyMedium, color = Cm.text2); r.errors.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text) } }
    if (Seguiti.clean(r)) Text(stringResource(R.string.seguiti_clean), style = MaterialTheme.typography.bodyMedium, color = Cm.text)
    r.task?.let {
        Text(stringResource(R.string.seguiti_card, it), style = MaterialTheme.typography.bodyMedium, color = Cm.text)
        TextButton(onClick = onLavori, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.seguiti_open_lavori)) }
    }
}
