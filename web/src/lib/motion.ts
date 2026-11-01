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

// Il cambio forma dei menu (Franz, 01/11: «l'animazione deve riguardare anche il contenuto»; growFromPress in PhoneTheme.kt):
// pannello e contenuto crescono insieme dal punto premuto, come un solo oggetto, da un quinto della grandezza e mai da zero.
export const MENU_FROM = 0.2
const OPEN_MS = 320, CLOSE_MS = 160, PRESS_FRESH_MS = 1000
let lastPress: { x: number; y: number; at: number } | null = null
if (typeof addEventListener === 'function') addEventListener('pointerdown', (e) => { lastPress = { x: e.clientX, y: e.clientY, at: performance.now() } }, { capture: true, passive: true })

/** Il perno della crescita, in pixel dal bordo del menu: il punto premuto; senza un tocco recente [fallback], come frazioni del menu. */
export function pressOrigin(r: { left: number; top: number; width: number; height: number }, press: { x: number; y: number } | null, fallback: [number, number] = [1, 0]): string {
  return press ? `${press.x - r.left}px ${press.y - r.top}px` : `${r.width * fallback[0]}px ${r.height * fallback[1]}px`
}
const recentPress = () => (lastPress && performance.now() - lastPress.at < PRESS_FRESH_MS ? lastPress : null)

/** Per i menu in `{#if}`: `transition:growFromPress`. Si può invertire a metà, e la chiusura è più rapida dell'apertura. */
export const growFromPress = (node: Element) => {
  const o = pressOrigin(node.getBoundingClientRect(), recentPress())
  return ({ direction }: { direction?: 'in' | 'out' | 'both' } = {}) => ({
    duration: reducedMotion() ? 0 : direction === 'out' ? CLOSE_MS : OPEN_MS, easing: out,
    css: (t: number) => `transform-origin: ${o}; scale: ${MENU_FROM + (1 - MENU_FROM) * t}; opacity: ${Math.min(1, t * 6)}`,
  })
}

/**
 * Tutti i `<dialog>` dell'app, in un posto solo: crescono dal tasto all'apertura e ci tornano alla chiusura, anche a metà
 * corsa. `close()` aspetta la fine dell'uscita. Restano fuori gli avvisi, che non nascono da un menu e stanno al centro.
 */
export function installDialogGrow() {
  if (typeof HTMLDialogElement !== 'function') return
  const P = HTMLDialogElement.prototype, show = P.showModal, close = P.close
  const runs = new WeakMap<HTMLDialogElement, { all: Animation[]; closing: boolean }>()
  const grows = (d: HTMLDialogElement) => !reducedMotion() && !!d.animate && !d.matches('.alert, .revdlg, .closef')
  P.showModal = function (this: HTMLDialogElement) {
    const r = runs.get(this)
    if (r?.closing) { r.closing = false; for (const a of r.all) { a.onfinish = null; a.updatePlaybackRate(1); a.play() } return }
    show.call(this)
    if (!grows(this)) return
    const transformOrigin = pressOrigin(this.getBoundingClientRect(), recentPress(), [0.5, 1])
    const all = [
      this.animate([{ scale: MENU_FROM, opacity: 0, transformOrigin }, { opacity: 1, offset: 0.15 }, { scale: 1, opacity: 1, transformOrigin }], { duration: OPEN_MS, easing: 'cubic-bezier(0.23, 1, 0.32, 1)', fill: 'backwards' }),
      this.animate({ opacity: [0, 1] }, { duration: OPEN_MS, pseudoElement: '::backdrop', fill: 'backwards' }),
    ]
    runs.set(this, { all, closing: false })
  }
  P.close = function (this: HTMLDialogElement, value?: string) {
    const r = runs.get(this)
    if (!r || !this.open || !grows(this)) { runs.delete(this); close.call(this, value); return }
    if (r.closing) return
    r.closing = true
    let left = r.all.length
    for (const a of r.all) {
      a.effect?.updateTiming({ fill: 'both' })
      a.onfinish = () => { if (--left || !r.closing) return; runs.delete(this); close.call(this, value); for (const x of r.all) x.cancel() }
      a.updatePlaybackRate(-(OPEN_MS / CLOSE_MS)); a.play()
    }
  }
  // Esc passa dalla stessa uscita.
  document.addEventListener('cancel', (e) => { if (e.target instanceof HTMLDialogElement && runs.has(e.target) && !e.defaultPrevented) { e.preventDefault(); e.target.close() } }, true)
}
