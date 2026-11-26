package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Option
import it.pixelbox.cmwatch.contract.Question
import it.pixelbox.cmwatch.contract.QuestionKind
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le voci che l'app non offre più (app-issue4-cause-app-26-11, contratto 1.161, issue #4). Gli stessi casi di
 * web/src/lib/vociAssenti.test.ts.
 */
class VociAssentiTest {
    private fun q(foot: List<String>? = null, allowAll: Boolean? = null) = Question(
        id = "q1", kind = QuestionKind.PERMISSION, text = "Esegui?", options = listOf(Option(1, "Sì"), Option(2, "No")),
        tier = Tier.LOW, askedAt = 0, allowAll = allowAll, foot = foot,
    )
    private val fixture = it.pixelbox.cmwatch.contract.ContractJson.decodeState(java.io.File("../contract/state-1-question.json").readText())
    private fun sess(state: SessionState, attached: Boolean = false, atPc: Boolean = false) = fixture.sessions.first().copy(name = "s1", state = state, attached = attached, atPc = atPc)

    @Test fun chatAndTypeFollowTheFoot() {
        assertTrue(VociAssenti.chat(q()))
        assertTrue(VociAssenti.type(q()))
        assertTrue(VociAssenti.chat(q(foot = listOf("type", "chat"))))
        assertFalse(VociAssenti.chat(q(foot = listOf("type"))))
        assertFalse(VociAssenti.type(q(foot = listOf("chat"))))
        assertFalse(VociAssenti.chat(q(foot = emptyList())))
    }

    @Test fun allowAllOnlyWhenThere() {
        assertTrue(VociAssenti.allowAll(q()))
        assertTrue(VociAssenti.allowAll(q(allowAll = true)))
        assertFalse(VociAssenti.allowAll(q(allowAll = false)))
    }

    @Test fun nightOnlyWithTheOp() {
        assertTrue(VociAssenti.night(null))
        assertTrue(VociAssenti.night(listOf("answer", "night")))
        assertFalse(VociAssenti.night(listOf("answer", "agenda")))
    }

    @Test fun reopenOnlyClosed() {
        assertTrue(VociAssenti.reopen(sess(SessionState.GONE)))
        assertFalse(VociAssenti.reopen(sess(SessionState.IDLE)))
        assertTrue(VociAssenti.reopen(null))
    }

    @Test fun slashOnlyToALiveSession() {
        val st: State = fixture.copy(sessions = listOf(sess(SessionState.IDLE), sess(SessionState.GONE).copy(name = "gone")))
        assertTrue(VociAssenti.slash(st, "s1"))
        assertFalse(VociAssenti.slash(st, "gone"))
        assertFalse(VociAssenti.slash(st, "sparita"))
        assertFalse(VociAssenti.slash(null, "s1"))
    }

    @Test fun closeNotAttachedNorBackground() {
        assertTrue(VociAssenti.close(sess(SessionState.IDLE)))
        // app-chiudi-sessioni-ferme-26-11: attaccata e ferma si chiude; con Franz al PC (at_pc) no.
        assertTrue(VociAssenti.close(sess(SessionState.IDLE, attached = true)))
        assertFalse(VociAssenti.close(sess(SessionState.IDLE, attached = true, atPc = true)))
        listOf("busy", "question", "background", "gone").forEach { assertFalse(VociAssenti.close(sess(SessionState.IDLE, attached = true), it), it) }
        assertTrue(VociAssenti.close(sess(SessionState.IDLE, attached = true), "attached"))
    }

    @Test fun typedRefusal() {
        assertEquals("atlas-shop", VociAssenti.typedIn("supervisor talk: nella casella di «atlas-shop» c'è del testo digitato: «ciao»"))
        assertNull(VociAssenti.typedIn("atlas-shop is not running"))
        assertNull(VociAssenti.typedIn(null))
    }
}
