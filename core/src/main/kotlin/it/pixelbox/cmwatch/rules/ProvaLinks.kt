package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.contract.ProvePage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Le prove citate nei messaggi (app-prova-link-10-11, Franz 10/11 20:59: «quando una sessione cita una prova possiamo linkarla?»):
 * «prova #518», «prove #518 e #527», «prove 518, 527 e 543» diventano link `#/prova/<n>`, che aprono la scheda di quella prova. Lo
 * stesso in web/src/lib/provaLinks.ts.
 */
object ProvaLinks {
    /** Un riferimento nel testo: dove sta e il numero. Il primo di una frase comprende la parola «prova» o «prove». */
    data class Ref(val range: IntRange, val n: Long)

    // Il numero col cancelletto, o nudo di almeno tre cifre (le prove di oggi sono intorno a 500: «prove 10 minuti» non è una prova).
    private const val NUM = """(?:#\d{1,9}|\d{3,9})"""
    private val MENTION = Regex("""(?i)\bprov[ae]\s+($NUM(?:\s*(?:,|\be\b|\bed\b|\band\b)\s*$NUM)*)(?!\w)""")
    private val ONE = Regex("""#?(\d+)""")

    fun find(text: String): List<Ref> = MENTION.findAll(text).flatMap { m ->
        val g = m.groups[1]!!
        ONE.findAll(g.value).mapIndexed { i, x ->
            val start = if (i == 0) m.range.first else g.range.first + x.range.first
            Ref(start..(g.range.first + x.range.last), x.groupValues[1].toLong())
        }
    }.toList()

    /** Il link interno di una prova, lo stesso della web: `#/prova/<n>`. */
    fun link(n: Long): String = "#/prova/$n"

    private val LINK = Regex("""#/prova/(\d{1,9})$""")
    /** Il numero di una prova da un link `<web_app_url>/#/prova/<n>` o `#/prova/<n>`; null per ogni altro indirizzo. */
    fun idOf(link: String): Long? = LINK.find(link.trim())?.groupValues?.get(1)?.toLongOrNull()

    /** Com'è andata la lettura di una prova per numero. */
    sealed interface Read {
        data class Found(val prova: ProvaOne) : Read
        /** Un relay di prima del contratto 1.134: ignora l'arg e manda l'elenco, senza `id` in cima. */
        data object Unsupported : Read
        /** Il rifiuto del relay, in una riga («prova 999: non esiste»). */
        data class Refused(val message: String) : Read
    }

    /** La risposta di `prove` con l'arg: la prova, l'elenco di un relay vecchio, o il rifiuto. */
    fun read(ok: Boolean, text: String): Read {
        if (!ok) return Read.Refused(text.lines().firstOrNull { it.isNotBlank() }?.take(200) ?: "")
        val o = runCatching { ContractJson.json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return Read.Unsupported
        if ("id" !in o || "groups" in o) return Read.Unsupported
        return runCatching { Read.Found(ContractJson.json.decodeFromJsonElement(ProvaOne.serializer(), o as JsonObject)) }.getOrElse { Read.Unsupported }
    }

    /**
     * Dove porta il link di una prova (app-prova-link-diretta-10-11, Franz 10/11 22:57: «sarebbe preferibile che si apra direttamente
     * la scheda in verifica, ora sembra ci sia un passaggio in più»): una prova da fare che Verifica elenca si apre lì, sulla sua
     * scheda; una chiusa (ok, ko, rossa) o non ancora nell'elenco, nella pagina di dettaglio.
     */
    fun inVerify(n: Long, page: ProvePage?): Boolean = page?.groups?.any { g -> g.prove.any { it.id == n } } == true
}
