package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * app-verifica-vuota-16-11 (Franz 16/11 18:07, sul telefono: «Verifica 5», aperta «Niente da provare»): le prove di una sessione aperta
 * stanno nella sua scheda, e Verifica ha una riga per sessione col suo numero, come il web dal 12/11. Elenco e numero dicono la stessa
 * cosa: le prove fuori più quelle delle righe sono il numero di Verifica.
 */
class VerificaSessioniTest {
    private val file = Json.parseToJsonElement(javaClass.getResource("/verifica-cases.json")!!.readText()).jsonObject
    private val page = ContractJson.decodeProve(file["prove"].toString())

    @Test fun theListSaysWhatTheNumberCounts() = file["cases"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val name = o["name"]!!.jsonPrimitive.content
        val live = o["live"]!!.jsonArray.map { x -> x.jsonPrimitive.content }.toSet()
        val v = ToccaRules.verifica(page, { p -> p.session }, live)
        assertEquals(name, o["outside"]!!.jsonArray.map { x -> x.jsonPrimitive.long }, v.outside.groups.flatMap { g -> g.prove.map { p -> p.id } })
        assertEquals(name, o["rows"]!!.jsonArray.map { r -> r.jsonArray.let { a -> ToccaRules.InSession(a[0].jsonPrimitive.content, a[1].jsonPrimitive.int) } }, v.inSessions)
        assertEquals(name, o["outsideEmpty"]!!.jsonPrimitive.boolean, v.outsideEmpty)
        // Il numero di Verifica (il conto del relay, qui 5) è quello che la sezione mostra: prove fuori più righe.
        assertEquals(name, 5, v.outside.n + v.inSessions.sumOf { r -> r.n })
    }

    /** Gli stessi casi di toccaRules.test.ts (`proveInSessions`): una riga per sessione con prove, la più piena prima; le sessioni con sole decisioni no. */
    @Test fun oneRowPerSessionWithTests() {
        assertEquals(
            listOf(ToccaRules.InSession("win:supervisor-app", 29), ToccaRules.InSession("storefront", 3)),
            ToccaRules.proveInSessions(mapOf("win:supervisor-app" to (1 to 29), "blog" to (2 to 0), "storefront" to (0 to 3))),
        )
        assertEquals(emptyList<ToccaRules.InSession>(), ToccaRules.proveInSessions(emptyMap()))
    }
}
