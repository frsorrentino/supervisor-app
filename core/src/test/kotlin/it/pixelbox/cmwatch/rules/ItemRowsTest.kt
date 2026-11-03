package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.AltroveRun
import it.pixelbox.cmwatch.contract.DecisionGroup
import it.pixelbox.cmwatch.contract.DecisionItem
import it.pixelbox.cmwatch.contract.PrimaRaccoltaSection
import it.pixelbox.cmwatch.contract.ProveItem
import it.pixelbox.cmwatch.contract.ProveKo
import it.pixelbox.cmwatch.contract.RimastoItem
import it.pixelbox.cmwatch.contract.Suggestion
import it.pixelbox.cmwatch.rules.ItemRows.Kind
import it.pixelbox.cmwatch.rules.ItemRows.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** app-richiede-te-righe-02-11: le voci delle sezioni come righe di sessione; itemRows.test.ts nella web. */
class ItemRowsTest {
    @Test fun aDecisionFromHereIsAmberAndSaysWhatItUnblocks() {
        val g = DecisionGroup("k1", "README di fable-director", items = listOf(DecisionItem(kind = "ok", task = "t1", reason = "Ritocchi: versione 1.61")), weight = 2, where = "here")
        val r = ItemRows.decision(g)
        assertEquals("README di fable-director", r.name)
        assertEquals("Ritocchi: versione 1.61", r.sub)
        assertEquals(Tone.WAIT, r.tone)
        assertEquals(listOf(ItemRows.Right(Kind.HERE), ItemRows.Right(Kind.UNBLOCKS, n = 2)), r.right)
    }

    @Test fun aDecisionAtThePcIsGreyAndUnblocksOnlyFromTwo() {
        val r = ItemRows.decision(DecisionGroup("k2", "Studio da loggato", items = listOf(DecisionItem(kind = "pause", reason = "Prova con l'impronta")), weight = 1, where = "pc"))
        assertEquals(Tone.STILL, r.tone)
        assertEquals(listOf(ItemRows.Right(Kind.AT_PC)), r.right)
    }

    @Test fun aTestIsAmberOnlyWhenItsBuildIsInstalled() {
        val p = ProveItem(7, "app-x", short = "Zoom", build = "zoom-punto", fai = "Pizzica un'immagine")
        assertEquals(Tone.WAIT, ItemRows.prove(p, installed = true).tone)
        assertEquals("Pizzica un'immagine", ItemRows.prove(p, true).sub)
        assertEquals(ItemRows.Right(Kind.BUILD, "zoom-punto"), ItemRows.prove(p, true).right.single())
        assertEquals(Tone.STILL, ItemRows.prove(p, installed = false).tone)
        assertEquals(Kind.TO_INSTALL, ItemRows.prove(p, false).right.single().kind)
        assertEquals(Tone.RED, ItemRows.proveKo(ProveKo(8, "app-y", text = "Non si apre")).tone)
    }

    @Test fun followedItemsTakeTheirColourFromTheirState() {
        assertEquals(Tone.WAIT, ItemRows.rimasto(RimastoItem("sito", "k", "Pagina prezzi", stale = true)).tone)
        assertEquals(Tone.GOOD, ItemRows.rimasto(RimastoItem("sito", "k", stale = false)).tone)
        assertEquals("k", ItemRows.rimasto(RimastoItem("sito", "k", stale = null)).name)
        assertEquals(Tone.BUSY, ItemRows.altrove(AltroveRun("a", "Cerca X", running = true, positive = listOf("p"))).tone)
        assertEquals(Tone.WAIT, ItemRows.altrove(AltroveRun("a", "Cerca X", positive = listOf("repo-1"))).tone)
        assertEquals("repo-1", ItemRows.altrove(AltroveRun("a", "Cerca X", positive = listOf("repo-1"))).sub)
        assertEquals(ItemRows.Right(Kind.CHECKED, "3/5"), ItemRows.altrove(AltroveRun("a", total = 5, done = 3)).right.single())
    }

    @Test fun aFirstRoundSectionIsGreenWhenFilledAndSaysWhyWhenEmpty() {
        assertEquals(Tone.GOOD, ItemRows.prima(PrimaRaccoltaSection("hosts", n = 2, text = "win e Chromebook")).tone)
        val e = ItemRows.prima(PrimaRaccoltaSection("night", n = 0, why = "Nessuna notte ancora"))
        assertEquals(Tone.STILL, e.tone)
        assertEquals("Nessuna notte ancora", e.sub)
    }

    @Test fun aSuggestionSplitsIntoNameAndRest() {
        val r = ItemRows.suggestion(Suggestion("s1", "cleanup", text = "Chiudi le tre sessioni ferme. Sono inattive da due giorni."))
        assertEquals("Chiudi le tre sessioni ferme.", r.name)
        assertEquals("Sono inattive da due giorni.", r.sub)
        assertEquals(Tone.WAIT, r.tone)
        val one = ItemRows.suggestion(Suggestion("s2", "idea", text = "Un testo solo"))
        assertEquals("Un testo solo", one.name)
        assertNull(one.sub)
        assertEquals("Prima riga" to "seconda riga", ItemRows.split("Prima riga\n  seconda riga"))
    }

    @Test fun threeRowsThenTheRestBehindOneTap() {
        assertEquals(3, ItemRows.shown(10, all = false))
        assertEquals(10, ItemRows.shown(10, all = true))
        // «altre 1» non serve: con quattro si vedono tutte.
        assertEquals(4, ItemRows.shown(4, all = false))
    }
}
