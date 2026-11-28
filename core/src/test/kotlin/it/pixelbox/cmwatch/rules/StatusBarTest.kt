package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.StatusBar.Parts
import it.pixelbox.cmwatch.rules.StatusBar.Step
import it.pixelbox.cmwatch.rules.StatusBar.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** app-barra-stato-28-11: le regole della barra di stato, uguali a web/src/lib/statusBar.test.ts. */
class StatusBarTest {
    private val tipica = Parts(listOf(3, 1, 4), "Opus 5", "high", 89, 34)

    @Test fun `la batteria dice quanto resta delle 5 ore, fra 0 e 100`() {
        assertEquals(89, StatusBar.remaining(11))
        assertEquals(100, StatusBar.remaining(0))
        assertEquals(0, StatusBar.remaining(100))
        assertEquals(0, StatusBar.remaining(120))
        assertEquals(100, StatusBar.remaining(-5))
    }

    @Test fun `la batteria è ambra quando resta il 30 % o meno e rossa col 10 % o meno`() {
        assertEquals(Tone.NEUTRAL, StatusBar.batteryTone(31))
        assertEquals(Tone.WARN, StatusBar.batteryTone(30))
        assertEquals(Tone.WARN, StatusBar.batteryTone(11))
        assertEquals(Tone.ALERT, StatusBar.batteryTone(10))
        assertEquals(Tone.ALERT, StatusBar.batteryTone(0))
    }

    @Test fun `l'anellino del contesto è ambra dal 70 %`() {
        assertEquals(Tone.NEUTRAL, StatusBar.ringTone(69))
        assertEquals(Tone.WARN, StatusBar.ringTone(70))
        assertEquals(Tone.WARN, StatusBar.ringTone(100))
    }

    @Test fun `il modello con l'effort ai primi due passi, poi senza, e coi soli glifi niente`() {
        assertEquals("Opus 5 · high", StatusBar.modelText("Opus 5", "high", Step.FULL))
        assertEquals("Opus 5 · high", StatusBar.modelText("Opus 5", "high", Step.SMALL))
        assertEquals("Opus 5", StatusBar.modelText("Opus 5", "high", Step.NO_EFFORT))
        assertEquals("Opus 5", StatusBar.modelText("Opus 5", "high", Step.INSIDE))
        assertEquals("", StatusBar.modelText("Opus 5", "high", Step.GLYPHS))
        assertEquals("Opus 5", StatusBar.modelText("Opus 5", null, Step.FULL))
        assertEquals("", StatusBar.modelText(null, null, Step.FULL))
    }

    @Test fun `con tutto lo spazio la barra scrive tutto, a 360 come in una colonna larga`() {
        assertEquals(Step.FULL, StatusBar.step(360f, tipica))
        assertEquals(Step.FULL, StatusBar.step(407f, tipica))
    }

    @Test fun `stringendo passa per i passi in ordine, alle soglie scritte nel commento`() {
        assertEquals(Step.FULL, StatusBar.step(319f, tipica))
        assertEquals(Step.SMALL, StatusBar.step(318f, tipica))
        assertEquals(Step.SMALL, StatusBar.step(302f, tipica))
        assertEquals(Step.NO_EFFORT, StatusBar.step(301f, tipica))
        assertEquals(Step.NO_EFFORT, StatusBar.step(264.5f, tipica))
        assertEquals(Step.INSIDE, StatusBar.step(263f, tipica))
        assertEquals(Step.INSIDE, StatusBar.step(237f, tipica))
        assertEquals(Step.GLYPHS, StatusBar.step(236f, tipica))
        assertEquals(Step.GLYPHS, StatusBar.step(120f, tipica))
        var last = 0
        for (w in 420 downTo 120) {
            val i = StatusBar.step(w.toFloat(), tipica).ordinal
            assertTrue(i >= last)
            last = i
        }
    }

    @Test fun `al passo scelto il testo ci sta per intero, margini compresi`() {
        for (w in 190..420 step 7) {
            val s = StatusBar.step(w.toFloat(), tipica)
            assertTrue(StatusBar.needs(s, tipica) <= w)
        }
    }

    @Test fun `con il testo grande la barra si stringe prima, a 360 il testo piccolo`() {
        assertEquals(Step.SMALL, StatusBar.step(360f, tipica, 1.3f))
        assertEquals(Step.NO_EFFORT, StatusBar.step(360f, tipica, 2f))
        assertTrue(StatusBar.leftWidth(tipica.counts, 1.3f) > StatusBar.leftWidth(tipica.counts))
    }

    @Test fun `i numeri delle icone restano a ogni passo, anche coi soli glifi`() {
        assertTrue(StatusBar.leftWidth(listOf(3, 1, 4)) > StatusBar.leftWidth(listOf(0, 0, 0)))
        assertEquals(
            StatusBar.needs(Step.FULL, tipica) - StatusBar.rightWidth(Step.FULL, tipica),
            StatusBar.needs(Step.GLYPHS, tipica) - StatusBar.rightWidth(Step.GLYPHS, tipica), 0.001f,
        )
        assertEquals(StatusBar.BAT + StatusBar.ITEM_GAP + StatusBar.RING, StatusBar.rightWidth(Step.GLYPHS, tipica), 0.001f)
    }

    @Test fun `a zero solo l'icona, un numero a due cifre allarga la sinistra`() {
        assertEquals(17f * 3 + 10f * 2, StatusBar.leftWidth(listOf(0, 0, 0)), 0.001f)
        assertTrue(StatusBar.leftWidth(listOf(12, 0, 0)) > StatusBar.leftWidth(listOf(1, 0, 0)))
    }

    @Test fun `nel foglio della master i margini sono 16, lo stesso testo passa al passo dopo 8 dp prima`() {
        assertEquals(8f, StatusBar.needs(Step.FULL, tipica, 1f, StatusBar.PAD_SHEET) - StatusBar.needs(Step.FULL, tipica), 0.001f)
        assertEquals(Step.FULL, StatusBar.step(319f, tipica))
        assertEquals(Step.SMALL, StatusBar.step(319f, tipica, 1f, StatusBar.PAD_SHEET))
        assertEquals(Step.FULL, StatusBar.step(327f, tipica, 1f, StatusBar.PAD_SHEET))
    }

    @Test fun `senza quota né contesto a destra resta solo il modello`() {
        val solo = tipica.copy(left = null, ctx = null)
        assertEquals(6 * 0.6f * 9f, StatusBar.rightWidth(Step.INSIDE, solo), 0.001f)
        assertEquals(0f, StatusBar.rightWidth(Step.GLYPHS, solo), 0f)
        assertEquals(Step.FULL, StatusBar.step(300f, solo))
    }
}
