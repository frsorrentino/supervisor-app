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
}
