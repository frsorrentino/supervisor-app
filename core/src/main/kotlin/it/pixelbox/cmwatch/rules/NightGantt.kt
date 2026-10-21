package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.NightGoal
import it.pixelbox.cmwatch.contract.NightProject
import it.pixelbox.cmwatch.contract.NightReport
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Il Gantt della pagina «Notte» dal rapporto v2 (contratto 1.74; bozze A e B approvate da Franz il 21/10 alle 10:29,
 * docs/proposte/2026-10-21-notte-gantt.md): solo i conti, cioè asse, posizioni, larghezze, stati e totali. Ogni numero
 * viene dal rapporto; le parole le scrive la schermata con le sue risorse.
 */
object NightGantt {
    /** Dove è arrivato un progetto (`where.state`); un valore nuovo del relay vale UNKNOWN. */
    enum class Where { DONE, RUNNING, PARTIAL, STOPPED, UNKNOWN }

    /** Lo stato di un obiettivo e dei suoi punti: done, running, partial, stopped, todo. */
    enum class GoalState { DONE, RUNNING, PARTIAL, STOPPED, TODO }

    /** Che cosa segna la linea tratteggiata: l'ora del rapporto fatto di notte, o la fine della notte. */
    enum class Mark { NOW, END }

    data class Tick(val hour: Int, val at: Double)
    data class Axis(val start: Long, val end: Long, val ticks: List<Tick>, val mark: Double, val markAt: Long, val markKind: Mark) {
        /** I tick ogni `step` ore, per un asse stretto (vista B). */
        fun every(step: Int): List<Tick> = ticks.filterIndexed { i, _ -> i % step == 0 }
    }
    data class Bar(val from: Double, val to: Double)

    /** Vista A: una riga per progetto. `done`/`total`: i punti fatti sui totali; `pubs`: push e release sull'asse. */
    data class ProjectRow(
        val name: String, val where: Where, val needsYou: Boolean, val done: Int, val total: Int,
        val bars: List<Bar>, val pubs: List<Double>, val line: String?,
    )
    data class Projects(val axis: Axis, val rows: List<ProjectRow>, val counts: Map<Where, Int>)

    /**
     * Vista B: un solo segmento per obiettivo, con i suoi punti dentro. L'ora del singolo punto il relay non la conosce
     * (ogni punto prende quella del suo obiettivo): una barra per punto direbbe una cosa che non sappiamo.
     */
    data class Goal(val title: String, val state: GoalState, val bar: Bar?, val doneAt: Long?, val points: List<String>)
    data class Group(val name: String, val where: Where, val needsYou: Boolean, val goals: List<Goal>, val line: String?)
    data class Totals(val done: Int, val running: Int, val partial: Int, val stopped: Int, val todo: Int) {
        val total get() = done + running + partial + stopped + todo
    }
    data class Goals(val axis: Axis, val totals: Totals, val groups: List<Group>)

    /** Una barra più corta di così non si vedrebbe. */
    const val MIN_BAR = NightPage.MIN_BAR

    /** Il rapporto ha i dati del Gantt (v2, almeno un progetto con intervalli o obiettivi). */
    fun has(r: NightReport): Boolean = r.v >= 2 && r.projects.any { it.spans.isNotEmpty() || it.goals.isNotEmpty() }

    /** L'asse va dall'ora intera prima dell'inizio della notte all'ora intera dopo l'ultima cosa successa. */
    fun axis(r: NightReport, zone: ZoneId): Axis {
        val start = floorHour(r.window.start, zone)
        val last = (listOf(r.window.end) + r.projects.flatMap { p -> p.spans.map { it.end } + p.milestones.map { it.at } + p.goals.mapNotNull { it.doneAt } }).max()
        val end = maxOf(ceilHour(last, zone), start + 3600)
        val ticks = generateSequence(start) { it + 3600 }.takeWhile { it <= end }
            .map { t -> Tick(Instant.ofEpochSecond(t).atZone(zone).hour, (t - start).toDouble() / (end - start)) }.toList()
        val markAt = r.window.end
        return Axis(start, end, ticks, frac(markAt, start, end), markAt, if (r.window.endSource == "now") Mark.NOW else Mark.END)
    }

    fun projects(r: NightReport, zone: ZoneId): Projects {
        val axis = axis(r, zone)
        val rows = ordered(r.projects).map { p ->
            val (done, total) = points(p.goals)
            ProjectRow(
                name = p.name, where = where(p), needsYou = p.where?.needsYou.orEmpty().isNotEmpty(), done = done, total = total,
                bars = p.spans.sortedBy { it.start }.map { s -> bar(s.start, s.end, axis) },
                pubs = p.milestones.filter { it.kind == "push" || it.kind == "release" }.map { m -> frac(m.at, axis.start, axis.end) },
                line = p.where?.text?.let(::plain),
            )
        }
        return Projects(axis, rows, Where.entries.associateWith { w -> rows.count { it.where == w } }.filterValues { it > 0 })
    }

    fun goals(r: NightReport, zone: ZoneId): Goals {
        val axis = axis(r, zone)
        val groups = ordered(r.projects).map { p ->
            Group(
                name = p.name, where = where(p), needsYou = p.where?.needsYou.orEmpty().isNotEmpty(), line = p.where?.text?.let(::plain),
                goals = p.goals.map { g ->
                    val st = goalState(g.state)
                    val from = goalStart(g, p)
                    Goal(
                        title = g.title, state = st, doneAt = g.doneAt, points = g.points.map { it.title },
                        bar = when {
                            st == GoalState.TODO -> null
                            st == GoalState.DONE && g.doneAt != null -> bar(from, g.doneAt, axis)
                            else -> bar(from, maxOf(from, r.window.end), axis)
                        },
                    )
                },
            )
        }
        // Un obiettivo senza punti conta come un punto solo: è la cosa da fare.
        val all = r.projects.flatMap { p -> p.goals.flatMap { g -> List(maxOf(1, g.points.size)) { goalState(g.state) } } }
        val totals = Totals(
            done = all.count { it == GoalState.DONE }, running = all.count { it == GoalState.RUNNING },
            partial = all.count { it == GoalState.PARTIAL }, stopped = all.count { it == GoalState.STOPPED }, todo = all.count { it == GoalState.TODO },
        )
        return Goals(axis, totals, groups)
    }

    /** I punti fatti e i punti in tutto di un progetto; un obiettivo senza punti vale uno. */
    fun points(goals: List<NightGoal>): Pair<Int, Int> {
        val total = goals.sumOf { maxOf(1, it.points.size) }
        val done = goals.filter { goalState(it.state) == GoalState.DONE }.sumOf { maxOf(1, it.points.size) }
        return done to total
    }

    fun where(p: NightProject): Where = when (p.where?.state) {
        "done" -> Where.DONE; "running" -> Where.RUNNING; "partial" -> Where.PARTIAL; "stopped" -> Where.STOPPED
        else -> Where.UNKNOWN
    }

    fun goalState(s: String): GoalState = when (s) {
        "done" -> GoalState.DONE; "running" -> GoalState.RUNNING; "partial" -> GoalState.PARTIAL; "stopped" -> GoalState.STOPPED
        else -> GoalState.TODO
    }

    /** «!: /clear», «domanda: …», «ok: …» del relay: il testo senza il prefisso, come in «Aspetta» della pagina. */
    private fun plain(w: String): String = Regex("""^(?:!|domanda|ok)\s*:\s*""").replace(w, "")

    /** Prima i progetti che hanno lavorato, dal primo partito; poi quelli senza intervalli, nell'ordine del file. */
    private fun ordered(ps: List<NightProject>): List<NightProject> =
        ps.withIndex().sortedWith(compareBy({ it.value.spans.minOfOrNull { s -> s.start } ?: Long.MAX_VALUE }, { it.index })).map { it.value }

    /**
     * L'obiettivo comincia quando il progetto si mette al lavoro dopo averlo ricevuto: il primo intervallo che finisce dopo
     * `at`, mai prima di `at`. Senza intervalli, `at`.
     */
    private fun goalStart(g: NightGoal, p: NightProject): Long =
        p.spans.filter { it.end >= g.at }.minOfOrNull { it.start }?.let { maxOf(it, g.at) } ?: g.at

    private fun bar(s: Long, e: Long, a: Axis): Bar {
        var from = frac(s, a.start, a.end)
        var to = frac(e, a.start, a.end)
        if (to - from < MIN_BAR) { to = minOf(1.0, from + MIN_BAR); from = maxOf(0.0, to - MIN_BAR) }
        return Bar(from, to)
    }

    private fun frac(t: Long, start: Long, end: Long) = ((t - start).toDouble() / (end - start)).coerceIn(0.0, 1.0)

    private fun floorHour(t: Long, zone: ZoneId): Long =
        Instant.ofEpochSecond(t).atZone(zone).withMinute(0).withSecond(0).withNano(0).toEpochSecond()

    private fun ceilHour(t: Long, zone: ZoneId): Long = floorHour(t, zone).let { if (it == t) it else it + 3600 }

    /** «02:27»: l'ora di fine di un obiettivo, nella vista B. */
    fun hm(t: Long, zone: ZoneId): String = DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(Instant.ofEpochSecond(t))
}
