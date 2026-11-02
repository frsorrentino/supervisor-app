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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.launch
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
private fun DecideCard(picked: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    // La scheda scelta si schiarisce (voce 11), con il passaggio lento dei colori (voce 14).
    val bg by androidx.compose.animation.animateColorAsState(if (picked) Cm.surfaceHigh else Cm.surfaceLow, CmMotion.slow, label = "decide-card")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/**
 * Il tasto che diventa la conferma (voce 11, Franz 02/11 15:38): al tocco si accende di verde con la spunta e cresce un poco
 * con la molla dell'app; `faded` = è stato scelto un altro tasto. Al tocco anche la vibrazione di conferma (voce 13).
 */
@Composable
internal fun ConfirmButton(label: String, confirmed: Boolean, enabled: Boolean, faded: Boolean = false, onClick: () -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    val scale by androidx.compose.animation.core.animateFloatAsState(if (confirmed) 1.06f else 1f, CmMotion.bounceFloat, label = "confirm-scale")
    val bg by androidx.compose.animation.animateColorAsState(if (confirmed) V3.Good else Cm.surfaceHigh, CmMotion.slow, label = "confirm-bg")
    FilledTonalButton(
        onClick = { view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM); onClick() }, enabled = enabled || confirmed,
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = bg, contentColor = if (confirmed) Cm.bg else Cm.text, disabledContainerColor = bg, disabledContentColor = if (confirmed) Cm.bg else Cm.text2),
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (faded) 0.35f else 1f },
    ) { Text(if (confirmed) "✓ $label" else label) }
}

private fun Modifier.graphicsLayerAlpha(a: Float) = this.graphicsLayer { alpha = a }

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
                // Franz 02/11 15:38 (app-feedback-azioni-02-11): il tasto toccato diventa la conferma finché la scheda cambia.
                var tapped by remember(page.status, page.done) { mutableStateOf<String?>(null) }
                ConfirmButton(stringResource(R.string.prima_no), confirmed = tapped == "no", enabled = tapped == null, faded = tapped == "si") { tapped = "no"; onTouch("consent", "no") }
                ConfirmButton(stringResource(R.string.prima_yes), confirmed = tapped == "si", enabled = tapped == null, faded = tapped == "no") { tapped = "si"; onTouch("consent", "si") }
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
                var tappedRound by remember(page.status, page.done) { mutableStateOf(false) }
                Box(Modifier.align(Alignment.End)) { ConfirmButton(stringResource(R.string.prima_first_round), confirmed = tappedRound, enabled = !tappedRound) { tappedRound = true; onTouch("first_round", null) } }
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
fun DaProvareScreen(page: ProvePage?, buildOf: (String) -> BancoProva.Build, onBack: () -> Unit, onOk: suspend (String) -> Boolean, onKo: suspend (ProveItem, String, android.net.Uri?) -> Boolean, onScreen: (String) -> Unit) = DecidePage(stringResource(R.string.prove_title), "da-provare", onBack) { DaProvareBody(page, buildOf, onOk, onKo, onScreen) }

@Composable
fun DaProvareBody(page: ProvePage?, buildOf: (String) -> BancoProva.Build, onOk: suspend (String) -> Boolean, onKo: suspend (ProveItem, String, android.net.Uri?) -> Boolean, onScreen: (String) -> Unit) {
    var ko by remember { mutableStateOf<ProveItem?>(null) }
    // Franz 02/11 15:38: Ok non blocca più e la scheda non sparisce di colpo (Leave): conferma sul tasto, uscita in altezza,
    // le altre salgono; se il PC dice no, la scheda torna.
    var picked by remember { mutableStateOf(mapOf<String, String>()) }
    var gone by remember { mutableStateOf(setOf<String>()) }
    val off = animationsOff()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val hooks = remember {
        object : it.pixelbox.cmwatch.rules.Leave.Hooks {
            override fun pick(ids: List<String>, choice: String?) { picked = if (choice != null) picked + ids.associateWith { choice } else picked - ids.toSet() }
            override fun hide(ids: List<String>, hidden: Boolean) { gone = if (hidden) gone + ids else gone - ids.toSet() }
        }
    }
    val act: (List<String>, String, suspend () -> Boolean) -> Unit = { ids, choice, send ->
        scope.launch { it.pixelbox.cmwatch.rules.Leave.run(ids, choice, send, hooks) { kotlinx.coroutines.delay(it.pixelbox.cmwatch.rules.Leave.confirmMs(off)) } }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (page == null) Text(stringResource(R.string.prove_loading), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(4.dp))
        else if (page.groups.isEmpty() && page.ko.isEmpty() && page.red.isEmpty()) Text(stringResource(R.string.prove_none), style = MaterialTheme.typography.bodyMedium, color = Cm.text2, modifier = Modifier.padding(4.dp))
        page?.groups?.forEach { g ->
            val b = buildOf(g.build)
            val ids = g.prove.map { it.id.toString() }
            val shown = g.prove.filter { it.id.toString() !in gone }
            androidx.compose.animation.AnimatedVisibility(shown.isNotEmpty(), enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(if (off) 0 else 160)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(if (off) 0 else 160))) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(g.build, style = MonoSmall, modifier = Modifier.weight(1f))
                val allPicked = ids.all { picked[it] == "ok" }
                if (b != BancoProva.Build.TO_INSTALL && shown.size > 1) ConfirmButton(stringResource(R.string.prove_all_ok), confirmed = allPicked, enabled = ids.none { it in picked }) { act(ids, "ok") { onOk(BancoProva.allOk(g)) } }
            }
            }
            if (b == BancoProva.Build.TO_INSTALL) Text(stringResource(R.string.prove_install_first), style = MaterialTheme.typography.bodyMedium, color = Cm.waiting, modifier = Modifier.padding(horizontal = 4.dp))
            if (b == BancoProva.Build.UNKNOWN) Text(stringResource(R.string.prove_build_unknown), style = MaterialTheme.typography.bodySmall, color = Cm.text2, modifier = Modifier.padding(horizontal = 4.dp))
            g.prove.forEach { p ->
                val id = p.id.toString()
                val pk = picked[id]
                androidx.compose.animation.AnimatedVisibility(id !in gone, exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(if (off) 0 else 160)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(if (off) 0 else 160))) {
                DecideCard(picked = pk != null) {
                    Text(p.short ?: p.title ?: p.task, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Cm.text)
                    Text(p.fai, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                    Text(stringResource(R.string.prove_expected, p.atteso), style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BancoProva.screen(p.dove)?.let { s -> TextButton(onClick = { onScreen(s) }) { Text(stringResource(R.string.features_go)) } }
                        Spacer(Modifier.weight(1f))
                        if (b != BancoProva.Build.TO_INSTALL) {
                            OutlinedButton(onClick = { ko = p }, enabled = pk == null, modifier = Modifier.graphicsLayerAlpha(if (pk == "ok") 0.35f else 1f)) { Text(stringResource(R.string.prove_ko)) }
                            ConfirmButton(stringResource(R.string.prove_ok), confirmed = pk == "ok", enabled = pk == null, faded = pk == "ko") { act(listOf(id), "ok") { onOk(id) } }
                        }
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
            confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { val body = text.trim(); val s = shot; ko = null; act(listOf(p.id.toString()), "ko") { onKo(p, body, s) } }) { Text(stringResource(R.string.prove_ko_send)) } },
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
    // Franz 02/11 15:38 (app-feedback-azioni-02-11): il tasto scelto diventa la conferma e la scheda dopo entra con la sua
    // transizione, invece del cambio di colpo. Il comando parte subito; la scheda cambia dopo la conferma (Leave.confirmMs).
    var picked by remember { mutableStateOf<String?>(null) }
    val off = animationsOff()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
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
                    fun act(id: String, send: () -> Unit) {
                        if (picked != null) return
                        picked = id; send()
                        scope.launch { kotlinx.coroutines.delay(it.pixelbox.cmwatch.rules.Leave.confirmMs(off)); next(); picked = null }
                    }
                    Text(stringResource(R.string.decisions_position, touched.size + 1, minOf(many, page.groups.size)), style = MonoSmall, modifier = Modifier.padding(start = 4.dp))
                    // La scheda dopo entra da 0,96 a 1 con dissolvenza (voce 12), quella fatta esce in dissolvenza.
                    androidx.compose.animation.AnimatedContent(g, contentKey = { it.key }, label = "decide-next", transitionSpec = {
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(if (off) 0 else 200)) + androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(if (off) 0 else 200), initialScale = 0.96f)) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(if (off) 0 else 120))
                    }) { g ->
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
                                        CinqueMinuti.Btn.Proceed -> "$n-p".let { id -> ConfirmButton(stringResource(R.string.decisions_proceed), confirmed = picked == id, enabled = picked == null, faded = picked != null && picked != id) { act(id) { if (CinqueMinuti.viaApprove(i)) onApprove(i.task!!, null) else onAsk(proceed) } } }
                                        CinqueMinuti.Btn.Discuss -> "$n-d".let { id -> ConfirmButton(stringResource(R.string.decisions_discuss), confirmed = picked == id, enabled = picked == null, faded = picked != null && picked != id) { act(id) { onAsk(discuss) } } }
                                        CinqueMinuti.Btn.Done -> "$n-f".let { id -> ConfirmButton(stringResource(R.string.agenda_done), confirmed = picked == id, enabled = picked == null, faded = picked != null && picked != id) { act(id) { onDone(i) } } }
                                        is CinqueMinuti.Btn.Choice -> "$n-c-${btn.label}".let { id -> ConfirmButton(btn.label, confirmed = picked == id, enabled = picked == null, faded = picked != null && picked != id) { act(id) { if (CinqueMinuti.viaApprove(i)) onApprove(i.task!!, btn.label) else onAsk(choicePrompt + " " + btn.label) } } }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Cm.line)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { act("not_now") { onNotNow(g.key) } }, enabled = picked == null) { Text((if (picked == "not_now") "✓ " else "") + stringResource(R.string.decisions_not_now), color = if (picked == "not_now") V3.Good else androidx.compose.ui.graphics.Color.Unspecified) }
                            TextButton(onClick = { dropping = true }, enabled = picked == null) { Text(stringResource(R.string.decisions_drop), color = Cm.gone) }
                        }
                    }
                    }
                    if (dropping) {
                        var reason by rememberSaveable { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { dropping = false }, containerColor = Cm.surface,
                            title = { Text(stringResource(R.string.decisions_drop_title), color = Cm.text) },
                            text = { OutlinedTextField(reason, { reason = it }, label = { Text(stringResource(R.string.decisions_drop_reason)) }, modifier = Modifier.fillMaxWidth()) },
                            confirmButton = { TextButton(enabled = CinqueMinuti.canDrop(reason), onClick = { val r = reason.trim(); dropping = false; reason = ""; act("drop") { onDrop(g.key, r) } }) { Text(stringResource(R.string.decisions_drop), color = Cm.gone) } },
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
