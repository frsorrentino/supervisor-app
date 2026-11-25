package it.pixelbox.cmwatch.rules

/** Quale Transport usare: Firebase solo se accoppiato, con la chiave nel vault e Firebase avviato (dal telefono o dalla build); altrimenti il finto. */
object TransportChoice {
    enum class Kind { FAKE, FIREBASE }
    /** `demo`: la demo per i video, accesa solo via adb (Franz, 16/09 16:05); vince anche da accoppiati, l'accoppiamento resta. */
    fun pick(paired: Boolean, hasKey: Boolean, firebase: Boolean, demo: Boolean = false): Kind =
        if (!demo && linked(paired, hasKey, firebase)) Kind.FIREBASE else Kind.FAKE

    /**
     * L'accoppiamento regge: accoppiato, con la chiave che si apre e Firebase avviato. Le schermate usano questo, non il solo
     * `paired` delle preferenze: accoppiato a metà, l'orologio mostrava in silenzio i dati della demo (app-orologio-dati-demo-25-11).
     */
    fun linked(paired: Boolean, hasKey: Boolean, firebase: Boolean): Boolean = paired && hasKey && firebase
}
