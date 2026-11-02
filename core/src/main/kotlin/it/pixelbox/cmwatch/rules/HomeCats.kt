package it.pixelbox.cmwatch.rules

/**
 * La home per categorie (Franz 02/11 16:44, compito app-home-categorie-02-11; homeCats.ts nella web): quattro categorie con le
 * loro sezioni, le stesse su telefono e PWA. Si salva nel `home_order` di sempre: le chiavi di sezione nell'ordine voluto, poi
 * `shut:` con le categorie chiuse, poi lo slot `pin:`. L'ordine delle categorie è quello della loro prima sezione.
 * Da guardare, Rendiconti e Pagella hanno qui solo il posto: si disegnano quando esistono (app-da-guardare-02-11, app-rendiconti-2-02-11).
 */
object HomeCats {
    data class Cat(val id: String, val open: Boolean, val sections: List<String>)

    val DEFAULT = listOf(
        Cat("you", true, listOf("prima_raccolta", "decisioni", "prove", "suggestions")),
        Cat("work", true, listOf("sessions", "seguiti", "lavori")),
        Cat("out", false, listOf("esiti", "da_guardare", "rendiconti")),
        Cat("tools", false, listOf("usage", "pagella", "system")),
    )
    private const val SHUT = "shut:"

    fun catOf(key: String): String? = DEFAULT.firstOrNull { key in it.sections }?.id

    /** Le chiavi sconosciute si ignorano (anche `closed` e `plugins` di ieri, ora dentro Sessioni e Sistema), le mancanti si accodano nella loro categoria. */
    fun decode(saved: List<String>?): List<Cat> {
        val list = saved.orEmpty()
        val keys = list.filter { catOf(it) != null }.distinct()
        val order = (keys.map { catOf(it)!! } + DEFAULT.map { it.id }).distinct()
        val closed = list.firstOrNull { it.startsWith(SHUT) }?.removePrefix(SHUT)?.split(",")
        return order.map { id ->
            val d = DEFAULT.single { it.id == id }
            val mine = keys.filter { catOf(it) == id }
            Cat(id, closed?.let { id !in it } ?: d.open, mine + d.sections.filter { it !in mine })
        }
    }

    /** La lista per l'op `home_order`: null quando è tutto predefinito. */
    fun encode(cats: List<Cat>, pin: String?): List<String>? {
        if (pin == null && cats == DEFAULT) return null
        val shut = SHUT + cats.filter { !it.open }.joinToString(",") { it.id }
        return HomeOrder.withPin(cats.flatMap { it.sections } + shut, pin)
    }

    fun moveCat(cats: List<Cat>, from: Int, to: Int): List<Cat> {
        if (from !in cats.indices || to !in cats.indices || from == to) return cats
        return cats.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Dentro la categoria e basta: fuori dai limiti resta com'è. */
    fun moveSection(cats: List<Cat>, id: String, from: Int, to: Int): List<Cat> =
        cats.map { if (it.id == id) it.copy(sections = HomeOrder.move(it.sections, from, to)) else it }

    fun toggle(cats: List<Cat>, id: String): List<Cat> = cats.map { if (it.id == id) it.copy(open = !it.open) else it }

    /**
     * Dopo un trascinamento sulla home (ordine piatto): ogni categoria riprende le sue sezioni in quell'ordine, aperte e chiuse come
     * prima. Le chiavi che la lista non ha (non disegnate, come Pagella finché non esiste) tengono il loro posto.
     */
    fun reorder(cats: List<Cat>, flat: List<String>): List<Cat> = cats.map { c ->
        val moved = flat.filter { it in c.sections }.iterator()
        c.copy(sections = c.sections.map { k -> if (k in flat) moved.next() else k })
    }

    /** La categoria di una voce della home: Chiuse sta dentro Sessioni, Plugin e skill dentro Sistema. */
    fun catFor(key: String): String? = when (key) { "closed" -> "work"; "plugins" -> "tools"; else -> catOf(key) }

    /** Il trascinamento sul posto della home, dentro la categoria della voce presa: le vicine di un'altra categoria non contano. */
    fun dragTarget(order: List<String>, held: String, y: Float, mids: Map<String, Float>, present: Set<String>): List<String> =
        HomeOrder.dragTarget(order, held, y, mids, present.filter { catFor(it) == catFor(held) }.toSet())
}
