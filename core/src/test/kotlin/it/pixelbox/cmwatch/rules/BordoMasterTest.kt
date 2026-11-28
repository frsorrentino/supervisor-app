package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il bordo della master (app-bordo-master-27-11): l'anello, quando il raggio gira, dove sta nel giro. Gli stessi casi di
 * web/src/lib/bordoMaster.test.ts.
 */
class BordoMasterTest {
    @Test fun `i valori della nota del mockup`() {
        assertEquals(0xFF93A696L, BordoMaster.COLORE)
        assertEquals(1.5f, BordoMaster.SPESSORE_DP, 0f)
        assertEquals(24f, BordoMaster.ANGOLO_DP, 0f)
        assertEquals(220f, BordoMaster.RAGGIO_DP, 0f)
        assertEquals(15_000L, BordoMaster.GIRO_MS)
        assertEquals(240, BordoMaster.SFUMA_MS)
        // La luce sale al 50 % in un terzo della lunghezza e torna a 0 negli altri due, nulla alle estremità.
        assertEquals(listOf(0f to 0f, 0.18f to 0.22f, 0.35f to 0.5f, 0.62f to 0.22f, 1f to 0f), BordoMaster.LUCE)
        assertEquals(BordoMaster.CAPO, BordoMaster.LUCE.maxBy { it.second }.first, 0f)
    }

    @Test fun `l'anello al 12 per cento sempre, al 35 mentre lavora con le animazioni ridotte`() {
        assertEquals(0.12f, BordoMaster.anello(lavora = false, ridotto = false), 0f)
        assertEquals(0.12f, BordoMaster.anello(lavora = true, ridotto = false), 0f)
        assertEquals(0.12f, BordoMaster.anello(lavora = false, ridotto = true), 0f)
        assertEquals(0.35f, BordoMaster.anello(lavora = true, ridotto = true), 0f)
    }

    @Test fun `il raggio gira solo mentre la master lavora e il movimento non è ridotto`() {
        assertTrue(BordoMaster.gira(lavora = true, ridotto = false))
        assertFalse(BordoMaster.gira(lavora = true, ridotto = true))
        assertFalse(BordoMaster.gira(lavora = false, ridotto = false))
        assertFalse(BordoMaster.gira(lavora = false, ridotto = true))
    }

    @Test fun `un giro ogni 15 secondi verso antiorario, dall'inizio del lato di sopra`() {
        assertEquals(0f, BordoMaster.posizione(0), 0f)
        // Verso antiorario la frazione del bordo, misurata in senso orario, cala.
        assertEquals(0.75f, BordoMaster.posizione(3_750), 1e-6f)
        assertEquals(0.5f, BordoMaster.posizione(7_500), 1e-6f)
        assertEquals(0.25f, BordoMaster.posizione(11_250), 1e-6f)
        assertEquals(0.2f, BordoMaster.posizione(12_000), 1e-6f)
        assertEquals(0f, BordoMaster.posizione(15_000), 0f)
        // Lineare: un secondo vale sempre un quindicesimo di giro.
        assertEquals(1f / 15f, BordoMaster.posizione(1_000) - BordoMaster.posizione(2_000), 1e-6f)
    }

    @Test fun `il giro dall'orologio, anche con un tempo grande o prima dello zero`() {
        // Le 22:21 del 27/11/2026, ora di Roma, in ms: un multiplo di 15 s, il raggio è all'inizio.
        assertEquals(0f, BordoMaster.posizione(1_795_814_460_000L), 0f)
        assertEquals(0.75f, BordoMaster.posizione(1_795_814_463_750L), 1e-6f)
        assertEquals(0.25f, BordoMaster.posizione(-3_750), 1e-6f)
        for (ms in listOf(-20_000L, -1L, 1L, 14_999L, 1_795_814_469_999L)) {
            val p = BordoMaster.posizione(ms)
            assertTrue("$ms: $p", p >= 0f && p < 1f)
        }
    }
}
