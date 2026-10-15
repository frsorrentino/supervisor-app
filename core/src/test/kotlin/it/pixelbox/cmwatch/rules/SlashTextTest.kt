package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Gli stessi casi di slashText.test.ts nella web. */
class SlashTextTest {
    private val p = mapOf("clear" to "svuotare il contesto", "compact" to "compattare la conversazione", "resume" to "riprendere la conversazione")
    private fun r(t: String) = SlashText.readable(t) { p[it] ?: it }

    @Test fun aKnownCommandBecomesAPhraseWithTheCommandInBrackets() {
        assertEquals("svuotare il contesto (/clear)", r("/clear"))
        assertEquals("compattare la conversazione (/compact)", r(" /compact "))
        assertEquals("riprendere la conversazione (/resume abc)", r("/resume abc"))
    }

    @Test fun theRestStaysAsItIs() {
        assertEquals("ok to publish the post", r("ok to publish the post"))
        assertEquals("/deploy", r("/deploy"))
        assertNull(SlashText.known("run /clear later"))
    }

    @Test fun theWaitingLinesOfTheReport() {
        assertEquals("!: svuotare il contesto (/clear)", SlashText.readableWaiting("!: /clear") { p[it] ?: it })
        assertEquals("domanda: Pubblico?", SlashText.readableWaiting("domanda: Pubblico?") { p[it] ?: it })
    }
}
