package it.pixelbox.cmwatch.rules

/**
 * Dove si apre l'app del telefono (restyling 30/09, dal vivo): un nuovo avvio parte sempre dalla Panoramica e senza
 * schede aperte; solo una rotazione (stato salvato) tiene la scheda e la sessione aperta.
 */
object StartRoute {
    /** OVERVIEW è il riepilogo unico (design 03/10): il nome resta per lo stato salvato. LAVORI è la pagina unica (22/10), al posto del Registro. */
    enum class Tab { OVERVIEW, LAVORI }

    fun tab(restored: Tab?): Tab = restored ?: Tab.OVERVIEW

    /**
     * La master non è una pagina come le altre sessioni (Franz, 03/10 16:30: «l'apertura della master dovrebbe rimanere su
     * home, con una specie di toggle»): chiunque la apra porta la home sulla sua chat.
     */
    fun masterAtHome(name: String?): Boolean = name == ContextActions.MASTER

    /** Dove porta una scelta del menu «Vai a» o un link: null la home, la master la sua chat nella home, le altre la loro scheda. */
    data class Go(val open: String?, val masterChat: Boolean)
    fun go(name: String?, master: String?): Go = when {
        name == null -> Go(null, false)
        name == master || masterAtHome(name) -> Go(null, true)
        else -> Go(name, false)
    }

    fun openSheet(freshLaunch: Boolean, restored: String?): String? = if (freshLaunch) null else restored

    /**
     * L'app ancora viva e riaperta dopo un'assenza lunga vale un avvio nuovo (revisione 30/09): si torna sulla
     * Panoramica. Un giro breve, come «Apri in Claude» e ritorno, lascia la scheda dov'era.
     */
    const val AWAY_RESET_MS = 10 * 60_000L

    fun resetOnReturn(awayMs: Long): Boolean = awayMs >= AWAY_RESET_MS
}
