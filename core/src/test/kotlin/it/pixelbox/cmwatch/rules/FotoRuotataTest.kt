package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** L'angolo in alto a sinistra del file e quello in alto a destra, dopo ogni orientamento: la tabella EXIF. La foto è 40 per 30. */
class FotoRuotataTest {
    private val w = 40
    private val h = 30
    private fun corners(o: Int) = ShareImage.turn(o).let { t -> ShareImage.place(t, 0, 0, w, h) to ShareImage.place(t, w, 0, w, h) }

    @Test fun foto_ruotata_dritta() {
        for (o in listOf(0, 1, 9)) assertEquals("$o", (0 to 0) to (40 to 0), corners(o))
    }

    @Test fun foto_ruotata_lOrientamentoSeiGiraDiNovantaOrari() = assertEquals((30 to 0) to (30 to 40), corners(6))

    @Test fun foto_ruotata_ottoGiraDiNovantaAntiorari() = assertEquals((0 to 40) to (0 to 0), corners(8))

    @Test fun foto_ruotata_treSottosopra() = assertEquals((40 to 30) to (0 to 30), corners(3))

    @Test fun foto_ruotata_gliSpecchi() {
        assertEquals((40 to 0) to (0 to 0), corners(2))
        assertEquals((0 to 30) to (40 to 30), corners(4))
        assertEquals((0 to 0) to (0 to 40), corners(5))
        assertEquals((30 to 40) to (30 to 0), corners(7))
    }
}
