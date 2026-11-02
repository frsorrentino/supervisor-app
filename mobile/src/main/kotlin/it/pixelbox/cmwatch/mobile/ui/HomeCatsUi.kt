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
import androidx.compose.material.icons.outlined.PanTool
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

// La home per categorie (Franz 02/11 16:44, compito app-home-categorie-02-11): l'etichetta di una categoria aperta, la riga di una
// chiusa col suo segnale, e la card di una chiusa aperta sul posto. L'ordine e le chiavi stanno in HomeCats (core).

/** Un pezzo del segnale: `tone` null = grigio, altrimenti il colore (rosso per un fallito, ambra per quel che chiede attenzione). */
internal class CatBit(val text: String, val tone: Color? = null)

internal fun catIcon(id: String): ImageVector = when (id) {
    "you" -> Icons.Outlined.PanTool
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

/** Categoria aperta: il nome in maiuscoletto sopra le sue sezioni, con il filo che la separa dalla precedente. */
@Composable
internal fun CatLabel(id: String) {
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().testTag("home-cat-$id")) {
        HorizontalDivider(Modifier.padding(top = 12.dp), thickness = 1.dp, color = V3.Line)
        Text(catName(id).uppercase(), style = V3.Small.copy(fontSize = 13.sp, letterSpacing = 1.2.sp, color = V3.Tx3), modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp))
    }
}

/**
 * Categoria chiusa: una riga con a destra il segnale; in ambra quando c'è qualcosa (Franz: «un segnale, non uno spostamento»).
 * `open`: aperta sul posto, la riga diventa la testata della card.
 */
@Composable
internal fun CatRow(id: String, bits: List<CatBit>, amber: Boolean, open: Boolean, onToggle: () -> Unit) {
    val shape = if (open) CatPeekTop else V3.Shape
    var m = Modifier.fillMaxWidth().padding(top = 6.dp)
    m = when {
        open -> m.background(V3.S2, shape)
        amber -> m.background(V3.Wait.copy(alpha = 0.14f), shape)
        else -> m
    }
    Row(m.handCursor().clickable(onClick = onToggle).testTag("home-cat-$id").heightIn(min = 52.dp).padding(start = 0.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
