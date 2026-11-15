package it.pixelbox.cmwatch.rules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Contratto 1.148 (app-push-etag-15-11): la push di un evento dice quale /state c'è sul bus; chi ce l'ha già non lo rilegge. */
class PushStateTest {
    private val samples = Json.parseToJsonElement(File("../contract/fcm-event-sample.json").readText()).jsonObject.getValue("data").jsonArray
        .map { o -> o.jsonObject.mapValues { it.value.jsonPrimitive.content } }

    @Test fun theEtagIsTheFirst16HexOfTheSha1OfEnc() {
        assertEquals("a9993e364706816a", PushState.etagOf("abc"))
        assertEquals("a9993e364706816a", PushState.etagOfDoc(Json.parseToJsonElement("""{"v":1,"enc":"abc"}""")))
        assertNull(PushState.etagOfDoc(Json.parseToJsonElement("""{"v":1}""")))
        assertNull(PushState.etagOfDoc(Json.parseToJsonElement("null")))
    }

    @Test fun theSameEtagMeansTheStateIsAlreadyHere() {
        val (question, done, old) = samples
        assertTrue(PushState.known(question, "3f9a1c0b7d2e4a68", null))
        assertFalse(PushState.known(question, "b71c05e2a94d3f10", null))
        assertFalse(PushState.known(question, null, null))
        assertTrue(PushState.known(done, "b71c05e2a94d3f10", null))
        // Un relay più vecchio non manda i due campi: si legge come prima, qualunque cosa si abbia.
        assertFalse(PushState.known(old, "3f9a1c0b7d2e4a68", 1789999999.0))
    }

    @Test fun aPublishedAtNotNewerThanOursMeansTheStateIsAlreadyHere() {
        val question = samples[0]
        assertTrue(PushState.known(question, null, 1789805000.412))
        assertTrue(PushState.known(question, "altro", 1789805003.0))
        assertFalse(PushState.known(question, null, 1789805000.411))
        assertFalse(PushState.known(question - "etag" + ("published_at" to "x"), null, 1789805000.412))
    }
}
