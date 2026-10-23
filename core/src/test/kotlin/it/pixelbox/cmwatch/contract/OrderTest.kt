package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderTest {
    @Test fun shuffledSessionsComeBackInContractOrder() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val shuffled = s.sessions.reversed()
        assertEquals(listOf("ledger-api", "atlas-shop", "field-notes", "orbit-docs"), Order.sessions(shuffled).map { it.name })
    }

    @Test fun sameStateSortsByName() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val a = s.sessions[2].copy(name = "zeta"); val b = s.sessions[2].copy(name = "alpha")
        assertEquals(listOf("alpha", "zeta"), Order.sessions(listOf(a, b)).map { it.name })
    }

    @Test fun awaitingRanksWithBusy() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val awaiting = s.sessions[2].copy(name = "aaa-await", state = SessionState.AWAITING)
        assertEquals(listOf("ledger-api", "aaa-await", "atlas-shop", "field-notes", "orbit-docs"), Order.sessions(s.sessions + awaiting).map { it.name })
    }

    private fun state(ts: Long, published: Double?) = ContractJson.decodeState(Fixtures.stateIdle).copy(ts = ts, publishedAt = published)

    /** Senza `published_at` (relay precedente alla 1.43) l'età si conta da `ts`, come prima. */
    @Test fun freshnessThreeMinutes() {
        assertEquals(Freshness.Fresh, Freshness.of(state(1000, null), 1000 + 179))
        assertEquals(Freshness.Stale(3), Freshness.of(state(1000, null), 1000 + 180))
        assertEquals(Freshness.Stale(65), Freshness.of(state(1000, null), 1000 + 65 * 60 + 5))
    }

    /** 1.43: l'età si conta dalla pubblicazione; «PC lento» quando la raccolta ha preso 30 s o più. */
    @Test fun freshnessFromPublishedAt() {
        assertEquals(Freshness.Fresh, Freshness.of(state(1000, 1005.0), 1005 + 10))
        assertEquals(Freshness.Slow(45), Freshness.of(state(1000, 1045.0), 1045 + 10))
        assertEquals(Freshness.Stale(3), Freshness.of(state(1000, 1010.0), 1010 + 200))
    }

    @Test fun publishedTsFallsBackToTs() {
        assertEquals(1045L, state(1000, 1045.7).publishedTs)
        assertEquals(1000L, state(1000, null).publishedTs)
    }

    @Test fun durations() {
        assertEquals("0 m", Durations.since(100, 130))
        assertEquals("2 m", Durations.since(100, 100 + 150))
        assertEquals("1 h 05", Durations.since(0, 65 * 60))
        assertEquals("3 g", Durations.since(0, 3 * 86400 + 100))
    }

    /** Contratto 1.80: la scadenza di un needs-you si legge con il giorno: oggi solo l'ora, domani «domani», poi la data (23/10 21:23: «scade alle 21:23» per una scadenza a 24 ore). */
    @Test fun expiryTellsTheDay() {
        val zone = java.time.ZoneId.of("Europe/Rome")
        val now = java.time.LocalDate.of(2026, 10, 23).atTime(21, 23).atZone(zone).toEpochSecond()
        assertEquals(Durations.Expiry(Durations.Day.TODAY, "23:00"), Durations.expiry(now + 97 * 60, now, zone))
        assertEquals(Durations.Expiry(Durations.Day.TOMORROW, "21:23"), Durations.expiry(now + 86400, now, zone))
        assertEquals(Durations.Expiry(Durations.Day.LATER, "09:00", "26/10"), Durations.expiry(java.time.LocalDate.of(2026, 10, 26).atTime(9, 0).atZone(zone).toEpochSecond(), now, zone))
        // Già scaduta: resta «oggi» con l'ora, il relay la toglie da solo.
        assertEquals(Durations.Expiry(Durations.Day.TODAY, "20:00"), Durations.expiry(now - 83 * 60, now, zone))
    }
}
