package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * «Sessioni al massimo» nelle impostazioni (app-tabellone-frazioni-13-11, Franz 13/11 13:56: «il numero di sessioni massime gestite
 * deve essere selezionabile dall'utente nelle impostazioni»; contratto 1.145), gemella di web/src/lib/maxSessions.ts. Il valore di
 * adesso è `kpi_health.sessions_max`; si cambia con l'op `max_sessions_set`, presente in `ops` solo nei relay che lo sanno fare: il
 * numero in `arg` come intero in testo, da 1 a 20; la risposta buona è `{"max_sessions": N}`. Senza l'op la voce si legge e basta.
 */
object MaxSessions {
    const val OP = "max_sessions_set"
    const val MIN = 1
    const val MAX = 20

    fun canSet(ops: List<String>?): Boolean = ops?.contains(OP) == true
    fun valid(n: Int): Boolean = n in MIN..MAX

    /** I numeri fra cui scegliere; un valore di adesso fuori dall'intervallo (messo a mano nella config) resta in elenco al suo posto. */
    fun choices(now: Int?): List<Int> {
        val all = (MIN..MAX).toList()
        return if (now != null && !valid(now)) (all + now).sorted() else all
    }

    /** Il numero confermato dal relay, dal testo della risposta; null se non si legge. */
    fun answer(text: String): Int? = runCatching {
        (Json.parseToJsonElement(text).jsonObject["max_sessions"] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
    }.getOrNull()
}
