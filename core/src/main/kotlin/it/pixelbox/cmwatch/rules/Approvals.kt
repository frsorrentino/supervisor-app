package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State

/**
 * Franz 23/10 21:24 («ho già cliccato su approva ma continua ad apparire»): dopo Approva la voce sparisce subito, su app e
 * web, e non torna finché il registro non conferma; con un rifiuto torna col motivo.
 */
object Approvals {
    /** Lo stato senza gli ok dei compiti `tasks`: l'ok tolto e l'impegno già approvato, senza attesa della persona. */
    fun withoutApproved(state: State, tasks: Set<String>): State {
        if (tasks.isEmpty()) return state
        return state.copy(
            approvals = state.approvals.filter { it.task !in tasks },
            commitments = state.commitments?.map { c -> if (c.task in tasks) c.copy(approved = true, state = "approved", waits = null) else c },
        )
    }

    /** I compiti che il registro ha già portato oltre l'ok: si dimenticano, così una richiesta nuova dello stesso compito torna a vedersi. */
    fun settled(state: State, tasks: Set<String>): Set<String> {
        val asking = state.approvals.map { it.task }.toSet()
        val awaiting = state.commitments.orEmpty().filter { it.state == "awaiting_ok" }.map { it.task }.toSet()
        return tasks.filterTo(mutableSetOf()) { it !in asking && it !in awaiting }
    }
}
