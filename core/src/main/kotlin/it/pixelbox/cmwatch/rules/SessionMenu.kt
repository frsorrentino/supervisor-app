package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.DecisionItem
import it.pixelbox.cmwatch.contract.DecisionsPage
import it.pixelbox.cmwatch.contract.ProvePage
import it.pixelbox.cmwatch.contract.SeguitiPage
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * Il menu della sessione, uno solo per home, chat e plancia larga (Franz 03/11 15:58, app-menu-sessione-03-11: «allineiamo questi
 * menu?»). Qui l'ordine e quali voci ci sono; i testi e le icone li mette chi disegna. Le voci vuote non compaiono.
 */
object SessionMenu {
    enum class Kind { CHANGES, PROVE, DECIDI, GUARDA, SEGUITI, FOLLOW, ASK_MASTER, OPEN_IN_CLAUDE, TERMINAL, RESTART_CLEAN, EXIT }

    /** Una voce; `n` è il numero accanto al titolo per le voci di Tocca a te. */
    data class Item(val kind: Kind, val n: Int = 0)

    /** Le voci di Tocca a te che riguardano la sessione, nell'ordine della categoria (Prova, Decidi, Guarda, Segui). */
    data class Mine(val prove: Int = 0, val decidi: Int = 0, val guarda: Int = 0, val seguiti: Int = 0)

    /**
     * I risultati non visti della sessione, da `state.results` (al massimo 10 voci nello stato: è un minimo, non il totale).
     * Basta finché il relay non porta i numeri per sessione.
     */
    fun unseenResults(st: State, name: String): Int = st.results?.items?.count { it.session == name && it.seenAt == null } ?: 0

    /** Una pausa che aspetta una prova (contratto 1.107): sta in Prova, non in Decidi. */
    private fun isTest(i: DecisionItem) = i.kind == "pause" && i.prova != null

    /**
     * I numeri di Tocca a te della sessione dalle pagine che l'app ha già letto (null = non letta: quella voce non compare) e dai
     * risultati dello stato.
     */
    fun mine(st: State, name: String, prove: ProvePage?, decisions: DecisionsPage?, seguiti: SeguitiPage?): Mine {
        val items = decisions?.groups.orEmpty().flatMap { it.items }.filter { it.session == name }
        val proveItems = prove?.groups.orEmpty().flatMap { it.prove }.filter { it.session == name }.map { it.task }
        val proveTasks = (proveItems + items.filter(::isTest).map { it.task ?: "" }).filter { it.isNotEmpty() }.toSet()
        return Mine(
            prove = proveTasks.size,
            decidi = items.count { !isTest(it) },
            guarda = unseenResults(st, name),
            seguiti = seguiti?.rimasto.orEmpty().count { it.session == name },
        )
    }

    /**
     * @param changes la sessione ha modifiche da mostrare e la riga delle pillole non le tiene (solo dove c'è quella riga)
     * @param terminal il terminale dal telefono, dove serve ancora
     * @param canExit il PC accetta /exit dall'app
     */
    fun entries(s: Session, st: State, mine: Mine = Mine(), changes: Boolean = false, terminal: Boolean = false, canExit: Boolean = false): List<Item> {
        val live = s.state != SessionState.GONE
        return buildList {
            if (changes) add(Item(Kind.CHANGES))
            if (mine.prove > 0) add(Item(Kind.PROVE, mine.prove))
            if (mine.decidi > 0) add(Item(Kind.DECIDI, mine.decidi))
            if (mine.guarda > 0) add(Item(Kind.GUARDA, mine.guarda))
            if (mine.seguiti > 0) add(Item(Kind.SEGUITI, mine.seguiti))
            add(Item(Kind.FOLLOW))
            if (s.name != ContextActions.MASTER && ContextActions.master(st) != null) add(Item(Kind.ASK_MASTER))
            if (s.link.isNotBlank()) add(Item(Kind.OPEN_IN_CLAUDE))
            if (terminal) add(Item(Kind.TERMINAL))
            // Riparti pulito: l'handoff col comando della sessione, poi /clear a turno finito (il «Handoff, poi /clear» del foglio del contesto).
            if (live && ContextActions.canClear(st) && ContextActions.handoffPrompt(st, s) { it } != null) add(Item(Kind.RESTART_CLEAN))
            if (live && canExit) add(Item(Kind.EXIT))
        }
    }
}
