package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.DonePage
import it.pixelbox.cmwatch.contract.DoneRow
import it.pixelbox.cmwatch.contract.DoneSilence
import it.pixelbox.cmwatch.contract.WaitAnswered
import it.pixelbox.cmwatch.contract.WaitRound
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

    // Contratto 1.160: la regola è del registro, `silence_ok`; l'app non ne ha una sua.
    @Test fun onlyWhereTheRelaySaysSo() {
        val yes = item("ok").copy(silenceOk = true)
        assertTrue(Silence.offer(yes, ops))
        assertTrue(Silence.offer(item("prova").copy(silenceOk = true), ops))
        assertFalse(Silence.offer(item("ok").copy(silenceOk = false), ops))
        // Un relay prima della 1.160 non ha il campo: niente tasto (la fixture ha «pubblica 1.4 sul Play Store»).
        assertEquals(emptyList<String>(), list.filter { Silence.offer(it, ops) }.map { it.kind })
        // Senza l'op il relay non sa farlo: niente tasto.
        assertFalse(Silence.offer(yes, listOf("attese")))
    }

    @Test fun neverOnAClosedCardOrWithoutATask() {
        assertFalse(Silence.offer(item("ok").copy(silenceOk = true, task = null), ops))
        assertFalse(Silence.offer(item("ok").copy(silenceOk = true, state = "approved"), ops))
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

    // Come la cronologia e le schede riconoscono un compito chiuso per silenzio assenso. Come silence.test.ts.
    @Test fun closedBySilence() {
        val rows = Lavori.doneFromPages(mapOf("2026-11-26" to DonePage("2026-11-26", listOf(
            DoneRow("atlas-chi-siamo", "Bozza della pagina Chi siamo", at = 1795700400, outcome = "green", silence = DoneSilence("ok", 1795699800)),
            DoneRow("app-trova", "Trova messaggi", at = 1795690000, outcome = "green"),
        ))), ZoneId.of("Europe/Rome")).first().rows
        assertEquals(1795699800L, rows[0].silence)
        assertEquals("green", rows[0].outcome)
        assertEquals(null, rows[1].silence)
        val a = WaitAnswered(by = "silenzio assenso", at = 1795700400, reason = "silenzio assenso: nessuna risposta entro le 26/11 13:30")
        assertEquals(Scelta.State.SILENCE, Scelta.outcomeOf("silence", a, null).state)
        assertEquals("silence", Scelta.roundsOf(item("ok").copy(rounds = listOf(WaitRound("r1", 1795400000, "Bozza", state = "silence", by = "silenzio assenso", answeredAt = 1795700400)))).first().state)
    }
}
