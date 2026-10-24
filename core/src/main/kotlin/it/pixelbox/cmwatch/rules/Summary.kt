package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import java.time.ZoneId

/**
 * Il riepilogo unico (design 03/10, approvato da Franz): Sessioni e casa della master fuse. Ogni sessione una volta,
 * nell'ordine del bisogno: ti aspetta, ha finito, al lavoro, ferme; le chiuse a parte; le righe di servizio in fondo.
 * La master è la cornice (in basso, vicino al campo): nella lista solo con un ok o una richiesta per Franz (24/10 18:49);
 * le sue domande e il resto stanno nella sua barra e nel campo di scrittura (segnalazione del 21/10 21:15).
 */
object Summary {
    enum class Group { WAITING, FINISHED, WORKING, STILL }
    /** `quota`: la quota dell'account della sessione; sulla card la finestra delle 5 ore (Franz, 03/10 20:31). */
    data class Row(
        val group: Group, val session: Session, val text: String?, val at: Long?, val key: String? = null, val quota: QuotaAccount? = null,
        /** Gli ok del registro che chiede questa sessione, dal più vecchio, ognuno col suo Approva nella card. */
        val approvals: List<Approval> = emptyList(),
    )
    data class Model(val rows: List<Row>, val closed: List<Session>, val service: List<MasterHome.Row>, val open: Int, val master: Session?)

    private val SERVICE = setOf(MasterHome.Kind.CONTEXT, MasterHome.Kind.NIGHT_REPORT, MasterHome.Kind.NIGHT, MasterHome.Kind.NEXT_STEP, MasterHome.Kind.SCHEDULED)

    /**
     * Franz 23/10 19:34: «i tasti di riapprovazione starebbero meglio nelle schede della sessione»: gli ok del registro
     * stanno nella card della sessione che li chiede, non in schede a parte. L'impegno con lo stesso compito dice la
     * sessione; senza sessione vale il suo progetto. Una sessione chiusa non ha card: il suo ok resta fra le righe «Per te».
     */
    fun approvalsOf(state: State, s: Session): List<Approval> = state.approvals.filter { a ->
        val c = state.commitments?.firstOrNull { it.task == a.task } ?: return@filter false
        c.session == s.name || (c.session == null && c.project != null && s.project.isNotBlank() && (c.project.endsWith(s.project) || s.project.endsWith(c.project)))
    }.sortedBy { it.requestedAt }

    /** I compiti i cui ok stanno già in una card: le righe «Per te» della casa non li ripetono. */
    fun attachedTasks(rows: List<Row>): Set<String> = rows.flatMap { r -> r.approvals.map { it.task } }.toSet()

    fun build(state: State, events: List<Event>, sent: List<Sent>, now: Long, zone: ZoneId, read: Set<String>): Model {
        val live = state.sessions.filter { it.state != SessionState.GONE }
        val master = live.firstOrNull { it.name == ContextActions.MASTER }
        val others = live.filter { it.name != ContextActions.MASTER }
        val forYou = MasterHome.forYou(state, events, sent, now, zone, read, limit = Int.MAX_VALUE).rows
        // Contratto 1.68: chi è ferma per Franz conta come una domanda, col motivo; tutte dalla più vecchia.
        // Franz 24/10 18:49 (ok-master-dove-24-10): la master ha una scheda come le altre, ma solo quando ha qualcosa per
        // Franz, un ok del registro (con o senza opzioni) o una richiesta ferma; il resto di quello che chiede sta nella barra.
        val masterRow = master?.let { m ->
            val oks = approvalsOf(state, m)
            val x = StrongAlert.stop(m)
            if (oks.isEmpty() && x == null) null else Row(Group.WAITING, m, x?.text, x?.at ?: oks.first().requestedAt)
        }
        val waiting = (others.mapNotNull { s ->
            s.question?.let { Row(Group.WAITING, s, it.text, it.askedAt) }
                ?: StrongAlert.stop(s)?.let { Row(Group.WAITING, s, it.text, it.at) }
        } + listOfNotNull(masterRow)).sortedBy { it.at }
        val stopped = waiting.map { it.session.name }.toSet()
        val finished = forYou.filter { it.kind == MasterHome.Kind.FINISHED && it.session !in stopped }
            .mapNotNull { r -> others.firstOrNull { it.name == r.session }?.let { Row(Group.FINISHED, it, r.detail, r.at, r.key) } }
        val asking = waiting.map { it.session.name }.toSet()
        val working = MasterHome.working(state).filter { it.session.name !in asking }
            .map { Row(Group.WORKING, it.session, it.detail, it.session.turnStarted ?: it.session.since) }
        val taken = (waiting + finished + working).map { it.session.name }.toSet()
        // Ogni sessione viva resta nella lista anche in uno stato di passaggio del relay (in attesa senza domanda).
        val still = others.filter { it.name !in taken }.sortedByDescending { it.since }
            .map { Row(Group.STILL, it, it.outcome?.full, it.since) }
        return Model(
            rows = (waiting + finished + working + still).map { it.copy(quota = state.quota[it.session.account], approvals = approvalsOf(state, it.session)) },
            closed = state.sessions.filter { it.state == SessionState.GONE },
            service = forYou.filter { it.kind in SERVICE },
            open = others.size,
            master = master,
        )
    }
}
