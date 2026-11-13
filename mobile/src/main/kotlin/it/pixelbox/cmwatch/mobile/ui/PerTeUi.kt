package it.pixelbox.cmwatch.mobile.ui

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
enum class BlockIcon { HAND, BOLT, EYE }

/** I blocchi della chat aperti o ridotti, ricordati dall'app: chi non ha mai scelto li trova ridotti. */
class ChatBlocks(perTe: Boolean, results: Boolean, private val save: (String, Boolean) -> Unit) {
    var perTe by mutableStateOf(perTe)
        private set
    var results by mutableStateOf(results)
        private set
    fun perTe(open: Boolean) { perTe = open; save(PER_TE, open) }
    fun results(open: Boolean) { results = open; save(RESULTS, open) }
    companion object { const val PER_TE = "block_per_te_open"; const val RESULTS = "block_results_open" }
}
val LocalChatBlocks = androidx.compose.runtime.staticCompositionLocalOf { ChatBlocks(false, false) { _, _ -> } }

/** Il velo ambra sopra il fondo: il blocco e i tasti di ciò che aspetta (i Tasti B). */
private fun veil(alpha: Float, on: Color) = V3.Wait.copy(alpha = alpha).compositeOver(on)

@Composable
fun ChatBlock(
    icon: BlockIcon, title: String, n: Int, first: String?, open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier,
    amber: Boolean = false, hint: String? = null, tag: String? = null, bg: Color = V3.S1,
    extra: (@Composable RowScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit,
) {
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
            when (icon) {
                BlockIcon.HAND -> Icon(painterResource(R.drawable.ic_hand), null, tint = tint, modifier = Modifier.size(20.dp))
                BlockIcon.BOLT -> Icon(Icons.Outlined.Bolt, null, tint = tint, modifier = Modifier.size(20.dp))
                BlockIcon.EYE -> Icon(Icons.Outlined.Visibility, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(title, style = V3.Name.copy(fontSize = 16.sp), maxLines = 1, softWrap = false)
            Text(n.toString(), style = V3.Small.copy(fontSize = 13.sp, color = tint), maxLines = 1)
            Text((if (open) hint else first).orEmpty(), style = V3.Small, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            extra?.invoke(this)
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = V3.Tx3, modifier = Modifier.size(20.dp))
        }
        if (open) content()
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
                    append(sub)
                    if (ago != null) { if (sub.isNotEmpty()) append(" · "); if (hot) withStyle(SpanStyle(color = V3.Wait)) { append(ago) } else append(ago) }
                },
                style = V3.Small.copy(fontSize = 13.sp, lineHeight = 18.sp, color = tone ?: V3.Tx2), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
            )
        }
        if (main != null) VoceButton(main, hot, tag?.let { "$it-tasto" }, onMain)
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
 * «Per te» in cima alla chat: ridotto, una riga con la prima voce e il suo tasto; aperto, l'elenco. `onMain` è il tasto di una
 * voce (chi lo riceve manda il comando, segna visto o apre la scheda se il tasto chiede un testo), `onOpen` il tocco sulla voce.
 */
@Composable
fun PerTeBlock(
    items: List<PerTe.Voce>, now: Long, open: Boolean, onToggle: () -> Unit, onOpen: (PerTe.Voce) -> Unit, onMain: (PerTe.Voce) -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = items.firstOrNull() ?: return
    val seen = stringResource(R.string.watch_seen)
    val label: (PerTe.Voce) -> String? = { v -> if (v.result != null) seen else v.main?.label }
    ChatBlock(
        BlockIcon.HAND, stringResource(R.string.fy_title), items.size, top.title, open, onToggle, modifier, amber = true,
        hint = stringResource(R.string.per_te_hint), tag = "chat-per-te",
        extra = { if (!open) label(top)?.let { l -> VoceButton(l, hot = true, tag = "chat-per-te-tasto") { onMain(top) } } },
    ) {
        items.forEach { v ->
            HorizontalDivider(color = V3.Line)
            val sub = listOfNotNull(
                groupWord(v.group),
                v.result?.let { r -> listOfNotNull(kindWord(r.kind), r.version?.let { n -> stringResource(R.string.per_te_version, n) }).joinToString(", ") } ?: v.context,
            ).joinToString(" · ")
            val age = TiAspetta.age(now - v.since)
            VoceRow(v.group, v.title, sub, if (v.group == PerTe.Group.ASPETTA) stringResource(R.string.ti_aspetta_since, age) else age, label(v), { onOpen(v) }, { onMain(v) }, tag = "voce-${v.key}")
        }
    }
}
