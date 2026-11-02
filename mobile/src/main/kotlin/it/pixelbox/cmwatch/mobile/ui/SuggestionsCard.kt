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
    val seen = androidx.compose.runtime.remember { mutableSetOf<String>() }
    val answer = { s: Suggestion, action: String, opt: String? ->
        if (action == "accept" || action == "choose") view.confirmHaptic()
        if (off) onAnswer(s, action, opt)
        else {
            leaving[s.id] = action
            scope.launch { kotlinx.coroutines.delay(160); onAnswer(s, action, opt); kotlinx.coroutines.delay(3_000); leaving.remove(s.id) }
        }
        Unit
    }
    Column(Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).v3Edge()) {
        v.shown.forEachIndexed { i, s -> androidx.compose.runtime.key(s.id) {
            val isNew = seen.isNotEmpty() && s.id !in seen
            androidx.compose.runtime.SideEffect { seen += s.id }
            val rise = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (isNew && !off) 0.95f else 1f) }
            androidx.compose.runtime.LaunchedEffect(Unit) { rise.animateTo(1f, CmMotion.bounceFloat) }
            val how = leaving[s.id]
            val out by androidx.compose.animation.core.animateFloatAsState(if (how != null) 1f else 0f, androidx.compose.animation.core.tween(160, easing = CmMotion.easing), label = "leave")
            val down = how == "later" || how == "never"
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(V3.Line))
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
                // Contratto 1.102: un promemoria scattato; il suo prompt arriva alla master solo con Accetta.
                "reminder" -> Icons.Rounded.NotificationsActive to R.string.sug_kind_reminder
                // Un tipo che quest'app non conosce: neutro, non più «Quota» (documento suggerimenti-coda, §8.8).
                else -> Icons.Rounded.AutoAwesome to R.string.sug_kind_other
            }
            Row(Modifier.graphicsLayer {
                scaleX = rise.value; scaleY = rise.value; alpha = 1f - out
                if (down) translationY = out * size.height * 0.5f else translationX = out * size.width * 0.25f
            }.padding(top = 12.dp, end = 12.dp, bottom = 12.dp)) {
                Box(Modifier.width(V3.Rail).padding(top = 2.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = V3.Tx2, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(label), style = V3.Small.copy(fontSize = 13.sp, color = V3.Tx3))
                    Text(s.text, style = V3.Body)
                    FlowRow(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        } }
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
