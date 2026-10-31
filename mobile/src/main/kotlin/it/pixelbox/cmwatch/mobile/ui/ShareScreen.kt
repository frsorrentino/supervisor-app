@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import it.pixelbox.cmwatch.contract.Durations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.PhoneBoard

/** L'allegato arrivato con «Condividi», come riga a schermo: `blocked` se non si può mandare, e la riga dice perché. */
class ShareAttachment(val line: String, val image: Boolean, val blocked: Boolean = false)

/** «Condividi» (design 29/09, schermata 6): la sessione viva a cui mandare, il messaggio, un solo bottone pieno «Manda». */
@Composable
fun ShareScreen(state: State, text: String, attached: ShareAttachment?, sending: Boolean, onSend: (session: String, message: String) -> Unit) {
    val alive = PhoneBoard.sections(state).filter { it.group != PhoneBoard.Group.CLOSED }.flatMap { it.sessions }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf(text) }
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding()) {
        Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.share_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = Cm.text)
            if (state.share == null) {
                // Contratto 1.19: senza `share` il relay non accetta ancora «Condividi».
                Text(stringResource(R.string.share_update_pc), color = Cm.text2, style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            if (attached != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (attached.image) Icons.Rounded.Image else Icons.Rounded.Description, null, tint = if (attached.blocked) Cm.briefWarn else Cm.actionIcon)
                Text(attached.line, color = if (attached.blocked) Cm.briefWarn else Cm.text2)
            }
            OutlinedTextField(message, { message = it }, label = { Text(stringResource(R.string.share_message)) }, minLines = 2, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            if (sending) LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Cm.actionIcon, trackColor = Cm.briefTrack)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alive, key = { it.id }) { s ->
                    val on = s.name == chosen
                    // Le card della regia in piccolo: la scelta si vede dal bordo azzurro e dalla superficie più alta.
                    Surface(
                        onClick = { chosen = s.name }, color = if (on) Cm.surfaceHigh else Cm.surface, shape = MaterialTheme.shapes.large,
                        border = if (on) BorderStroke(2.dp, Cm.actionIcon) else null, modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SessionBadge(s)
                            Text(s.name, color = if (on) Cm.actionIcon else Cm.text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                            StatePill(s.state, Durations.since(s.since, state.ts))
                        }
                    }
                }
            }
        }
        if (state.share != null) Button(
            onClick = { chosen?.let { onSend(it, message.trim()) } },
            enabled = chosen != null && !sending && attached?.blocked != true && (attached != null || message.isNotBlank()),
            colors = ButtonDefaults.buttonColors(containerColor = Cm.primary, contentColor = Cm.onPrimary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
        ) { Text(stringResource(R.string.share_send)) }
    }
}
