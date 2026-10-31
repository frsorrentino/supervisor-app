package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeOrderTest {
    @Test fun withoutASavedOrderTheDefault() {
        assertEquals(HomeOrder.DEFAULT, HomeOrder.normalize(null))
        assertEquals(HomeOrder.DEFAULT, HomeOrder.normalize(emptyList()))
    }

    @Test fun unknownAndDoubleKeysAreDroppedMissingOnesAppended() {
        assertEquals(listOf("esiti", "sessions", "suggestions", "lavori", "usage", "system", "plugins", "closed"), HomeOrder.normalize(listOf("esiti", "nuova", "esiti", "sessions")))
    }

    @Test fun moveShiftsTheOthers() {
        assertEquals(listOf("suggestions", "lavori", "sessions", "esiti", "usage", "system", "plugins", "closed"), HomeOrder.move(HomeOrder.DEFAULT, 0, 2))
        assertEquals(HomeOrder.DEFAULT, HomeOrder.move(HomeOrder.DEFAULT, 0, 9))
    }

    @Test fun theArgIsAJsonArrayOrNullForTheDefault() {
        assertNull(HomeOrder.arg(HomeOrder.DEFAULT))
        assertEquals("""["esiti","sessions","suggestions","lavori","usage","system","plugins","closed"]""", HomeOrder.arg(listOf("esiti", "sessions", "suggestions", "lavori", "usage", "system", "plugins", "closed")))
    }

    @Test fun aSavedAltroExpandsInItsPlace() {
        assertEquals(listOf("usage", "system", "plugins", "closed", "sessions", "suggestions", "lavori", "esiti"), HomeOrder.normalize(listOf("altro", "sessions", "suggestions", "lavori", "esiti")))
    }

    @Test fun rowsAlreadyApartStayPutAndMissingOnesAreAppended() {
        assertEquals(listOf("system", "sessions", "usage", "suggestions", "lavori", "esiti", "plugins", "closed"), HomeOrder.normalize(listOf("system", "sessions", "usage")))
    }

    @Test fun contiguousRowsShareACardASeparateOneHasItsOwn() {
        assertEquals(listOf(listOf("usage", "system"), listOf("sessions"), listOf("plugins"), listOf("esiti"), listOf("closed")), HomeOrder.groups(listOf("usage", "system", "sessions", "plugins", "esiti", "closed")))
    }

    private val abc = listOf("a", "b", "c")

    @Test fun dragPassesTheNextOnlyWhenTheFingerCrossesIt() {
        assertEquals(abc, HomeOrder.dragTarget(abc, "a", 89f, mapOf("a" to 10f, "b" to 90f, "c" to 200f)))
        assertEquals(listOf("b", "a", "c"), HomeOrder.dragTarget(abc, "a", 91f, mapOf("a" to 10f, "b" to 90f, "c" to 200f)))
    }

    @Test fun dragPassesThePreviousOnlyWhenTheFingerCrossesIt() {
        assertEquals(abc, HomeOrder.dragTarget(abc, "c", 91f, mapOf("a" to 10f, "b" to 90f, "c" to 200f)))
        assertEquals(listOf("a", "c", "b"), HomeOrder.dragTarget(abc, "c", 89f, mapOf("a" to 10f, "b" to 90f, "c" to 200f)))
    }

    @Test fun anOffscreenNeighbourIsNotPassed() {
        assertEquals(abc, HomeOrder.dragTarget(abc, "a", 500f, mapOf("a" to 10f), setOf("a", "b", "c")))
    }

    @Test fun anEmptySectionIsNoObstacle() {
        assertEquals(listOf("b", "c", "a"), HomeOrder.dragTarget(abc, "a", 91f, mapOf("a" to 10f, "c" to 90f), setOf("a", "c")))
    }

    @Test fun afterASwapTheHeldStaysPut() {
        // «b» è salita sopra «a»: il dito resta sotto la sua metà, «c» è ancora sotto il dito.
        val swapped = listOf("b", "a", "c")
        assertEquals(swapped, HomeOrder.dragTarget(swapped, "a", 91f, mapOf("b" to 40f, "a" to 140f, "c" to 260f)))
    }
}
