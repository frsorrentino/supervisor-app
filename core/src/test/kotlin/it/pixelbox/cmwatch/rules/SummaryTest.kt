package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.Summary.Group
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Il riepilogo unico (design 03/10): ogni sessione una volta, nell'ordine del bisogno. */
class SummaryTest {
    private val zone = ZoneId.of("Europe/Rome")
    private fun at(h: Int, m: Int = 0) = LocalDate.of(2026, 10, 3).atTime(h, m).atZone(zone).toEpochSecond()
    private fun s(name: String, st: SessionState = SessionState.IDLE, since: Long = at(9)) =
        Session(id = name, name = name, account = "personale", project = name, state = st, since = since)
    private fun q(id: String, asked: Long) = Question(id, QuestionKind.ASK, "Pubblico?", emptyList(), Tier.LOW, asked)
    private fun done(at: Long) = Outcome("Fatto", "Test verdi.\nProssimi: tagga · apri la PR", at)
    private fun st(vararg ss: Session) = State(v = 1, ts = at(15), host = "pc", sessions = ss.toList())
    private fun build(state: State, sent: List<Sent> = emptyList()) = Summary.build(state, emptyList(), sent, at(15), zone, emptySet())

    /**
     * Franz 23/10 19:34: «i tasti di riapprovazione starebbero meglio nelle schede della sessione»: gli ok del registro stanno
     * nella card della sessione che li chiede (l'impegno con lo stesso compito ne dà la sessione, o il progetto), dal più
     * vecchio, ognuno col suo Approva; una sessione chiusa non li prende, così restano fra le righe «Per te».
     */
    @Test fun approvalsSitInTheCardOfTheSessionThatAsks() {
        val a1 = Approval("t1", "supervisor-app: uno", requestedAt = at(14))
        val a2 = Approval("t2", "supervisor-app: due", requestedAt = at(14, 30))
        val a3 = Approval("t3", "curriculum: ok", requestedAt = at(14, 10))
        val a4 = Approval("t4", "orfano", requestedAt = at(14, 20))
        val state = st(s("supervisor-app", SessionState.WAITING).copy(needsYou = NeedsYou("ok?", at(14))), s("curriculum"), s("gone", SessionState.GONE))
            .copy(approvals = listOf(a2, a1, a3, a4), commitments = listOf(
                Commitment("t1", "uno", session = "supervisor-app", state = "awaiting_ok"),
                Commitment("t2", "due", project = "/home/franz/personali/supervisor-app", state = "awaiting_ok"),
                Commitment("t3", "ok", session = "curriculum", state = "awaiting_ok"),
                Commitment("t4", "orfano", session = "gone", state = "awaiting_ok"),
            ))
        val m = build(state)
        assertEquals(listOf(a1, a2), m.rows.first { it.session.name == "supervisor-app" }.approvals)
        assertEquals(listOf(a3), m.rows.first { it.session.name == "curriculum" }.approvals)
        assertEquals(setOf("t1", "t2", "t3"), Summary.attachedTasks(m.rows))
    }

    @Test fun groupsInOrderOfNeed() {
        val m = build(st(
            s("idle"), s("busy", SessionState.BUSY),
            s("asks", SessionState.WAITING).copy(question = q("1", at(14))),
            s("fin").copy(outcome = done(at(14, 50)), followed = true),
        ))
        assertEquals(listOf(Group.WAITING, Group.FINISHED, Group.WORKING, Group.STILL), m.rows.map { it.group })
        assertEquals(listOf("asks", "fin", "busy", "idle"), m.rows.map { it.session.name })
    }

    // Franz, 03/10 20:31: sulla card la quota delle 5 ore dell'account al posto del «da N m».
    @Test fun eachRowCarriesItsAccountQuota() {
        val work = s("cli", SessionState.BUSY).copy(account = "professionale")
        val quota = mapOf("personale" to QuotaAccount(h5 = 4), "professionale" to QuotaAccount(h5 = 62, stale = true))
        val m = build(st(s("idle"), work).copy(quota = quota))
        assertEquals(listOf(62, 4), m.rows.map { it.quota?.h5 })
        assertEquals(listOf(true, false), m.rows.map { it.quota?.stale })
        assertNull(build(st(s("idle"))).rows.single().quota)
    }

    @Test fun aFinishedSessionIsNotAlsoStill() {
        val m = build(st(s("fin").copy(outcome = done(at(14, 50)), followed = true)))
        assertEquals(listOf(Group.FINISHED), m.rows.map { it.group })
    }

    // Franz, 21/10 21:15: la master non sta fra le sessioni quando chiede o ha finito; quello che chiede sta nella sua barra e
    // nel campo di scrittura. Dal 24/10 18:49 una scheda sua solo con un ok o una richiesta per Franz (sotto).
    @Test fun theMasterIsListedOnlyForFranz() {
        assertTrue(build(st(s("master", SessionState.BUSY))).rows.isEmpty())
        assertTrue(build(st(s("master", SessionState.WAITING).copy(question = q("1", at(14))))).rows.isEmpty())
        assertTrue(build(st(s("master").copy(outcome = done(at(14, 50)), followed = true))).rows.isEmpty())
        val blocked = build(st(s("master").copy(turnBlocking = TurnBlocking("ok al push", at(14))))).rows.single()
        assertEquals(Group.WAITING to "ok al push", blocked.group to blocked.text)
        val m = build(st(s("master", SessionState.WAITING).copy(question = q("1", at(14))), s("a", SessionState.WAITING).copy(question = q("2", at(14, 5)))))
        assertEquals(listOf("a"), m.rows.map { it.session.name })
        assertEquals("master", m.master?.name)
    }

    @Test fun closedAreOneRow() {
        val m = build(st(s("a"), s("x", SessionState.GONE), s("y", SessionState.GONE)))
        assertEquals(listOf("x", "y"), m.closed.map { it.name })
        assertTrue(m.rows.none { it.session.state == SessionState.GONE })
    }

    @Test fun openCountsLiveSessionsWithoutTheMaster() = assertEquals(2, build(st(s("a"), s("b"), s("master"), s("x", SessionState.GONE))).open)

    @Test fun stillMostRecentFirst() =
        assertEquals(listOf("new", "old"), build(st(s("old", since = at(8)), s("new", since = at(12)))).rows.map { it.session.name })

    @Test fun noMasterStillBuilds() {
        val m = build(st(s("a")))
        assertNull(m.master); assertEquals(1, m.rows.size)
    }

    @Test fun serviceRowsAreTheOtherKinds() {
        val state = st(s("a")).copy(night = Night(items = emptyList()))
        val m = Summary.build(state, emptyList(), emptyList(), at(21), zone, emptySet())
        assertEquals(listOf(MasterHome.Kind.NIGHT), m.service.map { it.kind })
    }

    // Revisione finale: uno stato di passaggio del relay non nasconde e non raddoppia una sessione.
    @Test fun waitingWithoutQuestionIsStillListed() =
        assertEquals(listOf("w"), build(st(s("w", SessionState.WAITING))).rows.map { it.session.name })

    @Test fun busyWithAQuestionAppearsOnce() {
        val m = build(st(s("b", SessionState.BUSY).copy(question = q("1", at(14)))))
        assertEquals(listOf(Group.WAITING), m.rows.map { it.group })
    }

    // Contratto 1.68: chi è ferma per Franz sta in cima con le domande, col motivo, dalla più vecchia; una volta sola.
    @Test fun sessionsStoppedForFranzJoinTheQuestionsAtTheTop() {
        val m = build(st(
            s("busy", SessionState.BUSY),
            s("asks", SessionState.WAITING).copy(question = q("1", at(14))),
            s("login").copy(needsYou = NeedsYou("login su win", at(13))),
            s("deploy").copy(turnBlocking = TurnBlocking("ok al deploy", at(14, 30)), outcome = done(at(14, 30)), followed = true),
        ))
        assertEquals(listOf("login", "asks", "deploy", "busy"), m.rows.map { it.session.name })
        assertEquals(listOf(Group.WAITING, Group.WAITING, Group.WAITING, Group.WORKING), m.rows.map { it.group })
        assertEquals(listOf("login su win", "Pubblico?", "ok al deploy"), m.rows.take(3).map { it.text })
    }

    /** Franz 24/10 18:49 (ok-master-dove-24-10): gli ok della master nella sua scheda, fra quelle che aspettano, non in Lavori. */
    @Test fun theMastersOksGetItsCard() {
        val a1 = Approval("ads", "Ads Widget su Play", requestedAt = at(14, 4), options = listOf("Non etichettare", "Etichetta come AI"))
        val state = st(s("master", SessionState.BUSY), s("a", SessionState.WAITING).copy(question = q("2", at(14, 5))))
            .copy(approvals = listOf(a1), commitments = listOf(Commitment("ads", "Ads", session = "master", state = "awaiting_ok")))
        val m = build(state)
        assertEquals(listOf("master", "a"), m.rows.map { it.session.name })
        assertEquals(listOf(a1), m.rows.first().approvals)
        assertNull(m.rows.first().text)
        assertEquals(setOf("ads"), Summary.attachedTasks(m.rows))
    }
}
