package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.WeekPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Gli stessi casi di week7.test.ts. Settimana da giovedì 03:00 (ora 0), contratto 1.86: una voce a ogni cambio.
class Week7Test {
    private val start = 1_789_005_600L
    private val reset = start + Week7.WEEK_S
    private fun h(n: Int) = start + n * 3600L

    // I campioni partono a metà settimana (dal lunedì sera, ora 110): i giorni prima non hanno barra, il primo è parziale.
    private val lateSamples = WeekPoints(start, listOf(listOf(110, 50), listOf(112, 53), listOf(120, 58), listOf(130, 62), listOf(140, 65)))

    @Test fun lineIsAStepFunctionUpToNow() {
        val c = Week7.chart(WeekPoints(start, listOf(listOf(0, 2), listOf(24, 30), listOf(25, 38))), 38, reset, h(30))
        assertEquals(listOf(0f to 2, 24 / 168f to 2, 24 / 168f to 30, 25 / 168f to 30, 25 / 168f to 38, 30 / 168f to 38), c.points)
        assertEquals(30 / 168f, c.nowX, 1e-6f)
    }

    // Il ritmo delle ultime 48 ore: da ora 92 (prima dei campioni: si parte dal primo, ora 110) a 140 = 15 punti in 30 ore.
    @Test fun projectionAtTheLast48HoursPace() {
        val c = Week7.chart(lateSamples, 65, reset, h(140))
        // 35 punti a 0,5 l'ora = 70 ore: ora 210, dopo il rinnovo (168): niente ora del 100%, al rinnovo 65 + 14 = 79.
        assertNull(c.fullAt)
        assertEquals(79, c.projected)
    }

    @Test fun fastPaceGivesTheTimeOf100BeforeTheRenewal() {
        val c = Week7.chart(WeekPoints(start, listOf(listOf(100, 40), listOf(124, 64), listOf(136, 76))), 76, reset, h(136))
        // 36 punti in 36 ore = 1 l'ora: 24 punti, ora 160.
        assertEquals(h(160), c.fullAt)
        assertEquals(100, c.projected)
    }

    // Sotto le 12 ore di campioni il ritmo è quello medio dall'inizio della settimana (w7 su ore passate).
    @Test fun shortSamplesFallBackToTheWeekAverage() {
        val c = Week7.chart(WeekPoints(start, listOf(listOf(100, 40), listOf(104, 44))), 44, reset, h(110))
        // 44 punti in 110 ore = 0,4 l'ora per 58 ore: 44 + 23 = 67.
        assertEquals(67, c.projected)
        assertNull(c.fullAt)
    }

    @Test fun daysSpentAgainstTheBudget() {
        val d = Week7.days(lateSamples, 65, h(140))
        assertEquals(7, d.size)
        // Giorni 0-3 prima dei campioni, il giorno 4 (ore 96-120) parte alle 110: parziale, senza barra.
        assertEquals(listOf(null, null, null, null, null), d.take(5).map { it.spent })
        assertTrue(d[4].partial)
        // Giorno 5 (ore 120-144): dal 53 dell'ora 112 (vale fino all'ora 120) al 65 di adesso; è oggi.
        assertEquals(12, d[5].spent)
        assertTrue(d[5].today)
        assertTrue(d[6].future)
        assertEquals(h(110), Week7.samplesFrom(lateSamples))
    }

    @Test fun fromTheResetEveryDayCounts() {
        val p = WeekPoints(start, listOf(listOf(0, 2), listOf(2, 4), listOf(3, 5), listOf(24, 30), listOf(25, 38)))
        val d = Week7.days(p, 38, h(30))
        // Il giorno 0 parte da 0 al rinnovo e chiude al 5 dell'ora 3; il giorno 1 (oggi) da 5 a 38.
        assertEquals(5, d[0].spent)
        assertEquals(33, d[1].spent)
        assertNull(Week7.samplesFrom(p))
        assertEquals(100.0 / 7, Week7.BUDGET, 1e-9)
    }
}
