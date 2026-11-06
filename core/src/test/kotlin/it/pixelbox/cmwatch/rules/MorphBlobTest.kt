package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MasterLanding.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Il blocco della master che cambia forma col dito (app-master-morph-m3-06-11); gli stessi casi di morphBlob.test.ts. */
class MorphBlobTest {
    private val bar = Rect(0f, 900f, 1500f, 60f)
    private val col = Rect(1080f, 0f, 420f, 960f)

    @Test fun progressGoesFromZeroAtTheStartToOneAtTheArrivalAndNoFurther() {
        assertEquals(0f, MorphBlob.progress(bar, col, 0f))
        assertEquals(0.5f, MorphBlob.progress(bar, col, -450f))
        assertEquals(1f, MorphBlob.progress(bar, col, -2000f))
        assertEquals(0f, MorphBlob.progress(bar, col, 300f))
    }

    @Test fun atTheStartTheBarAtTheEndTheColumnWithRoundCornersOnlyInBetween() {
        val a = MorphBlob.shape(bar, col, 0f, 750f, 930f)
        assertEquals(listOf(0f, 900f, 1500f, 60f), listOf(a.x, a.y, a.w, a.h))
        assertEquals(0f, a.rBottom)
        val z = MorphBlob.shape(bar, col, 1f, 1290f, 0f)
        assertEquals(listOf(1080, 0, 420, 960), listOf(Math.round(z.x), Math.round(z.y), Math.round(z.w), Math.round(z.h)))
        assertTrue(MorphBlob.shape(bar, col, 0.5f, 900f, 500f).rBottom > 0f)
    }

    @Test fun theDropTheEdgeTowardTheFingerStretchesUpToAMaximumAndRounds() {
        val near = MorphBlob.shape(bar, col, 0.1f, 750f, 700f)
        val far = MorphBlob.shape(bar, col, 0.1f, 750f, 100f)
        val plain = MorphBlob.shape(bar, col, 0.1f, 750f, 900f)
        assertTrue(near.y < plain.y)
        assertEquals(MorphBlob.STRETCH_MAX, plain.y - far.y, 0.001f)
        assertTrue(far.rTop > plain.rTop)
    }

    @Test fun contentFadesEarlyAndTheGripComesHalfway() {
        assertEquals(1f, MorphBlob.contentAlpha(0f)); assertEquals(0f, MorphBlob.contentAlpha(0.7f))
        assertEquals(0f, MorphBlob.gripAlpha(0.2f)); assertEquals(1f, MorphBlob.gripAlpha(1f))
        assertEquals(MorphBlob.still(col), MorphBlob.mix(MorphBlob.shape(bar, col, 0.3f, 900f, 500f), MorphBlob.still(col), 1f))
    }
}
