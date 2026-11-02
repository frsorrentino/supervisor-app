package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La home per categorie (compito app-home-categorie-02-11); gli stessi casi in homeCats.test.ts. */
class HomeCatsTest {
    private fun ids(c: List<HomeCats.Cat>) = c.map { it.id }

    @Test fun theDefaultHasFourCategoriesTwoOpen() {
        val d = HomeCats.decode(null)
        assertEquals(listOf("you", "work", "out", "tools"), ids(d))
        assertEquals(listOf("prima_raccolta", "decisioni", "prove", "suggestions"), d[0].sections)
        assertEquals(listOf("sessions", "seguiti", "lavori"), d[1].sections)
        assertEquals(listOf("esiti", "da_guardare", "rendiconti"), d[2].sections)
        assertEquals(listOf("usage", "pagella", "system"), d[3].sections)
        assertEquals(listOf(true, true, false, false), d.map { it.open })
        assertEquals(HomeCats.DEFAULT, HomeCats.decode(emptyList()))
    }

    @Test fun theDefaultIsSavedAsNull() {
        assertNull(HomeCats.encode(HomeCats.DEFAULT, null))
    }

    @Test fun aSavedOrderComesBackTheSame() {
        val cats = HomeCats.toggle(HomeCats.moveCat(HomeCats.DEFAULT, 1, 0), "you")
        val saved = HomeCats.encode(cats, "usage")!!
        assertEquals("usage", HomeOrder.pinned(saved))
        // Lo slot di prima sale sul tabellone (app-tabellone-quattro-02-11): Utilizzo esce da Numeri e strumenti.
        assertEquals(listOf("usage"), HomeCats.top(saved))
        assertEquals(cats.map { c -> c.copy(sections = c.sections - "usage") }, HomeCats.decode(saved))
        assertEquals("shut:you,out,tools", saved.single { it.startsWith("shut:") })
    }

    @Test fun nothingClosedIsSavedToo() {
        val all = HomeCats.DEFAULT.map { it.copy(open = true) }
        assertEquals(all, HomeCats.decode(HomeCats.encode(all, null)))
    }

    @Test fun yesterdaysFlatOrderFallsIntoCategories() {
        val d = HomeCats.decode(listOf("sessions", "closed", "usage", "prove", "plugins", "pin:usage"))
        // Utilizzo, agganciato allo slot, sale sul tabellone; Numeri e strumenti torna al suo posto predefinito.
        assertEquals(listOf("work", "you", "out", "tools"), ids(d))
        assertEquals(listOf("prove", "prima_raccolta", "decisioni", "suggestions"), d.single { it.id == "you" }.sections)
        assertTrue(d.flatMap { it.sections }.none { it == "closed" || it == "plugins" })
        assertEquals(listOf(true, true, false, false), d.map { it.open })
    }

    @Test fun theSavedListFitsTheRelay() {
        // Contratto 1.97: al massimo 16 chiavi, ognuna al massimo 32 caratteri.
        val worst = HomeCats.DEFAULT.reversed().map { it.copy(open = false, sections = it.sections.reversed()) }
        val saved = HomeCats.encode(worst, "prima_raccolta")!!
        assertTrue(saved.size <= 16); assertTrue(saved.all { it.length <= 32 })
    }

    @Test fun aSectionNeverLeavesItsCategory() {
        assertEquals(HomeCats.DEFAULT, HomeCats.moveSection(HomeCats.DEFAULT, "you", 1, 9))
        assertEquals(listOf("prove", "prima_raccolta", "decisioni", "suggestions"), HomeCats.moveSection(HomeCats.DEFAULT, "you", 2, 0)[0].sections)
        assertEquals("out", HomeCats.catOf("rendiconti")); assertNull(HomeCats.catOf("closed"))
    }

    @Test fun aDraggedFlatOrderKeepsCategoriesAndClosedOnes() {
        val cats = HomeCats.toggle(HomeCats.DEFAULT, "work")
        val r = HomeCats.reorder(cats, listOf("prove", "sessions", "decisioni", "esiti"))
        assertEquals(listOf("you", "work", "out", "tools"), ids(r))
        // Le chiavi che la lista trascinata non ha (non disegnate) tengono il loro posto.
        assertEquals(listOf("prima_raccolta", "prove", "decisioni", "suggestions"), r[0].sections)
        assertEquals(listOf(true, false, false, false), r.map { it.open })
    }

    @Test fun keysNotDrawnKeepTheirPlaceAfterADrag() {
        // La web non disegna Pagella finché non esiste: un trascinamento di Sistema sopra Utilizzo non la sposta in fondo.
        assertEquals(listOf("system", "pagella", "usage"), HomeCats.reorder(HomeCats.DEFAULT, listOf("system", "usage")).single { it.id == "tools" }.sections)
    }

    @Test fun aDragNeverCrossesItsCategory() {
        val order = HomeCats.DEFAULT.flatMap { it.sections }
        // Suggerimenti è l'ultima di Richiede te: il dito oltre Sessioni (Attività) non la sposta.
        val mids = mapOf("prove" to 100f, "suggestions" to 200f, "sessions" to 300f)
        assertEquals(order, HomeCats.dragTarget(order, "suggestions", 350f, mids, mids.keys))
        // Dentro la categoria sì: sopra Da provare scavalca.
        assertEquals(listOf("prima_raccolta", "decisioni", "suggestions", "prove"), HomeCats.dragTarget(order, "suggestions", 90f, mids, mids.keys).take(4))
        // Chiuse sta in Attività: conta come sezione di Sessioni.
        assertEquals("work", HomeCats.catFor("closed")); assertEquals("tools", HomeCats.catFor("plugins"))
    }

    @Test fun aDraggedCategoryOrderKeepsSectionsAndSwitches() {
        // app-categorie-trascina-02-11: nella home la categoria presa si porta con le sue sezioni; aperte e chiuse restano.
        val cats = HomeCats.toggle(HomeCats.moveSection(HomeCats.DEFAULT, "you", 2, 0), "out")
        val r = HomeCats.catOrder(cats, listOf("cat:out", "cat:you", "cat:work", "cat:tools"))
        assertEquals(listOf("out", "you", "work", "tools"), ids(r))
        assertEquals(cats.single { it.id == "you" }, r[1]); assertEquals(true, r[0].open)
        // Una lista incompleta o sconosciuta non perde categorie.
        assertEquals(listOf("tools", "you", "work", "out"), ids(HomeCats.catOrder(HomeCats.DEFAULT, listOf("cat:tools", "boh"))))
        assertEquals(listOf("cat:you", "cat:work", "cat:out", "cat:tools"), HomeCats.catKeys(HomeCats.DEFAULT))
    }

    // Lo spazio in cima (app-home-in-cima-02-11); gli stessi casi in homeCats.test.ts.
    @Test fun topKeysReplaceTheirPlainKeyAndLeaveTheirCategory() {
        val cats = HomeCats.decode(null)
        val saved = HomeCats.encode(HomeCats.toTop(cats, emptyList(), "sessions").second, null, listOf("sessions"))!!
        assertEquals("top:sessions", saved[0]); assertTrue("sessions" !in saved)
        assertEquals(listOf("sessions"), HomeCats.top(saved))
        assertEquals(listOf("seguiti", "lavori"), HomeCats.decode(saved)[1].sections)
        assertTrue(saved.size <= 16 && saved.all { it.length <= 32 })
        // Tutte le sezioni in cima stanno ancora nelle 16 chiavi del relay.
        val all = HomeCats.DEFAULT.flatMap { it.sections }
        assertEquals(15, HomeCats.encode(HomeCats.DEFAULT.map { it.copy(sections = emptyList()) }, "usage", all)!!.size)
        // Un ordine di ieri non ha cima; chiavi sconosciute o doppie in cima si ignorano.
        assertEquals(emptyList<String>(), HomeCats.top(listOf("sessions", "shut:out")))
        assertEquals(listOf("esiti"), HomeCats.top(listOf("top:esiti", "top:boh", "top:esiti")))
        assertNull(HomeCats.encode(HomeCats.DEFAULT, null, emptyList()))
    }

    @Test fun theDraggedListHasTheEdgeAfterTheTop() {
        val f = HomeCats.flat(listOf("esiti"), HomeCats.decode(listOf("top:esiti")))
        assertEquals(listOf("esiti", HomeCats.EDGE, "prima_raccolta"), f.take(3))
        assertEquals(1 + 1 + 12, f.size)
    }

    @Test fun settleSplitsAtTheEdge() {
        val cats = HomeCats.decode(null)
        // Esiti, sola nella sua categoria, sale sopra il bordo.
        val up = HomeCats.flat(emptyList(), cats).let { f -> listOf("esiti", HomeCats.EDGE) + f.filter { it != "esiti" && it != HomeCats.EDGE } }
        val (top, c1) = HomeCats.settle(cats, up)
        assertEquals(listOf("esiti"), top)
        assertEquals(listOf("da_guardare", "rendiconti"), c1[2].sections)
        // E torna giù, nella sua categoria e non in quella dove il dito l'ha lasciata.
        val down = listOf(HomeCats.EDGE, "prima_raccolta", "esiti") + HomeCats.flat(top, c1).filter { it !in setOf("esiti", HomeCats.EDGE, "prima_raccolta") }
        val (top2, c2) = HomeCats.settle(c1, down)
        assertEquals(emptyList<String>(), top2)
        assertEquals(listOf("prima_raccolta", "decisioni", "prove", "suggestions"), c2[0].sections)
        assertTrue("esiti" in c2[2].sections)
        // Le chiavi non disegnate tengono il posto; chiuse e aperte non cambiano.
        assertEquals(cats.map { it.open }, c2.map { it.open })
        val partial = listOf("lavori", HomeCats.EDGE, "sessions", "seguiti")
        val (t3, c3) = HomeCats.settle(cats, partial)
        assertEquals(listOf("lavori"), t3); assertEquals(listOf("sessions", "seguiti"), c3[1].sections)
        assertEquals(cats[3], c3[3])
    }

    @Test fun aSectionCrossesOnlyTheTopTheEdgeAndItsCategory() {
        val order = listOf("esiti", HomeCats.EDGE, "decisioni", "prove", "sessions", "lavori")
        val present = order.toSet()
        val mids = mapOf("esiti" to 10f, HomeCats.EDGE to 30f, "decisioni" to 50f, "prove" to 70f, "sessions" to 90f, "lavori" to 110f)
        // Sessioni sale: Da provare e Da decidere sono di un'altra categoria e non contano, il bordo sì.
        assertEquals(order, HomeCats.dragTarget(order, "sessions", 60f, mids - "sessions", present))
        assertEquals(listOf("esiti", "sessions", HomeCats.EDGE, "decisioni", "prove", "lavori"), HomeCats.dragTarget(order, "sessions", 25f, mids - "sessions", present))
        // In cima scavalca le altre voci in cima, qualunque sia la loro categoria.
        val t = listOf("esiti", "sessions", HomeCats.EDGE, "decisioni")
        assertEquals(listOf("sessions", "esiti", HomeCats.EDGE, "decisioni"), HomeCats.dragTarget(t, "sessions", 5f, mapOf("esiti" to 10f, HomeCats.EDGE to 30f), t.toSet()))
    }

    @Test fun reorderPageMovesInAndOutOfTheTop() {
        val (top, cats) = HomeCats.toTop(HomeCats.DEFAULT, listOf("esiti"), "prove")
        assertEquals(listOf("esiti", "prove"), top)
        assertEquals(listOf("prima_raccolta", "decisioni", "suggestions"), cats[0].sections)
        val (top2, cats2) = HomeCats.fromTop(cats, top, "prove")
        assertEquals(listOf("esiti"), top2)
        assertEquals(listOf("prima_raccolta", "decisioni", "suggestions", "prove"), cats2[0].sections)
        // Già in cima, o fuori dalla cima: niente cambia.
        assertEquals(top to cats, HomeCats.toTop(cats, top, "prove"))
        assertEquals(top2 to cats2, HomeCats.fromTop(cats2, top2, "prove"))
    }

    // Il tabellone a quattro voci (app-tabellone-quattro-02-11); gli stessi casi in homeCats.test.ts.
    @Test fun theBoardTakesFourAndTheOldSlotMovesOnIt() {
        var t = emptyList<String>(); var c = HomeCats.DEFAULT
        listOf("esiti", "sessions", "usage", "prove", "decisioni").forEach { k -> HomeCats.toTop(c, t, k).let { (a, b) -> t = a; c = b } }
        assertEquals(listOf("esiti", "sessions", "usage", "prove"), t)
        assertTrue("decisioni" in c[0].sections)
        assertEquals(t, HomeCats.top(HomeCats.encode(c, null, t)))
        // Lo slot di prima diventa la prima voce del tabellone e non si salva più come `pin:`.
        assertEquals(listOf("usage"), HomeCats.top(listOf("sessions", "pin:usage")))
        assertEquals(listOf("esiti"), HomeCats.top(listOf("top:esiti", "pin:usage")))
        assertTrue(HomeCats.encode(HomeCats.decode(listOf("pin:usage")), null, listOf("usage"))!!.none { it.startsWith("pin:") })
        assertEquals(4, HomeCats.top(listOf("top:esiti", "top:sessions", "top:usage", "top:prove", "top:lavori")).size)
    }

    @Test fun aSectionTakenInItsCategoryDoesNotCrossTheEdge() {
        val order = listOf("esiti", HomeCats.EDGE, "sessions", "lavori")
        val mids = mapOf("esiti" to 10f, HomeCats.EDGE to 30f, "lavori" to 110f)
        assertEquals(order, HomeCats.dragTarget(order, "sessions", 5f, mids, order.toSet(), fromBoard = false))
        assertEquals(listOf("sessions", "esiti", HomeCats.EDGE, "lavori"), HomeCats.dragTarget(listOf("esiti", "sessions", HomeCats.EDGE, "lavori"), "sessions", 5f, mapOf("esiti" to 10f, HomeCats.EDGE to 30f), order.toSet(), fromBoard = true))
    }
}
