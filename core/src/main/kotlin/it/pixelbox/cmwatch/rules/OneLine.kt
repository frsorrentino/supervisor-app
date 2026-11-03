package it.pixelbox.cmwatch.rules

/**
 * Il riassunto a destra delle righe della home e delle testate sta su una riga (Franz 03/11 08:07, variante 1,
 * app-riassunti-una-riga-03-11). Le parti vanno in ordine di importanza; quelle che non entrano cadono intere, dall'ultima; se non
 * entra nemmeno la prima resta il suo numero. Mai parole spezzate, mai puntini. La riga aperta mostra tutte le parti.
 * Le stesse regole in web/src/lib/oneLine.ts.
 */
object OneLine {
    const val SEP = " · "

    private val NUMBER = Regex("""[-+]?\d+(?:[.,]\d+)?\s?%?""")

    /** Le parti di un riassunto già scritto coi separatori. */
    fun parts(s: String): List<String> = s.split(SEP).map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Quante parti entrano in `max`: `width(k)` è la larghezza delle prime k unite coi separatori. 0 = nemmeno la prima, e allora
     * si mostra [short] della prima.
     */
    fun count(n: Int, max: Float, width: (Int) -> Float): Int {
        var k = n
        while (k > 0 && width(k) > max) k--
        return k
    }

    /** La prima parte ridotta al suo numero; senza un numero resta intera. */
    fun short(part: String): String = NUMBER.find(part)?.value?.trim() ?: part
}
