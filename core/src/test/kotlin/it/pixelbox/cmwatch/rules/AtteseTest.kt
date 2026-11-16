package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * app-riall-1-dati-16-11 (contratto 1.151): la lista unica di ciò che aspetta la persona. Gli stessi casi di attese.test.ts, dalla
 * fixture del relay: i conti delle voci sono quelli di `state.tocca` e di `sessions[].tocca`, così il numero sono le righe.
 */
class AtteseTest {
    private val f = Json.parseToJsonElement(File("../contract/attese-rd-sample.json").readText()).jsonObject
    private val items = ContractJson.decodeWaits(f["attese"].toString()).items
    private val st = f["state"]!!.jsonObject

    @Test fun theCountsOfAllItemsAreTheState() {
        val t = st["tocca"]!!.jsonObject
        assertEquals(Attese.Counts(t["n"]!!.jsonPrimitive.int, t["decidi"]!!.jsonPrimitive.int, t["prove"]!!.jsonPrimitive.int, t["rispondi"]!!.jsonPrimitive.int), Attese.counts(items))
        assertEquals(items.size, st["attese"]!!.jsonObject["n"]!!.jsonPrimitive.int)
        assertEquals(ContractJson.decodeWaits(f["attese"].toString()).rev, st["attese"]!!.jsonObject["rev"]!!.jsonPrimitive.content)
    }

    @Test fun eachSessionCountsItsTocca() = st["sessions"]!!.jsonArray.forEach { s ->
        val o = s.jsonObject
        val name = o["name"]!!.jsonPrimitive.content
        val t = o["tocca"]!!.jsonObject
        val c = Attese.counts(items, name, all = false)
        assertEquals(name, listOf(t["prove"]!!.jsonPrimitive.int, t["decidi"]!!.jsonPrimitive.int, t["rispondi"]!!.jsonPrimitive.int), listOf(c.prove, c.decidi, c.rispondi))
        assertEquals(name, t["decidi"]!!.jsonPrimitive.int to t["prove"]!!.jsonPrimitive.int, Attese.perSession(items)[name])
    }

    @Test fun decideAndVerifyLeaveNothingOut() =
        assertEquals(items.size, items.count(Attese::isDecide) + items.count(Attese::isVerify) + items.count(Attese::isReply))

    @Test fun anAnsweredOrClosedItemLeavesAndTheCountsDrop() {
        val one = items[1].id
        val v = Attese.visible(items, setOf(one))
        assertFalse(v.any { w -> w.id == one }); assertEquals(items.size - 1, Attese.counts(v).n)
        assertTrue(Attese.visible(listOf(items[0].copy(state = "approved")), emptySet()).isEmpty())
        val withClosed = ContractJson.decodeState(File("../contract/state-2-idle.json").readText()).let { s -> s.copy(attese = it.pixelbox.cmwatch.contract.AtteseState("x", 0, listOf(it.pixelbox.cmwatch.contract.AtteseClosed("w1", 1)))) }
        assertEquals(listOf("w1"), Attese.closedIds(withClosed))
    }
}
