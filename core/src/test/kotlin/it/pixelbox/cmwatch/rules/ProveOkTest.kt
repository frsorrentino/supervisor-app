package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// L'Ok su una prova: la scheda torna solo per un rifiuto vero (app-prova-ok-sparisce-04-11).
class ProveOkTest {
    @Test fun anAcceptedOkIsDone() = assertTrue(ProveOk.done(true, "prove ok: 1"))
    @Test fun alreadyConfirmedCountsAsDone() = assertTrue(ProveOk.done(false, "ERR prova 84: non c'è più o è già confermata"))
    @Test fun noAnswerInTimeKeepsTheCardAway() = assertTrue(ProveOk.done(null, null))
    @Test fun aRealRefusalBringsTheCardBack() = assertFalse(ProveOk.done(false, "prova: bad result maybe"))
}
