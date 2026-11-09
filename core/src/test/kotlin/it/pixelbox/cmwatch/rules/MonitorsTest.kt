package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.MonitorCard
import it.pixelbox.cmwatch.contract.MonitorsSummary
import it.pixelbox.cmwatch.transport.FakeTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * I Monitoraggi (app-monitoraggi-09-11, contratto 1.132): la fixture del relay e quella dei mockup, che ha una scheda per ogni forma;
 * gli stessi casi in web/src/lib/monitors.test.ts.
 */
class MonitorsTest {
    private val fake = FakeTransport(load = { File("../contract/$it.json").readText() })
    private val mock = ContractJson.decodeMonitors(javaClass.getResource("/monitors-mockup.json")!!.readText()).items
    private fun one(id: String) = mock.single { it.id == id }

    @Test fun theRelayFixtureReads() {
        val page = ContractJson.decodeMonitors(fake.sampleText("monitors"))
        assertEquals(listOf("adozione", "prezzi-portatili"), page.items.map { it.id })
        val p = page.items[1]
        assertEquals("multiples", p.form); assertTrue(Monitors.look(p)); assertEquals("1.749 €", p.value)
        assertEquals(listOf("Yoga 9i", "Zenbook 14"), p.series.map { it.name })
        assertEquals(1749.0, p.series[0].last); assertEquals("sotto 1.800 € · Yoga 9i", p.hits.single().text)
        assertEquals("prezzi-portatili", Monitors.idOf(p.link!!))
        val single = ContractJson.decodeMonitor(fake.sampleResult("monitors", "prezzi-portatili").second)
        assertEquals(p, single)
        assertTrue(page.items[0].example)
        // Lo stato: il conto c'è solo con dei monitoraggi; le tre fixture non ne hanno, ma conoscono l'op.
        val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
        assertNull(st.monitors); assertTrue("monitors" in st.ops.orEmpty())
    }

    @Test fun theMockupsCoverTheWholeCatalog() {
        assertEquals(Monitors.Form.entries.toSet(), mock.mapNotNull(Monitors::formOf).toSet())
        assertNull(Monitors.formOf(MonitorCard("x", form = "pie")))
    }

    @Test fun onlyATriggeredThresholdIsToLookAt() {
        assertEquals(listOf("prezzi-portatili", "quota-gemini"), mock.filter(Monitors::look).map { it.id })
        assertEquals(2, Monitors.looking(null, mock))
        // Lo stato vince: è lui che conta anche le schede non ancora lette.
        assertEquals(3, Monitors.looking(MonitorsSummary(9, 3, 0), mock))
        assertEquals(0, Monitors.looking(null, null))
    }

    @Test fun theLineUnderTheTitleIsTheVerdictOrTheDelta() {
        assertEquals("Settimana stabile: 130 utenti nello Store, −1 in 7 giorni.", Monitors.line(one("utenti-store")))
        assertEquals("−1 in 7 giorni", Monitors.line(one("adozione")))
    }

    @Test fun oneScalePerChartWithAboutThreeTicks() {
        val users = Monitors.values(one("utenti-store").series[0])
        assertEquals(Monitors.Scale(100.0, 140.0, listOf(100.0, 120.0, 140.0)), Monitors.scale(users))
        val npm = Monitors.values(one("download-npm").series[0])
        assertEquals(Monitors.Scale(0.0, 1000.0, listOf(0.0, 500.0, 1000.0)), Monitors.scale(npm, fromZero = true))
        assertEquals(0.75f, Monitors.scale(users).y(130.0), 1e-4f)
        assertEquals(0.5f, Monitors.scale(listOf(7.0, 7.0)).y(7.0), 1e-4f)
    }

    @Test fun theDotsGoOnTheMinimumTheMaximumAndTheLastValue() {
        val yoga = Monitors.values(one("prezzi-portatili").series[0])
        assertEquals(yoga.lastIndex, Monitors.minIndex(yoga)); assertEquals(yoga.lastIndex, Monitors.lastIndex(yoga))
        val msi = Monitors.values(one("prezzi-portatili").series[1])
        assertEquals(msi.size - 2, Monitors.minIndex(msi))
        val u = Monitors.values(one("utenti-store").series[0])
        assertEquals(135.0, u[Monitors.maxIndex(u)!!])
    }

    @Test fun aTouchPicksTheNearestPointOnTheSharedTimeAxis() {
        val c = one("prezzi-portatili")
        val span = Monitors.span(c)
        val t = Monitors.times(c.series[0])
        assertEquals(0, Monitors.nearest(t, 0f, span)); assertEquals(t.lastIndex, Monitors.nearest(t, 1.3f, span))
        assertEquals(10, Monitors.nearest(t, 0.5f, span))
        assertEquals(1f, Monitors.x(t.last(), span))
    }

    @Test fun theMeterFillsToPctWithTheThresholdMarked() {
        val (fill, mark) = Monitors.meter(one("quota-gemini"))
        assertEquals(0.82f, fill, 1e-4f); assertEquals(0.8f, mark!!, 1e-4f)
        assertFalse(Monitors.up(0.0)); assertTrue(Monitors.up(1.0))
    }

    @Test fun links() {
        assertEquals("prezzi-portatili", Monitors.idOf("https://supervisor.example.com/#/monitor/prezzi-portatili"))
        assertEquals("adozione", Monitors.idOf("https://supervisor.example.com/?demo#/monitor/adozione"))
        assertNull(Monitors.idOf("https://supervisor.example.com/#/attesa/w123"))
        assertNull(Monitors.idOf("https://supervisor.example.com/#/monitor/"))
    }
}
