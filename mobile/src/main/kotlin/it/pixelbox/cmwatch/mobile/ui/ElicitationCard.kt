package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.ElicitField
import it.pixelbox.cmwatch.contract.Elicitation
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Elicit
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Contratto 1.83 (lavoro mcp-elicitation-lavoro-24-10, approvato da Franz): un server MCP chiede qualcosa a metà di uno
 * strumento. In cima alla home, perché dopo `expires_at` (120 s) la domanda torna al terminale: il server, la sessione e il
 * tempo che resta, la domanda, il modulo (un tasto per ogni opzione, Sì/No, testo, numero) e «Rifiuta» / «Invia». Il modo
 * url (un accesso nel browser) ha solo il link e «Apri»: si risponde al terminale. Niente si riempie da solo: solo il
 * `default` che il server stesso propone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ElicitationCard(e: Elicitation, now: Long, onAnswer: (Elicitation, String, Map<String, String?>) -> Unit) {
    val values = rememberSaveable(e.id, saver = valuesSaver) {
        mutableStateMapOf<String, String?>().apply { e.fields.orEmpty().forEach { f -> put(f.key, Elicit.initial(f)) } }
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Extension, null, tint = CmColors.waiting, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(e.server, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
                val left = (e.expiresAt - now).coerceAtLeast(0)
                Text(stringResource(R.string.elicit_meta, e.session, "%d:%02d".format(left / 60, left % 60)), style = MonoSmall)
            }
        }
        Text(e.message, style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
        if (e.mode == "url") {
            val uri = androidx.compose.ui.platform.LocalUriHandler.current
            e.url?.let { u -> Text(u, style = MonoSmall.copy(color = CmColors.text2)) }
            Text(stringResource(R.string.elicit_link_note), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            e.url?.let { u -> ElicitButton(stringResource(R.string.elicit_open), filled = true) { runCatching { uri.openUri(u) } } }
        } else {
            e.fields.orEmpty().forEach { f -> ElicitFieldRow(f, values[f.key]) { v -> values[f.key] = v } }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ElicitButton(stringResource(R.string.elicit_decline), filled = false) { onAnswer(e, "decline", emptyMap()) }
                val ready = Elicit.ready(e, values)
                ElicitButton(stringResource(R.string.elicit_send), filled = true, enabled = ready) { onAnswer(e, "accept", values.toMap()) }
            }
        }
    }
}

/** Un campo: il titolo (con «*» se obbligatorio), la descrizione, e il controllo del suo tipo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ElicitFieldRow(f: ElicitField, v: String?, onChange: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val title = if (f.required) "${f.title} *" else f.title
        when (f.kind) {
            "choice", "boolean" -> {
                Text(title, style = MaterialTheme.typography.labelLarge, color = CmColors.text)
                f.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2) }
                val opts = if (f.kind == "boolean") listOf("true" to stringResource(R.string.elicit_yes), "false" to stringResource(R.string.elicit_no))
                    else f.options.orEmpty().map { it.value to it.label }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Un secondo tocco sull'opzione scelta la toglie, per un campo facoltativo.
                    opts.forEach { (value, label) -> ElicitButton(label, filled = v == value) { onChange(if (v == value && !f.required) null else value) } }
                }
            }
            else -> {
                val p = v?.takeIf { it.isNotBlank() }?.let { Elicit.problem(f, it) }
                val keyboard = when {
                    f.kind == "integer" -> KeyboardType.Number
                    f.kind == "number" -> KeyboardType.Decimal
                    f.format == "email" -> KeyboardType.Email
                    f.format == "uri" -> KeyboardType.Uri
                    else -> KeyboardType.Text
                }
                OutlinedTextField(
                    v.orEmpty(), { onChange(it) }, label = { Text(title) }, singleLine = true, isError = p != null,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                    supportingText = (p?.let { problemText(it) } ?: f.description)?.let { s -> { Text(s) } },
                    shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun problemText(p: Elicit.Problem): String = when (p) {
    Elicit.Problem.Required -> stringResource(R.string.elicit_required)
    Elicit.Problem.NotNumber -> stringResource(R.string.elicit_not_number)
    Elicit.Problem.NotWhole -> stringResource(R.string.elicit_not_whole)
    Elicit.Problem.NotOption -> stringResource(R.string.elicit_not_option)
    is Elicit.Problem.Length -> { val lo = p.min; val hi = p.max
        when {
            lo != null && hi != null -> stringResource(R.string.elicit_length, lo, hi)
            lo != null -> stringResource(R.string.elicit_length_min, lo)
            else -> stringResource(R.string.elicit_length_max, hi ?: 0)
        }
    }
    is Elicit.Problem.Range -> { val lo = p.min; val hi = p.max
        when {
            lo != null && hi != null -> stringResource(R.string.elicit_range, num(lo), num(hi))
            lo != null -> stringResource(R.string.elicit_range_min, num(lo))
            else -> stringResource(R.string.elicit_range_max, num(hi ?: 0.0))
        }
    }
}

private fun num(d: Double) = if (d == Math.floor(d)) d.toLong().toString() else d.toString()

/** Il tasto della scheda: pieno per la scelta fatta e per «Invia», tonale per gli altri; spento finché il modulo non va. */
@Composable
private fun ElicitButton(text: String, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled, shape = CircleShape,
        color = if (!enabled) CmColors.surfaceHigh.copy(alpha = 0.5f) else if (filled) CmColors.primary else CmColors.surfaceHigh,
        contentColor = if (!enabled) CmColors.text2 else if (filled) CmColors.onPrimary else CmColors.text,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
}

private val valuesSaver = mapSaver<SnapshotStateMap<String, String?>>(
    save = { m -> m.toMap() },
    restore = { saved -> mutableStateMapOf<String, String?>().apply { saved.forEach { (k, v) -> put(k, v as String?) } } },
)
