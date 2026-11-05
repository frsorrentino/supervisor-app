package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Feedback dal menù (app-feedback-menu-04-11): gli stessi casi di web/src/lib/feedback.test.ts.
class FeedbackTest {
    private val labels = Feedback.Labels("Suggerimento", "Bug", "Dati tecnici", "Foto della pagina allegata")
    private val tech = Feedback.Tech("0.2.1890", "abc1234", "Google Pixel 10 · Android 16 · 412×915 dp", "schermata: Lavori", "lingua: it")

    @Test fun theDraftIsAnonymous() = assertEquals(
        "scrivi a <email>, file in C:\\Users\\<utente>\\x e /home/<utente>/y, token <chiave> da <ip>",
        Feedback.anonymize("scrivi a fr.x@gmail.com, file in C:\\Users\\Utente\\x e /home/franz/y, token ghp_abcdefghijklmnopqrstuvwxyz0123 da 192.168.1.20"),
    )

    @Test fun theTitleIsTheFirstLineCutAtAWord() {
        assertEquals("La home sbiadisce", Feedback.titleOf("La home sbiadisce\naltro"))
        val t = Feedback.titleOf("parola ".repeat(20))
        assertTrue(t.length <= 72)
        assertTrue(t.endsWith("parola"))
    }

    @Test fun theDraftCarriesKindTextTechAndThePhotoOnlyWhenOn() {
        val d = Feedback.draft(Feedback.Kind.BUG, "Il tasto non risponde\nsu Lavori", tech, false, labels)
        assertEquals("[Bug] Il tasto non risponde", d.title)
        assertTrue("- app: 0.2.1890 (abc1234)" in d.body)
        assertTrue("schermata: Lavori" in d.body)
        assertFalse("Foto" in d.body)
        assertTrue("_Foto della pagina allegata_" in Feedback.draft(Feedback.Kind.IDEA, "x", tech, true, labels).body)
    }

    // Contratti 1.121 e 1.123, `feedback_app`: le voci 1210-1214 di cmd-result-sample.json.
    private val results = kotlinx.serialization.json.Json.parseToJsonElement(it.pixelbox.cmwatch.Fixtures.cmdResult).let { r -> (r as kotlinx.serialization.json.JsonObject).getValue("result") as kotlinx.serialization.json.JsonArray }
        .map { e -> it.pixelbox.cmwatch.contract.ContractJson.json.decodeFromJsonElement(it.pixelbox.cmwatch.contract.CmdResult.serializer(), e) }
    private fun res(n: String) = results.first { r -> r.id.endsWith(n) }

    @Test fun theDraftHasItsHash() {
        assertEquals("7c41e09a2b", Feedback.draftHash(res("00001210").text))
        assertEquals(null, Feedback.draftHash(res("00001214").text))
    }

    // 1.123: con la schermata la riga da mostrare prima dei tasti; github_ok assente vuol dire niente GitHub.
    @Test fun theDraftSaysTheScreenshotNoticeAndHidesGithubWithoutGithubOk() {
        val p = Feedback.preparedOf(res("00001210").text)!!
        assertEquals("7c41e09a2b", p.hash)
        assertTrue(p.notice!!.contains("maintainer"))
        assertEquals(false, p.githubOk)
        assertEquals(true, Feedback.preparedOf(res("00001210").text!!.replaceFirst("{", "{\"github_ok\": true, "))!!.githubOk)
        assertEquals(null, Feedback.preparedOf(res("00001214").text))
    }

    @Test fun theChoiceSaysWhereItWent() {
        assertTrue(Feedback.choiceOf(res("00001212").text)!!.url!!.startsWith("https://github.com/frsorrentino/supervisor-feedback/issues/new?"))
        assertTrue(Feedback.choiceOf(res("00001212").text)!!.screenshotUrl!!.startsWith("https://github.com/frsorrentino/supervisor-feedback/raw/"))
        assertEquals(null, Feedback.choiceOf(res("00001211").text)!!.screenshotUrl)
        assertTrue(Feedback.choiceOf(res("00001211").text)!!.text.contains("with the screenshot"))
        assertEquals("anonymous", Feedback.choiceOf(res("00001211").text)!!.choice)
        assertEquals(null, Feedback.choiceOf(res("00001213").text)!!.url)
    }

    @Test fun kindAndTechAsTheRelayWantsThem() {
        assertEquals("suggerimento", Feedback.relayKind(Feedback.Kind.IDEA))
        assertEquals("bug", Feedback.relayKind(Feedback.Kind.BUG))
        val t = Feedback.relayTech(tech.copy(device = "x".repeat(100)))
        assertTrue(t.keys.all { Regex("[a-z_]+").matches(it) })
        assertEquals(80, t.getValue("device").length)
    }
}
