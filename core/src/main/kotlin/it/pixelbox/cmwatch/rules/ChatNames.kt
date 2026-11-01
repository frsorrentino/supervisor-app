package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State

/**
 * I nomi dei compiti in chat (Franz 01/11 21:31): un codice che esiste davvero nello stato diventa il suo nome breve, con un tocco che apre
 * il compito. Confronto esatto, codice intero. Non si tocca un blocco di codice, un link, un indirizzo; un codice inline si cambia solo se è
 * tutto lì, perché è così che le sessioni li scrivono. Solo per il disegno: copia e lettura a voce tengono il testo com'era.
 */
object ChatNames {
    /** L'indirizzo finto del link: `[nome](TASK_URL + codice)`. Il disegno lo riconosce e apre il compito invece del browser. */
    const val TASK_URL = "https://task.invalid/"

    /** Codice → nome breve, dai compiti dello stato; senza nome breve il codice resta. */
    fun names(state: State?): Map<String, String> = state?.commitments.orEmpty().mapNotNull { c ->
        val short = c.short?.trim().orEmpty()
        if (short.isEmpty() || short == c.task || short.contains(']') || short.contains('[')) null else c.task to short
    }.toMap()

    private val FENCE = Regex("""^\s*```.*""")
    private val SKIP = Regex("""`[^`\n]+`|\[[^\]\n]+]\([^)\s]+\)|https?://\S+""")

    fun replace(text: String, names: Map<String, String>): String {
        if (names.isEmpty()) return text
        val codes = Regex("""(?<![\w-])(""" + names.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } + """)(?![\w-])""")
        fun link(code: String) = "[${names.getValue(code)}]($TASK_URL$code)"
        fun plain(s: String) = codes.replace(s) { link(it.value) }
        var fenced = false
        return text.split('\n').joinToString("\n") { line ->
            if (FENCE.matches(line)) { fenced = !fenced; return@joinToString line }
            if (fenced) return@joinToString line
            val b = StringBuilder()
            var at = 0
            SKIP.findAll(line).forEach { m ->
                b.append(plain(line.substring(at, m.range.first)))
                val inner = m.value.takeIf { it.startsWith("`") }?.trim('`')
                b.append(if (inner != null && inner in names) link(inner) else m.value)
                at = m.range.last + 1
            }
            b.append(plain(line.substring(at))).toString()
        }
    }
}
