package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Una lettura che non si arrende (app-monitoraggi-attesa-10-11, Franz 10/11 15:12: «Perché da app non vedo la scheda monitoraggi?»): il
 * comando di lettura restava in fila sul relay 23-51 s, l'app aspettava 30 s e poi lasciava la sezione vuota fino al valore nuovo, ore dopo.
 * Qui ogni tentativo aspetta [wait]; se non arriva niente parte un altro comando e si ascoltano tutti, così una risposta arrivata tardi a un
 * tentativo di prima si usa lo stesso. Un rifiuto: con [giveUpOnRefusal] (una scheda sola che non c'è più) si smette, altrimenti si
 * riprova dopo [after] del tentativo. Le risposte lette o lasciate si dimenticano ([forget]).
 */
object PatientRead {
    suspend fun read(
        send: suspend () -> String?, results: Flow<Map<String, CmdResult>>, forget: (String) -> Unit,
        giveUpOnRefusal: Boolean, wait: Long = 30_000L, after: (Int) -> Long = { a -> Monitors.retryAfter(a) * 1000 },
    ): String? = result(send, results, forget, giveUpOnRefusal, wait, after)?.takeIf { it.ok }?.text

    /** Come [read], ma col risultato: con [giveUpOnRefusal] anche il rifiuto, per dirne il motivo (una prova che non esiste). */
    suspend fun result(
        send: suspend () -> String?, results: Flow<Map<String, CmdResult>>, forget: (String) -> Unit,
        giveUpOnRefusal: Boolean, wait: Long = 30_000L, after: (Int) -> Long = { a -> Monitors.retryAfter(a) * 1000 },
    ): CmdResult? {
        val ids = mutableListOf<String>()
        var attempt = 0
        try {
            while (true) {
                runCatching { send() }.getOrNull()?.let { ids += it }
                val got = withTimeoutOrNull(maxOf(wait, after(attempt))) {
                    results.first { m -> ids.any { x -> m.containsKey(x) } }.let { m -> ids.first { x -> m.containsKey(x) }.let { x -> x to m.getValue(x) } }
                }
                if (got != null) {
                    val (id, r) = got
                    ids.remove(id); forget(id)
                    if (r.ok || giveUpOnRefusal) return r
                    delay(after(attempt))
                }
                attempt++
            }
        } finally { ids.forEach(forget) }
    }
}
