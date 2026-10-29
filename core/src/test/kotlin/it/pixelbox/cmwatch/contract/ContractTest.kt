package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.rules.RecapAgenda
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test

class ContractTest {
    private val all = listOf(Fixtures.stateQuestion, Fixtures.stateIdle, Fixtures.stateStale)

    @Test fun stateOneDecodesEveryField() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        assertEquals(1, s.v); assertEquals("crostini-demo", s.host); assertEquals(4, s.sessions.size)
        val led = s.sessions[0]
        assertEquals("ledger-api", led.name); assertEquals(SessionState.WAITING, led.state)
        assertTrue(led.followed); assertEquals("Wait for the go", led.next)
        val q = led.question!!
        assertEquals(QuestionKind.ASK, q.kind); assertEquals(Tier.MEDIUM, q.tier)
        assertEquals(listOf(1, 2), q.options.map { it.n }); assertEquals("yes", q.options[0].label)
        val atlas = s.sessions[1]
        assertEquals("Bash pytest -q tests", atlas.tool); assertEquals(1789210300L, atlas.outcome!!.at)
        assertNull(s.quota.getValue("work").h5); assertTrue(s.quota.getValue("work").stale)
        assertEquals(11, s.quota.getValue("personal").h5)
        assertEquals(3, s.projects.size); assertEquals(2, s.night.queued); assertNull(s.night.running)
        assertEquals("2026-09-12", s.recap.date); assertEquals(2, s.recap.items.size)
    }

    /** Contratto 1.42: `voice` sul prompt alla master, `via` e `confirmations` sugli ok della modalità live. */
    @Test fun liveCommandsCarryVoiceAndTheDoubleConfirmation() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val ask = cmds.single { it.voice == true }
        assertEquals(CmdOp.PROMPT, ask.op); assertEquals("master", ask.session)
        val live = cmds.filter { it.via == "live" }
        assertEquals(listOf(2, 1), live.map { it.confirmations }); assertTrue(live.all { it.op == CmdOp.APPROVE })
        assertEquals(listOf(true, false), live.map { c -> results.single { it.id == c.id }.ok })
        // Un comando senza i campi nuovi si scrive come prima: nessuna chiave in più per un relay vecchio.
        val plain = ContractJson.json.encodeToString(Cmd.serializer(), Cmd("x", CmdOp.APPROVE, null, "t", 1, "p"))
        listOf("voice", "via", "confirmations").forEach { assertFalse(it, "\"$it\"" in plain) }
        val sent = ContractJson.json.encodeToString(Cmd.serializer(), live[0])
        assertTrue("\"via\":\"live\"" in sent); assertTrue("\"confirmations\":2" in sent)
    }

    // Contratto 1.86: i campioni orari della settimana e da dove viene; senza, null.
    @Test fun quotaCarriesTheWeekPointsAndOrigins() {
        val q = ContractJson.decodeState(Fixtures.stateIdle).quota
        assertEquals(WeekPoints(1789005600, listOf(listOf(0, 2), listOf(2, 4), listOf(3, 5), listOf(24, 30), listOf(25, 38))), q.getValue("personal").w7Points)
        assertEquals(WeekOrigins(20.1, 6.5, 11.4), q.getValue("personal").w7Origins)
        assertNull(q.getValue("work").w7Points)
        assertNull(q.getValue("work").w7Origins)
    }

    @Test fun quotaCarriesTheFiveHourReset() {
        // Contratto 1.3: `reset_h5` è la ripartenza della finestra di 5 ore; `reset_w7` resta quella settimanale.
        val q = ContractJson.decodeState(Fixtures.stateQuestion).quota
        assertEquals(1789228800L, q.getValue("personal").resetH5)
        assertEquals(1789225200L, q.getValue("work").resetH5)
        assertEquals(1789610400L, q.getValue("personal").resetW7)
    }

    // Dalla 1.84 state-3 ha solo una sessione chiusa dall'app, con lo stato salvato.
    @Test fun staleFixtureHasNoLiveSessions() {
        val s = ContractJson.decodeState(Fixtures.stateStale)
        assertTrue(s.sessions.all { it.state == SessionState.GONE }); assertEquals(1789200000L, s.ts)
        assertEquals(ClosedState(1789199000, "Pricing page draft ready for review", "Pick up the pricing page", 2, listOf("src/pages/pricing.md", "docs/notes.md"), "3d1b2c4e-0000-4000-8000-000000000004"), s.sessions.single().closed)
    }

    /** Contratto 1.88 (27/10): la coda dei suggerimenti; state-3 ha una voce per tipo nuovo, state-2 le tre con rank e source. */
    @Test fun contract188() {
        val s2 = ContractJson.decodeState(Fixtures.stateIdle); val s3 = ContractJson.decodeState(Fixtures.stateStale)
        assertEquals(listOf(2, 5, 6), s2.suggestions!!.map { it.rank })
        assertTrue(s2.suggestions!!.all { it.source == "master" && it.prompt.isEmpty() })
        assertEquals(listOf("choice", "followup", "unsaved", "waiting", "observe", "idea", "proposal"), s3.suggestions!!.map { it.kind })   // 1.94: observe dopo l'attesa esterna
        assertEquals(listOf("Short", "Full"), s3.suggestions!!.first { it.kind == "choice" }.options)
        assertEquals(4, s3.suggestions!!.count { it.source == "script" })   // 1.94: anche observe viene da uno script
        assertTrue(s3.suggestions!!.filter { it.kind != "choice" }.all { it.options == null })
    }

    /** Dalla 1.84 state-2 ha già un needs_you (atlas-shop, legato a un compito): i casi che ne scrivono uno lo sostituiscono. */
    private fun idleWithNeedsYou(obj: String) = Fixtures.stateIdle.replaceFirst(Regex("\"needs_you\": \\{[^}]*\\}"), "\"needs_you\": $obj")

    /** Contratto 1.84 (24/10): suggerimenti, quota di oggi, needs_you legato a un compito, stato salvato alla chiusura. */
    @Test fun contract184() {
        val s1 = ContractJson.decodeState(Fixtures.stateQuestion); val s2 = ContractJson.decodeState(Fixtures.stateIdle)
        assertEquals(emptyList<Suggestion>(), s1.suggestions)
        assertEquals(3, s2.suggestions!!.size); assertEquals(3, s2.suggestions!!.map { it.kind }.toSet().size)
        assertEquals(4, s2.usageToday!!.size); assertEquals(2, s2.usageToday!!.map { it.account }.toSet().size)
        assertEquals(NeedsYou("Type the pairing code shown on the TV", 1789214200, expiresAt = 1789221400, task = "atlas-tv-pairing", from = "master"), s2.sessions.single().needsYou)
        assertEquals(ClosedState(1789200000, sessionId = "3d1b2c4e-0000-4000-8000-000000000004"), s1.sessions.first { it.closed != null }.closed)
    }

    @Test fun rulesHoldOnEveryFixture() {
        for (raw in all) {
            // Gli 8 KB sono quelli dello stato che viaggia, JSON compatto: la fixture è indentata per leggerla (con i
            // dispositivi della 1.32 e i ricorrenti della 1.33 state-1 indentata passa gli 8 KB, compatta ne pesa 5,7).
            assertTrue("state ≤ 8 KB", Json.parseToJsonElement(raw).toString().toByteArray().size <= 8 * 1024)
            val s = ContractJson.decodeState(raw)
            for (ses in s.sessions) {
                ses.outcome?.let { assertTrue(it.short.length <= 200); assertTrue(it.full.length <= 600) }
                ses.question?.let { q ->
                    assertFalse(q.text.endsWith("…")); assertTrue(q.text.isNotBlank())
                    assertEquals((1..q.options.size).toList(), q.options.map { it.n })
                }
            }
            assertEquals(Order.sessions(s.sessions), s.sessions)
        }
    }

    /** 1.43: `published_at` alla radice, decimali ammessi; assente con un relay precedente. */
    @Test fun publishedAtFromTheFixtures() {
        assertEquals(1789210801.5, ContractJson.decodeState(Fixtures.stateQuestion).publishedAt!!, 0.0)
        assertEquals(1789214401.5, ContractJson.decodeState(Fixtures.stateIdle).publishedAt!!, 0.0)
        assertNull(ContractJson.decodeState(Fixtures.stateIdle.replace("\"published_at\": 1789214401.5,", "")).publishedAt)
    }

    @Test fun roundTripKeepsEveryValue() {
        for (raw in all) {
            val s = ContractJson.decodeState(raw)
            val again = ContractJson.decodeState(ContractJson.json.encodeToString(State.serializer(), s))
            assertEquals(s, again)
        }
    }

    @Test fun eventsDecodeOnePerKind() {
        val ev = ContractJson.decodeEvents(Fixtures.events)
        // Manca «resumed»; week_unused e week_pace (1.67), needs_you, turn_blocking e attention_reminder (1.68) non sono
        // nelle fixture del relay 0.8.24.
        assertEquals(EventKind.entries.size - 6, ev.map { it.kind }.toSet().size)
        assertEquals("q-1789210500-1", ev.first { it.kind == EventKind.QUESTION }.ref)
        assertNull(ev.first { it.kind == EventKind.QUOTA }.session)
    }

    /** Contratto 1.85 (25/10): il limite di un modello; il quota_limit 5h non offre più «pass». */
    @Test fun modelLimitEvent() {
        val ev = ContractJson.decodeEvents(Fixtures.events).filter { it.kind == EventKind.QUOTA_LIMIT }
        val model = ev.single { it.window == "model" }
        assertEquals("1789199830_026", model.key); assertEquals("Opus 5.5", model.otherModel); assertNull(model.resetAt)
        assertEquals(listOf("other_model", "other_account"), model.actions)
        assertTrue(ev.none { "pass" in it.actions.orEmpty() })
    }

    // Contratto 1.68: ferma per Franz e promemoria, come li scrive il relay 0.8.24 (test R52 del relay).
    /** Contratto 1.80 (23/10): le richieste in «Serve te» si chiudono da sole; op `attention_done` = il tasto «Fatto». */
    @Test fun attentionDoneAndTheClosingRequests() {
        val st = ContractJson.decodeState(Fixtures.stateQuestion)
        assertTrue("attention_done" in st.ops.orEmpty())
        assertEquals("answer", st.attention.orEmpty().first().closes)
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val done = cmds.filter { it.op == CmdOp.ATTENTION_DONE }
        assertEquals(listOf("atlas-shop", "nobody"), done.map { it.arg })
        assertEquals(listOf("needs_you", "needs_you"), done.map { it.kind })
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        assertEquals("closed: needs_you atlas-shop", results.first { it.id == done[0].id }.text)
        assertFalse(results.first { it.id == done[1].id }.ok)
        // Il comando codificato porta kind e, per una ferma del turno, since; i campi assenti non si scrivono.
        val enc = ContractJson.json.encodeToString(Cmd("c1", CmdOp.ATTENTION_DONE, null, "pix-docs", 1, "phone", kind = "turn_blocking", since = 1789210000))
        assertTrue(enc, enc.contains("\"op\":\"attention_done\"") && enc.contains("\"kind\":\"turn_blocking\"") && enc.contains("\"since\":1789210000"))
        assertFalse(ContractJson.json.encodeToString(Cmd("c2", CmdOp.SCREEN, "x", null, 1, "phone")).contains("kind"))
        // needs_you con una condizione e una scadenza.
        val raw = idleWithNeedsYou("{\"reason\": \"login su win\", \"at\": 1789210100, \"until\": true, \"expires_at\": 1789296500}")
        val s = ContractJson.decodeState(raw).sessions.first { it.needsYou != null }
        assertEquals(NeedsYou("login su win", 1789210100, until = true, expiresAt = 1789296500), s.needsYou)
    }

    /** Contratto 1.81 (23/10): un compito che chiede una scelta porta `options`; op `approve` con `choice`. */
    @Test fun choicesWithButtons() {
        val a = ContractJson.decodeState(Fixtures.stateIdle).approvals.first { it.task == "atlas-checkout-layout" }
        assertEquals(listOf("Strada A", "Strada B"), a.options)
        assertTrue(ContractJson.decodeState(Fixtures.stateQuestion).approvals.none { it.options != null })
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val chosen = cmds.filter { it.op == CmdOp.APPROVE && it.choice != null }
        assertEquals(listOf("Strada B", "Strada C"), chosen.map { it.choice })
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        assertEquals("approved atlas-checkout-layout (Strada B)", results.first { it.id == chosen[0].id }.text)
        val ko = results.first { it.id == chosen[1].id }
        assertFalse(ko.ok); assertTrue(ko.text, ko.text.contains("not among"))
        // Il comando codificato porta choice; assente non si scrive.
        val enc = ContractJson.json.encodeToString(Cmd("c1", CmdOp.APPROVE, null, "atlas-checkout-layout", 1, "phone", text = "ok", choice = "Strada B"))
        assertTrue(enc, enc.contains("\"choice\":\"Strada B\""))
        assertFalse(ContractJson.json.encodeToString(Cmd("c2", CmdOp.APPROVE, null, "t", 1, "phone", text = "ok")).contains("choice"))
    }

    /** Contratto 1.83 (24/10): le domande dei server MCP (elicitation) in /state, l'evento e l'op `elicit`. */
    @Test fun elicitations() {
        val st = ContractJson.decodeState(Fixtures.stateIdle)
        assertTrue("elicit" in st.ops.orEmpty())
        val (form, link) = st.elicitations
        assertEquals("issue-tracker", form.server); assertEquals("form", form.mode); assertNull(form.url)
        assertEquals(listOf("choice", "boolean", "text", "integer"), form.fields.orEmpty().map { it.kind })
        val project = form.fields!!.first()
        assertEquals(listOf("DOCS", "SHOP", "OPS"), project.options!!.map { it.value }); assertEquals("Shop", project.options!![1].label)
        assertTrue(project.required); assertEquals(false, form.fields!![1].defaultValue?.jsonPrimitive?.boolean)
        assertEquals(3, form.fields!![2].minLength); assertEquals(80, form.fields!![2].maxLength); assertEquals("One line for the issue list", form.fields!![2].description)
        assertEquals(1.0, form.fields!![3].min); assertEquals(13.0, form.fields!![3].max)
        assertEquals(120, form.expiresAt - form.askedAt)
        assertEquals("url", link.mode); assertEquals("https://www.figma.com/oauth?client_id=demo", link.url); assertNull(link.fields)
        // Senza domande il campo manca: lista vuota, come prima della 1.83.
        assertTrue(ContractJson.decodeState(Fixtures.stateQuestion).elicitations.isEmpty())
        val ev = ContractJson.json.decodeFromString(ListSerializer(Event.serializer()), Fixtures.events).single { it.kind == EventKind.ELICITATION }
        assertEquals("issue-tracker", ev.title); assertEquals("e-4f1c2a9b7d30", ev.ref)
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val el = cmds.filter { it.op == CmdOp.ELICIT }
        assertEquals(listOf("accept", "accept", "decline"), el.map { it.action })
        assertEquals("SHOP", el[0].content!!.getValue("project").jsonPrimitive.content)
        assertEquals(3, el[0].content!!.getValue("estimate").jsonPrimitive.int)
        assertNull(el[2].content)
        assertEquals("answered issue-tracker (e-4f1c2a9b7d30)", results.first { it.id == el[0].id }.text)
        assertFalse(results.first { it.id == el[1].id }.ok)
        // Il comando codificato porta action e content; senza content la chiave non si scrive.
        val enc = ContractJson.json.encodeToString(Cmd("c1", CmdOp.ELICIT, "atlas-shop", "e-1", 1, "phone", action = "accept",
            content = buildJsonObject { put("project", "SHOP"); put("estimate", 3) }))
        assertTrue(enc, enc.contains("\"op\":\"elicit\"") && enc.contains("\"content\":{\"project\":\"SHOP\",\"estimate\":3}"))
        assertFalse(ContractJson.json.encodeToString(Cmd("c2", CmdOp.ELICIT, "s", "e-1", 1, "phone", action = "decline")).contains("content"))
    }

    /** Contratto 1.82 (24/10): `session_id` nel rapporto della notte; op `transcript` con `sid` per una conversazione chiusa. */
    @Test fun transcriptBySid() {
        val r = ContractJson.decodeNightReport(Fixtures.read("night-report-sample.json"))
        assertEquals("0b6e3c52-1f4a-4d7e-9a21-6c8d2e5f7a90", r.timeline.first { it.id == "atlas-shop" }.sessionId)
        assertNull(r.timeline.first { it.id == "master" }.sessionId)
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val bySid = cmds.filter { it.op == CmdOp.TRANSCRIPT && it.sid != null }
        assertEquals(2, bySid.size); assertNull(bySid[0].session)
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        assertTrue(results.first { it.id == bySid[0].id }.ok)
        assertEquals("no transcript for ffffffff-0000-4000-8000-000000000000", results.first { it.id == bySid[1].id }.text)
        val enc = ContractJson.json.encodeToString(Cmd("c1", CmdOp.TRANSCRIPT, null, "50", 1, "phone", sid = "abc"))
        assertTrue(enc, enc.contains("\"sid\":\"abc\""))
        assertFalse(ContractJson.json.encodeToString(Cmd("c2", CmdOp.TRANSCRIPT, "x", "50", 1, "phone")).contains("sid"))
    }

    @Test fun needsYouTurnBlockingAndReminder() {
        val st = ContractJson.decodeState(Fixtures.stateQuestion)
        val raw = idleWithNeedsYou("{\"reason\": \"login su win\", \"at\": 1789210100}, \"turn_blocking\": {\"text\": \"ok al deploy\", \"at\": 1789210000}")
        assertNotEquals(Fixtures.stateIdle, raw)
        val s = ContractJson.decodeState(raw).sessions.first { it.needsYou != null }
        assertEquals(NeedsYou("login su win", 1789210100), s.needsYou)
        assertEquals(TurnBlocking("ok al deploy", 1789210000), s.turnBlocking)
        assertTrue(st.sessions.all { it.needsYou == null && it.turnBlocking == null })
        val e = ContractJson.decodeEvents("""[{"key": "e-1", "kind": "attention_reminder", "session": "pix-docs", "account": null, "ts": 1789210700,
            "title": "⏰ still waiting for you (10 min): login su win", "body": "", "ref": "pix-docs", "attention_kind": "needs_you"}]""").single()
        assertEquals(EventKind.ATTENTION_REMINDER, e.kind); assertEquals("needs_you", e.attentionKind)
    }

    // Contratto 1.45 (08/10): lo stato del PC fermo, segnalato dal guardiano del relay; session, account e ref null.
    @Test fun relayStaleEvent() {
        val e = ContractJson.decodeEvents(Fixtures.events).single { it.kind == EventKind.RELAY_STALE }
        assertNull(e.session); assertNull(e.account); assertNull(e.ref); assertTrue(e.title.isNotBlank()); assertFalse(e.title.startsWith("⚠"))   // 1.78: niente simboli nei titoli
    }

    // Contratto 1.32: i dispositivi accoppiati, nell'ordine del PC; kind e seen null quando non si sanno.
    @Test fun stateCarriesThePairedDevices() {
        val d = ContractJson.decodeState(Fixtures.stateIdle).devices!!
        // Contratto 1.39: un browser, «Safari on Mac», di tipo web.
        assertEquals(listOf("phone", "watch", "tablet", "chromebook", null, "web"), d.map { it.kind })
        val unknown = d.single { it.kind == null }
        assertEquals("Pixel 7", unknown.name); assertNull(unknown.seen)
        assertNotNull(d.first().seen)
    }

    // Contratto 1.89: `cmd_at` = l'ultimo comando con l'uid del dispositivo, solo se ce n'è uno.
    @Test fun devicesCarryTheLastCommand() {
        val d = ContractJson.decodeState(Fixtures.stateStale).devices!!.associateBy { it.name }
        assertEquals(1789199950L, d.getValue("Pixel 9").cmdAt)
        assertEquals(1789198000L, d.getValue("Safari on Mac").cmdAt)
        assertNull(d.getValue("Pixel 7").cmdAt)
        assertEquals(1789150500L, d.getValue("Pixel 7").seen)
    }

    // Contratto 1.33: le azioni ricorrenti della master, nell'ordine del PC; param solo dove il prompt aspetta un pezzo.
    @Test fun stateCarriesTheRecurringActions() {
        val r = ContractJson.decodeState(Fixtures.stateIdle).recurring!!
        assertEquals(listOf("x-posts", "release-changelog", "plugin-rivals"), r.map { it.id })
        assertEquals(listOf(false, true, false), r.map { it.param })
        assertTrue(r.all { it.label.length <= 40 })
    }

    @Test fun cmdAndResultDecodeAndEncode() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        // Manca «unfollow»; dal contratto 1.72 anche «changes» e «diff», il cui risultato ha la sua fixture (changes-sample.json).
        assertEquals(setOf(CmdOp.UNFOLLOW, CmdOp.CHANGES, CmdOp.DIFF), CmdOp.entries.toSet() - cmds.map { it.op }.toSet())
        // Contratto 1.12: due risultati in più, «model» riuscito ed «effort» rifiutato su una sessione occupata.
        // Contratto 1.13: uno in più, il launch con il primo messaggio. Contratto 1.17: night_add e night_remove.
        // Contratto 1.19: tre report, uno rifiutato. Contratto 1.21: due interrupt, «stopped» e «nothing to stop».
        // Contratto 1.22: due transcript, la prima pagina e una pagina `after`, e un prompt con `device`.
        // Contratto 1.24: due file, uno aperto e uno rifiutato perché non è nella trascrizione.
        // Contratto 1.25: due slash, /compact con testo e un comando non consentito.
        // Contratto 1.26: un projects riuscito, tutti i progetti dal più recente.
        // Contratto 1.27: un search riuscito, «tuesday» in una sessione viva e in una chiusa.
        // Contratto 1.28: un report con un PDF, il nome ripulito dal relay.
        // Contratto 1.29: un timeline di tutte le sessioni da un epoch, con ogni kind di evento.
        // Contratto 1.31: un pair_add riuscito, il QR della 1.30 e il codice a 6 cifre.
        // Contratto 1.36: un prompt dalla web app, con `device` "web". Contratto 1.37: due approve (uno rifiutato) e un decision.
        // Contratto 1.39: due unpair, uno riuscito e uno rifiutato.
        // Contratto 1.42: un prompt a voce alla master e due approve dalla live, uno rifiutato con una conferma sola.
        // Contratto 1.44: due night, il rapporto e uno rifiutato perché quel giorno non c'è.
        // Contratto 1.46: due agenda, le righe e uno rifiutato senza file.
        // Contratto 1.47: otto agenda_set, quattro riusciti, la key vecchia rifiutata e tre rifiuti di validazione.
        // Contratto 1.48: due report a pezzi, uno riuscito e uno «bad parts». Contratto 1.49: un night_add alla master riuscito.
        // Contratto 1.50: due report con due id, uno riuscito e uno con il secondo allegato mancante.
        // Contratto 1.52: due answer con `question`, uno sulla domanda aperta e uno sulla precedente, rifiutato.
        // Contratto 1.58 (relay 0.8.20): tre quota_route, due riusciti e «pass» a una sessione non viva, e un prompt doppione rifiutato.
        // Contratti 1.63 e 1.64 (relay 0.8.21): un prompt con `at`, project_new col primo tocco rifiutato e la conferma riuscita.
        // Contratto 1.69 (relay 0.8.25): due permission_mode, uno riuscito e uno fuori dal giro di Shift+Tab.
        // Contratto 1.73: tre della pagina Studio (studio, calendario spento, prompt con scope), tutti riusciti.
        // Contratto 1.74: due nights, con due notti e vuoto.
        assertEquals(104, results.size); assertEquals(36, results.count { !it.ok })   // 1.95: plugins, plugin_check, plugin_update riuscito e uno da confermare al PC, plugin_update_all senza niente; 1.96: kpi; 1.91: sys_unit_stop riuscito, poi lo stesso rifiutato; 1.90: sys_kill riuscito, poi lo stesso rifiutato; 1.88: suggestion choose fuori dalle opzioni (rifiutato), discuss, choose accettato; 1.84: close_preview e close, ognuno riuscito e rifiutato; 1.87: suggestion riuscito e «already gone» (ok); 1.77: done riuscito, senza registro, data sbagliata; 1.80: attention_done riuscito e «nothing to close»; 1.81: approve con choice riuscito e rifiutato; 1.82: transcript per sid riuscito e rifiutato; 1.83: elicit riuscito, valore fuori dalle opzioni, link
        assertTrue(results.any { it.ok && it.text == "field-notes: permissions now acceptEdits" })
        assertTrue(results.any { !it.ok && it.text == "work-ledger-api: bypassPermissions is not in this session's Shift+Tab cycle, left on plan" })
        assertEquals("bad parts", results.first { it.id.endsWith("0481-4000-8000-000000000481") }.text)
        val sets = cmds.filter { it.op == CmdOp.AGENDA_SET }
        assertEquals(8, sets.size); assertTrue(sets.all { it.session == null && !it.arg.isNullOrBlank() && it.action != null })
        assertEquals("2026-10-20", sets.first { it.action == "snooze" }.until); assertEquals("claude", sets.first { it.action == "pass" }.blocks)
        val done = ContractJson.decodeAgendaSet(results.first { it.id == sets[0].id }.text).row
        assertEquals("fatto", done.state); assertTrue(done.key.isNotBlank() && done.key != sets[0].arg)
        assertEquals("agenda row changed", results.first { it.id == sets[1].id }.text)
        assertTrue("agenda_set" in ContractJson.decodeState(Fixtures.stateIdle).ops.orEmpty())
        // Un comando senza i campi 1.47 resta identico: action, until e blocks assenti non si scrivono.
        assertFalse("action" in ContractJson.json.encodeToString(Cmd.serializer(), Cmd("x", CmdOp.AGENDA, null, null, 1, "p")))
        val agenda = cmds.filter { it.op == CmdOp.AGENDA }
        assertEquals(2, agenda.size); assertNull(agenda[0].session); assertNull(agenda[0].arg)
        val agendaPage = ContractJson.decodeAgenda(results.first { it.id == agenda[0].id }.text)
        assertFalse(agendaPage.more)
        assertEquals(listOf("aperto", "aperto", "sospeso", "fatto", "scartato"), agendaPage.rows.map { it.state })
        assertEquals(listOf("agenzia", "personale", "agenzia", "postazione", "personale"), agendaPage.rows.map { it.scope })
        // Contratto 1.47: dettaglio con a capo veri e data di ritorno sulla sospesa, null altrove.
        assertEquals("2026-10-12", agendaPage.rows[2].until); assertTrue(agendaPage.rows[2].detail!!.contains('\n')); assertNull(agendaPage.rows[0].detail)
        assertEquals("orbit-docs", agendaPage.rows[1].ref); assertEquals("", agendaPage.rows[2].ref)
        // Contratto 1.79: il titolo breve delle righe dell'agenda e della riga che risponde ad agenda_set.
        assertEquals(listOf("Confirm the 6 client ids", "Move the docs site", "Staging of atlas-shop", "Backups of the workstation", "A second dashboard"), agendaPage.rows.map { it.short })
        assertEquals("Confirm the 6 client ids", done.short)
        assertEquals("no agenda file", results.first { it.id == agenda[1].id }.let { assertFalse(it.ok); it.text })
        assertTrue("agenda" in ContractJson.decodeState(Fixtures.stateIdle).ops.orEmpty())
        val night = cmds.first { it.op == CmdOp.NIGHT }
        assertNull(night.arg)
        val nightRep = ContractJson.decodeNightReport(results.first { it.id == night.id }.text)
        assertEquals("2026-10-07", nightRep.date)
        // Contratto 1.46: il prompt di ogni voce della notte, null dove non c'è.
        assertNull(nightRep.timeline.single { it.id == "atlas-shop" }.prompt)
        assertEquals("Dati del registro dei pagamenti per la pagina mensile.", nightRep.timeline.single { it.id == "e5f6a7b8" }.prompt)
        val invite = cmds.single { it.op == CmdOp.PAIR_ADD }
        assertNull(invite.session); assertNull(invite.arg)
        val offer = ContractJson.decodePairAdd(results.first { it.id == invite.id }.text)
        assertEquals("482913", offer.code); assertEquals(1789211100L, offer.exp)
        val qr = it.pixelbox.cmwatch.pairing.PairQr.parse(offer.qr!!)!!
        assertTrue(qr.add); assertEquals(offer.exp, qr.e)
        assertTrue("pair_add" in ContractJson.decodeState(Fixtures.stateIdle).ops.orEmpty())
        val timeline = cmds.single { it.op == CmdOp.TIMELINE }
        assertNull(timeline.session); assertEquals("1789200000", timeline.arg)
        val tl = ContractJson.decodeTimeline(results.first { it.id == timeline.id }.text)
        assertEquals(1789200000L, tl.since); assertFalse(tl.more)
        assertEquals(listOf("ledger-api", "field-notes"), tl.sessions.map { it.session })
        // Il progetto nella stessa forma di state.sessions[].project, relativo alla radice dei progetti.
        assertEquals("work/clients/ledger-api", tl.sessions[0].project); assertFalse(tl.sessions[0].live)
        assertEquals(setOf("prompt", "test", "commit", "outcome", "task", "wait"), tl.sessions.flatMap { it.events }.map { it.kind }.toSet())   // 1.96: le attese nella cronologia
        val tests = tl.sessions[0].events.filter { it.kind == "test" }
        assertEquals(listOf(false, true), tests.map { it.ok }); assertEquals("3/4 OK, FAIL: A2 invoices", tests[0].ref)
        assertEquals("0f20786", tl.sessions[0].events.single { it.kind == "commit" }.ref)
        // Contratto 1.36: il prompt della web app ha `ref` "web".
        assertEquals(listOf("phone", "web"), tl.sessions[1].events.mapNotNull { it.ref })
        // Mai eventi dopo la richiesta, mai prima di `since`.
        tl.sessions.flatMap { it.events }.forEach { e -> assertTrue(e.at in tl.since..timeline.issued) }
        assertTrue("timeline" in ContractJson.decodeState(Fixtures.stateIdle).ops.orEmpty())
        val pdf = cmds.single { it.id == "6f1c2d3e-0180-4000-8000-000000000180" }
        assertEquals(CmdOp.REPORT, pdf.op); assertEquals("phone", pdf.device)
        assertEquals("sent Preventivo cliente.pdf to atlas-shop", results.first { it.id == pdf.id }.text)
        assertTrue(ContractJson.decodeState(Fixtures.stateIdle).share!!.any)
        val search = cmds.single { it.op == CmdOp.SEARCH }
        assertNull(search.session); assertEquals("tuesday", search.arg)
        val hits = ContractJson.decodeSearch(results.first { it.id == search.id }.text)
        assertEquals(5, hits.hits.size)
        assertEquals(setOf(true, false), hits.hits.map { it.live }.toSet())
        hits.hits.forEach { h -> assertEquals("tuesday", h.snippet.substring(h.match[0], h.match[1]).lowercase()) }
        assertTrue("search" in ContractJson.decodeState(Fixtures.stateIdle).ops.orEmpty())
        val projects = cmds.single { it.op == CmdOp.PROJECTS }
        assertNull(projects.session); assertNull(projects.arg)
        val page = ContractJson.decodeProjects(results.first { it.id == projects.id }.text)
        assertEquals(listOf("atlas-shop", "ledger-api", "orbit-docs", "field-notes", "sketchbook"), page.projects.map { it.name })
        assertNull(page.projects.last().lastUsed); assertFalse(page.more)
        assertTrue("projects" in ContractJson.decodeState(Fixtures.stateIdle).ops.orEmpty())
        val slash = cmds.filter { it.op == CmdOp.SLASH }
        assertEquals(listOf("compact" to "sent /compact to field-notes", "model" to "not allowed: model"), slash.map { c -> c.arg to results.first { it.id == c.id }.text })
        assertNotNull(slash[0].text)
        assertEquals(listOf("compact", "clear", "exit", "context", "cost"), ContractJson.decodeState(Fixtures.stateIdle).slash)
        assertEquals(listOf("file ready (image/png, 69 bytes)", "not in the transcript"), cmds.indices.filter { cmds[it].op == CmdOp.FILE }.map { i -> results.first { it.id == cmds[i].id }.text })
        val enc = ContractJson.encode(cmds[0])
        assertTrue(enc.contains("\"op\":\"answer\"")); assertTrue(enc.contains("\"arg\":\"1\""))
        assertEquals(cmds[0], ContractJson.json.decodeFromString(Cmd.serializer(), enc))
        assertEquals(results[3], ContractJson.decodeResult(ContractJson.json.encodeToString(CmdResult.serializer(), results[3])))
    }

    /**
     * Contratto 1.13 (Franz, 16/09 14:33, «Nuova sessione» dal polso): `launch` porta il primo messaggio in `text`, il
     * `/result` dice il nome della sessione nata in `session`, e ogni progetto ha `last_used` per ordinarli dal più recente.
     */
    @Test fun launchWithFirstMessageNamesTheNewSession() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val launch = cmds.single { it.op == CmdOp.LAUNCH && it.text != null }
        assertEquals("check the draft for typos", launch.text)
        val r = results.single { it.id == launch.id }
        assertTrue(r.ok); assertEquals("field-notes-2", r.session)
        // Senza `text` il cmd si scrive come prima: un relay vecchio non vede campi nuovi.
        assertFalse(ContractJson.encode(cmds[0]).contains("\"text\""))
        val state = ContractJson.decodeState(Fixtures.stateQuestion)
        assertEquals(1789210700L, state.projects.single { it.name == "atlas-shop" }.lastUsed)
    }

    /**
     * Contratto 1.52 (14/10): `answer` porta `question`, l'id della domanda a cui l'utente ha risposto. Con l'id della
     * domanda aperta il tasto si preme; con quello di una domanda precedente il relay rifiuta senza premere niente.
     */
    @Test fun anAnswerIsOnlyForItsQuestion() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val open = ContractJson.decodeState(Fixtures.stateQuestion).sessions.single { it.name == "ledger-api" }.question!!.id
        val withId = cmds.filter { it.question != null }
        assertEquals(2, withId.size)
        val (right, old) = withId.partition { it.question == open }
        assertTrue(results.single { it.id == right.single().id }.ok)
        val r = results.single { it.id == old.single().id }
        assertFalse(r.ok); assertEquals("ledger-api: the question changed after your answer, nothing was pressed", r.text)
        assertTrue(ContractJson.encode(right.single()).contains("\"question\":\"$open\""))
        // Senza `question` il cmd si scrive come prima: un relay vecchio non vede campi nuovi.
        assertFalse(ContractJson.encode(cmds[0]).contains("\"question\""))
    }

    @Test fun lastAsksForTheWholeReply() {
        // Contratto 1.4: «last» chiede al PC l'ultima risposta intera della sessione, per la lettura a voce.
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val last = cmds.single { ContractJson.encode(it).contains("\"op\":\"last\"") }
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
            .single { it.id == last.id }
        assertTrue(res.ok); assertTrue(res.text.isNotBlank())
        assertEquals(last, ContractJson.json.decodeFromString(Cmd.serializer(), ContractJson.encode(last)))
    }

    @Test fun reopenBringsBackAGoneSession() {
        // Contratto 1.9: «reopen» rilancia una sessione gone nella sua cartella; `resume` resta la spinta a una viva.
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val reopen = cmds.single { it.op == CmdOp.REOPEN }
        assertEquals("orbit-docs", reopen.session); assertNull(reopen.arg)
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
            .single { it.id == reopen.id }
        assertTrue(res.ok); assertTrue(res.text.startsWith("reopened orbit-docs"))
        assertTrue(ContractJson.encode(reopen).contains("\"op\":\"reopen\""))
    }

    // Contratto 1.11 (16/09): ogni sessione porta modello, effort e contesto, letti dalla sua trascrizione. I campi ci
    // sono sempre e valgono null quando non si leggono: una sessione sparita non ha trascrizione, quindi niente numeri.
    @Test fun sessionsCarryModelEffortAndContext() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val ledger = s.sessions.single { it.name == "ledger-api" }
        assertEquals("claude-opus-5[1m]", ledger.model!!.id)
        assertEquals("Opus 5", ledger.model!!.label)
        assertEquals("high", ledger.effort)
        assertEquals(62, ledger.context)
        val atlas = s.sessions.single { it.name == "atlas-shop" }
        assertEquals("Sonnet 5", atlas.model!!.label)
        assertEquals(18, atlas.context)
        val gone = s.sessions.single { it.state == SessionState.GONE }
        assertNull(gone.model); assertNull(gone.effort); assertNull(gone.context)
    }

    // Contratto 1.16 (25/09): ogni sessione porta `low_priority` («off», «offered», «active», null se non si legge) e
    // `goal` (la condizione di completamento data con /goal: testo e da quando, null senza obiettivo).
    @Test fun sessionsCarryLowPriorityAndGoal() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val atlas = s.sessions.single { it.name == "atlas-shop" }
        assertEquals("active", atlas.lowPriority)
        assertEquals("All checkout tests green and the release tagged", atlas.goal!!.text)
        assertEquals(1789210700L, atlas.goal!!.since)
        assertEquals(false, atlas.goal!!.met)
        assertEquals("offered", s.sessions.single { it.name == "ledger-api" }.lowPriority)
        val notes = s.sessions.single { it.name == "field-notes" }
        assertEquals("off", notes.lowPriority); assertNull(notes.goal)
        val gone = s.sessions.single { it.state == SessionState.GONE }
        assertNull(gone.lowPriority); assertNull(gone.goal)
        // senza i campi (relay precedente) restano null
        val old = ContractJson.decodeState(Fixtures.stateQuestion.replace("\"low_priority\": \"active\",", "").replace(Regex(",\\s*\"goal\": \\{[^}]*\\}"), ""))   // `goal` è l'ultima chiave: via anche la virgola prima
        assertNull(old.sessions.single { it.name == "atlas-shop" }.lowPriority)
        assertNull(old.sessions.single { it.name == "atlas-shop" }.goal)
    }

    @Test fun unknownKeysAreIgnored() {
        val s = ContractJson.decodeState(Fixtures.stateIdle.replaceFirst("\"host\"", "\"extra\": 1, \"host\""))
        assertEquals("crostini-demo", s.host)
    }

    // Contratto 1.17 (29/09, richiesta R3 dell'app): la coda della notte modificabile. `night.items` c'è sempre per un
    // relay che la supporta, anche vuota; assente = relay precedente, e l'app lo dice invece di fallire.
    @Test fun nightQueueItemsAndCommands() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val items = s.night.items!!
        assertEquals(listOf("a3f09c1e", "7b21d4e8"), items.map { it.id })
        assertEquals("ledger-api", items[1].name); assertNull(items[1].started); assertEquals(1789210620L, items[1].added)
        assertTrue(items.all { !it.prompt.contains("…") && !it.prompt.contains("\n") })
        assertEquals(emptyList<NightItem>(), ContractJson.decodeState(Fixtures.stateStale).night.items)
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val add = cmds.single { it.op == CmdOp.NIGHT_ADD && it.arg != RecapAgenda.NIGHT_TARGET }
        assertEquals("/home/demo/workspaces/work/clients/ledger-api", add.arg); assertTrue(add.text!!.isNotBlank())
        assertEquals("7b21d4e8", res.single { it.id == add.id }.job)
        val remove = cmds.single { it.op == CmdOp.NIGHT_REMOVE }
        assertEquals("5d0e6b92", remove.arg); assertTrue(res.single { it.id == remove.id }.ok)
        assertTrue(ContractJson.encode(add).contains("\"op\":\"night_add\""))
    }

    @Test fun olderRelayHasNoNightItems() {
        val old = Fixtures.stateStale.replace(Regex(",\\s*\"items\"\\s*:\\s*\\[\\s*\\]"), "")
        assertNull(ContractJson.decodeState(old).night.items)
    }

    // Contratto 1.49 (09/10, scelta A di Franz delle 21:27): la notte della master. `night_add` con arg «master», `night.master`
    // nello stato, e l'item della master con nome «master» e la radice come cartella; un relay precedente non ha il flag.
    @Test fun nightToTheMaster() {
        val idle = ContractJson.decodeState(Fixtures.stateIdle)
        assertTrue(idle.night.master)
        val item = idle.night.items!!.single()
        assertEquals("master", item.name); assertEquals("c41d9e07", item.id); assertNull(item.started)
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val add = cmds.single { it.op == CmdOp.NIGHT_ADD && it.arg == RecapAgenda.NIGHT_TARGET }
        val r = res.single { it.id == add.id }
        assertTrue(r.ok); assertEquals("7b21d4e8", r.job); assertTrue(r.text.startsWith("queued for tonight: master"))
        val old = Fixtures.stateIdle.replace(Regex(",\\s*\"master\"\\s*:\\s*true"), "")
        assertTrue(!ContractJson.decodeState(old).night.master)
    }

    // Contratto 1.18 (29/09, richiesta R4): diario delle 20:00 e resoconto della notte arrivano anche all'app come eventi.
    @Test fun recapAndNightReportEvents() {
        val ev = ContractJson.decodeEvents(Fixtures.read("events-sample.json"))
        val recap = ev.single { it.kind == EventKind.RECAP }
        assertNull(recap.session); assertNull(recap.account); assertTrue(recap.body.isNotBlank()); assertNotNull(recap.ref)
        val night = ev.single { it.kind == EventKind.NIGHT_REPORT }
        assertTrue(night.title.isNotBlank()); assertNotNull(night.ref)
        assertTrue(ev.any { it.kind == EventKind.QUOTA && it.title == "quota personal back" })   // 1.78: il titolo nudo, l'icona dal kind
        assertTrue(ev.all { !it.body.contains("…") && !it.title.contains("\n") })
    }

    // Contratto 1.19 (29/09, richiesta R5): «Condividi» verso una sessione. `share` nello stato dice che il relay lo supporta.
    /** Contratto 1.77: `done` riuscito, senza registro, con una data sbagliata. */
    @Test fun doneResultsOfTheFixture() {
        val results = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val ok = results.first { it.id.endsWith("0760") }; val page = ContractJson.decodeDone(ok.text)
        assertEquals("release", page.rows[0].proof!!.kind); assertEquals("v2.3", page.rows[0].proof!!.value); assertEquals("check", page.rows[3].proof!!.kind); assertNull(page.rows[3].proof!!.url)
        assertEquals(false, page.more); assertEquals("personal/atlas-shop", page.rows[0].project)
        // Contratto 1.79: il titolo breve anche sulle righe chiuse.
        assertTrue(page.rows.all { !it.short.isNullOrBlank() && it.short!!.length <= 28 })
        assertEquals("no task registry", results.first { it.id.endsWith("0761") }.text); assertEquals("ieri: not a date YYYY-MM-DD", results.first { it.id.endsWith("0762") }.text)
        assertTrue(results.filter { it.id.endsWith("0761") || it.id.endsWith("0762") }.none { it.ok })
    }

    @Test fun reportAndShareSignal() {
        assertEquals(10_000_000, ContractJson.decodeState(Fixtures.stateIdle).share!!.maxBytes)
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val reports = cmds.filter { it.op == CmdOp.REPORT }
        // Contratto 1.28: il quarto è un file, un PDF. Contratto 1.48: il quinto e il sesto a pezzi, uno riuscito e uno rifiutato.
        // Contratto 1.50: il settimo e l'ottavo con due id (ShareMultiTest).
        assertEquals(8, reports.size)
        assertEquals("sent notes.txt to atlas-shop", res.single { it.id == reports[4].id }.text); assertFalse(res.single { it.id == reports[5].id }.ok)
        assertNotNull(reports[0].arg); assertNull(reports[1].arg)
        assertEquals("sent to field-notes", res.single { it.id == reports[1].id }.text)
        assertFalse(res.single { it.id == reports[2].id }.ok)
    }

    @Test fun olderRelayHasNoShare() {
        val old = Fixtures.stateIdle.replace(Regex(",\\s*\"share\"\\s*:\\s*\\{[^}]*\\}"), "")
        assertNull(ContractJson.decodeState(old).share)
    }

    /** Contratto 1.22: `device` dice al relay se il prompt arriva dal telefono o dall'orologio; assente, non si scrive. */
    @Test fun promptCarriesTheDevice() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmd = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }.last { it.op == CmdOp.PROMPT && it.device == "phone" }
        assertEquals("phone", cmd.device)
        assertTrue(ContractJson.encode(cmd).contains("\"device\":\"phone\""))
        assertFalse(ContractJson.encode(cmd.copy(device = null)).contains("device"))
    }

    // Contratto 1.36: la web app manda `device` "web".
    @Test fun webPromptCarriesItsDevice() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmd = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }.first { it.op == CmdOp.PROMPT && it.device == "web" }   // dalla 1.73 c'è anche il prompt della pagina Studio
        assertEquals("web", cmd.device); assertEquals("web", cmd.by)
    }

    // Contratto 1.37: consiglio, sessioni finite e approvazioni; i campi assenti prendono i default.
    @Test fun masterServiceFields() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val atlas = s.sessions.first { it.name == "atlas-shop" }
        assertEquals("claude-fable-5-1", atlas.advice?.model)
        assertEquals(36000L, atlas.advice?.switchCostTokens)
        assertEquals("next_task", atlas.advice?.whenToSwitch)
        assertTrue(atlas.advice!!.differs)
        assertFalse(atlas.finished)
        assertNull(atlas.duplicateOf)
        assertTrue(s.sessions.first { it.name == "field-notes" }.finished)
        assertEquals(listOf("atlas-release-2-4"), s.approvals.map { it.task })
        assertTrue(s.approvals.single().deploy)
        assertTrue(ContractJson.decodeState(Fixtures.stateStale).approvals.isEmpty())
    }

    // Contratto 1.38: i Prossimi che sbloccano, col «!» già tolto dal relay.
    @Test fun nextStepsCarryTheBlockingOnes() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        assertEquals(listOf(NextStep("ok to deploy on staging", true), NextStep("review the test seeds", false)), s.sessions.first { it.name == "atlas-shop" }.nextSteps)
        assertNull(s.sessions.first { it.name == "field-notes" }.nextSteps)
    }
}
