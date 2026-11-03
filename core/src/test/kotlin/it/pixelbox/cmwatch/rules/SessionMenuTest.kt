package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.DecisionGroup
import it.pixelbox.cmwatch.contract.DecisionItem
import it.pixelbox.cmwatch.contract.DecisionsPage
import it.pixelbox.cmwatch.contract.ProveGroup
import it.pixelbox.cmwatch.contract.ProveItem
import it.pixelbox.cmwatch.contract.ProvePage
import it.pixelbox.cmwatch.contract.Prova
import it.pixelbox.cmwatch.contract.ResultItem
import it.pixelbox.cmwatch.contract.RimastoItem
import it.pixelbox.cmwatch.contract.SeguitiPage
import it.pixelbox.cmwatch.contract.ResultsSummary
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.SessionMenu.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

/** Un menu della sessione, lo stesso da home, chat e plancia (app-menu-sessione-03-11). */
class SessionMenuTest {
    private fun s(name: String = "kb", state: SessionState = SessionState.IDLE, link: String = "", handoff: String? = null) =
        Session(id = name, name = name, account = "personal", project = "p", state = state, since = 0, link = link, handoff = handoff)

    private fun st(vararg ss: Session, slash: List<String>? = null) = State(v = 1, ts = 0, host = "pc", sessions = ss.toList(), slash = slash)

    private fun kinds(items: List<SessionMenu.Item>) = items.map { it.kind }

    @Test fun everyEntryInItsPlaceWhenTheSessionHasEverything() {
        val kb = s(link = "https://claude.ai/code/x", handoff = "/fable-director:handoff")
        val items = SessionMenu.entries(
            kb, st(kb, s("master"), slash = listOf("clear")), SessionMenu.Mine(prove = 2, decidi = 1, guarda = 3, seguiti = 1),
            changes = true, terminal = true, canExit = true,
        )
        assertEquals(
            listOf(Kind.CHANGES, Kind.PROVE, Kind.DECIDI, Kind.GUARDA, Kind.SEGUITI, Kind.FOLLOW, Kind.ASK_MASTER, Kind.OPEN_IN_CLAUDE, Kind.TERMINAL, Kind.RESTART_CLEAN, Kind.EXIT),
            kinds(items),
        )
        assertEquals(listOf(2, 1, 3, 1), items.filter { it.kind in setOf(Kind.PROVE, Kind.DECIDI, Kind.GUARDA, Kind.SEGUITI) }.map { it.n })
    }

    @Test fun emptyEntriesDoNotAppear() {
        val kb = s()
        assertEquals(listOf(Kind.FOLLOW), kinds(SessionMenu.entries(kb, st(kb))))
    }

    @Test fun theMasterIsNotAskedAboutItself() {
        val m = s("master")
        assertEquals(listOf(Kind.FOLLOW), kinds(SessionMenu.entries(m, st(m))))
        // Senza una master aperta niente «Fai controllare».
        val kb = s()
        assertEquals(listOf(Kind.FOLLOW), kinds(SessionMenu.entries(kb, st(kb, s("master", SessionState.GONE)))))
    }

    @Test fun restartCleanNeedsAHandoffCommandAndClear() {
        val kb = s(handoff = "/fable-director:handoff")
        assertEquals(false, Kind.RESTART_CLEAN in kinds(SessionMenu.entries(kb, st(kb))))
        assertEquals(false, Kind.RESTART_CLEAN in kinds(SessionMenu.entries(s(), st(s(), slash = listOf("clear")))))
        assertEquals(true, Kind.RESTART_CLEAN in kinds(SessionMenu.entries(kb, st(kb, slash = listOf("clear")))))
    }

    @Test fun aClosedSessionKeepsTheEntriesThatStillWorkButCannotRestartOrClose() {
        val gone = s(state = SessionState.GONE, link = "https://claude.ai/code/x", handoff = "/h")
        assertEquals(
            listOf(Kind.FOLLOW, Kind.OPEN_IN_CLAUDE),
            kinds(SessionMenu.entries(gone, st(gone, slash = listOf("clear")), canExit = true)),
        )
    }

    @Test fun unseenResultsCountOnlyThisSessionsUnseenOnes() {
        val r = ResultsSummary(
            nUnseen = 4,
            items = listOf(ResultItem("1", session = "kb"), ResultItem("2", session = "kb", seenAt = 5), ResultItem("3", session = "web"), ResultItem("4", session = "kb")),
        )
        assertEquals(2, SessionMenu.unseenResults(st(s()).copy(results = r), "kb"))
        assertEquals(0, SessionMenu.unseenResults(st(s()), "kb"))
    }

    @Test fun theNumbersOfYourTurnCountOnlyThisSession() {
        val decisions = DecisionsPage(groups = listOf(DecisionGroup("g", items = listOf(
            DecisionItem(kind = "ok", task = "a", session = "kb"),
            DecisionItem(kind = "choice", task = "b", session = "kb"),
            DecisionItem(kind = "ok", task = "c", session = "web"),
            // Una pausa con la prova va in Prova (contratto 1.107), anche se la stessa voce è nella pagina Prova.
            DecisionItem(kind = "pause", task = "d", session = "kb", prova = Prova(project = "p", release = "r")),
        ))))
        val prove = ProvePage(groups = listOf(ProveGroup("r", prove = listOf(ProveItem(1, "d", session = "kb"), ProveItem(2, "e", session = "kb"), ProveItem(3, "f", session = "web")))))
        val seguiti = SeguitiPage(rimasto = listOf(RimastoItem("p", "x", session = "kb"), RimastoItem("p", "y", session = "web")))
        val r = ResultsSummary(1, listOf(ResultItem("1", session = "kb")))
        val m = SessionMenu.mine(st(s()).copy(results = r), "kb", prove, decisions, seguiti)
        assertEquals(SessionMenu.Mine(prove = 2, decidi = 2, guarda = 1, seguiti = 1), m)
        // Pagine non lette: zero, e la voce non compare.
        assertEquals(SessionMenu.Mine(), SessionMenu.mine(st(s()), "kb", null, null, null))
    }

    @Test fun withContract1111TheRelayNumbersWin() {
        // La fixture del relay: atlas-shop porta tocca {2, 1, 3, 1}; le pagine lette non contano più.
        val st = it.pixelbox.cmwatch.contract.ContractJson.decodeState(java.io.File("../contract/state-2-idle.json").readText())
        val pages = DecisionsPage(groups = listOf(DecisionGroup("g", items = List(9) { i -> DecisionItem(kind = "ok", task = "t$i", session = "atlas-shop") })))
        assertEquals(SessionMenu.Mine(prove = 2, decidi = 1, guarda = 3, seguiti = 1), SessionMenu.mine(st, "atlas-shop", null, pages, null))
        // Una sessione senza tocca con un relay 1.111: niente da fare per lei, anche se una pagina vecchia diceva altro.
        val other = st.sessions.first { s -> s.name == "atlas-shop" }.copy(name = "web", tocca = null)
        val st2 = st.copy(sessions = st.sessions + other)
        assertEquals(SessionMenu.Mine(), SessionMenu.mine(st2, "web", null, DecisionsPage(groups = listOf(DecisionGroup("g", items = listOf(DecisionItem(kind = "ok", task = "a", session = "web"))))), null))
    }
}
