package it.pixelbox.cmwatch.rules

/**
 * Lo zoom del testo della conversazione con due dita (Franz, 03/10 21:16): un fattore sulla grandezza dei caratteri, che
 * vanno a capo da soli; ricordato fra un'apertura e l'altra.
 */
object ChatZoom {
    const val MIN = 0.85f
    const val MAX = 1.6f

    fun clamp(z: Float): Float = if (z.isFinite()) z.coerceIn(MIN, MAX) else 1f

    /** Il valore salvato; senza valore o con uno rovinato, la grandezza normale. */
    fun of(saved: Float?): Float = saved?.takeIf { it.isFinite() && it > 0f }?.let(::clamp) ?: 1f

    /** Un rettangolo in pixel della finestra: dove sta un'immagine della chat. */
    data class Area(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        operator fun contains(p: Pair<Float, Float>): Boolean = p.first in left..right && p.second in top..bottom
    }

    /**
     * Chi possiede un gesto a due dita (app-zoom-testo-02-11, Franz 02/11 23:59: «va in conflitto con lo zoom del testo»): il
     * pizzico che comincia su un'immagine è dell'immagine, quello sul testo è del testo. `downs`: dove è sceso ogni dito, in pixel
     * della finestra. Basta un dito sceso su un'immagine perché il testo lasci stare il gesto.
     */
    fun textOwns(downs: List<Pair<Float, Float>>, images: Collection<Area>): Boolean = downs.none { d -> images.any { d in it } }
}
