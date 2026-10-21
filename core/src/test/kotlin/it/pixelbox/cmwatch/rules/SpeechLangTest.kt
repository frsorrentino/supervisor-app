package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.SpeechLang.Lang.EN
import it.pixelbox.cmwatch.rules.SpeechLang.Lang.IT
import it.pixelbox.cmwatch.rules.SpeechLang.Piece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Voce bilingue (Franz, 21/10 00:57): in un testo italiano le frasi inglesi si leggono con la voce inglese; i termini
 * tecnici isolati restano italiani. Le frasi vengono dalle risposte della master e dagli handoff.
 */
class SpeechLangTest {
    private fun langs(text: String, base: SpeechLang.Lang = IT) = SpeechLang.split(text, base).map { it.lang }

    @Test fun unaRispostaItalianaConTerminiTecniciRestaItaliana() {
        for (t in listOf(
            "Tutto pushato su feature/tablet; contratto e fixture su master fino a 1.67.",
            "Ho fatto il commit e il push del branch, la CI è verde.",
            "APK release CI installato sul Pixel 11 Pro XL alle 23:13.",
            "Il workflow di build gira su GitHub Actions con il keystore nei secret.",
            "Golden: quando cambiano, si lancia il record e si scarica paparazzi-snapshots.",
            "Rilettura in sottofondo: prima /tail, transcript solo se il nodo manca.",
            "Pull request aperta, CI verde.",
        )) assertEquals(t, listOf(IT), langs(t))
    }

    @Test fun unaFraseIngleseInteraPassaAllInglese() {
        val t = "Ho mandato le note di versione. First closed test of Supervisor: see which Claude Code session waits on you. Poi ho salvato la bozza."
        assertEquals(
            listOf(
                Piece("Ho mandato le note di versione.", IT),
                Piece("First closed test of Supervisor: see which Claude Code session waits on you.", EN),
                Piece("Poi ho salvato la bozza.", IT),
            ),
            SpeechLang.split(t, IT),
        )
    }

    @Test fun lErroreCitatoTraVirgoletteSiLeggeInInglese() {
        val t = "Il build si ferma con «the system cannot find the file specified» e poi esce."
        assertEquals(
            listOf(
                Piece("Il build si ferma con", IT),
                Piece("«the system cannot find the file specified»", EN),
                Piece("e poi esce.", IT),
            ),
            SpeechLang.split(t, IT),
        )
    }

    @Test fun unaParentesiInglese() {
        val t = "Franz ha scelto la terza strada (keep the old channel and add a new one) per il test."
        assertEquals(listOf(IT, EN, IT), langs(t))
    }

    @Test fun sottoLeTreParoleNonSiCambiaVoce() {
        assertEquals(listOf(IT), langs("Ho letto «build failed» nel log."))
        assertEquals(listOf(IT), langs("Tests passed."))
        assertEquals(listOf(IT), langs("Fatto. Done! Ok."))
    }

    @Test fun bastanoDueParoleFunzionaliInglesi() =
        assertEquals(listOf(EN), langs("Build successful, all tests passed without warnings."))

    /** Dagli handoff del repo (prova del 21/10): frasi di soli nomi tecnici, che con le desinenze passavano all'inglese. */
    @Test fun unaFraseDiSoliNomiTecniciRestaNellaLinguaDiBase() {
        for (t in listOf("26 test TypeScript + 9 Python verdi.", "35 test verdi, npm run check ok.", "Memoria github-actions-credit.md.", "Il dominio my-pixelbox-it è pronto.",
            "Nuovo account (my-pixelbox-it, strumento Agent) in prova.", "Musica: tratto calmo sotto «It speaks.», rientro su «You answer.»"))
            assertEquals(t, listOf(IT), langs(t))
    }

    @Test fun leRigheDiElencoSiDecidonoUnaPerUna() {
        val t = "Esito: due cose.\nUpdate the contract to version 1.74.\nAggiornare i test del telefono."
        assertEquals(listOf(IT, EN, IT), langs(t))
    }

    @Test fun conLAppInIngleseSonoLeFrasiItalianeACambiare() {
        val t = "The relay is up. Il telefono non riceve le notifiche da ieri sera. Check the token."
        assertEquals(listOf(EN, IT, EN), langs(t, EN))
    }

    @Test fun iPezziDellaStessaLinguaSiUniscono() =
        assertEquals(
            listOf(Piece("Prima frase italiana. Seconda frase, sempre in italiano.", IT)),
            SpeechLang.split("Prima frase italiana. Seconda frase, sempre in italiano.", IT),
        )

    @Test fun ilTestoNonPerdeNiente() {
        val t = "Il build si ferma con «the system cannot find the file specified» e poi esce. Poi riparte."
        assertEquals(t.replace(Regex("\\s+"), ""), SpeechLang.split(t, IT).joinToString("") { it.text }.replace(Regex("\\s+"), ""))
    }

    @Test fun laLinguaDellaVoce() {
        assertEquals(IT, SpeechLang.Lang.of("it"))
        assertEquals(EN, SpeechLang.Lang.of("en"))
        assertNull(SpeechLang.Lang.of("de"))
    }

    @Test fun iPezziDaLeggereRestanoSottoIlLimiteEPortanoLaLingua() {
        val t = "Frase italiana di prova. Then a sentence in English that goes on. E di nuovo italiano."
        val pezzi = SpeechLang.chunks(t, IT, max = 30)
        assertEquals(listOf(IT, EN, EN, IT), pezzi.map { it.lang })
        assert(pezzi.all { it.text.length <= 30 })
    }

    @Test fun senzaLaVoceBilingueUnSoloPezzoNellaLinguaDiBase() =
        assertEquals(
            listOf(Piece("Ciao. Then a sentence in English that goes on.", IT)),
            SpeechLang.chunks("Ciao. Then a sentence in English that goes on.", IT, bilingual = false),
        )
}
