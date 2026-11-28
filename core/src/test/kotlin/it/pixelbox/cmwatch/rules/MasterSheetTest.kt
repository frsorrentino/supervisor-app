package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MasterSheet.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Il foglio della master (app-posizioni-sistema-27-11 gruppo 4): le tre misure, la proiezione di iOS, lo scatto, l'elastico, il tocco e
 * i tasti. Gli stessi casi di web/src/lib/masterSheet.test.ts.
 */
class MasterSheetTest {
    // Un telefono: sotto la barra in alto restano 700 dp; la riga misura 170.
    private val a = MasterSheet.anchors(700f, 170f)

    @Test fun `le tre misure, la riga misurata, la metà al 58 per cento del posto come nel mockup, tutto meno lo stacco`() {
        assertEquals(0.58f, MasterSheet.HALF_SHARE, 0f)
        assertEquals(170f, a.row, 0f)
        assertEquals(406f, a.half, 0.01f)
        assertEquals(700f - MasterSheet.GAP_DP, a.full, 0f)
        val col = MasterSheet.anchors(736f, 170f)
        assertEquals(426.88f, col.half, 0.01f)
        assertEquals(728f, col.full, 0f)
    }

    @Test fun `in un posto basso la metà non scende sotto la riga e le misure coincidono`() {
        assertEquals(MasterSheet.Anchors(170f, 170f, 242f), MasterSheet.anchors(250f, 170f))
        assertEquals(MasterSheet.Anchors(170f, 170f, 170f), MasterSheet.anchors(150f, 170f))
        assertEquals(MasterSheet.Anchors(0f, 0f, 0f), MasterSheet.anchors(0f, -4f))
    }

    @Test fun `la proiezione di iOS, la velocità per 0,499 s`() {
        assertEquals(0.998f, MasterSheet.RATE, 0f)
        assertEquals(300f, MasterSheet.project(300f, 0f), 0.001f)
        assertEquals(799f, MasterSheet.project(300f, 1000f), 0.01f)
        assertEquals(-199f, MasterSheet.project(300f, -1000f), 0.01f)
    }

    @Test fun `senza slancio scatta alla misura più vicina, a pari distanza la più bassa`() {
        assertEquals(Stop.ROW, MasterSheet.nearest(a, 200f))
        assertEquals(Stop.HALF, MasterSheet.nearest(a, 300f))
        assertEquals(Stop.FULL, MasterSheet.nearest(a, 600f))
        assertEquals(Stop.ROW, MasterSheet.nearest(MasterSheet.Anchors(170f, 350f, 692f), 260f))
        assertEquals(Stop.HALF, MasterSheet.snap(a, 340f, 0f))
    }

    @Test fun `un colpo veloce salta la misura vicina, uno lento no`() {
        assertEquals(Stop.FULL, MasterSheet.snap(a, 180f, 1000f))
        assertEquals(Stop.HALF, MasterSheet.snap(a, 300f, 100f))
        assertEquals(Stop.ROW, MasterSheet.snap(a, 680f, -1200f))
        assertEquals(Stop.HALF, MasterSheet.snap(a, 620f, -400f))
        assertEquals(Stop.ROW, MasterSheet.snap(a, 120f, -3000f))
        assertEquals(Stop.FULL, MasterSheet.snap(a, 720f, 3000f))
    }

    @Test fun `l'elastico, libero fra la riga e l'intera, oltre sempre meno`() {
        assertEquals(400f, MasterSheet.rubber(400f, 170f, 692f, 700f), 0f)
        val below = MasterSheet.rubber(70f, 170f, 692f, 700f)
        assertTrue(below < 170f && below > 70f)
        assertEquals(MasterSheet.band(100f, 700f), 170f - below, 0.001f)
        val over = MasterSheet.rubber(800f, 170f, 692f, 700f)
        assertTrue(over > 692f && over < 800f)
        assertTrue(MasterSheet.band(200f, 700f) < 2f * MasterSheet.band(100f, 700f))
        assertTrue(MasterSheet.band(1e9f, 700f) <= 700f)
        assertEquals(0f, MasterSheet.band(0f, 700f), 0f)
        assertEquals(0f, MasterSheet.band(50f, 0f), 0f)
    }

    @Test fun `il tocco sulla riga passa alla misura dopo, freccia su sale fino all'intera`() {
        assertEquals(Stop.HALF, MasterSheet.tap(Stop.ROW))
        assertEquals(Stop.FULL, MasterSheet.tap(Stop.HALF))
        assertEquals(Stop.ROW, MasterSheet.tap(Stop.FULL))
        assertEquals(listOf(Stop.HALF, Stop.FULL, Stop.FULL), listOf(MasterSheet.up(Stop.ROW), MasterSheet.up(Stop.HALF), MasterSheet.up(Stop.FULL)))
    }

    @Test fun `Esc riporta alla riga, freccia giù solo a campo vuoto o fuori dal campo`() {
        assertTrue(MasterSheet.toRow("Escape", inField = true, fieldEmpty = false))
        assertTrue(MasterSheet.toRow("ArrowDown", inField = false, fieldEmpty = false))
        assertTrue(MasterSheet.toRow("ArrowDown", inField = true, fieldEmpty = true))
        assertFalse(MasterSheet.toRow("ArrowDown", inField = true, fieldEmpty = false))
        assertFalse(MasterSheet.toRow("ArrowUp", inField = false, fieldEmpty = true))
        assertFalse(MasterSheet.toRow("Enter", inField = false, fieldEmpty = true))
    }

    @Test fun `la preferenza, una misura nota, altrimenti la riga`() {
        assertEquals(Stop.HALF, MasterSheet.read("half"))
        assertEquals(Stop.FULL, MasterSheet.read("full"))
        assertEquals(Stop.ROW, MasterSheet.read("row"))
        assertEquals(Stop.ROW, MasterSheet.read(null))
        assertEquals(Stop.ROW, MasterSheet.read("compact"))
        assertEquals("master_sheet", MasterSheet.PREF)
    }

    @Test fun `la conversazione si apre fra la riga e la metà`() {
        assertEquals(0f, MasterSheet.openness(a, 170f), 0f)
        assertEquals(0.5f, MasterSheet.openness(a, (170f + a.half) / 2f), 0.0001f)
        assertEquals(1f, MasterSheet.openness(a, a.half), 0f)
        assertEquals(1f, MasterSheet.openness(a, 600f), 0f)
        assertEquals(0f, MasterSheet.openness(a, 100f), 0f)
        assertEquals(0f, MasterSheet.openness(MasterSheet.anchors(150f, 170f), 170f), 0f)
        assertEquals(1f, MasterSheet.openness(MasterSheet.anchors(150f, 170f), 171f), 0f)
    }

    @Test fun `la molla arriva senza superare la misura, anche partendo dalla velocità del dito`() {
        val from = -180f
        val land = MasterGestures.land(from, 4000f)
        val w = sqrt(land.stiffness)
        var past = false
        var t = 0f
        while (t <= 1.2f) { if (MasterSheet.settleAt(from, land.velocity, w, t).x > 0.5f) past = true; t += 1f / 240f }
        assertFalse(past)
        assertTrue(abs(MasterSheet.settleAt(from, land.velocity, w, 1.2f).x) < 0.5f)
        val still = MasterGestures.land(-300f, 0f)
        val ws = sqrt(still.stiffness)
        assertEquals(0f, MasterSheet.settleAt(-300f, still.velocity, ws, 0f).v, 0.001f)
        assertTrue(abs(MasterSheet.settleAt(-300f, still.velocity, ws, 0.5f).x) < 3f)
    }
}
