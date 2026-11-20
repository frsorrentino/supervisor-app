package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** app-trova-messaggi-20-11: gli stessi casi di web/src/lib/mineJump.test.ts. */
class MineJumpTest {
    private fun e(id: String, role: String) = TranscriptEntry(id = id, role = role, text = id)
    private val rows = listOf(
        ChatTurns.Row.Turn("t1", ChatFeed.Item.User(e("u1", "user")), emptyList(), e("a1", "assistant")),
        // Un turno partito da solo (un avviso): non è un messaggio di Franz.
        ChatTurns.Row.Turn("t2", null, emptyList(), e("a2", "assistant"), auto = true),
        ChatTurns.Row.Turn("t3", ChatFeed.Item.User(e("u3", "user")), emptyList(), e("a3", "assistant")),
        // Mandato dal telefono e mai arrivato (il caso del 20/11): si ferma anche lì, sul mandato.
        ChatTurns.Row.Turn("t4", ChatFeed.Item.Mine(Sent("s4", "master", "Su Pixel si può installare Claude?", 0), ChatRules.Status.FAILED, null), emptyList(), null),
    )

    @Test fun onlyFranzsMessagesAreStops() =
        assertEquals(listOf(MineJump.Stop(0, "u1"), MineJump.Stop(2, "u3"), MineJump.Stop(3, "s4")), MineJump.stops(rows))

    @Test fun itStartsFromTheLastAndStopsAtTheEnds() {
        val s = MineJump.stops(rows)
        assertEquals("s4", MineJump.step(s, null, -1)!!.flash)
        assertEquals("u3", MineJump.step(s, "s4", -1)!!.flash)
        assertEquals("u1", MineJump.step(s, "u1", -1)!!.flash)
        assertEquals("s4", MineJump.step(s, "s4", +1)!!.flash)
        assertNull(MineJump.step(emptyList(), null, -1))
    }

    @Test fun theCounterCountsFromTheOldest() {
        val s = MineJump.stops(rows)
        assertEquals(2 to 3, MineJump.position(s, "u3"))
        assertNull(MineJump.position(s, "a1"))
    }
}
