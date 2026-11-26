package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ResultItem
import it.pixelbox.cmwatch.contract.TranscriptFile
import it.pixelbox.cmwatch.contract.WaitFile
import it.pixelbox.cmwatch.contract.WaitItem
import it.pixelbox.cmwatch.contract.WaitOption
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La carta con molte immagini (app-carta-immagini-26-11, Franz 26/11 11:43: «non vedo tutti i mockup, dove posso vederli?»): la scheda
 * chiusa mostrava le prime quattro e taglia il resto senza dirlo. Ora, oltre quattro, tre miniature in ordine e una casella «+N».
 * Gli stessi casi di web/src/lib/cartaImmagini.test.ts.
 */
class CartaImmaginiTest {
    private val nomi = listOf("01-copia", "02-lettura", "03-al-lavoro", "04-cifra", "05-passi", "06-tieni-premuto", "07-anello", "08-orologio")
    private fun png(n: String) = WaitFile(path = "/p/$n.png", name = "$n.png", mime = "image/png", size = 30_000, kind = "image")
    private fun ok(files: List<WaitFile>) = WaitItem(id = "w61eeb4cc67", type = "carta", kind = "ok", text = "Animazioni", files = files)

    @Test fun eightImagesShowThreeAndCountTheRest() {
        val s = Scelta.thumbStrip(ok(nomi.map(::png)))
        assertEquals(listOf("01-copia.png", "02-lettura.png", "03-al-lavoro.png"), s.thumbs.map { it.file.name })
        assertEquals(5, s.more)
    }

    @Test fun upToFourAllShownNoCount() {
        val s = Scelta.thumbStrip(ok(nomi.take(4).map(::png)))
        assertEquals(nomi.take(4).map { "$it.png" }, s.thumbs.map { it.file.name })
        assertEquals(0, s.more)
        assertEquals(2, Scelta.thumbStrip(ok(nomi.take(5).map(::png))).more)
    }

    @Test fun documentsDoNotCount() {
        val pdf = WaitFile(path = "/p/piano.pdf", name = "piano.pdf", mime = "application/pdf", size = 1000, kind = "document")
        val s = Scelta.thumbStrip(ok(nomi.take(4).map(::png) + pdf))
        assertEquals(4, s.thumbs.size)
        assertEquals(0, s.more)
    }

    @Test fun choiceCountsOptionsWithImages() {
        val opts = (1..6).map { WaitOption(n = it, label = "Opzione $it", value = "$it", files = listOf(png(nomi[it - 1]))) }
        val s = Scelta.thumbStrip(WaitItem(id = "w2", type = "carta", kind = "choice", text = "Quale?", options = opts))
        assertEquals(listOf("A", "B", "C"), s.thumbs.map { it.letter })
        assertEquals(3, s.more)
    }

    // Un SendUserFile con l'HTML e gli otto PNG (11:30): il visore mostrava solo 01-copia.png. Ora gli altri file stanno sotto, tutti.
    private val html = TranscriptFile("/p/animazioni-proposte.html", "text/html", 9000)
    private fun pngs() = nomi.map { TranscriptFile("/p/$it.png", "image/png", 30_000) }

    @Test fun mockupShowsTheOtherSevenAndKeepsTheHtmlAsPage() {
        val r = ResultItem(id = "r1", kind = "mockup", mime = "text/html", path = html.path, files = listOf(html) + pngs())
        assertEquals(nomi.drop(1).map { "/p/$it.png" }, Guardare.others(r).map { it.path })
    }

    @Test fun imageMainListsTheHtmlAndTheOtherImages() {
        val r = ResultItem(id = "r2", kind = "image", mime = "image/png", path = "/p/01-copia.png", files = listOf(html) + pngs())
        assertEquals(listOf(html.path) + nomi.drop(1).map { "/p/$it.png" }, Guardare.others(r).map { it.path })
    }

    @Test fun singleFileHasNoOthers() {
        val r = ResultItem(id = "r3", kind = "image", mime = "image/png", path = "/p/01-copia.png", files = listOf(TranscriptFile("/p/01-copia.png", "image/png", 30_000)))
        assertEquals(emptyList<TranscriptFile>(), Guardare.others(r))
    }
}
