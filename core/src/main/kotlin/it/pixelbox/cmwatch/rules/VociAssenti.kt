package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Question
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * Le voci che l'app non offre più quando il relay dice che non ci sono (app-issue4-cause-app-26-11, contratto 1.161, issue #4: il
 * rapporto di claude-observe dal Mac di un collega). Prima l'app offriva «Chat about this» e «Consenti sempre» su domande che non li
 * hanno, chiedeva `night` senza rapporto della notte, Riapri su sessioni aperte, slash a sessioni sparite e Chiudi su sessioni
 * attaccate al PC o con un lavoro in sottofondo: il PC rifiutava. Uguali sul web (web/src/lib/vociAssenti.ts).
 */
object VociAssenti {
    /** «Chat about this»: solo se c'è in fondo al dialogo; senza `foot` (relay precedente, altro host) come prima. */
    fun chat(q: Question): Boolean = q.foot == null || "chat" in q.foot

    /** «Scrivi» (la risposta libera, `answer` con `text:`): solo se il dialogo ha «Type something.»; senza `foot` come prima. */
    fun type(q: Question): Boolean = q.foot == null || "type" in q.foot

    /** «Consenti sempre»: le regole di sempre e il «don't ask again» che c'è davvero (`allow_all`, assente = come prima). */
    fun allowAll(q: Question): Boolean = QuestionRules.allowAllVisible(q)

    /** La notte: `ops` ha `night` solo con un rapporto su disco; senza `ops` (relay vecchissimo) come prima. */
    fun night(ops: List<String>?): Boolean = ops == null || "night" in ops

    /** Riapri: mai su una sessione che c'è ed è aperta; una chiusa, o che lo stato non porta più, la decide il PC. */
    fun reopen(s: Session?): Boolean = s == null || s.state == SessionState.GONE

    /** Uno slash va solo a una sessione che c'è ed è aperta. */
    fun slash(st: State?, name: String): Boolean = st?.sessions?.any { it.name == name && it.state != SessionState.GONE } == true

    /**
     * Chiudi: non su una sessione attaccata al PC (`attached`) né su una col lavoro in sottofondo (`close_preview` dice
     * `blocked_by: background`); `blockedBy` null se la lettura non c'è.
     */
    fun close(s: Session, blockedBy: String? = null): Boolean = !s.attached && blockedBy != "background"

    private val TYPED = Regex("""casella di «([^»]+)» c'è del testo digitato""")

    /**
     * Il rifiuto del PC quando nella casella della sessione c'è del testo scritto a mano («supervisor talk: nella casella di
     * «<session>» c'è del testo digitato: …»): il nome della sessione, o null se il rifiuto è un altro.
     */
    fun typedIn(text: String?): String? = text?.let { TYPED.find(it)?.groupValues?.get(1) }
}
