package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.PluginsPage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginsRunTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val fresh = PluginsPage(checkedAt = 2, accounts = emptyList())
    private fun ok(text: String) = CmdResult("i", true, text, 1)

    @Test fun afterTheConfirmItIsRunningAndASecondPressSendsNothing() {
        val answer = CompletableDeferred<CmdResult?>()
        val ops = mutableListOf<CmdOp>()
        val r = PluginsRun(scope, { fresh }, { op, _ -> ops += op; answer.await() })
        r.updateAll(listOf("personale"))
        assertTrue(r.state.value.all)
        r.updateAll(listOf("personale"))
        assertEquals(listOf(CmdOp.PLUGIN_UPDATE_ALL), ops)
        answer.complete(ok("fatto"))
        assertFalse(r.state.value.all)
    }

    @Test fun theOutcomeShowsTheRelayTextAndTheListIsReread() {
        var loads = 0
        val r = PluginsRun(scope, { loads++; fresh }, { _, _ -> ok("a@b aggiornato") })
        r.updateAll(listOf("personale"))
        assertEquals(PluginsRun.Note(true, "a@b aggiornato"), r.state.value.notes["personale"])
        assertEquals(1, loads)
        assertEquals(fresh, r.state.value.page)
    }

    @Test fun noAnswerIsSaidAndTheListIsRereadAnyway() {
        var loads = 0
        val r = PluginsRun(scope, { loads++; fresh }, { _, _ -> null })
        r.updateAll(listOf("personale"))
        assertEquals(PluginsRun.Note(false, null), r.state.value.notes["personale"])
        assertEquals(1, loads)
    }

    @Test fun aCheckDoesNotRereadTheList() {
        var loads = 0
        val r = PluginsRun(scope, { loads++; fresh }, { _, _ -> ok("controllo") })
        r.act("check", CmdOp.PLUGIN_CHECK, null, "_check")
        assertEquals(0, loads)
    }
}
