package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test

/** Voce 5 del 14/10: «@nome» nella chat della master manda a quella sessione. */
class MentionTest {
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)
    private val live = st.sessions.filter { it.state != SessionState.GONE }.map { it.name }

    @Test fun atOpensTheListOfLiveSessions() {
        assertEquals(live.filter { it != "master" }, Mention.suggest("@", st.sessions, "master").map { it.name })
        assertEquals(listOf("atlas-shop"), Mention.suggest("@atl", st.sessions, "master").map { it.name })
        assertTrue(Mention.suggest("@atlas-shop run", st.sessions, "master").isEmpty())
        assertTrue(Mention.suggest("hello @atl", st.sessions, "master").isEmpty())
    }

    @Test fun theMessageGoesToTheNamedSession() {
        assertEquals("atlas-shop" to "run the tests", Mention.parse("@atlas-shop run the tests", st.sessions, "master"))
        assertNull(Mention.parse("@atlas-shop", st.sessions, "master"))
        assertNull(Mention.parse("@nobody hi", st.sessions, "master"))
        assertNull(Mention.parse("run @atlas-shop", st.sessions, "master"))
    }
}
