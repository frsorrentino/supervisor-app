package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.Kpi.BoardFilter
import it.pixelbox.cmwatch.rules.Kpi.SessionsFold
import org.junit.Assert.assertEquals
import org.junit.Test

/** app-tabellone-filtro-25-11 (Franz 25/11 17:04): la casella scelta apre Sessioni; togliendo il filtro si torna come prima. */
class TabelloneFiltroTest {
    @Test fun scegliereUnaCasellaApreSessioni() {
        assertEquals(SessionsFold(open = true, byFilter = true), Kpi.boardFold(BoardFilter.WAITING, SessionsFold(open = false, byFilter = false)))
    }

    @Test fun togliereIlFiltroRichiudeQuelloCheAvevaAperto() {
        val aperta = Kpi.boardFold(BoardFilter.BUSY, SessionsFold(open = false, byFilter = false))
        assertEquals(SessionsFold(open = false, byFilter = false), Kpi.boardFold(null, aperta))
    }

    @Test fun apertaAManoRestaAperta() {
        val mano = SessionsFold(open = true, byFilter = false)
        assertEquals(mano, Kpi.boardFold(BoardFilter.WAITING, mano))
        assertEquals(mano, Kpi.boardFold(null, mano))
    }

    @Test fun cambiareCasellaNonRichiude() {
        val aperta = Kpi.boardFold(BoardFilter.WAITING, SessionsFold(open = false, byFilter = false))
        assertEquals(aperta, Kpi.boardFold(BoardFilter.BUSY, aperta))
    }
}
