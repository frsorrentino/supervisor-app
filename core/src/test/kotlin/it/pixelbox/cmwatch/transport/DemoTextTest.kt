package it.pixelbox.cmwatch.transport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Nella demo (e nelle schermate per Play) nessun valore che sembri un token vero: le fixture li hanno per i test del relay. */
class DemoTextTest {
    @Test fun noTokenValueInTheDressedFixtures() {
        val files = File("../contract").listFiles { f -> f.extension == "json" }!!
        assertTrue(files.any { f -> f.readText().contains("dpl_9f8e7d6c5b4a") })
        files.forEach { f ->
            val dressed = DemoText.dress(f.readText())
            assertFalse(f.name, Regex("""(dpl|pvw)_[0-9a-f]{6,}""").containsMatchIn(dressed))
        }
    }
}
