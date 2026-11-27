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
 * app-messaggio-master-effetto-27-11, opzione E «Subito, due segnali» (Franz 27/11 15:48, mockup 04-subito.html; prima la B), come
 * ArrivoRisposta.kt: la risposta che arriva a chat aperta e ferma compare intera in 120 ms e sale con la lista, una corsa sola con la molla
 * del pannello (400 40, senza rimbalzo, a pixel interi), senza scoperte. 150 ms dopo due segnali: il fondo del piede si accende e torna in
 * 900 ms, e la spunta dell'esito entra da 0,8 con la molla di casa (620 ms). Un dito o la rotella mostrano subito tutto e lasciano la lista
 * lì; una risposta nuova chiude subito quella in corsa; più in alto, o con la lista che si muove, niente (resta «1 nuovo ↓»). Moto ridotto:
 * la chat si spegne in 70 ms, va in fondo e si riaccende in 150 ms; niente lampo.
 */
let chiudiCorsa: (() => void) | null = null
/** C'è una risposta in arrivo: la chat non fa il suo scorrimento morbido verso il fondo. */
export const corsaInArrivo = (): boolean => chiudiCorsa != null
const EASE = 'cubic-bezier(.3, 0, .2, 1)'
/** Un colore calcolato come terna rgb, anche nella forma color(srgb …) che dà color-mix. */
export function rgbOf(c: string): [number, number, number] | null {
  let m = c.match(/rgba?\(\s*([\d.]+)[,\s]+([\d.]+)[,\s]+([\d.]+)/)
  if (m) return [+m[1], +m[2], +m[3]]
  m = c.match(/color\(srgb\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)/)
  if (m) return [+m[1] * 255, +m[2] * 255, +m[3] * 255]
  return null
}
/** Il fondo acceso del lampo: il fondo del piede spinto verso il colore dell'esito (dal 10 al 26 % circa, come nel mockup). */
export const litOf = (base: [number, number, number], tone: [number, number, number]): string =>
  `rgb(${base.map((v, i) => Math.round(v + (tone[i] - v) * 0.18)).join(', ')})`
export function arriva(node: HTMLElement, p: { on: boolean }) {
  if (!p.on) return
  chiudiCorsa?.()
  const list = node.closest<HTMLElement>('[data-chat-list]')
  // La corsa parte solo a lista ferma (Franz 27/11 15:17): se la lista si muove, la risposta compare intera al suo posto.
  if (!list || list.dataset.atEnd !== 'true' || list.dataset.moving === 'true') return
  const tail = node.closest('.it')?.querySelector<HTMLElement>(':scope > .tm') ?? null
  const parts = tail ? [node, tail] : [node]
  const s0 = list.scrollTop
  const d = Math.max(0, Math.round(list.scrollHeight - list.clientHeight - s0))
  const anims: Animation[] = []
  let m: { vai: () => void; stop: () => void } | null = null
  let t: ReturnType<typeof setTimeout> | undefined
  const libera = () => {
    for (const ev of ['pointerdown', 'wheel', 'touchstart'] as const) list.removeEventListener(ev, finito)
    if (chiudiCorsa === finito) chiudiCorsa = null
  }
  // Tutto intero e fermo: opacità piena, lampo spento, spunta a grandezza piena.
  const finito = () => { m?.stop(); clearTimeout(t); for (const a of anims) a.finish(); libera() }
  chiudiCorsa = finito
  for (const ev of ['pointerdown', 'wheel', 'touchstart'] as const) list.addEventListener(ev, finito, { passive: true })
  if (ridotto()) {
    if (d <= 0) { anims.push(...parts.map((el) => el.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 120, easing: EASE }))); libera(); return { destroy: finito } }
    const out = list.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 70, fill: 'forwards' })
    t = setTimeout(() => {
      list.scrollTop = list.scrollHeight - list.clientHeight
      out.cancel()
      list.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 150, easing: EASE })
      libera()
    }, 80)
    return { destroy: finito }
  }
  anims.push(...parts.map((el) => el.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 120, easing: EASE })))
  const foot = node.querySelector<HTMLElement>('.zf')
  const gl = node.querySelector<HTMLElement>('.zf .gl')
  if (foot && gl) {
    const now = getComputedStyle(foot).backgroundColor
    const base = rgbOf(now), tone = rgbOf(getComputedStyle(gl).color)
    if (base && tone) anims.push(foot.animate([{ backgroundColor: litOf(base, tone) }, { backgroundColor: now }], { duration: 900, delay: 150, easing: EASE }))
    const bounce = getComputedStyle(document.documentElement).getPropertyValue('--spring-bounce').trim() || EASE
    anims.push(gl.animate([{ transform: 'scale(.8)' }, { transform: 'scale(1)' }], { duration: 620, delay: 150, easing: bounce, fill: 'backwards' }))
  }
  if (d <= 0) { libera(); return { destroy: finito } }
  const S = molla(0, 400, 40)
  S.t = d
  let done = 0
  m = motore([S], () => {
    const to = Math.min(d, Math.max(0, Math.round(S.x)))
    if (to !== done) { list.scrollTop = s0 + to; done = to }
    // Finita la corsa la chat torna a seguire il fondo; lampo e spunta finiscono da soli.
    if (done >= d && ferma(S)) { m?.stop(); libera() }
  })
  m.vai()
  return { destroy: finito }
}

/**
 * Voce 2, fedele all'originale (Morph island; app-animazioni-fedeli-26-11), la barra della master che si allunga: l'altezza del testo va
 * con la molla 300 22 quando cresce e 390 22 quando cala; mentre corre la barra cede in larghezza fino al 5 % (`-v/7000`); a ogni cambio
 * la barra si abbassa e torna con la molla 520 18 (spinta -1,6 se cresce, -1 se cala). Moto ridotto: altezza criticamente smorzata.
 */
export function isola(node: HTMLElement, peek: boolean) {
  const bar = (node.closest('.dock') as HTMLElement | null) ?? node
  const H = molla(node.offsetHeight, 300, 22)
  const P = molla(1, 520, 18)
  let last = peek
  let moving = false
  const render = () => {
    const give = clamp(-H.v / 7000, -0.05, 0.05)
    bar.style.transformOrigin = '50% 100%'
    bar.style.transform = `scale(${(P.x * (1 + give)).toFixed(4)}, ${P.x.toFixed(4)})`
    node.style.height = moving && !(ferma(H) && ferma(P)) ? `${Math.max(20, H.x).toFixed(2)}px` : ''
    node.style.overflow = node.style.height ? 'hidden' : ''
    if (ferma(H) && ferma(P)) { moving = false; bar.style.transform = ''; node.style.height = '' }
  }
  const m = motore([H, P], render)
  return {
    update(p: boolean) {
      if (p === last) return
      last = p
      const from = moving ? H.x : node.offsetHeight
      node.style.height = ''
      const to = node.offsetHeight
      const grow = to > from
      const k = 300 * (grow ? 1 : 1.3)
      H.k = k; H.c = ridotto() ? 2 * Math.sqrt(k) : 22
      H.x = from; H.t = to
      P.v -= grow ? 1.6 : 1
      moving = true
      // Il contenuto che entra: dopo 90 ms, da sfocato (6 px) e scala 0,9, opacità in 350 ms, scala in 600 ms con (.16, 1, .3, 1).
      if (!ridotto() && node.animate) {
        node.animate([{ opacity: 0, filter: 'blur(6px)' }, { opacity: 1, filter: 'blur(0px)' }], { duration: 350, delay: 90, easing: 'ease', fill: 'backwards' })
        node.animate([{ scale: '0.9' }, { scale: '1' }], { duration: 600, delay: 90, easing: 'cubic-bezier(.16, 1, .3, 1)', fill: 'backwards' })
      }
      render()
      m.vai()
    },
    destroy: () => m.stop(),
  }
}

/**
 * Voce 3, fedele all'originale (Notification stack; app-animazioni-fedeli-26-11): all'apertura di «Per te» ogni voce scende dalla pila al
 * suo posto, da 10 px per livello sotto la testa e con la scala del suo livello (-5 % ciascuno), in 300 ms con (0.23, 1, 0.32, 1); dalla
 * quarta entrano dall'invisibile. Moto ridotto: solo l'opacità, come l'originale.
 */
export function cala(node: HTMLElement, index: number) {
  if (!node.animate) return
  const depth = Math.min(index + 1, 3)
  const first = node.parentElement?.firstElementChild as HTMLElement | null
  const y = node.offsetTop - (first?.offsetTop ?? 0)
  const ease = 'cubic-bezier(0.23, 1, 0.32, 1)'
  if (ridotto()) {
    if (depth > 2) node.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 300, easing: ease })
    return
  }
  node.style.transformOrigin = '50% 0'
  node.animate(
    [{ transform: `translateY(${-y + 10 * depth}px) scale(${1 - 0.05 * depth})`, opacity: depth > 2 ? 0 : 1 }, { transform: 'none', opacity: 1 }],
    { duration: 300, easing: ease },
  )
}

/**
 * Voce 6a, fedele all'originale (Ghost suggestion; app-animazioni-fedeli-26-11, scelta della master del 26/11): preso il suggerimento,
 * le sue lettere vere, sopra il campo, passano dal grigio al colore pieno in 180 ms e fanno un piccolo salto (spinta di 70 px/s verso
 * l'alto sulla molla 520 16), da sinistra a destra con 16 ms di scarto; intanto il testo del campo è trasparente, poi torna lui.
 */
export function saltaLettere(area: HTMLTextAreaElement, from = 'var(--stale)', to = 'var(--text)') {
  if (ridotto() || !area.parentElement) return
  const text = area.value
  if (!text) return
  const cs = getComputedStyle(area)
  const box = area.parentElement
  if (getComputedStyle(box).position === 'static') box.style.position = 'relative'
  const ov = document.createElement('div')
  ov.setAttribute('aria-hidden', 'true')
  for (const k of ['font', 'letterSpacing', 'lineHeight', 'paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft', 'borderTopWidth', 'borderLeftWidth', 'textAlign', 'tabSize'] as const) (ov.style as unknown as Record<string, string>)[k] = cs[k]
  Object.assign(ov.style, { position: 'absolute', left: `${area.offsetLeft}px`, top: `${area.offsetTop}px`, width: `${area.offsetWidth}px`, height: `${area.offsetHeight}px`, boxSizing: 'border-box', borderStyle: 'solid', borderColor: 'transparent', whiteSpace: 'pre-wrap', overflowWrap: 'break-word', overflow: 'hidden', pointerEvents: 'none' })
  const letters: { el: HTMLSpanElement; s: Molla }[] = []
  for (const part of text.split(/(\s+)/)) {
    if (!part) continue
    if (!part.trim()) { ov.append(part); continue }
    const w = document.createElement('span')
    w.style.whiteSpace = 'nowrap'
    for (const ch of part) {
      const el = document.createElement('span')
      el.textContent = ch
      Object.assign(el.style, { display: 'inline-block', color: from, transition: 'color .18s ease' })
      w.append(el)
      letters.push({ el, s: molla(0, 520, 16) })
    }
    ov.append(w)
  }
  ov.scrollTop = area.scrollTop
  box.append(ov)
  const was = area.style.color
  area.style.color = 'transparent'
  const render = () => { for (const l of letters) l.el.style.transform = `translateY(${l.s.x.toFixed(2)}px)` }
  const m = motore(letters.map((l) => l.s), render)
  letters.forEach((l, i) => setTimeout(() => { l.el.style.color = to; l.s.v = -70; m.vai() }, i * 16))
  setTimeout(() => { m.stop(); ov.remove(); area.style.color = was }, letters.length * 16 + 500)
}

/**
 * Voce 4 del terzo giro, fedele all'originale (Prompt box e Mention picker; app-animazioni-terzo-c-26-11): sul tasto d'invio la freccia
 * diventa il quadrato dello Stop sulla molla 520 30 (la freccia scala a 0 ruotando di 90°, il quadrato scala fino a 1,2 e torna);
 * all'invio il tasto si stringe a 0,94 in 260 ms e la freccia vola su di 26 px svanendo e sfocandosi, poi rientra da 22 px sotto, in
 * 560 ms con (.3, .7, .2, 1). Moto ridotto: cambia e basta.
 */
export function invio(node: HTMLButtonElement, stop: boolean) {
  const M = molla(stop ? 1 : 0, 520, 30)
  const ar = () => node.querySelector<SVGElement>('.ar'), sq = () => node.querySelector<SVGElement>('.sq')
  const render = () => {
    const m = clamp(M.x, 0, 1.2)
    const a = ar(), q = sq()
    if (a) a.style.transform = `scale(${Math.max(0, 1 - m).toFixed(4)}) rotate(${(m * 90).toFixed(2)}deg)`
    if (q) q.style.transform = `scale(${Math.max(0, m).toFixed(4)})`
  }
  const mm = motore([M], render)
  render()
  const fly = () => {
    if (M.t === 1 || ridotto() || !node.animate) return
    ar()?.animate([{ translate: '0 0', opacity: 1, filter: 'blur(0)' }, { translate: '0 -26px', opacity: 0, filter: 'blur(2px)', offset: 0.5 }, { translate: '0 22px', opacity: 0, filter: 'blur(2px)', offset: 0.5 }, { translate: '0 0', opacity: 1, filter: 'blur(0)' }], { duration: 560, easing: 'cubic-bezier(.3, .7, .2, 1)' })
    node.animate([{ scale: '1' }, { scale: '.94' }, { scale: '1' }], { duration: 260 })
  }
  node.addEventListener('click', fly)
  return {
    update(s: boolean) { M.t = s ? 1 : 0; if (ridotto()) { M.x = M.t; M.v = 0; render() } else mm.vai() },
    destroy() { node.removeEventListener('click', fly); mm.stop() },
  }
}

/** Voce 4 del terzo giro (Prompt box): il campo cresce con il testo, fino a cinque righe, sulla molla 420 34 invece di scattare. */
export function crescita(node: HTMLTextAreaElement, _v: string) {
  const H = molla(node.offsetHeight, 420, 34)
  let on = false
  const render = () => { node.style.height = on && !ferma(H) ? `${H.x.toFixed(2)}px` : ''; if (ferma(H)) on = false }
  const mm = motore([H], render)
  return {
    update() {
      const from = on ? H.x : node.offsetHeight
      node.style.height = ''
      const to = node.offsetHeight
      if (Math.abs(to - from) < 0.5 || ridotto()) { H.x = H.t = to; H.v = 0; return }
      H.x = from; H.t = to; on = true
      render(); mm.vai()
    },
    destroy: () => mm.stop(),
  }
}

/**
 * Voce 6 del terzo giro, fedele all'originale (File drop; app-animazioni-terzo-c-26-11): al rilascio ogni file (al massimo tre) parte
 * dal punto del rilascio come una pastiglia col suo nome e vola nel campo: prima pigro (molla 150 18), dopo 0,35 s deciso (520 36),
 * inclinato dalla sua velocità (fino a 16°); arrivato si stringe a 0 (520 34) e svanisce 0,16 s dopo. Moto ridotto: niente volo.
 */
export function volaFile(names: string[], x: number, y: number, target: HTMLElement | null) {
  if (ridotto() || !target || !names.length) return
  const r = target.getBoundingClientRect()
  const tx = r.left + 24, ty = r.top + r.height / 2
  names.slice(0, 3).forEach((name, i) => {
    const el = document.createElement('div')
    el.textContent = name
    el.setAttribute('aria-hidden', 'true')
    Object.assign(el.style, { position: 'fixed', left: '0', top: '0', zIndex: '60', height: '44px', padding: '0 14px', display: 'flex', alignItems: 'center', borderRadius: '14px', background: 'var(--tx)', color: 'var(--bg, #000)', font: '500 14px var(--sans, sans-serif)', whiteSpace: 'nowrap', pointerEvents: 'none', transformOrigin: '50% 50%' })
    document.body.append(el)
    const FX = molla(x + i * 12, 150, 18), FY = molla(y + i * 12, 150, 18), FS = molla(1, 520, 34)
    FX.t = tx + i * 8; FY.t = ty
    let gone = false
    const render = () => {
      const rot = clamp(FX.v * 0.04, -16, 16)
      el.style.transform = `translate(${(FX.x - el.offsetWidth / 2).toFixed(1)}px, ${(FY.x - 22).toFixed(1)}px) rotate(${rot.toFixed(2)}deg) scale(${Math.max(0, FS.x).toFixed(4)})`
      if (gone) el.style.opacity = '0'
    }
    const mm = motore([FX, FY, FS], render)
    render(); mm.vai()
    setTimeout(() => { FX.k = FY.k = 520; FX.c = FY.c = 36; mm.vai() }, 350 + i * 60)
    setTimeout(() => { FS.t = 0; mm.vai() }, 700 + i * 60)
    setTimeout(() => { gone = true; render() }, 860 + i * 60)
    setTimeout(() => { mm.stop(); el.remove() }, 1300 + i * 60)
  })
}
