package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * «Chiudi le finite (N)» (Franz 24/10 19:46, B4 del mockup; contratto 1.84). Franz: «bisogna accertarsi che quando si chiude
 * una sessione l'ultimo stato sia stato salvato». Candidate: le ferme senza finestra al PC, ferme da almeno un'ora; il relay
 * rifà i controlli (al lavoro, domanda, lavori in sottofondo) e salva lo stato prima di chiudere. Stessa regola di closeFinished.ts.
 */
object CloseFinished {
    const val IDLE_FOR_S = 3600L

    fun candidates(rows: List<Summary.Row>, now: Long): List<Session> =
        rows.filter { it.group == Summary.Group.STILL && it.session.state == SessionState.IDLE && !it.session.attached && now - it.session.since >= IDLE_FOR_S }.map { it.session }

    @Serializable data class Saved(
        val outcome: String? = null, val next: String? = null, @SerialName("session_id") val sessionId: String? = null,
        @SerialName("dirty_n") val dirtyN: Int? = null, val dirty: List<String>? = null,
    )
    @Serializable data class Row(val name: String, val closable: Boolean = false, @SerialName("blocked_by") val blockedBy: String? = null, val save: Saved? = null)
    @Serializable data class Preview(val sessions: List<Row> = emptyList())

    /** Il testo dell'op `close_preview`; null se non si legge. */
    fun read(text: String?): Preview? = runCatching { ContractJson.json.decodeFromString(Preview.serializer(), text.orEmpty()) }.getOrNull()

    /** Le chiudibili, nell'ordine dell'anteprima. */
    fun closable(p: Preview): List<String> = p.sessions.filter { it.closable }.map { it.name }
}
