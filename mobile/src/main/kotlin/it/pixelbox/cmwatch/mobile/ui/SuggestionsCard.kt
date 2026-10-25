package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.Suggestion
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Suggestions
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup docs/mockup/2026-10-24-suggerimenti-home; contratto
 * 1.84): la stessa struttura delle altre schede, al massimo tre voci, ognuna col suo simbolo, «Accetta» tonale, «Non ora» e
 * «Mai», più «Parliamone» (Franz 25/10 07:36). Accetta manda il prompt alla master: niente parte senza un tocco.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SuggestionsCard(v: Suggestions.Visible, at: String, onAnswer: (Suggestion, String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(lerp(CmColors.surfaceLow, CmColors.advice, 0.09f)).padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(CmColors.advice), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF0B0D10), modifier = Modifier.size(15.dp))
            }
            Text(stringResource(R.string.sug_title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.weight(1f))
            Text("$at · " + stringResource(R.string.sug_count, v.shown.size, v.total), style = MonoSmall)
        }
        v.shown.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = CmColors.advice.copy(alpha = 0.18f))
            val (icon, tone, label) = when (s.kind) {
                "recurring" -> Triple(Icons.Rounded.Repeat, CmColors.briefRing, R.string.sug_kind_recurring)
                "cleanup" -> Triple(Icons.Rounded.DeleteOutline, CmColors.idle, R.string.sug_kind_cleanup)
                "deadline" -> Triple(Icons.Rounded.Schedule, CmColors.briefWarn, R.string.sug_kind_deadline)
                else -> Triple(Icons.Rounded.DataUsage, CmColors.advice, R.string.sug_kind_quota)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(tone.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tone, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(label).uppercase(), style = MonoSmall.copy(color = tone, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold))
                    Text(s.text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text)
                    FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SugButton(stringResource(R.string.sug_accept), tonal = true) { onAnswer(s, "accept") }
                        SugButton(stringResource(R.string.sug_discuss), tonal = false) { onAnswer(s, "discuss") }
                        SugButton(stringResource(R.string.sug_later), tonal = false) { onAnswer(s, "later") }
                        SugButton(stringResource(R.string.sug_never), tonal = false) { onAnswer(s, "never") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SugButton(text: String, tonal: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (tonal) CmColors.surfaceHigh else Color.Transparent, contentColor = if (tonal) CmColors.text else CmColors.text2) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = if (tonal) 12.dp else 6.dp, vertical = 8.dp))
    }
}
