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
