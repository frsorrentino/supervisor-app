package it.pixelbox.cmwatch.rules

/**
 * La scheda «Ti aspetta» (app-scheda-ti-aspetta-07-11, mockup approvato da Franz il 07/11 alle 21:04): una pagina per ogni domanda
 * che aspetta Franz, aperta da un link o da Tocca a te. In alto la carta coi tasti (Decidi, Scegli, Verifica, Rispondi), sotto
 * «Da dove viene» col pezzo di trascrizione e il salto al messaggio, in fondo le altre che aspettano. Qui la parte che non
 * dipende dal contratto: il modello della pagina, la frase della domanda accesa nel pezzo e l'età. I dati veri arrivano con
 * supervisor-attese-link-07-11. La stessa regola in web/src/lib/tiAspetta.ts, coi casi di core/src/test/resources/ti-aspetta-cases.json.
 */
object TiAspetta {
    enum class Kind { DECIDE, CHOOSE, VERIFY, ANSWER }

    /** Il pezzo di trascrizione da cui viene la domanda: la sessione, l'ora della risposta, il testo, la frase da accendere, il messaggio. */
    data class Source(val session: String, val at: Long?, val text: String, val ask: String?, val entry: String?)

    /** Un'altra domanda che aspetta, per passare alla sua scheda. */
    data class Other(val key: String, val kind: Kind, val title: String, val session: String?, val since: Long)

    data class Page(val key: String, val kind: Kind, val session: String?, val since: Long, val source: Source?, val others: List<Other> = emptyList())

    data class Seg(val text: String, val on: Boolean)

    /** Il testo con la frase della domanda accesa; maiuscole e spazi non contano. Senza frase o se non c'è, tutto spento. */
    fun marks(text: String, ask: String?): List<Seg> {
        val words = ask?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
        if (words.isEmpty()) return listOf(Seg(text, false))
        val re = Regex(words.joinToString("\\s+") { Regex.escape(it) }, RegexOption.IGNORE_CASE)
        val m = re.find(text) ?: return listOf(Seg(text, false))
        return listOfNotNull(
            Seg(text.substring(0, m.range.first), false).takeIf { it.text.isNotEmpty() },
            Seg(m.value, true),
            Seg(text.substring(m.range.last + 1), false).takeIf { it.text.isNotEmpty() },
        )
    }

    /** L'etichetta del relay (decide, choose, verify, reply) come tipo della carta. */
    fun kindOf(label: String): Kind = when (label) { "choose" -> Kind.CHOOSE; "verify" -> Kind.VERIFY; "reply" -> Kind.ANSWER; else -> Kind.DECIDE }

    /** La pagina di una voce di `attesa` (con `msg`) e delle altre di `attese`; il titolo di un'altra è il «cosa» della carta o la domanda. */
    fun pageOf(item: it.pixelbox.cmwatch.contract.WaitItem, others: List<it.pixelbox.cmwatch.contract.WaitItem>): Page = Page(
        item.id, kindOf(item.label), item.session, item.at,
        item.msg?.let { m -> Source(m.session ?: item.session.orEmpty(), m.at, m.text, m.key, m.id) },
        others.filter { o -> o.id != item.id }.map { o -> Other(o.id, kindOf(o.label), o.card?.what?.takeIf { w -> w.isNotBlank() } ?: o.text, o.session, o.at) },
    )

    private val LINK = Regex("#/attesa/(w[0-9a-f]{10})$")

    /** Contratto 1.130: l'id di una domanda da un link `<web_app_url>/#/attesa/<id>`; null per ogni altro indirizzo. */
    fun waitOf(link: String): String? = LINK.find(link.trim())?.groupValues?.get(1)

    /** Da quanto aspetta: minuti (almeno 1), poi ore, poi giorni. */
    fun age(seconds: Long): String = when {
        seconds < 3600 -> "${maxOf(1, seconds / 60)} min"
        seconds < 86400 -> "${seconds / 3600} h"
        else -> "${seconds / 86400} g"
    }
}
