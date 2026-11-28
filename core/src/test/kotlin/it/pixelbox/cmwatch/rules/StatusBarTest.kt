package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.StatusBar.Parts
import it.pixelbox.cmwatch.rules.StatusBar.Step
import it.pixelbox.cmwatch.rules.StatusBar.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/** app-barra-ibrida-28-11: le regole dello stato nella capsula (H1), uguali a web/src/lib/statusBar.test.ts. */
class StatusBarTest {
    private val tipica = Parts("Opus 5", "high", 11, 34)

    @Test fun `la quota si scrive come la pillola di Utilizzo, quanto è usato`() {
        assertEquals("5h 11%", StatusBar.quotaText(11))
        assertEquals("5h 0%", StatusBar.quotaText(0))
        assertEquals(42, StatusBar.used(42))
        assertEquals(null, StatusBar.used(null))
    }

    @Test fun `la quota è ambra dal 70 % usato e rossa dal 90 %`() {
        assertEquals(Tone.NEUTRAL, StatusBar.quotaTone(69))
        assertEquals(Tone.WARN, StatusBar.quotaTone(70))
        assertEquals(Tone.WARN, StatusBar.quotaTone(89))
        assertEquals(Tone.ALERT, StatusBar.quotaTone(90))
        assertEquals(Tone.ALERT, StatusBar.quotaTone(100))
    }

    @Test fun `l'anellino del contesto è ambra dal 70 %`() {
        assertEquals(Tone.NEUTRAL, StatusBar.ringTone(69))
        assertEquals(Tone.WARN, StatusBar.ringTone(70))
    }

    @Test fun `prima via l'effort, poi il modello resta finché c'è posto, poi niente`() {
        assertEquals("Opus 5 · high", StatusBar.modelText("Opus 5", "high", Step.FULL))
        assertEquals("Opus 5", StatusBar.modelText("Opus 5", "high", Step.NO_EFFORT))
        assertEquals("Opus 5", StatusBar.modelText("Opus 5", "high", Step.SMALL))
        assertEquals("", StatusBar.modelText("Opus 5", "high", Step.NO_MODEL))
        assertEquals("", StatusBar.modelText("Opus 5", "high", Step.NO_PCT))
        assertEquals("Opus 5", StatusBar.modelText("Opus 5", null, Step.FULL))
    }

    @Test fun `ogni passo stringe lo stato, nelle larghezze scritte nel commento`() {
        val w = Step.entries.map { s -> StatusBar.statusWidth(s, tipica).roundToInt() }
        assertEquals(listOf(214, 168, 153, 111, 51), w)
    }

    @Test fun `con tutto lo spazio lo stato è intero, e si riduce in ordine stringendo la riga`() {
        assertEquals(Step.FULL, StatusBar.step(800f, 190f, tipica))
        var last = 0
        for (x in 800 downTo 200) {
            val i = StatusBar.step(x.toFloat(), 190f, tipica).ordinal
            assertTrue(i >= last)
            last = i
        }
        assertEquals(Step.NO_PCT, StatusBar.step(200f, 190f, tipica))
    }

    @Test fun `a 360, con le tre schede e il nome più lungo aperto, lo stato resta senza modello, in una colonna da 407 col testo piccolo`() {
        assertEquals(Step.NO_MODEL, StatusBar.step(360f, 212f, tipica))
        assertEquals(Step.SMALL, StatusBar.step(407f, 212f, tipica))
        assertEquals(Step.NO_EFFORT, StatusBar.step(420f, 212f, tipica))
    }

    @Test fun `al passo scelto la riga ci sta per intero, margini compresi`() {
        for (x in 260..700 step 9) {
            val s = StatusBar.step(x.toFloat(), 190f, tipica)
            if (s != Step.NO_PCT) assertTrue(StatusBar.needs(s, 190f, tipica) <= x)
        }
    }

    @Test fun `col testo grande lo stato si riduce prima`() {
        assertTrue(StatusBar.statusWidth(Step.FULL, tipica, 1.3f) > StatusBar.statusWidth(Step.FULL, tipica))
        assertTrue(StatusBar.step(420f, 190f, tipica, 1.3f).ordinal > StatusBar.step(420f, 190f, tipica).ordinal)
    }

    @Test fun `senza quota né contesto lo stato è il solo modello, e senza niente non c'è`() {
        val solo = tipica.copy(used = null, ctx = null)
        assertEquals(15f + 6f + 13 * 0.6f * 11f, StatusBar.statusWidth(Step.FULL, solo), 0.001f)
        assertEquals(0f, StatusBar.statusWidth(Step.NO_MODEL, solo), 0f)
    }
}
