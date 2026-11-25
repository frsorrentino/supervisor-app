package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.settings.pairedWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L'orologio coi dati veri (app-orologio-dati-demo-25-11, Franz 25/11 12:29: «l'app su orologio è ok, ma mostra i dati demo»).
 * La demo, accesa una volta dal tasto «Prova la demo» della schermata di accoppiamento o dalle impostazioni, restava accesa anche
 * dopo l'accoppiamento dal telefono, e la demo vince sul Transport vero: l'orologio accoppiato mostrava le fixture. Ora un
 * accoppiamento riuscito spegne la demo; e un accoppiamento che non regge (chiave che non si apre, Firebase che non parte) non
 * vale come accoppiato, così l'orologio chiede di accoppiarsi invece di mostrare in silenzio i dati finti.
 */
class OrologioDatiVeriTest {
    @Test fun accoppiarsiSpegneLaDemo() {
        val prima = Settings(demoMode = true)
        val dopo = prima.pairedWith(uid = "u1", host = "pc-franz", wrappedKey = "k")
        assertFalse(dopo.demoMode)
        assertTrue(dopo.paired)
        assertEquals("pc-franz", dopo.host)
        assertEquals(TransportChoice.Kind.FIREBASE, TransportChoice.pick(dopo.paired, hasKey = true, firebase = true, demo = dopo.demoMode))
    }

    @Test fun senzaChiaveOFirebaseNonSiEAccoppiati() {
        assertTrue(TransportChoice.linked(paired = true, hasKey = true, firebase = true))
        assertFalse(TransportChoice.linked(paired = true, hasKey = false, firebase = true))
        assertFalse(TransportChoice.linked(paired = true, hasKey = true, firebase = false))
        assertFalse(TransportChoice.linked(paired = false, hasKey = true, firebase = true))
    }

    /** Non accoppiato e senza demo: tile e complicazione non mostrano le sessioni del finto; con la demo scelta sì. */
    @Test fun senzaAccoppiamentoNienteSessioniFinte() {
        assertFalse(TransportChoice.showsData(paired = false, hasKey = false, firebase = false, demo = false))
        assertFalse(TransportChoice.showsData(paired = true, hasKey = false, firebase = true, demo = false))
        assertTrue(TransportChoice.showsData(paired = false, hasKey = false, firebase = false, demo = true))
        assertTrue(TransportChoice.showsData(paired = true, hasKey = true, firebase = true, demo = false))
    }

    /** Il codice a 6 cifre senza Firebase sull'orologio: non si finge l'accoppiamento (in debug il finto resta, per ARC). */
    @Test fun codiceSenzaFirebaseNonAccoppia() {
        assertFalse(TransportChoice.canPairWithCode(firebase = false, debug = false))
        assertTrue(TransportChoice.canPairWithCode(firebase = true, debug = false))
        assertTrue(TransportChoice.canPairWithCode(firebase = false, debug = true))
    }

    /** Accoppiato a metà e senza demo: la schermata è l'accoppiamento, non le sessioni del finto. */
    @Test fun accoppiamentoRottoChiedeDiAccoppiarsi() {
        val linked = TransportChoice.linked(paired = true, hasKey = false, firebase = true)
        val s = ViewState.reduce(it.pixelbox.cmwatch.data.Snapshot(null, it.pixelbox.cmwatch.contract.Freshness.Fresh), paired = linked, chosen = Screen.Sessions, seen = emptySet())
        assertEquals(Screen.Pairing, s)
    }
}
