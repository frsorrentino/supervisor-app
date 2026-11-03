package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** «Da guardare» (contratto 1.106, app-da-guardare-02-11): le righe dai risultati del relay, con le fixture vere. */
class GuardareTest {
    private fun fixture(n: String) = File("../contract/$n").readText()
    private val state = ContractJson.decodeState(fixture("state-3-stale.json"))
    private val page = ContractJson.decodeResults(
        ContractJson.json.parseToJsonElement(fixture("cmd-result-sample.json")).let { e ->
            (e as kotlinx.serialization.json.JsonObject)["result"]!!.let { r -> (r as kotlinx.serialization.json.JsonArray) }
                .first { (it as kotlinx.serialization.json.JsonObject)["id"].toString().contains("0919") }
                .let { (it as kotlinx.serialization.json.JsonObject)["text"]!!.let { t -> (t as kotlinx.serialization.json.JsonPrimitive).content } }
        },
    )

    @Test fun theStateCarriesTheUnseenResults() {
        val r = state.results!!
        assertEquals(1, r.nUnseen)
        val l = Guardare.line(r.items.single())
        assertEquals(Guardare.Line("Docs tour", 3, "orbit-docs", Guardare.Kind.VIDEO, 37, 1789199800, true), l)
    }

    @Test fun thePageGroupsTheVersionsWithNotes() {
        val g = page.groups.single()
        assertEquals(3, g.versions); assertTrue(g.unseen)
        assertEquals(listOf(3, 2), g.items.map { it.version })
        val v2 = Guardare.older(g).single()
        assertEquals(ResultNote(1789197100, "The logo flickers here", 12.4), v2.notes.single())
        assertEquals(true, v2.replaced)
        assertEquals(1789197000L, v2.seenAt)
        assertFalse(Guardare.line(v2).unseen)
    }

    @Test fun oneVersionShowsNoNumber() {
        assertNull(Guardare.line(ResultItem("x", title = "Riga Esito", kind = "mockup", version = 1, versions = 1)).version)
    }

    @Test fun aTitleFallsBackToTheFileName() {
        assertEquals("tour.mp4", Guardare.line(ResultItem("x", title = " ", path = "/a/b/tour.mp4")).title)
    }

    @Test fun thumbnailsOnlyForPictures() {
        val png = TranscriptFile("/m/a.png", "image/png", 10)
        assertEquals(png, Guardare.thumb(ResultItem("m", kind = "mockup", path = "/m/a.html", mime = "text/html", files = listOf(png))))
        assertEquals("/i/b.jpg", Guardare.thumb(ResultItem("i", kind = "image", path = "/i/b.jpg", mime = "image/jpeg"))?.path)
        assertNull(Guardare.thumb(ResultItem("v", kind = "video", path = "/v.mp4", mime = "video/mp4")))
        assertNull(Guardare.thumb(ResultItem("m", kind = "mockup", path = "/m/a.html", mime = "text/html")))
    }

    @Test fun theSessionRowListsItsWorks() {
        val l = Guardare.ofSession(page, "orbit-docs")
        assertEquals(1, l.groups.size); assertEquals(3, l.latest?.version)
        assertTrue(Guardare.ofSession(page, "atlas-shop").groups.isEmpty())
        assertNull(Guardare.ofSession(null, "x").latest)
    }

    @Test fun positionsReadLikeTheMockup() {
        assertEquals("36,7 s", Guardare.pos(36.7)); assertEquals("6 s", Guardare.pos(6.0)); assertEquals("1:05", Guardare.pos(65.2))
    }

    @Test fun aNoteIsNeverEmptyNorTooLong() {
        assertNull(Guardare.note("  ")); assertEquals(2000, Guardare.note("x".repeat(2500))!!.length)
    }
}
