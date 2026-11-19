package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Freshness

/**
 * La card della master chiusa (app-master-avvia-19-11, S1 dell'analisi avvio-master-2026-11-19): «Avvia la master» con un
 * tocco, anche dopo /exit o su un'installazione nuova; con lo stato del PC vecchio (oltre 3 battiti, [Freshness.Stale])
 * dice che il PC è spento o WSL è fermo, invece di un tasto che nessuno riceve.
 */
object MasterLaunch {
    enum class Kind { LAUNCH, STARTING, FAILED, PC_OFF }
    data class Card(val kind: Kind, val error: String? = null) {
        /** Il tasto c'è quando il tocco ha qualcuno che lo riceve e non è già partito. */
        val button: Boolean get() = kind == Kind.LAUNCH || kind == Kind.FAILED
    }

    /** Quanto resta «Si sta avviando» dopo il tocco senza che la master compaia nello stato: poi si può riprovare. */
    const val STARTING_S = 120L

    /** [sentAt] l'ora del tocco (s), [refused] il testo del relay se l'ha rifiutato. */
    fun card(freshness: Freshness?, sentAt: Long?, refused: String?, now: Long): Card = when {
        freshness == null || freshness is Freshness.Stale -> Card(Kind.PC_OFF)
        refused != null -> Card(Kind.FAILED, refused)
        sentAt != null && now - sentAt < STARTING_S -> Card(Kind.STARTING)
        else -> Card(Kind.LAUNCH)
    }
}
