package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileCacheTest {
    private val video = "/home/franz/Desktop/workspaces/personali/film/out/bozza.mp4"

    // La conversazione chiusa e riaperta rilegge le stesse voci: stessa chiave, stesso file sul disco, nessuna richiesta al PC.
    @Test fun theSameFileKeepsItsKeyAcrossReopenings() {
        val f = TranscriptFile(video, "video/mp4", 12_582_912)
        assertEquals(FileCache.key(f, "e1"), FileCache.key(f.copy(), "e1"))
        assertEquals(FileCache.name(FileCache.key(f, "e1"), video), FileCache.name(FileCache.key(f.copy(), "e1"), video))
        // Lo stesso file mostrato da due voci si scarica una volta sola.
        assertEquals(FileCache.key(f, "e1"), FileCache.key(f, "e2"))
    }

    // Una bozza renderizzata di nuovo con lo stesso nome: il relay dà la misura di adesso, anche alle voci vecchie.
    @Test fun theSamePathWithAnotherSizeIsAnotherFile() {
        val before = FileCache.key(TranscriptFile(video, "video/mp4", 12_582_912), "e1")
        val after = FileCache.key(TranscriptFile(video, "video/mp4", 12_582_913), "e1")
        assertNotEquals(before, after)
        assertNotEquals(FileCache.name(before, video), FileCache.name(after, video))
    }

    // Senza misura (file sparito dal PC, sessione su win) decide la voce: un nuovo invio dello stesso percorso si riscarica.
    @Test fun withoutASizeTheEntryDecides() {
        val f = TranscriptFile(video, "video/mp4", null)
        assertEquals(FileCache.key(f, "e1"), FileCache.key(f, "e1"))
        assertNotEquals(FileCache.key(f, "e1"), FileCache.key(f, "e2"))
        assertNotEquals(FileCache.key(f, "e1"), FileCache.key(f.copy(size = 12_582_912), "e1"))
    }

    // Il nome sul disco finisce con quello del file: le app di sistema lo mostrano quando lo aprono.
    @Test fun theNameOnDiskEndsWithTheFileName() {
        val name = FileCache.name(FileCache.key(TranscriptFile(video, "video/mp4", 1), "e1"), video)
        assertTrue(name, name.endsWith("-bozza.mp4"))
        assertTrue(name, name.removeSuffix("-bozza.mp4").matches(Regex("[0-9a-f]{24}")))
    }

    // Oltre il tetto escono i file usati meno di recente, finché il resto ci sta.
    @Test fun overTheCapTheLeastRecentlyUsedLeave() {
        val files = listOf(FileCache.Entry("a", 100, used = 1), FileCache.Entry("b", 100, used = 3), FileCache.Entry("c", 100, used = 2))
        assertEquals(emptyList<String>(), FileCache.evict(files, max = 300))
        assertEquals(listOf("a"), FileCache.evict(files, max = 200))
        assertEquals(listOf("a", "c"), FileCache.evict(files, max = 150))
    }

    // Mai il file appena arrivato né quelli che la chat aperta mostra: tolti, l'anteprima li richiederebbe al PC.
    @Test fun theFilesInUseStay() {
        val files = listOf(FileCache.Entry("a", 100, used = 1), FileCache.Entry("b", 100, used = 3), FileCache.Entry("c", 100, used = 2))
        assertEquals(listOf("c"), FileCache.evict(files, max = 200, keep = setOf("a")))
        assertEquals(emptyList<String>(), FileCache.evict(files, max = 100, keep = setOf("a", "b", "c")))
    }
}
