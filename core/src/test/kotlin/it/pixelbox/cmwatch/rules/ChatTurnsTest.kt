package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La scheda unica (app-chat-scheda-unica-13-11): gli stessi casi di web/src/lib/chatTurns.test.ts, dallo stesso file. */
class ChatTurnsTest {
    private val cases = Json.parseToJsonElement(javaClass.getResource("/chat-turns-cases.json")!!.readText()).jsonObject
    private fun str(o: JsonObject, k: String): String? = (o[k] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun entry(o: JsonObject) = TranscriptEntry(
        id = str(o, "id") ?: "x", role = str(o, "role") ?: "tool", text = str(o, "text"), tool = str(o, "tool"), note = str(o, "note"),
        at = (o["at"] as? JsonPrimitive)?.content?.toLongOrNull(),
        turn = (o["turn"] as? JsonObject)?.let { t -> it.pixelbox.cmwatch.contract.TranscriptTurn(started = (t["started"] as? JsonPrimitive)?.content?.toLongOrNull(), ended = (t["ended"] as? JsonPrimitive)?.content?.toLongOrNull()) },
    )
    private fun orNull(e: JsonElement?): String? = if (e == null || e is JsonNull) null else e.jsonPrimitive.content

    private fun shape(entries: List<TranscriptEntry>, grouped: Boolean): List<String> {
        val items = ChatFeed.merge(entries, emptyList())
        return ChatTurns.turns(if (grouped) ChatFeed.group(items) else items).map { r ->
            when (r) {
                is ChatTurns.Row.Sep -> "sep ${r.item.entry.id}"
                is ChatTurns.Row.Turn -> "${r.key} | ${r.request?.let(ChatFeed::entryOf)} | ${r.work.joinToString(",") { (if (it.say) "say:" else "tool:") + it.entry.id }} | ${r.answer?.id}" + if (r.auto) " | auto" else ""
            }
        }
    }

    @Test fun turns() = cases["turns"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val entries = o["entries"]!!.jsonArray.map { entry(it.jsonObject) }
        val want = o["out"]!!.jsonArray.map { x ->
            val w = x.jsonObject
            if ("sep" in w) "sep ${str(w, "sep")}"
            else "${str(w, "key")} | ${orNull(w["request"])} | ${w["work"]!!.jsonArray.joinToString(",") { it.jsonPrimitive.content }} | ${orNull(w["answer"])}" + if ((w["auto"] as? JsonPrimitive)?.content == "true") " | auto" else ""
        }
        assertEquals(str(o, "name"), want, shape(entries, false))
        // Le voci già raccolte in gruppi di passi danno gli stessi turni.
        assertEquals(str(o, "name"), want, shape(entries, true))
    }

    @Test fun aMessageNotYetTranscribedOpensItsTurn() {
        val sent = Sent(id = "c1", session = "a", text = "Vai", sentAt = 10)
        val rows = ChatTurns.turns(ChatFeed.merge(emptyList(), listOf(sent to ChatRules.Status.SENDING)))
        val t = rows.single() as ChatTurns.Row.Turn
        assertEquals("m-c1", t.key); assertTrue(t.request is ChatFeed.Item.Mine); assertTrue(t.work.isEmpty()); assertEquals(null, t.answer)
        assertEquals(0, ChatTurns.rowOf(rows, t.request!!))
    }

    @Test fun rowsOfEntriesAndItems() {
        val entries = cases["turns"]!!.jsonArray[0].jsonObject["entries"]!!.jsonArray.map { entry(it.jsonObject) }
        val items = ChatFeed.group(ChatFeed.merge(entries, emptyList()))
        val rows = ChatTurns.turns(items)
        assertEquals(items.map { 0 }, items.map { ChatTurns.rowOf(rows, it) })
        assertEquals(listOf("u1", "a1", "t1", "a2", "t2", "t3", "a3"), (rows[0] as ChatTurns.Row.Turn).entryIds)
        assertEquals(-1, ChatTurns.rowOfEntry(rows, "nope"))
    }

    @Test fun aLongWorkClosedShowsItsLastEntries() {
        val work = (0 until 9).map { i -> ChatTurns.Step(say = i % 4 == 0, entry = TranscriptEntry(id = "e$i", role = "tool", error = i == 5)) }
        assertTrue(ChatTurns.foldable(work))
        assertEquals(work.takeLast(ChatTurns.FOLD_TAIL), ChatTurns.shownWork(work, false))
        assertEquals(9, ChatTurns.shownWork(work, true).size)
        assertFalse(ChatTurns.foldable(work.take(6)))
        assertEquals(6, ChatTurns.shownWork(work.take(6), false).size)
        val t = ChatTurns.Row.Turn("k", null, work, null)
        assertEquals(6 to 1, t.tools to t.failed)
    }

    @Test fun stepLines() = cases["steps"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val e = TranscriptEntry(id = "x", role = "tool", tool = str(o, "tool"), text = str(o, "text"), note = str(o, "note"))
        val w = o["out"]!!.jsonObject
        assertEquals(str(o, "name"), ChatTurns.Line(str(w, "name")!!, str(w, "arg")!!, (w["prose"] as JsonPrimitive).content == "true"), ChatTurns.stepLine(e))
        assertEquals(str(o, "name"), ChatTurns.Detail(str(w, "note"), str(w, "text")), ChatTurns.stepDetail(e))
    }

    @Test fun sections() = cases["sections"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val want = Answer.Doc(str(o, "lead")!!, o["secs"]!!.jsonArray.map { Answer.Section(str(it.jsonObject, "title")!!, str(it.jsonObject, "body")!!) })
        assertEquals(str(o, "name"), want, Answer.sections(str(o, "text")!!))
    }

    @Test fun rich() = cases["rich"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val want = o["out"]!!.jsonArray.map { x ->
            val w = x.jsonObject
            when (str(w, "type")) {
                "p" -> Answer.Rich.P(str(w, "text")!!)
                "li" -> Answer.Rich.Li(str(w, "mark")!!, str(w, "text")!!, w["depth"]!!.jsonPrimitive.int)
                "quote" -> Answer.Rich.Quote(str(w, "text")!!)
                else -> Answer.Rich.Code(str(w, "text")!!)
            }
        }
        assertEquals(str(o, "name"), want, Answer.rich(str(o, "text")!!))
    }

    /** app-chat-testo-da-inviare-13-11: il tasto «Copia» del box di una citazione. */
    @Test fun copy() = cases["copy"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        assertEquals(str(o, "name"), str(o, "out"), Answer.copyText(str(o, "text")!!))
    }

    @Test fun tipo() {
        cases["tipo"]!!.jsonArray.forEach { c ->
            val o = c.jsonObject
            val w = o["out"]!!.jsonObject
            assertEquals(str(o, "name"), Answer.Tipo(str(w, "text")!!, str(w, "tipo")), Answer.stripTipo(str(o, "text")!!))
        }
        // In inglese la riga dell'esito con la sua etichetta chiude come «Esito».
        assertEquals(Answer.Tipo("Done.\nOutcome: fixed", "pronto"), Answer.stripTipo("Done.\nOutcome: fixed\nTipo: pronto", "Outcome"))
    }
}
