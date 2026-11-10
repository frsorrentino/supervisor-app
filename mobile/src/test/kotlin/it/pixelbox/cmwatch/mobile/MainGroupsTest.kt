package it.pixelbox.cmwatch.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Il corpo di `Main` non chiama i composable delle risorse (app-scorrimento-prima-sessione-09-11, telefono di Franz 10/11 08:04).
 * Main è un solo gruppo di Compose con centinaia di rami: `screen = open?.let { stringResource(...) }` aggiungeva un gruppo solo
 * con una sessione aperta, i gruppi dopo slittavano e Compose rifaceva da capo la home e la pagina appena aperta. Al rilascio del
 * dito la chat ripartiva vuota per un fotogramma (prima, in cima). Visto nell'emulatore con una traccia per livello: al rilascio si
 * smontavano CompositionLocalProvider, AppShell, il pager e SessionSheet, mentre il codice di Main prima di loro restava. Dentro
 * Main i testi vengono da `getString`, che non è un composable.
 */
class MainGroupsTest {
    private val main: String = File("src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt").readText().let { src ->
        val start = src.indexOf("private fun Main(")
        assertTrue("Main non trovato", start >= 0)
        val end = Regex("""\n    (private |internal )?fun """).find(src, start + 1)?.range?.first ?: src.length
        src.substring(start, end)
    }

    @Test fun mainNeverCallsResourceComposables() {
        val calls = Regex("""\b(stringResource|pluralStringResource|painterResource)\(""").findAll(main).map { it.value }.toList()
        assertEquals(emptyList<String>(), calls)
    }
}
