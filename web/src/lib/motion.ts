// Il movimento condiviso con il telefono (docs/plans/2026-10-31-animazioni-app.md). Stessa molla di CmMotion.bounce.
const z = 0.7, w = 20, T = 0.62, wd = w * Math.sqrt(1 - z * z)

export const reducedMotion = (): boolean => typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches

/** Per `animate:flip`: da 0 a 1 con circa il 5% di sforamento. */
export function bounceEase(t: number): number {
  if (t >= 1) return 1
  const s = t * T
  return 1 - Math.exp(-z * w * s) * (Math.cos(wd * s) + (z * w / wd) * Math.sin(wd * s))
}
export const bounceMs = (): number => (reducedMotion() ? 0 : T * 1000)

const cssCurve = (name: string): string => getComputedStyle(document.documentElement).getPropertyValue(name).trim() || 'ease-out'

/** Voce 3: quando lo stato cambia l'icona riparte da 0,9 col rimbalzo e dalla dissolvenza senza; al primo disegno niente. */
export function popOnChange(el: HTMLElement, value: unknown) {
  let last = value
  return {
    update(v: unknown) {
      if (v === last) return
      last = v
      if (reducedMotion() || !el.animate) return
      el.animate([{ transform: 'scale(.9)' }, { transform: 'scale(1)' }], { duration: T * 1000, easing: cssCurve('--spring-bounce') })
      el.animate([{ opacity: 0.3 }, { opacity: 1 }], { duration: 180, easing: 'cubic-bezier(0.23, 1, 0.32, 1)' })
    },
  }
}

export const FINISH_STILL_MS = 2000, FINISH_GAP_MS = 10000
export type FinishScene = { fingerDown: boolean; listMoving: boolean; sinceTouchMs: number; wasOnScreen: boolean; writing: boolean; sheetOpen: boolean; distancePx: number; viewportPx: number; sinceLastMs: number }

/** Voce 2: lo scorrimento non deve mai rubare il dito. Se manca una condizione la voce diventa verde dove si trova e basta. */
export const finishScrollAllowed = (s: FinishScene): boolean =>
  !s.fingerDown && !s.listMoving && s.sinceTouchMs >= FINISH_STILL_MS && s.wasOnScreen && !s.writing && !s.sheetOpen &&
  Math.abs(s.distancePx) <= s.viewportPx && s.sinceLastMs >= FINISH_GAP_MS

const out = (t: number) => 1 - (1 - t) ** 4

/** Voce 4: un solo passaggio per tutti i tasti che lavorano: tasto, conferma, attesa, esito. */
export const pass = () => ({ duration: reducedMotion() ? 0 : 200, easing: out, css: (t: number) => `opacity: ${t}; transform: scale(${0.96 + 0.04 * t})` })

/** Voce 5: la sezione si apre in altezza, così quel che sta sotto non salta. Con la riduzione delle animazioni, solo dissolvenza. */
export const fold = (_: Element, { ms = 200 }: { ms?: number } = {}) => ({
  duration: ms, easing: out,
  css: (t: number) => (reducedMotion() ? `opacity: ${t}` : `overflow: hidden; grid-template-rows: ${t}fr; opacity: ${t}`),
})
