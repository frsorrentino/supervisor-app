package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Le citazioni nei messaggi (app-citazioni-estetica-26-11). Gli stessi casi di web/src/lib/citazione.test.ts. */
class CitazioneTest {
    @Test fun quoteThenAnswer() {
        val t = "> Per farla valere in tutte le sessioni conviene metterla nelle istruzioni globali\n> (CLAUDE.md): lo faccio se mi dai l'ok.\n\nok"
        assertEquals(
            listOf(
                Citazione.Part(true, "Per farla valere in tutte le sessioni conviene metterla nelle istruzioni globali\n(CLAUDE.md): lo faccio se mi dai l'ok."),
                Citazione.Part(false, "ok"),
            ),
            Citazione.parts(t),
        )
    }

    @Test fun noQuote() {
        assertEquals(listOf(Citazione.Part(false, "solo testo")), Citazione.parts("solo testo"))
        assertEquals(emptyList<Citazione.Part>(), Citazione.parts(""))
    }

    @Test fun textQuoteText() {
        assertEquals(
            listOf(Citazione.Part(false, "prima"), Citazione.Part(true, "citata"), Citazione.Part(false, "dopo")),
            Citazione.parts("prima\n>citata\ndopo"),
        )
        assertEquals("prima\n\ncitata\n\ndopo", Citazione.plain("prima\n> citata\ndopo"))
    }
}
