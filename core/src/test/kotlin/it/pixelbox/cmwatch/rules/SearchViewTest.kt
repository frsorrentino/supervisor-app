package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneOffset

/** La pagina Cerca (app-cerca-pagina-03-11) sul search «tuesday» della fixture: 3 voci di orbit-docs chiusa, 2 di field-notes viva. */
class SearchViewTest {
    private val hits = run {
        val root = ContractJson.json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val id = root["cmd"]!!.jsonArray.first { it.jsonObject["op"]!!.jsonPrimitive.content == "search" }.jsonObject["id"]!!.jsonPrimitive.content
        val text = root["result"]!!.jsonArray.first { it.jsonObject["id"]!!.jsonPrimitive.content == id }.jsonObject["text"]!!.jsonPrimitive.content
        ChatSearch.withConversations(emptyList(), ContractJson.json.decodeFromString(it.pixelbox.cmwatch.contract.SearchPage.serializer(), text))
    }

    @Test fun groupsBySessionNewestFirst() {
        val g = SearchView.groups(hits, SearchView.Filter(), 0)
        assertEquals(listOf("orbit-docs", "field-notes"), g.map { it.session })
        assertEquals(listOf(false, true), g.map { it.live })
        assertEquals(listOf(3, 2), g.map { it.hits.size })
        assertEquals("c5.0", g[0].hits[0].entry)
        assertTrue(g.all { x -> x.hits.zipWithNext().all { (a, b) -> a.at >= b.at } })
    }

    @Test fun filtersSessionDayAndClosed() {
        assertEquals(listOf("field-notes"), SearchView.groups(hits, SearchView.Filter(closed = false), 0).map { it.session })
        assertEquals(listOf(2), SearchView.groups(hits, SearchView.Filter(session = "field-notes"), 0).map { it.hits.size })
        assertEquals(listOf("orbit-docs"), SearchView.groups(hits, SearchView.Filter(today = true), 1789286400).map { it.session })
        assertTrue(SearchView.groups(hits, SearchView.Filter(today = true, closed = false), 1789286400).isEmpty())
    }

    @Test fun recentsMoveUpAndStaySix() {
        var r = listOf(SearchView.Recent("deploy 2.8"), SearchView.Recent("Tuesday", "orbit-docs"))
        r = SearchView.remember(r, SearchView.Recent("  tuesday ", null))
        assertEquals(listOf("tuesday", "deploy 2.8"), r.map { it.q })
        (1..10).forEach { r = SearchView.remember(r, SearchView.Recent("q$it")) }
        assertEquals(SearchView.RECENT_MAX, r.size); assertEquals("q10", r[0].q)
        assertSame(r, SearchView.remember(r, SearchView.Recent("   ")))
    }

    @Test fun whenLikeTheMockup() {
        val z = ZoneOffset.UTC; val now = 1789286460L   // 13/09/2026 08:01 UTC, domenica
        assertEquals(SearchView.When.Time("08:01"), SearchView.whenOf(now, now, z))
        assertEquals(SearchView.When.Yesterday, SearchView.whenOf(now - 86_400, now, z))
        assertEquals(SearchView.When.Weekday(4), SearchView.whenOf(now - 3 * 86_400, now, z))
        assertEquals(SearchView.When.Date("06/09"), SearchView.whenOf(now - 7 * 86_400, now, z))
        assertEquals(1789257600L, SearchView.dayStart(now, z))
    }
}
