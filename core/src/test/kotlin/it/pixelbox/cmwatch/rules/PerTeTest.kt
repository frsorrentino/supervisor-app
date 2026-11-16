package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ResultGroup
import it.pixelbox.cmwatch.contract.ResultItem
import it.pixelbox.cmwatch.contract.WaitAction
import it.pixelbox.cmwatch.contract.WaitItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Il formato unico (app-formato-unico-12-11): gli stessi casi di web/src/lib/perTe.test.ts, dallo stesso file. */
class PerTeTest {
    private val cases = Json.parseToJsonElement(javaClass.getResource("/per-te-cases.json")!!.readText()).jsonObject
    private fun str(o: JsonObject, k: String): String? = (o[k] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun action(o: JsonObject) = WaitAction(
        label = str(o, "label")!!, style = str(o, "style")!!, cmd = JsonObject(emptyMap()), textField = str(o, "text_field"),
        recommended = (o["recommended"] as? JsonPrimitive)?.boolean == true,
    )

    @Test fun main() = cases["main"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val got = PerTe.mainOf(o["actions"]!!.jsonArray.map { action(it.jsonObject) })
        val want = (o["out"] as? JsonObject)?.let { w -> PerTe.Main(w["index"]!!.jsonPrimitive.int, str(w, "label")!!, w["direct"]!!.jsonPrimitive.boolean, w["recommended"]!!.jsonPrimitive.boolean) }
        assertEquals(str(o, "name"), want, got)
    }

    @Test fun noActions() = assertNull(PerTe.mainOf(null))

    @Test fun voci() = cases["voci"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val waits = o["waits"]!!.jsonArray.map { it.jsonObject }.map { w ->
            WaitItem(id = str(w, "id")!!, label = str(w, "label")!!, session = str(w, "session"), title = str(w, "title"), text = str(w, "text").orEmpty(),
                description = str(w, "description"), at = w["at"]!!.jsonPrimitive.long, state = str(w, "state"))
        }
        val results = o["results"]!!.jsonArray.map { it.jsonObject }.map { r ->
            val at = r["at"]!!.jsonPrimitive.long
            val unseen = r["unseen"]!!.jsonPrimitive.boolean
            ResultGroup(title = str(r, "title")!!, session = str(r, "session"), kind = "document", at = at, unseen = unseen,
                items = listOf(ResultItem(id = str(r, "id")!!, session = str(r, "session"), title = str(r, "title")!!, kind = "document", path = "out/x.md", at = at, seenAt = if (unseen) null else at)))
        }
        val got = PerTe.voci(waits, results, str(o, "session")!!).map { v -> listOf(v.key, v.group.name.lowercase(), v.title, v.context) }
        val want = o["out"]!!.jsonArray.map { it.jsonObject }.map { w -> listOf(str(w, "key"), str(w, "group"), str(w, "title"), if (w["context"] is JsonNull) null else str(w, "context")) }
        assertEquals(str(o, "name"), want, got)
    }

    @Test fun waitAndResult() {
        val w = WaitItem(id = "w1", label = "reply", session = "a", text = "Sì?", at = 1, actions = listOf(WaitAction("sì", "option", JsonObject(emptyMap()))))
        val g = ResultGroup(title = "Rapporto", session = "a", kind = "document", at = 2, unseen = true, items = listOf(ResultItem(id = "r1", session = "a", title = "Rapporto", kind = "document", path = "r.md", at = 2)))
        val got = PerTe.voci(listOf(w), listOf(g), "a")
        assertEquals(PerTe.Main(0, "1 · sì", true), got[0].main)
        assertEquals("w1", got[0].wait?.id)
        assertEquals(PerTe.Result("r1", Guardare.Kind.DOCUMENT, null), got[1].result)
    }

    @Test fun labels() = assertEquals(
        listOf(PerTe.Group.ASPETTA, PerTe.Group.DECIDI, PerTe.Group.DECIDI, PerTe.Group.PROVA, PerTe.Group.DECIDI),
        listOf("reply", "decide", "choose", "verify", "altro").map(PerTe::groupOf),
    )

    /** app-riall-2-viste-16-11 (direzione A): Dalla master per sessione; gli stessi casi di perTe.test.ts, dalla fixture del relay. */
    private val rd = Json.parseToJsonElement(java.io.File("../contract/attese-rd-sample.json").readText()).jsonObject
    private val rdItems = it.pixelbox.cmwatch.contract.ContractJson.decodeWaits(rd["attese"].toString()).items

    @Test fun groupsBySessionOldestFirstLooseLast() {
        val g = PerTe.groupsOf(rdItems, emptyList())
        assertEquals(listOf("win:alfa", "delta", null), g.map { x -> x.session })
        assertEquals(rdItems.size, g.sumOf { x -> x.voci.size })
        rd["state"]!!.jsonObject["sessions"]!!.jsonArray.forEach { e ->
            val o = e.jsonObject
            val t = o["tocca"]!!.jsonObject
            val name = o["name"]!!.jsonPrimitive.content
            assertEquals(name, t["prove"]!!.jsonPrimitive.int + t["decidi"]!!.jsonPrimitive.int + t["rispondi"]!!.jsonPrimitive.int, g.first { x -> x.session == name }.voci.size)
        }
        assertEquals(PerTe.voci(rdItems, emptyList(), "win:alfa"), g[0].voci)
    }

    @Test fun resultsJoinTheirSessionAndAnsweredLeave() {
        val res = listOf(group("zeta", true, "r1", "Rapporto", 1), group("delta", false, "r2", "Visto", 1))
        val g = PerTe.groupsOf(rdItems, res, setOf("w17f9a258a0", "wdd48019c59"))
        assertEquals(listOf("zeta", "delta", null), g.map { x -> x.session })
        assertEquals(listOf("r1" to PerTe.Group.VERIFICA), g[0].voci.map { v -> v.key to v.group })
        assertEquals(emptyList<PerTe.Gruppo>(), PerTe.groupsOf(emptyList(), emptyList()))
    }

    @Test fun theAdviceOnTheRow() {
        val ok = PerTe.voceOf(rdItems.first { w -> w.id == "w17f9a258a0" })
        assertEquals(PerTe.Advice(true, null), ok.advice)
        assertEquals(PerTe.Main(0, "1 · Approva", true, true), ok.main)
        assertNull(PerTe.voceOf(rdItems.first { w -> w.id == "wdd48019c59" }).advice)
        val pick = PerTe.mainOf(listOf(act("no", "option"), act("sì", "option", true)))
        fun adv(a: it.pixelbox.cmwatch.contract.WaitAdvice?, m: PerTe.Main?) = PerTe.adviceOf(rdItems[0].copy(advice = a), m)
        assertEquals(PerTe.Advice(null, "2 · sì"), adv(it.pixelbox.cmwatch.contract.WaitAdvice("già provato"), pick))
        assertEquals(PerTe.Advice(false, null), adv(it.pixelbox.cmwatch.contract.WaitAdvice("x", approve = false), pick))
        assertEquals(PerTe.Advice(null, "Domani mattina"), adv(it.pixelbox.cmwatch.contract.WaitAdvice("x", option = "  Domani   mattina "), null))
        assertNull(adv(it.pixelbox.cmwatch.contract.WaitAdvice("solo il motivo"), null))
        assertNull(adv(null, pick))
    }
    private fun act(label: String, style: String, rec: Boolean = false) = WaitAction(label = label, style = style, cmd = JsonObject(emptyMap()), recommended = rec)
    private fun group(session: String, unseen: Boolean, id: String, title: String, at: Long) = ResultGroup(title = title, session = session, kind = "document", at = at, unseen = unseen,
        items = listOf(ResultItem(id = id, session = session, title = title, kind = "document", path = "out/x.md", at = at, seenAt = if (unseen) null else at)))
}
