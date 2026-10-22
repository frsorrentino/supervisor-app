package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Tonality
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import it.pixelbox.cmwatch.rules.NightGantt
import it.pixelbox.cmwatch.rules.NightPage

/**
 * Le icone di stato al posto dei glifi ✓ ▶ ◐ ✗ ❓ ○ nei testi (Franz, 22/10 13:36: niente emoji nell'app, solo icone
 * vettoriali): stessi significati di prima, Material arrotondate, nel colore del testo accanto.
 */
object StateIcons {
    val done: ImageVector get() = Icons.Rounded.Check
    val running: ImageVector get() = Icons.Rounded.PlayArrow
    val partial: ImageVector get() = Icons.Rounded.Tonality
    val stopped: ImageVector get() = Icons.Rounded.Close
    val question: ImageVector get() = Icons.Rounded.QuestionMark
    val todo: ImageVector get() = Icons.Rounded.RadioButtonUnchecked

    fun of(i: NightPage.Icon): ImageVector = when (i) {
        NightPage.Icon.OK -> done; NightPage.Icon.RUNNING -> running; NightPage.Icon.PARTIAL -> partial
        NightPage.Icon.STOPPED -> stopped; NightPage.Icon.QUESTION -> question
    }

    fun of(w: NightGantt.Where): ImageVector = when (w) {
        NightGantt.Where.DONE -> done; NightGantt.Where.RUNNING -> running; NightGantt.Where.PARTIAL -> partial
        NightGantt.Where.STOPPED -> stopped; NightGantt.Where.UNKNOWN -> todo
    }

    fun of(g: NightGantt.GoalState): ImageVector = when (g) {
        NightGantt.GoalState.DONE -> done; NightGantt.GoalState.RUNNING -> running; NightGantt.GoalState.PARTIAL -> partial
        NightGantt.GoalState.STOPPED -> stopped; NightGantt.GoalState.TODO -> todo
    }
}

/** Un'icona di stato piccola, nel colore del testo accanto. */
@Composable
fun StateMark(icon: ImageVector, tint: Color, size: Dp = 14.dp, modifier: Modifier = Modifier) = Icon(icon, null, tint = tint, modifier = modifier.size(size))

/**
 * Icona e testo sulla stessa riga, dove prima il glifo stava in testa al testo. Con un testo su più righe l'icona sta
 * sulla prima riga, centrata sulla sua altezza (come stava il glifo), non a metà del blocco.
 */
@Composable
fun MarkedText(
    icon: ImageVector, text: String, tint: Color, style: TextStyle, modifier: Modifier = Modifier, size: Dp = 14.dp,
    maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip,
) {
    val line = if (style.lineHeight.isSpecified) with(LocalDensity.current) { style.lineHeight.toDp() } else null
    Row(modifier, verticalAlignment = if (line == null) Alignment.CenterVertically else Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StateMark(icon, tint, size, Modifier.padding(top = line?.let { ((it - size) / 2).coerceAtLeast(0.dp) } ?: 0.dp))
        Text(text, style = style, color = tint, maxLines = maxLines, overflow = overflow)
    }
}
