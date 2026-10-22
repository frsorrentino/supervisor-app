package it.pixelbox.cmwatch.rules

/**
 * Niente emoji nell'app e nelle notifiche (Franz, 22/10 13:36: «solo svg o fontawesome»): i testi che arrivano dal relay
 * (titoli e corpi degli eventi, anche da un relay prima dell'1.78) passano da qui prima di essere mostrati. Vanno via i
 * pittogrammi, i simboli e i dingbat (✓ ✗ ❓ ⚠ ⛔), gli orologi e i comandi (⏸ ⏺), le forme con presentazione emoji
 * (▶ ◀ ◾), i selettori di variante, lo ZWJ, i keycap e i tag delle bandiere; restano lettere, numeri, frecce («→» «↳»),
 * «·» e la punteggiatura. Gli spazi intorno a un simbolo tolto si chiudono, i «·» rimasti ai bordi cadono.
 * Lo stesso filtro sta in `web/src/lib/emoji.ts`.
 */
object Emoji {
    private fun drop(cp: Int): Boolean = when (cp) {
        in 0x1F000..0x1FAFF, in 0x2600..0x27BF, in 0x2B00..0x2BFF -> true
        in 0x231A..0x231B, in 0x23E9..0x23F3, in 0x23F8..0x23FA -> true
        0x25AA, 0x25AB, 0x25B6, 0x25C0, in 0x25FB..0x25FE -> true
        0x203C, 0x2049, 0x2139, 0x24C2, 0x2934, 0x2935, 0x3030, 0x303D, 0x3297, 0x3299 -> true
        0xFE0E, 0xFE0F, 0x200D, 0x20E3, in 0xE0020..0xE007F -> true
        else -> false
    }

    fun strip(text: String): String {
        if (text.none { it.code >= 0x2000 }) return text
        val sb = StringBuilder(text.length)
        var dropped = false
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (drop(cp)) { dropped = true; continue }
            if (dropped) {
                dropped = false
                if (cp == ' '.code && (sb.isEmpty() || sb.last() == ' ')) continue
                if (cp.toChar() in CLOSERS && sb.isNotEmpty() && sb.last() == ' ') sb.setLength(sb.length - 1)
            }
            sb.appendCodePoint(cp)
        }
        return sb.toString().trim().trim('·', ' ')
    }

    private const val CLOSERS = ",.;:!?)"
}
