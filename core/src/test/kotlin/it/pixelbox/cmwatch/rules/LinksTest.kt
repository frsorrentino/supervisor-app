package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** I link nei messaggi della chat, da rendere toccabili (Franz, 01/10 20:05). */
class LinksTest {
    private fun found(t: String) = Links.find(t).map { t.substring(it) }

    @Test fun findsPlainUrls() {
        assertEquals(listOf("https://claude.ai/artifact/RGdmD5fxUWUBLSbwWBBsCz"), found("Mockup: https://claude.ai/artifact/RGdmD5fxUWUBLSbwWBBsCz"))
    }

    @Test fun trailingPunctuationStaysOut() {
        assertEquals(listOf("https://example.com/x"), found("vedi https://example.com/x."))
        assertEquals(listOf("https://example.com/x"), found("(https://example.com/x)"))
        assertEquals(listOf("https://example.com/x"), found("«https://example.com/x»,"))
        assertEquals(listOf("https://example.com/x"), found("[link](https://example.com/x)"))
    }

    @Test fun keepsBalancedParenthesesAndQueries() {
        assertEquals(listOf("https://it.wikipedia.org/wiki/Roma_(città)"), found("https://it.wikipedia.org/wiki/Roma_(città) qui"))
        assertEquals(listOf("http://example.com/p?q=1&r=2#s"), found("http://example.com/p?q=1&r=2#s"))
    }

    @Test fun severalOrNone() {
        assertEquals(2, Links.find("https://example.com e https://example.org").size)
        assertEquals(0, Links.find("nessun link, solo claude.ai senza schema").size)
    }
}
