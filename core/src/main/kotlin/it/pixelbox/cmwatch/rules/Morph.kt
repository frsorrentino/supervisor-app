package it.pixelbox.cmwatch.rules

/**
 * La scheda che diventa pagina o colonna, e il ritorno (app-volo-titolo-telefono-05-11, Franz 05/11 08:26): una regola sola per
 * telefono, tablet e web. Quello che esce sparisce presto, quello che entra arriva dopo: mai due testi pieni uno sopra l'altro.
 * I bordi restano sulla loro molla; qui solo le dissolvenze dei due contenuti (`morph-out`, `morph-in` in web/src/v3.css).
 */
object Morph {
    /** Il vecchio sparisce in 140 ms, svelto all'inizio. */
    const val OUT_MS = 140
    /**
     * Il nuovo parte quando il vecchio è quasi trasparente e arriva pieno in 180 ms (app-morph-senza-stacco-06-11: prima 100 e
     * 200). Sotto i due contenuti il riquadro resta pieno, col fondo della pagina (`fullUnder` in mobile/ui/Fly.kt,
     * `::view-transition-group` in web/src/v3.css): Franz 06/11 08:56 vedeva «un piccolo stacco di fade out e fade in».
     */
    const val IN_DELAY_MS = 80
    const val IN_MS = 180
    /** cubic-bezier(0.33, 1, 0.68, 1), la stessa curva della web. */
    val OUT_EASE = floatArrayOf(0.33f, 1f, 0.68f, 1f)

    /**
     * Il collo della scheda che diventa colonna, prima del volo (app-volo-web-taratura-05-11, Franz 05/11 10:23: «rimane in alto
     * troppo tempo, forse sale un po' troppo»): si contrae per 50 ms, sale di 10 dp e si spezza a 140 ms, poi il volo parte
     * subito. Gli stessi tempi della web (`PRESS_MS`, `NECK_MS`, `LIFT_PX` in web/src/lib/cardFx.ts).
     */
    const val NECK_PRESS_MS = 50
    const val NECK_MS = 140
    const val NECK_LIFT_DP = 10f

    /** L'opacità del vecchio al tempo `ms` (la curva come 1 - (1 - x)³). */
    fun outAlpha(ms: Int): Float { val x = (ms.toFloat() / OUT_MS).coerceIn(0f, 1f); return (1f - x) * (1f - x) * (1f - x) }
    /** L'opacità del nuovo al tempo `ms`. */
    fun inAlpha(ms: Int): Float = ((ms - IN_DELAY_MS).toFloat() / IN_MS).coerceIn(0f, 1f)
}
