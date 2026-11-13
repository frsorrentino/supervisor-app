package it.pixelbox.cmwatch.rules

/**
 * La risposta come documento (app-chat-scheda-unica-13-11), gemella di web/src/lib/answer.ts: il testo diviso dove lo divide la
 * sessione, cioè ai suoi titoli; dentro, paragrafi, voci d'elenco e blocchi di codice. Le parole restano quelle della sessione.
 */
object Answer {
    data class Section(val title: String, val body: String)
    data class Doc(val lead: String, val secs: List<Section>)

    sealed interface Rich {
        data class P(val text: String) : Rich
        data class Li(val mark: String, val text: String, val depth: Int) : Rich
        data class Code(val text: String) : Rich
    }

    private val HEAD = Regex("^#{1,6}\\s+(.*)$")
    private val ITEM = Regex("^(\\s*)([-*•]|\\d+[.)])\\s+(.*)$")
    private val LEADING = Regex("^\\n+")
    private val INDENTED = Regex("^\\s+\\S")
    private fun fence(l: String) = l.trimStart().startsWith("```")
    private fun clean(lines: List<String>) = lines.joinToString("\n").replace(LEADING, "").trimEnd()

    /** Quello che sta prima del primo titolo e una sezione per titolo; i `#` dentro un blocco di codice non sono titoli. */
    fun sections(text: String): Doc {
        val lead = mutableListOf<String>()
        val secs = mutableListOf<Pair<String, MutableList<String>>>()
        var cur = lead
        var code = false
        text.split('\n').forEach { l ->
            if (fence(l)) code = !code
            val h = if (code) null else HEAD.find(l)
            if (h != null) { val lines = mutableListOf<String>(); secs += h.groupValues[1].trim() to lines; cur = lines } else cur += l
        }
        return Doc(clean(lead), secs.map { (t, lines) -> Section(t, clean(lines)) })
    }

    /** Un testo senza titoli a pezzi: paragrafi (una riga vuota li separa), voci d'elenco col loro segno, blocchi di codice chiusi. */
    fun rich(text: String): List<Rich> {
        val out = mutableListOf<Rich>()
        val lines = text.split('\n')
        var para = mutableListOf<String>()
        fun flush() { if (para.isNotEmpty()) out += Rich.P(para.joinToString("\n")); para = mutableListOf() }
        var i = 0
        while (i < lines.size) {
            val l = lines[i]
            if (fence(l)) {
                val end = (i + 1 until lines.size).firstOrNull { fence(lines[it]) }
                if (end != null) {
                    flush()
                    val body = lines.subList(i + 1, end).joinToString("\n")
                    if (body.isNotBlank()) out += Rich.Code(body)
                    i = end + 1
                    continue
                }
            }
            val m = ITEM.find(l)
            val last = out.lastOrNull()
            when {
                m != null -> {
                    flush()
                    val sign = m.groupValues[2]
                    out += Rich.Li(if (sign[0].isDigit()) "${sign.dropLast(1).toInt()}." else "•", m.groupValues[3], minOf(3, m.groupValues[1].length / 2))
                }
                l.isBlank() -> flush()
                // Una riga rientrata sotto una voce d'elenco continua la voce.
                para.isEmpty() && last is Rich.Li && INDENTED.containsMatchIn(l) && lines.getOrNull(i - 1)?.isNotBlank() == true ->
                    out[out.lastIndex] = last.copy(text = last.text + "\n" + l.trim())
                else -> para += l
            }
            i++
        }
        flush()
        return out
    }

    data class Tipo(val text: String, val tipo: String?)

    private val TIPO = Regex("^\\s*Tipo\\s*:\\s*(.*)$")

    /** La riga «Tipo: …» fra le righe di chiusura (Esito, Tipo, Prossimi) esce dal testo: è la pillola del piede. */
    fun stripTipo(text: String, label: String = "Esito"): Tipo {
        val closing = Regex("^\\s*(Esito|${Regex.escape(label)}|Tipo|Prossimi|Watch)\\s*:")
        val lines = text.split('\n')
        for (i in lines.indices.reversed()) {
            if (lines[i].isBlank()) continue
            if (!closing.containsMatchIn(lines[i])) break
            val m = TIPO.find(lines[i])
            if (m != null) return Tipo((lines.take(i) + lines.drop(i + 1)).joinToString("\n"), m.groupValues[1].trim().ifEmpty { null })
        }
        return Tipo(text, null)
    }
}
