package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.transport.FakeTransport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Le prove citate nei messaggi (app-prova-link-10-11, contratto 1.134). Gli stessi casi in web/src/lib/provaLinks.test.ts. */
class ProvaLinksTest {
    private fun refs(t: String) = ProvaLinks.find(t).map { r -> t.substring(r.range) to r.n }

    @Test fun aProvaCitedInATextIsALink() {
        assertEquals(listOf("prova #518" to 518L), refs("La prova #518 è segnata «non va»."))
        assertEquals(listOf("Prova #518" to 518L), refs("Prova #518 segnata «non va» con le sue parole"))
        assertEquals("il numero nudo di tre cifre", listOf("prove 543" to 543L), refs("prove 543 ok; il controllo gira"))
    }

    @Test fun severalProveInOneSentenceAreEachALink() {
        assertEquals(listOf("prove #518" to 518L, "#527" to 527L), refs("le prove #518 e #527 sono da Franz"))
        assertEquals(listOf("prove 518" to 518L, "527" to 527L, "543" to 543L), refs("prove 518, 527 e 543 passate"))
    }

    @Test fun whatIsNotAProvaStaysText() {
        assertEquals(emptyList<Pair<String, Long>>(), refs("prova 10 minuti e poi dimmi"))
        assertEquals(emptyList<Pair<String, Long>>(), refs("approva #518"))
        assertEquals(emptyList<Pair<String, Long>>(), refs("la prova è passata"))
    }

    @Test fun theLinkOpensTheProva() {
        assertEquals("#/prova/518", ProvaLinks.link(518))
        assertEquals(518L, ProvaLinks.idOf("https://supervisor.example.com/?demo#/prova/518"))
        assertEquals(518L, ProvaLinks.idOf("#/prova/518"))
        assertNull(ProvaLinks.idOf("https://supervisor.example.com/#/monitor/prezzi"))
        assertNull(ProvaLinks.idOf("#/prova/x"))
    }

    // Contratto 1.134: la prova sola; un relay vecchio manda l'elenco (senza id in cima): non supportato; il rifiuto in una riga.
    @Test fun readingOneProvaByNumber() {
        val found = ProvaLinks.read(true, """{"id": 518, "task": "app-x", "short": "X", "title": null, "session": "s", "project": "p", "build": "b", "dove": "home", "fai": "tocca", "atteso": "vede", "state": "ko", "text": "non va", "at": 100}""")
        assertTrue(found is ProvaLinks.Read.Found)
        assertEquals("ko", (found as ProvaLinks.Read.Found).prova.state)
        assertEquals(ProvaLinks.Read.Unsupported, ProvaLinks.read(true, """{"at": 1, "n": 2, "builds": 1, "groups": [], "ko": [], "red": []}"""))
        assertEquals(ProvaLinks.Read.Refused("prova 999: non esiste"), ProvaLinks.read(false, "prova 999: non esiste"))
    }

    @Test fun theDemoReadsOneProvaFromTheList() {
        val demo = FakeTransport(load = { File("../contract/$it.json").readText() })
        var n = 0
        fun read(arg: String?) = runBlocking { demo.send(Cmd(id = "p${n++}", op = CmdOp.PROVE, session = null, arg = arg, issued = 0, by = "test")) {} }
        val list = it.pixelbox.cmwatch.contract.ContractJson.decodeProve(read(null).text)
        val id = list.groups.first().prove.first().id
        val one = read(id.toString())
        val r = ProvaLinks.read(one.ok, one.text)
        assertTrue("$r", r is ProvaLinks.Read.Found && r.prova.id == id && r.prova.state == "todo")
        assertEquals(ProvaLinks.Read.Refused("prova 999999: non esiste"), read("999999").let { ProvaLinks.read(it.ok, it.text) })
    }

    // Gli esempi del relay (contratto 1.134, …1330-1334): da fare, ok (compito chiuso), ko con le parole, rossa col motivo, e uno che non c'è.
    @Test fun theRelaySamplesReadAsProve() {
        val fake = FakeTransport(load = { File("../contract/$it.json").readText() })
        val reads = listOf("41", "35", "38", "36", "999").map { a -> fake.sampleResult("prove", a).let { (ok, text) -> ProvaLinks.read(ok, text) } }
        assertEquals(listOf("todo", "ok", "ko", "red"), reads.take(4).map { r -> (r as ProvaLinks.Read.Found).prova.state })
        assertTrue("le parole del ko e il motivo del rosso", reads.drop(2).take(2).all { r -> !(r as ProvaLinks.Read.Found).prova.text.isNullOrBlank() })
        assertTrue(reads.last() is ProvaLinks.Read.Refused)
        assertEquals("l'elenco senza arg, come da un relay vecchio", ProvaLinks.Read.Unsupported, ProvaLinks.read(true, fake.sampleText("prove")))
    }
}
