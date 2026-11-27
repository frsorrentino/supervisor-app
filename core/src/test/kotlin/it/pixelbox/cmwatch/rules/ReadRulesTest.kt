package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** app-play-lettura-ovunque-27-11: il ▶ solo oltre una riga, come longText.test.ts sulla web. */
class ReadRulesTest {
    @Test fun onlyBeyondOneLine() {
        assertFalse("una riga sola", ReadRules.isLong(1, clipped = false))
        assertTrue("due righe", ReadRules.isLong(2, clipped = false))
        assertTrue("una riga tagliata, più lunga di quello che mostra", ReadRules.isLong(1, clipped = true))
    }
}
