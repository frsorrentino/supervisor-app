package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Suggestion
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Suggestions

/**
 * «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup docs/mockup/2026-10-24-suggerimenti-home; contratto
 * 1.84): la stessa struttura delle altre schede, al massimo tre voci, ognuna col suo simbolo, «Accetta» tonale, «Non ora» e
 * «Mai», più «Parliamone» (Franz 25/10 07:36). Accetta manda il prompt alla master: niente parte senza un tocco.
 * Contratto 1.88: una scelta (`choice`) ha un tasto tonale per opzione al posto di Accetta, come in «Per te».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SuggestionsCard(v: Suggestions.Visible, at: String, onAnswer: (Suggestion, String, String?) -> Unit) {
    // «Altre risposte» (app-home-riordino-29-10): Parliamone, Non ora e Mai dietro un tocco; prima erano dodici tasti in tre voci.
    var more by remember { mutableStateOf(setOf<String>()) }
    // Nello stile «Studio e Diretta» (30/10): una lista a filo col binario; il tipo è l'icona grigia a sinistra e la parola sopra il testo.
    Column(Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1)) {
        Row(Modifier.padding(top = 12.dp, end = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(V3.Rail), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AutoAwesome, null, tint = V3.Tx2, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sug_title), style = V3.Name, modifier = Modifier.weight(1f))
            Text("$at · " + stringResource(R.string.sug_count, v.shown.size, v.total), style = V3.Meter, maxLines = 1)
        }
        v.shown.forEach { s ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
            val (icon, label) = when (s.kind) {
                "recurring" -> Icons.Rounded.Repeat to R.string.sug_kind_recurring
                "cleanup" -> Icons.Rounded.DeleteOutline to R.string.sug_kind_cleanup
                "deadline" -> Icons.Rounded.Schedule to R.string.sug_kind_deadline
                "quota" -> Icons.Rounded.DataUsage to R.string.sug_kind_quota
                "choice" -> Icons.Rounded.AltRoute to R.string.sug_kind_choice
                "followup" -> Icons.AutoMirrored.Rounded.Redo to R.string.sug_kind_followup
                "unsaved" -> Icons.Rounded.SaveAs to R.string.sug_kind_unsaved
                "waiting" -> Icons.Rounded.HourglassEmpty to R.string.sug_kind_waiting
                "idea" -> Icons.Rounded.Lightbulb to R.string.sug_kind_idea
                "proposal" -> Icons.Rounded.TipsAndUpdates to R.string.sug_kind_proposal
                // Contratto 1.94: le note di claude-observe di un plugin, da smistare nella sua sessione.
                "observe" -> Icons.Rounded.StickyNote2 to R.string.sug_kind_observe
                // Un tipo che quest'app non conosce: neutro, non più «Quota» (documento suggerimenti-coda, §8.8).
                else -> Icons.Rounded.AutoAwesome to R.string.sug_kind_other
            }
            Row(Modifier.padding(top = 12.dp, end = 12.dp, bottom = 12.dp)) {
                Box(Modifier.width(V3.Rail).padding(top = 2.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(label), style = V3.Small.copy(fontSize = 13.sp, color = V3.Tx3))
                    Text(s.text, style = V3.Body)
                    FlowRow(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (s.id in more) {
                            SugText(stringResource(R.string.sug_discuss)) { onAnswer(s, "discuss", null) }
                            SugText(stringResource(R.string.sug_later)) { onAnswer(s, "later", null) }
                            SugText(stringResource(R.string.sug_never)) { onAnswer(s, "never", null) }
                        } else SugText(stringResource(R.string.sug_more)) { more = more + s.id }
                        val opts = Suggestions.options(s)
                        if (opts.isEmpty()) V3Pill(stringResource(R.string.sug_accept)) { onAnswer(s, "accept", null) }
                        else opts.forEach { o -> V3Pill(o) { onAnswer(s, "choose", o) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SugText(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.Transparent, contentColor = V3.Tx2) {
        Text(text, style = V3.Name.copy(fontSize = 15.sp, color = V3.Tx2), maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp))
    }
}
