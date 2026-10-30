package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class RailTest {
    private fun s(state: SessionState) = Session(id = "a", name = "a", account = "p", project = "p", state = state, since = 0)

    @Test fun spanGoesFromMinutesToDays() {
        assertEquals(Rail.Span(0, Rail.Unit.MIN), Rail.span(100, 50))
        assertEquals(Rail.Span(59, Rail.Unit.MIN), Rail.span(0, 59 * 60 + 59))
        assertEquals(Rail.Span(1, Rail.Unit.H), Rail.span(0, 3600))
        assertEquals(Rail.Span(1, Rail.Unit.D), Rail.span(0, 86400))
    }

    @Test fun glyphFollowsTheState() {
        assertEquals(Rail.Glyph.WAITING, Rail.glyph(s(SessionState.WAITING)))
        assertEquals(Rail.Glyph.BUSY, Rail.glyph(s(SessionState.BUSY)))
        assertEquals(Rail.Glyph.BUSY, Rail.glyph(s(SessionState.AWAITING)))
        assertEquals(Rail.Glyph.DONE, Rail.glyph(s(SessionState.IDLE), Summary.Group.FINISHED))
        assertEquals(Rail.Glyph.STILL, Rail.glyph(s(SessionState.IDLE), Summary.Group.STILL))
    }
}
