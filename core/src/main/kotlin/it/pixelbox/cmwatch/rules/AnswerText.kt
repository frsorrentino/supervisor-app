package it.pixelbox.cmwatch.rules

/**
 * La risposta intera della sessione (contratto 1.4, `last`) divisa in blocchi da leggere e da ascoltare uno per volta
 * (Franz, 15/09 17:19: «non solo sentire tutto ma anche sentire per blocchi/paragrafi»). Paragrafi separati da una riga
 * vuota, ogni voce d'elenco un blocco, titoli markdown o righe tutte in grassetto come titoli, blocchi ``` come codice.
 * Il grassetto si toglie; i `backtick` restano, li disegna l'interfaccia in mono. Un blocco ```text è testo da copiare
 * (una email, un modulo: Franz, 25/11 22:53), `TEXT`: si mostra come il codice, con il tasto copia, ma la voce lo legge.
 */
object AnswerText {
    enum class Kind { PARA, BULLET, HEADING, CODE, TEXT }

    /** Le lingue di un blocco ``` che dicono «testo da copiare», non codice. */
    private val TESTO = setOf("text", "txt", "plain", "plaintext")

    /** `text` per un'apertura ```text: il blocco è testo da copiare. */
    fun isCopyText(fence: String): Boolean = fence.trim().removePrefix("```").trim().lowercase() in TESTO

    data class Block(val kind: Kind, val text: String)

    private val TITOLO = Regex("^#{1,6}\\s+(.+)$")
    private val GRASSETTO = Regex("^\\*\\*(.+?)\\*\\*:?$")
    private val VOCE = Regex("^\\s*(?:[-*•]|\\d+[.)])\\s+(.+)$")
    private val ENFASI = Regex("\\*\\*|__")

    fun blocks(text: String): List<Block> {
        val out = mutableListOf<Block>()
        val para = StringBuilder()
        var code: StringBuilder? = null
        var codeKind = Kind.CODE
        var afterBullet = false
        fun flush() {
            if (para.isNotBlank()) out += Block(Kind.PARA, pulito(para.toString()))
            para.clear()
        }
        for (raw in text.lines()) {
            val line = raw.trimEnd()
            val c = code
            if (c != null) {
                if (line.trimStart().startsWith("```")) { out += Block(codeKind, c.toString().trimEnd('\n')); code = null }
                else c.append(line).append('\n')
                continue
            }
            val t = line.trim()
            val voce = VOCE.find(line)
            when {
                t.startsWith("```") -> { flush(); code = StringBuilder(); codeKind = if (isCopyText(t)) Kind.TEXT else Kind.CODE; afterBullet = false }
                t.isEmpty() -> { flush(); afterBullet = false }
                TITOLO.matches(t) -> { flush(); out += Block(Kind.HEADING, pulito(TITOLO.find(t)!!.groupValues[1])); afterBullet = false }
                GRASSETTO.matches(t) -> { flush(); out += Block(Kind.HEADING, pulito(GRASSETTO.find(t)!!.groupValues[1])); afterBullet = false }
                voce != null -> { flush(); out += Block(Kind.BULLET, pulito(voce.groupValues[1])); afterBullet = true }
                // Una riga rientrata sotto una voce d'elenco la continua.
                afterBullet && raw.startsWith(" ") -> { val b = out.removeAt(out.lastIndex); out += b.copy(text = b.text + " " + pulito(t)) }
                else -> { if (para.isNotEmpty()) para.append(' '); para.append(t); afterBullet = false }
            }
        }
        code?.let { if (it.isNotBlank()) out += Block(codeKind, it.toString().trimEnd('\n')) }
        flush()
        return out.filter { it.text.isNotBlank() }
    }

    private fun pulito(t: String) = t.replace(ENFASI, "").trim()
}
