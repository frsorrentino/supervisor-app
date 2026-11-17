package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Pagella
import it.pixelbox.cmwatch.contract.PagellaRow

/**
 * app-rendiconti-2-02-11 (programma delle otto proposte, fase C, approvato da Franz il 01/11 alle 20:35), contratto 1.103: i rendiconti
 * per mese e cliente e la pagella. Porta di conti.ts nella web.
 */
object ContiRules {
    /** La pagella mostra solo le righe con abbastanza casi (da `min`, 10): una percentuale su 6 casi non dice niente. Dalla più usata. */
    fun pagellaRows(p: Pagella?): List<PagellaRow> =
        p?.rows.orEmpty().filter { it.enough && it.n >= p!!.min }.sortedWith(compareByDescending<PagellaRow> { it.n }.thenBy { it.model })

    /** «Opus 5.5» da «claude-opus-5-5»: il nome come lo dice Franz. */
    fun modelName(id: String): String {
        val m = Regex("""^claude-([a-z]+)-(\d+)(?:-(\d+))?""").find(id) ?: return id
        val (name, major, minor) = m.destructured
        return "${name.replaceFirstChar { it.uppercase() }} $major${if (minor.isNotEmpty()) ".$minor" else ""}"
    }

    /** La percentuale dei casi con un difetto, arrotondata; null senza misura. */
    fun ratePct(r: PagellaRow): Int? = r.rate?.let { Math.round(it * 100).toInt() }

    /** «settembre 2026» da «2026-09», nella lingua data. */
    fun monthName(mese: String, locale: java.util.Locale): String {
        val (y, m) = mese.split("-").map { it.toInt() }
        val t = java.time.YearMonth.of(y, m)
        return "${t.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale)} $y"
    }
}
