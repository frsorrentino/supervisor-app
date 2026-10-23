package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.NeedsYou
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.TurnBlocking
import org.junit.Assert.*
import org.junit.Test

/** La coda «Ti aspettano» (piano 30/09, Task 1): tutte le domande aperte in fila, dalla più vecchia. */
class AttentionQueueTest {
    private val base = ContractJson.decodeState(Fixtures.stateQuestion)
    private val q = base.sessions.first { it.question != null }

    /** Tre sessioni con una domanda ciascuna, chieste a 300, 100 e 200. */
    private fun three(): State = base.copy(sessions = listOf(
        q.copy(id = "a", name = "a", question = q.question!!.copy(id = "qa", askedAt = 300)),
        q.copy(id = "b", name = "b", question = q.question!!.copy(id = "qb", askedAt = 100)),
        q.copy(id = "c", name = "c", question = q.question!!.copy(id = "qc", askedAt = 200)),
        q.copy(id = "d", name = "d", question = null),
    ))

    @Test fun oldestFirst() {
        assertEquals(listOf("qb", "qc", "qa"), AttentionQueue.items(three()).map { it.questionId })
        assertEquals("b", AttentionQueue.items(three()).first().session)
    }

    @Test fun nextAfterCurrent() {
        assertEquals("qc", AttentionQueue.next(three(), "qb")?.questionId)
        assertEquals("qb", AttentionQueue.next(three(), null)?.questionId)
    }

    /** L'ultima della fila risposta: si ricomincia dalla più vecchia rimasta, mai null se ce n'è ancora una. */
    @Test fun afterTheLastComesTheOldest() {
        assertEquals("qb", AttentionQueue.next(three(), "qa")?.questionId)
    }

    /** Risposta al PC mentre la coda è aperta: la domanda corrente sparisce, `next` dà la più vecchia rimasta. */
    @Test fun answeredElsewhereLeavesTheQueue() {
        val s = three().let { st -> st.copy(sessions = st.sessions.map { if (it.name == "c") it.copy(question = null) else it }) }
        assertEquals("qb", AttentionQueue.next(s, "qc")?.questionId)
        assertFalse(AttentionQueue.items(s).any { it.questionId == "qc" })
    }

    @Test fun emptyWhenNoQuestions() {
        val s = base.copy(sessions = base.sessions.map { it.copy(question = null) })
        assertTrue(AttentionQueue.items(s).isEmpty())
        assertNull(AttentionQueue.next(s, null))
    }

    /**
     * Revisione finale 01/10 (I1): la risposta toglie subito la domanda (ottimistica). La pagina dopo è quella della
     * domanda che seguiva, cercata per id nella fila già accorciata, non «indice + 1» (che saltava una domanda).
     */
    @Test fun pageAfterAnswerFollowsTheNextQuestionById() {
        val before = three()
        val target = AttentionQueue.next(before, "qb")!!.questionId
        val after = before.copy(sessions = before.sessions.map { if (it.name == "b") it.copy(question = null) else it })
        assertEquals(0, AttentionQueue.page(AttentionQueue.items(after), target))
        assertNull(AttentionQueue.page(AttentionQueue.items(after), "gone"))
    }

    /**
     * Contratto 1.68: chi è ferma per Franz conta come una domanda, anche nella coda, come nel banner (web dal 15/10,
     * `f69ba30a`). Telefono 23/10 18:39: il banner diceva «3 sessioni ti aspettano» con tre needs-you e la coda «Nessuno
     * ti aspetta».
     */
    @Test fun stopsForFranzQueueWithTheQuestions() {
        val s = three().let { st -> st.copy(sessions = st.sessions.map { if (it.name == "d") it.copy(turnBlocking = TurnBlocking("ok alla pubblicazione", 150)) else it }) }
        val l = AttentionQueue.items(s)
        assertEquals(listOf("b", "d", "c", "a"), l.map { it.session })
        assertEquals(AttentionQueue.Kind.STOP, l[1].kind)
        assertEquals("ok alla pubblicazione", l[1].text)
        assertEquals(AttentionQueue.Kind.QUESTION, l[0].kind)
    }

    /** Contratto 1.80: una ferma si chiude dal telefono col tasto «Fatto» (attention_done): kind e ref, since per una ferma del turno. */
    @Test fun aStopCarriesItsDoneAndTheExpiryOfANeedsYou() {
        val s = three().let { st -> st.copy(sessions = st.sessions.map {
            when (it.name) {
                "d" -> it.copy(needsYou = NeedsYou("login", 150, until = true, expiresAt = 999))
                "a" -> it.copy(question = null, turnBlocking = TurnBlocking("ok al deploy", 160))
                else -> it
            }
        }) }
        val l = AttentionQueue.items(s)
        assertEquals(AttentionQueue.Done("needs_you", "d"), l.first { it.session == "d" }.done)
        assertEquals(999L, l.first { it.session == "d" }.expiresAt)
        assertEquals(AttentionQueue.Done("turn_blocking", "a", 160), l.first { it.session == "a" }.done)
        assertNull(l.first { it.session == "a" }.expiresAt)
        assertNull(l.first { it.session == "b" }.done)
    }

    /** Una ferma con la domanda conta una volta, come domanda; una sessione chiusa mai, con o senza domanda, come nel banner. */
    @Test fun aStopWithAQuestionCountsOnceAndAGoneSessionNever() {
        val s = three().let { st -> st.copy(sessions = st.sessions.map {
            when (it.name) {
                "a" -> it.copy(needsYou = NeedsYou("login", 50))
                "c" -> it.copy(state = SessionState.GONE)
                "d" -> it.copy(state = SessionState.GONE, needsYou = NeedsYou("login", 50))
                else -> it
            }
        }) }
        assertEquals(listOf("b" to AttentionQueue.Kind.QUESTION, "a" to AttentionQueue.Kind.QUESTION), AttentionQueue.items(s).map { it.session to it.kind })
    }
}
