package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.ContractJson
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

    /** «Ti aspetta da 66 g» nella scheda di una sessione: le attese della fixture erano di settembre. Ora la più vecchia è di un paio d'ore, come nella fixture. */
    @Test fun theWaitsAreOfToday() = runBlocking {
        val r = demo.send(Cmd("c1", CmdOp.ATTESE, issued = now, by = "test")) {}
        val p = ContractJson.decodeWaits(r.text)
        assertEquals(now, p.at)
        assert(p.items.isNotEmpty() && p.items.all { w -> now - w.at in 0..86_400 }) { p.items.map { w -> now - w.at }.toString() }
        val one = demo.send(Cmd("c2", CmdOp.ATTESA, arg = p.items.first().id, issued = now, by = "test")) {}
        if (one.ok) assert(now - ContractJson.decodeWait(one.text).at in 0..86_400) { one.text.take(200) }
    }

    /** La demo dichiara `conti`: l'app lo chiede all'avvio e prima riceveva «CONTI: not in this demo yet». */
    @Test fun theDemoAnswersConti() = runBlocking {
        val st = demo.state.first()
        assert(st.ops?.contains("conti") == true)
        val r = demo.send(Cmd("c3", CmdOp.CONTI, issued = now, by = "test")) {}
        assert(r.ok) { r.text }
        assert(ContractJson.decodeConti(r.text).clienti.isNotEmpty())
        val a = demo.send(Cmd("c4", CmdOp.CONTI_AZIONE, arg = "2026-09", issued = now, by = "test", project = "work/acme-site")) {}
        assert(a.ok) { a.text }
    }

    /** Aprire una sezione della home salva l'ordine: la demo rispondeva «the demo does not save the order» a ogni tocco. */
    @Test fun theDemoKeepsTheHomeOrder() = runBlocking {
        val r = demo.send(Cmd("c5", CmdOp.HOME_ORDER, arg = "[\"lavori\", \"sessions\"]", issued = now, by = "test")) {}
        assert(r.ok) { r.text }
        assertEquals(listOf("lavori", "sessions"), demo.state.first().homeOrder)
        assert(!demo.send(Cmd("c6", CmdOp.HOME_ORDER, arg = "{}", issued = now, by = "test")) {}.ok)
    }

    /** «Riapri» sulla master della home: la demo diceva «master is not a closed session»; ora la master c'è, ferma, con l'esito. */
    @Test fun theDemoReopensTheMaster() = runBlocking {
        assert(demo.state.first().sessions.none { s -> s.name == "master" })
        val r = demo.send(Cmd("c7", CmdOp.REOPEN, session = "master", issued = now, by = "test")) {}
        assert(r.ok) { r.text }
        val m = demo.state.first().sessions.single { s -> s.name == "master" }
        assertEquals(it.pixelbox.cmwatch.contract.SessionState.IDLE, m.state)
        assert(m.outcome?.full?.contains("Prossimi:") == true)
    }
}
