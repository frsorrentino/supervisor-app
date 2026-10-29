package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.KpiChecksDay
import it.pixelbox.cmwatch.contract.KpiData
import it.pixelbox.cmwatch.contract.KpiSuggestions
import it.pixelbox.cmwatch.contract.KpiTask
import it.pixelbox.cmwatch.contract.KpiTasks
import it.pixelbox.cmwatch.contract.KpiTurn
import it.pixelbox.cmwatch.contract.KpiWaits
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

    // ---- Proposte piccole (4, 5, 7, 14, 16; compito app-kpi-piccole-25-10) ----

    /** 5 · la soglia di handoff sul contesto (fable-director: handoff dal 60%, rosso dall'80%). */
    const val HANDOFF = 60

    data class Slice(val session: String, val pct: Int)
    data class Split(val account: String, val h5: Int, val slices: List<Slice>, val others: Int)

    /**
     * 4 · la quota 5h di un account divisa per sessione, in proporzione alla parte di token di oggi (`usage_today.share`):
     * le prime tre, il resto insieme. Null senza quota 5h o senza token di oggi per quell'account.
     */
    fun split(state: State, account: String, top: Int = 3): Split? {
        val h5 = state.quota[account]?.h5 ?: return null
        val kind = state.quota[account]?.kind
        val mine = state.usageToday.orEmpty().filter { it.account == account || (kind != null && it.account == kind) }.sortedByDescending { it.share }
        if (mine.isEmpty()) return null
        val slices = mine.take(top).map { Slice(it.session, Math.round(h5 * it.share).toInt()) }
        return Split(account, h5, slices, (h5 - slices.sumOf { it.pct }).coerceAtLeast(0).takeIf { mine.size > top } ?: 0)
    }

    /** 5 · la parte di token di oggi della sessione sul suo account, in percento; null senza dati. */
    fun todayShare(state: State, session: String): Int? = state.usageToday?.firstOrNull { it.session == session }?.let { Math.round(it.share * 100).toInt() }

    /** 7 · tre cifre in testa alla home: chi ti aspetta (e da quanto la prima), chi lavora, gli esiti di oggi e i rossi. */
    data class Head(val waiting: Int, val oldestWaitS: Long?, val busy: Int, val outcomes: Int?, val red: Int?)

    fun head(state: State, page: TimelinePage?, now: Long, zone: ZoneId): Head {
        val live = state.sessions.filter { it.state != SessionState.GONE }
        val waits = live.mapNotNull { waitingSince(it) }
        val from = midnight(now, zone)
        val today = page?.sessions?.flatMap { it.events }?.filter { it.at in from..now }
        return Head(
            waiting = waits.size, oldestWaitS = waits.minOrNull()?.let { now - it }, busy = live.count { it.state == SessionState.BUSY },
            outcomes = today?.count { it.kind == "outcome" }, red = today?.count { it.kind == "task" && it.ok == false },
        )
    }

    // ---- I box della home aperti (compito app-box-cliccabili-29-10, mockup approvato da Franz il 29/10 alle 18:57) ----

    enum class WaitKind { QUESTION, NEEDS_YOU, BLOCKING }

    /** Una sessione che aspetta Franz: perché, da quando; `closesItself` = il relay verifica da sé la condizione (needs-you --until). */
    data class Waiting(val session: String, val kind: WaitKind, val text: String, val since: Long, val closesItself: Boolean)

    /** «Ti aspettano»: le stesse sessioni contate da [head], la più vecchia prima. */
    fun waitingRows(state: State): List<Waiting> = state.sessions.filter { it.state != SessionState.GONE }.mapNotNull { s ->
        val q = s.question
        val n = s.needsYou?.takeIf { it.reason.isNotBlank() }
        val b = s.turnBlocking?.takeIf { it.text.isNotBlank() }
        when {
            q != null -> Waiting(s.name, WaitKind.QUESTION, q.shareable, q.askedAt, false)
            n != null -> Waiting(s.name, WaitKind.NEEDS_YOU, n.reason, n.at, n.until)
            b != null -> Waiting(s.name, WaitKind.BLOCKING, b.text, b.at, false)
            else -> null
        }
    }.sortedBy { it.since }

    /** Una sessione al lavoro: l'obiettivo (/goal), lo strumento di adesso, da quanto dura il turno. */
    data class Working(val session: String, val goal: String?, val tool: String?, val toolNote: String?, val turnS: Long?, val model: String?, val context: Int?)

    /** «Al lavoro adesso»: le sessioni con un turno in corso, il turno più lungo prima. */
    fun workingRows(state: State, now: Long): List<Working> = state.sessions.filter { it.state == SessionState.BUSY }.map { s ->
        Working(s.name, s.goal?.text?.takeIf { it.isNotBlank() }, s.tool, s.toolNote?.takeIf { it.isNotBlank() }, s.turnStarted?.let { maxOf(0L, now - it) }, s.model?.let { it.label ?: it.id }, s.context)
    }.sortedByDescending { it.turnS ?: -1L }

    /** Una sessione ferma che non aspetta Franz, col suo ultimo esito. */
    data class Still(val session: String, val outcome: String?, val at: Long?)

    /** Sotto «Al lavoro adesso»: le ferme senza niente per Franz, l'esito più nuovo prima. */
    fun stillRows(state: State): List<Still> = state.sessions
        .filter { it.state != SessionState.GONE && it.state != SessionState.BUSY && waitingSince(it) == null }
        .map { s -> Still(s.name, (s.outcome?.preview?.short ?: s.outcome?.short)?.takeIf { it.isNotBlank() }, s.outcome?.at) }
        .sortedByDescending { it.at ?: -1L }

    /** Una riga di «Esiti di oggi»: un esito (`ref` null) o un compito rosso (`ref` = id del compito). */
    data class Done(val session: String, val text: String, val at: Long, val ref: String?)

    /** «Esiti di oggi»: i rossi in cima, poi gli esiti, il più nuovo prima; `atLeast` = la cronologia è al tetto. */
    data class Outcomes(val red: List<Done>, val done: List<Done>, val atLeast: Boolean)

    /** Gli stessi eventi contati da [head]; null senza cronologia. */
    fun outcomes(page: TimelinePage?, now: Long, zone: ZoneId): Outcomes? {
        page ?: return null
        val from = midnight(now, zone)
        val today = page.sessions.flatMap { s -> s.events.filter { it.at in from..now }.map { s.session to it } }.sortedByDescending { it.second.at }
        return Outcomes(
            red = today.filter { it.second.kind == "task" && it.second.ok == false }.map { (n, e) -> Done(n, e.text, e.at, e.ref) },
            done = today.filter { it.second.kind == "outcome" }.map { (n, e) -> Done(n, e.text, e.at, null) },
            atLeast = page.more == true || page.sessions.any { it.events.size >= EVENTS_CAP },
        )
    }

    /** 14 · l'anello della complicazione: la quota 5h dell'account e, al centro, quante sessioni ti aspettano. */
    data class Ring(val quota: Int?, val waiting: Int)

    fun ring(state: State?, account: String): Ring = Ring(state?.quota?.get(account)?.h5, state?.sessions.orEmpty().count { it.state != SessionState.GONE && waitingSince(it) != null })

    /** 16 · il riepilogo della notifica fissa; null senza stato. */
    data class Pinned(val waiting: Int, val oldestWaitS: Long?, val quota: Int?, val resetAt: Long?)

    fun pinned(state: State?, now: Long): Pinned? {
        state ?: return null
        val account = Accounts.personalQuota(state) ?: state.quota.keys.firstOrNull()
        val q = account?.let { state.quota[it] }
        val waits = state.sessions.filter { it.state != SessionState.GONE }.mapNotNull { waitingSince(it) }
        return Pinned(waits.size, waits.minOrNull()?.let { now - it }, q?.h5, q?.resetH5?.takeIf { it > now })
    }

    // ---- Proposte coi dati nuovi del relay (8, 9, 10, 12, 13, 17; contratto 1.96, compito app-kpi-dati-relay-25-10) ----

    /** 8 · la barra d'età di un compito: piena da un giorno in su. */
    fun ageFraction(ageS: Long): Float = (ageS / 86_400f).coerceIn(0f, 1f)

    /** 8 · i compiti fermi prima, poi i più vecchi. */
    fun tasksOrder(t: KpiTasks): List<KpiTask> = t.tasks.sortedWith(compareByDescending<KpiTask> { it.stalled }.thenByDescending { it.ageS })

    /** 9 · i verdi e i rossi della settimana. */
    fun checksTotals(days: List<KpiChecksDay>): Pair<Int, Int> = days.sumOf { it.green } to days.sumOf { it.red }

    /** 10 · i turni di oggi di una sessione; vuoto se il relay non ne ha. */
    fun turnsOf(data: KpiData?, session: String): List<KpiTurn> = data?.turnsToday.orEmpty().firstOrNull { it.session == session }?.turns.orEmpty()

    data class TurnsSummary(val n: Int, val minutes: Long, val failed: Int, val tokensFirst: Long, val tokensLast: Long, val tokensMax: Long)

    /** 10 · quanti turni, i minuti in tutto, i falliti e il costo del primo, dell'ultimo e del più caro. */
    fun turnsSummary(turns: List<KpiTurn>): TurnsSummary = TurnsSummary(
        n = turns.size, minutes = (turns.sumOf { (it.end - it.start).coerceAtLeast(0) } + 30) / 60, failed = turns.count { it.outcome == "fail" },
        tokensFirst = turns.firstOrNull()?.tokens ?: 0, tokensLast = turns.lastOrNull()?.tokens ?: 0, tokensMax = turns.maxOfOrNull { it.tokens } ?: 0,
    )

    /** 13 · la parte accettata in percento; null senza suggerimenti nei sette giorni. */
    fun acceptRate(s: KpiSuggestions): Int? {
        val total = suggestionsTotal(s)
        return if (total == 0) null else Math.round(s.accepted * 100f / total)
    }

    fun suggestionsTotal(s: KpiSuggestions): Int = s.accepted + s.discussed + s.later + s.never + s.expired

    /** 17 · le attese chiuse nella settimana e la mediana più alta, per la scala delle colonne. */
    fun waitsClosed(w: KpiWaits): Int = w.week.sumOf { it.n }
    fun waitsTop(w: KpiWaits): Double = w.week.mapNotNull { it.medianMin }.maxOrNull() ?: 0.0
}
