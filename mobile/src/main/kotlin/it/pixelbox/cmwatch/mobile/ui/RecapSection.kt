package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.rules.RecapActions
import it.pixelbox.cmwatch.rules.RecapAgenda

/** «06/10» dal giorno ISO del recap; vuoto se non si legge. */
internal fun recapDay(date: String): String =
    runCatching { java.time.LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) }.getOrDefault("")

/**
 * Un'azione della sezione Recap come tasto (mockup approvato l'08/10, tavola 1): il testo e, piccolo accanto, da dove viene
 * (la sessione, o «recap 06/10»). Mandata, resta grigia con la spunta finché lo stato non la toglie.
 */
@Composable
internal fun RecapActionChip(a: RecapActions.Action, day: String, sent: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.step_sent, a.text)
    Surface(
        onClick = onClick, enabled = !sent, shape = CircleShape,
        color = if (sent) Cm.idle.copy(alpha = 0.14f) else Cm.surfaceLow, contentColor = if (sent) Cm.text2 else Cm.text,
        modifier = if (sent) Modifier.semantics { contentDescription = label } else Modifier,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sent) Icon(Icons.Rounded.Check, null, tint = Cm.idle, modifier = Modifier.size(16.dp))
            // L'etichetta resta su una riga e, se è un nome lungo, scende sotto: il testo dell'azione non si schiaccia mai.
            V3Split(Modifier.weight(1f, fill = false), text = { Text(a.text, style = MaterialTheme.typography.bodyLarge) }) {
                Text(
                    if (a.recap) stringResource(R.string.recap_from_recap, day).trim() else a.from,
                    style = MaterialTheme.typography.bodySmall, color = Cm.text2, softWrap = false, maxLines = 1,
                )
            }
        }
    }
}

/**
 * «Mandare questa azione?» (tavola 3): da dove viene, il testo che parte, a chi va — la sessione viva del progetto, o la
 * master con «Riprendi …». Un solo tasto pieno, «Manda».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecapSendSheet(a: RecapActions.Action, day: String, onDismiss: () -> Unit, from: String? = null, onSend: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cm.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.recap_send_title), style = MaterialTheme.typography.headlineSmall, color = Cm.text)
            Text(
                from ?: when {
                    a.agenda -> stringResource(R.string.recap_send_from_agenda)
                    a.recap -> stringResource(R.string.recap_send_from_recap, day, a.from)
                    else -> stringResource(R.string.recap_send_from_session, a.from)
                },
                style = MaterialTheme.typography.bodyMedium, color = Cm.text2,
            )
            Text(
                a.send, style = MaterialTheme.typography.bodyLarge, color = Cm.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cm.surfaceLow).padding(horizontal = 18.dp, vertical = 14.dp),
            )
            val to = stringResource(if (a.agenda) R.string.recap_send_to_master_agenda else if (a.viaMaster) R.string.recap_send_to_master else R.string.recap_send_to_session, a.to)
            Text(
                buildAnnotatedString {
                    val i = to.indexOf(a.to)
                    if (i < 0) append(to) else {
                        append(to.substring(0, i)); withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Cm.text)) { append(a.to) }; append(to.substring(i + a.to.length))
                    }
                },
                style = MaterialTheme.typography.bodyMedium, color = Cm.text2,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Cm.actionIcon) }
                androidx.compose.material3.Button(
                    onClick = onSend,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary),
                ) { Text(stringResource(R.string.recap_send)) }
            }
        }
    }
}

/** Il segno a sinistra di una riga dell'agenda: tondo personale, quadrato agenzia, rombo postazione. */
@Composable
internal fun AgendaMark(scope: String) {
    val m = RecapAgenda.mark(scope)
    val border = androidx.compose.foundation.BorderStroke(2.dp, Cm.text2)
    val mod = Modifier.padding(top = 3.dp).size(18.dp)
    when (m) {
        RecapAgenda.Mark.PERSONAL -> androidx.compose.foundation.layout.Box(mod.border(border, CircleShape))
        RecapAgenda.Mark.AGENCY -> androidx.compose.foundation.layout.Box(mod.border(border, RoundedCornerShape(5.dp)))
        RecapAgenda.Mark.DESK -> androidx.compose.foundation.layout.Box(mod.graphicsLayer { rotationZ = 45f; scaleX = 0.8f; scaleY = 0.8f }.border(border, RoundedCornerShape(3.dp)))
        RecapAgenda.Mark.NONE -> androidx.compose.foundation.layout.Box(mod)
    }
}

/**
 * Una riga dell'agenda (tavole 1 e 2): segno, titolo, sotto il rimando — o «blocca: …» se è ferma su altro, o lo stato se
 * non è aperta. Come le card delle sessioni (Franz, 10/10 09:24, variante A, al posto del tasto «Azioni»): ˅ la apre sul
 * posto, con il dettaglio salvato, le azioni come pillole («Fallo» piena) e il riferimento; il tocco sul titolo apre
 * «Approfondisci».
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AgendaRowCard(
    row: AgendaRow, other: Boolean = false, onOpen: (() -> Unit)? = null,
    /** Le voci del menu della scheda (`RecapAgenda.menu`); senza voci niente ˅. */
    items: List<RecapAgenda.Item> = emptyList(),
    /** Solo per i provini: la card già aperta, e con l'elenco delle altre azioni aperto. */
    startOpen: Boolean = false, startMore: Boolean = false,
    onItem: (RecapAgenda.Item) -> Unit = {},
) {
    var open by rememberSaveable(row.key.ifBlank { row.title }) { mutableStateOf(startOpen) }
    var more by rememberSaveable(row.key.ifBlank { row.title }) { mutableStateOf(startMore) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cm.surface).padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            AgendaMark(row.scope)
            Column(
                Modifier.weight(1f).then(if (onOpen != null) Modifier.clip(RoundedCornerShape(10.dp)).handCursor().clickable(onClick = onOpen) else Modifier),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(row.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = Cm.text)
                val sub = when {
                    row.state.trim().lowercase() == "sospeso" && !row.until.isNullOrBlank() -> stringResource(R.string.agenda_until, untilLabel(row.until))
                    !RecapAgenda.isOpen(row) -> listOf(row.state.trim(), row.ref.trim()).filter { it.isNotEmpty() }.joinToString(" · ")
                    other -> stringResource(R.string.recap_blocks, row.blocks.trim())
                    else -> row.ref.trim()
                }
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = Cm.text2)
            }
            if (items.isNotEmpty()) androidx.compose.material3.IconButton(onClick = { open = !open }, modifier = Modifier.size(40.dp).handCursor()) {
                Icon(
                    if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    stringResource(if (open) R.string.agenda_card_close else R.string.agenda_card_open), tint = Cm.text2,
                )
            }
        }
        if (open && items.isNotEmpty()) Column(Modifier.padding(end = 10.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.HorizontalDivider(color = Cm.line)
            val detail = row.detail?.trim()?.takeIf { it.isNotEmpty() }
            Text(detail ?: stringResource(R.string.agenda_no_detail), style = MaterialTheme.typography.bodyMedium, color = if (detail != null) Cm.text else Cm.text2)
            // I tasti (Franz, 10/10 10:36, variante A + C): una barra di icone con l'etichetta sotto, uguali, «Fallo» pieno, per le
            // azioni più usate; «Altro» apre qui sotto l'elenco delle altre, come nella C. Senza «Fallo» (scheda non aperta) la
            // barra comincia da quello che c'è.
            val bar = listOf(RecapAgenda.Item.DO, RecapAgenda.Item.NIGHT, RecapAgenda.Item.TALK, RecapAgenda.Item.DONE).filter { it in items }
            val rest = items.filter { it != RecapAgenda.Item.DEEPEN && it !in bar }
            Row(Modifier.fillMaxWidth()) {
                bar.forEach { item -> AgendaBarKey(barIcon(item), barLabel(item), filled = item == RecapAgenda.Item.DO, lit = false, Modifier.weight(1f)) { onItem(item) } }
                if (rest.isNotEmpty()) AgendaBarKey(Icons.Rounded.MoreHoriz, stringResource(R.string.agenda_more_short), filled = false, lit = more, Modifier.weight(1f)) { more = !more }
                // La griglia resta di cinque: con meno tasti stanno a sinistra, della stessa misura.
                repeat(5 - bar.size - (if (rest.isNotEmpty()) 1 else 0)) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
            if (rest.isNotEmpty() && more) Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cm.surfaceLow).padding(vertical = 6.dp),
            ) {
                rest.forEach { item ->
                    val red = item == RecapAgenda.Item.REMOVE
                    Row(
                        Modifier.fillMaxWidth().handCursor().clickable { onItem(item) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(icon(item), null, tint = if (red) Cm.gone else Cm.actionIcon, modifier = Modifier.size(22.dp))
                        Text(label(item), style = MaterialTheme.typography.bodyLarge, color = if (red) Cm.gone else Cm.text)
                    }
                }
            }
        }
    }
}

/** Un tasto della barra: la piastrella con l'icona e sotto l'etichetta; `filled` = «Fallo», `lit` = «Altro» aperto. */
@Composable
private fun AgendaBarKey(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, filled: Boolean, lit: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).handCursor().clickable(onClickLabel = text, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(
                when { filled -> Cm.primary; lit -> androidx.compose.ui.graphics.Color(0xFF3A4A66); else -> Cm.surfaceHigh },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = when { filled -> Cm.onPrimary; lit -> Cm.primary; else -> Cm.actionIcon }, modifier = Modifier.size(24.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = Cm.text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
    }
}

/** Le icone della barra: ▶ per «Fallo», la luna per «Stanotte», il fumetto per «Parlane», la spunta per «Fatto». */
private fun barIcon(i: RecapAgenda.Item): androidx.compose.ui.graphics.vector.ImageVector = when (i) {
    RecapAgenda.Item.DO -> Icons.Rounded.PlayArrow
    RecapAgenda.Item.DONE -> Icons.Rounded.Check
    else -> icon(i)
}

/** Le etichette brevi della barra: «Parlane» invece di «Parlane con la master», che nell'elenco resta intera. */
@Composable
private fun barLabel(i: RecapAgenda.Item): String = if (i == RecapAgenda.Item.TALK) stringResource(R.string.agenda_talk_short) else label(i)

/** «ven 10/10» da `AAAA-MM-GG`; com'è se non si legge. */
@Composable
internal fun untilLabel(until: String?): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return runCatching { java.time.LocalDate.parse(until!!.trim()).format(java.time.format.DateTimeFormatter.ofPattern("EEE dd/MM", locale)) }.getOrDefault(until.orEmpty())
}

/** Il titoletto colorato di un gruppo: «ASPETTA TE · 8» ambra, «PUÒ FARLO CLAUDE · 6» celeste, gli altri grigi. */
@Composable
internal fun RecapGroup(text: String, tone: androidx.compose.ui.graphics.Color) = GroupHeader(text, tone)

/** Le Azioni come tasti, a capo quando non stanno. */
@Composable
internal fun RecapActionChips(actions: List<RecapActions.Action>, day: String, sent: List<String>, onAsk: (RecapActions.Action) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { a -> RecapActionChip(a, day, sentKey(a) in sent) { onAsk(a) } }
    }
}

internal fun sentKey(a: RecapActions.Action) = a.to + "\n" + a.send

/**
 * La pagina Recap (tavola 2): dal menu ≡ o da «Tutto il recap». Filtri per ambito in cima; le Azioni; poi chi deve muoversi;
 * in fondo, richiuso, fatto e sospeso. `agenda` null = in arrivo; `error` = il rifiuto del relay, già in parole.
 */
