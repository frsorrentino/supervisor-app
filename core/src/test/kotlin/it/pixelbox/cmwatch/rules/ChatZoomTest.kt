package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Lo zoom del testo della conversazione (Franz, 03/10 21:16). */
class ChatZoomTest {
    @Test fun staysWithinTheBounds() {
        assertEquals(ChatZoom.MAX, ChatZoom.clamp(3f))
        assertEquals(ChatZoom.MIN, ChatZoom.clamp(0.2f))
        assertEquals(1.3f, ChatZoom.clamp(1.3f))
    }

    @Test fun aMissingOrBrokenValueIsTheNormalSize() {
        assertEquals(1f, ChatZoom.of(null))
        assertEquals(1f, ChatZoom.of(Float.NaN))
        assertEquals(1f, ChatZoom.of(-2f))
        assertEquals(1f, ChatZoom.clamp(Float.POSITIVE_INFINITY))
        assertEquals(ChatZoom.MAX, ChatZoom.of(9f))
    }

    /** app-zoom-testo-02-11: il pizzico su un'immagine non tocca lo zoom del testo, quello sul testo sì. */
    @Test fun aPinchThatStartsOnAnImageIsNotTheText() {
        val img = listOf(ChatZoom.Area(0f, 400f, 1000f, 900f))
        assertEquals(true, ChatZoom.textOwns(listOf(100f to 200f, 300f to 300f), img))
        assertEquals(false, ChatZoom.textOwns(listOf(100f to 500f, 300f to 800f), img))
        // Un dito sull'immagine e uno sul testo: del testo no, per non ridimensionare la chat sotto l'immagine.
        assertEquals(false, ChatZoom.textOwns(listOf(100f to 200f, 300f to 450f), img))
        // Senza immagini il gesto è del testo, come sempre.
        assertEquals(true, ChatZoom.textOwns(listOf(100f to 500f, 300f to 800f), emptyList()))
    }
}
