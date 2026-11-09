package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import it.pixelbox.cmwatch.rules.ItemRows
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
import androidx.compose.material.icons.rounded.NotificationsActive
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
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
internal fun SuggestionsCard(v: Suggestions.Visible, onAnswer: (Suggestion, String, String?) -> Unit) {
    // «Altre risposte» (app-home-riordino-29-10): Parliamone, Non ora e Mai dietro un tocco; prima erano dodici tasti in tre voci.
    var more by remember { mutableStateOf(setOf<String>()) }
    // Nello stile «Studio e Diretta» (30/10): una lista a filo col binario; il tipo è l'icona grigia a sinistra e la parola sopra il testo.
    // Voce 12: la voce esce nel verso della scelta, la successiva sale con la molla. Il tocco vale subito; se la voce resta, torna.
    val off = animationsOff()
    val view = androidx.compose.ui.platform.LocalView.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val leaving = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val answer = { s: Suggestion, action: String, opt: String? ->
        if (action == "accept" || action == "choose") view.confirmHaptic()
        leaving[s.id] = action
        onAnswer(s, action, opt)
        scope.launch { kotlinx.coroutines.delay(3_000); leaving.remove(s.id) }
        Unit
    }
    // Le voci come righe (app-richiede-te-righe-02-11): pallino ambra, la prima frase, a destra il tipo, sotto il resto; il tocco
    // apre il testo intero coi tasti. Una voce risposta esce subito; se il relay la tiene, dopo 3 secondi torna.
    val shown = v.shown.filter { it.id !in leaving }
    ItemList(shown.map { ItemRows.suggestion(it) }, "suggerimento", right = { r -> itemRight(r) { k -> stringResource(sugKindLabel(k)) } }) { r ->
        val s = shown.firstOrNull { it.id == r.key } ?: return@ItemList
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            ItemRows.split(s.text).second?.let { Text(it, style = V3.Body) }
            FlowRow(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Contratto 1.132: la voce di un monitoraggio porta il link della sua scheda, che l'app apre dentro (app-monitoraggi-09-11).
                s.link?.let { l -> val uri = androidx.compose.ui.platform.LocalUriHandler.current; SugText(stringResource(R.string.sug_open_chart)) { uri.openUri(l) } }
                if (s.id in more) {
                    SugText(stringResource(R.string.sug_discuss)) { answer(s, "discuss", null) }
                    SugText(stringResource(R.string.sug_later)) { answer(s, "later", null) }
                    SugText(stringResource(R.string.sug_never)) { answer(s, "never", null) }
                } else SugText(stringResource(R.string.sug_more)) { more = more + s.id }
                val opts = Suggestions.options(s)
                if (opts.isEmpty()) V3Pill(stringResource(R.string.sug_accept)) { answer(s, "accept", null) }
                else opts.forEach { o -> V3Pill(o) { answer(s, "choose", o) } }
            }
        }
    }
}

@Composable
private fun SugText(text: String, onClick: () -> Unit) {
    val press = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Surface(onClick = onClick, interactionSource = press, modifier = Modifier.pressScale(press), shape = CircleShape, color = Color.Transparent, contentColor = V3.Tx2) {
        Text(text, style = V3.Name.copy(fontSize = 15.sp, color = V3.Tx2), maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp))
    }
}

/** L'etichetta del tipo, la stessa della card; un tipo che quest'app non conosce è neutro. */
internal fun sugKindLabel(kind: String): Int = when (kind) {
    "recurring" -> R.string.sug_kind_recurring
    "cleanup" -> R.string.sug_kind_cleanup
    "deadline" -> R.string.sug_kind_deadline
    "quota" -> R.string.sug_kind_quota
    "choice" -> R.string.sug_kind_choice
    "followup" -> R.string.sug_kind_followup
    "unsaved" -> R.string.sug_kind_unsaved
    "waiting" -> R.string.sug_kind_waiting
    "idea" -> R.string.sug_kind_idea
    "proposal" -> R.string.sug_kind_proposal
    "observe" -> R.string.sug_kind_observe
    "reminder" -> R.string.sug_kind_reminder
    else -> R.string.sug_kind_other
}
