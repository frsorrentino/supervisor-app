package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ProofStep
import it.pixelbox.cmwatch.contract.ProofSteps
import it.pixelbox.cmwatch.contract.Session
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Le tappe della prova su un filo (app-animazioni-passi-26-11, contratto 1.162), dalla fixture del relay
 * (contract/steps-in-verifica.json). Gli stessi casi di web/src/lib/passiProva.test.ts.
 */
class PassiProvaTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val sessions: List<Session> = run {
        val root = Json.parseToJsonElement(java.io.File("../contract/steps-in-verifica.json").readText()).jsonObject
        // Le sessioni della fixture hanno solo nome e tappe: si completano coi campi obbligatori di una sessione vera.
        val base = it.pixelbox.cmwatch.contract.ContractJson.decodeState(java.io.File("../contract/state-1-question.json").readText()).sessions.first()
        root["sessions"]!!.jsonArray.map { e ->
            val o = e.jsonObject
            base.copy(name = o["name"].toString().trim('"'), steps = o["steps"]?.let { json.decodeFromJsonElement(ProofSteps.serializer(), it) })
        }
    }

    @Test fun fixtureReadsStepsAndNull() {
        val s = sessions.first { it.name == "supervisor-app" }.steps!!
        assertEquals("app-animazioni-otto-26-11", s.task)
        assertEquals(listOf("Test", "CI", "Pre-release", "Prova"), s.items.map { it.label })
        assertEquals(listOf(PassiProva.Look.DONE, PassiProva.Look.DONE, PassiProva.Look.NOW, PassiProva.Look.TODO), s.items.map { PassiProva.look(it) })
        assertNull(sessions.first { it.name == "gamma" }.steps)
    }

    @Test fun railReachesTheCurrentStep() {
        val s = sessions.first { it.name == "supervisor-app" }.steps!!
        assertEquals(2, PassiProva.reach(s.items))
        assertEquals(2f / 3f, PassiProva.fill(s.items), 0.001f)
        val all = s.items.map { it.copy(state = "ok") }
        assertEquals(1f, PassiProva.fill(all), 0.001f)
        assertEquals(0f, PassiProva.fill(s.items.map { it.copy(state = "todo") }), 0.001f)
    }

    @Test fun lineSaysWhereItIs() {
        val s = sessions.first { it.name == "supervisor-app" }.steps!!
        assertEquals(PassiProva.Line(PassiProva.Say.RUNNING, "Pre-release"), PassiProva.line(s))
        val ko = s.copy(items = s.items.mapIndexed { i, x -> if (i == 1) x.copy(state = "ko") else x })
        assertEquals(PassiProva.Line(PassiProva.Say.FAILED, "CI"), PassiProva.line(ko))
        assertEquals(PassiProva.Line(PassiProva.Say.TODO, "Prova"), PassiProva.line(s.copy(items = s.items.map { if (it.key == "proof") it else it.copy(state = "ok") })))
        assertEquals(PassiProva.Line(PassiProva.Say.ALL_DONE, ""), PassiProva.line(s.copy(items = s.items.map { it.copy(state = "ok") })))
        assertNull(PassiProva.line(null))
        assertNull(PassiProva.line(ProofSteps("t", emptyList<ProofStep>())))
    }

    @Test fun stateWithoutStepsKeyDecodes() {
        // Un relay prima della 1.162, o una sessione senza compito in verifica: la chiave manca e si legge null.
        val st = it.pixelbox.cmwatch.contract.ContractJson.decodeState(java.io.File("../contract/state-1-question.json").readText())
        assertNull(st.sessions.first().steps)
    }
}
