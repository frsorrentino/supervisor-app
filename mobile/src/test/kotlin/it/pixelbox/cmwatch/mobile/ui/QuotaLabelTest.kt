package it.pixelbox.cmwatch.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** La pillola della quota in testata dice sempre di quale quota parla (Franz, 12/10 21:25: «aggiungiamo 5h alla percentuale senza indicazioni»). */
class QuotaLabelTest {
    @Test fun everyFormKeepsTheFiveHourLabel() {
        QuotaForm.entries.forEach { f ->
            assertEquals(f.name, "5h 9%", quotaLabel(f, "5h 9%", null))
            assertEquals(f.name, true, quotaLabel(f, "5h 9%", "22:00").startsWith("5h 9%"))
        }
    }

    @Test fun theResetTimeOnlyWhereItFits() {
        assertEquals("5h 9% · 22:00", quotaLabel(QuotaForm.FULL, "5h 9%", "22:00"))
        assertEquals("5h 9% · lun 09:00", quotaLabel(QuotaForm.FULL, "5h 9%", "lun 09:00"))
        assertEquals("5h 9%", quotaLabel(QuotaForm.SHORT, "5h 9%", "22:00"))
        assertEquals("5h 9%", quotaLabel(QuotaForm.TINY, "5h 9%", "22:00"))
    }
}
