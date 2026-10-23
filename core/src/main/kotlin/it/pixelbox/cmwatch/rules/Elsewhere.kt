package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * L'avviso delle altre sessioni nella chat di una sessione (Franz, 02/10 20:47, variante A dei mockup): una pastiglia
 * sotto la barra. Prima chi ti aspetta, finché la domanda c'è; poi un turno finito, una volta sola e solo se recente,
 * delle sessioni che segui o a cui hai scritto dal telefono (con 5-6 sessioni al lavoro, tutte non sarebbe discreto).
 * Mai la sessione aperta.
 */
object Elsewhere {
    sealed interface Alert { val key: String }

    /** Le sessioni ferme su una domanda, dalla più vecchia; `requests` è la chiave di ogni richiesta (`w:sessione@at`). */
    data class Waiting(val sessions: List<String>, val requests: List<String>) : Alert { override val key get() = "w:" + sessions.joinToString(",") }

    /** Il turno finito più recente non ancora visto. */
    data class Finished(val session: String, val at: Long) : Alert { override val key get() = "f:$session@$at" }

    /** Le chiavi da segnare come viste alla chiusura con la ✕ o con lo scorrimento: ogni richiesta in attesa, o l'esito. */
    val Alert.seenKeys: List<String> get() = if (this is Waiting) requests else listOf(key)

    /** Oltre questo tempo un esito non è più una novità: all'apertura dell'app non si elencano i turni di ore fa. */
    const val FRESH_S = 600L

    fun alert(state: State, current: String, now: Long, mine: Set<String>, seen: Set<String>): Alert? {
        val others = state.sessions.filter { it.name != current && it.state != SessionState.GONE }
        // Contratto 1.68: chi è ferma per Franz conta come una domanda.
        val waiting = others.mapNotNull { s -> (s.question?.askedAt ?: StrongAlert.stop(s)?.at)?.let { s.name to it } }.sortedBy { it.second }
        // Franz 23/10: chiuso con la ✕ o con lo scorrimento, torna solo quando arriva una richiesta nuova: ogni richiesta ha
        // la sua chiave in `seen`; una risposta altrove non lo fa tornare.
        val requests = waiting.map { "w:${it.first}@${it.second}" }
        if (waiting.isNotEmpty() && requests.any { it !in seen }) return Waiting(waiting.map { it.first }, requests)
        return others.filter { it.state == SessionState.IDLE && (it.followed || it.name in mine) }
            .mapNotNull { s -> s.outcome?.takeIf { now - it.at <= FRESH_S }?.let { Finished(s.name, it.at) } }
            .filter { it.key !in seen }
            .maxByOrNull { it.at }
    }
}
