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

    /** app-download-fuori-scheda-04-11 (Franz, 04/11 07:15): i file vanno al gestore dei download, le pagine al browser. */
    @Test fun aFileLinkIsADownload() {
        assertEquals(Links.Download("supervisor-grafica.apk", true), Links.download("https://github.com/frsorrentino/supervisor-app/releases/download/grafica-2026-11-04/supervisor-grafica.apk"))
        assertEquals(Links.Download("promo.MP4", false), Links.download("https://example.com/v/promo.MP4?x=1#t"))
        assertEquals(Links.Download("dist.zip", false), Links.download("https://example.com/dist.zip"))
        // Una release senza estensione nota resta un download, perché /releases/download/ è un file.
        assertEquals(Links.Download("notes", false), Links.download("https://github.com/o/r/releases/download/v1/notes"))
        assertEquals(null, Links.download("https://github.com/frsorrentino/supervisor-app/releases/tag/grafica-2026-11-04"))
        assertEquals(null, Links.download("https://example.com/report.html"))
        assertEquals(null, Links.download("https://example.com/"))
        assertEquals(null, Links.download("intent://x.apk"))
    }
}
