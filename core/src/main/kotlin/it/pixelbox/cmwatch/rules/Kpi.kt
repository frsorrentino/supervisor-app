package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.TimelineEvent
import it.pixelbox.cmwatch.contract.TimelinePage
import java.time.Instant
import java.time.ZoneId

/**
 * I KPI coi dati che il relay ha già (proposte 6, 11, 15 e 18 del 25/10, compito app-kpi-medie-25-10): tutto dalla
 * cronologia di oggi (contratto 1.29) e dallo stato. La cronologia non registra le attese: «ti aspetta» vale solo per il
 * tratto in corso, letto dallo stato (master, 29/10 08:07).
 */
object Kpi {
    const val HOURS = 6
    /** Due eventi a meno di 20 minuti l'uno dall'altro sono lo stesso tratto di lavoro. */
    const val GAP_S = 20 * 60L
    /** Un evento isolato vale almeno 10 minuti di lavoro. */
    const val MIN_WORK_S = 10 * 60L
    /** Contratto 1.29: al massimo 40 eventi per sessione; con tanti la cifra è un minimo. */
    const val EVENTS_CAP = 40

    enum class Band { WORK, WAIT, STILL }
    data class Span(val from: Long, val to: Long, val band: Band)

    private val ACTIVITY = setOf("prompt", "test", "commit", "outcome", "compact", "task")
    private fun activity(events: List<TimelineEvent>) = events.filter { it.kind in ACTIVITY }

    /** Da quando una sessione aspetta Franz adesso: una domanda aperta o una ferma; null se non aspetta. */
    fun waitingSince(s: Session): Long? = s.question?.askedAt ?: StrongAlert.stop(s)?.at

    /** Da dove chiedere la cronologia: la mezzanotte di oggi o sei ore fa, la più vecchia. */
    fun since(now: Long, zone: ZoneId): Long = minOf(midnight(now, zone), now - HOURS * 3600)

    fun midnight(now: Long, zone: ZoneId): Long = Instant.ofEpochSecond(now).atZone(zone).toLocalDate().atStartOfDay(zone).toEpochSecond()

    /** 6 · le ultime sei ore, un conto per ora (la più vecchia prima): prompt, test, commit ed esiti. */
    fun hourly(events: List<TimelineEvent>, now: Long, hours: Int = HOURS): List<Int> {
        val out = IntArray(hours)
        for (e in activity(events)) {
            val back = ((now - e.at) / 3600).toInt()
            if (e.at <= now && back < hours) out[hours - 1 - back]++
        }
        return out.toList()
    }

    /**
     * 11 · la giornata a fasce, da `from` a `now`: lavoro dove gli eventi sono vicini, ferma negli intervalli; con
     * `waitingSince` l'ultimo tratto è «ti aspetta».
     */
    fun day(events: List<TimelineEvent>, from: Long, now: Long, waitingSince: Long?): List<Span> {
        val work = mutableListOf<Pair<Long, Long>>()
        for (at in activity(events).map { it.at }.filter { it in from..now }.sorted()) {
            val last = work.lastOrNull()
            if (last != null && at - last.second <= GAP_S) work[work.size - 1] = last.first to maxOf(last.second, at)
            else work += at to at
        }
        val spans = mutableListOf<Span>()
        var t = from
        for ((a, b) in work) {
            val end = minOf(now, maxOf(b, a + MIN_WORK_S))
            if (a > t) spans += Span(t, a, Band.STILL)
            if (end > maxOf(a, t)) spans += Span(maxOf(a, t), end, Band.WORK)
            t = maxOf(t, end)
        }
        if (t < now) spans += Span(t, now, Band.STILL)
        val w = waitingSince?.coerceAtLeast(from) ?: return merge(spans)
        if (w >= now) return merge(spans)
        val cut = spans.mapNotNull { s -> if (s.to <= w) s else if (s.from < w) s.copy(to = w) else null }
        return merge(cut + Span(w, now, Band.WAIT))
    }

    private fun merge(spans: List<Span>): List<Span> = spans.fold(mutableListOf()) { acc, s ->
        val last = acc.lastOrNull()
        if (last != null && last.band == s.band && last.to == s.from) acc[acc.size - 1] = last.copy(to = s.to) else acc += s
        acc
    }

    fun workSeconds(spans: List<Span>): Long = spans.filter { it.band == Band.WORK }.sumOf { it.to - it.from }

    /** 15 · sei caselle, una per ora: lavoro se ci sono eventi, ferma altrimenti; l'ora in corso è ambra per chi aspetta adesso. */
    fun cells(events: List<TimelineEvent>, now: Long, waitingNow: Boolean): List<Band> =
        hourly(events, now).mapIndexed { i, n -> if (i == HOURS - 1 && waitingNow) Band.WAIT else if (n > 0) Band.WORK else Band.STILL }

    data class TileRow(val name: String, val cells: List<Band>)

    /** 15 · le tre sessioni vive più attive nelle ultime sei ore, le altre che aspettano prima delle ferme. */
    fun tileRows(page: TimelinePage?, state: State?, now: Long, rows: Int = 3): List<TileRow> {
        val live = state?.sessions.orEmpty().filter { it.state != SessionState.GONE }
        val bySession = page?.sessions.orEmpty().filter { it.live }.associateBy { it.session }
        return live.map { s -> s to hourly(bySession[s.name]?.events.orEmpty(), now).sum() }
            .filter { (s, n) -> n > 0 || waitingSince(s) != null }
            .sortedWith(compareByDescending<Pair<Session, Int>> { it.second }.thenBy { it.first.name })
            .take(rows)
            .map { (s, _) -> TileRow(s.name, cells(bySession[s.name]?.events.orEmpty(), now, waitingSince(s) != null)) }
    }

    /** 18 · la giornata in cifre. `atLeast` = qualche sessione è al tetto di eventi del relay: le cifre sono un minimo. */
    data class DayFigures(
        val prompts: Int, val commits: Int, val atLeast: Boolean, val tokens: Long, val topSession: String?, val topShare: Double,
        val waitingNow: Int, val oldestWaitS: Long?, val perHour: List<Pair<Int, Int>>, val firstHour: Int,
    )

    fun dayFigures(page: TimelinePage?, state: State?, now: Long, zone: ZoneId): DayFigures {
        val from = midnight(now, zone)
        val today = page?.sessions.orEmpty().flatMap { it.events }.filter { it.at in from..now }
        val prompts = today.filter { it.kind == "prompt" }
        val commits = today.filter { it.kind == "commit" }
        val usage = state?.usageToday.orEmpty()
        val top = usage.maxByOrNull { it.tokens }
        val waits = state?.sessions.orEmpty().mapNotNull { waitingSince(it) }
        val hourOf = { at: Long -> Instant.ofEpochSecond(at).atZone(zone).hour }
        val nowHour = hourOf(now)
        val first = (prompts + commits).minOfOrNull { hourOf(it.at) } ?: nowHour
        val perHour = (first..nowHour).map { h -> prompts.count { hourOf(it.at) == h } to commits.count { hourOf(it.at) == h } }
        return DayFigures(
            prompts = prompts.size, commits = commits.size,
            atLeast = page?.more == true || page?.sessions.orEmpty().any { it.events.size >= EVENTS_CAP },
            tokens = usage.sumOf { it.tokens }, topSession = top?.session, topShare = top?.let { t -> t.tokens.toDouble() / usage.sumOf { it.tokens }.coerceAtLeast(1) } ?: 0.0,
            waitingNow = waits.size, oldestWaitS = waits.minOrNull()?.let { now - it }, perHour = perHour, firstHour = first,
        )
    }
}
