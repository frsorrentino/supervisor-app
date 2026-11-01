package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SlotInfoTest {
    private fun sys(load: Double = .42, avail: Int = 5040, level: String = "green") = SystemStatus(at = 1, cores = 8, load1 = load * 8, loadPerCore = load, memTotalMb = 8000, memAvailableMb = avail, level = level, loadLevel = level, memLevel = "green")
    private fun session(name: String, state: SessionState) = Session(id = name, name = name, account = "a", project = name, state = state, since = 0)
    private val gb = 1_000_000_000L

    @Test fun slot_info_sistemaCaricoRamEMacchine() {
        val i = SlotInfo.system(sys(), listOf(HostStatus("win", true, 16 * gb, 40 * gb)), listOf(session("master", SessionState.IDLE), session("a", SessionState.BUSY), session("b", SessionState.IDLE), session("c", SessionState.GONE)))
        assertEquals(42, i.pct); assertEquals(42, i.barPct); assertEquals("green", i.level)
        assertEquals(37, i.ramPct)
        assertEquals(listOf(SlotInfo.HostRam("win", 40)), i.hosts)
        assertEquals(2, i.sessions)
    }

    @Test fun slot_info_sistemaOltreIlCentoLaBarraSiFerma() {
        val i = SlotInfo.system(sys(load = 1.6, level = "red"), emptyList(), emptyList())
        assertEquals(160, i.pct); assertEquals(100, i.barPct); assertEquals("red", i.level)
    }

    @Test fun slot_info_macchinaSpentaAlPostoDellaPercentuale() {
        val i = SlotInfo.system(sys(), listOf(HostStatus("win", false, 16 * gb, 40 * gb), HostStatus("nuc", true)), emptyList())
        assertEquals(listOf(SlotInfo.HostRam("win", null)), i.hosts)
    }

    private fun line(short: String, since: Long?, works: Int = 1) = Lavori.Line(Lavori.Who.YOU, List(works) { Lavori.Work(null, null, emptyList()) }, short, since = since)
    private val axis = Lavori.axis(1000, java.time.ZoneOffset.UTC)

    @Test fun slot_info_lavoriINumeriDellaSezioneEUltimoPerTe() {
        val ip = Lavori.InProgress(listOf(Lavori.Line(Lavori.Who.WORKING, List(2) { Lavori.Work(null, null, emptyList()) }, "w")), listOf(line("Vecchio", 100), line("Logo ambra", 900, works = 2)), emptyList(), axis)
        val i = SlotInfo.lavori(ip)
        assertEquals(3, i.you); assertEquals(ip.total, i.total); assertEquals(5, i.total); assertEquals(60, i.youPct)
        assertEquals("Logo ambra", i.last); assertEquals(900L, i.lastSince)
    }

    @Test fun slot_info_lavoriSeNessunoAspettaFranz() {
        val i = SlotInfo.lavori(Lavori.InProgress(emptyList(), emptyList(), emptyList(), axis))
        assertEquals(0, i.you); assertEquals(0, i.youPct); assertNull(i.last); assertNull(i.lastSince)
    }

    @Test fun slot_info_suggerimentiLaPrimaDellaCard() {
        val s = listOf(Suggestion("1", "idea", text = "prima"), Suggestion("2", "idea", text = "seconda"))
        assertEquals("prima", SlotInfo.suggestion(s))
        assertNull(SlotInfo.suggestion(emptyList()))
    }

    @Test fun slot_info_pluginNomiOOraDelControllo() {
        val up = SlotInfo.plugins(PluginsSummary(1600, listOf(PluginsAccountSummary("a", listOf("alfa@m", "beta@m")), PluginsAccountSummary("b", listOf("alfa@m")))))
        assertEquals(listOf("alfa", "beta"), up.names); assertNull(up.checkedAt)
        val none = SlotInfo.plugins(PluginsSummary(1600, listOf(PluginsAccountSummary("a"))))
        assertEquals(emptyList<String>(), none.names); assertEquals(1600L, none.checkedAt)
    }
}
