package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip

// La home per categorie (Franz 02/11 16:44, compito app-home-categorie-02-11): l'etichetta di una categoria aperta, la riga di una
// chiusa col suo segnale, e la card di una chiusa aperta sul posto. L'ordine e le chiavi stanno in HomeCats (core).

/** Un pezzo del segnale: `tone` null = grigio, altrimenti il colore (rosso per un fallito, ambra per quel che chiede attenzione). */
internal class CatBit(val text: String, val tone: Color? = null)

internal fun catIcon(id: String): ImageVector = when (id) {
    // Tocca a te (app-home-nomi-02-11): la cartellina con l'orologio, non più la mano.
    "you" -> Icons.Outlined.PendingActions
    "work" -> Icons.Outlined.PlayCircle
    "out" -> Icons.Outlined.Inbox
    else -> Icons.Outlined.Speed
}

@Composable
internal fun catName(id: String): String = stringResource(when (id) {
    "you" -> R.string.cat_you
    "work" -> R.string.cat_work
    "out" -> R.string.cat_out
    else -> R.string.cat_tools
})

/** Il fondo della card di una categoria chiusa aperta sul posto: la testata, le sezioni e la chiusura lo condividono. */
internal val CatPeekTop = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
internal val CatPeekEnd = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)

/**
 * Categoria aperta: il nome in maiuscoletto sopra le sue sezioni, con il filo che la separa dalla precedente. Il tocco la chiude e la
 * scelta resta (app-home-in-cima-02-11): la freccia a destra lo dice.
 */
@Composable
internal fun CatLabel(id: String, modifier: Modifier = Modifier, onToggle: () -> Unit = {}) {
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().testTag("home-cat-$id").then(modifier).handCursor().clickable(onClick = onToggle)) {
        HorizontalDivider(Modifier.padding(top = 12.dp), thickness = 1.dp, color = V3.Line)
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(catName(id).uppercase(), style = V3.Small.copy(fontSize = 13.sp, letterSpacing = 1.2.sp, color = V3.Tx3), modifier = Modifier.weight(1f).padding(top = 8.dp, bottom = 2.dp))
            TurnArrow(true, V3.Tx3, Modifier.size(20.dp))
        }
    }
}

/**
 * I due gruppi dentro Sessioni (Franz 02/11 23:57: «aspettano te sia un sottoinsieme di sessioni»): «Ti aspettano N» in ambra sopra
 * le sessioni che aspettano Franz, «Le altre» in grigio sopra il resto. Piccolo, come l'etichetta di una categoria.
 */
@Composable
internal fun WaitGroupLabel(text: String, n: Int?) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 2.dp).testTag(if (n != null) "home-sessioni-aspettano" else "home-sessioni-altre"), verticalAlignment = Alignment.CenterVertically) {
        val tone = if (n != null) V3.Wait else V3.Tx3
        Text(text.uppercase(), style = V3.Small.copy(fontSize = 12.sp, letterSpacing = 1.1.sp, color = tone))
        n?.let { Text(it.toString(), style = V3.Small.copy(fontSize = 12.sp, color = tone), modifier = Modifier.padding(start = 6.dp)) }
    }
}

/** L'etichetta delle voci sul tabellone (app-tabellone-quattro-02-11, mockup richiede-te.png punto 2): la puntina e «In cima». */
@Composable
internal fun BoardLabel() {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp, bottom = 2.dp).testTag("home-tabellone-voci"), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.PushPin, null, tint = V3.Tx3, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.home_board_label).uppercase(), style = V3.Small.copy(fontSize = 13.sp, letterSpacing = 1.2.sp, color = V3.Tx3))
    }
}

/**
 * Il bordo dello spazio in cima (app-home-in-cima-02-11). Fermo non si vede; durante il trascinamento di una sezione è la striscia
 * tratteggiata dove lasciarla, accesa quando la voce presa sta già sopra.
 */
@Composable
internal fun TopEdge(dragging: Boolean, hot: Boolean, modifier: Modifier = Modifier) {
    if (!dragging) { Box(modifier.fillMaxWidth().height(1.dp)); return }
    Box(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp).heightIn(min = 48.dp).clip(V3.Shape).background(V3.Bg.copy(alpha = .72f)).drawDashedEdge(hot).testTag(if (hot) "home-cima-acceso" else "home-cima"), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.home_top_drop), style = V3.Name.copy(fontSize = 15.sp, color = V3.Tx), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/**
 * Categoria chiusa: una riga con a destra il segnale; in ambra quando c'è qualcosa (Franz: «un segnale, non uno spostamento»).
 * `open`: aperta sul posto, la riga diventa la testata della card.
 */
@Composable
internal fun CatRow(id: String, bits: List<CatBit>, amber: Boolean, open: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val shape = if (open) CatPeekTop else V3.Shape
    var m = Modifier.fillMaxWidth().padding(top = 6.dp)
    m = when {
        open -> m.background(V3.S2, shape)
        amber -> m.background(V3.Wait.copy(alpha = 0.14f), shape)
        else -> m
    }
    Row(m.then(modifier).handCursor().clickable(onClick = onToggle).testTag("home-cat-$id").heightIn(min = 52.dp).padding(start = 0.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(V3.Rail), contentAlignment = Alignment.Center) { Icon(catIcon(id), null, tint = V3.Tx2, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(8.dp))
        val text = buildAnnotatedString {
            bits.forEachIndexed { i, b ->
                if (i > 0) append(" · ")
                withStyle(SpanStyle(color = b.tone ?: (if (amber) V3.Wait else V3.Tx2))) { append(b.text) }
            }
        }
        CatTitleLine(Modifier.weight(1f)) {
            // A capo se non ci sta (fontScale 1.3 a 320 dp), mai tagliato.
            Text(catName(id), style = V3.Section)
            Text(text, style = V3.Small, textAlign = TextAlign.End)
        }
        TurnArrow(open, V3.Tx3, Modifier.padding(start = 6.dp).size(24.dp))
    }
}

/**
 * Nome e segnale: sulla stessa riga se ci stanno, il segnale a destra; altrimenti il segnale va intero sulla riga sotto, sotto il
 * nome, mai in colonna (regole di larghezza, Franz 12/09).
 */
@Composable
private fun CatTitleLine(modifier: Modifier, content: @Composable () -> Unit) = androidx.compose.ui.layout.Layout(content, modifier) { ms, c ->
    val gap = 12.dp.roundToPx()
    val name = ms[0].measure(c.copy(minWidth = 0))
    val want = ms[1].maxIntrinsicWidth(c.maxHeight)
    if (name.width + gap + want <= c.maxWidth) {
        val sig = ms[1].measure(c.copy(minWidth = 0, maxWidth = want))
        val h = maxOf(name.height, sig.height)
        layout(c.maxWidth, h) { name.place(0, (h - name.height) / 2); sig.place(c.maxWidth - sig.width, (h - sig.height) / 2) }
    } else {
        val sig = ms[1].measure(c.copy(minWidth = 0))
        layout(c.maxWidth, name.height + sig.height) { name.place(0, 0); sig.place(0, name.height) }
    }
}

/** La chiusura della card aperta sul posto. */
@Composable
internal fun CatPeekEnd() = Box(Modifier.fillMaxWidth().height(10.dp).background(V3.S2, CatPeekEnd))

/** TalkBack sulla testata di una categoria della home (app-categorie-trascina-02-11): «Sposta su» e «Sposta giù» come in Riordina. */
@Composable
internal fun Modifier.catMoveActions(name: String, i: Int, size: Int, onMove: (Int, Int) -> Unit): Modifier {
    val up = stringResource(R.string.reorder_up, name)
    val down = stringResource(R.string.reorder_down, name)
    val move by androidx.compose.runtime.rememberUpdatedState(onMove)
    return this.then(Modifier.semantics {
        customActions = listOfNotNull(
            if (i > 0) CustomAccessibilityAction(up) { move(i, i - 1); true } else null,
            if (i < size - 1) CustomAccessibilityAction(down) { move(i, i + 1); true } else null,
        )
    })
}
