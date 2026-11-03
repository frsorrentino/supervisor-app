package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session

/**
 * Cosa dice in più la scheda di una sessione aperta (Franz 02/11 21:28, app-attesa-dettagli-02-11: «deve darmi maggiori
 * dettagli sulla scelta da compiere, ora ci sono solo dati statistici»). Tutto viene già dallo stato, senza chiedere al relay:
 * l'ultimo messaggio è `outcome.full` (la coda vera, fino a 600 caratteri), le scelte sono `next_steps`, il lavoro in corso
 * `tool` e `tool_note`. Le statistiche restano, sotto.
 */
object SessionDetail {
    /**
     * `said` = l'ultimo messaggio di chi aspetta Franz (esito e proposta, senza le righe di chiusura), null se ripete la riga in
     * testa alla scheda; `steps` = le voci di «Prossimi:» che non hanno già un tasto; `doing` = strumento e nota del turno in corso.
     */
    data class Detail(val said: String?, val saidAt: Long?, val steps: List<String>, val doing: String?)

    private val TIPO = Regex("^[\\s*_>-]*Tipo\\s*:", RegexOption.IGNORE_CASE)

    /** `shown` = il testo già in testa alla scheda (la domanda, il motivo o l'esito). */
    fun of(s: Session, group: Summary.Group, shown: String?): Detail {
        val asks = group == Summary.Group.WAITING && s.question == null
        val o = s.outcome?.takeIf { asks }
        val said = o?.let { OutcomeText.summary(it) }
            ?.lines()?.filterNot { TIPO.containsMatchIn(it) }?.joinToString("\n")?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals(shown?.trim(), ignoreCase = true) }
        // La voce che ha già il suo «Ok» non si ripete; con «Fatto» (needs-you, «Da te») le voci di Prossimi sono tutte nuove.
        val ok = AttentionQueue.okOf(s)?.lowercase()
        val steps = if (!asks) emptyList() else s.nextSteps.orEmpty().map { it.text.trim() }.filter { it.isNotEmpty() && it.lowercase() != ok }
        val inTurn = s.outcome == null || (s.turnStarted ?: 0) > s.outcome!!.at
        val doing = listOfNotNull(s.tool?.trim()?.takeIf { it.isNotEmpty() }, s.toolNote?.trim()?.takeIf { it.isNotEmpty() })
            .takeIf { group == Summary.Group.WORKING && inTurn && s.tool?.isNotBlank() == true }?.joinToString(" · ")
        return Detail(said, if (said != null) o?.at else null, steps, doing)
    }
}
