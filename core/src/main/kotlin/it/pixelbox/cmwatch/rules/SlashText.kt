package it.pixelbox.cmwatch.rules

/**
 * Un comando nudo come testo di sblocco («/clear») diventa una frase col comando fra parentesi (Franz, 15/10 22:05:
 * «Rendi leggibile /clear»). Solo i comandi noti; il resto resta com'è. Quello che parte resta il comando. Stessa regola
 * di slashText.ts nella web.
 */
object SlashText {
    val KNOWN = listOf("clear", "compact", "login", "resume", "exit", "context", "cost")
    private val COMMAND = Regex("^/([a-z-]+)(\\s.*)?$")

    /** Il comando noto, se il testo è solo quello (con eventuali argomenti); altrimenti null. */
    fun known(text: String): String? = COMMAND.find(text.trim())?.groupValues?.get(1)?.takeIf { it in KNOWN }

    /**
     * La frase leggibile, «svuotare il contesto (/clear)»; `phrase` dà la frase del comando nella lingua dell'app.
     * Dopo la barra un word joiner (U+2060): senza, Android andava a capo fra «(/» e «clear)» (golden del 16/10).
     */
    fun readable(text: String, phrase: (String) -> String): String =
        known(text)?.let { "${phrase(it)} (${text.trim().replaceFirst("/", "/$JOIN")})" } ?: text

    const val JOIN = "\u2060"

    /** Le righe «Aspetta» del rapporto («!: /clear»): la parte dopo il prefisso, leggibile. */
    fun readableWaiting(line: String, phrase: (String) -> String): String {
        val m = Regex("^(!|ok|domanda): (.*)$").find(line) ?: return readable(line, phrase)
        return "${m.groupValues[1]}: ${readable(m.groupValues[2], phrase)}"
    }
}
