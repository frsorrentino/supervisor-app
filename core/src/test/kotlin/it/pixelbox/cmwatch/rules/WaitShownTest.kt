package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.File

/** app-attesa-caricamento-17-11: la scheda di una voce parte da quello che l'app sa già. Stessi casi di waitShown.test.ts, sulla fixture 1.151. */
class WaitShownTest {
    private val list = ContractJson.decodeWaits(kotlinx.serialization.json.Json.parseToJsonElement(File("../contract/attese-rd-sample.json").readText())
        .let { e -> (e as kotlinx.serialization.json.JsonObject).getValue("attese").toString() }).items
    private val id = list.first().id

    @Test fun knownItemShowsAtOnce() = assertSame(list.first(), TiAspetta.waitShown(id, null, list, gone = false))
    @Test fun thePcAnswerTakesItsPlace() { val full = list.first().copy(title = "dal PC"); assertSame(full, TiAspetta.waitShown(id, full, list, gone = false)) }
    @Test fun closedOrUnknownIsNull() { assertNull(TiAspetta.waitShown(id, null, list, gone = true)); assertNull(TiAspetta.waitShown("w9", null, list, gone = false)) }
    @Test fun theMark() {
        assertEquals(TiAspetta.Load.DETAILS, TiAspetta.waitLoad(loaded = false, failed = false, gone = false, slow = false))
        assertEquals(TiAspetta.Load.SLOW, TiAspetta.waitLoad(loaded = false, failed = false, gone = false, slow = true))
        assertEquals(TiAspetta.Load.FAILED, TiAspetta.waitLoad(loaded = false, failed = true, gone = false, slow = true))
        assertNull(TiAspetta.waitLoad(loaded = true, failed = false, gone = false, slow = false))
        assertNull(TiAspetta.waitLoad(loaded = false, failed = false, gone = true, slow = false))
    }
}
