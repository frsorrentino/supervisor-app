package it.pixelbox.cmwatch.rules

/**
 * L'ordine delle sezioni della home (Franz 30/10 17:22, compito app-riordino-sezioni-30-10; homeOrder.ts nella web): un ordine
 * unico sul relay, `state.home_order`. Tabellone e «Ti aspettano» non hanno chiave: restano fissi in cima.
 */
object HomeOrder {
    /** Le righe separabili (Franz 31/10 15:50) sostituiscono `altro`. */
    val ROWS = listOf("usage", "system", "plugins", "closed")
    val DEFAULT = listOf("sessions", "suggestions", "lavori", "esiti") + ROWS

    /** Le chiavi sconosciute si ignorano e le mancanti si accodano, così una sezione nuova non chiede una release del relay. */
    fun normalize(saved: List<String>?): List<String> {
        // Un ordine salvato prima della separazione: `altro` si espande al suo posto.
        val known = saved.orEmpty().flatMap { if (it == "altro") ROWS else listOf(it) }.filter { it in DEFAULT }.distinct()
        return known + DEFAULT.filter { it !in known }
    }

    /** Sposta la voce `from` al posto `to`; fuori dai limiti la lista resta com'è. */
    fun move(list: List<String>, from: Int, to: Int): List<String> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }

    /**
     * Il trascinamento sul posto (Franz 31/10 14:27): la voce presa scavalca la vicina solo quando il dito incrocia la metà della
     * sua testata. `mids`: le testate sullo schermo; `present`: le sezioni che esistono, quelle vuote non fanno da ostacolo. Una
     * vicina fuori schermo non si scavalca: ci arriva lo scorrimento automatico. Dopo lo scambio la condizione opposta è falsa,
     * quindi la voce non rimbalza.
     */
    fun dragTarget(order: List<String>, held: String, y: Float, mids: Map<String, Float>, present: Set<String> = mids.keys): List<String> {
        val i = order.indexOf(held)
        if (i < 0) return order
        val next = order.drop(i + 1).firstOrNull { it in present }
        if (next != null && mids[next]?.let { y > it } == true) return move(order, i, order.indexOf(next))
        val prev = order.take(i).lastOrNull { it in present }
        if (prev != null && mids[prev]?.let { y < it } == true) return move(order, i, order.indexOf(prev))
        return order
    }

    /**
     * Lo slot del tabellone (Franz 01/11 14:03, «Slot agganciabile»): una voce sola, salvata nella lista stessa come `pin:chiave`,
     * così non serve una release del relay. Resta anche il suo posto nell'ordine: sganciata, torna dov'era.
     */
    const val PIN = "pin:"
    val PINNABLE = setOf("usage", "suggestions", "system", "lavori", "plugins", "closed")
    fun pinnable(key: String): Boolean = key in PINNABLE
    fun pinned(saved: List<String>?): String? = saved.orEmpty().firstOrNull { it.startsWith(PIN) }?.removePrefix(PIN)?.takeIf(::pinnable)
    /** La lista da salvare: l'ordine e, in fondo, l'aggancio. */
    fun withPin(order: List<String>, pin: String?): List<String> = order.filter { !it.startsWith(PIN) } + listOfNotNull(pin?.takeIf(::pinnable)?.let { PIN + it })
    /** Chi sta nello slot adesso: l'allarme della settimana passa davanti, e la voce agganciata riprende il posto quando finisce. */
    fun slot(pin: String?, alarm: Boolean): String? = if (alarm) "usage" else pin

    /** L'arg dell'op `home_order`: l'array JSON delle chiavi; null = torna al predefinito. */
    fun arg(order: List<String>): String? = if (order == DEFAULT) null else order.joinToString(",", "[", "]") { "\"$it\"" }
}
