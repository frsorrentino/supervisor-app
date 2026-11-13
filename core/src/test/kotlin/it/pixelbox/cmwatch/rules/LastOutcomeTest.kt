package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TimelineEvent
import it.pixelbox.cmwatch.contract.TimelinePage
import it.pixelbox.cmwatch.contract.TimelineSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

/** La riga degli esiti agganciata al tabellone (app-tabellone-utilizzo-b-13-11): l'ultimo esito di oggi a parole; gli stessi casi di kpi.test.ts. */
class LastOutcomeTest {
    private val utc = ZoneId.of("UTC")
    private val now = 1_789_214_700L // 12/09/2026 11:25 UTC
    private fun page(vararg ev: Pair<String, TimelineEvent>) = TimelinePage(since = 0, sessions = ev.groupBy({ it.first }, { it.second }).map { (s, e) -> TimelineSession(s, true, events = e) })

    @Test fun theLatestSettledOutcomeOfToday() {
        val p = page(
            "storefront" to TimelineEvent(now - 3600, "outcome", "checkout verde", type = "done"),
            "payments-api" to TimelineEvent(now - 600, "outcome", "rimborso parziale corretto\nseconda riga", type = "done"),
            "blog" to TimelineEvent(now - 60, "outcome", "sto scrivendo il post", type = "running"),
            "blog" to TimelineEvent(now - 30, "commit", "bozza"),
            "docs" to TimelineEvent(now - 90_000, "outcome", "pagine di ieri", type = "done"),
        )
        val last = Kpi.lastOutcome(p, now, utc)!!
        assertEquals("payments-api" to "rimborso parziale corretto", last.session to last.text)
    }

    @Test fun nothingWithoutAPageOrOutcomes() {
        assertNull(Kpi.lastOutcome(null, now, utc))
        assertNull(Kpi.lastOutcome(page("blog" to TimelineEvent(now - 60, "outcome", "in corso", type = "running")), now, utc))
    }
}
