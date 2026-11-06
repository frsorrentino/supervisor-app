import type { Attachment } from 'svelte/attachments'

// Cruscotto e plancia montati una volta (app-fluidita-montaggio-06-11): quello che non si vede resta montato fuori dal documento
// invece di essere smontato e rifatto a ogni apertura e chiusura (circa 400 ms a CPU 4x). Fuori dal documento non ha stile né
// impaginazione e non si vede nelle ricerche nella pagina (nomi della transizione, data-vt, colonne del regista): per il resto
// della pagina c'è un ramo solo, come prima. Svelte continua ad aggiornarlo, e rientra già fatto.
//
// L'elemento spostato non deve mai essere il primo o l'ultimo nodo di un blocco di Svelte (che rimuove i blocchi da un nodo
// all'altro): sta da solo dentro un elemento fisso, il suo posto.

/** Il posto fuori dal documento. */
export const offstage = (typeof document === 'undefined' ? null : document.createElement('div')) as HTMLElement

// Le posizioni di scorrimento: un elemento tolto dal documento le perde, e rientrando partirebbe dall'inizio. Si annotano allo
// scorrimento (a impaginazione pulita, senza costi) e si rimettono al rientro.
const scrolls = new Map<Element, [number, number]>()
if (typeof document !== 'undefined') document.addEventListener('scroll', (e) => {
  const el = e.target
  if (!(el instanceof Element)) return
  if (el.scrollTop || el.scrollLeft) scrolls.set(el, [el.scrollTop, el.scrollLeft]); else scrolls.delete(el)
}, { capture: true, passive: true })

/** Rimette le posizioni di scorrimento dentro `root`, appena rientrato nel documento; dimentica quelle degli elementi smontati. */
export function restoreScroll(root: Element) {
  for (const [el, [top, left]] of scrolls) {
    if (root.contains(el)) { el.scrollTop = top; el.scrollLeft = left }
    else if (!el.isConnected && !offstage.contains(el)) scrolls.delete(el)
  }
}

// Le misure: fuori dal documento ogni elemento osservato misura 0, e i componenti (bind:clientWidth, i grafici, le colonne) si
// ridisegnavano a misura nulla; al rientro il primo fotogramma, quello che la transizione fotografa, aveva le colonne stirate e le
// testate strette. Gli osservatori ignorano gli elementi staccati: i componenti tengono l'ultima misura vera, che al rientro è
// quasi sempre ancora quella. Va installato prima che Svelte crei il suo osservatore, al primo montaggio.
if (typeof window !== 'undefined' && window.ResizeObserver) {
  const Base = window.ResizeObserver
  window.ResizeObserver = class extends Base {
    constructor(cb: ResizeObserverCallback) {
      super((entries, ro) => { const on = entries.filter(e => e.target.isConnected); if (on.length) cb(on, ro) })
    }
  }
}

const slots = new WeakMap<Element, Element>()
const runs = new WeakMap<Element, number>()

/** Sposta `el` fuori dal documento finché `shown` è falso, e lo rimette al suo posto quando torna vero. */
export const park = (shown: boolean): Attachment<HTMLElement> => (el) => {
  const home = slots.get(el) ?? el.parentElement
  if (!home || !offstage) return
  slots.set(el, home)
  const run = (runs.get(el) ?? 0) + 1
  runs.set(el, run)
  if (shown) moveInto(el, home)
  else if (el.parentElement !== offstage) offstage.append(el)
  // Smontato da Svelte mentre era fuori: il blocco toglie il suo posto, non l'elemento, che va tolto qui. Un nuovo giro
  // dell'attacco (cambio di `shown`) arriva subito, nello stesso aggiornamento.
  return () => queueMicrotask(() => { if (runs.get(el) === run && el.parentElement === offstage) el.remove() })
}

/** Sposta `el` dentro `to`, con le sue posizioni di scorrimento. */
export function moveInto(el: Element, to: Element) {
  if (el.parentElement === to) return
  to.append(el)
  if (el.isConnected) restoreScroll(el)
}
