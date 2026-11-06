package it.pixelbox.cmwatch.rules

/**
 * La scheda che diventa pagina o colonna, e il ritorno (app-volo-titolo-telefono-05-11, Franz 05/11 08:26): una regola sola per
 * telefono, tablet e web. Mai due testi uno sopra l'altro, e mai un riquadro vuoto (app-morph-senza-stacco-06-11, scelta A
 * della master; Franz 06/11 09:40: «da pwa lo stacco c'è sempre»): niente dissolvenze, dal primo fotogramma del volo quello che
 * entra è pieno e sta sopra, su fondo opaco, e quello che esce non si vede. Il riquadro cresce e il nuovo si scopre dall'angolo
 * in alto a sinistra, alla sua misura. I bordi restano sulla loro molla (`morph` e `fx-focus` in web/src/v3.css, `Fly.kt`).
 */
object Morph {

    /**
     * Il collo della scheda che diventa colonna, prima del volo (app-volo-web-taratura-05-11, Franz 05/11 10:23: «rimane in alto
     * troppo tempo, forse sale un po' troppo»): si contrae per 50 ms, sale di 10 dp e si spezza a 140 ms, poi il volo parte
     * subito. Gli stessi tempi della web (`PRESS_MS`, `NECK_MS`, `LIFT_PX` in web/src/lib/cardFx.ts).
     */
    const val NECK_PRESS_MS = 50
    const val NECK_MS = 140
    const val NECK_LIFT_DP = 10f

    /** L'opacità di quello che esce al tempo `ms` del volo: nascosto dal primo fotogramma. */
    @Suppress("UNUSED_PARAMETER") fun outAlpha(ms: Int): Float = 0f
    /** L'opacità di quello che entra al tempo `ms` del volo: pieno dal primo fotogramma. */
    @Suppress("UNUSED_PARAMETER") fun inAlpha(ms: Int): Float = 1f
}
