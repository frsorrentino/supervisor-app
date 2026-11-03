package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.KpiHealth
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

/** Contratto 1.110 sul `kpi` della fixture: win al lavoro (una sessione), disco locale 20 GiB liberi di 58. */
class KpiHostsTest {
    private val h: KpiHealth = run {
        val root = ContractJson.json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val id = root["cmd"]!!.jsonArray.first { it.jsonObject["op"]!!.jsonPrimitive.content == "kpi" }.jsonObject["id"]!!.jsonPrimitive.content
        val text = root["result"]!!.jsonArray.first { it.jsonObject["id"]!!.jsonPrimitive.content == id }.jsonObject["text"]!!.jsonPrimitive.content
        val health = ContractJson.json.parseToJsonElement(text).jsonObject["kpi_health"]!!
        ContractJson.json.decodeFromJsonElement(KpiHealth.serializer(), health)
    }

    @Test fun aWorkingHostStaysAndTheDiskWaits() {
        assertEquals(listOf("session"), h.hosts.single().workingWhy)
        assertEquals(listOf("win"), KpiHosts.shown(h).map { it.name })
        assertNull(KpiHosts.disk(h))
    }

    @Test fun aRestingHostGivesWayToTheDisk() {
        val rest = h.copy(hosts = h.hosts.map { it.copy(working = false, workingWhy = emptyList()) })
        assertTrue(KpiHosts.shown(rest).isEmpty())
        assertEquals(21474836480L, KpiHosts.disk(rest)!!.free)
        assertNull(KpiHosts.disk(rest.copy(localDisk = null)))
    }

    @Test fun anOlderRelayKeepsEveryHost() {
        val old = h.copy(hosts = h.hosts.map { it.copy(working = null) }, localDisk = null)
        assertEquals(listOf("win"), KpiHosts.shown(old).map { it.name })
        assertNull(KpiHosts.disk(old))
    }
}
