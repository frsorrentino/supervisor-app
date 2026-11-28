package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Pagella
import it.pixelbox.cmwatch.contract.PagellaRow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** app-rendiconti-2-02-11: la pagina dei conti della fixture del relay (contratto 1.103, cmd-result-sample …0913). Stessi casi di conti.test.ts. */
class ContiRulesTest {
    private val page = Json.parseToJsonElement(File("../contract/cmd-result-sample.json").readText()).jsonObject.getValue("result").jsonArray
        .map { it.jsonObject }.first { it.getValue("id").jsonPrimitive.content.endsWith("0913") }.getValue("text").jsonPrimitive.content
        .let(ContractJson::decodeConti)

    /** Il rendiconto del mese: i clienti e il dettaglio misurato di uno (lavori, commit, release). */
    @Test fun monthWithClientsAndTheDetail() {
        assertEquals("2026-09", page.mese)
        assertEquals(listOf("acme-site"), page.clienti.map { it.name })
        assertEquals(7, page.dettaglio!!.measured.tasks)
        assertEquals(41, page.dettaglio!!.measured.commits)
        assertEquals("settembre 2026", ContiRules.monthName(page.mese, java.util.Locale.ITALIAN))
    }

    @Test fun pagellaOnlyFromTenCases() {
        assertEquals(listOf(Triple("Opus 5.5", 14, 14)), ContiRules.pagellaRows(page.pagella).map { Triple(ContiRules.modelName(it.model), it.n, ContiRules.ratePct(it)) })
    }

    @Test fun withoutFullRowsNoPagella() {
        assertEquals(emptyList<PagellaRow>(), ContiRules.pagellaRows(Pagella(0, 10, listOf(PagellaRow("claude-sonnet-5-5", "fix", 9)))))
        assertEquals(emptyList<PagellaRow>(), ContiRules.pagellaRows(null))
    }

    // app-pagella-effort-28-11: dal contratto 1.103 il consiglio della pagella è un oggetto; una stringa resta leggibile come testo.
    @Test fun pagella_consiglioOggettoOStringa() {
        val p = ContractJson.json.decodeFromString(Pagella.serializer(),
            """{"since":1,"advice":[{"kind":"fix","better":"claude-opus-5-5","worse":"claude-sonnet-5-5","rate_better":0.08,"rate_worse":0.21},"un consiglio scritto"]}""")
        assertEquals(2, p.advice.size)
        assertEquals("fix", p.advice[0].kind)
        assertEquals("Opus 5.5", ContiRules.modelName(p.advice[0].better))
        assertEquals(8, ContiRules.pct(p.advice[0].rateBetter))
        assertEquals(21, ContiRules.pct(p.advice[0].rateWorse))
        assertEquals(null, p.advice[0].text)
        assertEquals("un consiglio scritto", p.advice[1].text)
        assertEquals(null, ContiRules.pct(null))
    }
}
