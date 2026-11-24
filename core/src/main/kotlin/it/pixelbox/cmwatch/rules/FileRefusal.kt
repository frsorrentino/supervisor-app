package it.pixelbox.cmwatch.rules

/**
 * Il rifiuto del PC per un file della chat (contratto 1.24): «too large: <byte>», con la 1.34 anche « max <byte>».
 * Franz, 04/10 21:55: l'app mostrava i byte nudi («too large: 3630371»); così mostra la misura e, se c'è, il limite.
 */
object FileRefusal {
    data class TooLarge(val bytes: Long, val max: Long?)

    private val TOO_LARGE = Regex("""too large:\s*(\d+)(?:\s*max\s*(\d+))?""", RegexOption.IGNORE_CASE)

    fun tooLarge(reason: String): TooLarge? =
        TOO_LARGE.find(reason)?.let { m -> TooLarge(m.groupValues[1].toLong(), m.groupValues[2].toLongOrNull()) }

    /**
     * Il file non c'è più sul PC (app-file-mancante-24-11, Franz 24/11 21:08: «cos'è questo missing or unreadable che mi appare
     * spesso?»): citato nella conversazione e poi spostato o cancellato. Il relay lo dice con «missing or unreadable» (un OSError in
     * `file_open`). Non torna da solo: l'app lo segna non disponibile, non lo richiede e lo dice in italiano col nome, solo al tocco.
     */
    fun gone(reason: String): Boolean = GONE.containsMatchIn(reason)

    private val GONE = Regex("""missing or unreadable|no such file""", RegexOption.IGNORE_CASE)

    /** Il nome che si legge nell'avviso: l'ultima parte del percorso. */
    fun name(path: String): String = path.trimEnd('/').substringAfterLast('/').ifEmpty { path }
}
