package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** app-attesa-dopo-scelta-17-11: dopo una risposta dalla scheda di una voce, cosa dice la conferma e dove si va. Stessi casi di afterAnswer.test.ts. */
class AfterAnswerTest {
    private val live = { s: String -> s == "preventivi" }

    @Test fun choiceGoesToTheSessionOfTheItem() =
        assertEquals(TiAspetta.After("Asiano", "preventivi", "preventivi"), TiAspetta.afterAnswer("preventivi", TiAspetta.sentWhat("Asiano", null), "preventivi", live))

    @Test fun writtenTextIsPartOfWhatWasSent() =
        assertEquals("Rispondi con parole tue: il secondo, ma più corto", TiAspetta.sentWhat("Rispondi con parole tue", "  il secondo, ma più corto "))

    @Test fun withoutSessionToTheMasterThenHome() =
        assertEquals(TiAspetta.After("Approva", "master", null), TiAspetta.afterAnswer(null, "Approva", null, live))

    @Test fun closedSessionGoesHomeButKeepsTheName() =
        assertEquals(TiAspetta.After("Va", "ledger-api", null), TiAspetta.afterAnswer("ledger-api", "Va", null, live))
}
