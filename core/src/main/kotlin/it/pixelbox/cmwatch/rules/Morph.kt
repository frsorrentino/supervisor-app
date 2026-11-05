package it.pixelbox.cmwatch.rules

/**
 * La scheda che diventa pagina o colonna, e il ritorno (app-volo-titolo-telefono-05-11, Franz 05/11 08:26): una regola sola per
 * telefono, tablet e web. Quello che esce sparisce presto, quello che entra arriva dopo: mai due testi pieni uno sopra l'altro.
 * I bordi restano sulla loro molla; qui solo le dissolvenze dei due contenuti (`morph-out`, `morph-in` in web/src/v3.css).
 */
object Morph {
    /** Il vecchio sparisce in 140 ms, svelto all'inizio. */
    const val OUT_MS = 140
    /** Il nuovo parte quando il vecchio è quasi trasparente e arriva pieno in 200 ms. */
    const val IN_DELAY_MS = 100
    const val IN_MS = 200
    /** cubic-bezier(0.33, 1, 0.68, 1), la stessa curva della web. */
    val OUT_EASE = floatArrayOf(0.33f, 1f, 0.68f, 1f)

    /** L'opacità del vecchio al tempo `ms` (la curva come 1 - (1 - x)³). */
    fun outAlpha(ms: Int): Float { val x = (ms.toFloat() / OUT_MS).coerceIn(0f, 1f); return (1f - x) * (1f - x) * (1f - x) }
    /** L'opacità del nuovo al tempo `ms`. */
    fun inAlpha(ms: Int): Float = ((ms - IN_DELAY_MS).toFloat() / IN_MS).coerceIn(0f, 1f)
}
