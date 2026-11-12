package it.pixelbox.cmwatch.rules

/**
 * Scelta della voce della lettura (Franz, 14/09 13:28: «si può avere una voce maschile?»). Android non dice se una voce
 * è maschile o femminile: le voci italiane del motore si provano una alla volta dalle impostazioni, e si tiene quella
 * che suona giusta. `null` è la voce predefinita del motore.
 */
object VoiceRules {
    /** La voce dopo `current`: dalla predefinita alla prima, poi in ordine, dopo l'ultima di nuovo la predefinita. */
    fun next(voices: List<String>, current: String?): String? {
        if (voices.isEmpty()) return null
        val i = voices.indexOf(current)
        return if (current == null || i < 0) voices.first() else voices.getOrNull(i + 1)
    }

    /** Posizione da mostrare: 0 per la predefinita o per una voce che non c'è più, altrimenti da 1 a n. */
    fun position(voices: List<String>, current: String?): Int = voices.indexOf(current) + 1

    /**
     * La voce da cui parte chi non ne ha mai scelta una (Franz, 12/11 16:34, con la schermata delle sue impostazioni: «voce di
     * lettura e velocità di lettura fossero di default le stesse che ho impostato io»): la sua, `it-it-x-itc-local` dei servizi
     * vocali Google, o la stessa di rete; dove il motore non l'ha, null = la predefinita del telefono.
     */
    const val PREFERRED = "it-it-x-itc"
    fun preferred(voices: List<String>): String? =
        voices.firstOrNull { it.equals("$PREFERRED-local", ignoreCase = true) } ?: voices.firstOrNull { it.equals("$PREFERRED-network", ignoreCase = true) }

    /**
     * La voce con cui si parte da quella salvata: mai scelta (`saved` null) la preferita, se si legge in italiano; vuota = è stata
     * scelta la predefinita del telefono; altrimenti quella salvata.
     */
    fun start(saved: String?, voices: List<String>, italian: Boolean = true): String? = when {
        saved == null -> if (italian) preferred(voices) else null
        saved.isEmpty() -> null
        else -> saved
    }

    /** Come si salva una scelta: la predefinita del telefono è una scelta (vuota), non l'assenza. */
    fun stored(choice: String?): String = choice ?: ""
}
