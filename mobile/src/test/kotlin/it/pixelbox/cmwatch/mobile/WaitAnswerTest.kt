package it.pixelbox.cmwatch.mobile

import it.pixelbox.cmwatch.mobile.ui.WaitAnswer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * app-ti-aspetta-lista-chiusa-08-11 (Franz 08/11 21:38: «quando clicco ok deve riportarmi alla schermata precedente»): una risposta
 * dalla scheda «Ti aspetta» manda il comando e poi chiude la scheda; una verifica che il PC non prende lascia la scheda aperta.
 */
class WaitAnswerTest {
    private val log = mutableListOf<String>()
    private val answer = WaitAnswer { log += "chiusa" }

    @Test fun aTapSendsThenCloses() {
        answer.two<String, String?> { a, t -> log += "manda $a ${t.orEmpty()}" }("approva", null)
        answer.two<String, String> { k, r -> log += "chiudi senza ok $k $r" }("w1", "non serve")
        answer.one<String> { k -> log += "non ora $k" }("w2")
        assertEquals(listOf("manda approva ", "chiusa", "chiudi senza ok w1 non serve", "chiusa", "non ora w2", "chiusa"), log)
    }

    @Test fun aCheckClosesOnlyWhenThePcTookIt() = runBlocking {
        assertEquals(false, answer.taken<String> { false }("p1"))
        assertEquals(emptyList<String>(), log)
        assertEquals(true, answer.taken<String, String, String?> { _, _, _ -> true }("p2", "non va", null))
        assertEquals(listOf("chiusa"), log)
    }
}
