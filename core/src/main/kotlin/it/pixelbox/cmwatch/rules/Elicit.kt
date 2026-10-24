package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ElicitField
import it.pixelbox.cmwatch.contract.Elicitation
import it.pixelbox.cmwatch.contract.State
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * Contratto 1.83 (lavoro mcp-elicitation-lavoro-24-10, approvato da Franz): le domande dei server MCP sul telefono. Una
 * scheda per domanda aperta; il modulo si compila qui e parte con l'op `elicit`. I valori del modulo stanno come testo
 * (choice = il value, boolean = "true"/"false", numeri come digitati) e diventano JSON solo all'invio. Il relay ricontrolla
 * tutto: qui si ferma solo l'invio che sarebbe rifiutato di sicuro, senza riempire nulla da solo.
 */
object Elicit {
    sealed interface Problem {
        data object Required : Problem
        data class Length(val min: Int?, val max: Int?) : Problem
        data object NotNumber : Problem
        data object NotWhole : Problem
        data class Range(val min: Double?, val max: Double?) : Problem
        data object NotOption : Problem
    }

    /** Le domande ancora aperte: dopo `expires_at` la domanda è tornata al terminale e la scheda va via. */
    fun open(state: State?, now: Long): List<Elicitation> = state?.elicitations.orEmpty().filter { it.expiresAt > now }

    /** Il valore di partenza: il `default` del server, se c'è; mai inventato. */
    fun initial(f: ElicitField): String? = (f.defaultValue as? JsonPrimitive)?.let { p -> p.booleanOrNull?.toString() ?: p.contentOrNull }

    /** Cosa non va nel valore di un campo; null = va bene (vuoto e facoltativo va bene). */
    fun problem(f: ElicitField, v: String?): Problem? {
        val x = v?.trim().orEmpty()
        if (x.isEmpty()) return if (f.required) Problem.Required else null
        return when (f.kind) {
            "choice" -> if (f.options.orEmpty().none { it.value == x }) Problem.NotOption else null
            "boolean" -> if (x != "true" && x != "false") Problem.NotOption else null
            "text" -> if ((f.minLength != null && x.length < f.minLength) || (f.maxLength != null && x.length > f.maxLength)) Problem.Length(f.minLength, f.maxLength) else null
            "number", "integer" -> {
                val n = x.replace(',', '.').toDoubleOrNull() ?: return Problem.NotNumber
                when {
                    f.kind == "integer" && n != Math.floor(n) -> Problem.NotWhole
                    (f.min != null && n < f.min) || (f.max != null && n > f.max) -> Problem.Range(f.min, f.max)
                    else -> null
                }
            }
            else -> null
        }
    }

    /** Si può mandare: ogni campo senza problemi. */
    fun ready(e: Elicitation, values: Map<String, String?>): Boolean = e.fields.orEmpty().all { problem(it, values[it.key]) == null }

    /** Il `content` dell'op `elicit`: i campi compilati, con il tipo giusto (numeri come numeri, sì/no come booleani). */
    fun content(e: Elicitation, values: Map<String, String?>): JsonObject = buildJsonObject {
        e.fields.orEmpty().forEach { f ->
            val x = values[f.key]?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            when (f.kind) {
                "boolean" -> put(f.key, x == "true")
                "integer" -> x.replace(',', '.').toDoubleOrNull()?.let { put(f.key, it.toLong()) }
                "number" -> x.replace(',', '.').toDoubleOrNull()?.let { n -> if (n == Math.floor(n) && Math.abs(n) < 1e15) put(f.key, n.toLong()) else put(f.key, n) }
                else -> put(f.key, x)
            }
        }
    }
}
