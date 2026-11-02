package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.transport.FakeTransport
import org.junit.Assert.*
import org.junit.Test

/** Contratto 1.100, sulle fixture del relay: gli stessi casi di primaRaccolta.test.ts. */
class PrimaRaccoltaTest {
    private val page = ContractJson.decodePrimaRaccolta(FakeTransport({ Fixtures.read("$it.json") }).sampleText("prima_raccolta"))
    private val summary = ContractJson.decodeState(Fixtures.stateStale).primaRaccolta!!

    @Test fun prima_raccolta_summaryAndPageDecode() {
        assertEquals(PrimaRaccoltaSummary("ask", 7, 8, true, null), summary)
        assertEquals("ask", page.status); assertEquals(0, page.cost); assertNull(page.consent)
        assertEquals(listOf("plugins", "sessions"), page.sections.map { it.key })
        assertEquals("12 plugins, 2 to update, 30 skills", page.sections[0].text)
        assertEquals("It fills as soon as you answer.", page.sections[1].whenFills)
    }

    @Test fun prima_raccolta_opensByItselfOnce() {
        assertTrue(PrimaRaccolta.autoOpen(summary, seen = false))
        assertTrue(PrimaRaccolta.autoOpen(summary.copy(status = "running"), seen = false))
        assertFalse(PrimaRaccolta.autoOpen(summary, seen = true))
        assertFalse(PrimaRaccolta.autoOpen(summary.copy(status = "done"), seen = false))
        assertFalse(PrimaRaccolta.autoOpen(null, seen = false))
    }

    @Test fun prima_raccolta_emptySectionSaysWhy() {
        assertFalse(PrimaRaccolta.empty(page.sections[0]))
        assertTrue(PrimaRaccolta.empty(page.sections[1]))
        assertEquals("Past sessions are read only with your yes.", page.sections[1].why)
    }

    @Test fun prima_raccolta_consentAsksOnce() {
        assertTrue(PrimaRaccolta.askConsent(page))
        assertFalse(PrimaRaccolta.askConsent(page.copy(consent = true)))
        assertFalse(PrimaRaccolta.askConsent(page.copy(consent = false)))
        assertFalse(PrimaRaccolta.askConsent(page.copy(askText = null)))
    }

    @Test fun prima_raccolta_startOnlyWhenNotRunning() {
        assertFalse(PrimaRaccolta.canStart(page))
        assertTrue(PrimaRaccolta.canStart(page.copy(status = "none")))
        assertTrue(PrimaRaccolta.canStart(page.copy(status = "interrupted")))
        assertFalse(PrimaRaccolta.canStart(page.copy(status = "running")))
    }

    @Test fun prima_raccolta_firstRoundSaysItsCostAndStartsOnce() {
        assertNull(PrimaRaccolta.firstRoundOffer(page))
        val done = page.copy(status = "done")
        assertTrue(PrimaRaccolta.firstRoundOffer(done)!!.contains("It starts only if you touch it"))
        assertNull(PrimaRaccolta.firstRoundOffer(done.copy(firstRound = done.firstRound!!.copy(at = 5))))
        assertTrue(PrimaRaccolta.finished(done)); assertFalse(PrimaRaccolta.finished(page))
    }

    @Test fun prima_raccolta_touchCarriesActionAndValue() {
        val c = Cmd("i", CmdOp.PRIMA_RACCOLTA_AZIONE, null, null, 1, "p", device = "phone", action = "consent", value = "si")
        val j = ContractJson.encode(c)
        assertTrue(j, j.contains("\"op\":\"prima_raccolta_azione\"") && j.contains("\"action\":\"consent\"") && j.contains("\"value\":\"si\"") && j.contains("\"device\":\"phone\""))
        assertFalse(ContractJson.encode(c.copy(value = null)).contains("\"value\""))
    }
}
