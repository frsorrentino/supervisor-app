package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * I link `https://claude.ai/code/session_<id>` nei testi della chat (Franz, 21/10 19:04: «piuttosto che mandare il link
 * all'app claude possiamo mandare link o tasto che apra sessioni in app supervisor?»). Un link uguale al `link` di una
 * sessione dello stato esce dal testo e diventa il tasto «Apri in supervisor» col nome della sessione; la pressione lunga
 * apre il link com'era. Un link di sessione che lo stato non conosce resta un link normale. Un link con testo
 * `[officina](url)` lascia il suo testo. La riga che senza il link è solo un'etichetta («• **Link:**») sparisce, come le
 * parentesi rimaste vuote; i due punti rimasti in coda a una frase se ne vanno.
 *
 * Il deep link dell'app, per le notifiche e Telegram: `cmwatch://session/<nome>` apre la chat di quella sessione
 * (`master` compresa); sul web vale `?s=<nome>`.
 */
object SessionLinks {
    data class Button(val session: String, val url: String)
    data class Stripped(val text: String, val buttons: List<Button>)

    private val MD_LINK = Regex("""\[([^\]\n]+)]\((https?://[^)\s]+)\)""")
    private val EMPTY_PARENS = Regex("""\(\s*\)|«\s*»""")
    private val SPACES = Regex(""" {2,}""")
    private val ORPHAN = Regex("""\s+([.,;:!?»)])""")
    private const val EATEN = ".,;:!?"
    private val BULLET = Regex("""^(\s*)([-*•]|\d+[.)])\s+""")
    private val MARKS = Regex("""[*_`#]+""")
    private val TRAILING_COLON = Regex("""\s*:\s*([*_`]*)\s*$""")
    private const val LABEL_WORDS = 3

    /** La sessione il cui `link` è `url`, null se nessuna; la coda `?`, `#` e `/` non conta. */
    fun sessionOf(url: String, sessions: List<Session>): Session? {
        val key = norm(url)
        return sessions.firstOrNull { it.link.isNotBlank() && norm(it.link) == key }
    }

    /** Il testo senza i link delle sessioni note, e un tasto per sessione nell'ordine in cui compaiono. */
    fun strip(text: String, sessions: List<Session>): Stripped {
        if (!text.contains("://") || sessions.none { it.link.isNotBlank() }) return Stripped(text, emptyList())
        val buttons = mutableListOf<Button>()
        fun note(s: Session, url: String) { if (buttons.none { it.session == s.name }) buttons += Button(s.name, url) }
        var t = MD_LINK.replace(text) { m ->
            sessionOf(m.groupValues[2], sessions)?.let { s -> note(s, m.groupValues[2]); m.groupValues[1] } ?: m.value
        }
        val bare = Links.find(t).filter { r -> sessionOf(t.substring(r), sessions)?.also { s -> note(s, t.substring(r)) } != null }
        if (buttons.isEmpty()) return Stripped(text, emptyList())
        val sb = StringBuilder(t)
        bare.asReversed().forEach { r ->
            // La punteggiatura attaccata al link («url.», «url,») va via con lui, se dopo c'è uno spazio o la fine della riga.
            var end = r.last + 1
            while (end < sb.length && sb[end] in EATEN) end++
            if (end < sb.length && !sb[end].isWhitespace()) end = r.last + 1
            sb.delete(r.first, end)
        }
        t = sb.toString()
        val before = text.split('\n')
        val after = t.split('\n')
        val out = arrayOfNulls<String>(after.size)
        for (i in after.indices) out[i] = if (after[i] == before[i]) after[i] else tidy(after[i], before[i].takeWhile { it == ' ' || it == '\t' })
        // L'etichetta sulla riga sopra un link che stava da solo («• **Link:**» e a capo l'indirizzo) se ne va con lui.
        for (i in after.indices) if (out[i] == null && i > 0 && out[i - 1]?.let(::isLabel) == true) out[i - 1] = null
        return Stripped(out.filterNotNull().joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim('\n'), buttons)
    }

    /** Una riga da cui è uscito un link, col rientro che aveva: null se non resta niente da leggere. */
    private fun tidy(line: String, indent: String): String? {
        var l = EMPTY_PARENS.replace(line, "")
        l = indent + ORPHAN.replace(SPACES.replace(l.trim(), " "), "$1")
        if (core(l).isBlank() || isLabel(l)) return null
        return TRAILING_COLON.replace(l, "$1").trimEnd()
    }

    /** La riga senza il pallino dell'elenco e i segni del markdown. */
    private fun core(line: String): String = MARKS.replace(BULLET.replace(line, ""), "").trim()

    /** «Link:», «• **Apri qui:**»: fino a tre parole e i due punti, niente altro. */
    private fun isLabel(line: String): Boolean {
        val c = core(line)
        return c.endsWith(':') && c.trimEnd(':').trim().split(Regex("\\s+")).size <= LABEL_WORDS
    }

    private fun norm(url: String): String = url.substringBefore('#').substringBefore('?').trimEnd('/').trim()

    private const val SCHEME = "cmwatch"
    private const val HOST = "session"

    /** Il nome della sessione da aprire se `uri` è `cmwatch://session/<nome>`; null per ogni altro link. */
    fun deepLink(uri: String): String? {
        val u = runCatching { URI(uri) }.getOrNull() ?: return null
        if (u.scheme != SCHEME || u.host != HOST) return null
        val raw = u.rawPath.orEmpty().trim('/')
        if (raw.isEmpty() || raw.contains('/')) return null
        return runCatching { URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /** Il deep link che apre la chat di `name` nell'app Android. */
    fun deepLinkTo(name: String): String = "$SCHEME://$HOST/" + URLEncoder.encode(name, "UTF-8").replace("+", "%20")
}
