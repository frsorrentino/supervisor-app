package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MasterLanding.Height
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Spostare la master col suo tasto tondo (app-sposta-master-a-13-11; mockup app-mockup-sposta-master-12-11, giro E per il tasto e
 * giro F variante A per l'arrivo, scelta di Franz del 13/11 alle 12:07), gemella di web/src/lib/masterKey.ts. Col puntatore dentro
 * il tasto compare una freccia sola: quella della direzione possibile dalla parte del tasto dove sta il puntatore; al centro, e dove
 * non c'è una direzione possibile, resta il chevron che apre e chiude.
 */
object MasterMove {
    /** I gradi in senso orario da destra, come sullo schermo (giù = 90). */
    enum class Dir(val deg: Int) { E(0), SE(45), S(90), SW(135), W(180), NW(225), N(270), NE(315) }

    /** Dove sta la master: il lato, l'altezza (riga in basso, colonna, sotto la home a metà o compatta) e se la Home è in riga. */
    data class Spot(val left: Boolean, val height: Height, val homeRow: Boolean = false)
    data class Move(val dir: Dir, val to: Spot)

    /** Il nome di un posto d'arrivo: `HOME_ROW` e `HOME` sono la Home che va in riga e che torna su. */
    enum class Name { LEFT, RIGHT, COLUMN, HALF, COMPACT, BAR, HOME_ROW, HOME }

    /**
     * Da dove sta la master, le direzioni possibili e il posto d'arrivo di ognuna; `sessionsOpen` = ci sono sessioni aperte accanto.
     * «Dentro» è verso le sessioni, «fuori» verso il bordo dove sta la master. Le stesse regole e lo stesso ordine di `moves` in
     * masterKey.ts.
     */
    fun moves(now: Spot, sessionsOpen: Boolean): List<Move> {
        val inn = if (now.left) Dir.E else Dir.W
        val downIn = if (now.left) Dir.SE else Dir.SW
        val downOut = if (now.left) Dir.SW else Dir.SE
        fun at(h: Height, left: Boolean = now.left, homeRow: Boolean = false) = Spot(left, h, homeRow)
        return buildList {
            when (now.height) {
                Height.BAR -> {
                    add(Move(Dir.NW, at(Height.COLUMN, true))); add(Move(Dir.NE, at(Height.COLUMN, false)))
                    if (sessionsOpen) add(Move(Dir.N, at(Height.HALF)))
                }
                Height.COLUMN -> {
                    val row = now.homeRow && sessionsOpen
                    add(Move(inn, at(Height.COLUMN, !now.left, row)))
                    if (row) add(Move(Dir.N, at(Height.COLUMN)))
                    else if (sessionsOpen) { add(Move(Dir.S, at(Height.HALF))); add(Move(downOut, at(Height.COLUMN, now.left, true))) }
                    add(Move(downIn, at(Height.BAR)))
                }
                Height.HALF -> {
                    add(Move(Dir.N, at(Height.COLUMN))); add(Move(Dir.S, at(Height.COMPACT))); add(Move(inn, at(Height.HALF, !now.left))); add(Move(downIn, at(Height.BAR)))
                }
                Height.COMPACT -> {
                    add(Move(Dir.N, at(Height.HALF))); add(Move(Dir.S, at(Height.BAR))); add(Move(inn, at(Height.COMPACT, !now.left)))
                }
            }
        }
    }

    /** Il centro del tasto, dove resta il chevron: 8 col mouse, 30 col dito trascinato. */
    const val DEAD_MOUSE = 8f
    const val DEAD_FINGER = 30f

    /**
     * La direzione dalla posizione del puntatore rispetto al centro del tasto (giù positivo): dentro `dead` nessuna; fuori, la più
     * vicina fra quelle possibili, se sta entro 45° dal puntatore; altrimenti nessuna.
     */
    fun dirAt(dx: Float, dy: Float, dead: Float, dirs: List<Dir>): Dir? {
        if (hypot(dx, dy) < dead || dirs.isEmpty()) return null
        val a = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0)
        fun gap(d: Dir): Double { val x = abs(a - d.deg); return minOf(x, 360.0 - x) }
        val best = dirs.minByOrNull { gap(it) } ?: return null
        return best.takeIf { gap(it) <= 45.0 }
    }

    /** Dalla tastiera: una freccia, o due insieme per una diagonale; l'ultima premuta se non si combinano. Solo N, S, E, W. */
    fun combo(held: List<Dir>): Dir? {
        if (held.isEmpty()) return null
        if (held.size == 1) return held[0]
        val v = if (Dir.N in held) "N" else if (Dir.S in held) "S" else null
        val h = if (Dir.E in held) "E" else if (Dir.W in held) "W" else null
        return if (v != null && h != null) Dir.valueOf(v + h) else held.last()
    }

    fun nameOf(from: Spot, to: Spot): Name = when {
        to.homeRow && !from.homeRow -> Name.HOME_ROW
        from.homeRow && !to.homeRow && to.height == Height.COLUMN && to.left == from.left -> Name.HOME
        // Da un'altra colonna o dalla riga in basso una colonna si chiama col suo lato.
        to.height == Height.COLUMN && (from.height == Height.COLUMN || from.height == Height.BAR) -> if (to.left) Name.LEFT else Name.RIGHT
        else -> when (to.height) { Height.COLUMN -> Name.COLUMN; Height.HALF -> Name.HALF; Height.COMPACT -> Name.COMPACT; Height.BAR -> Name.BAR }
    }

    /** Il posto di adesso dal posto ricordato sul tablet e da aperta o chiusa. */
    fun spotOf(place: MasterPlace, open: Boolean, sessionsOpen: Boolean): Spot {
        val p = place.copy(open = open)
        return Spot(p.left, p.height(sessionsOpen), p.homeInRow(sessionsOpen))
    }

    /** Il posto ricordato dopo lo spostamento; `open` dice se la master resta aperta. */
    fun apply(place: MasterPlace, to: Spot): MasterPlace {
        val base = place.copy(left = to.left, homeRow = to.homeRow)
        return when (to.height) {
            Height.BAR -> base.copy(open = false)
            Height.COLUMN -> base.copy(open = true, under = false)
            else -> base.copy(open = true, under = true, stack = to.height)
        }
    }
}
