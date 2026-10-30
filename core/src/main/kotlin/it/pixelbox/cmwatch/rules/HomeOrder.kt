package it.pixelbox.cmwatch.rules

/**
 * L'ordine delle sezioni della home (Franz 30/10 17:22, compito app-riordino-sezioni-30-10; homeOrder.ts nella web): un ordine
 * unico sul relay, `state.home_order`. Tabellone e «Ti aspettano» non hanno chiave: restano fissi in cima.
 */
object HomeOrder {
    val DEFAULT = listOf("sessions", "suggestions", "lavori", "esiti", "altro")

    /** Le chiavi sconosciute si ignorano e le mancanti si accodano, così una sezione nuova non chiede una release del relay. */
    fun normalize(saved: List<String>?): List<String> {
        val known = saved.orEmpty().filter { it in DEFAULT }.distinct()
        return known + DEFAULT.filter { it !in known }
    }

    /** Sposta la voce `from` al posto `to`; fuori dai limiti la lista resta com'è. */
    fun move(list: List<String>, from: Int, to: Int): List<String> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** L'arg dell'op `home_order`: l'array JSON delle chiavi; null = torna al predefinito. */
    fun arg(order: List<String>): String? = if (order == DEFAULT) null else order.joinToString(",", "[", "]") { "\"$it\"" }
}
