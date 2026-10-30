package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * La colonna dei minuti dello stile «Studio e Diretta» (terza versione, approvata da Franz il 30/10 alle 09:49; rail.ts nella
 * web): a sinistra di ogni riga il glifo dello stato e, sotto, da quanto aspetta o da quanto dura il turno.
 */
object Rail {
    enum class Unit { MIN, H, D }
    data class Span(val n: Long, val unit: Unit)
    enum class Glyph { WAITING, BUSY, DONE, RED, STILL }

    /** Da `from` a `now` in minuti interi, poi ore, poi giorni; mai negativo. */
    fun span(from: Long, now: Long): Span {
        val m = maxOf(0L, (now - from) / 60)
        if (m < 60) return Span(m, Unit.MIN)
        val h = m / 60
        return if (h < 24) Span(h, Unit.H) else Span(h / 24, Unit.D)
    }

    /** ❓ chi aspetta te, anche con un turno in corso; ▶ chi lavora; ✓ chi ha finito e non l'hai visto; tratteggio chi è fermo. */
    fun glyph(s: Session, group: Summary.Group? = null): Glyph = when {
        s.state == SessionState.WAITING || Kpi.waitingSince(s) != null -> Glyph.WAITING
        s.state == SessionState.BUSY || s.state == SessionState.AWAITING -> Glyph.BUSY
        group == Summary.Group.FINISHED -> Glyph.DONE
        else -> Glyph.STILL
    }
}
