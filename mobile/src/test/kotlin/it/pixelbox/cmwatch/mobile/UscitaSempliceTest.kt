package it.pixelbox.cmwatch.mobile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * app-prove-chiusura-anim-16-11 (Franz 16/11 09:58): in «Da provare» la riga approvata esce come le altre schede che si chiudono, in
 * altezza e sfumando (Fx.kt `leaveExit`), non più con l'uscita verso il contatore. Gli stessi controlli in leaveOut.test.ts.
 */
class UscitaSempliceTest {
    private fun src(p: String) = listOf(File(p), File("mobile/$p")).first { it.exists() }.readText()

    @Test fun theRowsOfDaProvareLeaveTheSimpleWay() {
        val rows = src("src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/ItemRowsUi.kt")
        assertTrue(rows.contains("exit = leaveExit(off)")); assertFalse(rows.contains("confirmedExit"))
        val fx = src("src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/Fx.kt").substringAfter("internal fun leaveExit").substringBefore("/**")
        assertTrue(fx.contains("shrinkVertically(tween(160))")); assertTrue(fx.contains("if (off) androidx.compose.animation.fadeOut(tween(120))"))
        assertFalse(fx.contains("scaleOut")); assertFalse(fx.contains("slideOut"))
    }
}
