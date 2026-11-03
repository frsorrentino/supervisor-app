package it.pixelbox.cmwatch.rules

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * La pagina Cerca (app-cerca-pagina-03-11, mockup del 03/11, scelta «Così» di Franz): prima di scrivere le ricerche recenti e i
 * filtri; i risultati raggruppati per sessione, dal gruppo col risultato più recente. Il relay cerca sempre 7 giorni di tutte le
 * sessioni (contratto 1.27): i filtri si applicano qui.
 */
object SearchView {
    /** `session` null = tutte; `today` false = 7 giorni; `closed` = anche le sessioni chiuse. */
    data class Filter(val session: String? = null, val today: Boolean = false, val closed: Boolean = true)

    /** `session` null = il registro; `live` null per il registro. */
    data class Group(val session: String?, val live: Boolean?, val hits: List<ChatSearch.Hit>)

    data class Recent(val q: String, val session: String? = null)

    const val RECENT_MAX = 6

    fun groups(hits: List<ChatSearch.Hit>, f: Filter, dayStart: Long): List<Group> = hits
        .filter { h -> (f.session == null || h.session == f.session) && (!f.today || h.at >= dayStart) && (f.closed || h.live != false) }
        .groupBy { it.session to (it.live == false) }
        .map { (k, v) -> Group(k.first, if (k.first == null) null else !k.second, v.sortedByDescending { it.at }) }
        .sortedByDescending { g -> g.hits.first().at }

    /** In cima la più recente; la stessa ricerca (senza accenti né maiuscole) sale invece di ripetersi. */
    fun remember(list: List<Recent>, r: Recent): List<Recent> {
        val q = r.q.trim().replace(Regex("\\s+"), " ")
        if (q.isEmpty()) return list
        return (listOf(r.copy(q = q)) + list.filterNot { ChatSearch.fold(it.q) == ChatSearch.fold(q) }).take(RECENT_MAX)
    }

    sealed interface When {
        data class Time(val hhmm: String) : When
        data object Yesterday : When
        /** 1 = lunedì … 7 = domenica. */
        data class Weekday(val day: Int) : When
        data class Date(val ddmm: String) : When
    }

    /** L'ora di un risultato come nel mockup: oggi l'ora, poi «ieri», poi il giorno della settimana, oltre la data. */
    fun whenOf(at: Long, now: Long, zone: ZoneId): When {
        val t = Instant.ofEpochSecond(at).atZone(zone)
        val today = Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
        val d: LocalDate = t.toLocalDate()
        return when {
            d == today -> When.Time("%02d:%02d".format(t.hour, t.minute))
            d == today.minusDays(1) -> When.Yesterday
            d.isAfter(today.minusDays(7)) -> When.Weekday(d.dayOfWeek.value)
            else -> When.Date("%02d/%02d".format(d.dayOfMonth, d.monthValue))
        }
    }

    fun dayStart(now: Long, zone: ZoneId): Long = Instant.ofEpochSecond(now).atZone(zone).toLocalDate().atStartOfDay(zone).toEpochSecond()
}
