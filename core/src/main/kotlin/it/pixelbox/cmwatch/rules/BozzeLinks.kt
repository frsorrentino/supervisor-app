package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptEntry
import it.pixelbox.cmwatch.contract.TranscriptFile

/**
 * Le bozze citate in una risposta (app-bozze-link-19-11, Franz 19/11 17:27: «così come i riferimenti alle prove hanno un link,
 * anche le bozze»): «le tre bozze», «la bozza», «bozza A», «bozze A e B» e le voci d'elenco «A, riepilogo…:» diventano link alle
 * immagini (o agli HTML) mandate nello stesso turno. Una lettera apre il file che porta quella lettera nel nome (`scheda-a.png`),
 * le altre parole il primo. Senza file visivi nel turno niente link. Lo stesso in web/src/lib/bozzeLinks.ts.
 */
object BozzeLinks {
    /** Un riferimento: dove sta nel testo e quale file apre (indice in `files`). */
    data class Ref(val range: IntRange, val file: Int)

    private val WORD = Regex(
        """(?<!\p{L})(?:[Ll]e\s+(?:(?:due|tre|quattro|cinque|sei|sette|nuove|altre|prime)\s+)?|[Ll]a\s+|[Uu]na\s+)?[Bb]ozz[ae](?!\p{L})""" +
            """(?:\s+([A-H](?:\s*(?:,|/|\be\b|\bed\b)\s*[A-H])*)(?![\p{L}\d]))?""",
    )
    private val LETTER = Regex("""[A-H]""")
    // Una voce d'elenco che comincia con la lettera: «A, riepilogo e ultimi turni:» o «B: un grafico»; il link copre fino ai due punti.
    private val HEAD = Regex("""(?m)^[ \t]*(?:[-*•][ \t]+)?([A-H])(?=[,:][ \t])([^:\n]{0,60}:)?""")

    private val VISUAL_EXT = setOf("png", "jpg", "jpeg", "webp", "gif", "svg", "html", "htm")
    fun visual(f: TranscriptFile): Boolean =
        f.mime?.let { it.startsWith("image/") || it == "text/html" } == true || f.path.substringAfterLast('.', "").lowercase() in VISUAL_EXT

    /** La lettera di un file dal nome: `scheda-a.png`, `bozza_B.html`, `c.png`; null se non ne porta una. */
    fun letterOf(path: String): Char? {
        val stem = path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
        val m = Regex("""(?:^|[-_ .])([A-Ha-h])$""").find(stem) ?: return null
        return m.groupValues[1].uppercase()[0]
    }

    /** I file visivi del turno di [index]: quelli dei passi dall'ultimo messaggio della persona fino a [index] compreso. */
    fun turnFiles(entries: List<TranscriptEntry>, index: Int): List<Pair<TranscriptFile, String>> {
        if (index !in entries.indices || entries[index].role == "user") return emptyList()
        var start = index
        while (start > 0 && entries[start - 1].role != "user") start--
        return (start..index).flatMap { i -> entries[i].files.orEmpty().filter(::visual).map { it to entries[i].id } }
    }

    fun find(text: String, files: List<String>): List<Ref> {
        if (files.isEmpty()) return emptyList()
        val letters = files.map(::letterOf)
        fun fileOf(c: Char?): Int? = if (c == null) 0 else letters.indexOf(c).takeIf { it >= 0 }
        val out = mutableListOf<Ref>()
        WORD.findAll(text).forEach { m ->
            val g = m.groups[1]
            if (g == null) { out += Ref(m.range, 0); return@forEach }
            // «bozze A e B»: la parola con la prima lettera, poi una lettera per link.
            LETTER.findAll(g.value).forEachIndexed { i, x ->
                val at = g.range.first + x.range.first
                val f = fileOf(x.value[0]) ?: return@forEachIndexed
                out += Ref((if (i == 0) m.range.first else at)..at, f)
            }
        }
        HEAD.findAll(text).forEach { m ->
            val c = m.groupValues[1][0]
            val f = fileOf(c) ?: return@forEach
            val r = m.groups[1]!!.range.first..(m.groups[2]?.range?.last?.minus(1) ?: m.groups[1]!!.range.last)
            if (out.none { o -> o.range.first <= r.last && r.first <= o.range.last }) out += Ref(r, f)
        }
        return out.sortedBy { it.range.first }
    }
}
