package it.pixelbox.cmwatch.rules

/**
 * Il bordo della master (app-bordo-master-27-11; Franz 27/11 22:21: un Border Beam come quello di Magic UI, «Reverse», che distingua la
 * master dalle sessioni; dei tre posti proposti la master ha consigliato A, il campo di testo: dalla-master/bordo-master-2026-11-27/
 * bordo-master.html, con tutti i valori nella nota). Sul campo della master un anello fermo di 1,5 dp di [COLORE], al 12 % sempre; mentre
 * la master lavora (lo stesso «al lavoro» del suo glifo, [Rail.glyph]) un raggio di 220 dp corre lungo il bordo, un giro ogni 15 s, lineare,
 * verso antiorario, e si accende e si spegne in 240 ms. Con le animazioni ridotte niente giro: mentre lavora l'anello sale al 35 %.
 * Le stesse regole sul web (web/src/lib/bordoMaster.ts).
 * app-bordo-foglio-28-11 (Franz 28/11, variante B di dalla-master/bordo-foglio-28-11.html): anello e raggio corrono attorno a tutto il foglio
 * della master, alla riga, a metà e intero, invece che sul campo, che torna una pillola semplice come quello delle sessioni. Il bordo è più
 * lungo: il raggio da 320 dp. Il disegno sta in BordoMasterUi.kt, sul foglio (MasterSheetBox).
 */
object BordoMaster {
    /** Il colore del bordo: 20 % del verde dell'app (#71D083) e 80 % del grigio dei testi (#9B9B9B). */
    const val COLORE = 0xFF93A696L
    /** L'anello: 1,5 dp, sempre acceso e fermo. */
    const val SPESSORE_DP = 1.5f
    /** Il raggio degli angoli del campo, che è una pillola per la master e per le sessioni (il bordo del foglio segue i suoi angoli). */
    const val ANGOLO_DP = 24f
    /** L'anello da fermo, e mentre lavora con le animazioni ridotte. */
    const val ANELLO_FERMO = 0.12f
    const val ANELLO_RIDOTTO = 0.35f
    /** Il raggio: una fascia diritta di 320 × 24 dp lungo il bordo del foglio (era 220 sul campo), come la luce del mockup, che colora solo l'anello. */
    const val RAGGIO_DP = 320f
    const val FASCIA_DP = 24f
    /** Il punto più acceso del raggio, a un terzo della sua lunghezza da davanti: il punto che sta sul bordo a [posizione]. */
    const val CAPO = 0.35f
    /** La sfumatura del raggio da davanti a dietro, (punto, opacità del colore): da 0 sale al 50 % in un terzo, torna a 0 negli altri due. */
    val LUCE: List<Pair<Float, Float>> = listOf(0f to 0f, 0.18f to 0.22f, CAPO to 0.5f, 0.62f to 0.22f, 1f to 0f)
    /** Un giro ogni 15 s, lineare. */
    const val GIRO_MS = 15_000L
    /** Il raggio si accende e si spegne in 240 ms con la curva cubic-bezier(.23, 1, .32, 1). */
    const val SFUMA_MS = 240

    /** L'opacità dell'anello: 12 % sempre; mentre la master lavora con le animazioni ridotte, al posto del giro, il 35 %. */
    fun anello(lavora: Boolean, ridotto: Boolean): Float = if (lavora && ridotto) ANELLO_RIDOTTO else ANELLO_FERMO

    /** Il raggio gira solo mentre la master lavora, e mai con le animazioni ridotte. */
    fun gira(lavora: Boolean, ridotto: Boolean): Boolean = lavora && !ridotto

    /**
     * Dove sta il raggio al tempo [ms] (l'orologio, così web e telefono lo mostrano nello stesso punto del bordo): la frazione del bordo da
     * 0 a 1, misurata in senso orario dall'estremo sinistro del lato di sopra, dove comincia `rect()` in CSS. Col tempo cala: il raggio gira
     * verso antiorario, un giro ogni [GIRO_MS], lineare.
     */
    fun posizione(ms: Long): Float {
        val f = ms.mod(GIRO_MS).toFloat() / GIRO_MS
        return if (f == 0f) 0f else 1f - f
    }
}
