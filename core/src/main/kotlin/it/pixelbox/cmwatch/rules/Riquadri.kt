package it.pixelbox.cmwatch.rules

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * I riquadri della sessione chiusi come pillole (app-riquadri-chiusi-16-11, Franz 16/11 20:02, primo pezzo del passo 3 del
 * riallineamento, docs/valutazioni/2026-11-16-chiarezza-sessione.md): Per te, Risultati, Azioni, Contesto, Altre sessioni e Avvisi
 * nascono chiusi, una pillola col numero, e si aprono al tocco. Da solo si apre soltanto «Per te» quando ha una voce che aspetta la
 * persona; chiuso a mano resta chiuso finché non arriva una voce nuova. Lo stato resta per sessione. Come riquadri.ts, coi casi di
 * core/src/test/resources/riquadri-cases.json.
 */
object Riquadri {
    enum class Kind(val key: String) { PERTE("perte"), RISULTATI("risultati"), AZIONI("azioni"), CONTESTO("contesto"), ALTRE("altre"), AVVISI("avvisi") }

    /** Lo stato di una sessione: i riquadri aperti o chiusi a mano; [keys] le voci di «Per te» che aspettavano quando è stato chiuso. */
    @Serializable data class State(val open: Map<String, Boolean> = emptyMap(), val keys: List<String>? = null)

    /** Aperto adesso. [waiting]: le chiavi delle voci di Per te che aspettano la persona (non i risultati da guardare). */
    fun isOpen(st: State, kind: Kind, waiting: List<String> = emptyList()): Boolean {
        val o = st.open[kind.key]
        if (kind != Kind.PERTE) return o == true
        return when (o) {
            true -> true
            false -> waiting.any { k -> k !in st.keys.orEmpty() }
            null -> waiting.isNotEmpty()
        }
    }

    /** Il tocco: apre o chiude. Chiudendo «Per te» si ricordano le voci che aspettavano, così una nuova lo riapre. */
    fun toggle(st: State, kind: Kind, waiting: List<String> = emptyList()): State {
        val was = isOpen(st, kind, waiting)
        val open = st.open + (kind.key to !was)
        return if (kind == Kind.PERTE && was) st.copy(open = open, keys = waiting) else st.copy(open = open)
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    /** Lo stato salvato di una sessione; illeggibile vale nessuno stato. */
    fun decode(raw: String?): State? = raw?.let { r -> runCatching { json.decodeFromString(State.serializer(), r) }.getOrNull() }
    fun encode(st: State): String = json.encodeToString(State.serializer(), st)
}
