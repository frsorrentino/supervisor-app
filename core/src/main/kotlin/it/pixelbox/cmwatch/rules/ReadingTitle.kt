package it.pixelbox.cmwatch.rules

/**
 * Il titolo della lettura nel player di Android (app-player-lettura-03-11): la prima riga del testo senza segni del markdown,
 * fino alla fine della prima frase, al massimo 80 caratteri tagliati a una parola intera. Mai «…» (regole di forma, Franz 12/09).
 */
object ReadingTitle {
    const val MAX = 80

    fun of(text: String): String {
        val line = text.lineSequence().map { clean(it) }.firstOrNull { it.isNotBlank() } ?: return ""
        val sentence = Regex("""^(.+?[.!?])(\s|$)""").find(line)?.groupValues?.get(1) ?: line
        if (sentence.length <= MAX) return sentence
        val cut = sentence.take(MAX + 1)
        val space = cut.lastIndexOf(' ')
        return (if (space > MAX / 2) cut.take(space) else sentence.take(MAX)).trimEnd(',', ';', ':', ' ')
    }

    private fun clean(s: String) = s
        .replace(Regex("""^\s*(#{1,6}\s+|[-*+]\s+|>\s*|\d+\.\s+)"""), "")
        .replace(Regex("""[*_`~]+"""), "")
        .replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")
        .trim()
}
