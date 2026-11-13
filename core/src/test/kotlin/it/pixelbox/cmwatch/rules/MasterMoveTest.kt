package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MasterLanding.Height
import it.pixelbox.cmwatch.rules.MasterMove.Dir
import it.pixelbox.cmwatch.rules.MasterMove.Spot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Il tasto tondo della master (app-sposta-master-a-13-11): gli stessi casi di web/src/lib/masterKey.test.ts, dallo stesso file. */
class MasterMoveTest {
    private val cases = Json.parseToJsonElement(javaClass.getResource("/master-key-cases.json")!!.readText()).jsonObject
    private fun s(o: JsonObject, k: String) = o[k]!!.jsonPrimitive.content
    private fun spot(o: JsonObject) = Spot(s(o, "side") == "left", Height.valueOf(s(o, "height").uppercase()), o["homeRow"]!!.jsonPrimitive.boolean)
    private fun show(from: Spot, m: MasterMove.Move): String {
        val name = when (MasterMove.nameOf(from, m.to)) {
            MasterMove.Name.HOME_ROW -> "homeRow"
            else -> MasterMove.nameOf(from, m.to).name.lowercase()
        }
        return "${m.dir}>${if (m.to.left) "left" else "right"}/${m.to.height.name.lowercase()}/${if (m.to.homeRow) "H" else "-"}:$name"
    }

    @Test fun moves() = cases["moves"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val now = spot(o["now"]!!.jsonObject)
        assertEquals(s(o, "name"), o["out"]!!.jsonArray.map { it.jsonPrimitive.content }, MasterMove.moves(now, o["sessionsOpen"]!!.jsonPrimitive.boolean).map { show(now, it) })
    }

    @Test fun neverTwoArrowsTheSameWayNorThePlaceOfNow() {
        for (left in listOf(false, true)) for (h in Height.entries) for (row in listOf(false, true)) for (open in listOf(false, true)) {
            val now = Spot(left, h, row && h == Height.COLUMN)
            val ms = MasterMove.moves(now, open)
            assertEquals(ms.size, ms.map { it.dir }.toSet().size)
            assertFalse(ms.any { it.to == now })
        }
    }

    @Test fun dirAt() = cases["dirAt"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val want = o["out"]!!.takeIf { it !is JsonNull }?.jsonPrimitive?.content?.let(Dir::valueOf)
        val got = MasterMove.dirAt(o["dx"]!!.jsonPrimitive.float, o["dy"]!!.jsonPrimitive.float, o["dead"]!!.jsonPrimitive.float, o["dirs"]!!.jsonArray.map { Dir.valueOf(it.jsonPrimitive.content) })
        assertEquals(s(o, "name"), want, got)
    }

    @Test fun combo() = cases["combo"]!!.jsonArray.forEach { c ->
        val o = c.jsonObject
        val want = o["out"]!!.takeIf { it !is JsonNull }?.jsonPrimitive?.content?.let(Dir::valueOf)
        assertEquals(s(o, "name"), want, MasterMove.combo(o["held"]!!.jsonArray.map { Dir.valueOf(it.jsonPrimitive.content) }))
    }

    // Il posto ricordato sul tablet, andata e ritorno: ogni freccia porta dove dice, e la master è aperta o chiusa di conseguenza.
    @Test fun placeRoundTrip() {
        for (left in listOf(false, true)) for (h in Height.entries) for (row in listOf(false, true)) {
            val now = Spot(left, h, row && h == Height.COLUMN)
            val place = MasterMove.apply(MasterPlace(), now)
            assertEquals(now, MasterMove.spotOf(place, place.open, sessionsOpen = true))
            MasterMove.moves(now, true).forEach { m ->
                val next = MasterMove.apply(place, m.to)
                assertEquals(m.to, MasterMove.spotOf(next, next.open, sessionsOpen = true))
                assertEquals(m.to.height != Height.BAR, next.open)
            }
        }
    }
}
