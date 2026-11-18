package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.transport.DemoText
import it.pixelbox.cmwatch.transport.FakeTransport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * app-favicon-siti-18-11 (contratto 1.153): la favicon del sito come avatar della sessione. Lo stato porta l'impronta, l'op
 * `favicon` il PNG; senza l'una o l'altra, o con dati che non tornano, resta l'icona di oggi.
 */
class FaviconsTest {
    private fun load(n: String) = File("../contract/$n.json").readText()
    private val st = ContractJson.decodeState(load("state-1-question"))

    @Test fun theRelayFixtureHasTheOpButNoSite() {
        assertTrue(st.ops!!.contains("favicon"))
        assertEquals(emptyList<String>(), Favicons.wanted(st))
    }

    @Test fun oneAskPerHashOnlyWithTheOp() {
        val a = st.sessions[0].copy(favicon = "8c0173c7600b"); val b = st.sessions[1].copy(favicon = "8c0173c7600b")
        val c = st.sessions[2].copy(favicon = "../../etc/pw")
        val with = st.copy(sessions = listOf(a, b, c))
        assertEquals(listOf("8c0173c7600b"), Favicons.wanted(with))
        assertEquals(emptyList<String>(), Favicons.wanted(with.copy(ops = with.ops!! - "favicon")))
    }

    private val demo = FakeTransport(load = { n -> DemoText.dress(load(n)) }, now = { st.ts })
    private suspend fun ask(hash: String) = demo.send(Cmd("f-$hash", CmdOp.FAVICON, arg = hash, issued = st.ts, by = "test")) {}

    @Test fun theDemoHasTwoSitesAndOneWithout() = runBlocking {
        val s = demo.state.first()
        assertNotNull(s.sessions.single { x -> x.name == "storefront" }.favicon)
        assertNotNull(s.sessions.single { x -> x.name == "blog" }.favicon)
        assertNull(s.sessions.single { x -> x.name == "payments-api" }.favicon)
        assertEquals(2, Favicons.wanted(s).size)
    }

    @Test fun theOpGivesThePngOfThatHash() = runBlocking {
        val h = demo.state.first().sessions.single { x -> x.name == "storefront" }.favicon!!
        val r = ask(h)
        val png = Favicons.png(h, r.ok, r.text)
        assertNotNull(png)
        assertTrue(png!!.size > 100)
        // Un'impronta diversa da quella chiesta, una risposta d'errore o un'impronta che il relay non ha: niente immagine.
        assertNull(Favicons.png("000000000000", r.ok, r.text))
        assertNull(Favicons.png(h, false, r.text))
        val unknown = ask("0123456789ab")
        assertTrue(!unknown.ok && unknown.text.contains("not found"))
    }

    @Test fun brokenDataKeepsTodaysIcon() {
        val h = "8c0173c7600b"
        assertNull(Favicons.png(h, true, """{"hash": "$h", "mime": "image/png", "data": "not base64!"}"""))
        assertNull(Favicons.png(h, true, """{"hash": "$h", "mime": "image/png", "data": "R0lGODlhAQABAAAAACw="}"""))
        assertNull(Favicons.png(h, true, """{"hash": "$h", "mime": "image/jpeg", "data": ""}"""))
        assertNull(Favicons.png(h, true, "favicon $h not found"))
    }
}
