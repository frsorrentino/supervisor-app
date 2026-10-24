package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.rules.Elicit.Problem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class ElicitTest {
    private val st = ContractJson.decodeState(Fixtures.stateIdle)
    private val form = st.elicitations[0]
    private fun f(key: String) = form.fields!!.single { it.key == key }

    @Test fun openUntilExpiry() {
        assertEquals(2, Elicit.open(st, form.askedAt).size)
        assertEquals(listOf("e-9a0b1c2d3e4f"), Elicit.open(st, form.expiresAt).map { it.id })
        assertTrue(Elicit.open(null, 0).isEmpty())
    }

    @Test fun problemsPerKind() {
        assertEquals(Problem.Required, Elicit.problem(f("project"), null))
        assertEquals(Problem.NotOption, Elicit.problem(f("project"), "BLOG"))
        assertNull(Elicit.problem(f("project"), "SHOP"))
        assertNull(Elicit.problem(f("urgent"), null))
        assertEquals(Problem.Length(3, 80), Elicit.problem(f("title"), "ab"))
        assertNull(Elicit.problem(f("title"), "Checkout"))
        assertEquals(Problem.NotNumber, Elicit.problem(f("estimate"), "tre"))
        assertEquals(Problem.NotWhole, Elicit.problem(f("estimate"), "2,5"))
        assertEquals(Problem.Range(1.0, 13.0), Elicit.problem(f("estimate"), "21"))
        assertNull(Elicit.problem(f("estimate"), ""))
        assertEquals("false", Elicit.initial(f("urgent"))); assertNull(Elicit.initial(f("project")))
    }

    /** Il modulo compilato come nel comando di esempio …0830: stesso `content`, numeri come numeri. */
    @Test fun contentAsTheSampleCommand() {
        val values = mapOf("project" to "SHOP", "urgent" to "true", "title" to "Checkout total ignores the gift card", "estimate" to "3")
        assertTrue(Elicit.ready(form, values))
        assertFalse(Elicit.ready(form, values - "title"))
        val sample = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("cmd").jsonArray
            .map { it.jsonObject }.single { it["id"].toString().contains("0830") }.getValue("content")
        assertEquals(sample, Elicit.content(form, values))
        assertEquals(setOf("project", "title"), Elicit.content(form, mapOf("project" to "SHOP", "title" to "Abc", "estimate" to " ")).keys)
    }
}
