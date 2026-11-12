package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.MonitorCard
import it.pixelbox.cmwatch.contract.MonitorRefresh
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

    // Contratto 1.138 (app-monitor-link-12-11): la pagina della cosa guardata, sulla scheda, sulle serie e sull'avviso scattato.
    @Test fun theLinksOfTheMonitoredThing() {
        val (ad, p) = ContractJson.decodeMonitors(fake.sampleText("monitors")).items
        assertNull(ad.url); assertEquals(listOf<String?>(null), ad.series.map { it.url })
        assertEquals("https://shop.example/offer/yoga-9i", p.url)
        assertEquals(listOf("https://shop.example/offer/yoga-9i", "https://shop.example/offer/zenbook-14"), p.series.map { it.url })
        assertEquals("https://shop.example/offer/yoga-9i", p.hits.single().url)
    }

    // Gli stessi casi di pageUrl in web/src/lib/monitors.test.ts: solo http e https, su una riga, fino a 2000 caratteri.
    @Test fun onlyHttpAndHttpsOnOneLine() {
        assertEquals("https://shop.example/offer/yoga-9i", Monitors.pageUrl("https://shop.example/offer/yoga-9i"))
        assertEquals("http://shop.example/a?b=1", Monitors.pageUrl("http://shop.example/a?b=1"))
        assertEquals("https://shop.example/x", Monitors.pageUrl(" https://shop.example/x "))
        listOf("javascript:alert(1)", "file:///etc/passwd", "ftp://x.example/a", "https://a.example/one two", "https://a.example/" + Char(10) + "b",
            "https://a.example/" + "x".repeat(2000), "", "shop.example/x").forEach { u -> assertNull(u, Monitors.pageUrl(u)) }
        assertNull(Monitors.pageUrl(null))
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

    // Contratto 1.133 (app-monitoraggi-aggiorna-10-11): il tasto «Aggiorna adesso». Gli stessi casi in web/src/lib/monitors.test.ts.
    private val ops = listOf("monitors", "monitor_refresh")
    private fun card(state: String? = "idle", refreshable: Boolean = true, retry: Long? = null, message: String? = null, rc: Int? = null, ended: Long? = null) =
        MonitorCard("m", refreshable = refreshable, at = 500, refresh = state?.let { MonitorRefresh(it, started = 900, ended = ended, rc = rc, message = message, retryAt = retry, timeoutS = 60) })

    @Test fun theRefreshButtonOnlyWhenRefreshableAndTheRelayHasTheOp() {
        assertNull(Monitors.refresh(card(refreshable = false, state = null), ops, 1000))
        assertNull("relay senza monitor_refresh", Monitors.refresh(card(), listOf("monitors"), 1000))
        assertNull(Monitors.refresh(card(), null, 1000))
        assertEquals(Monitors.Refresh.Ready, Monitors.refresh(card(), ops, 1000))
    }

    @Test fun theRefreshButtonSpinsWhileRunningAndWaitsBeforeRetryAt() {
        assertEquals(Monitors.Refresh.Running, Monitors.refresh(card("running"), ops, 1000))
        assertEquals("il tocco partito, la risposta non ancora", Monitors.Refresh.Running, Monitors.refresh(card(), ops, 1000, asked = true))
        assertEquals(Monitors.Refresh.Wait(1030), Monitors.refresh(card("ok", retry = 1030, ended = 990), ops, 1000))
        assertEquals(Monitors.Refresh.Ready, Monitors.refresh(card("ok", retry = 1000, ended = 990), ops, 1000))
    }

    @Test fun theOutcomeIsTheNewTimeOrOneLineOfError() {
        assertNull(Monitors.outcome(card()))
        assertNull(Monitors.outcome(card("running")))
        assertEquals(Monitors.Outcome.Done(990), Monitors.outcome(card("ok", ended = 990)))
        assertEquals(Monitors.Outcome.Failed("curl: (6) host non trovato"), Monitors.outcome(card("error", message = "curl: (6) host non trovato\nseconda riga", rc = 6)))
        assertEquals(Monitors.Outcome.Failed(null, timeout = true), Monitors.outcome(card("error", rc = 124)))
        assertEquals("il rifiuto del relay vince", Monitors.Outcome.Failed("too early: retry in 12 s"), Monitors.outcome(card("ok", ended = 990), failed = "too early: retry in 12 s"))
    }

    // La demo: si aggiorna quello che la fixture dice aggiornabile, gli altri no; il giro parte, gira e finisce con l'ora nuova, poi è troppo presto.
    @Test fun theDemoRefreshesTheFirstMonitorOnly() {
        var t = 2_000_000_000L
        var n = 0
        val demo = FakeTransport(load = { File("../contract/$it.json").readText() }, now = { t })
        fun run(op: it.pixelbox.cmwatch.contract.CmdOp, arg: String?) = kotlinx.coroutines.runBlocking { demo.send(it.pixelbox.cmwatch.contract.Cmd(id = "c${n++}", op = op, session = null, arg = arg, issued = t, by = "test")) {} }
        val page = ContractJson.decodeMonitors(run(it.pixelbox.cmwatch.contract.CmdOp.MONITORS, null).text)
        // La fixture del relay (1.133): prezzi-portatili si aggiorna, adozione no.
        val first = page.items.first { it.refreshable }; val other = page.items.first { !it.refreshable }
        assertTrue(first.refreshable); assertFalse(other.refreshable); assertNull(other.refresh)
        assertFalse(run(it.pixelbox.cmwatch.contract.CmdOp.MONITOR_REFRESH, other.id).ok)
        val started = run(it.pixelbox.cmwatch.contract.CmdOp.MONITOR_REFRESH, first.id)
        assertTrue(started.ok); assertEquals("running", ContractJson.decodeMonitor(started.text).refresh?.state)
        t += 5
        val done = ContractJson.decodeMonitor(run(it.pixelbox.cmwatch.contract.CmdOp.MONITORS, first.id).text)
        assertEquals("ok", done.refresh?.state); assertTrue((done.refresh?.retryAt ?: 0) > t)
        assertFalse("troppo presto", run(it.pixelbox.cmwatch.contract.CmdOp.MONITOR_REFRESH, first.id).ok)
    }

    // app-monitoraggi-attesa-10-11: la sezione c'è appena il riassunto dice n > 0, col suo numero e le righe in arrivo; poi le schede.
    @Test fun theSectionShowsFromTheSummaryBeforeTheCards() {
        assertNull(Monitors.section(null, null))
        assertNull(Monitors.section(MonitorsSummary(0, 0, null), null))
        assertEquals(Monitors.Section(3, loading = true), Monitors.section(MonitorsSummary(3, 1, 100), null))
        assertEquals(Monitors.Section(3, loading = true), Monitors.section(MonitorsSummary(3, 1, 100), emptyList()))
        assertEquals(Monitors.Section(mock.size, loading = false), Monitors.section(MonitorsSummary(3, 1, 100), mock))
        assertEquals(listOf(5L, 10L, 20L, 40L, 60L, 60L), (0..5).map(Monitors::retryAfter))
    }
}
