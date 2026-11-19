package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptEntry
import it.pixelbox.cmwatch.contract.TranscriptFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Le bozze citate in una risposta (app-bozze-link-19-11), coi casi che legge anche web/src/lib/bozzeLinks.test.ts. */
class BozzeLinksTest {
    private val cases = Json.parseToJsonElement(javaClass.getResource("/bozze-links-cases.json")!!.readText()).jsonArray

    @Test fun sharedCases() = cases.forEach { c ->
        val o = c.jsonObject
        val text = o["text"]!!.jsonPrimitive.content
        val got = BozzeLinks.find(text, o["files"]!!.jsonArray.map { it.jsonPrimitive.content }).map { text.substring(it.range) to it.file }
        val want = o["links"]!!.jsonArray.map { l -> l.jsonObject["text"]!!.jsonPrimitive.content to l.jsonObject["file"]!!.jsonPrimitive.int }
        assertEquals(o["name"]!!.jsonPrimitive.content, want, got)
    }

    @Test fun theLetterComesFromTheFileName() {
        assertEquals('A', BozzeLinks.letterOf("docs/mockups/scheda-a.png"))
        assertEquals('B', BozzeLinks.letterOf("C:/x/bozza_B.html"))
        assertEquals('C', BozzeLinks.letterOf("c.png"))
        assertNull(BozzeLinks.letterOf("schermo.png"))
    }

    @Test fun theTurnFilesStopAtThePersonsMessage() {
        fun e(id: String, role: String, vararg p: String) = TranscriptEntry(id = id, role = role, files = p.map { TranscriptFile(it) }.takeIf { it.isNotEmpty() })
        val l = listOf(e("1", "assistant", "old-a.png"), e("2", "user"), e("3", "tool", "scheda-a.png", "note.md"), e("4", "tool", "scheda-b.html"), e("5", "assistant"))
        assertEquals(listOf("scheda-a.png" to "3", "scheda-b.html" to "4"), BozzeLinks.turnFiles(l, 4).map { it.first.path to it.second })
        assertEquals(emptyList<Any>(), BozzeLinks.turnFiles(l, 1))
    }
}
