package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

/** La frase accesa e l'età della scheda «Ti aspetta», coi casi che legge anche web/src/lib/tiAspetta.test.ts. */
class TiAspettaTest {
    private val file = Json.parseToJsonElement(javaClass.getResource("/ti-aspetta-cases.json")!!.readText()).jsonObject

    @Test fun marks() = file["marks"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val want = o["out"]!!.jsonArray.map { s -> s.jsonArray.let { TiAspetta.Seg(it[0].jsonPrimitive.content, it[1].jsonPrimitive.boolean) } }
        assertEquals(o["name"]!!.jsonPrimitive.content, want, TiAspetta.marks(o["text"]!!.jsonPrimitive.content, o["ask"]!!.jsonPrimitive.content))
    }

    @Test fun age() = file["age"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        assertEquals(o["out"]!!.jsonPrimitive.content, TiAspetta.age(o["s"]!!.jsonPrimitive.long))
    }
}
