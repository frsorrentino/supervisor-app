package it.pixelbox.cmwatch.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * app-verifica-file-prova-12-11 (prova #601 di Franz, 12/11 22:22, Pixel con l'app 0.2.2129): in Verifica l'immagine di una prova
 * restava un riquadro grigio col nome e il documento non si apriva, e nel relay.log non c'era nessuna richiesta `file`. La pagina
 * Verifica del telefono si apre prima dei punti di Main che danno `LocalWaitFiles`, e lì valeva il fornitore vuoto, che non chiede
 * niente. Ogni pagina di Main che mostra i file di una richiesta deve stare dentro un `LocalWaitFiles provides`.
 */
class WaitFilesProvidedTest {
    private val src = File("src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt").readText()

    @Test fun everyPageWithRequestFilesHasTheFilesHost() {
        val bare = Regex("""\bDaProvareScreen\(""").findAll(src).map { m -> m.range.first }
            .filter { at -> !src.substring(maxOf(0, at - 400), at).contains("LocalWaitFiles provides") }
            .map { at -> src.substring(0, at).count { it == '\n' } + 1 }.toList()
        assertEquals("righe di MainActivity con Verifica senza i file delle richieste", emptyList<Int>(), bare)
    }
}
