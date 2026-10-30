package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.AttentionQueue

/**
 * «Ti aspettano» (piano 30/09, Task 1): le domande aperte di tutte le sessioni in fila, dalla più vecchia, una per
 * pagina. Ogni pagina è la testata della sessione e la stessa card della scheda; dopo una risposta si passa alla
 * successiva. Una domanda risposta al PC sparisce dalla fila da sola (`AttentionQueue`).
 */
@Composable
fun QueueScreen(
    state: State, now: Long, actionsFor: (Session) -> SheetActions, onSession: (String) -> Unit, skip: String? = null,
    /** Contratto 1.80: il tasto «Fatto» di una ferma (op `attention_done`). */
    onDone: (AttentionQueue.Done) -> Unit = {},
) {
    // Aperta dal banner di una chat: la sessione di quella chat non è in fila, come nel conto del banner (web 15/10).
    val items = AttentionQueue.items(state).filter { it.session != skip }
    Column(Modifier.fillMaxSize().background(Cm.bg).systemBarsPadding()) {
        Text(
            stringResource(R.string.queue_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Cm.text, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        )
        if (items.isEmpty()) {
            Text(stringResource(R.string.queue_empty), color = Cm.text2, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 20.dp))
            return@Column
        }
        val pager = rememberPagerState { items.size }
        // Dopo una risposta: la domanda che seguiva, cercata per id quando la fila si è accorciata (revisione 01/10).
        var goTo by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(goTo, items) {
            val i = AttentionQueue.page(items, goTo) ?: return@LaunchedEffect
            if (i != pager.currentPage) pager.animateScrollToPage(i)
            goTo = null
        }
        HorizontalPager(pager, key = { items[it].questionId }, modifier = Modifier.weight(1f)) { page ->
            val item = items[page]
            val s = state.sessions.firstOrNull { it.name == item.session } ?: return@HorizontalPager
            val q = s.question
            if (q == null || item.kind == AttentionQueue.Kind.STOP) {
                // Contratto 1.68: ferma per Franz (un login, un ok, una scelta di quota): il motivo, e la chat dove si risponde.
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QueueHead(s, item.askedAt, now, onSession)
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.queue_stop_head), style = MaterialTheme.typography.labelLarge, color = Cm.text2)
                        Text(item.text, style = MaterialTheme.typography.bodyLarge, color = Cm.text)
                        // Contratto 1.80: la scadenza di un needs-you e il tasto «Fatto», che chiude la richiesta dal telefono.
                        item.expiresAt?.let { Text(expiryText(it, now), style = MaterialTheme.typography.labelLarge, color = Cm.text2) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { onSession(s.name) }) { Text(stringResource(R.string.queue_stop_open)) }
                            item.done?.let { d -> Button(onClick = { onDone(d) }) { Text(stringResource(R.string.attention_done)) } }
                        }
                    }
                }
                return@HorizontalPager
            }
            var holdHint by remember(q.id) { mutableStateOf(false) }
            val base = actionsFor(s)
            // Dopo la risposta la pagina dopo, subito: la domanda sparisce dalla fila quando il PC la chiude.
            val actions = base.copy(answer = { n ->
                goTo = AttentionQueue.next(state, q.id)?.questionId?.takeIf { it != q.id }
                base.answer(n)
            })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QueueHead(s, q.askedAt, now, onSession)
                QuestionCard(q, holdHint = holdHint, onHold = { holdHint = true }, actions = actions)
            }
        }
        Text(
            stringResource(R.string.queue_position, (pager.currentPage + 1).coerceAtMost(items.size), items.size), style = MaterialTheme.typography.labelLarge,
            color = Cm.text2, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp),
        )
    }
}

/** La testata di una pagina della fila: la sessione e da quanto aspetta; il tocco apre la chat. */
@Composable
private fun QueueHead(s: Session, askedAt: Long, now: Long, onSession: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().handCursor().clickable { onSession(s.name) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SessionBadge(s, size = 26.dp)
        Text(s.name, style = MaterialTheme.typography.titleMedium, color = Cm.text, modifier = Modifier.weight(1f), maxLines = 1)
        Text(stringResource(R.string.queue_age, Durations.since(askedAt, now)), style = MaterialTheme.typography.labelLarge, color = Cm.text2)
    }
}
