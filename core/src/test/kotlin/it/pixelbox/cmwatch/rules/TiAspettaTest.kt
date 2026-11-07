package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

/** La frase accesa e l'età della scheda «Ti aspetta», coi casi che legge anche web/src/lib/tiAspetta.test.ts. */
class TiAspettaTest {
    private val file = Json.parseToJsonElement(javaClass.getResource("/ti-aspetta-cases.json")!!.readText()).jsonObject

    @Test fun marks() = file["marks"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val want = o["out"]!!.jsonArray.map { s -> s.jsonArray.let { TiAspetta.Seg(it[0].jsonPrimitive.content, it[1].jsonPrimitive.boolean) } }
        assertEquals(o["name"]!!.jsonPrimitive.content, want, TiAspetta.marks(o["text"]!!.jsonPrimitive.content, o["ask"]!!.jsonPrimitive.content))
    }

    @Test fun links() = file["links"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        assertEquals(o["link"]!!.jsonPrimitive.content, (o["out"] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content, TiAspetta.waitOf(o["link"]!!.jsonPrimitive.content))
    }

    /** La pagina dalla voce della fixture (contratto 1.130): il pezzo con la frase da accendere e il salto, le altre senza di lei. */
    @Test fun pageFromTheFixture() {
        val fake = it.pixelbox.cmwatch.transport.FakeTransport(load = { java.io.File("../contract/$it.json").readText() })
        val one = it.pixelbox.cmwatch.contract.ContractJson.decodeWait(fake.sampleResult("attesa", "wd2f87e85e2").second)
        val all = it.pixelbox.cmwatch.contract.ContractJson.decodeWaits(fake.sampleText("attese")).items
        val p = TiAspetta.pageOf(one, all)
        assertEquals(TiAspetta.Kind.CHOOSE, p.kind); assertEquals("atlas-shop", p.session)
        assertEquals("5d2e8f1a-3c47-4b9e-8a61-7e0d9c4b2f18.0", p.source?.entry)
        assertEquals(listOf("wb973ef3ed0"), p.others.map { it.key })
        assertEquals(true, TiAspetta.marks(p.source!!.text, p.source!!.ask).any { it.on })
    }

    @Test fun age() = file["age"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        assertEquals(o["out"]!!.jsonPrimitive.content, TiAspetta.age(o["s"]!!.jsonPrimitive.long))
    }
}
