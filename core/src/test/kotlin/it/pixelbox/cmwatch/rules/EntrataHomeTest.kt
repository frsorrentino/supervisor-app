package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** L'entrata della home all'avvio (app-entrata-home-27-11, variante B). Gli stessi casi di web/src/lib/entrataHome.test.ts. */
class EntrataHomeTest {
    @Test fun `il primo blocco parte dopo 70 ms e ognuno 45 ms dopo quello sopra`() {
        assertEquals(listOf(70, 115, 160, 205, 250), (0..4).map(EntrataHome::delayMs))
        assertEquals(70 + 45 * 12, EntrataHome.delayMs(12))
        // Una posizione che non c'è vale la prima.
        assertEquals(70, EntrataHome.delayMs(-3))
    }

    @Test fun `la barra della settimana parte 220 ms dopo il suo riquadro`() {
        assertEquals(290, EntrataHome.barDelayMs(0))
        // Il tabellone sotto una domanda aperta: secondo blocco.
        assertEquals(335, EntrataHome.barDelayMs(1))
        assertEquals(800, EntrataHome.BAR_MS)
    }

    @Test fun `i valori della variante B`() {
        assertEquals(400, EntrataHome.FADE_MS)
        assertEquals(5f, EntrataHome.BLUR, 0f)
        assertEquals(24f, EntrataHome.RISE, 0f)
        assertEquals(0.94f, EntrataHome.SCALE_FROM, 0f)
        assertEquals(220f, EntrataHome.SPRING.k, 0f)
        assertEquals(0.506f, EntrataHome.SPRING.ratio(), 0.001f)
        // Con «riduci movimento» niente molla: la home compare ferma.
        assertEquals(1f, EntrataHome.SPRING.ratio(reduced = true), 0f)
    }

    @Test fun `entra una volta sola, anche se quella volta era ferma`() {
        val once = EntrataHome.Once()
        assertFalse(once.taken)
        assertTrue(EntrataHome.plays(once.take(), reduced = false))
        assertTrue(once.taken)
        // Tornando alla home da una sessione non si ripete.
        assertFalse(EntrataHome.plays(once.take(), reduced = false))
        // Con «riduci movimento» la prima volta passa ferma, e dopo resta ferma.
        val still = EntrataHome.Once()
        assertFalse(EntrataHome.plays(still.take(), reduced = true))
        assertFalse(EntrataHome.plays(still.take(), reduced = false))
    }

    @Test fun `le posizioni vanno ai blocchi del primo disegno, nell'ordine`() {
        val order = EntrataHome.Order()
        assertEquals(listOf(0, 1, 2), List(3) { order.next() })
        order.close()
        // Chi arriva dopo, scorrendo o con un dato nuovo, resta fermo.
        assertNull(order.next())
        assertNull(order.next())
    }
}
