package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Commitment
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Session
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * app-allegati-carta-27-11: a chi vanno gli allegati della risposta di una carta (Ti aspetta, Banco prova). Gli stessi casi di
 * web/src/lib/allegatiCarta.test.ts, in core/src/test/resources/allegati-carta-cases.json.
 */
class AllegatiCartaTest {
    private val file = ContractJson.json.parseToJsonElement(javaClass.getResource("/allegati-carta-cases.json")!!.readText()).jsonObject
    private val state = ContractJson.decodeState(File("../contract/state-1-question.json").readText()).copy(
        sessions = ContractJson.json.decodeFromJsonElement(ListSerializer(Session.serializer()), file.getValue("sessions")),
        commitments = ContractJson.json.decodeFromJsonElement(ListSerializer(Commitment.serializer()), file.getValue("commitments")),
    )
    private fun str(o: kotlinx.serialization.json.JsonObject, k: String): String? = (o[k] as? JsonPrimitive)?.takeIf { p -> p.isString }?.content

    @Test fun `i casi condivisi con la web`() = file.getValue("cases").jsonArray.forEach { c ->
        val o = c.jsonObject
        assertEquals(o.getValue("name").jsonPrimitive.content, o.getValue("out").jsonPrimitive.content, AllegatiCarta.target(str(o, "session"), str(o, "task"), state))
    }

    @Test fun `senza stato vanno alla master`() {
        assertEquals(ContextActions.MASTER, AllegatiCarta.target("alfa", "app-x", null))
    }

    @Test fun `la carta di prova della fixture del relay va alla sua sessione, se è viva`() {
        val items = ContractJson.decodeWaits(ContractJson.json.parseToJsonElement(File("../contract/attese-rd-sample.json").readText()).jsonObject.getValue("attese").toString()).items
        val prova = items.first { w -> w.kind == "prova" }
        assertEquals("win:alfa", prova.session)
        val live = state.copy(sessions = state.sessions + state.sessions.first().copy(id = "s9", name = "win:alfa"))
        assertEquals("win:alfa", AllegatiCarta.target(prova.session, prova.task, live))
        // La raccolta non ha né sessione né compito: la master.
        val raccolta = items.first { w -> w.kind == "raccolta" }
        assertEquals(ContextActions.MASTER, AllegatiCarta.target(raccolta.session, raccolta.task, live))
    }

    @Test fun `lo stesso tetto del campo delle sessioni`() = assertEquals(5, AllegatiCarta.MAX)
}
