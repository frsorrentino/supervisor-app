package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Question
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.rules.TieniPremuto.Place
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il tasto «tieni premuto» (app-tieni-premuto-28-11): i tempi dei posti, il pieno lineare, lasciato prima o fatto, la scossa e quale ok
 * pubblica. I casi delle carte stanno in tieni-premuto-cases.json, letto anche da web/src/lib/tieniPremuto.test.ts.
 */
class TieniPremutoTest {
    private val cases = Json.parseToJsonElement(javaClass.getResource("/tieni-premuto-cases.json")!!.readText()).jsonObject

    @Test fun `ogni posto ha il suo tempo, i posti a un tocco nessuno`() {
        assertEquals(1200L, TieniPremuto.holdMs(Place.CHIUDI))
        assertEquals(1000L, TieniPremuto.holdMs(Place.RIPARTI))
        assertEquals(1500L, TieniPremuto.holdMs(Place.PUBBLICA))
        assertEquals(0L, TieniPremuto.holdMs(Place.APPROVA))
        assertEquals(0L, TieniPremuto.holdMs(Place.VA))
    }

    @Test fun `si tiene premuto solo nei tre posti di Franz, ogni altro ok e ogni Va sono un tocco`() {
        assertEquals(
            listOf(TieniPremuto.Mode.HOLD, TieniPremuto.Mode.HOLD, TieniPremuto.Mode.HOLD, TieniPremuto.Mode.TAP, TieniPremuto.Mode.TAP),
            listOf(Place.CHIUDI, Place.RIPARTI, Place.PUBBLICA, Place.APPROVA, Place.VA).map(TieniPremuto::modeOf),
        )
    }

    @Test fun `il pieno cresce lineare col tempo tenuto`() {
        assertEquals(0f, TieniPremuto.progress(0, 1200), 0f)
        assertEquals(0.25f, TieniPremuto.progress(300, 1200), 1e-6f)
        assertEquals(0.5f, TieniPremuto.progress(600, 1200), 1e-6f)
        assertEquals(1f, TieniPremuto.progress(1800, 1200), 0f)
        assertEquals(0f, TieniPremuto.progress(-40, 1200), 0f)
        assertEquals(1f, TieniPremuto.progress(0, 0), 0f)
    }

    @Test fun `lasciato prima non parte niente, al tempo pieno parte`() {
        assertEquals(TieniPremuto.Release.EARLY, TieniPremuto.release(1199, 1200))
        assertEquals(TieniPremuto.Release.COMPLETE, TieniPremuto.release(1200, 1200))
        assertEquals(TieniPremuto.Release.EARLY, TieniPremuto.release(1400, 1500))
        assertEquals(TieniPremuto.Release.COMPLETE, TieniPremuto.release(1000, 1000))
    }

    @Test fun `la scossa del no cresce col pieno fino a 260 e sparisce col moto ridotto`() {
        assertEquals(78f, TieniPremuto.kick(0f), 1e-3f)
        assertEquals(182f, TieniPremuto.kick(0.2f), 1e-3f)
        assertEquals(260f, TieniPremuto.kick(0.35f), 1e-3f)
        assertEquals(260f, TieniPremuto.kick(0.9f), 1e-3f)
        assertEquals(78f, TieniPremuto.kick(-0.1f), 1e-3f)
        assertEquals(0f, TieniPremuto.kick(0.5f, reduced = true), 0f)
    }

    @Test fun `col moto ridotto le molle arrivano senza rimbalzo`() {
        assertEquals(14f, TieniPremuto.damping(TieniPremuto.RELEASE_K, TieniPremuto.RELEASE_C, reduced = false), 0f)
        assertEquals(2f * kotlin.math.sqrt(260f), TieniPremuto.damping(TieniPremuto.RELEASE_K, TieniPremuto.RELEASE_C, reduced = true), 1e-4f)
        assertEquals(610L, TieniPremuto.FIRE_MS)
        assertEquals(800L, TieniPremuto.LEAVE_MS)
    }

    @Test fun `quali testi nominano un'uscita`() = cases["publishes"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val text = o["text"]!!.jsonPrimitive.content
        assertEquals(text, o["out"]!!.jsonPrimitive.boolean, TieniPremuto.publishes(listOf(text)))
    }

    @Test fun `il tasto da tenere premuto e il posto di ogni tasto delle carte`() = cases["cards"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val name = o["name"]!!.jsonPrimitive.content
        val w = ContractJson.json.decodeFromJsonElement(WaitItem.serializer(), o["item"]!!)
        val hold = o["hold"]!!.let { h -> if (h is JsonNull) null else h.jsonPrimitive.int }
        assertEquals(name, hold, TieniPremuto.holdIndex(w))
        val places = o["places"]!!.jsonArray.map { p -> if (p is JsonNull) null else Place.valueOf((p as JsonPrimitive).content.uppercase()) }
        assertEquals(name, places, w.actions.orEmpty().indices.map { i -> TieniPremuto.placeOf(w, i) })
    }

    @Test fun `l'opzione di una domanda che pubblica si tiene premuta`() = cases["questions"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val q = ContractJson.json.decodeFromJsonElement(Question.serializer(), o["question"]!!)
        val want = o["out"]!!.let { x -> if (x is JsonNull) null else x.jsonPrimitive.int }
        assertEquals(o["name"]!!.jsonPrimitive.content, want, TieniPremuto.holdOption(q))
    }

    @Test fun `un ok dello stato si tiene premuto se pubblica`() = cases["approvals"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val a = ContractJson.json.decodeFromJsonElement(Approval.serializer(), o["approval"]!!)
        assertEquals(a.title, o["out"]!!.jsonPrimitive.boolean, TieniPremuto.holds(a))
    }

    @Test fun `un no o un rimanda non diventano mai il tasto da tenere premuto`() {
        assertTrue(TieniPremuto.refuses("no · aspetta l'ok del cliente"))
        assertTrue(TieniPremuto.refuses("2 · Non ora"))
        assertTrue(TieniPremuto.refuses("Tomorrow morning"))
        assertTrue(TieniPremuto.refuses("domattina"))
        assertFalse(TieniPremuto.refuses("sì · Deploy 2.8.0 in produzione adesso"))
        assertFalse(TieniPremuto.refuses("Release now"))
        assertFalse(TieniPremuto.refuses("Approva"))
    }

    @Test fun `una carta senza tasti, dall'archivio, non ha posti`() {
        val w = WaitItem(id = "w", label = "decide", title = "Pubblica la 2.4", actions = null)
        assertEquals(null, TieniPremuto.holdIndex(w))
        assertEquals(null, TieniPremuto.placeOf(w, 0))
        assertTrue(TieniPremuto.publishes(listOf(null, "Pubblica la 2.4")))
        assertFalse(TieniPremuto.publishes(listOf(null, null)))
    }
}
