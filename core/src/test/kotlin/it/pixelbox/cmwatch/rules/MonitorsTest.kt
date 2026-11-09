package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** I Monitoraggi coi dati dei mockup (app-monitoraggi-09-11); gli stessi casi in web/src/lib/monitors.test.ts. */
class MonitorsTest {
    private val page = Monitors.decode(javaClass.getResource("/monitors-mockup.json")!!.readText())
    private fun one(id: String) = page.items.single { it.id == id }

    @Test fun theMockupsCoverTheWholeCatalog() {
        assertEquals(listOf("prezzi-portatili", "adozione-plugin", "stelle-github", "quota-gemini", "sito-online"), page.items.map { it.id })
        assertEquals(Monitors.Form.entries.toSet(), page.items.flatMap { it.charts }.mapNotNull(Monitors::formOf).toSet())
        // Una forma fuori dal catalogo non si disegna.
        assertNull(Monitors.formOf(Monitors.Chart("pie")))
    }

    @Test fun onlyATriggeredThresholdIsToLookAt() {
        assertEquals(2, Monitors.looking(page.items))
        assertEquals(listOf("prezzi-portatili", "quota-gemini"), page.items.filter { it.look }.map { it.id })
        assertEquals(0, Monitors.looking(page.items.filter { !it.look }))
    }

    @Test fun oneScalePerChartWithAboutThreeTicks() {
        val users = one("adozione-plugin").charts[0].series[0].values
        assertEquals(Monitors.Scale(100.0, 140.0, listOf(100.0, 120.0, 140.0)), Monitors.scale(users))
        // Le barre partono da zero.
        val npm = one("adozione-plugin").charts[1].series[0].values
        assertEquals(Monitors.Scale(0.0, 1000.0, listOf(0.0, 500.0, 1000.0)), Monitors.scale(npm, fromZero = true))
        assertEquals(0.75f, Monitors.scale(users).y(130.0), 1e-4f)
        // Un valore solo, o tutti uguali: una linea a metà.
        assertEquals(0.5f, Monitors.scale(listOf(7.0, 7.0)).y(7.0), 1e-4f)
    }

    @Test fun theDotsGoOnTheMinimumTheMaximumAndTheLastValue() {
        val c = one("prezzi-portatili").charts.single()
        val yoga = c.series[0].values
        assertEquals(c.days.indexOf("28/11"), Monitors.minIndex(yoga))
        assertEquals(c.days.indexOf("28/11"), Monitors.lastIndex(yoga))
        // MSI Flip: il minimo il 27/11, poi risale.
        assertEquals(c.days.indexOf("27/11"), Monitors.minIndex(c.series[1].values))
        val u = one("adozione-plugin").charts[0]
        assertEquals(135.0, u.series[0].values[Monitors.maxIndex(u.series[0].values)!!])
        assertNull(Monitors.lastIndex(listOf(null, null)))
    }

    @Test fun aTouchPicksTheNearestDay() {
        assertEquals(0, Monitors.dayAt(0f, 24))
        assertEquals(23, Monitors.dayAt(1.2f, 24))
        assertEquals(12, Monitors.dayAt(0.5f, 24))
        assertEquals(0, Monitors.dayAt(0.7f, 1))
    }

    @Test fun theMeterShowsTheLimitAtFourFifths() {
        assertEquals(0.8f to true, Monitors.meter(80.0, 80.0).let { (f, o) -> (Math.round(f * 100) / 100f) to o })
        assertEquals(false, Monitors.meter(60.0, 80.0).second)
        assertEquals(1f, Monitors.meter(500.0, 80.0).first)
    }
}
