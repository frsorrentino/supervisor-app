// app-ios-scatti-tastiera-12-11 (Franz 12/11 12:09, «ok per le correzioni»; due difetti segnalati dal collega che usa la web app
// su iPhone). Regole pure, applicate da main.ts.

/**
 * 1) `content-visibility: auto` fa contare 120 px (o 90) a una voce non ancora disegnata; quando entra vicino allo schermo prende
 * l'altezza vera. Chrome tiene fermo il punto di lettura con l'ancoraggio dello scorrimento; Safari ha content-visibility dalla 18
 * ma `overflow-anchor` solo dalla 27, e fino ad allora salendo la trascrizione salta di tutta la differenza. Senza ancoraggio si
 * rinuncia a content-visibility: `html.anchor` la accende in Feed.svelte e v3.css.
 */
export const anchorOk = (supports: ((rule: string) => boolean) | undefined): boolean => {
  try { return !!supports?.('overflow-anchor: auto') } catch { return false }
}

export type VisualViewportLike = { height: number; offsetTop: number; scale: number }

/**
 * 4) Il fondo di una lista che scorre (app-zoom-pan-laterale-16-11). scrollHeight e clientHeight sono interi: con un'altezza
 * frazionaria (la chat alta 180,44 px) `scrollTop = scrollHeight` la portava a 2995, mentre il compositore la ferma a 2994. Con lo
 * zoom delle dita il primo gesto laterale sulla chat si agganciava lì, per riportarla di quel pixel, e l'inquadratura non si
 * spostava. Il fondo vero, arrotondato in giù: al più un pixel sopra, che conta come fondo (la soglia della chat è 2).
 */
export const listEnd = (scrollHeight: number, height: number): number => Math.max(0, Math.floor(scrollHeight - height + 1e-3))
/** Oltre questa differenza fra la finestra e l'area visibile c'è la tastiera (la barra degli indirizzi di Safari sta sotto i 100 px). */
export const KEYBOARD_MIN = 150

/**
 * 2) Nella PWA su iPhone, con la tastiera aperta la finestra di layout non si accorcia: si accorcia l'area visibile, e Safari in più
 * fa scorrere la pagina per mostrare il campo (`offsetTop` > 0). #app seguiva solo l'altezza: corto e spostato in su, il campo
 * restava a metà schermo sopra una fascia nera e la testata finiva sotto la barra di stato. Ora #app sta fisso sull'area visibile:
 * altezza e scostamento dall'alto, e a tastiera aperta niente margine sicuro in basso (lo copre la tastiera).
 */
export function viewportVars(vv: VisualViewportLike | null | undefined, innerHeight: number): { vvh: number; vvt: number; kb: boolean; zoomed: boolean } {
  if (!vv) return { vvh: Math.round(innerHeight), vvt: 0, kb: false, zoomed: false }
  const vvh = Math.round(vv.height * vv.scale)
  // 3) La pagina ingrandita con le dita (app-zoom-pan-laterale-16-11): `html.zoomed`, e in app.css l'inquadratura va anche di lato.
  const zoomed = vv.scale > 1.01
  // Con lo zoom delle dita lo scostamento è quello dello zoom, non della tastiera: #app resta in cima.
  const vvt = zoomed ? 0 : Math.max(0, Math.round(vv.offsetTop))
  return { vvh, vvt, kb: innerHeight - vvh > KEYBOARD_MIN, zoomed }
}
