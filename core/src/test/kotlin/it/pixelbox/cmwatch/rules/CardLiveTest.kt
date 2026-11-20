package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ProvaOne
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.contract.WaitTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** app-carte-stato-vivo-20-11: gli stessi casi di web/src/lib/cardLive.test.ts. */
class CardLiveTest {
    private fun p(id: Long, state: String = "todo", at: Long = 1_000, text: String? = null) = ProvaOne(id = id, state = state, at = at, text = text)
    private val verify = WaitItem(id = "w1", label = "verify", tests = listOf(WaitTest(1349, state = "todo"), WaitTest(1347, state = "ok")))

    @Test fun theOpenProveComeFromTheOpenVerifications() {
        assertEquals(setOf(1349L), CardLive.openProve(listOf(verify, verify.copy(id = "w2", state = "closed", tests = listOf(WaitTest(1348))))))
        assertNull("senza attese non si sa", CardLive.openProve(null))
    }

    @Test fun anOpenProvaHasItsButton() = assertTrue(CardLive.prova(p(1349), setOf(1349L)).actionable)

    // Lo screenshot delle 20:37: 1347 confermata altrove; la carta dice l'esito, senza tasto.
    @Test fun theOutcomeReadDecidesNotTheAbsence() {
        assertEquals(CardLive.View(CardLive.Kind.OK, 1_000), CardLive.prova(p(1347, "ok"), setOf(1349L)))
        assertEquals(CardLive.Kind.KO, CardLive.prova(p(1347, "ko", text = "il totale va a capo"), null).kind)
        assertEquals(CardLive.Kind.RED, CardLive.prova(p(1347, "red"), setOf(1347L)).kind)
    }

    @Test fun outOfTheOpenButStillTodoIsNotConfirmed() {
        val v = CardLive.prova(p(1348), setOf(1349L))
        assertEquals(CardLive.Kind.NOT_OPEN, v.kind)
        assertFalse(v.actionable)
    }

    @Test fun withoutAtteseTheCardStaysAsTheProvaSays() = assertTrue(CardLive.prova(p(1348), null).actionable)

    @Test fun cardsShownOpenButNoLongerOpenAreReadAgain() =
        assertEquals(listOf(1347L, 1348L), CardLive.stale(mapOf(1347L to p(1347), 1348L to p(1348), 1349L to p(1349), 1350L to p(1350, "ok"), 1351L to null), setOf(1349L)))

    @Test fun anAlreadyConfirmedProvaIsNotAnError() {
        assertTrue(ProveOk.alreadyDone("ERR prova 1347: non c'è più o è già confermata"))
        assertFalse("un altro no resta un errore", ProveOk.alreadyDone("ERR prova 1347: build not installed"))
        assertFalse("non una prova", ProveOk.alreadyDone("kb is no longer running"))
    }
}
