package it.pixelbox.cmwatch.rules

/**
 * La Home in riga (app-home-riga-c1-13-11, giro C dei mockup, variante C1 scelta da Franz il 13/11 alle 09:32), gemella di
 * web/src/lib/homeRow.ts. Mentre la master occupa la colonna, la Home è una riga in basso che mostra solo quello che non si vede
 * già: le sessioni che ti aspettano e non sono a schermo, e le novità da quando la Home è in riga. Una voce per cosa, che apre la
 * sua sessione; una novità va via quando la apri o quando riapri la Home. Vuota, resta la maniglia.
 */
object HomeRow {
    /** Una riga della Home, come la vede la regola: `group` waiting | finished | working | still. */
    data class In(val session: String, val group: String, val text: String?, val at: Long?, val key: String? = null)

    /** `WAIT` = ti aspetta fuori schermo; `NEWS` = un esito arrivato da quando la Home è in riga. */
    enum class Kind { WAIT, NEWS }
    data class Voice(val kind: Kind, val session: String, val text: String?, val at: Long?, val key: String)

    /** Le righe della Home per la regola: di chi ha finito il titolo dell'esito, degli altri la prima riga del testo. */
    fun fromSummary(rows: List<Summary.Row>): List<In> = rows.map { r ->
        In(
            r.session.name, r.group.name.lowercase(),
            if (r.group == Summary.Group.FINISHED) r.session.outcome?.let(OutcomeText::headline) ?: r.text else r.text,
            r.at, r.key,
        )
    }

    /**
     * Le voci della riga: `shown` = le sessioni aperte a schermo, `since` = da quando la Home è in riga (epoch s; null = nessuna
     * novità), `opened` = le novità già aperte. Prima chi ti aspetta, nell'ordine della Home; poi le novità. La master non c'è mai.
     */
    fun voices(rows: List<In>, shown: List<String>, since: Long?, opened: Set<String>): List<Voice> {
        val off = rows.filter { it.session != ContextActions.MASTER && it.session !in shown }
        fun line(x: String?) = x?.substringBefore('\n')?.trim()?.takeIf { it.isNotEmpty() }
        val waits = off.filter { it.group == "waiting" }.map { Voice(Kind.WAIT, it.session, line(it.text), it.at, "wait:${it.session}") }
        val news = if (since == null) emptyList() else off.filter { it.group == "finished" && (it.at ?: 0) > since }
            .map { Voice(Kind.NEWS, it.session, line(it.text), it.at, it.key ?: "news:${it.session}@${it.at}") }
            .filter { it.key !in opened }
        return waits + news
    }

    /** L'altezza della riga in dp: piena con le voci, solo la maniglia senza. */
    const val ROW_H = 64
    const val GRIP_H = 28
    fun rowHeight(voices: List<Voice>): Int = if (voices.isEmpty()) GRIP_H else ROW_H
}
