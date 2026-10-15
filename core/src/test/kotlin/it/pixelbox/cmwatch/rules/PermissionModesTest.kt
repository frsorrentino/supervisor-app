package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.PermissionModes.Glyph
import org.junit.Assert.*
import org.junit.Test

/** Mockup dei permessi (ok di Franz, 15/10 07:39) e contratto 1.69. */
class PermissionModesTest {
    private fun s(mode: String?, bypass: Boolean = false, st: SessionState = SessionState.IDLE) =
        Session(id = "1", name = "kb", account = "personale", project = "p", state = st, since = 0, permissionMode = mode, permissionBypass = bypass)

    @Test fun theTerminalSigns() {
        assertEquals(Glyph.NONE, PermissionModes.glyph(null))
        assertEquals(Glyph.NONE, PermissionModes.glyph("default"))
        assertEquals(Glyph.PLAN, PermissionModes.glyph("plan"))
        assertEquals(Glyph.FORWARD, PermissionModes.glyph("acceptEdits"))
        assertEquals(Glyph.FORWARD, PermissionModes.glyph("auto"))
        assertEquals(Glyph.BYPASS, PermissionModes.glyph("bypassPermissions"))
        assertEquals(Glyph.UNKNOWN, PermissionModes.glyph("dontAsk"))
    }

    @Test fun theHomeMarksOnlyWhatIsUnusual() {
        assertEquals(Glyph.NONE, PermissionModes.homeGlyph(s("bypassPermissions", bypass = true)))
        assertEquals(Glyph.PLAN, PermissionModes.homeGlyph(s("plan", bypass = true)))
        assertEquals(Glyph.BYPASS, PermissionModes.homeGlyph(s("bypassPermissions")))
    }

    @Test fun bypassIsAChoiceOnlyForASessionStartedThatWay() {
        assertEquals(listOf("plan", "default", "acceptEdits", "auto"), PermissionModes.choices(s("default")))
        assertEquals(listOf("plan", "default", "acceptEdits", "auto", "bypassPermissions"), PermissionModes.choices(s("plan", bypass = true)))
    }

    @Test fun moreAutonomyAsksForConfirmationLessDoesNot() {
        assertTrue(PermissionModes.needsConfirm("plan", "acceptEdits"))
        assertTrue(PermissionModes.needsConfirm(null, "auto"))
        assertFalse(PermissionModes.needsConfirm("bypassPermissions", "default"))
        assertFalse(PermissionModes.needsConfirm("acceptEdits", "plan"))
    }

    @Test fun onlyWithARelayThatKnowsTheOp() {
        val st = State(v = 1, ts = 0, host = "pc", ops = listOf("prompt", "permission_mode"))
        assertTrue(PermissionModes.canChange(st, s("default")))
        assertFalse(PermissionModes.canChange(st.copy(ops = listOf("prompt")), s("default")))
        assertFalse(PermissionModes.canChange(st, s("default", st = SessionState.GONE)))
    }
}
