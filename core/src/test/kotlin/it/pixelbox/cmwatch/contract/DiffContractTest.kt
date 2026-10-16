package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

/**
 * Contratto 1.72, il diff dal telefono (specifica del 15/10): le fixture del relay, uguali nei due repo, lette come le legge
 * l'app. Gli stessi casi in web/src/lib/contract.test.ts.
 */
class DiffContractTest {
    private val totalsHunk = Hunk(12, 3, 12, 4, listOf(
        " export function total(cart: Cart) {",
        "-  return cart.items.reduce((s, i) => s + i.price, 0)",
        "+  const sub = cart.items.reduce((s, i) => s + i.price * i.qty, 0)",
        "+  return sub - (cart.giftCard?.amount ?? 0)",
        " }",
    ))

    @Test fun theChangesSampleReadsWhole() {
        val v = ContractJson.decodeChanges(Fixtures.changes)
        assertEquals(listOf("git", "/home/dev/storefront", "a1b2c3d"), listOf(v.source, v.repo, v.base))
        assertEquals(listOf("src/checkout/totals.ts", "tests/checkout/totals.test.ts", ".env.local", "assets/logo.png"), v.files.map { it.path })
        assertEquals(listOf("modified", "added", "modified", "binary"), v.files.map { it.status })
        assertEquals(listOf(2 to 1, 5 to 0, 1 to 1, 0 to 0), v.files.map { it.added to it.removed })
        assertEquals(listOf(2140L, 212L, 88L, 48213L), v.files.map { it.size })
        assertEquals(listOf(totalsHunk), v.files[0].hunks)
        assertEquals(
            Hunk(0, 0, 1, 5, listOf(
                "+import { total } from '../../src/checkout/totals'", "+", "+test('gift card lowers the total', () => {",
                "+  expect(total({ items: [{ price: 10, qty: 2 }], giftCard: { amount: 5 } })).toBe(15)", "+})",
            )),
            v.files[1].hunks.single(),
        )
        // Il nome sensibile e il binario arrivano senza righe: solo nome, stato e conteggi.
        assertTrue(v.files[2].hunks.isEmpty() && v.files[3].hunks.isEmpty())
        assertTrue(v.files.none { it.cut })
        assertEquals(ChangesTotals(4, 8, 2), v.totals); assertFalse(v.cut); assertEquals(1792100000L, v.at)
    }

    @Test fun theStateCarriesTheCountsOnlyWhereThereAreChanges() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        // Nella demo atlas-shop si chiama storefront (DemoText).
        assertEquals(listOf("atlas-shop" to ChangesCount(4, 8, 2, 1792100000L)), s.sessions.filter { it.changes != null }.map { it.name to it.changes })
        for (raw in listOf(Fixtures.stateQuestion, Fixtures.stateIdle)) {
            assertTrue(ContractJson.decodeState(raw).ops.orEmpty().containsAll(listOf("changes", "diff")))
        }
    }

    /** Ogni voce della trascrizione ha la chiave `diff`, null dove non serve; la voce Edit porta il suo. */
    @Test fun everyTranscriptEntryCarriesDiffAndTheEditOneHasIt() {
        val pages = transcriptTexts()
        val raw = pages.flatMap { p -> Json.parseToJsonElement(p).jsonObject.getValue("entries").jsonArray.map { it.jsonObject } }
        assertTrue(raw.isNotEmpty())
        assertTrue(raw.all { "diff" in it })
        val withDiff = pages.flatMap { ContractJson.decodeTranscript(it).entries }.filter { it.diff != null }.distinctBy { it.id }
        assertEquals(listOf("Edit"), withDiff.map { it.tool })
        assertEquals(StepDiff("/home/dev/storefront/src/checkout/totals.ts", "edit", 2, 1, listOf(totalsHunk), cut = false), withDiff.single().diff)
    }

    @Test fun theTwoOpsAndTheWholeStep() {
        val c = Cmd("x", CmdOp.CHANGES, "atlas-shop", "file=src/checkout/totals.ts", 1, "phone")
        val enc = ContractJson.encode(c)
        assertTrue("\"op\":\"changes\"" in enc); assertTrue("\"arg\":\"file=src/checkout/totals.ts\"" in enc)
        assertEquals(CmdOp.DIFF, ContractJson.json.decodeFromString(Cmd.serializer(), ContractJson.encode(c.copy(op = CmdOp.DIFF, arg = "a8.0"))).op)
        val whole = ContractJson.decodeDiff("""{"path":"/w/blog/bench/results.csv","kind":"create","added":261,"removed":0,"hunks":[{"old_start":0,"old_lines":0,"new_start":1,"new_lines":261,"lines":["+page,run"]}],"cut":false}""")
        assertEquals("create", whole.kind); assertEquals(261, whole.added); assertEquals(1, whole.hunks.single().newStart)
    }

    /** I testi JSON delle trascrizioni nelle fixture: le pagine di `transcript` e il /tail dell'API locale (1.70). */
    private fun transcriptTexts(): List<String> {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val ids = root.getValue("cmd").jsonArray.map { it.jsonObject }
            .filter { it.getValue("op").jsonPrimitive.content == "transcript" }.map { it.getValue("id").jsonPrimitive.content }.toSet()
        val pages = root.getValue("result").jsonArray.map { it.jsonObject }
            .filter { it.getValue("id").jsonPrimitive.content in ids && it.getValue("ok").jsonPrimitive.content == "true" }
            .map { it.getValue("text").jsonPrimitive.content }
        val tail = Json.parseToJsonElement(Fixtures.read("local-api.json")).jsonObject.getValue("exchanges").jsonArray.map { it.jsonObject }
            .filter { it.getValue("name").jsonPrimitive.content == "tail" }.map { it.getValue("response").jsonObject.getValue("body").toString() }
        return pages + tail
    }
}
