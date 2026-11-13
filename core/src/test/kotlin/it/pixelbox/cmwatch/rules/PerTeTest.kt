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
        val want = (o["out"] as? JsonObject)?.let { w -> PerTe.Main(w["index"]!!.jsonPrimitive.int, str(w, "label")!!, w["direct"]!!.jsonPrimitive.boolean) }
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
}
