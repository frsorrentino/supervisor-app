package it.pixelbox.cmwatch.rules

/**
 * app-trova-messaggi-20-11 (Franz 20/11 20:56: non ritrovava un suo messaggio; 21:12 «ok A»): il salto fra i miei messaggi nella
 * conversazione di una sessione, due frecce e il contatore «3 di 12». Le fermate sono le richieste dei turni: i messaggi di Franz,
 * dal telefono, dalla web o scritti al PC, anche quelli non ancora arrivati; mai i turni partiti da soli (avvisi, notifiche). La
 * stessa regola in web/src/lib/mineJump.ts.
 */
object MineJump {
    /** Una fermata: la riga della conversazione e la voce da far lampeggiare (la voce della trascrizione, o il mandato se non c'è). */
    data class Stop(val row: Int, val flash: String)

    fun stops(rows: List<ChatTurns.Row>): List<Stop> = rows.mapIndexedNotNull { i, r ->
        val t = r as? ChatTurns.Row.Turn ?: return@mapIndexedNotNull null
        if (t.auto) return@mapIndexedNotNull null
        when (val q = t.request) {
            is ChatFeed.Item.User -> Stop(i, q.entry.id)
            is ChatFeed.Item.Mine -> Stop(i, q.entry?.id ?: q.sent.id)
            else -> null
        }
    }

    /**
     * La fermata dopo un tocco: [at] la voce mostrata ora (null = si comincia), [dir] -1 il messaggio prima, +1 quello dopo. Si comincia
     * dall'ultimo; ai capi si resta fermi. Per voce e non per indice: caricando le pagine più vecchie le righe cambiano, la voce no.
     */
    fun step(stops: List<Stop>, at: String?, dir: Int): Stop? {
        if (stops.isEmpty()) return null
        val i = stops.indexOfFirst { it.flash == at }
        if (at == null || i < 0) return stops.last()
        return stops[(i + dir).coerceIn(0, stops.size - 1)]
    }

    /** «3 di 12»: la posizione dal più vecchio, su quanti sono caricati; null se la voce non c'è. */
    fun position(stops: List<Stop>, at: String?): Pair<Int, Int>? = stops.indexOfFirst { it.flash == at }.takeIf { it >= 0 }?.let { (it + 1) to stops.size }
}
