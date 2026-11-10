package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.*
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ItemRows

// Le voci dentro le sezioni come righe di sessione (app-richiede-te-righe-02-11, mockup richiede-te.png punto 4): pallino, nome,
// dato breve a destra, sotto cosa c'è; il tocco apre sul posto la scheda coi tasti di sempre. Il contenuto della riga sta in ItemRows.

internal fun itemTone(t: ItemRows.Tone): Color = when (t) {
    ItemRows.Tone.WAIT -> V3.Wait
    ItemRows.Tone.BUSY -> V3.Busy
    ItemRows.Tone.GOOD -> V3.Good
    ItemRows.Tone.RED -> V3.Red
    ItemRows.Tone.STILL -> V3.Tx3
}

/** L'icona di ogni tipo di suggerimento, al posto del pallino (Franz 03/11 10:06, app-suggerimenti-icone-03-11); le stesse in sugIcons.ts. */
internal fun sugKindIcon(kind: String): androidx.compose.ui.graphics.vector.ImageVector = with(androidx.compose.material.icons.Icons.Outlined) {
    when (kind) {
        "unsaved" -> Save; "followup" -> Update; "observe" -> Extension; "choice" -> AltRoute; "idea" -> Lightbulb; "proposal" -> EditNote
        "reminder" -> Notifications; "recurring" -> Repeat; "cleanup" -> CleaningServices; "deadline" -> Event; "quota" -> DataUsage
        "waiting" -> HourglassEmpty; else -> AutoAwesome
    }
}

/** Il dato a destra in parole, le parti separate da « · ». `kindLabel`: il nome del tipo di un suggerimento. */
@Composable
internal fun itemRight(r: ItemRows.Row, kindLabel: @Composable (String) -> String = { it }): String = r.right.mapNotNull { p ->
    when (p.kind) {
        ItemRows.Kind.HERE -> stringResource(R.string.decisions_here)
        ItemRows.Kind.AT_PC -> stringResource(R.string.decisions_at_pc)
        ItemRows.Kind.UNBLOCKS -> stringResource(R.string.decisions_unblocks, p.n)
        ItemRows.Kind.BUILD, ItemRows.Kind.PRODUCT, ItemRows.Kind.CHECKED -> p.text
        ItemRows.Kind.TO_INSTALL -> stringResource(R.string.item_to_install)
        ItemRows.Kind.REPORTED -> stringResource(R.string.item_reported)
        ItemRows.Kind.RED -> stringResource(R.string.item_red)
        ItemRows.Kind.AT_TIME -> p.text?.toLongOrNull()?.let { t -> it.pixelbox.cmwatch.rules.Seguiti.whenText(t) }
        ItemRows.Kind.UNTIL -> stringResource(R.string.item_until)
        ItemRows.Kind.FILLED -> p.n.takeIf { it > 0 }?.toString()
        ItemRows.Kind.EMPTY -> stringResource(R.string.item_empty)
        ItemRows.Kind.KIND -> p.text?.let { k -> kindLabel(k) }
    }?.takeIf { it.isNotBlank() }
}.joinToString(" · ")

/** Una riga: pallino nel binario, nome e dato a destra sulla stessa riga, sotto al più due righe di cosa c'è; aperta, la scheda. */
@Composable
internal fun ItemRowView(r: ItemRows.Row, right: String, first: Boolean, open: Boolean, name: String = r.name, tag: String? = null, onClick: () -> Unit, card: @Composable () -> Unit) {
    if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
    var m = Modifier.fillMaxWidth().heightIn(min = 52.dp).handCursor().clickable(onClick = onClick)
    if (tag != null) m = m.testTag(tag)
    // Nella sottoscheda (B «Bordo leggero», app-superfici-plancia-07-11): meno margine, il pallino accanto al titolo e il dato nella
    // riga sotto il titolo, mai a destra su una riga sua.
    val sub = LocalSubCard.current
    Row(m.padding(start = if (sub) 11.dp else 0.dp, top = if (sub) 9.dp else 10.dp, bottom = if (sub) 9.dp else 10.dp, end = if (sub) 11.dp else 12.dp), verticalAlignment = Alignment.Top) {
        // Il segno al centro della riga del titolo (20 sp come V3.Name); per un suggerimento l'icona del suo tipo (app-suggerimenti-icone-03-11).
        Box(Modifier.width(if (sub) 18.dp else V3.Rail).height(with(androidx.compose.ui.platform.LocalDensity.current) { V3.Name.lineHeight.toDp() }), contentAlignment = Alignment.Center) {
            val icon = r.icon
            if (icon != null) androidx.compose.material3.Icon(sugKindIcon(icon), null, tint = itemTone(r.tone), modifier = Modifier.capAlign(V3.Name).size(18.dp))
            else Box(Modifier.capAlign(V3.Name).size(8.dp).clip(CircleShape).background(itemTone(r.tone)))
        }
        Spacer(Modifier.width(if (sub) 6.dp else 8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (sub) {
                Text(name, style = V3.Name)
                if (right.isNotEmpty()) Text(right, style = V3.Meter)
            } else
            // Nome e dato: il dato a destra se ci sta, altrimenti sotto il nome, mai in colonna (V3Split).
            V3Split(text = { Text(name, style = V3.Name) }) { if (right.isNotEmpty()) Text(right, style = V3.Meter, textAlign = TextAlign.End) }
            // Due righe al più, tagliate senza «…» (regole di forma, Franz 12/09): il testo intero sta nella scheda.
            if (!open) r.sub?.let { Text(it, style = V3.Small, maxLines = 2, overflow = TextOverflow.Clip) }
        }
    }
    FoldIn(open) { Column(Modifier.fillMaxWidth().padding(start = if (sub) 11.dp else 12.dp, end = if (sub) 11.dp else 12.dp, bottom = if (sub) 11.dp else 12.dp)) { card() } }
}

/**
 * Le righe di una sezione in una lista a filo: le prime tre e «altre N» (ItemRows.shown); una sola aperta alla volta. `card`: la
 * scheda della riga col tasti di sempre; `name`: il nome tradotto quando la riga lo dà come chiave (Prima raccolta).
 */
@Composable
internal fun ItemList(
    rows: List<ItemRows.Row>, tag: String, right: @Composable (ItemRows.Row) -> String = { itemRight(it) }, name: @Composable (ItemRows.Row) -> String = { it.name },
    /** Le righe confermate che escono (app-effetti-movimenti-04-11, punto 4): restano nella lista e se ne vanno con confirmedExit. */
    gone: Set<String> = emptySet(),
    /** Quante cose dicono le righe nascoste in «altre N»: di solito le righe, per Decidi le decisioni dei gruppi (web-plancia-j2-ripasso-06-11). */
    more: (List<ItemRows.Row>) -> Int = { it.size },
    /** Contratto 1.130 (app-scheda-ti-aspetta-07-11): true se il tocco ha aperto la scheda «Ti aspetta» della riga, che allora non si apre sul posto. */
    onOpen: ((ItemRows.Row) -> Boolean)? = null,
    /**
     * La riga da mostrare subito (app-prova-link-diretta-10-11): il link «prova #N» apre Verifica con la scheda di quella prova aperta,
     * portata in vista ed evidenziata per un momento; se stava fra le «altre N», la lista si apre tutta.
     */
    focus: String? = null,
    card: @Composable (ItemRows.Row) -> Unit,
) {
    if (rows.none { it.key !in gone }) return
    val off = animationsOff()
    val focused = focus?.takeIf { f -> rows.any { it.key == f && it.key !in gone } }
    // Con una riga cercata la lista nasce con quella aperta e, se stava fra le «altre N», tutta aperta.
    var openKey by rememberSaveable(tag, focused) { mutableStateOf(focused) }
    var all by rememberSaveable(tag, focused) { mutableStateOf(rows.filter { it.key !in gone }.let { l -> focused != null && l.indexOfFirst { it.key == focused } >= ItemRows.shown(l.size, false) }) }
    val live = rows.count { it.key !in gone }
    val n = ItemRows.shown(live, all)
    // Nella sottoscheda ogni riga è un riquadro suo, uno sotto l'altro (B «Bordo leggero», app-superfici-plancia-07-11).
    val sub = LocalSubCard.current
    val wrap: @Composable (@Composable () -> Unit) -> Unit = if (sub) ({ c -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { c() } }) else ({ c -> V3List(c) })
    wrap {
        rows.take(n + rows.count { it.key in gone }).forEachIndexed { i, r ->
            androidx.compose.runtime.key(r.key) {
                // Divisore e riga in una colonna: dentro AnimatedVisibility, che li sovrapporrebbe come un Box, la riga perdeva il divisore
                // (1 px più in alto da lì in giù nei golden di 9622c048).
                androidx.compose.animation.AnimatedVisibility(r.key !in gone, enter = androidx.compose.animation.EnterTransition.None, exit = confirmedExit(off)) {
                    val row: @Composable () -> Unit = { Column(if (r.key == focused) Modifier.fillMaxWidth().focusFlash(off) else Modifier.fillMaxWidth()) {
                        ItemRowView(r, right(r), i == 0 || sub, openKey == r.key, name(r), "$tag-${r.key}", onClick = { if (onOpen?.invoke(r) != true) openKey = if (openKey == r.key) null else r.key }) { card(r) }
                    } }
                    if (sub) V3List(row) else row()
                }
            }
        }
        if (n < live) {
            if (!sub) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).handCursor().clickable { all = true }.testTag("$tag-altre"), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.item_more, more(rows.filter { it.key !in gone }.drop(n))), style = V3.Small.copy(color = V3.Tx2))
            }
        }
    }
}

/**
 * La riga cercata da un link (app-prova-link-diretta-10-11): appena aperta la pagina va in vista, centrata se c'è posto, e si accende
 * del colore d'attesa che si spegne in poco più di un secondo. Senza animazioni resta solo lo scorrimento.
 */
@Composable
private fun Modifier.focusFlash(off: Boolean): Modifier {
    val req = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val glow = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (off) 0f else 1f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // La scheda si apre con FoldIn: si aspetta che abbia la sua altezza, poi la si porta in vista.
        kotlinx.coroutines.delay(if (off) 0 else 260)
        req.bringIntoView()
        if (!off) glow.animateTo(0f, androidx.compose.animation.core.tween(1400, delayMillis = 500))
    }
    val tone = Cm.waiting
    return this.bringIntoViewRequester(req).drawBehind { if (glow.value > 0f) drawRect(tone.copy(alpha = 0.16f * glow.value)) }
}
