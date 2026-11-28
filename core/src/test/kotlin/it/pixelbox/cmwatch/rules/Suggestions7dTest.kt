package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.KpiData
import it.pixelbox.cmwatch.contract.KpiSuggestions
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * app-suggerimenti-push-27-11 (Franz 27/11 21:16, «la percentuale non si aggiorna in tempo reale»): contratto 1.164, i numeri dei
 * suggerimenti dei sette giorni in ogni push. La fixture è quella del relay, identica.
 */
class Suggestions7dTest {
    private val sample: KpiSuggestions = ContractJson.json.decodeFromJsonElement(
        KpiSuggestions.serializer(),
        ContractJson.json.parseToJsonElement(Fixtures.read("suggestions-7d-sample.json")).jsonObject.getValue("suggestions_7d"),
    )
    private val kpi = KpiData(at = 0, suggestions7d = KpiSuggestions(expired = 23))
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)

    @Test fun `la fixture del relay da 5 accettati su 23, cioe 22 per cento`() {
        assertEquals(22, Kpi.acceptRate(sample))
    }

    @Test fun `con il campo nella push vale la push, anche se l'op kpi e piu vecchia`() {
        assertEquals(sample, Kpi.suggestions7d(idle.copy(suggestions7d = sample), kpi))
    }

    @Test fun `senza il campo, con un relay precedente, vale l'op kpi come prima`() {
        assertNull(idle.suggestions7d)
        assertEquals(kpi.suggestions7d, Kpi.suggestions7d(idle, kpi))
    }

    @Test fun `senza push ne op kpi non c'e niente da mostrare`() {
        assertNull(Kpi.suggestions7d(null, null))
    }
}
