import { springFrames } from './gesture'

// Il blocco della master che cambia forma mentre lo si trascina fra riga, colonna di lato e pila (app-master-morph-m3-06-11,
// Franz 06/11 01:12-01:14, «ok m3»): un Blob Morph ammorbidito scritto qui. Posizione, misure e raggio vanno dal posto di partenza
// a quello d'arrivo con l'avanzamento del dito; il bordo verso il dito si allunga a goccia; il contenuto della riga sfuma mentre
// compare la presa della testata. Al rilascio la molla del cambio di lato completa la forma; annullato, torna indietro.

export type Rect = { x: number; y: number; w: number; h: number }
export type Shape = Rect & { rTop: number; rBottom: number }

/** Quanto è avanti il dito fra partenza e arrivo, da 0 a 1, misurato sul bordo alto (o sull'altezza, se il bordo non si sposta). */
export function progress(from: Rect, to: Rect, dy: number): number {
  const span = to.y - from.y || from.h - to.h
  if (!span) return dy === 0 ? 0 : 1
  return Math.max(0, Math.min(1, dy / span))
}

/** La goccia: fino a quanti px il bordo si allunga verso il dito, e quanto lo segue. */
export const STRETCH_MAX = 44
const STRETCH_K = 0.4

/**
 * La forma all'avanzamento `p` (già con la sua curva: veloce all'inizio, morbida alla fine), tirata verso il dito `fy`, `fx`: il
 * bordo alto si allunga verso il dito fino a STRETCH_MAX e si arrotonda; il centro si sposta appena verso il dito.
 */
export function blobShape(from: Rect, to: Rect, p: number, fx: number, fy: number): Shape {
  const e = 1 - (1 - p) * (1 - p)
  const l = (a: number, b: number) => a + (b - a) * e
  const x = l(from.x, to.x), w = l(from.w, to.w)
  let y = l(from.y, to.y), h = l(from.h, to.h)
  const out = Math.max(0, y - fy)
  const stretch = Math.min(STRETCH_MAX, out * STRETCH_K)
  y -= stretch; h += stretch
  const cx = x + w / 2, nudge = Math.max(-24, Math.min(24, (fx - cx) * 0.06))
  return { x: x + nudge, y, w, h, rTop: 14 + stretch * 0.5, rBottom: 14 * (1 - Math.abs(2 * e - 1)) }
}

const css = (s: Shape) => ({ left: `${s.x}px`, top: `${s.y}px`, width: `${s.w}px`, height: `${s.h}px`, borderRadius: `${s.rTop}px ${s.rTop}px ${s.rBottom}px ${s.rBottom}px` })
const mix = (a: Shape, b: Shape, q: number): Shape => ({
  x: a.x + (b.x - a.x) * q, y: a.y + (b.y - a.y) * q, w: a.w + (b.w - a.w) * q, h: a.h + (b.h - a.h) * q,
  rTop: a.rTop + (b.rTop - a.rTop) * q, rBottom: a.rBottom + (b.rBottom - a.rBottom) * q,
})

/** Il blocco sullo schermo: la copia della master di partenza che sfuma, la presa che compare, la forma che segue il dito. */
export function morphBlob(src: HTMLElement) {
  const r = src.getBoundingClientRect()
  const from: Rect = { x: r.left, y: r.top, w: r.width, h: r.height }
  const el = document.createElement('div')
  el.className = 'master-blob'
  el.style.cssText = 'position:fixed;z-index:65;overflow:hidden;pointer-events:none;background:var(--s1);border:1px solid var(--line);box-shadow:0 10px 32px rgb(0 0 0 / .45)'
  // La copia riprende lo scorrimento dell'originale: clonata, la chat ripartiva dall'inizio invece che dal punto in vista
  // (app-fluidita-06-11). Si segnano gli elementi scorsi, si clona, si ricopia lo scorrimento una volta nel documento.
  // Solo le liste della chat scorrono nel pannello: leggere lo scorrimento di ogni nodo costava 70-100 ms a CPU 4× con una chat
  // lunga all'avvio del gesto.
  const scrolled = [...src.querySelectorAll<HTMLElement>('.lines')].filter(e => e.scrollTop > 0 || e.scrollLeft > 0)
  scrolled.forEach((e, i) => e.setAttribute('data-blob-scroll', String(i)))
  const copy = src.cloneNode(true) as HTMLElement
  scrolled.forEach(e => e.removeAttribute('data-blob-scroll'))
  copy.style.cssText += `;position:absolute;left:0;top:0;width:${r.width}px;height:${r.height}px;margin:0;transform:none`
  const grip = document.createElement('i')
  grip.style.cssText = 'position:absolute;left:50%;top:10px;width:48px;height:4px;margin-left:-24px;border-radius:2px;background:var(--tx3);opacity:0'
  el.append(copy, grip)
  document.body.appendChild(el)
  // Fuori dal suo posto la lista perde l'altezza che la fa scorrere: le si dà quella vera. Lo scorrimento si dà quando il blocco
  // ha già la sua forma (prima la copia non ha misura e resta a 0) e si ridà a ogni movimento finché non tiene.
  const keep: [HTMLElement, number, number][] = []
  copy.querySelectorAll<HTMLElement>('[data-blob-scroll]').forEach(c => {
    const o = scrolled[Number(c.getAttribute('data-blob-scroll'))]
    c.removeAttribute('data-blob-scroll')
    if (!o) return
    c.style.height = `${o.clientHeight}px`; c.style.flex = 'none'; c.style.overflow = 'hidden'
    keep.push([c, o.scrollTop, o.scrollLeft])
  })
  const hold = () => { for (const [c, top, left] of keep) { if (c.scrollTop !== top) c.scrollTop = top; if (c.scrollLeft !== left) c.scrollLeft = left } }
  src.style.visibility = 'hidden'
  let now: Shape = { ...from, rTop: 0, rBottom: 0 }
  Object.assign(el.style, css(now))
  hold()
  const settle = (to: Shape, v: number, done: () => void) => {
    const start = now
    const f = springFrames(1, -v)
    el.animate(f.map(k => css(mix(start, to, 1 - k))), { duration: (f.length / 60) * 1000, easing: 'linear', fill: 'forwards' }).finished.then(done, done)
  }
  return {
    from,
    /** Il dito a `fx`, `fy`, spostato di `dy` dall'inizio, verso il posto `to`. */
    move(to: Rect, dy: number, fx: number, fy: number) {
      const p = progress(from, to, dy)
      now = blobShape(from, to, p, fx, fy)
      Object.assign(el.style, css(now))
      hold()
      copy.style.opacity = String(Math.max(0, 1 - p * 1.6))
      grip.style.opacity = String(Math.max(0, p * 1.4 - 0.4))
    },
    /** Al rilascio: la forma va al posto vero `to` con la molla (`v`, la velocità in avanzamento al secondo), poi lascia il posto alla master. */
    land(to: HTMLElement, v: number) {
      const t = to.getBoundingClientRect()
      to.style.opacity = '0'
      settle({ x: t.left, y: t.top, w: t.width, h: t.height, rTop: 0, rBottom: 0 }, v, () => {
        el.remove(); to.style.opacity = ''; src.style.visibility = ''
        to.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 140 })
      })
    },
    /** Annullato: torna alla forma di partenza e restituisce la master com'era. */
    back(v: number) {
      settle({ ...from, rTop: 0, rBottom: 0 }, v, () => { el.remove(); src.style.visibility = '' })
    },
  }
}
