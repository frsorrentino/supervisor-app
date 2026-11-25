package it.pixelbox.cmwatch.rules

/**
 * Contratto 1.31 (Franz, 04/10 13:47: «pairing dal telefono senza PC»): i rifiuti di `pair_add` riconosciuti dal testo del
 * relay (inglese anche col relay in italiano, tranne la chiave mancante) e il tempo che resta all'invito.
 */
object PairAddText {
    enum class Refusal { BUSY, NO_KEY, FULL, FAILED, OTHER }

    private val FULL = Regex("""already (\d+) devices""")

    /** I dispositivi che il PC accetta (contratto 1.39, `PAIR_MAX_DEVICES`): otto, non più quattro. */
    const val MAX_DEVICES = 8

    /**
     * Quanti dispositivi ha già il PC, dal testo del rifiuto («already 8 devices»); senza numero, il massimo del contratto.
     * Il telefono diceva «quattro» anche col limite a otto (Franz 25/11 13:06, app-orologio-dati-demo-25-11).
     */
    fun devices(text: String?): Int = text?.let { FULL.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: MAX_DEVICES

    fun refusal(text: String): Refusal = when {
        "a pairing is already open" in text -> Refusal.BUSY
        text.startsWith("pair --add:") -> Refusal.NO_KEY
        FULL.containsMatchIn(text) -> Refusal.FULL
        text.startsWith("pairing not started") -> Refusal.FAILED
        else -> Refusal.OTHER
    }

    /** Il motivo dopo «pairing not started: », da mostrare com'è. */
    fun reason(text: String): String = text.substringAfter("pairing not started:", text).trim()

    /** «m:ss» fino alla scadenza, mai sotto zero. */
    fun left(exp: Long, now: Long): String {
        val s = (exp - now).coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }
}
