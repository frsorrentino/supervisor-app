package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FeaturesTest {
    private val c = Features.decode(File("../mobile/src/main/assets/features.json").readText())
    private val routes = setOf("home", "queue", "box-aspetta", "box-lavoro", "box-esiti", "lavori", "launch", "night", "search", "settings", "homeorder", "usage")

    @Test fun panoramica_ogniSezioneDellaHomeHaLaSuaVoce() {
        assertEquals(emptyList<String>(), Features.missingSections(c, HomeOrder.DEFAULT))
        assertEquals(listOf("nuova"), Features.missingSections(c, listOf("nuova")))
    }

    @Test fun panoramica_ogniVoceCheCeHaLaProvaNelCodice() {
        c.features.filter { it.state == Features.READY }.forEach { f ->
            assertTrue(f.key, f.phone != null || f.web != null)
            listOfNotNull(f.phone, f.web).forEach { p -> assertTrue("${f.key}: ${p.file} ${p.needle}", File("../${p.file}").readText().contains(p.needle)) }
        }
    }

    @Test fun panoramica_leDieciDelProgrammaSonoInArrivoESenzaTocco() {
        val coming = c.features.filter { it.state == Features.COMING }
        assertEquals(setOf("banco_prova", "cinque_minuti", "ok_informato", "rimasto_indietro", "stesso_difetto", "promemoria", "rendiconto", "pagella", "panoramica", "prima_raccolta"), coming.map { it.key }.toSet())
        assertFalse(coming.any(Features::tappable))
    }

    @Test fun panoramica_chiaviUnicheRotteConosciuteGruppiSenzaOrfani() {
        assertEquals(c.features.size, c.features.map { it.key }.toSet().size)
        c.features.forEach { f ->
            f.route?.let { assertTrue(f.key, it in routes) }
            assertTrue(f.key, c.groups.any { g -> g.key == f.group })
        }
        assertEquals(c.features.size, Features.grouped(c).sumOf { it.second.size })
    }
}
