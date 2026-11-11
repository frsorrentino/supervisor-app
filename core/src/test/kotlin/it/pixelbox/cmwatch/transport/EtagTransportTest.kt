package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.ContractJson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** Etag e «unchanged» (app-letture-leggere-11-11, contratto 1.135), con un relay nuovo e con uno di prima. */
class EtagTransportTest {
    private var n = 0
    private fun read(op: CmdOp = CmdOp.DECISIONI, arg: String? = null) = Cmd(id = "c${++n}", op = op, session = null, arg = arg, issued = n.toLong(), by = "test")
    private class Inner(val base: Transport, val reply: (Cmd) -> CmdResult) : Transport by base {
        val sent = mutableListOf<Cmd>()
        override suspend fun send(cmd: Cmd, onWritten: () -> Unit): CmdResult { sent += cmd; return reply(cmd) }
    }
    private fun inner(reply: (Cmd) -> CmdResult) = Inner(FakeTransport(load = { File("../contract/$it.json").readText() }), reply)

    @Test fun theSecondSameReadCarriesTheEtagAndUnchangedGivesBackWhatWeHad() = runBlocking {
        val i = inner { c -> if (c.etag != null) CmdResult(c.id, true, "", 2, etag = "aaaa", unchanged = true) else CmdResult(c.id, true, "{\"groups\":[]}", 1, etag = "aaaa") }
        val t = EtagTransport(i)
        val a = t.send(read())
        val c2 = read()
        val b = t.send(c2)
        assertNull(i.sent[0].etag)
        assertEquals("aaaa", i.sent[1].etag)
        assertEquals(c2.id, b.id)
        assertEquals(a.text, b.text)
    }

    @Test fun anOldRelayIgnoresTheEtagAndEveryAnswerIsWhole() = runBlocking {
        var k = 0
        val i = inner { c -> CmdResult(c.id, true, "pagina ${++k}", k.toLong()) }
        val t = EtagTransport(i)
        assertEquals("pagina 1", t.send(read()).text)
        assertEquals("pagina 2", t.send(read()).text)
        assertEquals(listOf(null, null), i.sent.map { it.etag })
    }

    @Test fun theEtagBelongsToOneReadAndAnErrorDropsIt() = runBlocking {
        var fail = false
        val i = inner { c -> if (fail) CmdResult(c.id, false, "no", 1) else CmdResult(c.id, true, "x", 1, etag = "e-${c.arg}") }
        val t = EtagTransport(i)
        t.send(read(CmdOp.PROVE, "518")); t.send(read(CmdOp.PROVE, "527")); t.send(read(CmdOp.PROVE, "518"))
        fail = true; t.send(read(CmdOp.PROVE, "518")); fail = false; t.send(read(CmdOp.PROVE, "518"))
        assertEquals(listOf(null, null, "e-518", "e-518", null), i.sent.map { it.etag })
    }

    @Test fun aCommandWithoutEtagIsWrittenAsBefore() {
        assertFalse("etag" in ContractJson.encode(read()))
        assertEquals("\"etag\":\"ab\"", Regex("\"etag\":\"ab\"").find(ContractJson.encode(read().copy(etag = "ab")))?.value)
    }
}
