package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * app-anteprime-webp-27-11: le anteprime che si muovono e la bozza HTML della loro immagine. Gli stessi casi di
 * web/src/lib/anteprimeAnimate.test.ts, in core/src/test/resources/anteprime-animate-cases.json.
 */
class AnteprimeAnimateTest {
    private val file = Json.parseToJsonElement(javaClass.getResource("/anteprime-animate-cases.json")!!.readText()).jsonObject
    private fun cases(k: String) = file.getValue(k).jsonArray.map { it.jsonObject }
    private fun str(o: JsonObject, k: String): String? = (o[k] as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
    private fun name(o: JsonObject) = o.getValue("name").jsonPrimitive.content
    private fun bool(o: JsonObject, k: String) = o.getValue(k).jsonPrimitive.boolean

    @Test fun `un'immagine che può muoversi, per tipo o per estensione`() = cases("capable").forEach { o ->
        assertEquals(name(o), bool(o, "out"), AnteprimeAnimate.capable(str(o, "mime"), str(o, "path")!!))
    }

    @Test fun `si muove davvero, dai byte del file`() = cases("moves").forEach { o ->
        assertEquals(name(o), bool(o, "out"), AnteprimeAnimate.moves(Base64.getDecoder().decode(str(o, "b64")!!)))
    }

    @Test fun `gira solo a schermo e senza animazioni ridotte`() = cases("plays").forEach { o ->
        assertEquals(name(o), bool(o, "out"), AnteprimeAnimate.plays(bool(o, "visible"), bool(o, "reduced")))
    }

    @Test fun `la bozza HTML dell'immagine, nello stesso elenco`() = cases("draft").forEach { o ->
        val files = o.getValue("files").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(name(o), str(o, "out"), AnteprimeAnimate.draftOf(str(o, "image")!!, str(o, "mime"), files))
    }

    @Test fun `l'anteprima animata della bozza, nello stesso elenco`() = cases("preview").forEach { o ->
        val files = (o.getValue("files") as JsonArray).map { f -> f.jsonObject.let { x -> str(x, "path")!! to str(x, "mime") } }
        assertEquals(name(o), str(o, "out"), AnteprimeAnimate.previewOf(str(o, "draft")!!, files))
    }

    // Il WebP della demo, fatto da entrata-home-360-web-28-11.webp della master: si muove; le schermate PNG della demo no.
    @Test fun `il WebP animato della demo si muove, i PNG no`() {
        val demo = File("../web/public/demo-files")
        assertTrue(AnteprimeAnimate.moves(File(demo, "entrata-home.webp").readBytes()))
        assertFalse(AnteprimeAnimate.moves(File(demo, "cart-a.png").readBytes()))
        assertEquals("${demo.path}/entrata-home.html", AnteprimeAnimate.draftOf("${demo.path}/entrata-home.webp", "image/webp", listOf("${demo.path}/entrata-home.html")))
    }
}
