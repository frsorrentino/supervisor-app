package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test

/** Il diff dal telefono (contratto 1.72): le regole di telefono, tablet e web; gli stessi casi in web/src/lib/diff.test.ts. */
class DiffRulesTest {
    private val view = ContractJson.decodeChanges(Fixtures.changes)
    private val totals = view.files[0]
    private fun csv(n: Int, cut: Boolean) = StepDiff("/w/a.csv", "create", 261, 0, listOf(Hunk(0, 0, 1, 261, List(n) { "+r$it" })), cut)

    @Test fun theClosedRow() {
        assertEquals("+2 \u22121", DiffRules.counts(2, 1))
        assertEquals("totals.ts · +2 \u22121", DiffRules.stepLine(StepDiff("/home/dev/storefront/src/checkout/totals.ts", "edit", 2, 1, totals.hunks)))
        assertEquals("totals.ts", DiffRules.name("src/checkout/totals.ts"))
        assertEquals("src/checkout", DiffRules.folder("src/checkout/totals.ts"))
        assertNull(DiffRules.folder(".env.local"))
    }

    @Test fun theLabelsAndTheNumbers() {
        assertEquals(listOf(null, DiffRules.Label.NEW, null, DiffRules.Label.BINARY), view.files.map { DiffRules.label(it.status) })
        assertEquals(listOf(DiffRules.Label.DELETED, DiffRules.Label.RENAMED, null), listOf("deleted", "renamed", "copied").map { DiffRules.label(it) })
        assertEquals(listOf(true, true, true, false), view.files.map { DiffRules.showCounts(it) })
    }

    @Test fun whyAFileComesWithoutLines() {
        assertEquals(listOf(null, null, DiffRules.Hidden.SECRET, DiffRules.Hidden.BINARY), view.files.map { DiffRules.hidden(it) })
        assertEquals(DiffRules.Hidden.BIG, DiffRules.hidden(ChangedFile("data/dump.sql", "modified", 10, 2, 3_200_000, emptyList(), false)))
        listOf(".env", "certs/server.pem", "deploy.key", "id_ed25519", "store.p12", "credentials.json").forEach { assertTrue(it, DiffRules.secret(it)) }
        listOf("environment.ts", "keys.md", "src/identity.ts").forEach { assertFalse(it, DiffRules.secret(it)) }
        // Un file nuovo e vuoto: niente righe, e la vista lo dice.
        assertEquals(DiffRules.Hidden.EMPTY, DiffRules.hidden(ChangedFile("notes/empty.md", "added", 0, 0, 0, emptyList(), false)))
    }

    // Caso 2 dei punti delicati: rinominato senza righe cambiate.
    @Test fun aRenameWithoutChangedLines() {
        val f = ChangedFile("src/checkout/sum.ts", "renamed", 0, 0, 2140, emptyList(), false)
        assertEquals(DiffRules.Label.RENAMED, DiffRules.label(f.status))
        assertFalse(DiffRules.showCounts(f))
        assertEquals(DiffRules.Hidden.RENAMED, DiffRules.hidden(f))
        val g = f.copy(added = 1, removed = 1, hunks = totals.hunks)
        assertTrue(DiffRules.showCounts(g)); assertNull(DiffRules.hidden(g))
    }

    @Test fun theNumberedRowsOfTheFixture() {
        val r = DiffRules.rows(totals.hunks)
        assertEquals(listOf(
            DiffRules.Row(DiffRules.Kind.SAME, 12, 12, "export function total(cart: Cart) {"),
            DiffRules.Row(DiffRules.Kind.REMOVED, 13, null, "  return cart.items.reduce((s, i) => s + i.price, 0)"),
            DiffRules.Row(DiffRules.Kind.ADDED, null, 13, "  const sub = cart.items.reduce((s, i) => s + i.price * i.qty, 0)"),
            DiffRules.Row(DiffRules.Kind.ADDED, null, 14, "  return sub - (cart.giftCard?.amount ?? 0)"),
            DiffRules.Row(DiffRules.Kind.SAME, 14, 15, "}"),
        ), r)
        assertEquals(2, DiffRules.gutter(r))
        val created = DiffRules.rows(view.files[1].hunks)
        assertEquals((1..5).toList(), created.map { it.new })
        assertTrue(created.all { it.kind == DiffRules.Kind.ADDED && it.old == null })
        assertEquals("", created[1].text)
    }

    @Test fun gapsNotesTabsAndWindowsLineEnds() {
        val r = DiffRules.rows(listOf(
            Hunk(3, 1, 3, 1, listOf("-\told\r", "+\tnew\r")),
            Hunk(40, 1, 40, 2, listOf(" end", "+more", "\\ No newline at end of file")),
        ))
        assertEquals(listOf(DiffRules.Kind.REMOVED, DiffRules.Kind.ADDED, DiffRules.Kind.GAP, DiffRules.Kind.SAME, DiffRules.Kind.ADDED, DiffRules.Kind.NOTE), r.map { it.kind })
        assertEquals(listOf("    old", "    new"), r.take(2).map { it.text })
        assertEquals(DiffRules.Row(DiffRules.Kind.NOTE, null, null, "No newline at end of file"), r[5])
        assertEquals(listOf(3, null, null, 40, null, null), r.map { it.old })
        assertEquals(listOf(null, 3, null, 40, 41, null), r.map { it.new })
    }

    // Caso 1 dei punti delicati: una riga lunghissima senza spazi (un file minimizzato).
    @Test fun aVeryLongLineWithoutSpaces() {
        val r = DiffRules.rows(listOf(Hunk(1, 0, 1, 1, listOf("+" + "x".repeat(50_000))))).single()
        assertEquals(DiffRules.MAX_LINE_CHARS, r.text.length)
        assertEquals(48_000, r.more)
        assertFalse(r.text.contains('\n'))
    }

    // Caso 3 dei punti delicati: `cut` sul passaggio e sulla vista.
    @Test fun cutOnTheStepAndOnTheView() {
        val step = csv(200, cut = true)
        assertTrue(DiffRules.shown(step, null, wantAll = false, busy = false).showAll)
        assertEquals(DiffRules.Shown(step, showAll = false, ask = true, beyond = false), DiffRules.shown(step, null, wantAll = true, busy = false))
        val whole = csv(261, cut = false)
        assertEquals(DiffRules.Shown(whole, showAll = false, ask = false, beyond = false), DiffRules.shown(step, whole, wantAll = true, busy = false))
        // Oltre le 5000 righe anche il passaggio intero è tagliato: lo dice l'ultima riga.
        assertTrue(DiffRules.shown(step, whole.copy(cut = true), wantAll = true, busy = false).beyond)
        val cutView = view.copy(files = List(50) { totals.copy(path = "src/f$it.ts") }, totals = ChangesTotals(60, 900, 120), cut = true)
        assertEquals(10, DiffRules.hiddenFiles(cutView)); assertEquals(0, DiffRules.hiddenFiles(view))
        // Un file tagliato senza righe nell'elenco non è «vuoto»: si apre intero con `file=`.
        assertNull(DiffRules.hidden(totals.copy(hunks = emptyList(), cut = true)))
        assertEquals("file=src/checkout/totals.ts", DiffRules.fileArg("src/checkout/totals.ts"))
    }

    // Caso 4 dei punti delicati: una sessione chiusa con modifiche tiene la pillola, la cartella c'è ancora.
    @Test fun aClosedSessionKeepsThePill() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion).sessions.single { it.changes != null }
        assertEquals(ChangesCount(4, 8, 2, 1792100000L), DiffRules.pill(s.copy(state = SessionState.GONE)))
        assertNull(DiffRules.pill(s.copy(changes = null)))
    }

    // Caso 5 dei punti delicati: dopo una rotazione la scelta «Mostra tutto» c'è, la copia in memoria no.
    @Test fun afterARotationTheWholeStepIsAskedAgain() {
        val after = DiffRules.shown(csv(200, cut = true), full = null, wantAll = true, busy = false)
        assertTrue(after.ask); assertEquals(200, after.diff.hunks.single().lines.size)
        // Con una richiesta in volo non se ne fa un'altra.
        assertFalse(DiffRules.shown(csv(200, cut = true), null, wantAll = true, busy = true).ask)
    }

    @Test fun theOutcomeOfARead() {
        assertEquals(DiffRules.Load.Failed(DiffRules.Why.DOWN), DiffRules.read(null, ContractJson::decodeChanges))
        assertEquals(DiffRules.Why.GIT, (DiffRules.read(CmdResult("1", false, "git did not answer in 5 s", 0), ContractJson::decodeChanges) as DiffRules.Load.Failed).why)
        assertEquals(DiffRules.Load.Failed(DiffRules.Why.OTHER, "no session x"), DiffRules.read(CmdResult("2", false, "no session x", 0), ContractJson::decodeChanges))
        assertEquals(view, (DiffRules.read(CmdResult("3", true, Fixtures.changes, 0), ContractJson::decodeChanges) as DiffRules.Load.Ok).value)
        assertEquals(DiffRules.Why.OTHER, (DiffRules.read(CmdResult("4", true, "not json", 0), ContractJson::decodeChanges) as DiffRules.Load.Failed).why)
        assertEquals(totals, DiffRules.alone(view, "src/checkout/totals.ts"))
        assertNull(DiffRules.alone(view.copy(files = emptyList()), "src/checkout/totals.ts"))
    }

    @Test fun whereTheChangesComeFrom() {
        assertEquals(DiffRules.Source.GIT, DiffRules.source(view))
        assertEquals(DiffRules.Source.NO_COMMITS, DiffRules.source(view.copy(base = null)))
        assertEquals(DiffRules.Source.TRANSCRIPT, DiffRules.source(view.copy(source = "transcript", repo = null, base = null)))
    }

    // Revisione finale (16/10): `file=` solo per un file tagliato; con le righe già complete niente lettura, quindi niente
    // «PC non raggiungibile» sotto un diff intero a PC spento.
    @Test fun onlyACutFileIsAskedAgain() {
        assertFalse(DiffRules.askFile(totals))
        assertTrue(DiffRules.askFile(totals.copy(cut = true)))
        assertTrue(DiffRules.askFile(totals.copy(hunks = emptyList(), cut = true)))
        assertFalse(DiffRules.askFile(view.files[2]))
        assertFalse(DiffRules.askFile(view.files[3]))
    }

    // Franz, 16/10 13:08: «l'header su 2 righe non va bene, da mobile spostare diff in menu». Sotto 600 dp la pillola diventa
    // la prima voce del menu ⋮, con un puntino sul ⋮; tablet e finestre larghe la tengono.
    @Test fun `Modifiche nel menu sul telefono, pillola sul tablet`() {
        assertTrue(DiffRules.inMenu(411))
        assertTrue(DiffRules.inMenu(599))
        assertFalse(DiffRules.inMenu(600))
        assertFalse(DiffRules.inMenu(1280))
        assertEquals("Modifiche · 22 file +2440 \u22129", DiffRules.menuLine("Modifiche", "22 file", ChangesCount(22, 2440, 9, 0)))
    }
}
