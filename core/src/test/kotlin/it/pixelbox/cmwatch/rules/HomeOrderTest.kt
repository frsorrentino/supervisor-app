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
        assertEquals(listOf("esiti", "sessions", "suggestions", "lavori", "altro"), HomeOrder.normalize(listOf("esiti", "nuova", "esiti", "sessions")))
    }

    @Test fun moveShiftsTheOthers() {
        assertEquals(listOf("suggestions", "lavori", "sessions", "esiti", "altro"), HomeOrder.move(HomeOrder.DEFAULT, 0, 2))
        assertEquals(HomeOrder.DEFAULT, HomeOrder.move(HomeOrder.DEFAULT, 0, 9))
    }

    @Test fun theArgIsAJsonArrayOrNullForTheDefault() {
        assertNull(HomeOrder.arg(HomeOrder.DEFAULT))
        assertEquals("""["esiti","sessions","suggestions","lavori","altro"]""", HomeOrder.arg(listOf("esiti", "sessions", "suggestions", "lavori", "altro")))
    }
}
