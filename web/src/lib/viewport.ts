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
/** Oltre questa differenza fra la finestra e l'area visibile c'è la tastiera (la barra degli indirizzi di Safari sta sotto i 100 px). */
export const KEYBOARD_MIN = 150

/**
 * 2) Nella PWA su iPhone, con la tastiera aperta la finestra di layout non si accorcia: si accorcia l'area visibile, e Safari in più
 * fa scorrere la pagina per mostrare il campo (`offsetTop` > 0). #app seguiva solo l'altezza: corto e spostato in su, il campo
 * restava a metà schermo sopra una fascia nera e la testata finiva sotto la barra di stato. Ora #app sta fisso sull'area visibile:
 * altezza e scostamento dall'alto, e a tastiera aperta niente margine sicuro in basso (lo copre la tastiera).
 */
export function viewportVars(vv: VisualViewportLike | null | undefined, innerHeight: number): { vvh: number; vvt: number; kb: boolean } {
  if (!vv) return { vvh: Math.round(innerHeight), vvt: 0, kb: false }
  const vvh = Math.round(vv.height * vv.scale)
  // Con lo zoom delle dita lo scostamento è quello dello zoom, non della tastiera: #app resta in cima.
  const vvt = vv.scale > 1.01 ? 0 : Math.max(0, Math.round(vv.offsetTop))
  return { vvh, vvt, kb: innerHeight - vvh > KEYBOARD_MIN }
}
