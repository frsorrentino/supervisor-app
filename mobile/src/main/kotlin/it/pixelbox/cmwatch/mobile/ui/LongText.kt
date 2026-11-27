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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Animazioni

/**
 * app-play-lettura-ovunque-27-11 (Franz 27/11 13:29): il ▶ della lettura, uno solo in tutta l'app, quello della chat: tondo da 36 dp, la
 * stessa icona, «Leggi ad alta voce», ■ mentre legge. Una lettura alla volta (la voce ferma quella di prima); la pillola con velocità e
 * barra dei paragrafi è quella di sempre, in fondo allo schermo. [onClick]: il tocco di chi chiama, quando fa di più che leggere (il
 * resoconto della notte legge e segna letto); senza, legge [text] con [LocalSpeakText], e senza la voce il tasto non c'è.
 */
@Composable
internal fun ReadButton(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val speak = LocalSpeakText.current
    val tap: () -> Unit = onClick ?: speak?.let { s -> { s(text) } } ?: return
    val reading = LocalSpeaking.current == text
    FilledTonalIconButton(onClick = tap, modifier = modifier.size(36.dp).testTag("leggi")) {
        Icon(
            if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(if (reading) R.string.stop_reading else R.string.read_aloud),
            tint = Cm.actionIcon, modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Un testo con accanto il ▶ ([ReadButton]) quando supera una riga, o è tagliato ed è più lungo di quello che mostra
 * (app-play-lettura-ovunque-27-11, Franz 27/11: ovunque, solo oltre una riga, accanto a ogni testo lungo anche nelle carte; prima oltre due
 * righe nelle pagine nuove, app-tasto-play-03-11). Mentre legge, le parole dette si accendono ([ReadAlongText]). Diventato lungo resta lungo
 * finché il testo non cambia, così il tasto non va e viene col margine che prende. [annotated]: lo stesso testo con segni o link, disegnato
 * al posto di [text] (che resta il testo letto).
 */
@Composable
internal fun LongText(
    text: String, modifier: Modifier = Modifier, style: TextStyle = TextStyle.Default, color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE, onTextLayout: ((TextLayoutResult) -> Unit)? = null, annotated: AnnotatedString? = null,
) {
    var long by remember(text) { mutableStateOf(false) }
    val reading = LocalSpeaking.current == text
    val word = LocalSpeakingWord.current
    val block = LocalSpeakingBlock.current
    val blocksOf = LocalBlocksOf.current
    Row(modifier, verticalAlignment = Alignment.Top) {
        if (reading && word != null && block != null) {
            // La parola detta nel testo intero: le parole dei paragrafi letti prima più quella del paragrafo di adesso.
            val before = remember(text, block) { blocksOf(text).take(block).sumOf { b -> Animazioni.wordCount(b.text) } }
            ReadAlongText(annotated ?: AnnotatedString(text), before + word, style, color, Modifier.weight(1f))
        } else Text(
            annotated ?: AnnotatedString(text), Modifier.weight(1f), style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Clip,
            onTextLayout = { r -> if (!long && it.pixelbox.cmwatch.rules.ReadRules.isLong(r.lineCount, r.hasVisualOverflow)) long = true; onTextLayout?.invoke(r) },
        )
        if (long) ReadButton(text, Modifier.padding(start = 8.dp))
    }
}

/**
 * app-play-voce-ovunque-27-11 (prova 1400, Franz 27/11: «nella card da cui sto rispondendo non c'è alcun tasto play»): la voce per tutta
 * l'app, intorno a ogni pagina, anche quelle a tutto schermo che Main apre prima della sua impalcatura (Ti aspetta, Da provare, i box).
 * Senza, il ▶ ([ReadButton]) non trova la voce e non si disegna.
 */
@Composable
internal fun ProvideSpeech(
    speaking: String?, block: Int?, word: Int?, rate: Float, blocksOf: (String) -> List<it.pixelbox.cmwatch.rules.AnswerText.Block>,
    speak: (String) -> Unit, setRate: (Float) -> Unit, content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSpeaking provides speaking, LocalSpeakingBlock provides block, LocalSpeakingWord provides word, LocalBlocksOf provides blocksOf,
        LocalSpeakText provides speak, LocalSpeechRate provides rate, LocalSetSpeechRate provides setRate, content = content,
    )
}
