package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

/** Voce della lettura scelta a orecchio fra quelle italiane del motore (Franz, 14/09 13:28: «una voce maschile»). */
class VoiceRulesTest {
    private val v = listOf("it-it-x-itb-local", "it-it-x-itc-local", "it-it-x-itd-local")

    @Test fun dallaPredefinitaSiPassaAllaPrima() = assertEquals("it-it-x-itb-local", VoiceRules.next(v, null))
    @Test fun siScorreInOrdine() = assertEquals("it-it-x-itd-local", VoiceRules.next(v, "it-it-x-itc-local"))
    @Test fun dopoLUltimaSiTornaAllaPredefinita() = assertNull(VoiceRules.next(v, "it-it-x-itd-local"))
    @Test fun unaVoceSparitaRipartedallaPrima() = assertEquals("it-it-x-itb-local", VoiceRules.next(v, "vecchia"))
    @Test fun senzaVociRestaLaPredefinita() = assertNull(VoiceRules.next(emptyList(), "x"))
    // Franz, 12/11 16:34 (schermata delle impostazioni: it-it-x-itc-local, 1,25×): chi non ha mai scelto parte dalla sua voce.
    private val pixel = listOf("it-IT-language", "it-it-x-itb-local", "it-it-x-itc-local", "it-it-x-itd-local", "it-it-x-kda-local", "it-it-x-itb-network", "it-it-x-itc-network")
    @Test fun maiSceltaParteDallaPreferita() = assertEquals("it-it-x-itc-local", VoiceRules.start(null, pixel))
    @Test fun senzaQuellaLocaleLaStessaDiRete() = assertEquals("it-it-x-itc-network", VoiceRules.start(null, listOf("it-IT-language", "it-it-x-itb-local", "it-it-x-itc-network")))
    @Test fun doveIlMotoreNonLHaRestaLaPredefinitaDelTelefono() = assertNull(VoiceRules.start(null, listOf("Samsung it-IT", "it-IT-language")))
    @Test fun inUnAltraLinguaNonSiParteDaUnaVoceItaliana() = assertNull(VoiceRules.start(null, pixel, italian = false))
    @Test fun laPredefinitaDelTelefonoSceltaResta() {
        assertEquals("", VoiceRules.stored(null)); assertNull(VoiceRules.start("", pixel))
    }
    @Test fun unaSceltaSalvataResta() {
        assertEquals("it-it-x-kda-local", VoiceRules.stored("it-it-x-kda-local")); assertEquals("it-it-x-kda-local", VoiceRules.start("it-it-x-kda-local", pixel))
    }
    @Test fun laPosizioneContaDaUno() {
        assertEquals(0, VoiceRules.position(v, null)); assertEquals(2, VoiceRules.position(v, "it-it-x-itc-local"))
    }
}
