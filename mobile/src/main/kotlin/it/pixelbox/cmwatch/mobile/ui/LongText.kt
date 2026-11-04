package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R

/**
 * Un testo delle pagine nuove (Da decidere, Da provare, Da seguire, Prima raccolta, Risultati): oltre due righe ha accanto il
 * tasto ▶ della lettura vocale, ■ mentre legge (Franz 03/11 15:22, app-tasto-play-03-11; LongText.svelte sul web). Diventato
 * lungo resta lungo finché il testo non cambia, così il tasto non va e viene col margine che prende.
 */
@Composable
internal fun LongText(
    text: String, modifier: Modifier = Modifier, style: TextStyle = TextStyle.Default, color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE, onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    var long by remember(text) { mutableStateOf(false) }
    val speak = LocalSpeakText.current
    Row(modifier, verticalAlignment = Alignment.Top) {
        Text(
            text, Modifier.weight(1f), style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Clip,
            onTextLayout = { r -> if (!long && r.lineCount > 2) long = true; onTextLayout?.invoke(r) },
        )
        if (long && speak != null) {
            val reading = LocalSpeaking.current == text
            FilledTonalIconButton(onClick = { speak(text) }, modifier = Modifier.padding(start = 8.dp).size(40.dp).testTag("leggi")) {
                Icon(if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(if (reading) R.string.stop_reading else R.string.dock_listen))
            }
        }
    }
}
