package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.CommitmentPause
import it.pixelbox.cmwatch.contract.CommitmentStep
import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.CommitmentWaits
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.DonePage
import it.pixelbox.cmwatch.rules.Lavori.Bar
import it.pixelbox.cmwatch.rules.Lavori.Kind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** La pagina unica «Lavori» (mockup approvato da Franz il 22/10 alle 12:43): i quattro blocchi dal contratto 1.76, dall'agenda e dagli eventi. */
class LavoriTest {
    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)
    private val stale = ContractJson.decodeState(Fixtures.stateStale)
    private val night = ContractJson.decodeNightReport(Fixtures.read("night-report-sample.json"))
    private val events = ContractJson.decodeEvents(Fixtures.events)
    private val agenda: AgendaPage = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray
        .first { it.jsonObject.getValue("id").jsonPrimitive.content.endsWith("0386") }
        .let { ContractJson.decodeAgenda(it.jsonObject.getValue("text").jsonPrimitive.content) }
    /** Il giorno dopo l'`until` della riga sospesa dell'agenda (2026-10-12): conta come aperta. */
    private val today: LocalDate = LocalDate.of(2026, 10, 13)
    private val okRelease = Approval("ledger-release-14", "Release 1.4 di ledger-api", "tag v1.4 e push", "github.com/example/ledger-api", requestedAt = 1791332460)
    private val okLoose = Approval("loose-task", "Loose ok", "push", "master", requestedAt = 1789214000)
    private val st = idle.copy(commitments = night.commitments + idle.commitments!!, approvals = listOf(okRelease, okLoose))
    private val eventDay: LocalDate = Instant.ofEpochSecond(1789199700).atZone(zone).toLocalDate()

    @Test fun theFixturesGiveTheFourBlocks() {
        val fy = Lavori.forYou(st, agenda, idle.ts, today)
        assertEquals(listOf("Loose ok", "Release 1.4 di ledger-api"), fy.ok.map { it.title })
        assertEquals(okLoose, fy.ok[0].approval); assertNull(fy.ok[0].commitment); assertEquals("master", fy.ok[0].who)
        assertEquals("ledger-release-14", fy.ok[1].commitment!!.task); assertEquals(okRelease, fy.ok[1].approval); assertEquals("ledger-api", fy.ok[1].who)
        assertEquals(listOf("Confirm the 6 client ids with a candidate"), fy.outside.map { it.title })
        assertEquals(agenda.rows[0].ref, fy.outside[0].who); assertEquals(Kind.OUTSIDE, fy.outside[0].kind); assertEquals(agenda.rows[0], fy.outside[0].agenda)
        assertEquals(listOf("Move the docs site to the new host"), fy.claude.map { it.title }); assertEquals(Kind.CLAUDE, fy.claude[0].kind)
        assertEquals(4, fy.total)
        val ip = Lavori.inProgress(st, agenda, idle.ts, zone, today)
        // Mockup del 22/10 21:16: tre gruppi per chi deve muoversi. Chi aspetta una sessione è al lavoro, chi aspetta Franz no.
        assertEquals(listOf("notes-gantt-apk", "ledger-migrations-011", "atlas-seo-pages"), ip.working.map { it.works.single().row!!.c.task })
        assertTrue(ip.working.all { it.bars.size == 1 && !it.bars[0].dashed })
        assertEquals(listOf("ledger-release-14"), ip.you.map { it.works.single().row!!.c.task })
        assertEquals("l'ok a: tag v1.4 e push", ip.you[0].next); assertEquals(1791332460L, ip.you[0].since)
        assertEquals(listOf("Staging of atlas-shop not reachable from the CLI"), ip.others.map { it.agenda!!.title })
        assertEquals("Staging of atlas-shop", ip.others[0].short); assertEquals(5, ip.total)
        val done = Lavori.done(events, zone, eventDay)
        assertEquals(1, done.size); assertEquals(eventDay, done[0].date)
        val row = done[0].rows.single()
        assertEquals("atlas-release-2-4", row.task); assertEquals("Release 2.4 of atlas-shop", row.title)
        assertEquals("atlas-shop", row.session); assertEquals(1789199700L, row.at); assertNull(row.proof)
    }

    @Test fun anOkIsNeverListedTwice() {
        val fy = Lavori.forYou(idle.copy(commitments = night.commitments, approvals = listOf(okRelease)), null, idle.ts, today)
        assertEquals(1, fy.ok.size); assertEquals(okRelease, fy.ok[0].approval); assertEquals("ledger-release-14", fy.ok[0].commitment!!.task)
    }

    @Test fun aPauseWithoutPersonStaysInProgress() {
        val s = stale.copy(commitments = listOf(stale.commitments!!.single().copy(waits = CommitmentWaits("person", "orbit-docs"))))   // relay 1.76
        assertEquals(0, Lavori.forYou(s, null, stale.ts, today).total)
        val ip = Lavori.inProgress(s, null, stale.ts, zone, today)
        // In pausa in attesa di una sessione = al lavoro (in coda), col motivo della pausa come riga sotto e la barra tratteggiata.
        assertEquals(listOf("orbit-docs-site"), ip.working.map { it.works.single().row!!.c.task }); assertTrue(ip.you.isEmpty())
        assertEquals("after the client's review", ip.working[0].reason); assertTrue(ip.working[0].bars.any { it.dashed })
    }

    @Test fun aPauseThatWaitsThePersonIsForYou() {
        val c = stale.commitments!!.single()   // 1.77: waits.on = person, paused.at = l'inizio vero
        val fy = Lavori.forYou(stale, null, stale.ts, today)
        assertTrue(fy.ok.isEmpty())
        val row = fy.outside.single()
        assertEquals("after the client's review", row.title); assertEquals("orbit-docs", row.who); assertEquals(1789100000L, row.at); assertEquals(c, row.commitment)
        val ip = Lavori.inProgress(stale, null, stale.ts, zone, today)
        assertEquals(listOf("orbit-docs-site"), ip.you.map { it.works.single().row!!.c.task }); assertTrue(ip.working.isEmpty())
        assertEquals("after the client's review", ip.you[0].reason); assertEquals(1789100000L, ip.you[0].since)
    }

    @Test fun theAxisCoversSevenDaysEndingTomorrow() {
        val ax = Lavori.axis(idle.ts, zone)   // 12/09/2026 14:00 a Roma
        assertEquals(LocalDate.of(2026, 9, 6).atStartOfDay(zone).toEpochSecond(), ax.start)
        assertEquals(LocalDate.of(2026, 9, 13).atStartOfDay(zone).toEpochSecond(), ax.end)
        assertEquals(listOf("6", "7", "8", "9", "10", "11", "12"), ax.days.map { it.first })
        assertEquals(1.0 / 7, ax.days[1].second, 1e-9)
        assertEquals((6 * 24 + 14).toDouble() / (7 * 24), ax.now, 1e-9)
    }

    @Test fun aBarOlderThanTheAxisStartsAtTheEdge() {
        val ax = Lavori.axis(idle.ts, zone)
        val c = idle.commitments!![1].copy(approvedAt = ax.start - 86400)
        assertEquals(listOf(Bar(0.0, ax.now, false)), Lavori.bars(c, ax, idle.ts))
    }

    @Test fun aPausedBarIsSolidThenDashed() {
        val ax = Lavori.axis(stale.ts, zone)
        val f = { t: Long -> (t - ax.start).toDouble() / (ax.end - ax.start) }
        val fixture = stale.commitments!!.single()   // ok 1789120000, pausa dal 1789100000: in pausa da prima dell'ok, tutta tratteggiata
        assertEquals(listOf(Bar(f(1789120000), f(stale.ts), true)), Lavori.bars(fixture, ax, stale.ts))
        val c = fixture.copy(paused = fixture.paused!!.copy(at = 1789190000))
        assertEquals(listOf(Bar(f(1789120000), f(1789190000), false), Bar(f(1789190000), f(stale.ts), true)), Lavori.bars(c, ax, stale.ts))
        val old = c.copy(approvedAt = ax.start - 2 * 86400, paused = c.paused!!.copy(at = ax.start - 86400))
        assertEquals(listOf(Bar(0.0, f(stale.ts), true)), Lavori.bars(old, ax, stale.ts))
    }

    @Test fun aFreshBarHasAMinimumWidth() {
        val ax = Lavori.axis(idle.ts, zone)
        val b = Lavori.bars(idle.commitments!![1].copy(approvedAt = idle.ts - 10), ax, idle.ts).single()
        assertEquals(Lavori.MIN_BAR, b.to - b.from, 1e-9); assertTrue(b.to <= 1.0)
    }

    @Test fun doneKeepsTheLastCheckOfATask() {
        val later = Event(key = "x_1", kind = EventKind.TASK_CHECK, session = "ledger-api", ts = 1789200360, title = "✓ ledger-migrations-011: check green", body = "Migrations 008-011 on staging\nall green", ref = "ledger-migrations-011")
        val earlier = Event(key = "x_0", kind = EventKind.TASK_CHECK, session = "atlas-shop", ts = 1789199000, title = "✓ atlas-release-2-4: check green", body = "Release 2.4 of atlas-shop", ref = "atlas-release-2-4")
        val day = Lavori.done(events + later + earlier, zone, eventDay).single()
        assertEquals(listOf("ledger-migrations-011" to 1789200360L, "atlas-release-2-4" to 1789199700L), day.rows.map { it.task to it.at })
        assertEquals("Migrations 008-011 on staging", day.rows[0].title)
        // Fuori dai sette giorni non si vede; un giorno dopo l'evento è ancora dentro.
        assertTrue(Lavori.done(events, zone, eventDay.plusDays(7)).isEmpty())
        assertEquals(1, Lavori.done(events, zone, eventDay.plusDays(1)).size)
    }

    /** Contratto 1.77: l'op `done` porta la prova e i giorni prima; i giorni senza righe sono richiusi col conteggio. */
    @Test fun theDoneOpGivesTheProofAndTheDaysBefore() {
        val raw = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray
            .first { it.jsonObject.getValue("id").jsonPrimitive.content.endsWith("0760") }
            .jsonObject.getValue("text").jsonPrimitive.content
        val page: DonePage = ContractJson.decodeDone(raw)
        assertEquals("2026-02-10", page.date); assertEquals(4, page.rows.size); assertEquals(listOf("2026-02-09" to 3, "2026-02-07" to 1), page.days.map { it.date to it.n })
        val days = Lavori.doneFromPages(mapOf(page.date to page), zone)
        assertEquals(listOf("2026-02-10", "2026-02-09", "2026-02-07"), days.map { it.date.toString() })
        assertEquals(listOf(4, 3, 1), days.map { it.n }); assertEquals(listOf(4, 0, 0), days.map { it.rows.size })
        val rows = days[0].rows
        assertEquals(listOf("atlas-release-2-3", "ledger-migrations-010", "notes-night-report", "orbit-docs-lint"), rows.map { it.task })
        assertEquals(listOf("release v2.3", "commit 9f8e7d6", "docs/notte/2026-02-10-field-notes.md", null), rows.map { it.proof })
        assertEquals("https://github.com/example/atlas-shop/releases/tag/v2.3", rows[0].url); assertNull(rows[2].url)
        assertEquals(listOf("green", "green", "cancelled", "failed"), rows.map { it.outcome })
        assertEquals("Release 2.3 of atlas-shop", rows[0].title); assertEquals("atlas-shop", rows[0].session); assertNull(rows[2].session); assertEquals(1789211400L, rows[0].at)
        // Un giorno prima caricato dopo si unisce; il più recente resta primo.
        val older = page.copy(date = "2026-02-09", rows = page.rows.take(3), days = emptyList())
        val merged = Lavori.doneFromPages(mapOf(page.date to page, older.date to older), zone)
        assertEquals(listOf(4, 3, 1), merged.map { it.n }); assertEquals(3, merged[1].rows.size)
    }

    /** La riga di home «fatto oggi N»: con la pagina di oggi dell'op `done` conta le sue righe, altrimenti i `task_check` verdi di oggi. */
    @Test fun theHomeCountsTodayFromTheDoneOpWhenItIsLoaded() {
        val raw = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray
            .first { it.jsonObject.getValue("id").jsonPrimitive.content.endsWith("0760") }
            .jsonObject.getValue("text").jsonPrimitive.content
        val page: DonePage = ContractJson.decodeDone(raw)
        val pageDay = LocalDate.parse(page.date)
        assertEquals(4, Lavori.doneToday(mapOf(page.date to page), emptyList(), zone, pageDay))
        // Senza la pagina di oggi si torna agli eventi: nella fixture un solo controllo verde quel giorno, niente il giorno della pagina.
        assertEquals(1, Lavori.doneToday(emptyMap(), events, zone, eventDay))
        assertEquals(0, Lavori.doneToday(emptyMap(), events, zone, pageDay))
        assertEquals(1, Lavori.doneToday(mapOf(page.date to page), events, zone, eventDay))
    }

    @Test fun oldRelayHasNoRows() {
        // Un relay prima dell'1.76 non manda né impegni né ok (la fixture 1.81 ne porta uno).
        val s = idle.copy(commitments = null, approvals = emptyList())
        assertEquals(0, Lavori.forYou(s, null, idle.ts, today).total)
        val ip = Lavori.inProgress(s, null, idle.ts, zone, today)
        assertTrue(ip.working.isEmpty() && ip.you.isEmpty() && ip.others.isEmpty() && ip.total == 0)
        assertEquals(1, Lavori.done(events, zone, eventDay).size)
    }

    // Contratto 1.79: `short` del relay quando c'è; altrimenti dal titolo: via il prefisso «nome: », parole intere entro 28, mai un articolo o una parentesi aperta in coda.
    @Test fun theShortTitleComesFromTheRelayOrFromTheTitle() {
        assertEquals("Migrations 008-011", Lavori.shortTitle("ledger-api: migrations 008-011 on staging", "Migrations 008-011"))
        assertEquals("con un suggerimento visibile", Lavori.shortTitle("supervisor-app: con un suggerimento visibile nella casella di testo, Tab (web) lo copia nel campo"))
        assertEquals("niente emoji nell'app", Lavori.shortTitle("supervisor-app: niente emoji nell'app e nelle notifiche, solo icone vettoriali"))
        assertEquals("Albo Chiaro fase 1", Lavori.shortTitle("Albo Chiaro fase 1 (task 1-8): repository e CI, verifiche, database"))
        assertEquals("Release 1.4 di ledger-api", Lavori.shortTitle("Release 1.4 di ledger-api"))
        assertEquals("Release 1.4 di ledger-api", Lavori.shortTitle("Release 1.4 di ledger-api", "  "))
        assertEquals("x".repeat(28), Lavori.shortTitle("x".repeat(40)))
        assertEquals("Lavori: In corso leggibile", Lavori.shortTitle("Lavori: In corso leggibile, la sezione è un muro di testo", "Lavori: In corso leggibile"))
    }

    // Il caso reale della sera del 22/10: 2 al lavoro (uno in coda dietro l'altro), 7 che aspettano Franz con Albo Chiaro ×5 in una riga, 2 fermi su terzi.
    @Test fun theRealCaseGroupsByWhoHasToMove() {
        val now = 1792700000L // 22/10/2026 ~04:13 UTC: non conta, le date stanno sotto
        fun c(task: String, title: String, session: String, waitsOn: String, paused: CommitmentPause? = null, approvedAt: Long? = null, next: String? = null, short: String? = null) =
            Commitment(task, title, session = session, state = if (paused != null) "assigned" else "running", approved = true, approvedAt = approvedAt, ageS = 3600, idleS = 600,
                next = next, waits = CommitmentWaits(if (waitsOn == "person") session else "person", waitsOn), paused = paused, short = short)
        val albo = CommitmentPause("Albo Chiaro sospeso dal 18/10 a stato pulito: aspetta da Franz la casella Aruba albo@, l'utente con password e le chiavi Gemini e OCR", 1792274400, "master")
        val list = listOf(
            c("niente-emoji-22-10", "supervisor-app: niente emoji nell'app e nelle notifiche", "supervisor-app", "supervisor-app", approvedAt = 1792695060, short = "Niente emoji nell'app"),
            c("tab-suggerimento-22-10", "supervisor-app: con un suggerimento visibile nella casella di testo, Tab", "supervisor-app", "supervisor-app",
                paused = CommitmentPause("in coda dopo niente-emoji-22-10", 1792695100, "master"), approvedAt = 1792695060),
            c("albo-fase1-20261017", "Albo Chiaro fase 1 (task 1-8): repository e CI", "albo-crotone", "person", paused = albo, approvedAt = 1792200000, short = "Albo Chiaro fase 1"),
            c("albo-fase2-20261017", "Albo Chiaro fase 2 (task 9-15): testo, OCR", "albo-crotone", "person", paused = albo, approvedAt = 1792200000, short = "Albo Chiaro fase 2"),
            c("albo-fase3-20261017", "Albo Chiaro fase 3 (task 16-19): appalti", "albo-crotone", "person", paused = albo, approvedAt = 1792200000, short = "Albo Chiaro fase 3"),
            c("albo-provincia-20261017", "Albo Chiaro: albo della Provincia di Crotone", "albo-crotone", "person", paused = albo, approvedAt = 1792200000, short = "Albo Chiaro: Provincia"),
            c("albo-regione-20261017", "Albo Chiaro: provvedimenti della Regione Calabria", "albo-crotone", "person", paused = albo, approvedAt = 1792200000, short = "Albo Chiaro: Regione"),
            c("lavori-gerarchia-22-10", "Lavori: In corso leggibile", "supervisor-app", "person", approvedAt = null, next = "l'ok a: il mockup", short = "Lavori: In corso leggibile"),
            c("phone-fix-0310-sera", "App telefono: menu che si chiudono, volo card", "claude-master-phone", "person",
                paused = CommitmentPause("compito del 03/10: da confermare con Franz se è superato", 1792622164, "master"), approvedAt = 1791050000, short = "App telefono del 03/10"),
        )
        val ag = AgendaPage(listOf(
            AgendaRow("aperto", "personale", "terzi", "Revisione Chrome Web Store di chrome-bridge 1.12.0 con move_tab", "chrome-bridge 1.12.0", key = "a1", short = "Revisione Chrome Web Store"),
            AgendaRow("aperto", "personale", "terzi", "chrome-bridge, issue #2: risposta di jeet-dhandha", "", detail = "risposta di jeet-dhandha\nseconda riga", until = "2026-10-25", key = "a2"),
        ), owner = "franz")
        val state = idle.copy(commitments = list, approvals = emptyList(), ts = now)
        val ip = Lavori.inProgress(state, ag, now, zone, LocalDate.of(2026, 10, 22))
        assertEquals(listOf("Niente emoji nell'app", "con un suggerimento visibile"), ip.working.map { it.short })
        assertEquals(listOf(null, "in coda dopo niente-emoji-22-10"), ip.working.map { it.reason })
        assertEquals(listOf(1792695060L, 1792695060L), ip.working.map { it.since })
        assertTrue(ip.working[1].bars.any { it.dashed })
        // Gli ok da dare prima (si fanno con un tocco), poi le pause nell'ordine del relay.
        assertEquals(listOf("Lavori: In corso leggibile", "Albo Chiaro", "App telefono del 03/10"), ip.you.map { it.short })
        assertEquals(listOf(1, 5, 1), ip.you.map { it.n })
        assertEquals(albo.reason, ip.you[1].reason); assertEquals(1792274400L, ip.you[1].since); assertEquals("albo-crotone", ip.you[1].session)
        assertEquals("l'ok a: il mockup", ip.you[0].next); assertNull(ip.you[0].reason)
        assertEquals(listOf("Revisione Chrome Web Store", "chrome-bridge, issue #2"), ip.others.map { it.short })
        assertEquals("2026-10-25", ip.others[1].agenda!!.until)
        assertEquals(11, ip.total)
        assertEquals(listOf(Lavori.Who.WORKING, Lavori.Who.YOU, Lavori.Who.OTHERS), listOf(ip.working[0].who, ip.you[1].who, ip.others[0].who))
    }

    /** Strada B (Franz 23/10 23:08): una riga «Per te» di una sessione aperta sta nella scheda di quella sessione; le altre restano in lista. */
    @Test fun aRowOfAnOpenSessionSitsInItsCard() {
        val fy = Lavori.forYou(stale, null, stale.ts, today) // una pausa che aspetta Franz, sessione orbit-docs
        val placed = Lavori.place(fy, open = setOf("orbit-docs"))
        assertEquals(listOf("after the client's review"), placed.inCards.getValue("orbit-docs").map { it.title })
        assertTrue(placed.loose.outside.isEmpty()); assertEquals(0, placed.loose.total)
        // Sessione chiusa: niente scheda, la riga resta in lista.
        val closed = Lavori.place(fy, open = emptySet())
        assertTrue(closed.inCards.isEmpty()); assertEquals(1, closed.loose.outside.size)
        // Le righe dell'agenda non hanno sessione: restano in lista; un ok il cui compito sta già in una scheda non si ripete.
        val all = Lavori.forYou(st, agenda, idle.ts, today)
        val p = Lavori.place(all, open = setOf("ledger-api"), attachedOk = setOf("ledger-release-14"))
        assertEquals(all.claude, p.loose.claude)
        assertEquals(all.outside.filter { it.agenda != null }, p.loose.outside.filter { it.agenda != null })
        assertEquals(listOf("loose-task"), p.loose.ok.map { it.approval?.task })
    }

    /**
     * Franz 24/10 00:44: «in lavori ogni scheda dovrebbe essere espandibile con maggiori dettagli»: i dettagli di una riga
     * «Per te», nell'ordine, senza i campi vuoti: impegno (chi aspetta chi, ultimo passo, prossimo, cartella, esito atteso,
     * pausa), riga dell'agenda (riferimento, dettaglio, chi blocca, fino a quando), ok (cosa, dove, chiesto alle).
     */
    @Test fun aRowOpensOnItsDetails() {
        val c = Commitment("t1", "uno", project = "personali/app", session = "app", state = "running", waits = CommitmentWaits("app", "person"),
            lastStep = CommitmentStep(1789210000, "note", "build verde"), next = "l'ok al push", expected = "release 1.2", paused = CommitmentPause("manca la chiave", 1789210100))
        val row = Lavori.ForYou(Kind.OUTSIDE, "manca la chiave", "app", 1789210100, commitment = c)
        assertEquals(listOf(
            Lavori.Detail.WAITS to "app ← person", Lavori.Detail.LAST_STEP to "build verde", Lavori.Detail.NEXT to "l'ok al push",
            Lavori.Detail.FOLDER to "personali/app", Lavori.Detail.EXPECTED to "release 1.2", Lavori.Detail.PAUSED to "manca la chiave",
        ), Lavori.details(row).map { it.kind to it.value })
        val a = AgendaRow("aperto", "personale", "franz", "Revocare la chiave", "console.anthropic.com", detail = "scade a maggio", until = "2026-11-02")
        assertEquals(listOf(Lavori.Detail.REF to "console.anthropic.com", Lavori.Detail.DETAIL to "scade a maggio", Lavori.Detail.BLOCKS to "franz", Lavori.Detail.UNTIL to "2026-11-02"),
            Lavori.details(Lavori.ForYou(Kind.OUTSIDE, a.title, a.ref, null, agenda = a)).map { it.kind to it.value })
        val ok = Approval("t2", "Release", "tag e push", "github.com/x", requestedAt = 1789214000)
        assertEquals(listOf(Lavori.Detail.WHAT to "tag e push", Lavori.Detail.WHERE to "github.com/x", Lavori.Detail.ASKED_AT to "1789214000"),
            Lavori.details(Lavori.ForYou(Kind.OK, ok.title, null, ok.requestedAt, approval = ok)).map { it.kind to it.value })
        // Niente campi vuoti.
        assertTrue(Lavori.details(Lavori.ForYou(Kind.OK, "x", null, null, approval = Approval("t3", "x"))).isEmpty())
    }

    /** Franz 24/10 18:36: mai il comando del controllo, mai una release o un commit senza cifre. */
    @Test fun proofLabelLeggibile() {
        assertNull(Lavori.proofLabel("check", "git fetch -q origin && git rev-list --remotes | grep -q ."))
        assertNull(Lavori.proofLabel("release", "v"))
        assertEquals("release v2.3", Lavori.proofLabel("release", "v2.3"))
        assertEquals("commit 9f8e7d6", Lavori.proofLabel("commit", "9f8e7d6"))
        assertEquals("docs/a.md", Lavori.proofLabel("file", "docs/a.md"))
        assertNull(Lavori.proofLabel("", "x"))
    }
}
