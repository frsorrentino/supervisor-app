package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.Summary.Group
import org.junit.Assert.*
import org.junit.Test

/**
 * Franz 02/11 21:28 (app-attesa-dettagli-02-11): «nella scheda della sessione che aspetta me quando la apro deve darmi
 * maggiori dettagli sulla scelta da compiere, ora ci sono solo dati statistici». Aperta, la scheda dice prima cosa si
 * sceglie (l'ultimo messaggio: esito e proposta) e mette tutte le voci di «Prossimi:» come tasti; per chi lavora dice cosa
 * sta facendo.
 */
class SessionDetailTest {
    private fun s(st: SessionState = SessionState.IDLE) = Session(id = "a", name = "a", account = "personale", project = "a", state = st, since = 100)
    private val full = "Ho confrontato le due strade: l'unione tiene un solo elenco, la separazione due.\n" +
        "Consiglio l'unione.\nEsito: analisi della home valutata: 6 punti\nTipo: aspetta\nProssimi: Ok ai consigliati, con l'unione · Solo i punti 1-3 · Rifai l'analisi"
    private val waiting = s(SessionState.WAITING).copy(
        outcome = Outcome("analisi della home valutata: 6 punti", full, 500),
        nextSteps = listOf(NextStep("Ok ai consigliati, con l'unione", true), NextStep("Solo i punti 1-3"), NextStep("Rifai l'analisi")),
        turnBlocking = TurnBlocking("Ok ai consigliati, con l'unione", 500),
    )

    @Test fun waitingShowsTheLastMessageWithoutTheClosingLines() {
        val d = SessionDetail.of(waiting, Group.WAITING, shown = "Ok ai consigliati, con l'unione")
        assertEquals(
            "analisi della home valutata: 6 punti\nHo confrontato le due strade: l'unione tiene un solo elenco, la separazione due.\nConsiglio l'unione.",
            d.said,
        )
        assertEquals(500L, d.saidAt)
    }

    @Test fun waitingPutsEveryOtherNextStepAsAButton() {
        // La prima ha già il suo «Ok»: le altre diventano tasti accanto.
        assertEquals(listOf("Solo i punti 1-3", "Rifai l'analisi"), SessionDetail.of(waiting, Group.WAITING, shown = "x").steps)
    }

    @Test fun needsYouKeepsAllStepsBecauseItsButtonIsDone() {
        val w = waiting.copy(needsYou = NeedsYou("rinnova il certificato", 600), turnBlocking = null)
        assertEquals(3, SessionDetail.of(w, Group.WAITING, shown = "rinnova il certificato").steps.size)
    }

    @Test fun waitingWithAQuestionAddsNothing() {
        // La domanda è a metà turno: l'esito di prima parlerebbe d'altro, e le scelte sono le opzioni.
        val q = waiting.copy(question = Question("q", QuestionKind.ASK, "Pubblico?", emptyList(), Tier.LOW, 700))
        val d = SessionDetail.of(q, Group.WAITING, shown = "Pubblico?")
        assertNull(d.said); assertTrue(d.steps.isEmpty())
    }

    @Test fun saidIsDroppedWhenItRepeatsTheHeadline() {
        val w = waiting.copy(outcome = Outcome("Ok?", "Esito: Ok?\nProssimi: !Ok", 500))
        assertNull(SessionDetail.of(w, Group.WAITING, shown = "Ok?").said)
    }

    @Test fun workingSaysWhatItIsDoingDuringTheTurn() {
        val w = s(SessionState.BUSY).copy(tool = "Bash", toolNote = "Lancio i test del core", turnStarted = 900, outcome = Outcome("prima", "prima", 500))
        val d = SessionDetail.of(w, Group.WORKING, shown = "prima")
        assertEquals("Bash · Lancio i test del core", d.doing)
        assertNull(d.said); assertTrue(d.steps.isEmpty())
    }

    @Test fun nothingIsDoingWithoutATool() {
        assertNull(SessionDetail.of(s(SessionState.BUSY).copy(turnStarted = 900), Group.WORKING, shown = null).doing)
        // Ferma: lo strumento è dell'ultimo turno, finito.
        assertNull(SessionDetail.of(s().copy(tool = "Bash", turnStarted = 400, outcome = Outcome("x", "x", 500)), Group.STILL, shown = "x").doing)
    }
}
