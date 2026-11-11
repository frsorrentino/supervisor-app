package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.ResultItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Le anteprime delle consegne (app-anteprime-verifica-11-11, contratto 1.136), dagli esempi del relay …1360-1363. */
class ThumbsTest {
    private val results = (ContractJson.json.parseToJsonElement(File("../contract/cmd-result-sample.json").readText()) as JsonObject)["result"] as JsonArray
    private fun sample(end: String) = results.map { it as JsonObject }.single { it["id"]!!.jsonPrimitive.content.endsWith(end) }
    private fun read(end: String) = sample(end).let { Thumbs.read(it["ok"]!!.jsonPrimitive.boolean, it["text"]!!.jsonPrimitive.content) }

    @Test fun theRelaySamplesRead() {
        val img = read("1360")!!
        assertEquals("image", img.kind); assertEquals("image/jpeg", img.mime); assertTrue(img.width!! <= 240)
        val shown = Thumbs.shown(img)
        assertTrue(shown is Thumbs.Shown.Image)
        val bytes = (shown as Thumbs.Shown.Image).bytes
        assertEquals(0xFF.toByte(), bytes[0]); assertEquals(0xD8.toByte(), bytes[1])
        val txt = read("1361")!!
        assertEquals("# Verbale della riunione", (Thumbs.shown(txt) as Thumbs.Shown.Lines).text.lines().first())
        val none = read("1362")!!
        assertEquals("type", none.why); assertEquals(Thumbs.Shown.Icon, Thumbs.shown(none))
        assertNull(read("1363"))
        assertEquals(Thumbs.Shown.Icon, Thumbs.shown(null))
    }

    @Test fun onlyFilesWithoutAnImageOfTheirOwnAsk() {
        assertTrue(Thumbs.wants(ResultItem("a", kind = "document", path = "/x/PREV.pdf", mime = "application/pdf")))
        assertTrue(Thumbs.wants(ResultItem("b", kind = "other", path = "/x/verbale.md")))
        assertFalse(Thumbs.wants(ResultItem("c", kind = "image", path = "/x/a.png", mime = "image/png")))
        assertFalse(Thumbs.wants(ResultItem("d", kind = "video", path = "/x/a.mp4", mime = "video/mp4")))
    }

    @Test fun aNewVersionIsANewPreview() {
        val i = ResultItem("a", kind = "document", path = "/x/PREV.pdf", version = 1, at = 10)
        assertFalse(Thumbs.key(i) == Thumbs.key(i.copy(version = 2, at = 20)))
        assertFalse(Thumbs.key(i) == Thumbs.key(i.copy(replaced = true)))
    }
}
