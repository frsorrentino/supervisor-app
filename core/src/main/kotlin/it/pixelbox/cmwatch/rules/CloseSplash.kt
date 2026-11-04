package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Il pannello di chiusura di una sessione (Franz, 08/10 18:06-18:29, variante A): la pagina non sparisce più. Dopo /exit
 * un pannello sopra la pagina dice che si sta chiudendo, con i passi veri (consegnato, confermato dal PC); poi che è chiusa
 * e a che ora, con «Riapri» e «Torna alla home», e dopo [CLOSED_SHOW_MS] si torna alla home. Gli stessi valori nella web.
 */
object CloseSplash {
    /** Quanto resta il pannello «è chiusa» prima del ritorno alla home. */
    const val CLOSED_SHOW_MS = 3_000L
    /** Oltre questi secondi senza conferma il pannello dice che ci sta mettendo più del solito. */
    const val SLOW_S = 20L
    /** Contratto 1.120: «Chiudi comunque» si chiude a fine turno o entro 180 s; «più del solito» solo dopo. */
    const val FORCE_S = 180L
    /** I motivi per cui il relay arma una chiusura (`close` con when after o force): mai una attaccata o che non gira. */
    val ARMABLE = setOf("busy", "question", "background")

    /**
     * La chiusura chiesta da qui: quale sessione, quando, con quale comando; [refused] è il motivo del rifiuto del relay, già
     * in chiaro (app-chiudi-esito-04-11), e [armable] se si può chiudere quando finisce o comunque (contratto 1.120);
     * [force]: la chiusura è un «Chiudi comunque».
     */
    data class Leaving(val name: String, val sentAt: Long, val cmd: String?, val refused: String? = null, val armable: Boolean = false, val force: Boolean = false)

    sealed interface Phase {
        data class Closing(val name: String, val sentAt: Long, val delivered: Boolean, val slow: Boolean, val force: Boolean = false) : Phase
        data class Closed(val name: String, val at: Long, val byMe: Boolean) : Phase
        data class Refused(val name: String, val why: String, val armable: Boolean = false) : Phase
    }

    /**
     * La fase per la pagina di [name]; null = niente pannello. [lastSeen] è la sessione com'era l'ultima volta nello stato,
     * per l'ora di chiusura quando non c'è più; [delivered] dice se il PC ha risposto al comando di chiusura.
     * Chiusa per conto suo ma ancora nello stato resta una pagina con «Riapri» (03/10 19:57): il pannello arriva solo
     * quando esce dallo stato.
     */
    fun of(name: String, sessions: List<Session>, leaving: Leaving?, lastSeen: Session?, now: Long, delivered: Boolean = false): Phase? {
        val s = sessions.firstOrNull { it.name == name }
        val mine = leaving?.name == name
        // Per una gone `since` è l'ultimo avvistamento, cioè quando si è chiusa; per una viva no: allora vale adesso.
        val at = s?.since ?: lastSeen?.takeIf { it.state == SessionState.GONE }?.since ?: now
        return when {
            s == null -> Phase.Closed(name, at, byMe = mine)
            s.state == SessionState.GONE -> if (mine) Phase.Closed(name, at, byMe = true) else null
            // Il Chiudi dà sempre un esito (Franz, 04/11): rifiutata, il pannello dice perché finché non lo togli.
            mine && leaving!!.refused != null -> Phase.Refused(name, leaving.refused!!, leaving.armable)
            // Dal vivo 08/10 19:29: dopo /exit Claude Code può chiedere cosa fare dei lavori in background; il pannello non
            // copre la domanda, e torna quando è risposta.
            mine && s.question != null -> null
            mine -> Phase.Closing(name, leaving!!.sentAt, delivered, slow = now - leaving.sentAt >= (if (leaving.force) FORCE_S + SLOW_S else SLOW_S), force = leaving.force)
            else -> null
        }
    }

    /** Una riga «<nome> not closed: <motivo>» del relay (op close e /exit); [kind] è la chiave di `blocked_by`, null se nuova. */
    data class Refusal(val name: String, val kind: String?, val reason: String) {
        /** Contratto 1.120: si può chiudere quando finisce o comunque. */
        val armable get() = kind in ARMABLE
    }

    private val NOT_CLOSED = Regex("""(?:^|\s)(\S+) not closed: (.+)$""")

    /** Il motivo del relay come una delle chiavi di `close_preview.blocked_by`; null se non lo riconosce. Come la web. */
    fun refusalKind(reason: String): String? = when {
        reason.contains("background", ignoreCase = true) -> "background"
        reason.contains("question", ignoreCase = true) -> "question"
        reason.contains("working", ignoreCase = true) || reason.contains("busy", ignoreCase = true) -> "busy"
        Regex("attached|terminal|open at the PC", RegexOption.IGNORE_CASE).containsMatchIn(reason) -> "attached"
        else -> null
    }

    /** Le sessioni che il relay non ha chiuso, dal testo del risultato. */
    fun refusals(text: String): List<Refusal> = text.lines().mapNotNull { l ->
        NOT_CLOSED.find(l.trim())?.let { m -> Refusal(m.groupValues[1], refusalKind(m.groupValues[2]), m.groupValues[2]) }
    }
}
