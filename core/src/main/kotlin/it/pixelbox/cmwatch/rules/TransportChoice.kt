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

    /**
     * Le sessioni si mostrano solo se vengono dal PC o dalla demo scelta. Senza accoppiamento il Transport è comunque il finto:
     * tile e complicazione mostravano le sue fixture come vere anche con la demo spenta (app-orologio-dati-demo-25-11, 25/11 13:02).
     */
    fun showsData(paired: Boolean, hasKey: Boolean, firebase: Boolean, demo: Boolean): Boolean = demo || linked(paired, hasKey, firebase)

    /** Il codice a 6 cifre va al bus vero: senza Firebase il finto accettava qualunque codice e l'orologio si diceva accoppiato. */
    fun canPairWithCode(firebase: Boolean, debug: Boolean): Boolean = firebase || debug
}
