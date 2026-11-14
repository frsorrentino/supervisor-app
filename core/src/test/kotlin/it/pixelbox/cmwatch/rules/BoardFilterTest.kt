package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

/** Le caselle del tabellone come filtri (app-tabellone-filtri-14-11): gli stessi casi di web/src/lib/kpi.test.ts, dallo stesso file. */
class BoardFilterTest {
    private val cases = Json.parseToJsonElement(javaClass.getResource("/board-filter-cases.json")!!.readText()).jsonObject
    private val zone = ZoneId.of("Europe/Rome")
    private fun state(k: String) = ContractJson.decodeState(cases[k]!!.toString())
    private fun filter(e: JsonElement?): Kpi.BoardFilter? = when (e?.takeIf { it !is JsonNull }?.jsonPrimitive?.content) { "aspetta" -> Kpi.BoardFilter.WAITING; "lavoro" -> Kpi.BoardFilter.BUSY; else -> null }
    private fun names(k: String) = cases[k]!!.jsonArray.map { it.jsonPrimitive.content }

    @Test fun theFilteredListIsTheOneTheBoardCounts() {
        val st = state("state")
        val h = Kpi.head(st, null, 1000, zone)
        assertEquals(names("aspetta"), st.sessions.filter { Kpi.boardMatch(it, Kpi.BoardFilter.WAITING) }.map { it.name })
        assertEquals(names("lavoro"), st.sessions.filter { Kpi.boardMatch(it, Kpi.BoardFilter.BUSY) }.map { it.name })
        assertEquals(listOf(names("aspetta").size, names("lavoro").size), listOf(h.waiting, h.busy))
    }

    @Test fun taps() {
        for ((list, st) in listOf("taps" to "state", "quietTaps" to "quiet")) {
            val h = Kpi.head(state(st), null, 1000, zone)
            cases[list]!!.jsonArray.forEach { c -> val o = c.jsonObject; assertEquals(o["name"].toString(), filter(o["out"]), Kpi.boardTap(filter(o["cur"]), filter(o["tap"])!!, h)) }
        }
    }

    @Test fun aFilterLeftWithNoSessionDropsByItself() {
        val h = Kpi.head(state("quiet"), null, 1000, zone)
        cases["quietShown"]!!.jsonArray.forEach { c -> val o = c.jsonObject; assertEquals(o["name"].toString(), filter(o["out"]), Kpi.boardShown(filter(o["cur"]), h)) }
    }
}
