// Il bordo della master (app-bordo-master-27-11; Franz 27/11 22:21: un Border Beam come quello di Magic UI, «Reverse», che distingua la
// master dalle sessioni; dei tre posti proposti la master ha consigliato A, il campo di testo: dalla-master/bordo-master-2026-11-27/
// bordo-master.html, con tutti i valori nella nota). Sul campo della master un anello fermo di 1,5 px di #93A696, al 12 % sempre; mentre
// la master lavora (lo stesso «al lavoro» del suo glifo, rail.glyphOf) un raggio di 220 px corre lungo il bordo, un giro ogni 15 s, lineare,
// verso antiorario, e si accende e si spegne in 240 ms. Con «riduci movimento» niente giro: mentre lavora l'anello sale al 35 %. Le stesse
// regole in Kotlin (rules/BordoMaster.kt).
// app-bordo-foglio-28-11 (Franz 28/11, variante B di dalla-master/bordo-foglio-28-11.html): anello e raggio corrono attorno a tutto il foglio
// della master, alla riga, a metà e intero, invece che sul campo, che torna una pillola semplice come quello delle sessioni. Il bordo è più
// lungo: il raggio da 320 px. Il disegno sta in MasterSheet.svelte, solo CSS.

/** Il colore del bordo: 20 % del verde dell'app (#71D083) e 80 % del grigio dei testi (#9B9B9B). */
export const COLORE = '#93A696'
/** L'anello: 1,5 px, sempre acceso e fermo. */
export const SPESSORE_PX = 1.5
/** Il raggio degli angoli del campo, che è una pillola per la master e per le sessioni (il bordo del foglio segue i suoi angoli). */
export const ANGOLO_PX = 24
/** L'anello da fermo, e mentre lavora con «riduci movimento». */
export const ANELLO_FERMO = 0.12
export const ANELLO_RIDOTTO = 0.35
/** Il raggio: 320 px attorno al foglio (era 220 sul campo, più corto), il punto più acceso a un terzo da davanti. */
export const RAGGIO_PX = 320
/** Un giro ogni 15 s, lineare. */
export const GIRO_MS = 15_000
/** Il raggio si accende e si spegne in 240 ms con la curva cubic-bezier(.23, 1, .32, 1). */
export const SFUMA_MS = 240

/** L'opacità dell'anello: 12 % sempre; mentre la master lavora con «riduci movimento», al posto del giro, il 35 %. */
export const anello = (lavora: boolean, ridotto: boolean): number => (lavora && ridotto ? ANELLO_RIDOTTO : ANELLO_FERMO)

/** Il raggio gira solo mentre la master lavora, e mai con «riduci movimento». */
export const gira = (lavora: boolean, ridotto: boolean): boolean => lavora && !ridotto

/**
 * Dove sta il raggio al tempo `ms` (l'orologio, così web e telefono lo mostrano nello stesso punto del bordo): la frazione del bordo da 0
 * a 1, misurata in senso orario dall'estremo sinistro del lato di sopra, dove comincia `rect()` in CSS. Col tempo cala: il raggio gira
 * verso antiorario, un giro ogni GIRO_MS, lineare.
 */
export function posizione(ms: number): number {
  const f = (((ms % GIRO_MS) + GIRO_MS) % GIRO_MS) / GIRO_MS
  return f === 0 ? 0 : 1 - f
}
