package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FeaturesTest {
    private val c = Features.decode(File("../mobile/src/main/assets/features.json").readText())
    private val routes = setOf("prove", "decisions", "queue", "box-aspetta", "box-lavoro", "box-esiti", "lavori", "launch", "night", "search", "settings", "homeorder", "usage")

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
        assertEquals(setOf("rimasto_indietro", "stesso_difetto", "promemoria", "rendiconto", "pagella", "prima_raccolta"), coming.map { it.key }.toSet())
        assertFalse(coming.any(Features::canGo))
    }

    @Test fun panoramica_chiaviUnicheRotteConosciuteGruppiSenzaOrfani() {
        assertEquals(c.features.size, c.features.map { it.key }.toSet().size)
        c.features.forEach { f ->
            f.route?.let { assertTrue(f.key, it in routes) }
            assertTrue(f.key, c.groups.any { g -> g.key == f.group })
        }
        assertEquals(c.features.size, Features.grouped(c).sumOf { it.second.size })
    }

    @Test fun panoramica_demo_nessunaVoceTornaAllaHome() {
        c.features.forEach { f -> assertTrue(f.key, f.route != "home") }
        assertFalse(Features.canGo(c.features.first().copy(route = "home")))
        assertFalse(Features.canGo(c.features.first().copy(route = null)))
        assertTrue(Features.canGo(c.features.first { it.key == "cerca" }))
    }

    @Test fun panoramica_demo_ogniVoceHaLaSuaPagina() {
        c.features.forEach { f ->
            assertTrue(f.key, f.what.it.isNotBlank() && f.what.en.isNotBlank() && f.`when`.it.isNotBlank() && f.`when`.en.isNotBlank())
            if (f.state == Features.READY) assertTrue(f.key, f.how.it.isNotBlank() && f.how.en.isNotBlank())
        }
    }

    @Test fun panoramica_demo_ilRiquadroProvaSoloSuVociVere() {
        assertEquals(10, Features.DEMO_KEYS.size)
        Features.DEMO_KEYS.forEach { k -> assertTrue(k, c.features.any { it.key == k && it.state == Features.READY }) }
    }

    @Test fun panoramica_demo_iDatiDelRiquadroRiempionoIlTabellone() {
        val zone = java.time.ZoneId.of("Europe/Rome")
        val now = 1_793_560_000L
        val base = it.pixelbox.cmwatch.contract.ContractJson.decodeState(File("../contract/state-1-question.json").readText())
        val st = FeatureDemoState.state(base, now, true)
        val h = Kpi.head(st, FeatureDemoState.page(now, zone, true), now, zone)
        assertTrue(h.waiting > 0); assertTrue(h.busy > 0); assertEquals(6, h.outcomes)
        assertTrue(st.system != null && st.plugins != null && st.sessions.any { it.name == "master" })
        assertEquals(2, Suggestions.visible(st, emptySet(), now).shown.size)
    }

    @Test fun panoramica_demo_ogniVocePresenteHaRiquadroOImmagine() {
        c.features.filter { it.state == Features.READY }.forEach { f ->
            assertTrue("${f.key} senza riquadro né immagine", f.key in Features.DEMO_KEYS || f.image != null)
            f.image?.let { assertTrue(it, File("../mobile/src/main/assets/feature-images/$it").exists()) }
        }
    }
}
