package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MasterLanding.Height
import it.pixelbox.cmwatch.rules.MasterLanding.Rect
import it.pixelbox.cmwatch.rules.MasterLanding.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La master del tablet si sposta e si misura come nella PWA larga (masterLanding.test.ts, app-tablet-come-pwa-03-11). */
class MasterLandingTest {
    @Test fun headerDroppedInAHalfChoosesTheSide() {
        assertEquals(Side.LEFT, MasterLanding.sideAt(1920f, 100f))
        assertEquals(Side.LEFT, MasterLanding.sideAt(1920f, 959f))
        assertEquals(Side.RIGHT, MasterLanding.sideAt(1920f, 960f))
    }

    // Un margine di 48 intorno al centro: il lato non sfarfalla mentre il dito passa sul confine.
    @Test fun sideKeepsItsSideNearTheCentre() {
        assertEquals(Side.RIGHT, MasterLanding.sideDragged(1920f, 930f, Side.RIGHT))
        assertEquals(Side.LEFT, MasterLanding.sideDragged(1920f, 990f, Side.LEFT))
        assertEquals(Side.LEFT, MasterLanding.sideDragged(1920f, 900f, Side.RIGHT))
    }

    @Test fun widthStopsOnNearSnapsAndStaysFreeBetween() {
        assertEquals(MasterLanding.COLUMN, MasterLanding.snapWidth(MasterLanding.COLUMN + 20f, 1920f))
        assertEquals(960f, MasterLanding.snapWidth(940f, 1920f))
        assertEquals(650f, MasterLanding.snapWidth(650f, 1920f))
    }

    @Test fun widthNeverBelowMinimumNorAbove60Percent() {
        assertEquals(MasterLanding.MIN_W, MasterLanding.clampWidth(100f, 1920f))
        assertEquals(1152f, MasterLanding.clampWidth(1800f, 1920f))
    }

    // Al rilascio la misura a metà fra colonna e metà schermo decide se è «stretta» (il tasto Allarga) o «larga».
    @Test fun releasedWidthIsNarrowBelowTheMidpoint() {
        assertTrue(MasterLanding.narrow(420f, 1920f))
        assertTrue(MasterLanding.narrow(689f, 1920f))
        assertEquals(false, MasterLanding.narrow(691f, 1920f))
    }

    @Test fun topEdgeBandsColumnHalfCompactBar() {
        val zs = MasterLanding.heightZones(Rect(0f, 0f, 420f, 1000f), under = true)
        assertEquals(listOf(Height.COLUMN, Height.HALF, Height.COMPACT, Height.BAR, Height.BAR), listOf(50f, 400f, 700f, 950f, 1200f).map { MasterLanding.heightAt(zs, it) })
    }

    @Test fun withoutOpenSessionsOnlyColumnOrBar() {
        val zs = MasterLanding.heightZones(Rect(0f, 100f, 420f, 900f), under = false)
        assertEquals(listOf(Height.COLUMN, Height.BAR), zs.map { it.key })
        assertEquals(Height.COLUMN, MasterLanding.heightAt(zs, 500f))
        assertEquals(Height.BAR, MasterLanding.heightAt(zs, 990f))
    }

    // La banda di adesso tiene finché il dito non la supera di 28.
    @Test fun heightKeepsTheCurrentBandWithin28() {
        val zs = MasterLanding.heightZones(Rect(0f, 0f, 420f, 1000f), under = true)
        assertEquals(Height.COLUMN, MasterLanding.heightDragged(zs, 170f, Height.COLUMN))
        assertEquals(Height.HALF, MasterLanding.heightDragged(zs, 190f, Height.COLUMN))
        assertEquals(Height.HALF, MasterLanding.heightDragged(zs, 140f, Height.HALF))
    }

    // I segni di arrivo: due ai lati, due sulla larghezza, quattro (o due senza sessioni) sull'altezza.
    @Test fun marksOfEachGesture() {
        val side = MasterLanding.sideMarks(1920f, 1000f, Side.LEFT)
        assertEquals(listOf(14f, 1906f), side.map { it.x })
        assertEquals(listOf(true, false), side.map { it.on })
        val width = MasterLanding.widthMarks(1920f, 1000f, left = false, snapped = 960f)
        assertEquals(listOf(1500f, 960f), width.map { it.x })
        assertEquals(listOf(false, true), width.map { it.on })
        val h = MasterLanding.heightMarks(Rect(1500f, 0f, 420f, 1000f), under = true, on = Height.COMPACT)
        assertEquals(listOf(24f, 500f, 850f, 964f), h.map { it.y })
        assertEquals(listOf(Height.COMPACT), h.filter { it.on }.map { it.key })
        assertEquals(listOf(Height.COLUMN, Height.BAR), MasterLanding.heightMarks(Rect(0f, 0f, 420f, 1000f), under = false, on = Height.BAR).map { it.key })
    }

    // La calamita: un segno vicino al dito si allunga (48 a riposo, fino a 136) e gli va incontro lungo il suo bordo.
    @Test fun magnetPullsNearMarks() {
        val m = MasterLanding.Mark(Height.COLUMN, 100f, 500f, vertical = true, on = false)
        assertEquals(0f, MasterLanding.pull(m, 100f, 900f), 0.001f)
        assertEquals(1f, MasterLanding.pull(m, 100f, 500f), 0.001f)
        assertEquals(48f, MasterLanding.length(m, 100f, 900f), 0.001f)
        assertEquals(136f, MasterLanding.length(m, 100f, 500f), 0.001f)
        // Quello di arrivo non scende sotto 0,6.
        assertEquals(48f + 0.6f * 88f, MasterLanding.length(m.copy(on = true), 100f, 900f), 0.001f)
    }
}
