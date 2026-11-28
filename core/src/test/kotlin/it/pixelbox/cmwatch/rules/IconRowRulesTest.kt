package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.IconRowRules.Span
import it.pixelbox.cmwatch.rules.IconRowRules.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La riga di icone con la pillola elastica (app-posizioni-sistema-27-11, gruppo 1, e app-dock-elastico-27-11). Gli stessi casi di
 * web/src/lib/iconRow.test.ts.
 */
class IconRowRulesTest {
    // Tre schede: chiusa, aperta col nome, chiusa; 2 di spazio fra l'una e l'altra.
    private val spans = listOf(Span(0f, 44f), Span(46f, 110f), Span(112f, 156f))

    @Test fun `il numero sparisce a zero e tiene il colore del suo stato`() {
        assertEquals(IconRowRules.Badge("7", Tone.WAIT), IconRowRules.badge(7, Tone.WAIT))
        assertEquals(IconRowRules.Badge("1", Tone.NEW), IconRowRules.badge(1, Tone.NEW))
        assertEquals(IconRowRules.Badge("4", Tone.COUNT), IconRowRules.badge(4, Tone.COUNT))
        assertNull(IconRowRules.badge(0, Tone.WAIT))
        assertNull(IconRowRules.badge(-2, Tone.NEW))
        assertEquals("Per te 7", IconRowRules.spoken("Per te", 7))
        assertEquals("Chat", IconRowRules.spoken("Chat", 0))
    }

    @Test fun `le molle hanno lo smorzamento relativo dei valori della master`() {
        assertEquals(0.751f, IconRowRules.NAME.ratio(), 0.001f)
        assertEquals(0.751f, IconRowRules.LEAD.ratio(), 0.001f)
        assertEquals(0.653f, IconRowRules.TRAIL.ratio(), 0.001f)
        assertEquals(0.844f, IconRowRules.DRAG_LEAD.ratio(), 0.001f)
        assertEquals(0.727f, IconRowRules.DRAG_TRAIL.ratio(), 0.001f)
        assertEquals(0.898f, IconRowRules.SETTLE.ratio(), 0.001f)
        assertEquals(0.351f, IconRowRules.POP.ratio(), 0.001f)
        assertEquals(0.520f, IconRowRules.ROW.ratio(), 0.001f)
    }

    @Test fun `con riduci movimento lo smorzamento diventa critico`() {
        assertEquals(1f, IconRowRules.TRAIL.ratio(reduced = true), 0f)
        assertEquals(2f * kotlin.math.sqrt(150f), IconRowRules.TRAIL.damping(reduced = true), 1e-4f)
        assertEquals(16f, IconRowRules.TRAIL.damping(), 0f)
    }

    @Test fun `guida il bordo verso cui va la pillola, quello dietro resta indietro`() {
        assertEquals(IconRowRules.TRAIL to IconRowRules.LEAD, IconRowRules.edges(1, drag = false))
        assertEquals(IconRowRules.LEAD to IconRowRules.TRAIL, IconRowRules.edges(-1, drag = false))
        assertEquals(IconRowRules.DRAG_TRAIL to IconRowRules.DRAG_LEAD, IconRowRules.edges(1, drag = true))
        assertEquals(IconRowRules.DRAG_LEAD to IconRowRules.DRAG_TRAIL, IconRowRules.edges(-1, drag = true))
        assertEquals(IconRowRules.SETTLE to IconRowRules.SETTLE, IconRowRules.edges(0, drag = false))
        assertEquals(IconRowRules.SETTLE to IconRowRules.SETTLE, IconRowRules.edges(0, drag = true))
        assertEquals(1, IconRowRules.direction(0, 3))
        assertEquals(-1, IconRowRules.direction(3, 1))
        assertEquals(0, IconRowRules.direction(2, 2))
        assertEquals(0, IconRowRules.direction(-1, 2))
    }

    @Test fun `la meta della pillola è la scheda scelta, spostata dall'elastico`() {
        assertEquals(Span(46f, 110f), IconRowRules.target(spans[1]))
        assertEquals(Span(36f, 100f), IconRowRules.target(spans[1], -10f))
        assertNull(IconRowRules.target(null))
    }

    @Test fun `ferma la pillola è la scheda, alta 40`() {
        assertEquals(IconRowRules.Pill(46f, 64f, 40f, 0f), IconRowRules.pill(46f, 110f, spans[1]))
        // I bordi incrociati si leggono nell'ordine giusto.
        assertEquals(IconRowRules.Pill(46f, 64f, 40f, 0f), IconRowRules.pill(110f, 46f, spans[1]))
        // Mai più stretta di 24.
        assertEquals(IconRowRules.Pill(50f, 24f, 40f, 0f), IconRowRules.pill(50f, 52f, Span(50f, 52f)))
        // Le misure del telefono in px: alta 110 a densità 2,75.
        assertEquals(110f, IconRowRules.pill(0f, 100f, Span(0f, 100f), height = 110f, minWidth = 66f).height, 1e-4f)
    }

    @Test fun `tirata si assottiglia ad area costante fra 0,66 e 1,1`() {
        val mild = IconRowRules.pill(0f, 100f, Span(0f, 64f))
        assertEquals(32f, mild.height, 1e-4f)
        assertEquals(4f, mild.top, 1e-4f)
        assertEquals(100f, mild.width, 0f)
        val far = IconRowRules.pill(0f, 256f, Span(192f, 256f))
        assertEquals(26.4f, far.height, 1e-4f)
        assertEquals(6.8f, far.top, 1e-4f)
        val squeezed = IconRowRules.pill(0f, 50f, Span(0f, 64f))
        assertEquals(44f, squeezed.height, 1e-4f)
        assertEquals(-2f, squeezed.top, 1e-4f)
    }

    @Test fun `oltre le estremità la pillola tira come un elastico e non passa mai 46`() {
        assertEquals(50f, IconRowRules.rubber(50f, 0f, 100f), 0f)
        assertEquals(116.32258f, IconRowRules.rubber(146f, 0f, 100f), 1e-3f)
        assertEquals(-16.32258f, IconRowRules.rubber(-46f, 0f, 100f), 1e-3f)
        assertTrue(IconRowRules.rubber(100_000f, 0f, 100f) < 146f)
        // I centri della prima e dell'ultima scheda sono 22 e 134.
        assertEquals(0f, IconRowRules.stretch(60f, spans), 0f)
        assertEquals(0f, IconRowRules.stretch(134f, spans), 0f)
        assertEquals(16.32258f, IconRowRules.stretch(180f, spans), 1e-3f)
        assertEquals(-16.32258f, IconRowRules.stretch(-24f, spans), 1e-3f)
        assertEquals(0f, IconRowRules.stretch(500f, emptyList()), 0f)
        // La riga segue al 35 %.
        assertEquals(5.7129f, IconRowRules.stretch(180f, spans) * IconRowRules.FOLLOW, 1e-3f)
    }

    @Test fun `col dito la pillola passa alla scheda sotto, con 6 di tolleranza`() {
        assertEquals(0, IconRowRules.under(spans, 50f, 0))
        assertEquals(1, IconRowRules.under(spans, 51f, 0))
        assertEquals(0, IconRowRules.under(spans, 45f, 0))
        assertEquals(1, IconRowRules.under(spans, 111f, 1))
        assertEquals(2, IconRowRules.under(spans, 117f, 1))
        // Fuori dalla riga conta la scheda all'estremità.
        assertEquals(2, IconRowRules.under(spans, 500f, 0))
        assertEquals(0, IconRowRules.under(spans, -30f, 2))
        // Fra due schede resta quella di adesso.
        assertEquals(0, IconRowRules.under(spans, 111f, 0))
        assertEquals(-1, IconRowRules.under(emptyList(), 10f, 0))
    }

    @Test fun `al rilascio guida il bordo che torna verso la riga`() {
        assertEquals(-1, IconRowRules.settle(16f))
        assertEquals(1, IconRowRules.settle(-5f))
        assertEquals(0, IconRowRules.settle(0f))
    }

    @Test fun `il nome compare dopo un terzo della corsa e lo spazio cresce con lui`() {
        assertEquals(0f, IconRowRules.nameAlpha(0f), 0f)
        assertEquals(0f, IconRowRules.nameAlpha(0.2f), 0f)
        assertEquals(0.3f, IconRowRules.nameAlpha(0.5f), 1e-5f)
        assertEquals(1f, IconRowRules.nameAlpha(1f), 1e-5f)
        assertEquals(1f, IconRowRules.nameAlpha(1.05f), 0f)
        assertEquals(0f, IconRowRules.nameGap(-0.1f), 0f)
        assertEquals(0.5f, IconRowRules.nameGap(0.5f), 0f)
        assertEquals(1f, IconRowRules.nameGap(1.2f), 0f)
    }

    @Test fun `Alt e una cifra da 1 a 5 sceglie la scheda che c'è`() {
        assertEquals(0, IconRowRules.altDigit(1, 5))
        assertEquals(4, IconRowRules.altDigit(5, 5))
        assertNull(IconRowRules.altDigit(5, 4))
        assertNull(IconRowRules.altDigit(6, 9))
        assertNull(IconRowRules.altDigit(0, 5))
    }
}
