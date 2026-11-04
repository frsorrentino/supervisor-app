// La scheda che diventa colonna (app-effetti-movimenti-04-11, punto 1, Franz 04/11 21:54-21:57, valori da tarare dal vivo).
// Il tocco sulla scheda: si contrae al 98 % per 80 ms, poi si alza dalla lista e fra lei e il suo posto resta un ponte della
// stessa superficie, che si stringe al centro come una vita (dal 100 al 25 % della larghezza) mentre si allunga e si spezza a
// 200 ms; i due capi si ritirano con un piccolo rimbalzo. Da lì la View Transition (smooth in App.svelte, `html.fx-open` in
// v3.css) porta la scheda nella colonna con la molla, e la colonna vicina, già tesa di 12 px verso il posto nuovo, si apre.
// Il + apre senza scheda: solo lo split. Chiudendo, l'inverso più svelto (`html.fx-close`) e la scheda che riassorbe il collo.

export const PRESS_MS = 80, NECK_MS = 200, CLOSE_MS = 420, SETTLE_MS = 220
/** Quanto si alza la scheda, e quanto si tende la colonna vicina verso il posto nuovo. */
export const LIFT_PX = 22, STRETCH_PX = 12
/** La vita del ponte quando si spezza, in frazione della larghezza. */
export const WAIST = 0.25

/**
 * Il ponte fra il fondo della scheda (y = top) e il suo posto (y = bottom), largo `w`, stretto al centro a `waist` della
 * larghezza: due curve che si toccano al centro.
 */
export function neckPath(w: number, top: number, bottom: number, waist: number): string {
  const mid = (top + bottom) / 2, cx = w / 2, half = (w * waist) / 2
  const r = (n: number) => Math.round(n * 10) / 10
  return `M0 ${r(top)} C${r(w * 0.12)} ${r(top)} ${r(cx - half)} ${r(mid - (mid - top) * 0.4)} ${r(cx - half)} ${r(mid)} ` +
    `C${r(cx - half)} ${r(mid + (bottom - mid) * 0.4)} ${r(w * 0.12)} ${r(bottom)} 0 ${r(bottom)} ` +
    `L${r(w)} ${r(bottom)} C${r(w * 0.88)} ${r(bottom)} ${r(cx + half)} ${r(mid + (bottom - mid) * 0.4)} ${r(cx + half)} ${r(mid)} ` +
    `C${r(cx + half)} ${r(mid - (mid - top) * 0.4)} ${r(w * 0.88)} ${r(top)} ${r(w)} ${r(top)} Z`
}

/** Un capo spezzato: una goccia larga `w` appesa al bordo `y`, alta `h` (verso il basso se positiva). */
export function nubPath(w: number, y: number, h: number): string {
  const r = (n: number) => Math.round(n * 10) / 10
  return `M${r(w * 0.3)} ${r(y)} Q${r(w / 2)} ${r(y + h * 2)} ${r(w * 0.7)} ${r(y)} Z`
}

/** La vita a un punto del collo, `t` da 0 a 1: dal 100 % al 25 %, più svelta verso la fine. */
export const waistAt = (t: number): number => 1 - (1 - WAIST) * Math.min(1, Math.max(0, t)) ** 1.6

const ease = (t: number) => 1 - (1 - t) ** 3

/**
 * Il collo prima della View Transition: la scheda si contrae e si alza, il ponte si stringe e si spezza; `neighbor` (la colonna
 * vicina al posto nuovo) si tende di STRETCH_PX verso `dir`. Risolve a NECK_MS con `done`, da chiamare quando la transizione ha
 * fotografato la pagina: toglie il ponte e ferma le animazioni. I due capi spezzati hanno i nomi `neck-top` e `neck-bottom`, e
 * nella transizione si ritirano (v3.css).
 */
export function lift(card: HTMLElement, neighbor: HTMLElement | null, dir: 1 | -1): Promise<() => void> {
  const r = card.getBoundingClientRect()
  const anims: Animation[] = []
  anims.push(card.animate(
    [{ transform: 'none' }, { transform: 'scale(.98)', offset: PRESS_MS / NECK_MS }, { transform: `translateY(-${LIFT_PX}px)` }],
    { duration: NECK_MS, easing: 'linear', fill: 'forwards' },
  ))
  if (neighbor) anims.push(neighbor.animate([{ transform: 'none' }, { transform: `translateX(${dir * STRETCH_PX}px)` }], { duration: NECK_MS, easing: 'cubic-bezier(.23,1,.32,1)', fill: 'forwards' }))
  const surface = getComputedStyle(document.documentElement).getPropertyValue('--s2').trim() || '#181818'
  const svg = (name: string) => {
    const s = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    s.setAttribute('aria-hidden', 'true')
    Object.assign(s.style, { position: 'fixed', left: `${r.left}px`, top: `${r.bottom - LIFT_PX}px`, width: `${r.width}px`, height: `${LIFT_PX}px`, overflow: 'visible', pointerEvents: 'none', zIndex: '60', viewTransitionName: name })
    s.setAttribute('viewBox', `0 0 ${r.width} ${LIFT_PX}`)
    const p = document.createElementNS('http://www.w3.org/2000/svg', 'path')
    p.setAttribute('fill', surface)
    s.append(p); document.body.append(s)
    return { s, p }
  }
  const a = svg('neck-top'), b = svg('neck-bottom')
  const t0 = performance.now()
  return new Promise(resolve => {
    let ended = false
    const end = () => {
      if (ended) return
      ended = true
      // Spezzato: un capo resta appeso alla scheda alzata, l'altro al suo posto.
      a.p.setAttribute('d', nubPath(r.width, 0, 5))
      b.p.setAttribute('d', nubPath(r.width, LIFT_PX, -5))
      resolve(() => { a.s.remove(); b.s.remove(); for (const x of anims) x.cancel() })
    }
    const frame = (now: number) => {
      if (ended) return
      const t = Math.min(1, (now - t0) / NECK_MS)
      const up = t < PRESS_MS / NECK_MS ? 0 : ease((t - PRESS_MS / NECK_MS) / (1 - PRESS_MS / NECK_MS)) * LIFT_PX
      a.p.setAttribute('d', neckPath(r.width, LIFT_PX - up, LIFT_PX, waistAt(t)))
      if (t >= 1) end(); else requestAnimationFrame(frame)
    }
    requestAnimationFrame(frame)
    // Senza fotogrammi (pagina in secondo piano) il collo finisce lo stesso.
    setTimeout(end, NECK_MS + 60)
  })
}

/** Chiudendo, la scheda tornata riassorbe il collo: si allunga di poco verso il basso e si assesta. */
export function settle(card: HTMLElement): void {
  card.animate([{ transform: 'scaleY(1.04)', transformOrigin: 'top' }, { transform: 'scaleY(.985)', transformOrigin: 'top', offset: 0.55 }, { transform: 'none', transformOrigin: 'top' }], { duration: SETTLE_MS, easing: 'ease-out' })
}

/** I tempi della scheda confermata che va nel contatore (punto 4): il giro, la piega, il volo; il battito del contatore. */
export const FLY_MS = 650, PULSE_MS = 220

/**
 * La scheda confermata (app-effetti-movimenti-04-11, punto 4): dopo la spunta una sua copia fa un giro su se stessa, si piega in
 * una striscia e vola nel contatore di «Tocca a te» (`[data-counter="you"]`), che batte una volta; senza contatore in vista sale
 * e sparisce. La scheda vera esce intanto con la sua transizione; `back` la rimette se il PC dice no.
 */
export function toCounter(el: Element | null, reduced: boolean): (() => void) | null {
  if (!el || reduced || typeof document === 'undefined') return null
  const r = el.getBoundingClientRect()
  if (!r.width || !r.height) return null
  const target = [...document.querySelectorAll<HTMLElement>('[data-counter="you"]')].find(x => { const b = x.getBoundingClientRect(); return b.width > 0 && b.bottom > 0 && b.top < innerHeight })
  const t = target?.getBoundingClientRect()
  const dx = t ? t.left + t.width / 2 - (r.left + r.width / 2) : 0
  const dy = t ? t.top + t.height / 2 - (r.top + r.height / 2) : -60
  const clone = el.cloneNode(true) as HTMLElement
  Object.assign(clone.style, { position: 'fixed', left: `${r.left}px`, top: `${r.top}px`, width: `${r.width}px`, height: `${r.height}px`, margin: '0', zIndex: '80', pointerEvents: 'none', overflow: 'hidden', borderRadius: '12px', background: 'var(--s2)', transformOrigin: '50% 50%' })
  clone.setAttribute('aria-hidden', 'true')
  document.body.append(clone)
  const real = el as HTMLElement
  real.style.visibility = 'hidden'
  const p = 'perspective(900px)'
  const a = clone.animate([
    { transform: `${p} rotateY(0deg)`, opacity: 1 },
    { transform: `${p} rotateY(360deg)`, opacity: 1, offset: 0.35 },
    { transform: `${p} scale(1, .12)`, opacity: 1, offset: 0.55 },
    { transform: `translate(${dx}px, ${dy}px) scale(.08, .04)`, opacity: t ? 0.9 : 0 },
  ], { duration: FLY_MS, easing: 'cubic-bezier(.45, 0, .25, 1)' })
  const end = () => { clone.remove(); target?.animate([{ transform: 'none' }, { transform: 'scale(1.25)' }, { transform: 'none' }], { duration: PULSE_MS, easing: 'ease-out' }) }
  a.onfinish = end; a.oncancel = () => clone.remove()
  setTimeout(() => clone.remove(), FLY_MS + 200)
  return () => { real.style.visibility = '' }
}
