package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DecideTest {
    private val sample = File("../contract/cmd-result-sample.json").readText()
    private val root = kotlinx.serialization.json.Json.parseToJsonElement(sample) as kotlinx.serialization.json.JsonObject
    private fun textOf(op: String): String {
        val cmds = root["cmd"] as kotlinx.serialization.json.JsonArray
        val id = cmds.first { (it as kotlinx.serialization.json.JsonObject)["op"].toString() == "\"$op\"" }.let { (it as kotlinx.serialization.json.JsonObject)["id"].toString().trim('"') }
        val r = (root["result"] as kotlinx.serialization.json.JsonArray).first { (it as kotlinx.serialization.json.JsonObject)["id"].toString().trim('"') == id } as kotlinx.serialization.json.JsonObject
        return (r["text"] as kotlinx.serialization.json.JsonPrimitive).content
    }

    private fun rel(tag: String, at: Long) = AppUpdate.Release(tag, tag, at, true, "", 0, null, "")

    @Test fun banco_prova_laBuildSiConfrontaColloInstallato() {
        val rs = listOf(rel("nuova", 200), rel("vecchia", 100))
        assertEquals(BancoProva.Build.INSTALLED, BancoProva.build("vecchia", rs, 150))
        assertEquals(BancoProva.Build.INSTALLED, BancoProva.build("vecchia", rs, 100))
        assertEquals(BancoProva.Build.TO_INSTALL, BancoProva.build("nuova", rs, 150))
        assertEquals(BancoProva.Build.UNKNOWN, BancoProva.build("altra", rs, 150))
    }

    @Test fun banco_prova_laFixtureSiLeggeETutteOkMandaGliIdMostrati() {
        val p = ContractJson.decodeProve(textOf("prove"))
        assertEquals(2, p.n)
        assertEquals("41,42", BancoProva.allOk(p.groups.single()))
        assertEquals("checkout", p.groups.single().prove[0].dove)
        assertEquals(38L, p.ko.single().id)
    }

    @Test fun banco_prova_soloLeSchermateConosciute() {
        assertEquals("search", BancoProva.screen(" search "))
        assertNull(BancoProva.screen("checkout"))
        assertNull(BancoProva.screen(null))
    }

    @Test fun cinque_minuti_laFixtureNellOrdineDelRelayEQuanteNeVuoi() {
        val p = ContractJson.decodeDecisions(textOf("decisioni"))
        val keys = p.groups.map { it.key }
        assertEquals(keys.take(1), CinqueMinuti.next(p, emptySet(), 1).map { it.key })
        assertEquals(keys.drop(1), CinqueMinuti.next(p, setOf(keys[0]), 5).map { it.key })
        assertEquals(emptyList<String>(), CinqueMinuti.next(p, keys.toSet(), 5).map { it.key })
    }

    @Test fun cinque_minuti_cosaFaIlTasto() {
        assertEquals(CinqueMinuti.Act.APPROVE, CinqueMinuti.act(DecisionItem(kind = "ok", task = "a")))
        assertEquals(CinqueMinuti.Act.DONE, CinqueMinuti.act(DecisionItem(kind = "pause", task = "a")))
        assertEquals(CinqueMinuti.Act.DONE, CinqueMinuti.act(DecisionItem(kind = "needs_you", session = "s")))
        assertEquals(CinqueMinuti.Act.NONE, CinqueMinuti.act(DecisionItem(kind = "needs_you")))
        assertEquals(CinqueMinuti.Act.NONE, CinqueMinuti.act(DecisionItem(kind = "altro", task = "a")))
        assertFalse(CinqueMinuti.canDrop("  ")); assertTrue(CinqueMinuti.canDrop("Fatto a voce"))
    }

    @Test fun ok_informato_quelloCheMancaNonSiInventa() {
        val none = OkInformato.lines(Approval("t", "T"))
        assertNull(none.untested); assertEquals(OkInformato.Undo.UNDECLARED, none.undo); assertNull(none.undoHow); assertNull(none.precedent)
        assertNull(OkInformato.also(Approval("t", "T")))
        val st = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())
        val a = st.approvals.single { it.untested != null }
        val l = OkInformato.lines(a)
        assertEquals("phone", l.untested); assertEquals(OkInformato.Undo.PARTLY, l.undo); assertEquals("revert", l.undoHow)
        assertEquals(listOf("atlas-seo-pages"), OkInformato.also(a))
        assertEquals(OkInformato.Undo.YES, OkInformato.lines(Approval("t", "T", undo = "si")).undo)
        assertEquals(OkInformato.Undo.NO, OkInformato.lines(Approval("t", "T", undo = "no")).undo)
        assertEquals(OkInformato.Undo.UNDECLARED, OkInformato.lines(Approval("t", "T", undo = "boh")).undo)
    }

    @Test fun chat_esito_soloLUltimaRigaCheEDavveroLEsito() {
        assertEquals(ChatEsito.Split("Fatto.\n", "Esito: pubblicato").let { it.copy(body = "Fatto.") }, ChatEsito.split("Fatto.\n\nEsito: pubblicato\n", "Esito"))
        val mid = "Esito: a metà\nAltro testo"
        assertEquals(ChatEsito.Split(mid, null), ChatEsito.split(mid, "Esito"))
        assertEquals(ChatEsito.Split("Esitone", null), ChatEsito.split("Esitone", "Esito"))
        assertEquals(ChatEsito.Split("", "Esito: solo"), ChatEsito.split("Esito: solo", "Esito"))
    }

    @Test fun chat_esito_ilTipoDallaVoce() {
        val e = ContractJson.decodeTranscript("""{"entries":[{"id":"a","role":"assistant","text":"x","outcome_type":"shipped"},{"id":"b","role":"assistant","text":"y"}],"more":false}""")
        assertEquals(Kpi.OutcomeType.SHIPPED, Kpi.OutcomeType.of(e.entries[0].outcomeType))
        assertEquals(Kpi.OutcomeType.NONE, Kpi.OutcomeType.of(e.entries[1].outcomeType))
    }

    private fun session(text: String?) = Session(id = "s", name = "demo", account = "a", project = "p", state = SessionState.IDLE, since = 0, headsUp = text?.let { HeadsUp(it, 5) })

    @Test fun heads_up_giaLoSoLaNascondeUnaNuovaRicompare() {
        val s = session("Usa un worktree")
        assertEquals("Usa un worktree", HeadsUpNote.visible(s, emptySet())?.text)
        val seen = setOf(HeadsUpNote.key("demo", "Usa un worktree"))
        assertNull(HeadsUpNote.visible(s, seen))
        assertEquals("Altra nota", HeadsUpNote.visible(session("Altra nota"), seen)?.text)
        assertNull(HeadsUpNote.visible(session(null), emptySet()))
        assertNull(HeadsUpNote.visible(session("  "), emptySet()))
    }

    @Test fun heads_up_dalContratto() {
        val st = ContractJson.decodeState("""{"v":1,"ts":1,"host":"pc","sessions":[{"id":"s","name":"n","account":"a","project":"p","state":"idle","since":0,"heads_up":{"text":"t","seen_at":9}}],"quota":{}}""")
        assertEquals(HeadsUp("t", 9), st.sessions[0].headsUp)
    }

    @Test fun chat_esito_ancheEsitoConLAppInInglese() {
        assertEquals("Esito: done", ChatEsito.split("x\nEsito: done", "Outcome").outcome)
        assertEquals("Outcome: done", ChatEsito.split("x\nOutcome: done", "Outcome").outcome)
    }

    @Test fun notte_due_mesi_ogniGiornoIlSuoMese() {
        val name = { m: java.time.Month -> m.name.lowercase() }
        assertEquals(NightTitle.Parts(31, "october", 1, "november"), NightTitle.parts(java.time.LocalDate.of(2026, 10, 31), java.time.LocalDate.of(2026, 11, 1), name))
        assertEquals(NightTitle.Parts(6, null, 7, "october"), NightTitle.parts(java.time.LocalDate.of(2026, 10, 6), java.time.LocalDate.of(2026, 10, 7), name))
    }

    @Test fun chat_esito_allineata_senzaPrefissoEIconaAlCentroDellaRiga() {
        assertEquals("fase B uscita", ChatEsito.text("Esito: fase B uscita", "Esito"))
        assertEquals("fase B uscita", ChatEsito.text("Esito: fase B uscita", "Outcome"))
        assertEquals("done", ChatEsito.text("Outcome: done", "Outcome"))
        assertEquals("Esito fatto", ChatEsito.text("Esito fatto", "Esito"))
        assertEquals(5f, ChatEsito.iconTop(28f, 18f), 0f)
        assertEquals(3f, ChatEsito.iconTop(24f, 18f), 0f)
    }

    @Test fun decisione_spiegata_laCardDiceCosaSiPropone() {
        val g = ContractJson.decodeDecisions(textOf("decisioni")).groups[0]
        val i = g.items[0]
        assertEquals("Order mails of atlas-shop", i.title); assertEquals("ok", i.asks)
        assertEquals("Proposal: move the checkout to the new layout, with the old one kept behind a flag.", i.planStart)
        val c = CinqueMinuti.card(g)
        assertEquals(listOf("With the mailbox the order mails and the receipts start", "mailbox for the shop"), c.head)
        assertEquals(CinqueMinuti.ItemText("Order mails of atlas-shop", i.planStart, "Needs the shop's mailbox"), c.items[0])
    }

    @Test fun decisione_spiegata_maiLoStessoTestoDueVolte() {
        val p = "Proposta del 28/10 22:22: aspetta l'ok di Franz"
        val g = DecisionGroup("k", title = p, items = listOf(DecisionItem(kind = "pause", task = "t", title = "Logo ambra su Play", reason = p, planStart = "Cambiare l'icona quando Google approva.")))
        val c = CinqueMinuti.card(g)
        assertEquals(listOf(p), c.head)
        assertEquals(CinqueMinuti.ItemText("Logo ambra su Play", "Cambiare l'icona quando Google approva.", null), c.items[0])
    }

    @Test fun decisione_spiegata_iTastiSecondoCosaSiAspetta() {
        val ok = DecisionItem(kind = "pause", task = "t", asks = "ok")
        assertEquals(listOf(CinqueMinuti.Btn.Proceed, CinqueMinuti.Btn.Discuss), CinqueMinuti.buttons(ok))
        assertFalse("una pausa non passa dall'ok del registro", CinqueMinuti.viaApprove(ok))
        assertTrue(CinqueMinuti.viaApprove(DecisionItem(kind = "ok", task = "t", asks = "ok")))
        assertEquals(listOf(CinqueMinuti.Btn.Choice("A"), CinqueMinuti.Btn.Choice("B")), CinqueMinuti.buttons(DecisionItem(kind = "ok", task = "t", asks = "choice", options = listOf("A", "B"))))
        assertEquals(listOf(CinqueMinuti.Btn.Done), CinqueMinuti.buttons(DecisionItem(kind = "needs_you", session = "s", asks = "pc")))
        assertEquals(listOf(CinqueMinuti.Btn.Done, CinqueMinuti.Btn.Discuss), CinqueMinuti.buttons(DecisionItem(kind = "needs_you", session = "s", asks = "other")))
        assertEquals("senza asks il tasto di prima", listOf(CinqueMinuti.Btn.Done), CinqueMinuti.buttons(DecisionItem(kind = "pause", task = "t")))
        assertEquals(listOf(CinqueMinuti.Btn.Proceed), CinqueMinuti.buttons(DecisionItem(kind = "ok", task = "t")))
    }

    // app-decidere-tasti-03-11 (Franz 03/11 14:22: «In da decidere non ci sono tasti per decidere»): 31 voci su 42 avevano solo Parliamone.
    @Test fun decidere_tasti_unaPausaSiDecideDalTelefono() {
        fun pause(reason: String, asks: String = "other", options: List<String>? = null) = DecisionItem(kind = "pause", task = "t", session = "s", asks = asks, reason = reason, options = options)
        val works = listOf(CinqueMinuti.Btn.Works, CinqueMinuti.Btn.Broken)
        assertEquals(works, CinqueMinuti.buttons(pause("Aspetta la prova di Franz sul telefono: versione finale")))
        assertEquals(works, CinqueMinuti.buttons(pause("Franz prova la pre-release tabellone-4-2026-11-02 e la PWA")))
        assertEquals(works, CinqueMinuti.buttons(pause("Seconda prova di Franz: pre-release in-cima-2")))
        assertEquals(works, CinqueMinuti.buttons(pause("Franz guarda la v12 (copia leggera mandata in chat)")))
        assertEquals(works, CinqueMinuti.buttons(pause("Waits for you to try build 0.4.12 on the phone")))
        assertEquals("una prova vince anche su asks ok", works, CinqueMinuti.buttons(pause("Aspetta la prova di Franz sul telefono: nomi, ordine e ok", asks = "ok")))
        assertEquals("le parole intere: «provato» o «approvare» non sono una prova", listOf(CinqueMinuti.Btn.Proceed, CinqueMinuti.Btn.Discuss), CinqueMinuti.buttons(pause("Aspetta l'ok di Franz per approvare il piano", asks = "ok")))
        assertEquals(listOf(CinqueMinuti.Btn.Done, CinqueMinuti.Btn.Discuss), CinqueMinuti.buttons(pause("Rimandato da Franz il 28/10 21:35")))
        assertEquals("al PC resta Fatto", listOf(CinqueMinuti.Btn.Done), CinqueMinuti.buttons(pause("Franz prova al PC", asks = "pc")))
        assertEquals(listOf(CinqueMinuti.Btn.Choice("A")), CinqueMinuti.buttons(pause("Aspetta la scelta di Franz", asks = "choice", options = listOf("A"))))
        // Una scelta senza le opzioni non si decide da qui: la card dice dove.
        val noOpts = pause("Aspetta la scelta di Franz sulla pulizia", asks = "choice")
        assertEquals(listOf(CinqueMinuti.Btn.Discuss), CinqueMinuti.buttons(noOpts))
        assertEquals("s", CinqueMinuti.chooseAt(noOpts))
        assertEquals(null, CinqueMinuti.chooseAt(pause("Aspetta la scelta", asks = "choice", options = listOf("A"))))
        assertEquals(null, CinqueMinuti.chooseAt(pause("Rimandato")))
    }

    // Contratto 1.107 (app-sezioni-accorpate-03-11): gli stessi casi in decide.test.ts.
    private fun pv(key: String, task: String, project: String, release: String?, since: Long, web: String? = null) =
        DecisionGroup(key, task, items = listOf(DecisionItem("pause", task, task, project = project, since = since, prova = Prova(project, release, web))), unblocks = listOf(task), weight = 1, where = "here", since = since, refs = listOf(task))

    @Test fun prove_fuse_laFixtureDaUnaSchedaEDecidiRestaSenzaLaProva() {
        val p = ContractJson.decodeDecisions(textOf("decisioni"))
        val c = ProveFuse.cards(p)
        assertEquals(1, c.size)
        assertEquals(Triple("personal/orbit-app", "home-2026-09-12", "0.2.1663"), Triple(c[0].project, c[0].release, c[0].web))
        assertEquals(listOf("5be07a3c9e42" to "New home"), c[0].rows.map { it.key to it.text })
        assertEquals(listOf("5be07a3c9e42"), ProveFuse.keys(c[0]))
        val d = ProveFuse.withoutTests(p)
        // Dalla fixture di supervisor 0.8.121 c'è anche il gruppo del login di work, qui.
        assertEquals(listOf("3f2a91c07d11", "a81c55e20b34", "5b0e1f7c2a93"), d.groups.map { it.key })
        assertEquals(3 to 2, d.n to d.here)
    }

    @Test fun prove_fuse_leProveSuperateStannoNellaPreReleasePiuNuova() {
        val p = ContractJson.decodeDecisions(textOf("decisioni"))
        val page = p.copy(groups = listOf(pv("k1", "a", "x/app", "r-1", 100), pv("k2", "b", "x/app", "r-2", 300, "0.2.9"), pv("k3", "c", "y/web", null, 200, "1.0.1")) + p.groups)
        val c = ProveFuse.cards(page)
        assertEquals(listOf(Triple("personal/orbit-app", "home-2026-09-12", "0.2.1663"), Triple("x/app", "r-2", "0.2.9"), Triple<String, String?, String?>("y/web", null, "1.0.1")), c.map { Triple(it.project, it.release, it.web) })
        assertEquals(listOf("k2", "k1"), c[1].rows.map { it.key })
        assertEquals(listOf("k2", "k1"), ProveFuse.keys(c[1]))
    }

    @Test fun prove_fuse_unGruppoMistoRestaInDecidi() {
        val base = pv("m", "a", "x/app", "r-1", 100)
        val mixed = base.copy(items = base.items + DecisionItem("pause", "z", since = 50))
        val page = DecisionsPage(groups = listOf(mixed))
        assertEquals(emptyList<ProveFuse.Card>(), ProveFuse.cards(page))
        assertEquals(listOf("m"), ProveFuse.withoutTests(page).groups.map { it.key })
    }
}

/** app-prove-segnalate-android-13-11: le segnalate si chiudono con «Ora va», e le note della versione stanno con le sue prove. */
class ProveSegnalateTest {
    @org.junit.Test fun aReportedTestClosesWithItsNumberAndLeavesItsRow() {
        val k = it.pixelbox.cmwatch.contract.ProveKo(id = 389, task = "app-x", text = "non va")
        org.junit.Assert.assertEquals(BancoProva.NowOk("389", "ko-389"), BancoProva.nowOk(k))
        org.junit.Assert.assertEquals(ItemRows.proveKo(k).key, BancoProva.nowOk(k).row)
    }

    // Il caso del 13/11: tre versioni che l'app non riconosce, senza prove a schermo: nessuna nota da sola.
    @org.junit.Test fun theNotesOfAVersionStayWithItsTests() {
        org.junit.Assert.assertFalse(BancoProva.installFirstNote(BancoProva.Build.UNKNOWN, 0))
        org.junit.Assert.assertFalse(BancoProva.installFirstNote(BancoProva.Build.TO_INSTALL, 0))
        org.junit.Assert.assertTrue(BancoProva.installFirstNote(BancoProva.Build.TO_INSTALL, 2))
        org.junit.Assert.assertFalse(BancoProva.installFirstNote(BancoProva.Build.INSTALLED, 2))
        org.junit.Assert.assertTrue(BancoProva.unknownNoteInCard(BancoProva.Build.UNKNOWN))
        org.junit.Assert.assertFalse(BancoProva.unknownNoteInCard(BancoProva.Build.INSTALLED))
        org.junit.Assert.assertFalse(BancoProva.unknownNoteInCard(BancoProva.Build.TO_INSTALL))
    }
}

/** app-sessione-bg-avviso-14-11 (contratto 1.146): l'avviso su /logout solo nella scheda di una sessione viva lanciata con --bg. */
class BgSessionTest {
    private fun state(extra: String, st: String = "idle") = it.pixelbox.cmwatch.contract.ContractJson.decodeState(
        """{"v":1,"ts":1,"host":"pc","sessions":[{"id":"s","name":"n","account":"a","project":"p","state":"$st","since":0$extra}],"quota":{}}""").sessions[0]

    @org.junit.Test fun aBgSessionWarnsAndOneWithoutTheFieldDoesNot() {
        org.junit.Assert.assertTrue(BgSession.warns(state(""","bg":true""")))
        org.junit.Assert.assertTrue(BgSession.warns(state(""","bg":true""", "busy")))
        org.junit.Assert.assertFalse(BgSession.warns(state("")))
        org.junit.Assert.assertFalse(BgSession.warns(state(""","bg":false""")))
        org.junit.Assert.assertFalse(BgSession.warns(state(""","bg":true""", "gone")))
    }
}
