package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

/** La Home in riga (app-home-riga-c1-13-11): gli stessi casi di web/src/lib/homeRow.test.ts, dallo stesso file. */
class HomeRowTest {
    private val cases = Json.parseToJsonElement(javaClass.getResource("/home-row-cases.json")!!.readText()).jsonObject
    private fun str(o: JsonObject, k: String): String? = (o[k] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun num(o: JsonObject, k: String): Long? = o[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.long

    @Test fun voices() = cases["voices"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val rows = o["rows"]!!.jsonArray.map { r -> r.jsonObject.let { HomeRow.In(str(it, "session")!!, str(it, "group")!!, str(it, "text"), num(it, "at"), str(it, "key")) } }
        val want = o["out"]!!.jsonArray.map { v ->
            v.jsonObject.let { HomeRow.Voice(if (str(it, "kind") == "wait") HomeRow.Kind.WAIT else HomeRow.Kind.NEWS, str(it, "session")!!, str(it, "text"), num(it, "at"), str(it, "key")!!) }
        }
        val got = HomeRow.voices(rows, o["shown"]!!.jsonArray.map { it.jsonPrimitive.content }, num(o, "since"), o["opened"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())
        assertEquals(str(o, "name"), want, got)
    }

    @Test fun emptyKeepsTheGrip() {
        assertEquals(28, HomeRow.rowHeight(emptyList()))
        assertEquals(64, HomeRow.rowHeight(listOf(HomeRow.Voice(HomeRow.Kind.WAIT, "a", null, null, "wait:a"))))
    }
}
