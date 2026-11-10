package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ProvaLinks
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DM = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/**
 * La scheda di una prova (app-prova-link-10-11, Franz 10/11 20:59: «quando una sessione cita una prova possiamo linkarla?»): si apre dal
 * link «prova #N» di un messaggio o da `#/prova/<n>`. Il numero e il compito; lo stato (da provare, va, non va, confermata col controllo
 * rosso) con le parole della persona o il motivo e l'ora; dove, cosa fare e cosa vedere. Ancora da provare, il tasto porta a Verifica,
 * dove si risponde. `read` null: sta arrivando; un relay di prima del contratto 1.134 lo dice in una riga. Come la scheda di un
 * monitoraggio: sul telefono a tutto schermo con lo spazio delle barre di sistema, nella colonna del tablet la × al posto della freccia.
 */
@Composable
fun ProvaPage(n: Long, read: ProvaLinks.Read?, wide: Boolean, onBack: () -> Unit, onVerify: (() -> Unit)?) {
    val p = (read as? ProvaLinks.Read.Found)?.prova
    Column(Modifier.fillMaxSize().background(LocalPageBg.current ?: V3.Bg).then(if (wide) Modifier else Modifier.systemBarsPadding()).testTag("prova-$n")) {
        Row(Modifier.fillMaxWidth().padding(start = if (wide) 18.dp else 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = V3.Tx) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.prova_title, n.toString()), style = V3.Section.copy(fontSize = 22.sp, lineHeight = 26.sp))
                p?.let { x -> listOfNotNull(x.short ?: x.task.takeIf { it.isNotBlank() }, x.session, x.build.takeIf { it.isNotBlank() }).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { Text(it, style = V3.Small) } }
            }
            if (wide) IconButton(onClick = onBack, modifier = Modifier.handCursor()) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = V3.Tx2) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (read) {
                null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("prova-in-arrivo")) {
                    if (!animationsOff()) androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), color = V3.Tx2, strokeWidth = 2.dp)
                    Text(stringResource(R.string.prova_loading), style = V3.Body.copy(color = V3.Tx2))
                }
                ProvaLinks.Read.Unsupported -> Text(stringResource(R.string.prova_unsupported), style = V3.Body.copy(color = V3.Tx2), modifier = Modifier.testTag("prova-non-supportata"))
                is ProvaLinks.Read.Refused -> Text(read.message, style = V3.Body.copy(color = V3.Red), modifier = Modifier.testTag("prova-rifiutata"))
                is ProvaLinks.Read.Found -> {
                    ProvaState(read.prova)
                    read.prova.title?.takeIf { it.isNotBlank() }?.let { Text(it, style = V3.Body.copy(color = V3.Tx2)) }
                    ProvaField(stringResource(R.string.prova_where), read.prova.dove)
                    ProvaField(stringResource(R.string.prova_do), read.prova.fai)
                    ProvaField(stringResource(R.string.prova_expected), read.prova.atteso)
                }
            }
        }
        if (p?.state == "todo" && onVerify != null) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            WideButton(stringResource(R.string.prova_open_verify), filled = true, tag = "prova-verifica", onClick = onVerify)
        }
    }
}

/** Lo stato della prova, come lo stato di un monitoraggio: il pallino, la parola, le parole della persona o il motivo, l'ora. */
@Composable
private fun ProvaState(p: ProvaOne) {
    val (word, tone) = when (p.state) {
        "ok" -> R.string.prova_state_ok to V3.Good
        "ko" -> R.string.prova_state_ko to V3.Red
        "red" -> R.string.prova_state_red to V3.Red
        else -> R.string.prova_state_todo to V3.Wait
    }
    Column(Modifier.fillMaxWidth().clip(V3.Shape).background(V3.S1).v3Edge().padding(14.dp).testTag("prova-stato-${p.state}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(tone))
            Text(stringResource(word).uppercase(), style = V3.Name.copy(fontSize = 15.sp, letterSpacing = 0.6.sp, color = tone))
        }
        p.text?.takeIf { it.isNotBlank() }?.let { Text(it, style = V3.Body) }
        if (p.at > 0) Text(stringResource(R.string.prova_at, DM.format(Instant.ofEpochSecond(p.at).atZone(ZoneId.systemDefault()))), style = V3.Small)
    }
}

/** Una voce della prova col suo nome sopra, come «Da dove viene» nella scheda «Ti aspetta»; vuota non c'è. */
@Composable
private fun ProvaField(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), style = V3.Small.copy(fontSize = 12.sp, letterSpacing = 1.2.sp, color = V3.Tx3))
        Text(value, style = V3.Body)
    }
}
