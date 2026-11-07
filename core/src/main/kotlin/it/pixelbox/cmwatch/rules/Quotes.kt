package it.pixelbox.cmwatch.rules

/**
 * Citare la trascrizione (app-citazione-trascrizione-07-11, Franz 07/11 20:06): i pezzi scelti con «Cita» o «Cita tutto» vanno
 * alla sessione in Markdown, ogni riga con «> », una riga vuota fra un pezzo e l'altro e sotto la risposta. Un pezzo più lungo di
 * MAX caratteri si taglia a fine parola con «…». La stessa regola sta in web/src/lib/quotes.ts; i casi in comune sono in
 * core/src/test/resources/quote-cases.json. Niente cambi al relay né al contratto: arriva come un prompt qualunque.
 */
object Quotes {
    const val MAX = 1000

    /** Il pezzo pulito e, se serve, tagliato: a fine parola se l'ultima parola sta negli ultimi 200 caratteri, se no secco. */
    fun cut(text: String): String {
        val t = text.replace("\r\n", "\n").trim()
        if (t.length <= MAX) return t
        val head = t.substring(0, MAX)
        val kept = if (t[MAX].isWhitespace()) head.trimEnd()
        else head.indexOfLast { it.isWhitespace() }.takeIf { it > MAX * 4 / 5 }?.let { i -> head.substring(0, i).trimEnd() } ?: head
        return kept + "…"
    }

    /** Il testo da mandare: i pezzi citati, poi la risposta; senza pezzi, la risposta com'è scritta. */
    fun markdown(quotes: List<String>, reply: String): String {
        val body = quotes.map(::cut).filter { it.isNotEmpty() }.joinToString("\n\n") { q ->
            q.lines().joinToString("\n") { l -> if (l.isBlank()) ">" else "> " + l.trimEnd() }
        }
        val r = reply.replace("\r\n", "\n").trim()
        return listOf(body, r).filter { it.isNotEmpty() }.joinToString("\n\n")
    }
}
