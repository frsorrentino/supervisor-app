package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.publishedTs

/**
 * «Impegni» (proposte P2 e P4 del 21/10, approvate da Franz alle 23:58; contratto 1.76): la vista del registro in cima
 * alla home, sopra le sessioni. Ogni impegno approvato e non compiuto resta in vista con l'età, chi deve fare il prossimo
 * passo e da quanto è fermo; quando aspetta Franz e c'è un ok del registro collegato, la riga ha «Approva», che è lo
 * stesso `approve` di sempre: dopo l'ok il relay risveglia la sessione (P4). L'ordine è quello del relay (prima i fermi,
 * poi per inattività); gli impegni in pausa vanno in fondo. L'età cresce da sola fra una pubblicazione e l'altra.
 */
object Commitments {
    /** Il colore dell'attesa: ambra da due ore senza un passo, rosso da otto; niente per un impegno in pausa. */
    enum class Tone { NORMAL, WARN, ALERT }

    /** `waits.who`/`waits.on` = Franz. */
    const val PERSON = "person"
    const val WARN_S = 2 * 3600L
    const val ALERT_S = 8 * 3600L

    /**
     * `idle`/`age`: secondi fermo e dall'ok, adesso (quelli del relay più il tempo dalla pubblicazione); `yourTurn`: il
     * prossimo passo tocca a Franz; `turn`: la sessione a cui tocca, o null; `approval`: l'ok del registro con lo stesso
     * compito, se c'è (il tasto «Approva»).
     */
    data class Row(
        val c: Commitment, val idle: Long, val age: Long, val tone: Tone, val yourTurn: Boolean, val turn: String?,
        val approval: Approval?,
    )

    fun rows(state: State, now: Long): List<Row> {
        val list = state.commitments ?: return emptyList()
        val passed = (now - state.publishedTs).coerceAtLeast(0)
        val rows = list.map { c ->
            val idle = c.idleS + passed
            Row(
                c, idle, c.ageS + passed, tone(c, idle), c.waits?.on == PERSON, turnOf(c),
                state.approvals.firstOrNull { it.task == c.task },
            )
        }
        return rows.sortedBy { it.c.paused != null }
    }

    fun tone(c: Commitment, idle: Long): Tone = when {
        c.paused != null -> Tone.NORMAL
        idle >= ALERT_S -> Tone.ALERT
        c.stalled || idle >= WARN_S -> Tone.WARN
        else -> Tone.NORMAL
    }

    /** La sessione a cui tocca il prossimo passo; null quando tocca a Franz o non si sa. */
    fun turnOf(c: Commitment): String? = c.waits?.on?.takeIf { it.isNotBlank() && it != PERSON }

    /** Quanti impegni aspettano Franz adesso (non in pausa). */
    fun yours(state: State): Int = state.commitments.orEmpty().count { it.waits?.on == PERSON && it.paused == null }
}
