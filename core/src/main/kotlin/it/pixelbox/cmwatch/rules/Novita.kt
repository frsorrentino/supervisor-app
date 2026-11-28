package it.pixelbox.cmwatch.rules

/**
 * Le novità di un aggiornamento (app-changelog-aggiornamento-26-11, Franz 26/11 14:00: «possiamo aggiungere il changelog
 * dell'aggiornamento in breve? lo mettiamo con accordion espandibile?»).
 *
 * La fonte: ogni commit dell'app porta, accanto a Co-Authored-By, una o più righe `Novita: <frase>` in italiano, scritte per Franz
 * (cosa cambia per lui, non il nome del commit). La CI le raccoglie per la pre-release, dal commit dopo la pre-release precedente,
 * in una sezione «## Novità» all'inizio del corpo, una voce per riga «- frase». Qui si sommano le sezioni di tutte le pre-release
 * fra la versione installata e quella offerta, la più nuova prima, senza doppioni. Le pre-release precedenti non hanno la
 * sezione: per quelle l'interfaccia dice «Correzioni minori».
 */
object Novita {
    /** Le righe mostrate al massimo; oltre, le prime [MAX] − 1 e «e altre N novità». */
    const val MAX = 6

    data class Lines(val lines: List<String>, val more: Int)

    private val HEAD = Regex("""^#{1,3}\s*Novit[àa]\s*$""", RegexOption.IGNORE_CASE)

    /** Le voci della sezione «## Novità» del corpo di una pre-release, nell'ordine; vuota se la sezione non c'è. */
    fun section(notes: String): List<String> {
        val rows = notes.lines()
        val start = rows.indexOfFirst { HEAD.matches(it.trim()) }
        if (start < 0) return emptyList()
        return rows.drop(start + 1).dropWhile { it.isBlank() }.takeWhile { it.trimStart().startsWith("- ") }
            .map { it.trimStart().removePrefix("- ").trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Le novità fra l'installata e l'offerta: le pre-release uscite dopo l'installazione fino all'offerta compresa (solo
     * l'offerta se non se ne sa di più), la più nuova prima, senza doppioni, al massimo [MAX] righe.
     */
    fun lines(releases: List<AppUpdate.Release>, installed: AppUpdate.Installed, offer: AppUpdate.Release): Lines =
        lines(releases, since(releases, installed), offer)

    /**
     * Da quando contano le pre-release (app-novita-cosa-cambia-28-11, Franz 28/11 13:57: «la sezione novità non dice più cosa
     * cambia»): dalla pubblicazione della release installata, riconosciuta dal digest dell'APK, non dall'ora in cui è stata
     * installata. Franz aveva messo la 0.2.2435 dalla carta della prova quando la 0.2.2436, con la correzione del foglio, era già
     * uscita; la 0.2.2437 offerta aveva solo i golden, e la scheda diceva «Correzioni minori». Senza digest che torni, l'ora
     * dell'installazione come prima.
     */
    fun since(releases: List<AppUpdate.Release>, installed: AppUpdate.Installed): Long =
        installed.sha256?.let { sha -> releases.firstOrNull { it.sha256 == sha }?.publishedAt } ?: installed.installedAt

    fun lines(releases: List<AppUpdate.Release>, installedAt: Long, offer: AppUpdate.Release): Lines {
        val span = releases.filter { it.publishedAt > installedAt && it.publishedAt <= offer.publishedAt }.ifEmpty { listOf(offer) }
        val all = span.sortedByDescending { it.publishedAt }.flatMap { section(it.notes) }.distinct()
        return if (all.size <= MAX) Lines(all, 0) else Lines(all.take(MAX - 1), all.size - (MAX - 1))
    }
}
