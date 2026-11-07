package it.pixelbox.cmwatch.mobile

import it.pixelbox.cmwatch.mobile.ui.HomeFolds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Le frecce della plancia del tablet (app-frecce-plancia-tablet-07-11): il tocco apre e chiude, e lo stato resta quando la lista
 * si rifà. I tocchi sono quelli della freccia della scheda (`onToggle`), della testata di sezione e di «Più tardi».
 */
class HomeFoldsTest {
    /**
     * Ogni prova in uno snapshot suo, buttato alla fine: gli stati scritti fuori da una composizione non restano nello snapshot
     * globale della JVM, dove li vedrebbero le gif di Paparazzi delle classi dopo (SlotSubitoTest, SuggFocusTest).
     */
    private fun isolated(body: () -> Unit) {
        val s = Snapshot.takeMutableSnapshot()
        try { s.enter(body) } finally { s.dispose() }
    }

    @Test fun cardArrowOpensAndCloses() {
        isolated {
            val f = HomeFolds()
            f.toggle("storefront")
            assertTrue(f.isOpen("storefront"))
            f.toggle("storefront")
            assertFalse(f.isOpen("storefront"))
        }
    }

    @Test fun oneCardOpenAtATime() {
        isolated {
            val f = HomeFolds()
            f.toggle("storefront"); f.toggle("blog")
            assertFalse(f.isOpen("storefront"))
            assertTrue(f.isOpen("blog"))
        }
    }

    /** Il guasto: con il gruppo nella chiave, una sessione che passava da al lavoro a ferma si richiudeva da sola. */
    @Test fun cardStaysOpenWhenTheSessionChangesGroup() {
        isolated {
            val f = HomeFolds()
            f.toggle("win:supervisor-app")
            // La lista si rifà con la sessione in un altro gruppo: la scheda si cerca per nome.
            assertTrue(f.isOpen("win:supervisor-app"))
        }
    }

    /** Il guasto: nelle colonne della plancia le sezioni erano sempre aperte e la freccia non faceva niente. */
    @Test fun planciaSectionArrowClosesAndReopens() {
        isolated {
            val f = HomeFolds()
            val lavori = f.state("plancia.lavori", true)
            assertTrue(lavori.value)
            lavori.value = !lavori.value
            assertFalse(f.state("plancia.lavori", true).value)
            lavori.value = !lavori.value
            assertTrue(f.state("plancia.lavori", true).value)
        }
    }

    /** La lista si rifà (nuovo SummaryList, stesso HomeFolds dell'Activity): «Più tardi» resta aperto. */
    @Test fun laterKeepsItsStateAcrossARebuild() {
        isolated {
            val f = HomeFolds()
            val first = f.state("later", false)
            first.value = true
            val rebuilt = f.state("later", false)
            assertSame(first, rebuilt)
            assertTrue(rebuilt.value)
        }
    }

    @Test fun initiallyOpenKeepsNamesWithColons() {
        isolated {
            assertEquals("ledger-api", HomeFolds.cardOf("WAITING:ledger-api"))
            assertEquals("win:supervisor-app", HomeFolds.cardOf("WORKING:win:supervisor-app"))
            assertEquals("win:supervisor-app", HomeFolds.cardOf("win:supervisor-app"))
            assertNull(HomeFolds.cardOf(null))
        }
    }
}
