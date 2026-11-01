package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.BancoProva
import it.pixelbox.cmwatch.rules.CinqueMinuti
import it.pixelbox.cmwatch.rules.HeadsUpNote
import it.pixelbox.cmwatch.rules.OkInformato

@Composable
private fun DecideBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Cm.text) }
        Text(title, style = MaterialTheme.typography.titleLarge, color = Cm.text)
    }
}

@Composable
private fun DecideCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/**
 * «Da provare» (contratto 1.99, banco_prova): le prove per versione. Quelle di una versione non ancora installata dicono di installarla prima e non
 * hanno i tasti. «Non va» chiede cosa non va, a voce dalla tastiera, e uno screenshot facoltativo: arrivano alla master.
 */
@Composable
fun DaProvareScreen(page: ProvePage?, buildOf: (String) -> BancoProva.Build, onBack: () -> Unit, onOk: (String) -> Unit, onKo: (ProveItem, String, android.net.Uri?) -> Unit, onScreen: (String) -> Unit) {
    var ko by remember { mutableStateOf<ProveItem?>(null) }
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().verticalScroll(rememberScrollState()).testTag("da-provare").padding(start = 12.dp, end = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DecideBar(stringResource(R.string.prove_title), onBack)
        if (page == null) Text(stringResource(R.string.prove_loading), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(4.dp))
        else if (page.groups.isEmpty() && page.ko.isEmpty() && page.red.isEmpty()) Text(stringResource(R.string.prove_none), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(4.dp))
        page?.groups?.forEach { g ->
            val b = buildOf(g.build)
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(g.build, style = MonoSmall, modifier = Modifier.weight(1f))
                if (b != BancoProva.Build.TO_INSTALL && g.prove.size > 1) FilledTonalButton(onClick = { onOk(BancoProva.allOk(g)) }) { Text(stringResource(R.string.prove_all_ok)) }
            }
            if (b == BancoProva.Build.TO_INSTALL) Text(stringResource(R.string.prove_install_first), style = MaterialTheme.typography.bodyMedium, color = Cm.waiting, modifier = Modifier.padding(horizontal = 4.dp))
            if (b == BancoProva.Build.UNKNOWN) Text(stringResource(R.string.prove_build_unknown), style = MaterialTheme.typography.bodySmall, color = Cm.text2, modifier = Modifier.padding(horizontal = 4.dp))
            g.prove.forEach { p ->
                DecideCard {
                    Text(p.short ?: p.title ?: p.task, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text)
                    Text(p.fai, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                    Text(stringResource(R.string.prove_expected, p.atteso), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BancoProva.screen(p.dove)?.let { s -> TextButton(onClick = { onScreen(s) }) { Text(stringResource(R.string.features_go)) } }
                        Spacer(Modifier.weight(1f))
                        if (b != BancoProva.Build.TO_INSTALL) {
                            OutlinedButton(onClick = { ko = p }) { Text(stringResource(R.string.prove_ko)) }
                            FilledTonalButton(onClick = { onOk(p.id.toString()) }) { Text(stringResource(R.string.prove_ok)) }
                        }
                    }
                }
            }
        }
        if (!page?.ko.isNullOrEmpty()) {
            Text(stringResource(R.string.prove_reported), style = MonoSmall, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            page!!.ko.forEach { k -> DecideCard { Text(k.short ?: k.task, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text); Text(k.text, style = MaterialTheme.typography.bodyMedium, color = Cm.text2) } }
        }
        if (!page?.red.isNullOrEmpty()) {
            Text(stringResource(R.string.prove_red), style = MonoSmall, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            page!!.red.forEach { r -> DecideCard { Text(r.short ?: r.task, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text); Text(r.reason ?: stringResource(R.string.prove_red_running), style = MaterialTheme.typography.bodyMedium, color = Cm.gone) } }
        }
    }
    ko?.let { p ->
        var text by rememberSaveable(p.id) { mutableStateOf("") }
        var shot by remember(p.id) { mutableStateOf<android.net.Uri?>(null) }
        val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { u -> if (u != null) shot = u }
        AlertDialog(
            onDismissRequest = { ko = null }, containerColor = Cm.surface,
            title = { Text(p.short ?: p.task, color = Cm.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(text, { text = it.take(2000) }, label = { Text(stringResource(R.string.prove_ko_hint)) }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { pick.launch("image/*") }) { Text(stringResource(if (shot == null) R.string.prove_ko_shot else R.string.prove_ko_shot_ok)) }
                }
            },
            confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onKo(p, text.trim(), shot); ko = null }) { Text(stringResource(R.string.prove_ko_send)) } },
            dismissButton = { TextButton(onClick = { ko = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** «Cinque minuti» (contratto 1.99, cinque_minuti): Franz sceglie quante, poi una alla volta, le più pesanti per prime. Niente timer. */
@Composable
fun CinqueMinutiScreen(page: DecisionsPage?, onBack: () -> Unit, onApprove: (String, String?) -> Unit, onDone: (DecisionItem) -> Unit, onNotNow: (String) -> Unit, onDrop: (String, String) -> Unit, startHowMany: Int? = null) {
    var howMany by rememberSaveable { mutableStateOf(startHowMany) }
    var touched by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var dropping by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().verticalScroll(rememberScrollState()).testTag("cinque-minuti").padding(start = 12.dp, end = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DecideBar(stringResource(R.string.decisions_title), onBack)
        val many = howMany
        when {
            page == null -> Text(stringResource(R.string.prove_loading), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(4.dp))
            page.groups.isEmpty() -> Text(stringResource(R.string.decisions_none), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(4.dp))
            many == null -> DecideCard {
                Text(stringResource(R.string.decisions_head, page.n, page.here, page.tasks), style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                Text(stringResource(R.string.decisions_how_many), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 5).filter { it <= page.groups.size || it == 1 }.forEach { n -> FilledTonalButton(onClick = { howMany = n }) { Text(n.toString()) } }
                    FilledTonalButton(onClick = { howMany = page.groups.size }) { Text(stringResource(R.string.decisions_all)) }
                }
            }
            else -> {
                val left = CinqueMinuti.next(page, touched.toSet(), (many - touched.size).coerceAtLeast(0))
                val g = left.firstOrNull()
                if (g == null) Text(stringResource(R.string.decisions_done), style = MaterialTheme.typography.bodyLarge, color = Cm.text, modifier = Modifier.padding(4.dp))
                else {
                    fun next() { touched = touched + g.key }
                    Text(stringResource(R.string.decisions_position, touched.size + 1, minOf(many, page.groups.size)), style = MonoSmall, modifier = Modifier.padding(start = 4.dp))
                    DecideCard {
                        Text(g.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text)
                        Text(stringResource(if (g.where == "pc") R.string.decisions_at_pc else R.string.decisions_here) + if (g.weight > 1) " · " + stringResource(R.string.decisions_unblocks, g.weight) else "", style = MonoSmall, color = if (g.where == "pc") Cm.waiting else Cm.text2)
                        g.note?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
                        g.items.forEach { i ->
                            HorizontalDivider(color = Cm.line)
                            Text(i.short ?: i.title ?: i.session ?: i.task.orEmpty(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Cm.text)
                            (i.reason ?: i.what)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
                            when (CinqueMinuti.act(i)) {
                                CinqueMinuti.Act.APPROVE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val opts = i.options.orEmpty()
                                    if (opts.isEmpty()) FilledTonalButton(onClick = { onApprove(i.task!!, null); next() }) { Text(stringResource(R.string.approve)) }
                                    else opts.forEach { o -> FilledTonalButton(onClick = { onApprove(i.task!!, o); next() }) { Text(o) } }
                                }
                                CinqueMinuti.Act.DONE -> FilledTonalButton(onClick = { onDone(i); next() }) { Text(stringResource(R.string.agenda_done)) }
                                CinqueMinuti.Act.NONE -> Unit
                            }
                        }
                        HorizontalDivider(color = Cm.line)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onNotNow(g.key); next() }) { Text(stringResource(R.string.decisions_not_now)) }
                            TextButton(onClick = { dropping = true }) { Text(stringResource(R.string.decisions_drop), color = Cm.gone) }
                        }
                    }
                    if (dropping) {
                        var reason by rememberSaveable { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { dropping = false }, containerColor = Cm.surface,
                            title = { Text(stringResource(R.string.decisions_drop_title), color = Cm.text) },
                            text = { OutlinedTextField(reason, { reason = it }, label = { Text(stringResource(R.string.decisions_drop_reason)) }, modifier = Modifier.fillMaxWidth()) },
                            confirmButton = { TextButton(enabled = CinqueMinuti.canDrop(reason), onClick = { onDrop(g.key, reason.trim()); dropping = false; reason = ""; next() }) { Text(stringResource(R.string.decisions_drop), color = Cm.gone) } },
                            dismissButton = { TextButton(onClick = { dropping = false }) { Text(stringResource(R.string.cancel)) } },
                        )
                    }
                }
            }
        }
    }
}

/** Le tre righe dell'ok informato (contratto 1.99): quello che manca dice «non dichiarato», non si inventa. Poi i compiti che partono con questo ok. */
@Composable
internal fun OkInfo(a: Approval) {
    val l = OkInformato.lines(a)
    val none = stringResource(R.string.ok_undeclared)
    val undo = when (l.undo) {
        OkInformato.Undo.YES -> stringResource(R.string.ok_undo_yes)
        OkInformato.Undo.NO -> stringResource(R.string.ok_undo_no)
        OkInformato.Undo.PARTLY -> stringResource(R.string.ok_undo_partly)
        OkInformato.Undo.UNDECLARED -> none
    } + (l.undoHow?.let { " · $it" } ?: "")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.testTag("ok-informato")) {
        @Composable fun row(k: String, v: String, muted: Boolean) = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(k, style = MaterialTheme.typography.bodySmall, color = Cm.text2, modifier = Modifier.width(92.dp))
            Text(v, style = MaterialTheme.typography.bodySmall, color = if (muted) Cm.text2 else Cm.text)
        }
        row(stringResource(R.string.ok_untested), l.untested ?: none, l.untested == null)
        row(stringResource(R.string.ok_undo), undo, l.undo == OkInformato.Undo.UNDECLARED)
        l.precedent?.let { p -> row(stringResource(R.string.ok_precedent), stringResource(if (p.relation == "fixes") R.string.ok_precedent_fixes else R.string.ok_precedent_defect, p.short ?: p.task), false) }
        a.also?.takeIf { it.isNotEmpty() }?.let { row(stringResource(R.string.ok_also), it.joinToString(", ") { x -> x.short ?: x.task }, false) }
    }
}

/** La nota di Claude Code (contratto 1.99, heads_up), nello stile di «Non provato»: nessun tasto mandato al terminale. */
class HeadsUpActions(val dismissed: Set<String>, val dismiss: (String) -> Unit, val propose: (String, String) -> Unit)
val LocalHeadsUp = androidx.compose.runtime.staticCompositionLocalOf<HeadsUpActions?> { null }

@Composable
internal fun HeadsUpNoteRow(s: Session, now: Long) {
    val a = LocalHeadsUp.current ?: return
    val n = HeadsUpNote.visible(s, a.dismissed) ?: return
    val prompt = stringResource(R.string.heads_up_prompt, n.text)
    Row(Modifier.fillMaxWidth().testTag("heads-up"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Info, null, tint = V3.Tx3, modifier = Modifier.padding(top = 3.dp).size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(n.text, style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
            Text(stringResource(R.string.queue_age, Durations.since(n.seenAt, now)), style = MonoSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { a.propose(s.name, prompt) }) { Text(stringResource(R.string.heads_up_propose)) }
                TextButton(onClick = { a.dismiss(HeadsUpNote.key(s.name, n.text)) }) { Text(stringResource(R.string.heads_up_known), color = Cm.text2) }
            }
        }
    }
}
