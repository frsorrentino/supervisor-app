package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Copia» su un file della chat: gli stessi casi di fileActions.test.ts nella web app. */
class FileActionsTest {
    @Test fun textFilesAreCopiedAsText() {
        for (m in listOf("text/plain", "text/markdown", "application/json", "application/xml", "image/svg+xml", "application/javascript", "text/csv; charset=utf-8"))
            assertEquals(m, FileActions.CopyKind.TEXT, FileActions.copyKind(m))
    }

    @Test fun imagesAreCopiedAsImages() {
        for (m in listOf("image/png", "image/jpeg", "image/webp")) assertEquals(m, FileActions.CopyKind.IMAGE, FileActions.copyKind(m))
    }

    // Franz, 15/10 22:19: «Nascondi Copia dove non copia il file».
    @Test fun noCopyWhereTheFileItselfWouldNotBeCopied() {
        for (m in listOf("application/pdf", "application/zip", "video/mp4", "application/octet-stream", null)) {
            assertEquals(m, FileActions.CopyKind.NONE, FileActions.copyKind(m))
            assertFalse(m, FileActions.canCopy(m))
        }
        assertTrue(FileActions.canCopy("text/plain"))
        assertTrue(FileActions.canCopy("image/png"))
    }

    // Franz, 16/10 11:18: le anteprime HTML come pagina, in una WebView separata: JavaScript sì, nessuna interfaccia JS, niente
    // file e content, niente cookie, niente navigazione e niente finestre. Lo stesso riquadro della web (PAGE_SANDBOX).
    @Test fun aPageIsShownInAnIsolatedView() {
        val p = FileActions.PAGE_VIEW
        assertTrue(p.javaScript)
        assertFalse(p.jsInterfaces || p.fileAccess || p.contentAccess || p.cookies || p.navigation || p.windows || p.domStorage)
        assertEquals("<h1>Hi</h1>", FileActions.pageDoc("text/html", "<h1>Hi</h1>"))
        assertEquals("<!doctype html><body style=\"margin:0\"><svg/>", FileActions.pageDoc("image/svg+xml", "<svg/>"))
    }
}
