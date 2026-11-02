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
        assertEquals(listOf("prima_raccolta", "seguiti", "prove", "decisioni", "esiti", "sessions", "suggestions", "lavori", "usage", "system", "plugins", "closed"), HomeOrder.normalize(listOf("esiti", "nuova", "esiti", "sessions")))
    }

    @Test fun moveShiftsTheOthers() {
        assertEquals(listOf("seguiti", "prove", "prima_raccolta", "decisioni", "sessions", "suggestions", "lavori", "esiti", "usage", "system", "plugins", "closed"), HomeOrder.move(HomeOrder.DEFAULT, 0, 2))
        assertEquals(HomeOrder.DEFAULT, HomeOrder.move(HomeOrder.DEFAULT, 0, 99))
    }

    @Test fun theArgIsAJsonArrayOrNullForTheDefault() {
        assertNull(HomeOrder.arg(HomeOrder.DEFAULT))
        assertEquals("""["esiti","sessions","suggestions","lavori","usage","system","plugins","closed"]""", HomeOrder.arg(listOf("esiti", "sessions", "suggestions", "lavori", "usage", "system", "plugins", "closed")))
    }

    @Test fun aSavedAltroExpandsInItsPlace() {
        assertEquals(listOf("prima_raccolta", "seguiti", "prove", "decisioni", "usage", "system", "plugins", "closed", "sessions", "suggestions", "lavori", "esiti"), HomeOrder.normalize(listOf("altro", "sessions", "suggestions", "lavori", "esiti")))
    }

    @Test fun rowsAlreadyApartStayPutAndMissingOnesAreAppended() {
        assertEquals(listOf("prima_raccolta", "seguiti", "prove", "decisioni", "system", "sessions", "usage", "suggestions", "lavori", "esiti", "plugins", "closed"), HomeOrder.normalize(listOf("system", "sessions", "usage")))
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

    @Test fun thePinStaysInTheListAndOutOfTheOrder() {
        val saved = HomeOrder.withPin(HomeOrder.DEFAULT, "suggestions")
        assertEquals("pin:suggestions", saved.last())
        assertEquals(HomeOrder.DEFAULT, HomeOrder.normalize(saved))
        assertEquals("suggestions", HomeOrder.pinned(saved))
        assertEquals("""["sessions","suggestions","lavori","esiti","usage","system","plugins","closed","pin:suggestions"]""", HomeOrder.arg(saved))
    }

    @Test fun onlyTheAllowedVociArePinned() {
        assertNull(HomeOrder.pinned(listOf("pin:sessions")))
        assertNull(HomeOrder.pinned(listOf("pin:esiti")))
        assertEquals(HomeOrder.DEFAULT, HomeOrder.withPin(HomeOrder.DEFAULT, "sessions"))
    }

    @Test fun aSecondPinTakesThePlaceOfTheFirst() {
        val first = HomeOrder.withPin(HomeOrder.DEFAULT, "system")
        assertEquals("usage", HomeOrder.pinned(HomeOrder.withPin(first, "usage")))
    }

    @Test fun theAlarmTakesTheSlotAndGivesItBack() {
        assertEquals("usage", HomeOrder.slot("system", alarm = true))
        assertEquals("system", HomeOrder.slot("system", alarm = false))
        assertNull(HomeOrder.slot(null, alarm = false))
    }

    @Test fun slot_rilascio_onlyInsideTheBoard() {
        assertEquals(true, HomeOrder.overSlot("usage", 150f, 100f, 200f, false))
        assertEquals(false, HomeOrder.overSlot("usage", 201f, 100f, 200f, false))
        assertEquals(false, HomeOrder.overSlot("usage", 99f, 100f, 200f, false))
        assertEquals(false, HomeOrder.overSlot("sessions", 150f, 100f, 200f, false))
        assertEquals(false, HomeOrder.overSlot("usage", 150f, 100f, 200f, true))
        assertEquals(false, HomeOrder.overSlot("usage", 150f, null, null, false))
    }

    @Test fun slot_rilascio_leavingTheSlotLandsUnderTheFinger() {
        val o = listOf("a", "b", "usage", "c")
        val mids = mapOf("a" to 100f, "b" to 200f, "c" to 300f)
        assertEquals(listOf("usage", "a", "b", "c"), HomeOrder.dropAt(o, "usage", 50f, mids))
        assertEquals(listOf("a", "usage", "b", "c"), HomeOrder.dropAt(o, "usage", 150f, mids))
        assertEquals(listOf("a", "b", "c", "usage"), HomeOrder.dropAt(o, "usage", 350f, mids))
    }

    @Test fun slot_rilascio_endsWhereTheFingerIs() {
        val o = listOf("a", "usage", "b")
        val moved = listOf("usage", "a", "b")
        val mids = mapOf("a" to 100f, "b" to 300f)
        assertEquals(o to "usage", HomeOrder.release(o, moved, "usage", null, false, true, 0f, mids))
        assertEquals(o to "usage", HomeOrder.release(o, moved, "usage", "usage", false, true, 0f, mids))
        assertEquals(listOf("a", "b", "usage") to null, HomeOrder.release(o, o, "usage", "usage", false, false, 400f, mids))
        assertEquals(moved to null, HomeOrder.release(o, moved, "usage", null, false, false, 50f, mids))
        assertEquals(moved to "usage", HomeOrder.release(o, moved, "a", "usage", false, false, 50f, mids))
    }

    @org.junit.Test fun slot_subito_lOrdineSceltoValeSenzaLaSpintaDelRelay() {
        val old = listOf("sessions"); val mine = listOf("sessions", "pin:system")
        org.junit.Assert.assertEquals(mine, HomeOrder.saved(old, HomeOrder.Sent(old, mine)))
    }

    @org.junit.Test fun slot_subito_unoStatoVecchioNonLoToglie_ilRelayNuovoSi() {
        val old = listOf("sessions"); val mine = listOf("sessions", "pin:system"); val other = listOf("esiti")
        val sent = HomeOrder.Sent(old, mine)
        org.junit.Assert.assertEquals(mine, HomeOrder.saved(old, sent))
        org.junit.Assert.assertEquals(mine, HomeOrder.saved(mine, sent))
        org.junit.Assert.assertEquals(other, HomeOrder.saved(other, sent))
        org.junit.Assert.assertEquals(old, HomeOrder.saved(old, null))
        org.junit.Assert.assertEquals(mine, HomeOrder.saved(null, HomeOrder.Sent(null, mine)))
    }

    /** Le voci arrivate col programma del 01/11 sono voci come le altre: le prossime nascono qui. */
    private val nuove = listOf("prima_raccolta", "seguiti", "prove", "decisioni")

    @org.junit.Test fun voci_nuove_trascinabili_nellOrdineEColGesto() {
        org.junit.Assert.assertEquals(nuove, HomeOrder.normalize(null).take(4))
        val o = HomeOrder.DEFAULT
        org.junit.Assert.assertEquals(o.indexOf("decisioni"), HomeOrder.dragTarget(o, "prove", 100f, mapOf("prove" to 50f, "decisioni" to 90f)).indexOf("prove"))
        org.junit.Assert.assertTrue(HomeOrder.arg(HomeOrder.move(o, 2, 7))!!.contains("\"prove\""))
    }

    @org.junit.Test fun voci_nuove_trascinabili_unOrdineSalvato() {
        org.junit.Assert.assertEquals(nuove + listOf("esiti", "sessions"), HomeOrder.normalize(listOf("esiti", "sessions")).take(6))
        org.junit.Assert.assertEquals(listOf("prima_raccolta", "seguiti", "decisioni", "sessions", "prove"), HomeOrder.normalize(listOf("sessions", "prove")).take(5))
    }

    @org.junit.Test fun voci_nuove_trascinabili_relayESlot() {
        org.junit.Assert.assertTrue(HomeOrder.withPin(HomeOrder.DEFAULT, "usage").size <= 16)
        nuove.forEach { org.junit.Assert.assertFalse(it, HomeOrder.pinnable(it)) }
    }
}
