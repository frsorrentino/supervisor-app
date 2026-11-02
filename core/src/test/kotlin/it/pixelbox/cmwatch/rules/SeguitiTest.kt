package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.transport.FakeTransport
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneOffset

/** Contratto 1.102, sulle fixture del relay: gli stessi casi di seguiti.test.ts. */
class SeguitiTest {
    private val page = ContractJson.decodeSeguiti(FakeTransport({ Fixtures.read("$it.json") }).sampleText("seguiti"))
    private val item = page.rimasto.single()
    private val run = page.altrove.single()

    @Test fun rimasto_indietro_decodes() {
        assertEquals(SeguitiSummary(2, 1, 1), ContractJson.decodeState(Fixtures.stateStale).seguiti)
        assertEquals(4, Seguiti.total(SeguitiSummary(2, 1, 1)))
        assertEquals(0, Seguiti.total(null))
        assertEquals("atlas-shop", item.product); assertEquals("demo", item.key); assertEquals("the demo on the site", item.where)
        assertEquals("v2.3.0", item.alignedVersion); assertEquals(1789100000L, item.alignedAt)
        assertEquals(Seguiti.Age.BEHIND, Seguiti.age(item)); assertEquals(Seguiti.Why.PATHS, Seguiti.why(item)); assertEquals(3, Seguiti.changedPaths(item))
    }

    @Test fun rimasto_indietro_unknownIsNotAligned() {
        val unknown = item.copy(stale = null, why = "no_git")
        assertEquals(Seguiti.Age.UNKNOWN, Seguiti.age(unknown)); assertEquals(Seguiti.Why.NO_GIT, Seguiti.why(unknown)); assertNull(Seguiti.changedPaths(unknown))
        assertEquals(Seguiti.Why.CHECK_TIMEOUT, Seguiti.why(unknown.copy(why = "check_timeout")))
        assertEquals(Seguiti.Why.NO_PROJECT, Seguiti.why(unknown.copy(why = "no_project")))
        assertTrue(Seguiti.canAlign(unknown)); assertTrue(Seguiti.canAccept(unknown))
    }

    @Test fun rimasto_indietro_actionsAndOrder() {
        assertTrue(Seguiti.canAlign(item)); assertTrue(Seguiti.canAccept(item))
        assertFalse("un compito aperto non se ne apre un altro", Seguiti.canAlign(item.copy(task = "allinea-1")))
        val fresh = item.copy(key = "f", stale = false, why = null)
        assertFalse(Seguiti.canAlign(fresh)); assertFalse(Seguiti.canAccept(fresh))
        val unknown = item.copy(key = "u", stale = null, why = "no_git")
        assertEquals(listOf("demo", "u", "f"), Seguiti.ordered(listOf(fresh, unknown, item)).map { it.key })
    }

    @Test fun stesso_difetto_neverCleanByMistake() {
        assertEquals(listOf("personal/atlas-shop"), run.positive); assertEquals(12, run.done); assertFalse(Seguiti.clean(run))
        assertTrue(Seguiti.clean(run.copy(positive = emptyList())))
        assertFalse("un errore non è un negativo", Seguiti.clean(run.copy(positive = emptyList(), errors = listOf("x"))))
        val cut = run.copy(positive = emptyList(), complete = false, done = 5)
        assertFalse("un parziale non è un negativo", Seguiti.clean(cut)); assertTrue(Seguiti.partial(cut))
        assertFalse("niente controllato", Seguiti.clean(run.copy(positive = emptyList(), total = 0, done = 0)))
    }

    @Test fun promemoria_decodesWaitingAndFired() {
        val p = page.promemoria.single()
        assertEquals("7f3e1a20", p.id); assertEquals("When the store approves, the amber icon", p.text); assertNull(p.expiresAt); assertNull(p.atTime)
        assertTrue(Seguiti.fired(page.promemoriaScattati.single())); assertFalse(Seguiti.fired(page.promemoriaScattati.single().copy(outcome = "expired")))
        assertEquals("15/09 12:00", Seguiti.whenText(1789473600, ZoneOffset.UTC))
    }

    @Test fun promemoria_firedSuggestionAndEventAreKnown() {
        assertTrue(Suggestions.known("reminder"))
        assertFalse(StrongAlert.strong(EventKind.REMINDER))
        assertEquals(NotificationIcon.Icon.BELL, NotificationIcon.of(EventKind.REMINDER))
        val e = ContractJson.json.decodeFromString(Event.serializer(), """{"key":"k","kind":"reminder","ts":1,"title":"Renew the certificate","ref":"s-1"}""")
        assertEquals(EventKind.REMINDER, e.kind)
    }

    @Test fun rimasto_indietro_actionCarriesProductAndKey() {
        val cmd = Cmd("i", CmdOp.SEGUITI_AZIONE, null, null, 1, "b", action = "allinea", product = "atlas-shop", key = "demo")
        val json = ContractJson.json.encodeToString(Cmd.serializer(), cmd)
        assertTrue(json, json.contains("\"op\":\"seguiti_azione\"") && json.contains("\"product\":\"atlas-shop\"") && json.contains("\"key\":\"demo\""))
        assertFalse("assente, non scritto", ContractJson.json.encodeToString(Cmd.serializer(), cmd.copy(product = null, key = null)).contains("product"))
    }
}
