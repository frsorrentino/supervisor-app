package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MasterLanding.Height
import it.pixelbox.cmwatch.rules.MasterPlace.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La master del tablet al posto e nella misura della PWA larga (app-tablet-come-pwa-03-11). */
class MasterPlaceTest {
    @Test fun panelWidthFromTheKeyOrTheEdge() {
        assertEquals(420f, MasterPlace().panelWidth())
        assertEquals(720f, MasterPlace(narrow = false).panelWidth())
        assertEquals(650f, MasterPlace(width = 650f).panelWidth())
        assertEquals(720f, MasterPlace(narrow = true, width = 650f).toggleSize().panelWidth())
    }

    @Test fun droppedWidthSnapsAndDecidesNarrow() {
        val p = MasterPlace().widthDropped(940f, 1920f)
        assertEquals(960f, p.width)
        assertFalse(p.narrow)
        assertTrue(MasterPlace(narrow = false).widthDropped(440f, 1920f).narrow)
    }

    // Sotto la home solo con le sessioni aperte: senza, la stessa master sta di lato a colonna intera.
    @Test fun underOnlyWithOpenSessions() {
        val p = MasterPlace(open = true, under = true, stack = Height.HALF)
        assertTrue(p.stacked(true))
        assertFalse(p.stacked(false))
        assertEquals(Height.HALF, p.height(true))
        assertEquals(Height.COLUMN, p.height(false))
        assertEquals(Height.BAR, p.copy(open = false).height(true))
    }

    @Test fun heightsFromTheTopEdge() {
        val p = MasterPlace(open = true)
        assertEquals(MasterPlace(open = false), p.withHeight(Height.BAR))
        assertEquals(MasterPlace(open = true, under = true, stack = Height.COMPACT), p.withHeight(Height.COMPACT))
        assertEquals(MasterPlace(open = true, under = false, stack = Height.HALF), MasterPlace(open = true, under = true, stack = Height.HALF).withHeight(Height.COLUMN))
        assertEquals(Height.HALF, MasterPlace(stack = Height.COMPACT).stepStack().stack)
        assertEquals(Height.COMPACT, MasterPlace(stack = Height.HALF).stepStack().stack)
    }

    @Test fun positionMenu() {
        assertEquals(listOf(Item.RIGHT, Item.LEFT), MasterPlace().menu(sessionsOpen = false).map { it.item })
        assertEquals(listOf(Item.RIGHT, Item.LEFT, Item.UNDER, Item.SIZE, Item.CLOSE), MasterPlace(open = true).menu(sessionsOpen = true).map { it.item })
        assertEquals(listOf(false, true), MasterPlace(left = true).menu(false).map { it.on })
        assertTrue(MasterPlace().pick(Item.LEFT).left)
        assertTrue(MasterPlace().pick(Item.UNDER).under)
        assertFalse(MasterPlace(open = true).pick(Item.CLOSE).open)
    }

    // Lato, misura e posto si ricordano; aperta o chiusa no.
    @Test fun prefRoundTrip() {
        val p = MasterPlace(open = true, left = true, narrow = false, width = 650f, under = true, stack = Height.HALF)
        assertEquals(p.copy(open = false), MasterPlace.fromPref(p.pref()))
        assertEquals(MasterPlace(), MasterPlace.fromPref(null))
        assertEquals(MasterPlace(), MasterPlace.fromPref("rotto"))
    }
}
