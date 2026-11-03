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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    Row(m.padding(top = 10.dp, bottom = 10.dp, end = 12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.width(V3.Rail).padding(top = 6.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(8.dp).clip(CircleShape).background(itemTone(r.tone))) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Nome e dato: il dato a destra se ci sta, altrimenti sotto il nome, mai in colonna (V3Split).
            V3Split(text = { Text(name, style = V3.Name) }) { if (right.isNotEmpty()) Text(right, style = V3.Meter, textAlign = TextAlign.End) }
            // Due righe al più, tagliate senza «…» (regole di forma, Franz 12/09): il testo intero sta nella scheda.
            if (!open) r.sub?.let { Text(it, style = V3.Small, maxLines = 2, overflow = TextOverflow.Clip) }
        }
    }
    FoldIn(open) { Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) { card() } }
}

/**
 * Le righe di una sezione in una lista a filo: le prime tre e «altre N» (ItemRows.shown); una sola aperta alla volta. `card`: la
 * scheda della riga col tasti di sempre; `name`: il nome tradotto quando la riga lo dà come chiave (Prima raccolta).
 */
@Composable
internal fun ItemList(rows: List<ItemRows.Row>, tag: String, right: @Composable (ItemRows.Row) -> String = { itemRight(it) }, name: @Composable (ItemRows.Row) -> String = { it.name }, card: @Composable (ItemRows.Row) -> Unit) {
    if (rows.isEmpty()) return
    var openKey by rememberSaveable(tag) { mutableStateOf<String?>(null) }
    var all by rememberSaveable(tag) { mutableStateOf(false) }
    val n = ItemRows.shown(rows.size, all)
    V3List {
        rows.take(n).forEachIndexed { i, r ->
            androidx.compose.runtime.key(r.key) {
                ItemRowView(r, right(r), i == 0, openKey == r.key, name(r), "$tag-${r.key}", onClick = { openKey = if (openKey == r.key) null else r.key }) { card(r) }
            }
        }
        if (n < rows.size) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).handCursor().clickable { all = true }.testTag("$tag-altre"), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.item_more, rows.size - n), style = V3.Small.copy(color = V3.Tx2))
            }
        }
    }
}
