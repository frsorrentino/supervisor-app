package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** I link delle sessioni nella chat diventano il tasto «Apri in supervisor» (Franz, 21/10 19:04). */
class SessionLinksTest {
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val sessions = st.sessions
    private val atlas = "https://claude.ai/code/session_018CKZ1Pum1Qs7DX5hbRLQ6X"
    private val ledger = "https://claude.ai/code/session_01CnGG8im7UG4KtjbPDx9fst"

    @Test fun matchesTheSessionLink() {
        assertEquals("atlas-shop", SessionLinks.sessionOf(atlas, sessions)?.name)
        assertEquals("atlas-shop", SessionLinks.sessionOf("$atlas/", sessions)?.name)
        assertEquals("atlas-shop", SessionLinks.sessionOf("$atlas?x=1#top", sessions)?.name)
        assertNull(SessionLinks.sessionOf("https://claude.ai/code/session_unknown", sessions))
        // orbit-docs è chiusa e senza link: un link vuoto non è mai una sessione.
        assertNull(SessionLinks.sessionOf("", sessions))
    }

    @Test fun theMasterMessageOfTheReport() {
        val text = "• **Prossimo passo:** rivedere il flusso dei rimborsi.\n• **Link:**\nhttps://claude.ai/code/session_018CKZ1Pum1Qs7DX5hbRLQ6X\n\nEsito: sessione atlas-shop aperta."
        val r = SessionLinks.strip(text, sessions)
        assertEquals("• **Prossimo passo:** rivedere il flusso dei rimborsi.\n\nEsito: sessione atlas-shop aperta.", r.text)
        assertEquals(listOf(SessionLinks.Button("atlas-shop", atlas)), r.buttons)
    }

    @Test fun labelOnTheSameLineGoesAway() {
        assertEquals("Sessione avviata.", SessionLinks.strip("Sessione avviata.\n• Link: $atlas", sessions).text)
        assertEquals("Sessione avviata.", SessionLinks.strip("Sessione avviata.\nLink: $atlas.", sessions).text)
    }

    @Test fun aSentenceKeepsItsWordsWithoutTheColon() {
        assertEquals("La sessione atlas-shop è aperta e la trovi qui", SessionLinks.strip("La sessione atlas-shop è aperta e la trovi qui: $atlas", sessions).text)
        assertEquals("**La sessione atlas-shop è aperta e la trovi qui**", SessionLinks.strip("**La sessione atlas-shop è aperta e la trovi qui: $atlas**", sessions).text)
    }

    @Test fun recapLineKeepsTheRestAndDropsEmptyParentheses() {
        val r = SessionLinks.strip("🟧 ledger-api ($ledger) · AskUserQuestion\n🟢 atlas-shop ($atlas): fatto.", sessions)
        assertEquals("🟧 ledger-api · AskUserQuestion\n🟢 atlas-shop: fatto.", r.text)
        assertEquals(listOf("ledger-api", "atlas-shop"), r.buttons.map { it.session })
    }

    @Test fun markdownLinkKeepsItsText() {
        val r = SessionLinks.strip("Apri [atlas-shop]($atlas) e [la doc](https://example.com/x).", sessions)
        assertEquals("Apri atlas-shop e [la doc](https://example.com/x).", r.text)
        assertEquals(listOf(SessionLinks.Button("atlas-shop", atlas)), r.buttons)
    }

    @Test fun oneButtonPerSessionInOrderOfAppearance() {
        val r = SessionLinks.strip("$ledger poi $atlas e ancora $ledger", sessions)
        assertEquals(listOf("ledger-api", "atlas-shop"), r.buttons.map { it.session })
        assertEquals("poi e ancora", r.text)
        assertEquals("- a\n  - b qui", SessionLinks.strip("- a\n  - b qui $atlas", sessions).text)
    }

    @Test fun unknownOrOtherLinksStay() {
        val text = "Mockup: https://claude.ai/artifact/X e https://claude.ai/code/session_unknown"
        val r = SessionLinks.strip(text, sessions)
        assertEquals(text, r.text)
        assertEquals(emptyList<SessionLinks.Button>(), r.buttons)
        assertEquals("niente", SessionLinks.strip("niente", sessions).text)
        assertEquals(atlas, SessionLinks.strip(atlas, emptyList()).text)
    }

    @Test fun deepLinkOpensASession() {
        assertEquals("officina", SessionLinks.deepLink("cmwatch://session/officina"))
        assertEquals("master", SessionLinks.deepLink("cmwatch://session/master/"))
        assertEquals("field notes", SessionLinks.deepLink("cmwatch://session/field%20notes"))
        assertNull(SessionLinks.deepLink("cmwatch://session/"))
        assertNull(SessionLinks.deepLink("cmwatch://session/a/b"))
        assertNull(SessionLinks.deepLink("cmwatch://pair?q=abc"))
        assertNull(SessionLinks.deepLink("https://claude.ai/code/session_01"))
        assertNull(SessionLinks.deepLink("::not a uri"))
    }

    @Test fun deepLinkRoundTrip() {
        for (n in listOf("officina", "master", "field notes", "ledger-api", "così")) assertEquals(n, SessionLinks.deepLink(SessionLinks.deepLinkTo(n)))
        assertEquals("cmwatch://session/field%20notes", SessionLinks.deepLinkTo("field notes"))
    }
}
