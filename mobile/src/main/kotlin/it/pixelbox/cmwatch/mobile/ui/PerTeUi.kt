package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Guardare
import it.pixelbox.cmwatch.rules.PerTe
import it.pixelbox.cmwatch.rules.TiAspetta

/**
 * Il formato unico sul telefono e sul tablet (app-formato-unico-12-11, Franz 12/11 07:29: formato A e blocchi B; mockup in
 * docs/mockup/2026-11-12-formato-unico), come Blocco, Voce e PerTe della web.
 * - [ChatBlock]: un blocco della chat ridotto a una riga (icona, nome, quante voci, la prima); il tocco lo apre sul posto e lo
 *   richiude. Lo stesso per «Per te», «Azioni» e «Risultati».
 * - [VoceRow]: una voce: l'icona del tipo, il titolo, una riga di contesto con da quanto aspetta, il tasto principale; il tocco
 *   apre la scheda. Titolo e contesto su una riga, tagliati senza «…».
 * - [PerTeBlock]: «Per te» in cima alla chat di una sessione, una voce per tutto ciò che aspetta la persona.
 */
enum class BlockIcon { HAND, BOLT, EYE, WARN, LAYERS, FLAG }

/**
 * I riquadri della sessione aperti o chiusi, per sessione (app-riquadri-chiusi-16-11, Franz 16/11 20:02): la regola è in
 * [it.pixelbox.cmwatch.rules.Riquadri], qui lo stato di tutte le sessioni, ricordato dall'app con [save]. Chi non ha mai scelto li
 * trova chiusi, tranne «Per te» con una voce che aspetta.
 */
class SessionBoxes(initial: Map<String, it.pixelbox.cmwatch.rules.Riquadri.State>, private val save: (String, it.pixelbox.cmwatch.rules.Riquadri.State) -> Unit) {
    private val all = androidx.compose.runtime.mutableStateMapOf<String, it.pixelbox.cmwatch.rules.Riquadri.State>().apply { putAll(initial) }
    fun isOpen(session: String, kind: it.pixelbox.cmwatch.rules.Riquadri.Kind, waiting: List<String> = emptyList()): Boolean =
        it.pixelbox.cmwatch.rules.Riquadri.isOpen(all[session] ?: it.pixelbox.cmwatch.rules.Riquadri.State(), kind, waiting)
    fun toggle(session: String, kind: it.pixelbox.cmwatch.rules.Riquadri.Kind, waiting: List<String> = emptyList()) {
        val next = it.pixelbox.cmwatch.rules.Riquadri.toggle(all[session] ?: it.pixelbox.cmwatch.rules.Riquadri.State(), kind, waiting)
        all[session] = next
        save(session, next)
    }
}
val LocalSessionBoxes = androidx.compose.runtime.staticCompositionLocalOf { SessionBoxes(emptyMap()) { _, _ -> } }

/** La riga dei riquadri (app-riquadri-chiusi-16-11): le pillole in fila, che vanno a capo; un riquadro aperto prende la riga intera. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun RiquadriRow(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) =
    androidx.compose.foundation.layout.FlowRow(
        modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content,
    )

/** Il velo ambra sopra il fondo: il blocco e i tasti di ciò che aspetta (i Tasti B). */
private fun veil(alpha: Float, on: Color) = V3.Wait.copy(alpha = alpha).compositeOver(on)

@Composable
fun ChatBlock(
    icon: BlockIcon, title: String, n: Int, first: String?, open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier,
    amber: Boolean = false, hint: String? = null, tag: String? = null, bg: Color = V3.S1,
    extra: (@Composable RowScope.() -> Unit)? = null,
    /** app-riquadri-chiusi-16-11: chiuso è una pillola con icona, nome e numero; [count] al posto del numero («65%»). */
    pill: Boolean = false, count: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (pill && !open) return BlockPill(icon, title, count ?: n.toString(), amber, onToggle, modifier, tag)
    val lit = amber && !open
    val shape = RoundedCornerShape(14.dp)
    val label = stringResource(if (open) R.string.box_close else R.string.box_open, title)
    Column(
        modifier.fillMaxWidth().clip(shape).background(if (lit) veil(0.09f, bg) else bg).border(1.dp, if (lit) veil(0.38f, V3.Line) else V3.Line, shape)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).handCursor().clickable(onClickLabel = label, role = Role.Button, onClick = onToggle).padding(start = 12.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tint = if (amber) V3.Wait else V3.Tx2
            BlockGlyph(icon, tint, 20)
            Text(title, style = V3.Name.copy(fontSize = 16.sp), maxLines = 1, softWrap = false)
            Text(count ?: n.toString(), style = V3.Small.copy(fontSize = 13.sp, color = tint), maxLines = 1)
            Text((if (open) hint else first).orEmpty(), style = V3.Small, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            extra?.invoke(this)
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.size(20.dp))
        }
        if (open) content()
    }
}

@Composable
private fun BlockGlyph(icon: BlockIcon, tint: Color, size: Int) {
    val m = Modifier.size(size.dp)
    when (icon) {
        BlockIcon.HAND -> Icon(painterResource(R.drawable.ic_hand), null, tint = tint, modifier = m)
        BlockIcon.BOLT -> Icon(Icons.Outlined.Bolt, null, tint = tint, modifier = m)
        BlockIcon.EYE -> Icon(Icons.Outlined.Visibility, null, tint = tint, modifier = m)
        BlockIcon.WARN -> Icon(androidx.compose.material.icons.Icons.Rounded.WarningAmber, null, tint = tint, modifier = m)
        BlockIcon.LAYERS -> Icon(androidx.compose.material.icons.Icons.Outlined.Layers, null, tint = tint, modifier = m)
        BlockIcon.FLAG -> Icon(androidx.compose.material.icons.Icons.Outlined.Flag, null, tint = tint, modifier = m)
    }
}

/** Un riquadro chiuso (app-riquadri-chiusi-16-11): una pillola con icona, nome e numero; il tocco lo apre. 40 dp per il dito. */
@Composable
private fun BlockPill(icon: BlockIcon, title: String, count: String, amber: Boolean, onToggle: () -> Unit, modifier: Modifier, tag: String?) {
    val tint = if (amber) V3.Wait else V3.Tx2
    Row(
        modifier.clip(CircleShape).background(if (amber) veil(0.09f, V3.S1) else V3.S1).border(1.dp, if (amber) veil(0.38f, V3.Line) else V3.Line, CircleShape)
            .handCursor().clickable(onClickLabel = stringResource(R.string.box_open, title), role = Role.Button, onClick = onToggle)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .heightIn(min = 40.dp).padding(start = 10.dp, end = 13.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BlockGlyph(icon, tint, 19)
        Text(title, style = V3.Name.copy(fontSize = 15.sp), maxLines = 1, softWrap = false)
        Text(count, style = V3.Small.copy(fontSize = 13.sp, color = tint), maxLines = 1, softWrap = false)
    }
}

/** Il tasto principale di una voce: tonale, in ambra sulla voce che aspetta una risposta. */
@Composable
fun VoceButton(label: String, hot: Boolean, tag: String? = null, onClick: () -> Unit) {
    Text(
        label, style = V3.Name.copy(fontSize = 15.sp, color = if (hot) V3.Wait else V3.Tx), maxLines = 1, softWrap = false,
        modifier = Modifier.clip(CircleShape).background(if (hot) veil(0.12f, V3.S1) else V3.S3)
            .then(if (hot) Modifier.border(1.dp, V3.Wait.copy(alpha = 0.45f), CircleShape) else Modifier)
            .handCursor().clickable(role = Role.Button, onClick = onClick).then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .heightIn(min = 36.dp).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private fun groupIcon(g: PerTe.Group): ImageVector? = when (g) {
    PerTe.Group.ASPETTA -> null
    PerTe.Group.DECIDI -> Icons.Outlined.Gavel
    PerTe.Group.PROVA -> Icons.Outlined.Science
    PerTe.Group.VERIFICA -> Icons.Outlined.Checklist
}

/**
 * Una voce. `main`: l'etichetta del tasto, null = la freccia; `tone`: il colore del contesto per l'esito di una prova già chiusa;
 * `card`: da sola in una chat, col suo bordo.
 */
@Composable
fun VoceRow(
    group: PerTe.Group, title: String, sub: String, ago: String?, main: String?, onOpen: () -> Unit, onMain: () -> Unit,
    modifier: Modifier = Modifier, tone: Color? = null, card: Boolean = false, tag: String? = null,
    /** app-riall-2-viste-16-11: il consiglio di chi chiede, in ambra in testa alla riga di contesto; [rec]: il tasto è la scelta consigliata. */
    advice: String? = null, rec: Boolean = false,
    /** app-silenzio-assenso-23-11: «si chiude da sola gio 26/11», in blu in testa alla riga e al posto di da quanto aspetta. */
    silence: String? = null,
) {
    val hot = group == PerTe.Group.ASPETTA
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth()
            .then(if (card) Modifier.clip(shape).background(V3.S1).border(1.dp, V3.Line, shape) else if (hot) Modifier.background(V3.Wait.copy(alpha = 0.08f)) else Modifier)
            .heightIn(min = 52.dp).handCursor().clickable(role = Role.Button, onClick = onOpen).then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (hot) veil(0.14f, V3.S1) else V3.S3)
                .then(if (hot) Modifier.border(1.dp, V3.Wait.copy(alpha = 0.4f), RoundedCornerShape(10.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            val tint = if (hot) V3.Wait else V3.Tx2
            groupIcon(group)?.let { Icon(it, null, tint = tint, modifier = Modifier.size(19.dp)) } ?: Icon(painterResource(R.drawable.ic_hand), null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = V3.Name, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            Text(
                buildAnnotatedString {
                    // La data del silenzio assenso in testa, così la riga tagliata a 360 dp la tiene.
                    if (silence != null) { withStyle(SpanStyle(color = V3.Busy)) { append(silence) }; if (advice != null || sub.isNotEmpty()) append(" · ") }
                    if (advice != null) { withStyle(SpanStyle(color = V3.Wait)) { append(advice) }; if (sub.isNotEmpty()) append(" · ") }
                    append(sub)
                    if (silence == null && ago != null) { if (sub.isNotEmpty()) append(" · "); if (hot) withStyle(SpanStyle(color = V3.Wait)) { append(ago) } else append(ago) }
                },
                style = V3.Small.copy(fontSize = 13.sp, lineHeight = 18.sp, color = tone ?: V3.Tx2), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
            )
        }
        if (main != null) VoceButton(main, hot || rec, tag?.let { "$it-tasto" }, onMain)
        else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx3, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun kindWord(k: Guardare.Kind) = stringResource(when (k) {
    Guardare.Kind.VIDEO -> R.string.watch_kind_video; Guardare.Kind.MOCKUP -> R.string.watch_kind_mockup
    Guardare.Kind.IMAGE -> R.string.watch_kind_image; Guardare.Kind.DOCUMENT -> R.string.watch_kind_document; Guardare.Kind.OTHER -> R.string.watch_kind_other
})

@Composable
private fun groupWord(g: PerTe.Group) = stringResource(when (g) {
    PerTe.Group.ASPETTA -> R.string.per_te_aspetta; PerTe.Group.DECIDI -> R.string.per_te_decidi
    PerTe.Group.PROVA -> R.string.per_te_prova; PerTe.Group.VERIFICA -> R.string.per_te_verifica
})

/**
 * Le voci di `attese` in Dalla master, di tutte le sessioni (app-riall-1-dati-16-11, contratto 1.151; Franz 16/11: le voci per intero,
 * coi tasti): la stessa riga di «Per te», col nome della sessione davanti al contesto. Il tocco apre la scheda Ti aspetta; il tasto
 * principale risponde ([onMain] col suo indice), e un tasto che chiede un testo apre la scheda.
 */
@Composable
fun VoceListBody(items: List<it.pixelbox.cmwatch.contract.WaitItem>, now: Long, tag: String, onOpen: (String) -> Unit, onMain: (it.pixelbox.cmwatch.contract.WaitItem, Int) -> Unit) {
    if (items.isEmpty()) return
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(V3.S1).border(1.dp, V3.Line, shape).testTag(tag)) {
        items.forEachIndexed { i, w ->
            val v = PerTe.voceOf(w)
            if (i > 0) HorizontalDivider(color = V3.Line)
            val sub = listOfNotNull(w.session, groupWord(v.group), v.context).joinToString(" · ")
            val age = TiAspetta.age(now - v.since)
            VoceRow(
                v.group, v.title, sub, if (v.group == PerTe.Group.ASPETTA) stringResource(R.string.ti_aspetta_since, age) else age, v.main?.label,
                { onOpen(v.key) }, { val m = v.main; if (m == null || !m.direct) onOpen(v.key) else onMain(w, m.index) }, tag = "voce-${v.key}",
            )
        }
    }
}

/**
 * «Per te» in cima alla chat: chiuso, una pillola col numero (app-riquadri-chiusi-16-11); aperto, l'elenco. `onMain` è il tasto di una
 * voce (chi lo riceve manda il comando, segna visto o apre la scheda se il tasto chiede un testo), `onOpen` il tocco sulla voce.
 */
@Composable
fun PerTeBlock(
    items: List<PerTe.Voce>, now: Long, open: Boolean, onToggle: () -> Unit, onOpen: (PerTe.Voce) -> Unit, onMain: (PerTe.Voce) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * app-riall-2-viste-16-11 (direzione A): [onMaster] è il rimando «anche in Dalla master ›» in testa alla lista; con [numberOnly] (il
     * tablet con la home accanto) la sessione tiene solo il numero, e il tocco mostra il suo gruppo in Dalla master.
     */
    onMaster: (() -> Unit)? = null, numberOnly: Boolean = false,
) {
    val top = items.firstOrNull() ?: return
    ChatBlock(
        BlockIcon.HAND, stringResource(R.string.fy_title), items.size, top.title, open && !(numberOnly && onMaster != null), if (numberOnly && onMaster != null) onMaster else onToggle, modifier, amber = true,
        hint = stringResource(R.string.per_te_hint), tag = "chat-per-te", pill = true,
    ) {
        if (onMaster != null && !numberOnly) {
            HorizontalDivider(color = V3.Line)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 36.dp).handCursor().clickable(role = Role.Button, onClick = onMaster).testTag("per-te-in-master").padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End,
            ) {
                Text(stringResource(R.string.per_te_also_master), style = V3.Small.copy(fontSize = 13.sp, color = V3.Tx2), maxLines = 1, softWrap = false)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx2, modifier = Modifier.size(16.dp))
            }
        }
        items.forEach { v ->
            HorizontalDivider(color = V3.Line)
            VoceItem(v, now, onOpen, onMain)
        }
    }
}

/**
 * La scheda di una sessione in Home (app-riall-3-doppioni-16-11, direzione A): niente tasti di risposta, la mano di «Per te» col numero
 * delle sue voci, compatta perché il nome resti intero; il tocco apre la sessione.
 */
@Composable
fun PerTeCardChip(n: Int, onClick: () -> Unit) {
    val label = "${stringResource(R.string.fy_title)} $n"
    Row(
        Modifier.clip(CircleShape).background(veil(0.12f, V3.S1)).border(1.dp, V3.Wait.copy(alpha = 0.45f), CircleShape).handCursor()
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label }.testTag("scheda-per-te").heightIn(min = 32.dp).padding(start = 9.dp, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(painterResource(R.drawable.ic_hand), null, tint = V3.Wait, modifier = Modifier.size(17.dp))
        Text(n.toString(), style = V3.Name.copy(fontSize = 14.sp, color = V3.Wait), maxLines = 1)
    }
}

/** Il consiglio di chi chiede sulla riga (app-riall-2-viste-16-11): «consigliata: approvare», «consigliata: 2 · sì». */
@Composable
fun adviceText(a: PerTe.Advice?): String? {
    if (a == null) return null
    val o = a.option
    return when {
        o != null -> stringResource(R.string.per_te_advice_option, o)
        a.approve == true -> stringResource(R.string.per_te_advice_approve)
        else -> stringResource(R.string.per_te_advice_reject)
    }
}

/** Una voce di «Per te», la stessa nella sessione e in Dalla master: contesto, da quanto aspetta, il consiglio e il tasto. */
@Composable
fun VoceItem(v: PerTe.Voce, now: Long, onOpen: (PerTe.Voce) -> Unit, onMain: (PerTe.Voce) -> Unit, session: String? = null) {
    // [session]: una carta che blocca un lavoro, fuori dal gruppo della sua sessione, la dice in testa (app-per-te-ordine-23-11).
    val sub = listOfNotNull(
        session,
        groupWord(v.group),
        v.result?.let { r -> listOfNotNull(kindWord(r.kind), r.version?.let { n -> stringResource(R.string.per_te_version, n) }).joinToString(", ") } ?: v.context,
    ).joinToString(" · ")
    val age = TiAspetta.age(now - v.since)
    VoceRow(
        v.group, v.title, sub, if (v.group == PerTe.Group.ASPETTA) stringResource(R.string.ti_aspetta_since, age) else age,
        if (v.result != null) stringResource(R.string.watch_seen) else v.main?.label, { onOpen(v) }, { onMain(v) }, tag = "voce-${v.key}",
        advice = adviceText(v.advice), rec = v.main?.recommended == true,
        silence = v.wait?.silenceUntil?.takeIf { u -> it.pixelbox.cmwatch.rules.Silence.pending(u, now) }?.let { u ->
            stringResource(R.string.silence_row, it.pixelbox.cmwatch.rules.Silence.whenShort(u, java.time.ZoneId.systemDefault(), it.pixelbox.cmwatch.rules.Silence.localeOf(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])))
        },
    )
}

/**
 * Dalla master per sessione (app-riall-2-viste-16-11, direzione A scelta da Franz il 16/11; mockup a-master e a-largo in
 * docs/mockup/2026-11-16-chiarezza): le stesse voci e gli stessi tasti di «Per te» nella sessione, un gruppo per sessione. Il titolo del
 * gruppo porta alla sessione; sul tablet il gruppo della sessione accanto è acceso («questa»). In fondo le voci senza sessione. Prende il
 * posto di Decidi e Verifica: il numero della sezione sono queste righe. [focus]: il rimando da una sessione, il cui gruppo viene in
 * vista e si illumina un momento.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PerTeGroupsBody(
    groups: List<PerTe.Gruppo>, now: Long, current: Set<String>, focus: HomeFolds.PerTeFocus?,
    onSession: (String) -> Unit, onOpen: (PerTe.Voce) -> Unit, onMain: (PerTe.Voce) -> Unit,
) {
    if (groups.isEmpty()) return
    // app-per-te-ordine-23-11 (Franz 23/11 19:00 e 19:01; contratto 1.160): in alto, aperte, le carte che bloccano un compito in corso,
    // ognuna con la sua sessione; sotto i gruppi per sessione col resto, chiusi di base, col nome, quante voci e da quanto, e la stessa
    // freccia delle altre sezioni; «apri» porta alla sessione. Il rimando da una sessione apre il suo gruppo. I gruppi aperti a mano
    // restano aperti finché l'app è aperta.
    val order = PerTe.splitBlocking(groups)
    val opened = androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<String>, String>({ it.toList() }, { androidx.compose.runtime.mutableStateListOf(*it.toTypedArray()) }),
    ) { androidx.compose.runtime.mutableStateListOf<String>() }
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(V3.S1).border(1.dp, V3.Line, shape).testTag("per-te-gruppi")) {
        if (order.top.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().background(V3.Wait.copy(alpha = 0.10f).compositeOver(V3.S2)).heightIn(min = 48.dp).padding(horizontal = 16.dp).testTag("per-te-bloccanti"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(V3.Wait))
                Text(stringResource(R.string.per_te_blocking), style = V3.Name.copy(fontSize = 16.sp), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                Text(pluralStringResource(R.plurals.per_te_group_n, order.top.size, order.top.size), style = V3.Small.copy(fontSize = 13.sp, color = V3.Tx2), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            }
            order.top.forEach { bl ->
                HorizontalDivider(color = V3.Line)
                VoceItem(bl.voce, now, onOpen, onMain, session = bl.session)
            }
        }
        order.groups.forEachIndexed { gi, g ->
            if (gi > 0 || order.top.isNotEmpty()) HorizontalDivider(color = V3.Line)
            val name = g.session
            val key = name.orEmpty()
            val on = key in opened
            val here = name != null && name in current
            val req = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
            var lit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            val fx = focus?.takeIf { f -> f.session == name }
            androidx.compose.runtime.LaunchedEffect(fx?.n) {
                if (fx == null || System.currentTimeMillis() - fx.at > 1500) return@LaunchedEffect
                if (key !in opened) opened += key
                lit = true
                req.bringIntoView()
                kotlinx.coroutines.delay(1600)
                lit = false
            }
            val bg by androidx.compose.animation.animateColorAsState(
                when { lit -> V3.Wait.copy(alpha = 0.26f).compositeOver(V3.S2); here -> V3.Wait.copy(alpha = 0.12f).compositeOver(V3.S2); else -> V3.S2 }, label = "gruppo",
            )
            Row(Modifier.fillMaxWidth().bringIntoViewRequester(req).background(bg).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 48.dp).handCursor().clickable(role = Role.Button) { if (on) opened -= key else opened += key }
                        .testTag(if (name != null) "gruppo-$name" else "gruppo-senza-sessione").padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (g.voci.any { v -> v.group != PerTe.Group.VERIFICA }) V3.Wait else V3.Tx3))
                    Text(name ?: stringResource(R.string.per_te_no_session), style = V3.Name.copy(fontSize = 16.sp), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                    Text(
                        pluralStringResource(R.plurals.per_te_group_n, g.voci.size, g.voci.size) + " · " +
                            (if (here) stringResource(R.string.per_te_group_this) else TiAspetta.age(now - g.voci.minOf { v -> v.since })),
                        style = V3.Small.copy(fontSize = 13.sp, color = if (here) V3.Wait else V3.Tx2), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f),
                    )
                    TurnArrow(on, V3.Tx2, Modifier.size(20.dp))
                }
                if (name != null) Row(
                    Modifier.heightIn(min = 48.dp).handCursor().clickable(role = Role.Button) { onSession(name) }.testTag("gruppo-apri-$name").padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.per_te_group_open), style = V3.Small.copy(fontSize = 13.sp, color = V3.Tx2), maxLines = 1, softWrap = false)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = V3.Tx2, modifier = Modifier.size(16.dp))
                }
            }
            FoldIn(on) {
                Column {
                    g.voci.forEach { v ->
                        HorizontalDivider(color = V3.Line)
                        VoceItem(v, now, onOpen, onMain)
                    }
                }
            }
        }
    }
}
