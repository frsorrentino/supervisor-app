package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Gli stessi casi di closeFinished.test.ts (contratto 1.84, B4). */
class CloseFinishedTest {
    private val now = 100_000L
    private fun row(name: String, group: Summary.Group, state: SessionState = SessionState.IDLE, since: Long = now - 7200, attached: Boolean = false) =
        Summary.Row(group, Session(id = name, name = name, account = "p", project = name, state = state, since = since, attached = attached), null, null)

    @Test fun candidatesAreTheStillOnesIdleForAnHour() {
        val rows = listOf(row("curriculum", Summary.Group.STILL), row("officina", Summary.Group.STILL, attached = true), row("fresca", Summary.Group.STILL, since = now - 600),
            row("blog", Summary.Group.WAITING), row("app", Summary.Group.WORKING, SessionState.BUSY))
        assertEquals(listOf("curriculum"), CloseFinished.candidates(rows, now).map { it.name })
    }

    @Test fun previewSaysWhatIsSavedAndWhoStays() {
        val p = CloseFinished.read("""{"sessions": [{"name": "curriculum", "closable": true, "blocked_by": null, "save": {"outcome": "CV inviato", "next": "Aspettare", "session_id": "3f2a", "dirty_n": 2, "dirty": ["cv.docx", "note.md"]}}, {"name": "app", "closable": false, "blocked_by": "busy"}]}""")
        assertEquals(2, p?.sessions?.first()?.save?.dirtyN)
        assertEquals(listOf("curriculum"), CloseFinished.closable(p!!))
        assertNull(CloseFinished.read("rotto"))
    }
}
