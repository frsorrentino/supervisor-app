// Il motore delle molle di designbookmark.com/motion (shared/motion.js), per le animazioni fedeli (app-animazioni-fedeli-26-11):
// massa 1, a = -k(x - t) - c·v, Eulero semi-implicito, 4 sottopassi per fotogramma, passo al più 1/30 s. Cambiare la meta tiene
// posizione e velocità, come nell'originale. Con il moto ridotto le molle saltano alla meta (regola dell'app: niente movimento).
export type Molla = { x: number; v: number; t: number; k: number; c: number }

export const molla = (x: number, k = 400, c = 30): Molla => ({ x, v: 0, t: x, k, c })

export function passo(s: Molla, dt: number): void {
  const a = -s.k * (s.x - s.t) - s.c * s.v
  s.v += a * dt
  s.x += s.v * dt
}

export const clamp = (v: number, a: number, b: number): number => Math.max(a, Math.min(b, v))

/** Ferma quando la distanza e la velocità sono sotto la soglia: un millesimo, invisibile a 0..1 e sotto il pixel a 0..100. */
export const ferma = (s: Molla): boolean => Math.abs(s.x - s.t) < 1e-3 && Math.abs(s.v) < 1e-3

/** Avanza le molle di dt secondi in 4 sottopassi, come il ciclo dell'originale. */
export function avanza(molle: Molla[], dt: number): void {
  const d = Math.min(dt, 1 / 30) / 4
  for (let i = 0; i < 4; i++) for (const s of molle) passo(s, d)
}

export const ridotto = (): boolean => typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches

/** Un ciclo per componente: [vai] dopo aver cambiato le mete, render a ogni fotogramma finché tutte si fermano. */
export function motore(molle: Molla[], render: () => void): { vai: () => void; stop: () => void } {
  let id = 0
  let last = 0
  const salta = () => { for (const s of molle) { s.x = s.t; s.v = 0 } }
  const loop = (now: number) => {
    avanza(molle, (now - last) / 1000)
    last = now
    if (molle.every(ferma)) { salta(); render(); id = 0; return }
    render()
    id = requestAnimationFrame(loop)
  }
  return {
    vai() {
      if (ridotto() || typeof requestAnimationFrame !== 'function') { salta(); render(); return }
      if (!id) { last = performance.now(); id = requestAnimationFrame(loop) }
    },
    stop() { if (id) cancelAnimationFrame(id); id = 0 },
  }
}

/**
 * Voce 1, fedele all'originale (Streaming reply; app-animazioni-fedeli-26-11): la bolla nuova entra con la molla 300 22, sale di 14 px
 * e l'opacità va da 0; quella di Franz (`me`) anche con la scala da 0,92 senza limite. Moto ridotto: al posto.
 */
export function entra(node: HTMLElement, p: { on: boolean; me: boolean }) {
  if (!p.on || ridotto()) return
  const s = molla(0, 300, 22)
  s.t = 1
  const render = () => {
    node.style.opacity = clamp(s.x, 0, 1).toFixed(3)
    node.style.transform = `translateY(${((1 - s.x) * 14).toFixed(2)}px)` + (p.me ? ` scale(${(0.92 + 0.08 * s.x).toFixed(4)})` : '')
    if (ferma(s)) { node.style.opacity = ''; node.style.transform = '' }
  }
  render()
  const m = motore([s], render)
  m.vai()
  return { destroy: () => m.stop() }
}

/**
 * Voce 1, fedele all'originale (Streaming reply; app-animazioni-fedeli-26-11), parole della risposta arrivata a chat aperta: ogni parola
 * parte 70 ms dopo la precedente (oltre 20 parole l'intervallo si accorcia e la risposta intera esce in 1,4 s, scelta b della master
 * del 26/11), sale di 6 px, l'opacità va da 0 e passa da sfocata (4 px) a nitida con la molla 340 26; la scheda cresce fino al fondo
 * dell'ultima parola (+ 16 px, il suo margine) con la molla 260 26, poi torna alla sua altezza. Moto ridotto: al posto.
 */
export function parole(node: HTMLElement, p: { on: boolean }) {
  if (!p.on || ridotto()) return
  const walker = document.createTreeWalker(node, NodeFilter.SHOW_TEXT)
  const texts: Text[] = []
  while (walker.nextNode()) texts.push(walker.currentNode as Text)
  const words: { el: HTMLSpanElement; s: Molla; on: boolean }[] = []
  for (const tn of texts) {
    const parts = (tn.nodeValue ?? '').split(/(\s+)/)
    if (!parts.some((x) => x.trim())) continue
    const frag = document.createDocumentFragment()
    for (const x of parts) {
      if (!x) continue
      if (!x.trim()) { frag.append(x); continue }
      const el = document.createElement('span')
      el.textContent = x
      el.style.display = 'inline-block'
      el.style.opacity = '0'
      frag.append(el)
      words.push({ el, s: molla(0, 340, 26), on: false })
    }
    tn.replaceWith(frag)
  }
  if (!words.length) return
  const gap = words.length <= 20 ? 70 : 1400 / words.length
  const H = molla(40, 260, 26)
  const full = node.scrollHeight
  node.style.overflow = 'hidden'
  const top = () => node.getBoundingClientRect().top
  const timers: number[] = []
  let done = false
  const render = () => {
    node.style.height = done && ferma(H) ? '' : `${Math.max(0, H.x).toFixed(2)}px`
    for (const w of words) {
      if (!w.on) continue
      const v = clamp(w.s.x, 0, 1.2)
      w.el.style.opacity = clamp(v, 0, 1).toFixed(3)
      w.el.style.transform = `translateY(${((1 - w.s.x) * 6).toFixed(2)}px)`
      w.el.style.filter = v < 0.98 ? `blur(${((1 - clamp(v, 0, 1)) * 4).toFixed(2)}px)` : 'none'
    }
    if (done && ferma(H)) node.style.overflow = ''
  }
  const m = motore([H, ...words.map((w) => w.s)], render)
  words.forEach((w, i) => timers.push(window.setTimeout(() => {
    w.on = true; w.s.t = 1
    const r = w.el.getBoundingClientRect()
    H.t = Math.min(full, r.bottom - top() + 16)
    m.vai()
  }, i * gap)))
  timers.push(window.setTimeout(() => { done = true; H.t = node.scrollHeight; m.vai() }, words.length * gap + 1000))
  render()
  m.vai()
  return { destroy: () => { timers.forEach(clearTimeout); m.stop() } }
}
