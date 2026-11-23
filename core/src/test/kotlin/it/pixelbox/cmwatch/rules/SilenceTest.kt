package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.WaitCard
import it.pixelbox.cmwatch.contract.WaitOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.util.Locale

/** app-silenzio-assenso-23-11 (contratto 1.159): quando c'è «Dai per risolto se tace» e come la carta dice la data. Come silence.test.ts. */
class SilenceTest {
    private val list = ContractJson.decodeWaits(kotlinx.serialization.json.Json.parseToJsonElement(File("../contract/attese-rd-sample.json").readText())
        .let { e -> (e as kotlinx.serialization.json.JsonObject).getValue("attese").toString() }).items
    private val ops = listOf("attese", "silence")
    private fun item(kind: String) = list.first { it.kind == kind }

    @Test fun onOkAndProvaOnly() {
        assertEquals(listOf("ok", "prova"), list.filter { Silence.offer(it, ops) }.map { it.kind })
        // Senza l'op il relay non sa farlo: niente tasto.
        assertFalse(Silence.offer(item("ok"), listOf("attese")))
    }

    @Test fun neverOnAChoiceOrProduction() {
        assertFalse(Silence.offer(item("ok").copy(options = listOf(WaitOption(1, "A"), WaitOption(2, "B"))), ops))
        assertFalse(Silence.offer(item("ok").copy(label = "choose"), ops))
        assertFalse(Silence.offer(item("ok").copy(card = WaitCard(deploy = true)), ops))
        assertFalse(Silence.offer(item("ok").copy(task = null), ops))
        assertFalse(Silence.offer(item("ok").copy(state = "approved"), ops))
    }

    @Test fun theDates() {
        val z = ZoneId.of("Europe/Rome")
        val until = java.time.ZonedDateTime.of(2026, 11, 26, 13, 30, 0, 0, z).toEpochSecond()
        assertEquals("giovedì 26/11 alle 13:30", Silence.whenLong(until, z, Locale.ITALIAN))
        assertEquals("Thursday 26/11 at 13:30", Silence.whenLong(until, z, Locale.ENGLISH))
        assertEquals("gio 26/11", Silence.whenShort(until, z, Locale.ITALIAN))
        assertEquals("Thu 26/11", Silence.whenShort(until, z, Locale.ENGLISH))
        assertTrue(Silence.pending(until, until - 1))
        assertFalse(Silence.pending(until, until))
        assertFalse(Silence.pending(null, 0))
    }
}
