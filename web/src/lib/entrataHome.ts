// app-entrata-home-27-11 (Franz 27/11 20:46, «l'effetto di entrata elastica su tutti gli elementi della home quando si avvia l'app»;
// variante B del mockup home-entrata.html della master): alla prima apertura della pagina i blocchi della home entrano dall'alto uno
// dopo l'altro, 70 ms + 45 ms per posizione. Ognuno sfuma da 0 e da sfocato (5 px) in 0,4 s ease, sale di 24 px e scala da 0,94 con la
// molla 220/15 con cui il Rubber dock compare (massa 1, smorzamento 0,506); la barra della settimana cresce in 0,8 s con
// cubic-bezier(.34, 1.56, .64, 1), 220 ms dopo il suo riquadro. Tornando alla home da una sessione non si ripete; con «riduci movimento»
// la home compare ferma. Porta di EntrataHome.kt (i tempi e la volta sola); la molla è quella del motore di molla.ts, campionata in
// fotogrammi chiave per WAAPI, che la fa correre fuori dal filo principale mentre la pagina si avvia.
import { type Spring } from './iconRow'
import { avanza, ferma, molla, ridotto } from './molla'

/** Il primo blocco parte dopo 70 ms, ognuno dei seguenti 45 ms dopo quello sopra, come le righe del Rubber dock. */
export const DELAY_MS = 70
export const STAGGER_MS = 45
/** Opacità da 0 e sfocatura da 5 px in 0,4 s ease. */
export const FADE_MS = 400
export const BLUR_PX = 5
/** Sale di 24 px e scala da 0,94 con la molla con cui il dock compare (dock.js, intro). */
export const RISE_PX = 24
export const SCALE_FROM = 0.94
/** La molla 220/15, massa 1: smorzamento relativo 15 / (2·√220) ≈ 0,506. */
export const SPRING: Spring = { k: 220, c: 15 }
/** La barra della settimana cresce in 0,8 s col rimbalzo delle barre del dock, 220 ms dopo il suo riquadro. */
export const BAR_MS = 800
export const BAR_AFTER_MS = 220
export const BAR_EASE = 'cubic-bezier(.34, 1.56, .64, 1)'
/** Senza il primo stato entro 2,5 s la home compare ferma, com'è. */
export const WAIT_MAX_MS = 2500
/** Il nome delle corse dell'entrata, per riconoscerle fra le animazioni del documento. */
export const ID = 'entrata-home'

/** Quando parte il blocco in posizione `index`, contata dall'alto da 0. */
export const delayMs = (index: number): number => DELAY_MS + STAGGER_MS * Math.max(0, index)
/** Quando parte la barra della settimana, il cui riquadro è in posizione `boxIndex`. */
export const barDelayMs = (boxIndex: number): number => delayMs(boxIndex) + BAR_AFTER_MS
/** Entra solo alla prima apertura e mai con «riduci movimento»: allora la home compare ferma. */
export const plays = (first: boolean, reduced: boolean): boolean => first && !reduced

/** La volta sola: `take` è vero alla prima chiamata e poi sempre falso, anche se quella volta la home è comparsa ferma. */
export class Once {
  taken = false
  take(): boolean {
    if (this.taken) return false
    this.taken = true
    return true
  }
}

/** La molla dal motore di molla.ts: lo scostamento da 1 al posto (0), un campione ogni 1/60 s finché si ferma. */
export function curve(s: Spring = SPRING, dt = 1 / 60): number[] {
  const m = molla(1, s.k, s.c)
  m.t = 0
  const xs = [1]
  for (let i = 0; i < 600 && !ferma(m); i++) { avanza([m], dt); xs.push(m.x) }
  xs[xs.length - 1] = 0
  return xs
}

/** I fotogrammi chiave della salita lungo la curva: a 1 giù di 24 px e a 0,94, a 0 al posto e a 1. */
export const keyframes = (xs: number[]): Keyframe[] =>
  xs.map((x, j) => ({ offset: j / Math.max(1, xs.length - 1), translate: `0 ${(RISE_PX * x).toFixed(2)}px`, scale: (1 - (1 - SCALE_FROM) * x).toFixed(4) }))

/** La volta sola della pagina: un modulo per caricamento. */
const once = new Once()

/**
 * I blocchi `[data-entra]` della colonna in vista, nell'ordine dall'alto (quello del documento: un blocco dentro un altro viene dopo di
 * lui), entrano uno dopo l'altro; le barre `[data-entra-barra]` di un blocco crescono 220 ms dopo di lui. Quelli sotto lo schermo restano
 * fermi, come sul telefono dove la lista compone solo ciò che si vede.
 */
function run(section: HTMLElement) {
  if (typeof section.animate !== 'function') return
  const view = section.getBoundingClientRect()
  const blocks = [...section.querySelectorAll<HTMLElement>('[data-entra]')].filter((el) => {
    const r = el.getBoundingClientRect()
    return r.height > 0 && r.bottom > view.top && r.top < view.bottom
  })
  if (!blocks.length) return
  const rise = keyframes(curve())
  const riseMs = ((rise.length - 1) * 1000) / 60
  const all: Animation[] = []
  blocks.forEach((el, i) => {
    const delay = delayMs(i)
    all.push(el.animate([{ opacity: 0, filter: `blur(${BLUR_PX}px)` }, { opacity: 1, filter: 'blur(0px)' }], { id: ID, delay, duration: FADE_MS, easing: 'ease', fill: 'backwards' }))
    // La salita non riempie l'attesa: il blocco è già invisibile, e chi misura la pagina in quei millisecondi (il segno di adesso del
    // tabellone) non vede la scala.
    all.push(el.animate(rise, { id: ID, delay, duration: riseMs }))
    for (const bar of el.querySelectorAll<HTMLElement>('[data-entra-barra]')) {
      if (bar.closest('[data-entra]') !== el) continue
      all.push(bar.animate([{ scale: '0 1', transformOrigin: '0 50%' }, { scale: '1 1', transformOrigin: '0 50%' }], { id: ID, delay: barDelayMs(i), duration: BAR_MS, easing: BAR_EASE, fill: 'backwards' }))
    }
  })
  // Una misura presa durante la corsa (getBoundingClientRect vede la scala) si rifà alla fine, come dopo un ridimensionamento.
  void Promise.allSettled(all.map((a) => a.finished)).then(() => dispatchEvent(new Event('resize')))
}

/** 'attesa': la home dell'app prima del primo stato; 'via': col primo stato; null: le colonne della plancia e le anteprime, mai. */
export type Entrata = 'attesa' | 'via' | null

/**
 * Sulla colonna della home (`section.home`). Alla prima home della pagina: in attesa del primo stato la colonna resta nascosta (al più
 * 2,5 s; prima c'era il tabellone vuoto, a zero), poi al fotogramma dopo il primo stato i blocchi entrano. Dopo, e con «riduci
 * movimento», la colonna c'è e basta.
 */
export function entrataHome(section: HTMLElement, state: Entrata) {
  let frame = 0
  let timer: ReturnType<typeof setTimeout> | undefined
  let hidden = false
  const show = () => { if (hidden) { section.style.visibility = ''; hidden = false } }
  const hide = () => { if (!hidden) { section.style.visibility = 'hidden'; hidden = true } }
  const update = (s: Entrata) => {
    if (frame) return
    clearTimeout(timer)
    if (s == null || once.taken) { show(); return }
    if (s === 'attesa' && !ridotto()) { hide(); timer = setTimeout(() => { once.take(); show() }, WAIT_MAX_MS); return }
    if (!plays(once.take(), ridotto())) { show(); return }
    // Al fotogramma dopo, con la home del primo stato già nel documento: i blocchi partono nascosti e la colonna si riaccende con loro.
    hide()
    frame = requestAnimationFrame(() => { frame = 0; show(); run(section) })
  }
  update(state)
  return { update, destroy() { clearTimeout(timer); cancelAnimationFrame(frame) } }
}
