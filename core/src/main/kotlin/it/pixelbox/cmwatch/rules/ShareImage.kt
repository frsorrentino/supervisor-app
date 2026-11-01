package it.pixelbox.cmwatch.rules

import kotlin.math.max
import kotlin.math.roundToInt

/** «Condividi» (contratto 1.19): l'immagine va ridotta a 1600 px sul lato lungo prima di cifrarla (circa 300-600 KB). */
object ShareImage {
    const val LONG_SIDE = 1600

    /**
     * Come raddrizzare una foto secondo l'orientamento EXIF (Franz 01/11 21:55, foto ruotata): prima lo specchio orizzontale, poi la rotazione
     * in senso orario. BitmapFactory lo ignora e la ricompressione in JPEG lo perde, quindi va applicato prima.
     */
    data class Turn(val degrees: Int, val mirror: Boolean)

    fun turn(exifOrientation: Int): Turn = when (exifOrientation) {
        2 -> Turn(0, true)
        3 -> Turn(180, false)
        4 -> Turn(180, true)
        5 -> Turn(270, true)
        6 -> Turn(90, false)
        7 -> Turn(90, true)
        8 -> Turn(270, false)
        else -> Turn(0, false)
    }

    /** Dove finisce il punto (x, y) di un'immagine w per h, dopo [turn]. */
    fun place(t: Turn, x: Int, y: Int, w: Int, h: Int): Pair<Int, Int> {
        val mx = if (t.mirror) w - x else x
        return when (t.degrees) {
            90 -> h - y to mx
            180 -> w - mx to h - y
            270 -> y to w - mx
            else -> mx to y
        }
    }

    fun scaled(w: Int, h: Int, longSide: Int = LONG_SIDE): Pair<Int, Int> {
        val big = max(w, h)
        if (big <= longSide) return w to h
        val f = longSide.toDouble() / big
        return (w * f).roundToInt() to (h * f).roundToInt()
    }

    /** Il campionamento della decodifica: la potenza di 2 più grande che lascia il lato lungo sopra `longSide`. */
    fun sampleSize(w: Int, h: Int, longSide: Int = LONG_SIDE): Int {
        var s = 1
        while (max(w, h) / (s * 2) >= longSide) s *= 2
        return s
    }
}
