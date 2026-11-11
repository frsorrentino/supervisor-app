package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ProvaLinks
import kotlinx.coroutines.launch

/**
 * Le prove citate nelle chat come schede compatte (app-prova-scheda-in-chat-11-11, Franz 11/11 19:49: «Riguardo i link all'interno delle
 * sessioni al tasto prova, vorrei che fossero proprio delle schede come queste, ma compatte»). La stessa famiglia di Verifica: casella,
 * titolo, cosa fare in una riga, i tasti Non va e Ok; con l'esito già dato, l'esito al posto dei tasti. Il tocco sulla scheda apre la
 * prova come il link. I dati dall'op `prove` col numero (contratto 1.134), una lettura per scheda con l'etag e poi in cache, riletta
 * solo dopo un tocco: niente letture periodiche. Fuori dall'app (i provini) nessun [LocalProvaCards]: restano i link.
 */
class ProvaCardsHost(
    /** La prova letta, null finché non arriva. */
    val read: (Long) -> ProvaLinks.Read?,
    /** Chiede la prova se non c'è ancora: una volta sola, poi la cache. */
    val ask: (Long) -> Unit,
    val open: (Long) -> Unit,
    val ok: suspend (Long) -> Boolean,
    val ko: suspend (ProvaOne, String) -> Boolean,
)

val LocalProvaCards = staticCompositionLocalOf<ProvaCardsHost?> { null }

/** Una scheda per ogni prova citata nel testo, nell'ordine in cui compare. */
@Composable
fun ProvaCards(text: String) {
    val host = LocalProvaCards.current ?: return
    val ns = remember(text) { ProvaLinks.find(text).map { it.n }.distinct() }
    ns.forEach { n ->
        LaunchedEffect(n) { host.ask(n) }
        ProvaCard(n, host.read(n), onOpen = { host.open(n) }, onOk = { host.ok(n) }, onKo = { p, why -> host.ko(p, why) })
    }
}

/** La scheda compatta di una prova. Un numero che non esiste, o un relay che non sa leggerla, non dà scheda: resta il link. */
@Composable
fun ProvaCard(n: Long, read: ProvaLinks.Read?, onOpen: () -> Unit, onOk: suspend () -> Boolean, onKo: suspend (ProvaOne, String) -> Boolean) {
    if (read is ProvaLinks.Read.Refused || read is ProvaLinks.Read.Unsupported) return
    val p = (read as? ProvaLinks.Read.Found)?.prova
    val scope = rememberCoroutineScope()
    var sent by remember(n) { mutableStateOf<String?>(null) }
    var asking by remember(n) { mutableStateOf(false) }
    val tone = when (p?.state) { "ok" -> V3.Good; "ko", "red" -> V3.Red; null -> V3.Tx3; else -> V3.Wait }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(V3.S1).border(1.dp, V3.Line, RoundedCornerShape(14.dp))
            .handCursor().clickable(onClick = onOpen).testTag("prova-scheda-$n").padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tone))
            Spacer(Modifier.width(8.dp))
            Text(p?.let { it.title ?: it.short ?: it.task } ?: stringResource(R.string.prova_title, n.toString()), style = V3.Name, maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
        }
        if (p == null) { Text(stringResource(R.string.prova_loading), style = V3.Small); return@Column }
        val head = p.title ?: p.short ?: p.task
        Text(listOfNotNull(stringResource(R.string.prova_title, n.toString()), listOf(p.short, p.task).firstOrNull { !it.isNullOrBlank() && it != head }).joinToString(" · "), style = V3.Small, maxLines = 1, overflow = TextOverflow.Clip)
        if (p.fai.isNotBlank()) Text(p.fai, style = V3.Body, maxLines = 1, overflow = TextOverflow.Clip)
        when (p.state) {
            "todo" -> Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { asking = true }, enabled = sent == null) { Text(stringResource(R.string.prove_ko)) }
                ConfirmButton(stringResource(R.string.prove_ok), confirmed = sent == "ok", enabled = sent == null, faded = sent == "ko") {
                    sent = "ok"; scope.launch { if (!onOk()) sent = null }
                }
            }
            else -> {
                val (label, color) = when (p.state) { "ok" -> R.string.prova_state_ok to V3.Good; "red" -> R.string.prova_state_red to V3.Red; else -> R.string.prova_state_ko to V3.Red }
                Text(listOfNotNull(stringResource(label), p.text?.takeIf { it.isNotBlank() }).joinToString(": "), style = V3.Small.copy(color = color), maxLines = 2, overflow = TextOverflow.Clip)
            }
        }
    }
    if (asking && p != null) {
        var why by rememberSaveable(n) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { asking = false }, containerColor = Cm.surface,
            title = { Text(p.title ?: p.short ?: p.task, color = Cm.text) },
            text = { OutlinedTextField(why, { why = it.take(2000) }, label = { Text(stringResource(R.string.prove_ko_hint)) }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(enabled = why.isNotBlank(), onClick = { val body = why.trim(); asking = false; sent = "ko"; scope.launch { if (!onKo(p, body)) sent = null } }) { Text(stringResource(R.string.prove_ko_send)) } },
            dismissButton = { TextButton(onClick = { asking = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
