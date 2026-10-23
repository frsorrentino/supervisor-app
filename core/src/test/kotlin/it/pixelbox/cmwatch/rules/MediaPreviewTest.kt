package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPreviewTest {
    @Test fun imagesUpTo10MbArriveOnTheirOwnOnAnyNetwork() {
        assertTrue(MediaPreview.auto("image/png", 24_645, unmetered = false))
        assertTrue(MediaPreview.auto("image/jpeg", 10_000_000, unmetered = false))
        assertTrue(MediaPreview.auto("image/png", null, unmetered = false))
        assertFalse(MediaPreview.auto("image/png", 10_000_001, unmetered = true))
    }

    @Test fun videosUpTo25MbArriveOnTheirOwnOnlyWithoutAMeteredNetwork() {
        assertTrue(MediaPreview.auto("video/mp4", 7_211_071, unmetered = true))
        assertTrue(MediaPreview.auto("video/mp4", 26_214_400, unmetered = true))
        assertFalse(MediaPreview.auto("video/mp4", 7_211_071, unmetered = false))
        assertFalse(MediaPreview.auto("video/mp4", 31_291_332, unmetered = true))
        // Senza misura un video potrebbe essere enorme: aspetta il tocco.
        assertFalse(MediaPreview.auto("video/mp4", null, unmetered = true))
    }

    // Franz, 15/10 22:20: «ogni file allegato mostri già l'anteprima». Gli stessi casi di media.test.ts nella web.
    @Test fun pdfsUpTo10MbAndTextsUpTo1MbArriveOnTheirOwnWithoutAMeteredNetwork() {
        assertTrue(MediaPreview.auto("application/pdf", 212_000, unmetered = true))
        assertFalse(MediaPreview.auto("application/pdf", 10_000_001, unmetered = true))
        assertFalse(MediaPreview.auto("application/pdf", 212_000, unmetered = false))
        assertTrue(MediaPreview.auto("text/markdown", 4_000, unmetered = true))
        assertTrue(MediaPreview.auto(null, 4_000, unmetered = true, path = "/w/notes/README.md"))
        assertFalse(MediaPreview.auto("text/plain", 1_000_001, unmetered = true))
        assertFalse(MediaPreview.auto("text/plain", 4_000, unmetered = false))
    }

    @Test fun theKindOfPreview() {
        assertEquals(MediaPreview.Kind.IMAGE, MediaPreview.kind("image/png"))
        // Franz, 16/10 11:18: HTML e SVG si vedono come pagina in una WebView isolata; XML resta testo.
        listOf("text/html", "image/svg+xml", "application/xhtml+xml", "text/html; charset=utf-8").forEach { assertEquals(it, MediaPreview.Kind.PAGE, MediaPreview.kind(it)) }
        assertEquals(MediaPreview.Kind.PAGE, MediaPreview.kind("application/octet-stream", "/w/site/index.html"))
        assertEquals(MediaPreview.Kind.TEXT, MediaPreview.kind("application/xml"))
        assertTrue(MediaPreview.auto("text/html", 2_000, unmetered = true)); assertFalse(MediaPreview.auto("text/html", 2_000_000, unmetered = true))
        assertEquals(MediaPreview.Kind.PDF, MediaPreview.kind("application/pdf"))
        assertEquals(MediaPreview.Kind.TEXT, MediaPreview.kind("application/octet-stream", "/w/x/notes.md"))
        assertEquals(MediaPreview.Kind.OTHER, MediaPreview.kind("application/zip", "a.zip"))
        assertFalse(MediaPreview.canPreview("application/zip"))
    }

    @Test fun theFirstLinesOfAText() {
        assertEquals("a\nb" to false, MediaPreview.firstLines("a\r\nb\n\n", 12))
        assertEquals((0 until 12).joinToString("\n") to true, MediaPreview.firstLines((0 until 20).joinToString("\n"), 12))
    }

    @Test fun otherFilesHaveAWideCardWithoutDownloading() {
        assertFalse(MediaPreview.auto("application/zip", 1_000, unmetered = true))
        assertFalse(MediaPreview.auto(null, 1_000, unmetered = true))
        assertTrue(MediaPreview.isVideo("video/webm"))
        assertFalse(MediaPreview.isVideo("image/gif"))
    }

    /** Franz 24/10 00:40 (anteprime in app): un HTML allegato si vede a tutta larghezza e alto quanto basta: il 60% dello schermo, fra 360 e 720. */
    @Test fun thePageFrameTakesSixtyPercentOfTheScreenBetween360And720() {
        assertEquals(360, MediaPreview.pageHeight(500))
        assertEquals(549, MediaPreview.pageHeight(915))
        assertEquals(720, MediaPreview.pageHeight(1400))
    }
}
