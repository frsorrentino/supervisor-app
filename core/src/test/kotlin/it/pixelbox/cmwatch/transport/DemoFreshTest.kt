package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.rules.UtilizzoRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * La demo è di adesso (app-play-aggiornamento-18-11): la fixture ha `published_at` di settembre, e senza spostarlo la demo
 * risultava vecchia — la quota in home diceva «lettura di 66 g fa», proprio nelle schermate per Play e per chi la revisiona.
 */
class DemoFreshTest {
    private val now = 1_800_000_000L
    private val demo = FakeTransport(load = { n -> File("../contract/$n.json").readText() }, now = { now })

    @Test fun theDemoStateIsFresh() = runBlocking {
        val st = demo.state.first()
        assertEquals(Freshness.Fresh, Freshness.of(st, now))
        assertNull(UtilizzoRules.dataAge(st, now))
    }

    @Test fun theQuotaResetLineIsAnHourNotAnAge() = runBlocking {
        val st = demo.state.first()
        val q = st.quota.values.first { it.resetW7 != null }
        val line = UtilizzoRules.resetLine(q.resetW7, now, q.stale, UtilizzoRules.dataAge(st, now))
        assert(line is UtilizzoRules.ResetLine.At) { "$line" }
    }
}
