package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.WaitAnswered
import it.pixelbox.cmwatch.contract.WaitFile
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.contract.WaitOption
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La scheda di scelta (app-scheda-scelta-12-11, contratto 1.137): gli stessi casi di web/src/lib/scelta.test.ts, dallo stesso file, e la
 * fixture del contratto (…1300 attese, …1301 attesa con un giro scartato, …1390 la scheda dall'archivio).
 */
class SceltaTest {
    private val cases = Json.parseToJsonElement(javaClass.getResource("/scelta-cases.json")!!.readText()).jsonObject
    private val fake = it.pixelbox.cmwatch.transport.FakeTransport(load = { java.io.File("../contract/$it.json").readText() })
    private fun open(): WaitItem = ContractJson.decodeWait(fake.sampleResult("attesa", "wd2f87e85e2").second)
    private fun archived(): WaitItem = ContractJson.decodeWait(fake.sampleResult("attesa", "w3a91c04e7b").second)
    private fun str(o: JsonObject, k: String): String? = (o[k] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun item(kind: String = "choice", options: List<WaitOption>? = null, files: List<WaitFile>? = null, state: String? = null, answered: WaitAnswered? = null) =
        WaitItem(id = "w1", type = "carta", kind = kind, text = "x", options = options, files = files, state = state, answered = answered)

    @Test fun letters() = cases["letters"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val want = o["out"]!!.jsonArray.map { p -> p.jsonArray.let { Scelta.Lettered(it[0].jsonPrimitive.content, it[1].jsonPrimitive.content) } }
        assertEquals(str(o, "name"), want, Scelta.lettersOf(o["labels"]!!.jsonArray.map { it.jsonPrimitive.content }))
    }

    @Test fun outcome() = cases["outcome"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val a = (o["answered"] as? JsonObject)?.let { x -> WaitAnswered(choice = str(x, "choice"), reason = str(x, "reason")) }
        val opts = o["options"]!!.jsonArray.mapIndexed { i, p -> p.jsonArray.let { WaitOption(i + 1, it[0].jsonPrimitive.content, it[1].jsonPrimitive.content) } }
        val r = Scelta.outcomeOf(str(o, "state"), a, opts)
        val w = o["out"]!!.jsonObject
        assertEquals(str(o, "name"), listOf(str(w, "state"), str(w, "letter"), str(w, "name"), str(w, "reason")), listOf(r.state.wire, r.letter, r.name, r.reason))
    }

    @Test fun files() = cases["files"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val fs = o["files"]!!.jsonArray.map { p -> p.jsonArray.let { WaitFile("/x/${it[0].jsonPrimitive.content}", it[0].jsonPrimitive.content, it[1].jsonPrimitive.content, kind = it[2].jsonPrimitive.content) } }
        val s = Scelta.splitFiles(fs)
        assertEquals(str(o, "name"), o["images"]!!.jsonArray.map { it.jsonPrimitive.content }, s.images.map { it.name })
        assertEquals(str(o, "name"), o["docs"]!!.jsonArray.map { it.jsonPrimitive.content }, s.docs.map { it.name })
    }

    /** Sul telefono la chiave di un file con la sessione è quella della sua scheda in chat, che non comincia col nome della sessione. */
    @Test fun keys() = cases["key"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val session = str(o, "session")
        val want = str(o, "out")!!.let { k -> if (session != null) k.removePrefix("$session\n") else k }
        val f = WaitFile(str(o, "path")!!, "x", size = (o["size"] as? JsonPrimitive)?.longOrNull, kind = "image", session = session)
        assertEquals(str(o, "name"), want, Scelta.fileKey(f))
    }

    @Test fun waitLinksLeaveTheText() = cases["strip"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        assertEquals(str(o, "name"), SessionLinks.Waits(str(o, "out")!!, o["ids"]!!.jsonArray.map { it.jsonPrimitive.content }), SessionLinks.stripWaits(str(o, "text")!!))
    }

    @Test fun aChoiceWithImagesIsAChoiceCard() {
        val all = ContractJson.decodeWaits(fake.sampleText("attese")).items
        assertEquals(listOf(true, false, false), all.map(Scelta::isChoice))
    }

    @Test fun eachOptionHasItsLetterLineFilesAndAction() {
        val c = Scelta.choiceOf(open())
        assertEquals(
            listOf(
                listOf("A", "Release now", "tested everywhere but Safari", true, listOf("home-a.png"), emptyList<String>(), 0),
                listOf("B", "Tomorrow morning", "one more day, Safari tested", false, listOf("home-b.png"), listOf("home-b.pdf"), 1),
            ),
            c.options.map { o -> listOf(o.letter, o.name, o.line, o.recommended, o.images.map { it.name }, o.docs.map { it.name }, o.action) },
        )
        assertEquals(2, c.redo); assertEquals(listOf(3), c.rest); assertEquals(emptyList<WaitFile>(), c.extra)
    }

    @Test fun theActionOfAnOptionByValueThenLabelThenPlace() {
        val w = open()
        val noChoice = w.copy(actions = w.actions!!.map { a -> a.copy(cmd = JsonObject(a.cmd - "choice")) })
        assertEquals(listOf(0, 1), Scelta.choiceOf(noChoice).options.map { it.action })
        val noLabel = noChoice.copy(actions = noChoice.actions!!.mapIndexed { i, a -> if (i < 2) a.copy(label = "x$i") else a })
        assertEquals(listOf(0, 1), Scelta.choiceOf(noLabel).options.map { it.action })
    }

    @Test fun fromTheArchiveNoButtonsAndHowItEnded() {
        val w = archived()
        val c = Scelta.choiceOf(w)
        assertEquals(listOf(null, null), c.options.map { it.action }); assertNull(c.redo); assertEquals(emptyList<Int>(), c.rest)
        val o = Scelta.outcomeOf(w)
        assertEquals(listOf<Any?>(Scelta.State.CHOSEN, "A", "Release now", "phone-pixel8", 1789216500L), listOf(o.state, o.letter, o.name, o.by, o.at))
    }

    @Test fun theEarlierRoundsLatestFirst() {
        val r = Scelta.roundsOf(open())
        assertEquals(listOf(listOf(1, "redo", "neither: the old one looks broken on mobile (from the phone)", listOf("A", "B"), listOf("round1-a.png"))),
            r.map { x -> listOf(x.n, x.state, x.reason, x.options.map { it.letter }, x.options[0].images.map { it.name }) })
        assertEquals(emptyList<Scelta.Round>(), Scelta.roundsOf(item()))
    }

    @Test fun theReasonOfRedo() {
        assertEquals(300, Scelta.REDO_MAX)
        assertFalse(Scelta.redoReady("")); assertFalse(Scelta.redoReady("   \n")); assertTrue(Scelta.redoReady("troppo grande"))
        assertTrue(Scelta.redoReady("x".repeat(300))); assertFalse(Scelta.redoReady("x".repeat(301)))
    }

    @Test fun thumbnails() {
        assertEquals(listOf(listOf("A", "Release now", "home-a.png"), listOf("B", "Tomorrow morning", "home-b.png")), Scelta.thumbsOf(open()).map { listOf(it.letter, it.name, it.file.name) })
        // La verifica della fixture (0.8.155): le sue immagini, senza lettere; il pdf resta fuori dalle miniature.
        val prova = ContractJson.decodeWaits(fake.sampleText("attese")).items.single { it.kind == "prova" }
        assertEquals(listOf(listOf(null, "", "checkout-before.png")), Scelta.thumbsOf(prova).map { listOf(it.letter, it.name, it.file.name) })
        assertEquals(emptyList<Scelta.Thumb>(), Scelta.thumbsOf(item(kind = "prova")))
    }

    @Test fun frameAndStripShapes() {
        assertEquals(4f / 3f, Scelta.frameRatio(listOf(null, null)), 1e-4f)
        assertEquals(1.6f, Scelta.frameRatio(listOf(null, 1.6f)), 1e-4f)
        assertEquals(9f / 16f, Scelta.frameRatio(listOf(412f / 915f, 1.6f)), 1e-4f)
        assertEquals(16f / 9f, Scelta.frameRatio(listOf(3f)), 1e-4f)
        assertEquals(1.1f, Scelta.stripRatio(null), 1e-4f); assertEquals(1.1f, Scelta.stripRatio(0.45f), 1e-4f); assertEquals(16f / 9f, Scelta.stripRatio(2.5f), 1e-4f)
    }

    @Test fun aSwipeInTheViewer() {
        assertEquals(1, Scelta.swipeTo(-80f, 5f, 300f)); assertEquals(-1, Scelta.swipeTo(80f, 0f, 300f))
        assertEquals(0, Scelta.swipeTo(-30f, 0f, 400f)); assertEquals(1, Scelta.swipeTo(-30f, 0f, 50f))
        assertEquals(0, Scelta.swipeTo(-60f, -90f, 200f)); assertEquals(0, Scelta.swipeTo(0f, 0f, 0f))
    }

    @Test fun theArchiveFlagIsWrittenOnlyWhenSet() {
        val plain = ContractJson.json.encodeToString(it.pixelbox.cmwatch.contract.Cmd.serializer(), it.pixelbox.cmwatch.contract.Cmd("1", it.pixelbox.cmwatch.contract.CmdOp.ATTESA, null, "w3a91c04e7b", 1, "phone"))
        assertFalse(plain.contains("archive"))
        val asked = ContractJson.json.encodeToString(it.pixelbox.cmwatch.contract.Cmd.serializer(), it.pixelbox.cmwatch.contract.Cmd("1", it.pixelbox.cmwatch.contract.CmdOp.ATTESA, null, "w3a91c04e7b", 1, "phone", archive = true))
        assertTrue(asked.contains("\"archive\":true"))
    }
}
