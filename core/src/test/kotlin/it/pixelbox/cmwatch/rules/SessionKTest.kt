package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.KpiData
import it.pixelbox.cmwatch.contract.KpiSessionTurns
import it.pixelbox.cmwatch.contract.KpiTurn
import it.pixelbox.cmwatch.contract.TimelineEvent
import it.pixelbox.cmwatch.contract.TimelinePage
import it.pixelbox.cmwatch.contract.TimelineSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** La scheda K della sessione (app-scheda-sessione-semplice-19-11), coi turni veri di joyconceptstore del 19/11. */
class SessionKTest {
    private fun t(h: Int, m: Int, min: Int, outcome: String = "ok", text: String? = null): KpiTurn {
        val s = (h * 3600 + m * 60).toLong()
        return KpiTurn(s, s + min * 60, outcome, text = text)
    }
    private val joy = listOf(t(16, 40, 25, text = "categorie di 252 prodotti corrette"), t(9, 4, 14), t(9, 28, 1), t(9, 35, 6), t(9, 49, 9), t(16, 34, 2, "fail"))

    @Test fun beforeShowsTheLastThreeOldestFirst() {
        val b = SessionK.before(joy)!!
        assertEquals(listOf(9 * 3600 + 49 * 60L, 16 * 3600 + 34 * 60L, 16 * 3600 + 40 * 60L), b.turns.map { it.start })
        assertEquals(3, b.hidden); assertEquals(6, b.total); assertEquals(57, b.minutes); assertEquals(1, b.failed)
        assertEquals(listOf(true, false, true), b.turns.map { it.ok })
        assertEquals("categorie di 252 prodotti corrette", b.turns.last().text)
        assertNull(b.turns.first().text)
    }

    @Test fun allShowsEveryTurnAndAShortTurnIsOneMinute() {
        val b = SessionK.before(joy, all = true)!!
        assertEquals(6, b.turns.size); assertEquals(0, b.hidden)
        assertEquals(1, SessionK.before(listOf(KpiTurn(0, 10, "ok")))!!.turns[0].minutes)
        assertNull(SessionK.before(emptyList())); assertNull(SessionK.before(null))
    }

    @Test fun theTaskIsTheFreshestOpenOneOfTheSession() {
        val c = listOf(
            Commitment("joy-capi-19-11", "CAPI", session = "joy", idleS = 60, next = "controllare su Meta gli eventi server"),
            Commitment("joy-old", "vecchio", session = "joy", idleS = 9000),
            Commitment("other", "altro", session = "kb", idleS = 1),
        )
        assertEquals("joy-capi-19-11", SessionK.task(c, "joy")?.task)
        assertNull(SessionK.task(c, "nessuna"))
        assertEquals("controllare su Meta gli eventi server", SessionK.nextStep(SessionK.task(c, "joy"), listOf("altro")))
        assertEquals("rileggi il log", SessionK.nextStep(null, listOf(" rileggi il log ")))
        assertNull(SessionK.nextStep(null, emptyList()))
    }

    @Test fun `la pagina Utilizzo elenca chi ha lavorato oggi, il più recente in cima`() {
        fun ev(at: Long) = TimelineEvent(at, "stop", "")
        val page = TimelinePage(0, listOf(
            TimelineSession("ieri", false, events = listOf(ev(50))),
            TimelineSession("supervisor", true, events = listOf(ev(150), ev(400))),
            TimelineSession("app", true, events = listOf(ev(300))),
        ))
        val data = KpiData(0, turnsToday = listOf(KpiSessionTurns("joyconceptstore", joy)))
        assertEquals(listOf("joyconceptstore", "supervisor", "app"), SessionK.todaySessions(page, data, 100))
        assertEquals(emptyList<String>(), SessionK.todaySessions(null, null, 100))
    }
}
