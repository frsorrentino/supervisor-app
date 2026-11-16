package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** app-riquadri-chiusi-16-11: i riquadri della sessione chiusi come pillole; gli stessi casi di riquadri.test.ts. */
class RiquadriTest {
    private val file = Json.parseToJsonElement(javaClass.getResource("/riquadri-cases.json")!!.readText()).jsonObject
    private fun kind(k: String) = Riquadri.Kind.entries.single { it.key == k }
    private fun state(e: kotlinx.serialization.json.JsonElement) = Riquadri.decode(e.toString())!!
    private fun waiting(e: kotlinx.serialization.json.JsonElement?) = e!!.jsonArray.map { it.jsonPrimitive.content }

    @Test fun openOrClosed() = file["cases"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val name = o["name"]!!.jsonPrimitive.content
        o["open"]!!.jsonObject.forEach { (k, v) ->
            assertEquals("$name · $k", v.jsonPrimitive.boolean, Riquadri.isOpen(state(o["st"]!!), kind(k), waiting(o["waiting"])))
        }
    }

    @Test fun toggles() = file["toggles"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        assertEquals(o["name"]!!.jsonPrimitive.content, state(o["out"]!!), Riquadri.toggle(state(o["st"]!!), kind(o["kind"]!!.jsonPrimitive.content), waiting(o["waiting"])))
    }

    @Test fun anUnreadableStateIsNoState() {
        assertNull(Riquadri.decode(null)); assertNull(Riquadri.decode("x")); assertNull(Riquadri.decode("[1]"))
        val st = Riquadri.State(mapOf("perte" to false), listOf("w1"))
        assertEquals(st, Riquadri.decode(Riquadri.encode(st)))
    }
}
