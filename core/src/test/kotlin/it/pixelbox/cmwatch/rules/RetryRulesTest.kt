package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.data.ChatLog
import org.junit.Assert.assertEquals
import org.junit.Test

/** Riprova su un invio non riuscito (Franz 31/10 16:05): lo stato si vede sulla stessa voce. */
class RetryRulesTest {
    private val failed = Sent("m1", "master", "File: video.mp4", 100, failed = "PUT share: HTTP 500")

    @Test fun aRetryUploadingShowsOnTheSameMessageEvenIfItHadFailed() {
        assertEquals(ChatRules.Status.UPLOADING, ChatRules.status(failed, null, null, null, ChatRules.Upload.Going))
    }

    private fun log() = ChatLog(java.io.File.createTempFile("chat", ".json").apply { delete() }) { 200 }

    @Test fun aClearedFailureStaysInPlaceWithItsTime() {
        val l = log()
        l.add(failed); l.add(Sent("m2", "master", "dopo", 150))
        l.clearFailed("m1")
        assertEquals(listOf("m1", "m2"), l.messages.value.map { it.id })
        assertEquals(null, l.messages.value[0].failed)
        assertEquals(100L, l.messages.value[0].sentAt)
    }

    @Test fun aResentTextKeepsItsPlaceWithTheNewId() {
        val l = log()
        l.add(failed); l.add(Sent("m2", "master", "dopo", 150))
        l.resent("m1", "m9")
        assertEquals(listOf("m9", "m2"), l.messages.value.map { it.id })
        assertEquals(100L, l.messages.value[0].sentAt)
    }
}
