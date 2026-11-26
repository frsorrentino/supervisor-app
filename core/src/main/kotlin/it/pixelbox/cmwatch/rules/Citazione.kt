package it.pixelbox.cmwatch.rules

/**
 * Le citazioni nei messaggi (app-citazioni-estetica-26-11, Franz 26/11 15:46: «miglioriamo l'estetica delle citazioni»): le righe di
 * fila che cominciano con «>» diventano un blocco di citazione, senza il segno; il resto resta testo. Nel fumetto di Franz il «>» si
 * vedeva grezzo. Uguale sul web (web/src/lib/citazione.ts).
 */
object Citazione {
    data class Part(val quote: Boolean, val text: String)

    /** Righe mostrate di una citazione chiusa; oltre, «altro». */
    const val LINES = 3

    private val MARK = Regex("^\\s{0,3}>\\s?")

    fun parts(text: String): List<Part> {
        val out = mutableListOf<Part>()
        val buf = StringBuilder()
        var inQuote: Boolean? = null
        fun flush() {
            val t = buf.toString().trim('\n')
            if (inQuote != null && t.isNotBlank()) out += Part(inQuote!!, t)
            buf.clear()
        }
        for (line in text.lines()) {
            val q = MARK.containsMatchIn(line)
            if (inQuote != q) { flush(); inQuote = q }
            if (buf.isNotEmpty()) buf.append('\n')
            buf.append(if (q) line.replaceFirst(MARK, "") else line)
        }
        flush()
        return out
    }

    /** Il testo senza segni di citazione, per la voce e per la copia. */
    fun plain(text: String): String = parts(text).joinToString("\n\n") { it.text }
}
