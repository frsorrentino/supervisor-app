package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class ChatFeedTest {
    private val results = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray
        .map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
    private val first = ContractJson.decodeTranscript(results[19].text)
    private val after = ContractJson.decodeTranscript(results[20].text)

    @Test fun parsesTheFixturePage() {
        // Contratto 1.72: in fondo alla pagina la voce Edit a8.0 col suo diff.
        assertEquals(19, first.entries.size)
        assertFalse(first.more)
        assertEquals("image/png", first.entries.first { it.id == "a6.0" }.files!!.single().mime)
        assertEquals(130L, first.entries.first { it.id == "a4.0" }.turn!!.out)
    }

    @Test fun phoneMessageBecomesMineWithItsStatus() {
        val p1 = first.entries.first { it.id == "p1.0" }
        val sent = Sent("c1", "field-notes", "Also add the attendees list", sentAt = p1.at!! - 2)
        val feed = ChatFeed.merge(first.entries, listOf(sent to ChatRules.Status.DONE))
        val mine = feed.filterIsInstance<ChatFeed.Item.Mine>().single()
        assertEquals("p1.0", mine.entry!!.id)
        assertEquals(ChatRules.Status.DONE, mine.status)
        assertTrue(feed.none { it is ChatFeed.Item.User && it.entry.id == "p1.0" })
    }

    @Test fun textTypedAtThePcIsNeverMine() {
        val sent = Sent("c3", "field-notes", "Also fix the typo in the title", sentAt = 1789207318)
        val feed = ChatFeed.merge(first.entries, listOf(sent to ChatRules.Status.DELIVERED))
        assertTrue(feed.any { it is ChatFeed.Item.User && it.entry.id == "u2.0" })
        assertNull(feed.filterIsInstance<ChatFeed.Item.Mine>().single().entry)
    }

    @Test fun prefixedUserEntryStillMatches() {
        val e = TranscriptEntry("u9.0", "user", text = "Dall'utente via polso (watch). Chiudi con Watch. run the tests", at = 100)
        val feed = ChatFeed.merge(listOf(e), listOf(Sent("c9", "kb", "run the tests", sentAt = 99) to ChatRules.Status.DELIVERED))
        assertTrue(feed.single() is ChatFeed.Item.Mine)
    }

    @Test fun unmatchedSentGoesByTime() {
        val sent = Sent("c2", "field-notes", "one more thing", sentAt = 1789207420)
        val feed = ChatFeed.merge(first.entries, listOf(sent to ChatRules.Status.SENDING))
        val last = feed.last() as ChatFeed.Item.Mine
        assertNull(last.entry)
        assertEquals("c2", last.sent.id)
    }

    @Test fun rolesMapToItems() {
        val feed = ChatFeed.merge(first.entries, emptyList())
        assertTrue(feed.first() is ChatFeed.Item.User)
        assertTrue(feed.first { (it as? ChatFeed.Item.Tool)?.entry?.id == "a3.0" } is ChatFeed.Item.Tool)
        assertTrue(feed.any { (it as? ChatFeed.Item.Claude)?.entry?.id == "a5.0" })
        // Contratto 1.72: la pagina finisce col passaggio di modifica a8.0.
        assertEquals("a8.0", (feed.last() as ChatFeed.Item.Tool).entry.id)
    }

    @Test fun afterPageAppendsWithoutDuplicates() {
        val merged = ChatFeed.append(first.entries.take(5), after, ChatFeed.Page.AFTER)
        assertEquals(first.entries.map { it.id }, merged.map { it.id })
    }

    // Contratto 1.93: l'id di `after` non c'è più (riavvio, /clear): il relay manda le ultime voci con reset, e prendono il posto.
    @Test fun transcript_reset_replacesTheListAndTakesMore() {
        val page = ContractJson.decodeTranscript("""{"entries": [{"id": "n1.0", "role": "user", "text": "ciao", "at": 1789300000}], "more": true, "reset": true}""")
        assertTrue(page.reset)
        val mode = ChatFeed.mode(page, ChatFeed.Page.AFTER)
        assertEquals(ChatFeed.Page.FRESH, mode)
        assertEquals(listOf("n1.0"), ChatFeed.append(first.entries, page, mode).map { it.id })
        assertFalse(after.reset)
        assertEquals(ChatFeed.Page.AFTER, ChatFeed.mode(after, ChatFeed.Page.AFTER))
    }

    @Test fun beforePagePrepends() {
        val older = TranscriptPage(listOf(TranscriptEntry("u0.0", "user", text = "earlier", at = 1)), more = false)
        assertEquals("u0.0", ChatFeed.append(first.entries, older, ChatFeed.Page.BEFORE).first().id)
    }

    /**
     * Franz 23/10 23:17: «il tasto carica messaggi precedenti non funziona» (chat della master dopo un /clear: la pagina
     * «prima» torna vuota, o con voci già note, e il tasto resta). Il tasto carica o sparisce: `more` resta vero solo se
     * la pagina ha portato voci nuove; una pagina rifiutata (null) lo spegne.
     */
    @Test fun theOlderButtonLoadsOrGoesAway() {
        val older = TranscriptPage(listOf(TranscriptEntry("u0.0", "user", text = "earlier", at = 1)), more = true)
        val (list, more) = ChatFeed.older(first.entries, older)
        assertEquals("u0.0", list.first().id); assertTrue(more)
        assertEquals(first.entries to false, ChatFeed.older(first.entries, TranscriptPage(emptyList(), more = true)))
        assertEquals(first.entries to false, ChatFeed.older(first.entries, TranscriptPage(first.entries.take(3), more = true)))
        assertEquals(first.entries to false, ChatFeed.older(first.entries, null))
    }

    @Test fun argForThePolls() {
        assertEquals("50", ChatFeed.arg(null))
        assertEquals("50:after=a5.0", ChatFeed.arg("a5.0"))
        assertEquals("50:before=u1.0", ChatFeed.olderArg("u1.0"))
    }

    // Revisione 30/09: una risposta corta ripetuta va al messaggio più vicino nel tempo, non al più vecchio.
    @Test fun nearestMessageWins() {
        val e = TranscriptEntry("u5.0", "user", text = "sì", at = 1000, origin = "phone")
        val old = Sent("a", "kb", "sì", sentAt = 100) to ChatRules.Status.DELIVERED
        val new = Sent("b", "kb", "sì", sentAt = 998) to ChatRules.Status.DELIVERED
        val mine = ChatFeed.merge(listOf(e), listOf(old, new)).filterIsInstance<ChatFeed.Item.Mine>()
        assertEquals("u5.0", mine.single { it.sent.id == "b" }.entry?.id)
    }

    @Test fun watchEntriesAreNotMine() {
        val e = TranscriptEntry("u6.0", "user", text = "status?", at = 1000, origin = "watch")
        val feed = ChatFeed.merge(listOf(e), listOf(Sent("c", "kb", "status?", sentAt = 999) to ChatRules.Status.DELIVERED))
        assertTrue(feed.any { it is ChatFeed.Item.User })
    }

    @Test fun failedMessagesDoNotMatch() {
        val e = TranscriptEntry("u7.0", "user", text = "deploy", at = 1000, origin = "phone")
        val feed = ChatFeed.merge(listOf(e), listOf(Sent("d", "kb", "deploy", sentAt = 999, failed = "x") to ChatRules.Status.FAILED))
        assertTrue(feed.any { it is ChatFeed.Item.User })
    }

    // Con pagine più vecchie ancora da caricare, i messaggi più vecchi della pagina non si accumulano in testa.
    @Test fun leftoversOlderThanThePageHideWhileMore() {
        val e = TranscriptEntry("u8.0", "user", text = "now", at = 1000, origin = "pc")
        val feed = ChatFeed.merge(listOf(e), listOf(Sent("e", "kb", "long ago", sentAt = 10) to ChatRules.Status.DONE), more = true)
        assertTrue(feed.none { it is ChatFeed.Item.Mine })
    }

    // Contratto 1.22 aggiornato (30/09 22:37): un messaggio scritto a turno in corso nasce «in coda» e poi la stessa voce
    // diventa normale; la chat la sostituisce, senza doppioni.
    @Test fun queuedEntryUpdatesInPlace() {
        val q = first.entries.single { it.queued }
        val page = TranscriptPage(listOf(q.copy(queued = false)), more = false)
        val merged = ChatFeed.append(first.entries, page, ChatFeed.Page.AFTER)
        assertEquals(first.entries.size, merged.size)
        assertFalse(merged.single { it.id == q.id }.queued)
    }

    // La lettura riparte da prima della prima voce in coda, così la vede cambiare; senza code, dalla penultima (il costo
    // del turno arriva sull'ultima dopo).
    @Test fun readsAnchorBeforeTheFirstQueued() {
        val firstQueued = first.entries.indexOfFirst { it.queued }
        assertEquals(first.entries[firstQueued - 1].id, ChatFeed.anchor(first.entries))
        val settled = first.entries.map { it.copy(queued = false) }
        assertEquals(settled[settled.size - 2].id, ChatFeed.anchor(settled))
        assertNull(ChatFeed.anchor(emptyList()))
    }

    // Se la voce è nella trascrizione il messaggio è arrivato, qualunque cosa dica il risultato che non è venuto.
    @Test fun matchedEntryProvesDelivery() {
        val e = TranscriptEntry("p9.0", "user", text = "check the logs", at = 1000, origin = "phone")
        val mine = ChatFeed.merge(listOf(e), listOf(Sent("f", "kb", "check the logs", sentAt = 990) to ChatRules.Status.UNCERTAIN))
            .filterIsInstance<ChatFeed.Item.Mine>().single()
        assertEquals(ChatRules.Status.DELIVERED, mine.status)
    }

    /** Un programmato non ancora partito non si aggancia a una voce con lo stesso testo: resta «parte alle …». */
    @Test fun scheduledIsNeverMatched() {
        val p1 = first.entries.first { it.id == "p1.0" }
        val sent = Sent("c1", "field-notes", "Also add the attendees list", sentAt = p1.at!! - 2, scheduledFor = p1.at!! + 3600)
        val feed = ChatFeed.merge(first.entries, listOf(sent to ChatRules.Status.SCHEDULED))
        assertEquals(ChatRules.Status.SCHEDULED, feed.filterIsInstance<ChatFeed.Item.Mine>().single().status)
        assertNull(feed.filterIsInstance<ChatFeed.Item.Mine>().single().entry)
    }

    // Franz, 01/10 15:59 («Gruppi + righe ricche»): i passaggi fra due messaggi di Claude diventano un gruppo.
    private fun tool(id: String, name: String, text: String, note: String? = null) =
        ChatFeed.Item.Tool(TranscriptEntry(id = id, role = "tool", tool = name, text = text, note = note, at = 1))
    private fun claude(id: String) = ChatFeed.Item.Claude(TranscriptEntry(id = id, role = "assistant", text = "ok", at = 1))

    @Test fun consecutiveToolsBecomeOneGroup() {
        val out = ChatFeed.group(listOf(claude("c1"), tool("t1", "Bash", "ls"), tool("t2", "Read", "a.md"), tool("t3", "Bash", "pwd"), claude("c2")))
        assertEquals(3, out.size)
        val steps = out[1] as ChatFeed.Item.Steps
        assertEquals(listOf("t1", "t2", "t3"), steps.entries.map { it.id })
    }

    @Test fun singleToolStaysARow() {
        val out = ChatFeed.group(listOf(claude("c1"), tool("t1", "Bash", "ls"), claude("c2")))
        assertTrue(out[1] is ChatFeed.Item.Tool)
    }

    @Test fun stepsCountByToolMostFirst() {
        val steps = ChatFeed.group(listOf(tool("t1", "Read", "a"), tool("t2", "Bash", "b"), tool("t3", "Bash", "c"))).single() as ChatFeed.Item.Steps
        assertEquals(listOf("Bash" to 2, "Read" to 1), steps.counts)
    }

    @Test fun stepsCountMcpToolsByShortName() {
        val steps = ChatFeed.group(listOf(tool("t1", "mcp__chrome-bridge__click", ""), tool("t2", "mcp__chrome-bridge__click", ""))).single() as ChatFeed.Item.Steps
        assertEquals(listOf("click" to 2), steps.counts)
    }

    // Segnalazione 02/10: una sessione appena rilanciata pulita ha la conversazione vuota; la rotella finiva mai.
    @Test fun spinnerOnlyUntilTheFirstAnswer() {
        assertTrue(ChatFeed.loading(emptyList(), answered = false))
        assertFalse(ChatFeed.loading(emptyList(), answered = true))
        assertFalse(ChatFeed.loading(first.entries, answered = false))
    }

    // Dal vivo 03/10 17:49: scorrendo da A a B, la pagina di B mostrava per un attimo la conversazione di A. Le voci dal vivo
    // valgono solo per la sessione a cui appartengono; per le altre l'ultima copia letta.
    @Test fun livePageEntriesOnlyForTheirOwnSession() {
        val a = listOf(it.pixelbox.cmwatch.contract.TranscriptEntry("a1", "assistant", "di A"))
        val b = listOf(it.pixelbox.cmwatch.contract.TranscriptEntry("b1", "assistant", "di B"))
        val cache = mapOf("B" to b)
        assertEquals(b, ChatFeed.pageEntries("B", open = "B", owner = "A", live = a, cache = cache))
        assertEquals(a, ChatFeed.pageEntries("A", open = "A", owner = "A", live = a, cache = cache))
        assertEquals(b, ChatFeed.pageEntries("B", open = "A", owner = "A", live = a, cache = cache))
        assertEquals(emptyList<it.pixelbox.cmwatch.contract.TranscriptEntry>(), ChatFeed.pageEntries("C", open = "C", owner = "A", live = a, cache = cache))
    }

    /** Piano prestazioni, Task 7: una lettura in volo da più di 10 s, con la chat già piena, dice da quanto aspetta il PC. */
    @Test fun attesaDelPcSoloConVociEOltreDieciSecondi() {
        assertNull(ChatFeed.waitingPc(true, null, 50_000))
        assertNull(ChatFeed.waitingPc(true, 40_000, 50_000))
        assertEquals(11, ChatFeed.waitingPc(true, 39_000, 50_000))
        assertNull(ChatFeed.waitingPc(false, 0, 50_000))
    }

    // Franz, 10/10 09:47 e 14/10 18:39: «quando cambio una chat vedo per qualche secondo il testo vecchio». Si rilegge in
    // sottofondo chi non è mai stato letto o ha attività nuova (anche durante il turno), al massimo ogni 12 s; mai la
    // sessione aperta, che si legge da sé, né una chiusa.
    @Test fun sessionsToReadAgainInTheBackground() {
        val st = ContractJson.decodeState(Fixtures.stateIdle)
        val s = st.sessions.first().copy(name = "atlas-shop", state = SessionState.BUSY, tool = "Bash", outcome = null)
        val read = ChatFeed.Read(ChatFeed.activity(s), 1_000, done = true)
        assertEquals(listOf("atlas-shop"), ChatFeed.due(listOf(s), emptyMap(), open = null, nowMs = 1_000))
        assertTrue(ChatFeed.due(listOf(s), mapOf("atlas-shop" to read), open = null, nowMs = 60_000).isEmpty())
        val next = s.copy(tool = "Edit")
        assertTrue(ChatFeed.due(listOf(next), mapOf("atlas-shop" to read), open = null, nowMs = 12_999).isEmpty())
        assertEquals(listOf("atlas-shop"), ChatFeed.due(listOf(next), mapOf("atlas-shop" to read), open = null, nowMs = 13_000))
        val ended = s.copy(state = SessionState.IDLE, tool = null, outcome = Outcome("done", "Esito: done", 200))
        assertEquals(listOf("atlas-shop"), ChatFeed.due(listOf(ended), mapOf("atlas-shop" to read), open = null, nowMs = 13_000))
        assertTrue(ChatFeed.due(listOf(next), emptyMap(), open = "atlas-shop", nowMs = 13_000).isEmpty())
        assertTrue(ChatFeed.due(listOf(next.copy(state = SessionState.GONE)), emptyMap(), open = null, nowMs = 13_000).isEmpty())
        // Aprendo la chat: la copia è attuale solo se l'ultima lettura è arrivata e nessuna attività è venuta dopo.
        assertFalse(ChatFeed.behind(s, read))
        assertTrue(ChatFeed.behind(next, read))
        assertTrue(ChatFeed.behind(s, read.copy(done = false)))
        assertTrue(ChatFeed.behind(s, null))
    }

    // Heads up del 12/10: riaperta con le voci in cache, la chat della master perdeva «Carica i messaggi precedenti» e i
    // messaggi mandati più vecchi della prima voce si ammucchiavano in testa, senza risposta. Vale l'ultima pagina letta da
    // capo o all'indietro; senza quel dato il tasto c'è.
    @Test fun aReopenedChatKeepsItsOlderPages() {
        assertTrue(ChatFeed.moreOnOpen(first.entries, known = true))
        assertFalse(ChatFeed.moreOnOpen(first.entries, known = false))
        assertTrue(ChatFeed.moreOnOpen(first.entries, known = null))
        assertFalse(ChatFeed.moreOnOpen(emptyList(), known = null))
        // Con il tasto, un messaggio mandato prima della prima voce aspetta la sua pagina.
        val old = Sent("c0", "field-notes", "Il messaggio di ieri", sentAt = first.entries.mapNotNull { it.at }.min() - 86_400)
        val feed = ChatFeed.merge(first.entries, listOf(old to ChatRules.Status.DONE), more = ChatFeed.moreOnOpen(first.entries, known = null))
        assertTrue(feed.none { it is ChatFeed.Item.Mine && it.sent.id == "c0" })
    }

    // Contratto 1.59: la chat si apre da /tail. Con la copia che si aggancia la storia vecchia resta; staccata, il tail la sostituisce.
    @Test fun aChatOpensFromItsTail() {
        val e = { id: String -> TranscriptEntry(id, "assistant", id, at = 1) }
        val tail = Tail("atlas-shop", "s1", 100, listOf(e("b"), e("c"), e("d")), more = true)
        assertEquals(listOf("a", "b", "c", "d") to null, ChatFeed.withTail(listOf(e("a"), e("b")), tail).let { (l, m) -> l.map { it.id } to m })
        assertEquals(listOf("b", "c", "d") to true, ChatFeed.withTail(listOf(e("x")), tail).let { (l, m) -> l.map { it.id } to m })
        assertEquals(listOf("x") to null, ChatFeed.withTail(listOf(e("x")), tail.copy(entries = emptyList())).let { (l, m) -> l.map { it.id } to m })
        assertEquals("abc_def_g_h_i_j_k", Tail.key("abc.def#g\$h[i]j/k"))
        val json = """{"session":"atlas-shop","id":"s1","tx":"t","at":100,"entries":[{"id":"b","role":"assistant","text":"hi","at":1}],"more":false}"""
        assertEquals("hi", ContractJson.json.decodeFromString(Tail.serializer(), json).entries.single().text)
    }
}
