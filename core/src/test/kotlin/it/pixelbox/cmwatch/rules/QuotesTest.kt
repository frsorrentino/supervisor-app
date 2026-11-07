package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/** La regola della citazione con i casi che legge anche web/src/lib/quotes.test.ts (app-citazione-trascrizione-07-11). */
class QuotesTest {
    private val file = Json.parseToJsonElement(javaClass.getResource("/quote-cases.json")!!.readText()).jsonObject

    @Test fun sameMaxAsTheWeb() = assertEquals(file["max"]!!.jsonPrimitive.int, Quotes.MAX)

    @Test fun sharedCases() {
        val cases = file["cases"]!!.jsonArray
        check(cases.isNotEmpty())
        cases.forEach { c ->
            val o = c.jsonObject
            val quotes = o["quotes"]!!.jsonArray.map { it.jsonPrimitive.content }
            assertEquals(o["name"]!!.jsonPrimitive.content, o["out"]!!.jsonPrimitive.content, Quotes.markdown(quotes, o["reply"]!!.jsonPrimitive.content))
        }
    }
}
