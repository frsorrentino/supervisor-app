// Il regista dei movimenti della plancia (app-fluidita-06-11): apertura e chiusura di una colonna senza la View Transition del
// browser. La transizione rifaceva lo stile di tutto il documento quattro volte (foto prima, aggiornamento, foto dopo, pulizia
// dei nomi), 130-170 ms ciascuna a CPU 4×, e la pagina restava ferma fino a 1,3 s prima che il volo partisse. Qui si misura
// dove stanno schede e colonne prima e dopo il cambio, e ognuna si porta al suo posto con transform e opacity soltanto (FLIP):
// gli stessi effetti di prima, con gli stessi tempi e le stesse curve (v3.css, `fx-open`/`fx-close`): la scheda che vola nella
// colonna con la dissolvenza incrociata, l'onda della home dall'angolo in alto a sinistra, le colonne che fanno posto con la
// molla senza rimbalzo. Il testo si stira durante il volo come prima (flatten, Franz 05/11 11:2x: «va bene testo stirato»).

export type Box = { x: number; y: number; w: number; h: number }
/** `lists`: per una colonna, scorrimento e altezza delle sue liste di chat, per rifarne la copia ferma al punto in vista. */
type Snap = { el: HTMLElement; b: Box; lists?: [number, number][] }

/** Le curve di v3.css: la molla breve senza rimbalzo (--spring-land) e quella della home (--reflow-ease). */
export const SPRING_LAND = 'linear(0, .154, .404, .615, .764, .86, .919, .954, .974, .986, 1)'
export const REFLOW_EASE = 'cubic-bezier(0.23, 1, 0.32, 1)'
/** I tempi di `html.fx-open` e `html.fx-close`: la home, le colonne, la scheda o colonna che vola. */
export const TIMES = { open: { home: 500, cols: 400, focus: 400 }, close: { home: 420, cols: 420, focus: 420 } } as const
/** L'onda: 18 ms a passo, al massimo 160 (waveDelays in App.svelte). */
export const WAVE_STEP = 18, WAVE_MAX = 160

const box = (r: DOMRect): Box => ({ x: r.left, y: r.top, w: r.width, h: r.height })
const seen = (b: Box) => b.w > 0 && b.h > 0 && b.y + b.h > 0 && b.y < innerHeight && b.x + b.w > 0 && b.x < innerWidth

/** Il passaggio da `a` a `b` come transform, con l'origine in alto a sinistra: `b` è dove l'elemento sta davvero. */
export function invert(a: Box, b: Box): string {
  const sx = b.w ? a.w / b.w : 1, sy = b.h ? a.h / b.h : 1
  return `translate(${a.x - b.x}px, ${a.y - b.y}px) scale(${sx}, ${sy})`
}

/** I ritardi dell'onda per chiave, dalle posizioni di partenza: in alto a sinistra prima. */
export function wave(boxes: [string, Box][]): Map<string, number> {
  const m = new Map<string, number>()
  ;[...boxes].sort((p, q) => (p[1].y + p[1].x / 3) - (q[1].y + q[1].x / 3)).forEach(([k], i) => m.set(k, Math.min(WAVE_MAX, i * WAVE_STEP)))
  return m
}

/** Le schede della home (`data-vt`, la prima per nome) e le colonne della plancia che si vedono. */
function snapshot(): Map<string, Snap> {
  const m = new Map<string, Snap>()
  for (const el of document.querySelectorAll<HTMLElement>('[data-vt]')) {
    const k = `vt:${el.dataset.vt}`
    if (m.has(k)) continue
    const b = box(el.getBoundingClientRect())
    if (seen(b)) m.set(k, { el, b })
  }
  for (const el of document.querySelectorAll<HTMLElement>('.desk .col[data-col]')) {
    const b = box(el.getBoundingClientRect())
    if (b.w > 0) m.set(`col:${el.dataset.col}`, { el, b, lists: [...el.querySelectorAll<HTMLElement>('.lines')].map(l => [l.scrollTop, l.clientHeight]) })
  }
  return m
}

/**
 * La copia di quello che esce dal volo, ferma sullo schermo dove stava l'originale, sopra la pagina: come la fotografia della
 * transizione, con quello che si vedeva. Le liste della chat riprendono l'altezza e lo scorrimento che avevano (`lists`).
 */
function ghostOf(el: HTMLElement, b: Box, lists: [number, number][] = []): HTMLElement {
  const g = el.cloneNode(true) as HTMLElement
  g.removeAttribute('data-vt'); g.removeAttribute('data-col'); g.removeAttribute('data-sess')
  g.setAttribute('aria-hidden', 'true')
  Object.assign(g.style, { position: 'fixed', left: `${b.x}px`, top: `${b.y}px`, width: `${b.w}px`, height: `${b.h}px`, margin: '0', zIndex: '61', pointerEvents: 'none', transformOrigin: '0 0', transition: 'none', animation: 'none', contentVisibility: 'visible' })
  document.body.append(g)
  const ls = [...g.querySelectorAll<HTMLElement>('.lines')]
  const hold = () => ls.forEach((l, i) => { const x = lists[i]; if (x && l.scrollTop !== x[0]) l.scrollTop = x[0] })
  ls.forEach((l, i) => { const x = lists[i]; if (x) Object.assign(l.style, { height: `${x[1]}px`, flex: 'none', overflow: 'hidden' }) })
  hold(); requestAnimationFrame(() => { hold(); requestAnimationFrame(hold) })
  return g
}

const running = new Set<Animation>()
/** Un movimento che arriva mentre un altro è in corso riparte da dove gli elementi si vedono in quel momento. */
function stopAll() { for (const a of running) a.cancel(); running.clear() }
function play(el: Element, k: Keyframe[], o: KeyframeAnimationOptions): Animation {
  const a = el.animate(k, o)
  running.add(a)
  const off = () => running.delete(a)
  a.onfinish = off; a.oncancel = off
  return a
}

/** --spring-fx di v3.css: la molla che supera di poco la misura e torna. */
const SPRING_FX = 'linear(0, .25, .6, .86, .98, 1.025, 1.028, 1.018, 1.008, 1.002, 1, .999, 1)'
/**
 * I due capi del collo spezzato (cardFx.lift) si ritiravano dentro la transizione (`fx-neck`, 160 ms): qui se ne fa una copia
 * prima che lift li tolga, e la copia si ritira allo stesso modo.
 */
export function retractNeck(): void {
  for (const s of document.querySelectorAll<SVGSVGElement>('body > svg')) {
    const name = s.style.viewTransitionName
    if (name !== 'neck-top' && name !== 'neck-bottom') continue
    const c = s.cloneNode(true) as SVGSVGElement
    c.style.viewTransitionName = 'none'; c.style.transformOrigin = name === 'neck-top' ? 'top' : 'bottom'
    document.body.append(c)
    c.animate([{ transform: 'scaleY(1)' }, { transform: 'scaleY(0)' }], { duration: 160, easing: SPRING_FX, fill: 'forwards' }).finished.finally(() => c.remove())
  }
}

// Le curve calcolate, per i fotogrammi delle schede annidate: la stessa forma di --reflow-ease e --spring-land.
function bezier(x1: number, y1: number, x2: number, y2: number): (t: number) => number {
  const at = (a: number, b: number, u: number) => 3 * a * u * (1 - u) ** 2 + 3 * b * u * u * (1 - u) + u ** 3
  return (t) => {
    if (t <= 0) return 0
    if (t >= 1) return 1
    let lo = 0, hi = 1, u = t
    for (let i = 0; i < 30; i++) { u = (lo + hi) / 2; if (at(x1, x2, u) < t) lo = u; else hi = u }
    return at(y1, y2, u)
  }
}
const LAND = [0, .154, .404, .615, .764, .86, .919, .954, .974, .986, 1]
export const reflowAt = bezier(0.23, 1, 0.32, 1)
export function landAt(t: number): number {
  if (t <= 0) return 0
  if (t >= 1) return 1
  const f = t * (LAND.length - 1), i = Math.floor(f)
  return LAND[i] + (LAND[i + 1] - LAND[i]) * (f - i)
}
type Move = { from: Box; to: Box; delay: number; dur: number; ease: (t: number) => number }
/** Dove si vede un elemento che si muove, al tempo `ms` dall'inizio. */
export function boxAt(m: Move, ms: number): Box {
  const e = m.ease(Math.min(1, Math.max(0, (ms - m.delay) / m.dur)))
  return { x: m.from.x + (m.to.x - m.from.x) * e, y: m.from.y + (m.to.y - m.from.y) * e, w: m.from.w + (m.to.w - m.from.w) * e, h: m.from.h + (m.to.h - m.from.h) * e }
}
/** Il primo antenato che si muove anche lui, se c'è. */
function outer(el: Element, moves: Map<Element, Move>): Move | null {
  for (let p = el.parentElement; p; p = p.parentElement) { const m = moves.get(p); if (m) return m }
  return null
}
/**
 * I fotogrammi di una scheda dentro un contenitore che si muove: a ogni passo il riquadro voluto sullo schermo riportato nel
 * riferimento del contenitore (che al tempo `ms` sta in `boxAt(up)` invece che in `up.to`), poi invertito sulla scheda.
 */
export function nested(m: Move, up: Move, steps = 24): Keyframe[] {
  const end = Math.max(m.delay + m.dur, up.delay + up.dur)
  return Array.from({ length: steps + 1 }, (_, i) => {
    const ms = (end * i) / steps
    const c = boxAt(m, ms), p = boxAt(up, ms)
    const sx = p.w / up.to.w, sy = p.h / up.to.h
    const want = { x: up.to.x + (c.x - p.x) / sx, y: up.to.y + (c.y - p.y) / sy, w: c.w / sx, h: c.h / sy }
    return { transform: invert(want, m.to), offset: i / steps }
  })
}

/**
 * Nella transizione del browser i gruppi volavano sopra la pagina; qui le schede restano dentro i loro contenitori, e una
 * sezione con content-visibility (o contain: paint) le taglierebbe al suo bordo. Per la durata del volo quei contenitori non
 * tagliano più; le liste che scorrono restano come sono. Ritorna chi rimette tutto com'era.
 */
function unclip(els: Element[]): () => void {
  const done = new Map<HTMLElement, [string, string]>()
  for (const el of els) {
    for (let a = el.parentElement; a && a !== document.body; a = a.parentElement) {
      if (done.has(a)) continue
      const cs = getComputedStyle(a)
      if (cs.contentVisibility === 'auto' || /paint|strict|content/.test(cs.contain)) {
        done.set(a, [a.style.contentVisibility, a.style.contain])
        a.style.contentVisibility = 'visible'; a.style.contain = 'none'
      }
    }
  }
  return () => { for (const [a, [cv, c]] of done) { a.style.contentVisibility = cv; a.style.contain = c } }
}

export type Flight = {
  /** La sessione che entra o esce. */
  focus: string
  opening: boolean
  /** Dove sta la scheda della sessione nella home, prima (aprendo) o dopo (chiudendo). */
  card: () => HTMLElement | null
  /** Il cambio vero, con l'attesa del DOM aggiornato. */
  go: () => Promise<void>
}

/**
 * Apre o chiude la colonna di `focus` con la coreografia di prima. Risolve a volo finito. Le transizioni CSS di `left` e
 * `width` delle colonne (Desk.svelte) restano spente per la durata del volo, come faceva `html.fx-open`.
 */
export async function fly(f: Flight): Promise<void> {
  const t = f.opening ? TIMES.open : TIMES.close
  const before = snapshot()
  stopAll()
  // La scheda toccata diventa la colonna: la sua copia nella home, dopo, entra sfumando al suo posto come faceva nella transizione
  // del browser (il nome della scheda lo prendeva la colonna), invece di attraversare lo schermo.
  const tapped = f.opening ? f.card() : null
  if (tapped?.dataset.vt) before.delete(`vt:${tapped.dataset.vt}`)
  const colKey = `col:${f.focus}`
  // Da dove parte la scheda o la colonna che vola.
  const from = f.opening ? f.card() : before.get(colKey)?.el ?? null
  const fromBox = from ? (f.opening ? box(from.getBoundingClientRect()) : before.get(colKey)!.b) : null
  const cols = [...document.querySelectorAll<HTMLElement>('.desk .col[data-col], .desk .tabslot')]
  const quiet = (els: HTMLElement[]) => els.forEach(el => { el.style.transition = 'none'; el.style.animation = 'none' })
  quiet(cols)
  await f.go()
  const after = snapshot()
  const newCols = [...document.querySelectorAll<HTMLElement>('.desk .col[data-col], .desk .tabslot')]
  quiet(newCols)
  const to = f.opening ? after.get(colKey)?.el ?? null : f.card()
  const toBox = to ? box(to.getBoundingClientRect()) : null
  const delays = wave([...before].filter(([k]) => k.startsWith('vt:') && k !== `vt:${f.focus}`).map(([k, s]) => [k, s.b]))
  const waits: Promise<unknown>[] = []
  const moves = new Map<Element, Move>()
  for (const [k, a] of after) {
    if (a.el === to) continue
    const isCol = k.startsWith('col:')
    const p = before.get(k)
    const o = { duration: isCol ? t.cols : t.home, easing: isCol ? SPRING_LAND : REFLOW_EASE, delay: isCol ? 0 : delays.get(k) ?? 0, fill: 'backwards' as FillMode }
    if (!p) { if (!isCol) waits.push(play(a.el, [{ opacity: 0 }, { opacity: 1 }], o).finished.catch(() => {})); continue }
    if (Math.abs(p.b.x - a.b.x) < 0.5 && Math.abs(p.b.y - a.b.y) < 0.5 && Math.abs(p.b.w - a.b.w) < 0.5 && Math.abs(p.b.h - a.b.h) < 0.5) continue
    moves.set(a.el, { from: p.b, to: a.b, delay: o.delay, dur: o.duration, ease: isCol ? landAt : reflowAt })
  }
  // Quello che esce senza un posto dopo (la colonna sostituita da una scheda, app-scheda-sostituisce-04-11): nella transizione
  // la sua fotografia sfumava dov'era, coi tempi del suo gruppo. Qui una copia leggera fa lo stesso.
  const gone: HTMLElement[] = []
  for (const [k, b] of before) {
    if (after.has(k) || b.el === from || k === `vt:${tapped?.dataset.vt}`) continue
    const isCol = k.startsWith('col:')
    const g = ghostOf(b.el, b.b, b.lists)
    gone.push(g)
    play(g, [{ opacity: 1 }, { opacity: 0 }], { duration: isCol ? t.cols : t.home, easing: isCol ? SPRING_LAND : REFLOW_EASE, fill: 'forwards' })
  }
  const opened = unclip([...moves.keys()])
  for (const [el, m] of moves) {
    const up = outer(el, moves)
    // Una scheda dentro una sezione che si muove anche lei (le sessioni dentro «Sessioni»): nella transizione del browser
    // erano gruppi separati. Qui la scheda eredita il movimento della sezione, quindi i suoi fotogrammi sono calcolati nel
    // riferimento della sezione, tempo per tempo, perché sullo schermo faccia lo stesso percorso di prima.
    const k = up ? nested(m, up) : [{ transform: invert(m.from, m.to) }, { transform: 'none' }]
    const o = up ? { duration: Math.max(m.delay + m.dur, up.delay + up.dur), easing: 'linear', fill: 'backwards' as FillMode }
      : { duration: m.dur, delay: m.delay, easing: m.ease === landAt ? SPRING_LAND : REFLOW_EASE, fill: 'backwards' as FillMode }
    ;(el as HTMLElement).style.transformOrigin = '0 0'
    waits.push(play(el, k, o).finished.catch(() => {}).finally(() => { (el as HTMLElement).style.transformOrigin = '' }))
  }
  if (to && toBox && fromBox) {
    const o = { duration: t.focus, easing: SPRING_LAND }
    to.style.zIndex = '60'
    waits.push(play(to, [{ transform: invert(fromBox, toBox), transformOrigin: '0 0' }, { transform: 'none', transformOrigin: '0 0' }], o).finished.catch(() => {}))
    // Senza dissolvenze (app-morph-senza-stacco-06-11, scelta A, regola Morph): quello che arriva è pieno dal primo fotogramma e
    // vola da solo; prima una copia di quello che usciva sfumava sopra, e il contenuto spariva e ricompariva («lo stacco»).
  }
  await Promise.all(waits)
  opened()
  for (const g of gone) g.remove()
  if (to) to.style.zIndex = ''
  for (const el of new Set([...cols, ...newCols])) { el.style.transition = ''; el.style.animation = '' }
}
