package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * app-tasti-riscontro-20-11, estensione di Franz del 20/11 alle 09:46 («Estendi il controllo ai comandi appesi»): a che punto è un
 * comando mandato da un tasto, anche quando l'esito non torna. Le stesse regole in web/src/lib/cmdTrack.ts.
 *
 * - entro [WAIT_S] secondi senza esito: «Mando al PC»;
 * - poi, ancora senza esito: «Il PC non ha ancora preso il comando» se è ancora in /cmd, «Il PC ci sta lavorando» se l'ha preso;
 * - l'esito arrivato tardi vale come quello puntuale;
 * - oltre [MAX_S] senza esito: la riga finale, nessuna risposta;
 * - «in coda» per una sessione occupata col suo nome, finché la sessione non comincia il turno che lo prende.
 */
object CmdTrack {
    const val WAIT_S = 4L
    const val MAX_S = 180L
    /** Ogni quanto si guarda il bus mentre si aspetta. */
    const val POLL_MS = 2_000L
    private const val QUEUED = "queued:"

    enum class Phase { SENDING, NOT_TAKEN, WORKING, DONE, QUEUED, DELIVERED, REFUSED, NO_ANSWER }

    /** Le fasi dopo cui non si guarda più. */
    fun final(p: Phase): Boolean = p == Phase.DONE || p == Phase.DELIVERED || p == Phase.REFUSED || p == Phase.NO_ANSWER

    /**
     * @param elapsedS secondi dall'invio
     * @param result l'esito, se è arrivato (anche tardi)
     * @param taken false = il comando è ancora in /cmd, true = il PC l'ha preso, null = non si sa
     * @param session la sessione a cui va il comando, com'è adesso nello stato
     * @param sentAt l'ora dell'invio (epoch s)
     */
    fun phase(elapsedS: Long, result: CmdResult?, taken: Boolean?, session: Session? = null, sentAt: Long = 0): Phase = when {
        result != null && !result.ok -> Phase.REFUSED
        result != null && result.text.startsWith(QUEUED) -> if (delivered(session, sentAt)) Phase.DELIVERED else Phase.QUEUED
        result != null -> Phase.DONE
        elapsedS >= MAX_S -> Phase.NO_ANSWER
        elapsedS < WAIT_S -> Phase.SENDING
        taken == false -> Phase.NOT_TAKEN
        taken == true -> Phase.WORKING
        else -> Phase.SENDING
    }

    /** Il prompt in coda è stato consegnato: la sessione ha cominciato un turno dopo l'invio, o è tornata ferma senza domanda. */
    fun delivered(s: Session?, sentAt: Long): Boolean {
        s ?: return false
        val started = s.turnStarted
        return (s.state == SessionState.BUSY && started != null && started >= sentAt - ChatRules.SKEW_S) ||
            s.state == SessionState.IDLE
    }
}
