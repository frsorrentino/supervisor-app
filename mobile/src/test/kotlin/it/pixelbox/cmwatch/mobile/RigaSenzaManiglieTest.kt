package it.pixelbox.cmwatch.mobile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * app-riga-senza-maniglie-15-11 (Franz 15/11 15:19: «dovremmo togliere anche maniglie e frecce quando la master è chiusa»): la riga
 * in basso non ha più le due maniglie con la ⌃, né le loro prese e stringhe. Restano il tiro in su della riga e la presa in testa
 * al pannello aperto. L'immagine è `TabletShellTest.riga_senza_maniglie`.
 */
class RigaSenzaManiglieTest {
    private fun src(p: String) = listOf(File(p), File("mobile/$p")).first { it.exists() }.readText()
    private val frame = src("src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/DeskColumns.kt")
    private val sheet = src("src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/MasterSheetUi.kt")

    @Test fun theClosedRowHasNoHandles() {
        assertFalse(frame.contains("master-maniglia-riga"))
        assertFalse(frame.contains("\"bar-left\"")); assertFalse(frame.contains("\"bar-right\""))
        for (f in listOf("values", "values-en")) assertFalse(src("src/main/res/$f/strings.xml").contains("master_bar_grip"))
    }

    @Test fun whatStaysIsStillThere() {
        // Il tiro in su della riga e una presa sola in testa (scelta del 13/11). Dal 28/11 (app-posizioni-sistema-27-11, gruppo 6) la riga
        // della master è il foglio anche sul tablet: la presa è la sua maniglia, e maniglia e testata si trascinano. Dal 28/11 sera
        // (app-maniglia-28-11) la maniglia è il chevron in fondo alla riga del titolo, col suo segno «foglio-maniglia»; la riga si trascina.
        assertTrue(sheet.contains("testTag(\"foglio-maniglia\")"))
        assertTrue(sheet.contains("detectVerticalDragGestures("))
        assertFalse(frame.contains("master-maniglia-lato"))
    }
}
