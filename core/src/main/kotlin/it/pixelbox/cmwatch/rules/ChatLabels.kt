package it.pixelbox.cmwatch.rules

/**
 * Le etichette di una risposta in chat (Franz 01/11 21:24, «Riquadro e Non provato»). Solo se l'etichetta è esatta e a inizio riga,
 * in grassetto o no, con o senza i due punti. «Non provato» senza grassetto e senza due punti vale solo da solo sulla riga: «Non provato
 * il gesto» è una frase. Dentro un blocco di codice non si guarda. Un messaggio senza etichette resta un pezzo solo, identico.
 */
object ChatLabels {
    sealed interface Part
    data class Plain(val text: String) : Part
    /** «Da te» con l'elenco che lo segue. */
    data class ForYou(val title: String, val body: String) : Part
    /** Il testo intero, etichetta compresa. */
    data class NotTested(val text: String) : Part

    private const val YOU = "Da te|For you"
    private const val NOT = "Non provato|Not tested"
    private val HEAD = Regex("""^\s*(?:\*\*|__)?($YOU):?(?:\*\*|__)?:?\s*$""")
    private val NOT_BOLD = Regex("""^\s*(?:\*\*|__)(?:$NOT):?(?:\*\*|__)(?:\s|:|$).*$""")
    private val NOT_PLAIN = Regex("""^\s*(?:$NOT)(?::.*|\s*)$""")
    private val ITEM = Regex("""^\s*(?:[-*•]|\d+[.)])\s+.*""")
    private val FENCE = Regex("""^\s*```.*""")

    fun split(text: String): List<Part> {
        val lines = text.split('\n')
        val out = mutableListOf<Part>()
        val plain = mutableListOf<String>()
        fun flush() {
            val t = plain.joinToString("\n").trim('\n')
            if (t.isNotBlank()) out += Plain(t)
            plain.clear()
        }
        var fenced = false
        var i = 0
        while (i < lines.size) {
            val l = lines[i]
            if (FENCE.matches(l)) { fenced = !fenced; plain += l; i++; continue }
            val head = if (fenced) null else HEAD.matchEntire(l)
            var next = i + 1
            while (next < lines.size && lines[next].isBlank()) next++
            if (head != null && next < lines.size && ITEM.matches(lines[next])) {
                val body = mutableListOf<String>()
                var j = next
                while (j < lines.size) {
                    val b = lines[j]
                    if (b.isBlank()) {
                        var k = j + 1
                        while (k < lines.size && lines[k].isBlank()) k++
                        if (k < lines.size && ITEM.matches(lines[k])) { j = k; continue } else break
                    }
                    if (!ITEM.matches(b) && !b.startsWith(" ") && !b.startsWith("\t")) break
                    body += b; j++
                }
                flush(); out += ForYou(head.groupValues[1], body.joinToString("\n")); i = j; continue
            }
            if (!fenced && (NOT_BOLD.matches(l) || NOT_PLAIN.matches(l))) {
                val body = mutableListOf(l)
                var j = i + 1
                while (j < lines.size && lines[j].isNotBlank() && !FENCE.matches(lines[j])) body += lines[j++]
                flush(); out += NotTested(body.joinToString("\n")); i = j; continue
            }
            plain += l; i++
        }
        flush()
        return if (out.all { it is Plain }) listOf(Plain(text)) else out
    }
}
