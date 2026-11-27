package it.pixelbox.cmwatch.rules

/**
 * Quando la chat insegue il fondo (app-apertura-master-scatti-26-11, Franz 26/11 23:34: «lo scorrimento del testo della master non
 * parte più al primo tocco»). Mai mentre un dito è sulla lista, mai durante uno scorrimento, solo se si stava seguendo e sotto c'è
 * ancora qualcosa. Il dito appoggiato ferma l'inseguimento subito, anche prima che il gesto diventi un trascinamento.
 */
object ChatFollow {
    fun chase(follow: Boolean, below: Boolean, items: Int, scrolling: Boolean, touching: Boolean): Boolean =
        follow && below && items > 0 && !scrolling && !touching

    /** Al rilascio si torna a seguire solo se si è in fondo. */
    fun afterRelease(atEnd: Boolean): Boolean = atEnd

    /**
     * app-scroll-master-lavoro-27-11 (video di Franz, 27/11 04:16, scelta (a) della master): mentre si rilegge nessuno scorrimento
     * automatico «porta in vista» muove la chat. Si rilegge quando la chat non segue il fondo o non è in fondo, e sempre con un dito
     * giù o uno slancio in corso. Passa solo la richiesta che segue di poco un tocco senza trascinamento (un tasto nella risposta) o un
     * tasto della tastiera con il fuoco nella chat.
     */
    fun holdBringIntoView(follow: Boolean, atEnd: Boolean, touching: Boolean, flinging: Boolean, sinceTapMs: Long): Boolean =
        touching || flinging || ((!follow || !atEnd) && sinceTapMs > TAP_MS)

    /** Quanto dopo un tocco una richiesta di «porta in vista» è sua: il tempo di finire l'animazione che il tocco ha chiesto. */
    const val TAP_MS = 1_000L
}
