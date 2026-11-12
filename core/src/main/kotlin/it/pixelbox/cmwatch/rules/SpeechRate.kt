package it.pixelbox.cmwatch.rules

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * La velocità della voce del tasto ▶ (Franz, 02/10 15:49: «la lettura con la voce è un po' troppo rapida»), in multipli
 * della velocità del motore (1 = la sua). Dal 07/10 è continua (Franz, 21:15: «analogica», con uno slider sul comando;
 * approvata alle 22:06): da 0,5× a 2× a passi di 0,05, con uno scatto su 1×.
 */
object SpeechRate {
    const val NORMAL = 1.0f
    /**
     * Da dove parte chi non ha mai scelto una velocità (Franz, 12/11 16:34, con la schermata delle sue impostazioni: «voce di
     * lettura e velocità di lettura fossero di default le stesse che ho impostato io»): la sua, 1,25×.
     */
    const val DEFAULT = 1.25f
    const val MIN = 0.5f
    const val MAX = 2.0f
    private const val STEP = 0.05f

    /** Al passo di 0,05 più vicino, dentro 0,5-2; vicino a 1 si ferma su 1, così la velocità normale si ritrova al tatto. */
    fun snap(v: Float): Float {
        val c = v.coerceIn(MIN, MAX)
        if (abs(c - NORMAL) < 0.035f) return NORMAL
        return ((c / STEP).roundToInt() * 5) / 100f
    }

    /** Il valore salvato, portato sul passo; senza scelta o con un valore rovinato, quella di partenza (`DEFAULT`). */
    fun of(saved: Float?): Float = saved?.takeIf { it.isFinite() && it > 0f }?.let { snap(it) } ?: DEFAULT

    /**
     * Le velocità del tocco sulla pillola (Franz, 08/10 19:50): un tocco passa alla prossima, dopo 2× torna a 0,75×; tenuta
     * premuta apre lo slider per le altre.
     */
    val CYCLE = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    /** La velocità dopo `v` nel giro del tocco: la prima più alta, o la prima del giro dopo l'ultima. */
    fun next(v: Float): Float = CYCLE.firstOrNull { it > v + 0.001f } ?: CYCLE.first()

    /** Dove sta `v` sulla corsa dello slider, da 0 a 1: per il segno di 1×. */
    fun fraction(v: Float): Float = (v - MIN) / (MAX - MIN)
}
