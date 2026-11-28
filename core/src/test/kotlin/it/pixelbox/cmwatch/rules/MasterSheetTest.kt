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
        assertEquals(0.62f, MasterSheet.HALF_SHARE, 0f)
        assertEquals(170f, a.row, 0f)
        assertEquals(434f, a.half, 0.01f)
        assertEquals(0f, MasterSheet.GAP_DP, 0f)
        assertEquals(700f, a.full, 0f)
        val col = MasterSheet.anchors(736f, 170f)
        assertEquals(456.32f, col.half, 0.01f)
        assertEquals(736f, col.full, 0f)
    }

    @Test fun `in un posto basso la metà non scende sotto la riga e le misure coincidono`() {
        assertEquals(MasterSheet.Anchors(170f, 170f, 250f), MasterSheet.anchors(250f, 170f))
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
        assertEquals(Stop.HALF, MasterSheet.nearest(a, 320f))
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

    // app-posizioni-sistema-27-11, prova 1447: il blocco rigido e la home che si vela e arretra.
    @Test fun `l'avanzamento va da 0 alla riga a 1 all'intera, e resta fra i due`() {
        assertEquals(0f, MasterSheet.progress(a, 170f), 0f)
        assertEquals(1f, MasterSheet.progress(a, a.full), 0f)
        assertEquals(0.5f, MasterSheet.progress(a, (170f + a.full) / 2f), 0.0001f)
        assertEquals(0f, MasterSheet.progress(a, 90f), 0f)
        assertEquals(1f, MasterSheet.progress(a, 900f), 0f)
        assertEquals(0f, MasterSheet.progress(MasterSheet.anchors(150f, 170f), 170f), 0f)
    }

    @Test fun `all'intera la home è velata al 40 per cento e arretra a 0,96, alla riga niente`() {
        assertEquals(0.4f, MasterSheet.DIM_MAX, 0f)
        assertEquals(0.04f, MasterSheet.RECEDE, 0f)
        assertEquals(MasterSheet.Cues(0f, 1f, MasterSheet.SHADE_MIN), MasterSheet.cues(0f))
        val full = MasterSheet.cues(1f)
        assertEquals(0.4f, full.dim, 0.0001f)
        assertEquals(0.96f, full.scale, 0.0001f)
        assertEquals(MasterSheet.SHADE_MAX, full.shade, 0.0001f)
        val half = MasterSheet.cues(0.5f)
        assertEquals(0.2f, half.dim, 0.0001f)
        assertEquals(0.98f, half.scale, 0.0001f)
        assertEquals(MasterSheet.cues(1f), MasterSheet.cues(1.4f))
        assertEquals(MasterSheet.cues(0f), MasterSheet.cues(-1f))
    }

    @Test fun `con il movimento ridotto la home non arretra, resta il velo`() {
        val r = MasterSheet.cues(1f, reduced = true)
        assertEquals(1f, r.scale, 0f)
        assertEquals(0.4f, r.dim, 0.0001f)
        assertEquals(MasterSheet.SHADE_MAX, r.shade, 0.0001f)
    }

    @Test fun `dalla riga la conversazione si ferma a quanta se ne vedrà all'arrivo, e col dito a quanta alla metà`() {
        val b = MasterSheet.anchors(700f, 170f)
        assertEquals(b.half - 170f, MasterSheet.freezeAt(b, 170f, Stop.ROW, Stop.HALF), 0.001f)
        assertEquals(b.full - 170f, MasterSheet.freezeAt(b, 170f, Stop.ROW, Stop.FULL), 0.001f)
        assertEquals(b.half - 170f, MasterSheet.freezeAt(b, 170f, Stop.ROW, null), 0.001f)
        assertEquals(0f, MasterSheet.freezeAt(b, 120f, Stop.HALF, Stop.ROW), 0f)
    }

    @Test fun `fuori dalla riga resta quanta se ne vede adesso, scendendo rigida e salendo si allunga col foglio`() {
        val b = MasterSheet.anchors(700f, 170f)
        assertEquals(b.half - 170f, MasterSheet.freezeAt(b, b.half, Stop.HALF, Stop.FULL), 0.001f)
        assertEquals(b.half - 170f, MasterSheet.freezeAt(b, b.half, Stop.HALF, null), 0.001f)
        assertEquals(b.full - 170f, MasterSheet.freezeAt(b, b.full, Stop.FULL, Stop.ROW), 0.001f)
        assertEquals(130f, MasterSheet.freezeAt(b, 300f, Stop.ROW, Stop.FULL), 0.001f)
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

    @Test fun `all'intera gli angoli in alto sono squadrati, e tornano tondi negli ultimi 22 px`() {
        val c = MasterSheet.CORNER_DP
        assertEquals(0f, MasterSheet.corner(a, a.full, c), 0f)
        assertEquals(11f, MasterSheet.corner(a, a.full - 11f, c), 0.001f)
        assertEquals(c, MasterSheet.corner(a, a.full - c, c), 0.001f)
        assertEquals(c, MasterSheet.corner(a, a.half, c), 0f)
        assertEquals(c, MasterSheet.corner(a, a.row, c), 0f)
        assertEquals(0f, MasterSheet.corner(a, a.full + 30f, c), 0f)
    }

    @Test fun `aperto, la riga del titolo si stringe di 12, alla riga no, e cresce coi primi 12 di corsa`() {
        val m = MasterSheet.LIFT_DP
        assertEquals(12f, m, 0f)
        assertEquals(0f, MasterSheet.lift(a, a.row, m), 0f)
        assertEquals(0f, MasterSheet.lift(a, a.row - 20f, m), 0f)
        assertEquals(5f, MasterSheet.lift(a, a.row + 5f, m), 0.001f)
        assertEquals(12f, MasterSheet.lift(a, a.half, m), 0f)
        assertEquals(12f, MasterSheet.lift(a, a.full, m), 0f)
    }
}
