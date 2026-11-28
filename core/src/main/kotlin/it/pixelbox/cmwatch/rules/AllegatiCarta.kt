package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * app-allegati-carta-27-11 (Franz 27/11 20:47, su una carta del Banco prova: «questo campo di testo dovrebbe avere la possibilità di
 * caricare allegati come nelle sessioni»): nel campo di risposta di una carta (Ti aspetta, Banco prova) il + del campo delle sessioni,
 * con gli stessi limiti e la stessa anteprima. Gli allegati partono prima della risposta, in un `report` alla sessione del compito,
 * così la sessione li ha già quando legge la risposta. Porta di allegatiTarget in web/src/lib/allegatiCarta.ts, coi casi condivisi di
 * core/src/test/resources/allegati-carta-cases.json.
 */
object AllegatiCarta {
    /** Quanti allegati per risposta: gli stessi del campo delle sessioni. */
    const val MAX = 5

    /**
     * A quale sessione vanno gli allegati: quella della carta (`session` di una voce di `attese`, di una prova), poi quella del compito
     * nel registro (`commitments[].session` del `task`), la prima delle due che è viva. Senza nessuna viva la master: una sessione
     * chiusa rifiuta il `report` («no session»), e la master sa a chi passarli.
     */
    fun target(session: String?, task: String?, state: State?): String {
        val live = state?.sessions.orEmpty().filter { it.state != SessionState.GONE }.map { it.name }.toSet()
        val ofTask = task?.let { t -> state?.commitments?.firstOrNull { it.task == t }?.session }
        return listOfNotNull(session, ofTask).firstOrNull { it in live } ?: ContextActions.MASTER
    }
}
