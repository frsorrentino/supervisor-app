package it.pixelbox.cmwatch.mobile

import it.pixelbox.cmwatch.rules.FileRefusal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * app-file-mancante-24-11 (Franz 24/11 21:08: «cos'è questo missing or unreadable che mi appare spesso?»): il PDF citato nella
 * conversazione di legale è sparito dal PC e il relay risponde «missing or unreadable». L'app lo riconosce, lo segna non disponibile
 * e al tocco dice in italiano il nome del file, invece del rifiuto in inglese; le richieste partite da sole restano mute.
 */
class FileMancanteTest {
    private val path = "rino/legale/INVIO-ISTANZE-ACCESSO-2026-11-17/reinvio-Provincia-2026-11/Verbale-prescrizione-06-2025_pag1_scanner.pdf"

    @Test fun ilRifiutoDelRelayDiceCheIlFileNonCePiu() {
        assertTrue(FileRefusal.gone("missing or unreadable"))
        assertNull(FileRefusal.tooLarge("missing or unreadable"))
    }

    @Test fun troppoGrandeNonESparito() = assertFalse(FileRefusal.gone("too large: 3630371 max 3000000"))

    @Test fun lAvvisoHaIlNomeDelFile() = assertEquals("Verbale-prescrizione-06-2025_pag1_scanner.pdf", FileRefusal.name(path))
}
