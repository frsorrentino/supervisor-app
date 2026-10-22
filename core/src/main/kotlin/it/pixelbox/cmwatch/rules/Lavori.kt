package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.DonePage
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.State
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * La pagina unica «Lavori» (mockup approvato da Franz il 22/10/2026 alle 12:43, `docs/mockup/2026-10-22-lavori/`), che
 * prende il posto di Registro, Recap e della sezione Impegni della home. Quattro blocchi ordinati per domanda, non per
 * fonte: «Per te» (solo quello che aspetta Franz, ognuno col suo tasto), «In corso» (un compito per riga con la barra dei
 * sette giorni), «Stanotte» (la coda, dallo stato) e «Fatto» (per giorno, con la prova). Fonti: `state.commitments` e
 * `state.approvals` (contratto 1.76), l'agenda (op `agenda`), gli eventi `task_check` (1.57); la prova e i giorni prima
 * arrivano con l'op `done` del contratto 1.77 (`docs/richieste/2026-10-22-lavori-fatto.md`).
 */
object Lavori {
    enum class Kind { OK, OUTSIDE, CLAUDE }

    /**
     * Una riga di «Per te». OK = un ok da dare (`approval` e/o `commitment`); OUTSIDE = una cosa da fare fuori dalla chat
     * (una riga dell'agenda che blocca su Franz, o una pausa che aspetta la persona: `commitment` con `paused`); CLAUDE =
     * una riga dell'agenda che può fare Claude. `who` = chi chiede o chi si sblocca; `at` = quando, epoch s, o null.
     */
    data class ForYou(
        val kind: Kind, val title: String, val who: String?, val at: Long?,
        val approval: Approval? = null, val commitment: Commitment? = null, val agenda: AgendaRow? = null,
    )

    data class ForYouGroups(val ok: List<ForYou>, val outside: List<ForYou>, val claude: List<ForYou>) {
        val total get() = ok.size + outside.size + claude.size
    }

    fun forYou(state: State, agenda: AgendaPage?, now: Long, today: LocalDate): ForYouGroups {
        val rows = Commitments.rows(state, now)
        // Gli impegni che aspettano Franz (non in pausa), con l'ok del registro collegato quando c'è: un ok solo, mai due volte.
        val linked = rows.filter { it.yourTurn && it.c.paused == null }
            .map { ForYou(Kind.OK, it.c.title, it.c.session, it.approval?.requestedAt?.takeIf { t -> t > 0 } ?: it.c.lastStep?.at, approval = it.approval, commitment = it.c) }
        val taken = linked.mapNotNull { it.approval?.task }.toSet()
        val loose = state.approvals.filter { it.task !in taken }
            .map { ForYou(Kind.OK, it.title, it.where, it.requestedAt.takeIf { t -> t > 0 }, approval = it) }
        val ok = (linked + loose).sortedWith(compareBy(nullsLast()) { it.at })
        val m = RecapAgenda.of(agenda, null, today)
        val outside = m.you.map { ForYou(Kind.OUTSIDE, it.title, it.ref.takeIf { r -> r.isNotBlank() }, null, agenda = it) } +
            state.commitments.orEmpty().filter { it.paused != null && it.waits?.on == Commitments.PERSON }
                .map { ForYou(Kind.OUTSIDE, it.paused!!.reason, it.session, it.paused.at.takeIf { t -> t > 0 }, commitment = it) }
        val claude = m.claude.map { ForYou(Kind.CLAUDE, it.title, it.ref.takeIf { r -> r.isNotBlank() }, null, agenda = it) }
        return ForYouGroups(ok, outside, claude)
    }

    /** Un tratto della barra, frazioni 0..1 dell'asse; `dashed` = in pausa. */
    data class Bar(val from: Double, val to: Double, val dashed: Boolean)

    /** Sette giorni: `start` = mezzanotte locale del primo, `end` = mezzanotte di domani; `days` = etichette con la frazione di inizio. */
    data class Axis(val start: Long, val end: Long, val days: List<Pair<String, Double>>, val now: Double)

    /** La larghezza minima di un tratto, perché un impegno appena approvato si veda. */
    const val MIN_BAR = 0.01

    fun axis(now: Long, zone: ZoneId, days: Int = 7): Axis {
        val today = Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
        val start = today.minusDays((days - 1).toLong()).atStartOfDay(zone).toEpochSecond()
        val end = today.plusDays(1).atStartOfDay(zone).toEpochSecond()
        val labels = (0 until days).map { i ->
            val d = today.minusDays((days - 1 - i).toLong())
            d.dayOfMonth.toString() to frac(d.atStartOfDay(zone).toEpochSecond(), start, end)
        }
        return Axis(start, end, labels, frac(now, start, end))
    }

    private fun frac(t: Long, start: Long, end: Long): Double = ((t - start).toDouble() / (end - start)).coerceIn(0.0, 1.0)

    /**
     * La barra di un impegno: dall'ok (o da `now − age_s` senza `approved_at`) a adesso, tagliata all'asse; in pausa, piena
     * fino all'inizio della pausa e tratteggiata da lì; tutta tratteggiata se la pausa è più vecchia dell'inizio visibile.
     */
    fun bars(c: Commitment, axis: Axis, now: Long): List<Bar> {
        val f = { t: Long -> frac(t, axis.start, axis.end) }
        // Un ok nel futuro (orologi diversi) non rompe niente: l'inizio non supera mai adesso.
        val begin = (c.approvedAt ?: (now - c.ageS)).coerceIn(axis.start, now.coerceAtLeast(axis.start))
        val end = now.coerceIn(begin, axis.end)
        val p = c.paused
        val raw = when {
            p == null -> listOf(Bar(f(begin), f(end), false))
            p.at <= begin -> listOf(Bar(f(begin), f(end), true))
            else -> listOf(Bar(f(begin), f(p.at.coerceAtMost(end)), false), Bar(f(p.at.coerceAtMost(end)), f(end), true))
        }
        return raw.map { b ->
            if (b.to - b.from >= MIN_BAR) b else { val from = b.from.coerceAtMost(1.0 - MIN_BAR); Bar(from, from + MIN_BAR, b.dashed) }
        }
    }

    /** Una riga di «In corso»: un impegno (`row`) con le sue barre, o una riga dell'agenda ferma su terzi (`agenda`, senza barre). */
    data class Work(val row: Commitments.Row?, val agenda: AgendaRow?, val bars: List<Bar>)

    data class InProgress(val active: List<Work>, val paused: List<Work>, val others: List<Work>, val axis: Axis)

    /** `active` = non in pausa e non tocca a Franz; `paused` = in pausa (anche se aspetta la persona: resta in vista, richiusa); `others` = fermi su terzi. */
    fun inProgress(state: State, agenda: AgendaPage?, now: Long, zone: ZoneId, today: LocalDate): InProgress {
        val ax = axis(now, zone)
        val rows = Commitments.rows(state, now)
        val active = rows.filter { it.c.paused == null && !it.yourTurn }.map { Work(it, null, bars(it.c, ax, now)) }
        val paused = rows.filter { it.c.paused != null }.map { Work(it, null, bars(it.c, ax, now)) }
        val others = RecapAgenda.of(agenda, null, today).other.map { Work(null, it, emptyList()) }
        return InProgress(active, paused, others, ax)
    }

    /**
     * Un compito chiuso: `proof` = la prova in una riga («release v2.3», «commit 9f8e7d6», il file, il comando del
     * controllo), null dagli eventi (la UI scrive «controllo verde»); `url` quando la prova ha un link; `outcome` ∈
     * green|cancelled|failed.
     */
    data class Done(val task: String, val title: String, val session: String?, val at: Long, val proof: String?, val url: String? = null, val outcome: String = "green")

    /** `n` = quanti chiusi quel giorno; `rows` vuote con `n` > 0 = un giorno prima richiuso, da caricare con `done` di quel giorno. */
    data class DoneDay(val date: LocalDate, val rows: List<Done>, val n: Int = rows.size)

    /**
     * Contratto 1.77: i giorni dalle pagine dell'op `done` già caricate (una per data), più i giorni prima che la pagina di
     * ogni data elenca col solo conteggio; dal più recente. Con le pagine l'app non guarda più gli eventi.
     */
    fun doneFromPages(pages: Map<String, DonePage>, zone: ZoneId): List<DoneDay> {
        val loaded = pages.values.mapNotNull { pg ->
            val d = runCatching { LocalDate.parse(pg.date) }.getOrNull() ?: return@mapNotNull null
            d to DoneDay(d, pg.rows.map { r ->
                val pr = r.proof
                val label = pr?.let { if (it.kind == "file" || it.kind == "check" || it.kind.isBlank()) it.value else "${it.kind} ${it.value}" }?.takeIf { it.isNotBlank() }
                Done(r.task, r.title.ifBlank { r.task }, r.session, r.at, label, pr?.url, r.outcome.ifBlank { "green" })
            })
        }.toMap()
        val refs = pages.values.flatMap { it.days }.mapNotNull { ref -> runCatching { LocalDate.parse(ref.date) }.getOrNull()?.let { it to ref.n } }
            .filter { (d, _) -> d !in loaded }.groupBy({ it.first }, { it.second }).mapValues { (_, ns) -> ns.max() }
        return (loaded.values + refs.map { (d, n) -> DoneDay(d, emptyList(), n) }).sortedByDescending { it.date }
    }

    /** I `task_check` verdi (`ok`, o ✓ nel titolo con un relay prima della 1.78) degli ultimi `days` giorni, uno per compito (l'ultimo), per giorno dal più recente; `title` = la prima riga del corpo. */
    fun done(events: List<Event>, zone: ZoneId, today: LocalDate, days: Int = 7): List<DoneDay> {
        val first = today.minusDays((days - 1).toLong())
        // Contratto 1.78: l'esito sta in `ok`; con un relay precedente lo dice il ✓ in testa al titolo.
        return events.filter { it.kind == EventKind.TASK_CHECK && (it.ok ?: it.title.startsWith("✓")) && !it.ref.isNullOrBlank() }
            .groupBy { it.ref!! }.values.map { es -> es.maxBy { it.ts } }
            .map { e -> Done(e.ref!!, e.body.lineSequence().firstOrNull()?.trim().orEmpty().ifEmpty { e.ref!! }, e.session, e.ts, null) }
            .groupBy { Instant.ofEpochSecond(it.at).atZone(zone).toLocalDate() }
            .filterKeys { !it.isBefore(first) && !it.isAfter(today) }
            .map { (d, rows) -> DoneDay(d, rows.sortedByDescending { it.at }) }
            .sortedByDescending { it.date }
    }
}
