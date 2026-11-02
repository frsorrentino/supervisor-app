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
import it.pixelbox.cmwatch.rules.PrimaRaccolta

@Composable
private fun DecideBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Cm.text) }
        Text(title, style = MaterialTheme.typography.titleLarge, color = Cm.text)
    }
}

/** Una pagina intera col corpo di una voce: da «Cosa sa fare», e all'abbinamento per la prima raccolta. In home lo stesso corpo si apre sul posto. */
@Composable
private fun DecidePage(title: String, tag: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding().verticalScroll(rememberScrollState()).testTag(tag).padding(start = 12.dp, end = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DecideBar(title, onBack)
        content()
    }
}

@Composable
private fun DecideCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cm.surfaceLow).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/**
 * «Prima raccolta» (contratto 1.100, funzione 10 del programma del 01/11): cosa si è trovato per ogni sezione, dati veri; una sezione vuota dice perché
 * e quando si riempie. Il consenso si chiede una volta, il primo giro della master dice il costo prima e parte con un tocco.
 */
@Composable
fun PrimaRaccoltaScreen(page: PrimaRaccoltaPage?, onBack: () -> Unit, onTouch: (String, String?) -> Unit, onScreen: (String) -> Unit, onFeatures: () -> Unit) = DecidePage(stringResource(R.string.prima_title), "prima-raccolta", onBack) { PrimaRaccoltaBody(page, onTouch, onScreen, onFeatures) }

@Composable
fun PrimaRaccoltaBody(page: PrimaRaccoltaPage?, onTouch: (String, String?) -> Unit, onScreen: (String) -> Unit, onFeatures: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (page == null) { Text(stringResource(R.string.prove_loading), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(4.dp)); return@Column }
        Text(
            when (page.status) {
                PrimaRaccolta.RUNNING -> stringResource(R.string.prima_running, page.done, page.total)
                PrimaRaccolta.ASK -> stringResource(R.string.prima_ask, page.done, page.total)
                PrimaRaccolta.INTERRUPTED -> stringResource(R.string.prima_interrupted, page.done, page.total)
                PrimaRaccolta.DONE -> stringResource(R.string.prima_done)
                else -> stringResource(R.string.prima_none)
            },
            style = MaterialTheme.typography.bodyLarge, color = Cm.text, modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (page.cost == 0) Text(stringResource(R.string.prima_free), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(horizontal = 4.dp))
        if (PrimaRaccolta.canStart(page)) FilledTonalButton(onClick = { onTouch("start", null) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (page.status == PrimaRaccolta.INTERRUPTED) R.string.prima_resume else R.string.prima_start))
        }
        if (PrimaRaccolta.askConsent(page)) DecideCard {
            Text(page.askText.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Cm.text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                OutlinedButton(onClick = { onTouch("consent", "no") }) { Text(stringResource(R.string.prima_no)) }
                FilledTonalButton(onClick = { onTouch("consent", "si") }) { Text(stringResource(R.string.prima_yes)) }
            }
        }
        page.sections.forEach { s ->
            DecideCard {
                Text(primaSection(s.key), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text)
                if (PrimaRaccolta.empty(s)) {
                    s.why?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
                    s.whenFills?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
                } else Text(s.text.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                BancoProva.screen(s.dove)?.let { d -> TextButton(onClick = { onScreen(d) }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.features_go)) } }
            }
        }
        PrimaRaccolta.firstRoundOffer(page)?.let { cost ->
            DecideCard {
                Text(cost, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                FilledTonalButton(onClick = { onTouch("first_round", null) }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.prima_first_round)) }
            }
        }
        if (PrimaRaccolta.finished(page)) TextButton(onClick = onFeatures, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.prima_features)) }
    }
}

@Composable
private fun primaSection(key: String): String = when (key) {
    "machine" -> stringResource(R.string.prima_sec_machine); "hosts" -> stringResource(R.string.prima_sec_hosts); "quota" -> stringResource(R.string.prima_sec_quota)
    "plugins" -> stringResource(R.string.prima_sec_plugins); "projects" -> stringResource(R.string.prima_sec_projects); "live" -> stringResource(R.string.prima_sec_live)
    "sessions" -> stringResource(R.string.prima_sec_sessions); "suggestions" -> stringResource(R.string.prima_sec_suggestions); "night" -> stringResource(R.string.prima_sec_night)
    "recap" -> stringResource(R.string.prima_sec_recap); else -> key
}

/**
 * «Da provare» (contratto 1.99, banco_prova): le prove per versione. Quelle di una versione non ancora installata dicono di installarla prima e non
 * hanno i tasti. «Non va» chiede cosa non va, a voce dalla tastiera, e uno screenshot facoltativo: arrivano alla master.
 */
@Composable
fun DaProvareScreen(page: ProvePage?, buildOf: (String) -> BancoProva.Build, onBack: () -> Unit, onOk: (String) -> Unit, onKo: (ProveItem, String, android.net.Uri?) -> Unit, onScreen: (String) -> Unit) = DecidePage(stringResource(R.string.prove_title), "da-provare", onBack) { DaProvareBody(page, buildOf, onOk, onKo, onScreen) }

@Composable
fun DaProvareBody(page: ProvePage?, buildOf: (String) -> BancoProva.Build, onOk: (String) -> Unit, onKo: (ProveItem, String, android.net.Uri?) -> Unit, onScreen: (String) -> Unit) {
    var ko by remember { mutableStateOf<ProveItem?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

/** «Da decidere» (contratto 1.99, cinque_minuti): Franz sceglie quante, poi una alla volta, le più pesanti per prime. Niente timer. */
@Composable
fun CinqueMinutiScreen(page: DecisionsPage?, onBack: () -> Unit, onApprove: (String, String?) -> Unit, onDone: (DecisionItem) -> Unit, onNotNow: (String) -> Unit, onDrop: (String, String) -> Unit, startHowMany: Int? = null, onAsk: (String) -> Unit = {}) = DecidePage(stringResource(R.string.decisions_title), "cinque-minuti", onBack) { CinqueMinutiBody(page, onApprove, onDone, onNotNow, onDrop, startHowMany, onAsk) }

@Composable
fun CinqueMinutiBody(page: DecisionsPage?, onApprove: (String, String?) -> Unit, onDone: (DecisionItem) -> Unit, onNotNow: (String) -> Unit, onDrop: (String, String) -> Unit, startHowMany: Int? = null, onAsk: (String) -> Unit = {}) {
    var howMany by rememberSaveable { mutableStateOf(startHowMany) }
    var touched by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var dropping by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        // Contratto 1.104 (Franz 02/11 08:04): la card si legge senza ricordare niente; mai lo stesso testo due volte.
                        val c = CinqueMinuti.card(g)
                        c.head.forEachIndexed { n, h -> Text(h, style = if (n == 0) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium, color = if (n == 0) Cm.text else Cm.text2) }
                        Text(stringResource(if (g.where == "pc") R.string.decisions_at_pc else R.string.decisions_here) + if (g.weight > 1) " · " + stringResource(R.string.decisions_unblocks, g.weight) else "", style = MonoSmall, color = if (g.where == "pc") Cm.waiting else Cm.text2)
                        g.items.forEachIndexed { n, i ->
                            val x = c.items[n]
                            val name = i.title ?: i.short ?: i.session ?: i.task.orEmpty()
                            HorizontalDivider(color = Cm.line)
                            x.title?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Cm.text) }
                            x.plan?.let { PlanText(it) }
                            x.reason?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cm.text2) }
                            val proceed = stringResource(R.string.decisions_proceed_prompt, name, i.task ?: i.session.orEmpty())
                            val discuss = stringResource(R.string.sug_discuss_prompt, stringResource(R.string.decisions_discuss_about, name, i.task ?: i.session.orEmpty()))
                            val choicePrompt = stringResource(R.string.decisions_choice_prompt, name, i.task ?: i.session.orEmpty())
                            val btns = CinqueMinuti.buttons(i)
                            if (btns.isNotEmpty()) @OptIn(ExperimentalLayoutApi::class) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                btns.forEach { btn ->
                                    when (btn) {
                                        CinqueMinuti.Btn.Proceed -> FilledTonalButton(onClick = { if (CinqueMinuti.viaApprove(i)) onApprove(i.task!!, null) else onAsk(proceed); next() }) { Text(stringResource(R.string.decisions_proceed)) }
                                        CinqueMinuti.Btn.Discuss -> OutlinedButton(onClick = { onAsk(discuss); next() }) { Text(stringResource(R.string.decisions_discuss)) }
                                        CinqueMinuti.Btn.Done -> FilledTonalButton(onClick = { onDone(i); next() }) { Text(stringResource(R.string.agenda_done)) }
                                        is CinqueMinuti.Btn.Choice -> FilledTonalButton(onClick = { if (CinqueMinuti.viaApprove(i)) onApprove(i.task!!, btn.label) else onAsk(choicePrompt + " " + btn.label); next() }) { Text(btn.label) }
                                    }
                                }
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

/** L'inizio del piano, piegato oltre quattro righe col tasto per aprirlo: mai tagliato senza poterlo aprire. */
@Composable
private fun PlanText(text: String) {
    var open by rememberSaveable(text) { mutableStateOf(false) }
    var cut by remember(text) { mutableStateOf(false) }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Cm.text, maxLines = if (open) Int.MAX_VALUE else 4, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, onTextLayout = { r -> if (!open) cut = r.hasVisualOverflow })
    if (cut || open) TextButton(onClick = { open = !open }) { Text(stringResource(if (open) R.string.permission_less else R.string.permission_more)) }
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
