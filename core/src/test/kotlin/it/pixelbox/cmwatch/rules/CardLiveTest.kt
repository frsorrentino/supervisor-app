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
        // Le voci del relay (cmd-result-sample, «Checkout on Safari») e della demo non hanno `tests`: il numero sta nel comando dei tasti.
        val relay = WaitItem(id = "w3", label = "verify", answer = it.pixelbox.cmwatch.contract.WaitAnswer("prova", "1412,1413"),
            actions = listOf(it.pixelbox.cmwatch.contract.WaitAction("Va", "primary", kotlinx.serialization.json.buildJsonObject { put("op", kotlinx.serialization.json.JsonPrimitive("prova")); put("arg", kotlinx.serialization.json.JsonPrimitive("1")) })))
        assertEquals(setOf(1L, 1412L, 1413L), CardLive.openProve(listOf(relay)))
    }

    @Test fun anOpenProvaHasItsButton() = assertTrue(CardLive.prova(p(1349), emptySet()).actionable)

    // Lo screenshot delle 20:37: 1347 confermata altrove; la carta dice l'esito, senza tasto.
    @Test fun theOutcomeReadDecidesNotTheAbsence() {
        assertEquals(CardLive.View(CardLive.Kind.OK, 1_000), CardLive.prova(p(1347, "ok"), setOf(1347L)))
        assertEquals(CardLive.Kind.KO, CardLive.prova(p(1347, "ko", text = "il totale va a capo"), emptySet()).kind)
        assertEquals(CardLive.Kind.RED, CardLive.prova(p(1347, "red"), emptySet()).kind)
    }

    @Test fun outOfTheOpenButStillTodoIsNotConfirmed() {
        val closed = CardLive.closedProve(seen = setOf(1348L, 1349L), open = setOf(1349L))
        assertEquals(setOf(1348L), closed)
        val v = CardLive.prova(p(1348), closed)
        assertEquals(CardLive.Kind.NOT_OPEN, v.kind)
        assertFalse(v.actionable)
    }

    @Test fun withoutAtteseTheCardStaysAsTheProvaSays() = assertTrue(CardLive.prova(p(1348), CardLive.closedProve(setOf(1348L), null)).actionable)

    // Il caso della demo: una prova aperta che `attese` non ha mai nominato non si dice chiusa.
    @Test fun aProvaNeverSeenOpenKeepsItsButton() = assertTrue(CardLive.prova(p(1), CardLive.closedProve(seen = setOf(1349L), open = setOf(1349L))).actionable)

    @Test fun cardsShownOpenButNoLongerOpenAreReadAgain() =
        assertEquals(listOf(1347L, 1348L), CardLive.stale(mapOf(1347L to p(1347), 1348L to p(1348), 1349L to p(1349), 1350L to p(1350, "ok"), 1351L to null), setOf(1347L, 1348L, 1350L)))

    @Test fun anAlreadyConfirmedProvaIsNotAnError() {
        assertTrue(ProveOk.alreadyDone("ERR prova 1347: non c'è più o è già confermata"))
        assertFalse("un altro no resta un errore", ProveOk.alreadyDone("ERR prova 1347: build not installed"))
        assertFalse("non una prova", ProveOk.alreadyDone("kb is no longer running"))
    }
}
