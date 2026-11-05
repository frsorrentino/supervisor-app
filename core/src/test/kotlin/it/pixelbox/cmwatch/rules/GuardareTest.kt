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

    // app-risultati-x-05-11: chiusa con la X resta chiusa finché non arriva un risultato più recente.
    @Test fun theClosedRowComesBackWithTheNextResult() {
        val l = Guardare.ofSession(page, "orbit-docs")
        val key = Guardare.lastKey(l)!!
        assertTrue(Guardare.shown(l, null)); assertFalse(Guardare.shown(l, key))
        val g0 = l.groups.first()
        val next = Guardare.SessionLine(l.groups + g0.copy(at = g0.at + 60, items = g0.items.map { it.copy(id = it.id + "-n", at = it.at + 60) }), l.latest)
        assertTrue(Guardare.shown(next, key))
        assertFalse(Guardare.shown(Guardare.ofSession(page, "atlas-shop"), null))
    }

    @Test fun onlyAnUnseenWorkIsThePreviewOnTopOfTheChat() {
        // app-risultati-visti-03-11: tutto visto, nessuna anteprima; con un visto più recente, l'anteprima è il non visto.
        val seen = page.copy(groups = page.groups.map { it.copy(unseen = false) })
        assertNull(Guardare.ofSession(seen, "orbit-docs").latest); assertTrue(Guardare.ofSession(seen, "orbit-docs").groups.isNotEmpty())
        val g0 = page.groups.first { it.session == "orbit-docs" }
        val mixed = page.copy(groups = listOf(g0.copy(unseen = false, at = g0.at + 100, title = "vecchio visto"), g0.copy(unseen = true)))
        assertEquals(g0.items.first(), Guardare.ofSession(mixed, "orbit-docs").latest)
    }

    @Test fun positionsReadLikeTheMockup() {
        assertEquals("36,7 s", Guardare.pos(36.7)); assertEquals("6 s", Guardare.pos(6.0)); assertEquals("1:05", Guardare.pos(65.2))
    }

    @Test fun aNoteIsNeverEmptyNorTooLong() {
        assertNull(Guardare.note("  ")); assertEquals(2000, Guardare.note("x".repeat(2500))!!.length)
    }

    // app-risultati-visti-03-11 (Franz 03/11 15:26): in Risultati solo quello che non ha visto; Visto lo toglie; aperto altrove conta come visto.
    private fun grp(id: String, seen: Long?, old: String? = null) = ResultGroup(title = id, items = listOfNotNull(ResultItem(id, seenAt = seen), old?.let { ResultItem(it, seenAt = 5) }))

    @Test fun risultatiVisti_inElencoSoloINonVisti() {
        val s = Guardare.splitSeen(listOf(grp("a", null), grp("b", 9), grp("c", null, old = "c0")), setOf("c"))
        assertEquals(listOf("a"), s.fresh.map { it.title })
        assertEquals("visto qui o dal relay: in fondo", listOf("b", "c"), s.seen.map { it.title })
    }

    @Test fun risultatiVisti_unFileApertoAltroveLoSegna() {
        val png = TranscriptFile("/m/a.png", "image/png", 10)
        val items = listOf(ResultItem("m", path = "/m/a.html", files = listOf(png)), ResultItem("i", path = "/i/b.jpg"), ResultItem("v", path = "/m/a.png", seenAt = 3))
        assertEquals(listOf("m"), Guardare.seenByPath(items, "/m/a.png"))
        assertEquals(listOf("i"), Guardare.seenByPath(items, "/i/b.jpg"))
        assertTrue(Guardare.seenByPath(items, "/x").isEmpty())
    }
}
