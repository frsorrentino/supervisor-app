package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Subagents
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SubagentRules
import kotlinx.coroutines.launch

/**
 * app-sottoagenti-23-11 (variante A del mockup, Franz 23/11 13:26 «procedi con la consigliata»): i sottoagenti di una sessione come
 * righe sotto la sessione, in «Al lavoro adesso». La sezione nasce chiusa e si apre col toggle delle altre (la freccia che gira);
 * chiusa dice «2 al lavoro, 5 finiti». Aperta: chi lavora col pallino blu, i finiti con la spunta, cosa fa, tipo e modello, da quanto
 * o quanto ha lavorato; «Tutti e N» chiede al PC l'elenco intero (op `subagents`, fino a 30). Contratto 1.158; come SubagentsFold.svelte.
 */
@Composable
internal fun SubagentsFold(subs: Subagents, now: Long, onAll: (suspend () -> Subagents?)?, modifier: Modifier = Modifier, startOpen: Boolean = false) {
    var open by remember { mutableStateOf(startOpen) }
    var full by remember { mutableStateOf<Subagents?>(null) }
    var asking by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val en = LocalConfiguration.current.locales[0].language == "en"
    val sum = SubagentRules.summary(subs)
    val running = if (sum.running > 0) pluralStringResource(R.plurals.subagents_running, sum.running, sum.running) else null
    val done = if (sum.done > 0) pluralStringResource(R.plurals.subagents_done, sum.done, sum.done) else null
    Column(modifier.fillMaxWidth().padding(start = 40.dp, bottom = 8.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Cm.line))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).handCursor().clickable(role = Role.Button, onClick = { open = !open }),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Cm.text, fontWeight = FontWeight.Medium)) { append(stringResource(R.string.subagents_title)) }
                append(" · ")
                running?.let { withStyle(SpanStyle(color = Cm.busy, fontWeight = FontWeight.SemiBold)) { append(it) }; if (done != null) append(", ") }
                done?.let { append(it) }
            }, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp), color = Cm.text2, modifier = Modifier.weight(1f))
            TurnArrow(open, Cm.text2, Modifier.size(20.dp))
        }
        FoldIn(open) {
            Column {
                SubagentRules.rows(full ?: subs, now).forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.width(16.dp).padding(top = 3.dp), contentAlignment = Alignment.Center) {
                            if (r.running) Box(Modifier.size(14.dp).clip(CircleShape).background(Cm.busy.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) { Box(Modifier.size(8.dp).clip(CircleShape).background(Cm.busy)) }
                            else Icon(Icons.Rounded.Check, null, tint = Cm.idle, modifier = Modifier.size(16.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.bodyMedium, color = Cm.text)
                            val meta = listOfNotNull(
                                r.type, r.model, if (r.bg) stringResource(R.string.subagents_bg) else null, r.phase,
                                r.tokens?.let { (i, o) -> stringResource(R.string.subagents_tokens, SubagentRules.tokens(i, en), SubagentRules.tokens(o, en)) },
                            ).joinToString(" · ")
                            Text(meta, style = MonoSmall.copy(fontSize = 12.sp))
                        }
                        r.seconds?.let { Text(SubagentRules.duration(it), style = MonoSmall.copy(fontSize = 12.sp), maxLines = 1) }
                    }
                }
                val more = if (full == null) SubagentRules.more(subs) else null
                if (more != null && onAll != null) {
                    val label = when {
                        asking -> stringResource(R.string.subagents_loading)
                        failed -> stringResource(R.string.subagents_failed)
                        else -> pluralStringResource(R.plurals.subagents_all, more, more)
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium), color = if (asking) Cm.text2 else Cm.busy,
                        modifier = Modifier.heightIn(min = 40.dp).handCursor().clickable(enabled = !asking, role = Role.Button) {
                            asking = true; failed = false
                            scope.launch { val r = runCatching { onAll() }.getOrNull(); asking = false; if (r != null) full = r else failed = true }
                        }.wrapContentHeight(Alignment.CenterVertically))
                } else if (full != null) {
                    Text(stringResource(R.string.subagents_fewer), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium), color = Cm.busy,
                        modifier = Modifier.heightIn(min = 40.dp).handCursor().clickable(role = Role.Button) { full = null }.wrapContentHeight(Alignment.CenterVertically))
                }
            }
        }
    }
}
