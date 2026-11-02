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
    private const val TOP = "top:"
    /** Il bordo dello spazio in cima nella lista piatta del trascinamento: sopra c'è la cima, sotto le categorie. */
    const val EDGE = "edge:top"

    fun catOf(key: String): String? = DEFAULT.firstOrNull { key in it.sections }?.id

    /** Le chiavi sconosciute si ignorano (anche `closed` e `plugins` di ieri, ora dentro Sessioni e Sistema), le mancanti si accodano nella loro categoria. */
    fun decode(saved: List<String>?): List<Cat> {
        val list = saved.orEmpty()
        val up = top(saved)
        val keys = list.filter { catOf(it) != null && it !in up }.distinct()
        val order = (keys.map { catOf(it)!! } + DEFAULT.map { it.id }).distinct()
        val closed = list.firstOrNull { it.startsWith(SHUT) }?.removePrefix(SHUT)?.split(",")
        return order.map { id ->
            val d = DEFAULT.single { it.id == id }
            val mine = keys.filter { catOf(it) == id }
            Cat(id, closed?.let { id !in it } ?: d.open, mine + d.sections.filter { it !in mine && it !in up })
        }
    }

    /** La lista per l'op `home_order`: null quando è tutto predefinito. */
    fun encode(cats: List<Cat>, pin: String?, top: List<String> = emptyList()): List<String>? {
        if (pin == null && top.isEmpty() && cats == DEFAULT) return null
        val shut = SHUT + cats.filter { !it.open }.joinToString(",") { it.id }
        return HomeOrder.withPin(top.map { TOP + it } + cats.flatMap { it.sections } + shut, pin)
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
    fun dragTarget(order: List<String>, held: String, y: Float, mids: Map<String, Float>, present: Set<String>, fromBoard: Boolean = true): List<String> {
        // Una voce del tabellone scavalca le altre voci del tabellone e il bordo, di qualunque categoria siano; una sezione presa in una
        // categoria resta lì: sul tabellone ci va solo rilasciandola sul riquadro (Franz 02/11 20:45, app-tabellone-quattro-02-11).
        val up = order.indexOf(EDGE).let { e -> if (e < 0 || !fromBoard) emptySet() else order.take(e + 1).toSet() }
        return HomeOrder.dragTarget(order, held, y, mids, present.filter { it in up || catFor(it) == catFor(held) }.toSet())
    }

    // Lo spazio in cima (Franz 02/11 19:18, app-home-in-cima-02-11): sezioni di qualsiasi categoria, nell'ordine scelto. Una voce
    // in cima esce dalla sua categoria (Franz 19:26) e si salva come `top:<chiave>` al posto della sua chiave: il conto delle chiavi
    // del relay non cresce. Un'app che non conosce `top:` la ignora e rimette la sezione nella sua categoria.

    /**
     * Le voci sul tabellone (Franz 02/11 20:45, app-tabellone-quattro-02-11): al massimo [BOARD_MAX], nell'ordine salvato; doppie e
     * sconosciute si ignorano. Un ordine di prima con lo slot (`pin:`) e senza voci in cima porta la voce dello slot sul tabellone.
     */
    const val BOARD_MAX = 4
    fun top(saved: List<String>?): List<String> {
        val list = saved.orEmpty().filter { it.startsWith(TOP) }.map { it.removePrefix(TOP) }.filter { catOf(it) != null }.distinct()
        return list.ifEmpty { listOfNotNull(HomeOrder.pinned(saved)?.takeIf { catOf(it) != null }) }.take(BOARD_MAX)
    }

    /** L'ordine mostrato e trascinato: la cima, il bordo, le sezioni delle categorie. */
    fun flat(top: List<String>, cats: List<Cat>): List<String> = top + EDGE + cats.flatMap { it.sections }

    /**
     * Dopo il rilascio: quello che sta sopra il bordo va in cima; il resto torna nella sua categoria nell'ordine del dito. Le chiavi
     * che la lista non ha (non disegnate) tengono il loro posto; chi scende dalla cima entra nella sua categoria dove l'ha lasciato il dito.
     */
    fun settle(cats: List<Cat>, flat: List<String>): Pair<List<String>, List<Cat>> {
        val e = flat.indexOf(EDGE)
        if (e < 0) return emptyList<String>() to reorder(cats, flat)
        val up = flat.take(e).filter { catOf(it) != null }.distinct()
        val below = flat.drop(e + 1)
        val back = cats.map { c ->
            val returned = below.filter { catOf(it) == c.id && it !in c.sections }
            c.copy(sections = c.sections.filter { it !in up } + returned)
        }
        return up to reorder(back, below)
    }

    /** Riordina e rilascio sul tabellone: la voce va in fondo al tabellone; pieno, niente cambia. */
    fun toTop(cats: List<Cat>, top: List<String>, key: String): Pair<List<String>, List<Cat>> =
        if (key in top || catOf(key) == null || top.size >= BOARD_MAX) top to cats
        else (top + key) to cats.map { c -> c.copy(sections = c.sections - key) }

    /** Riordina, «Togli dalla cima»: la voce torna in fondo alla sua categoria. */
    fun fromTop(cats: List<Cat>, top: List<String>, key: String): Pair<List<String>, List<Cat>> =
        if (key !in top) top to cats
        else (top - key) to cats.map { c -> if (c.id == catOf(key)) c.copy(sections = c.sections + key) else c }

    // Le categorie si trascinano anche nella home (Franz 02/11 19:00, app-categorie-trascina-02-11): nella lista del trascinamento
    // una categoria è `cat:<id>`, così non si confonde con le chiavi di sezione.
    fun catKeys(cats: List<Cat>): List<String> = cats.map { "cat:${it.id}" }

    /** Le categorie nell'ordine trascinato, con le loro sezioni e il loro interruttore; quelle che la lista non ha restano in fondo. */
    fun catOrder(cats: List<Cat>, keys: List<String>): List<Cat> {
        val picked = keys.mapNotNull { k -> cats.firstOrNull { "cat:${it.id}" == k } }.distinct()
        return picked + cats.filter { it !in picked }
    }
}
