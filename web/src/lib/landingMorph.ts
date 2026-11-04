// L'anteprima a fantasma della master (app-trascina-rifinitura-04-11, scelta A di Franz, 04/11 17:21): durante ogni gesto lo
// sfondo si scurisce appena e una sagoma traslucida gialla occupa il posto d'arrivo con la misura vera, con l'etichetta del
// posto dentro; cambiando posto scivola dal punto dov'è al nuovo. Al rilascio la master va al posto della sagoma, che poi
// sparisce; annullato il gesto sparisce subito. Prende il posto dei segni sui bordi e del riquadro giallo d'arrivo.
export type Box = { x: number; y: number; w: number; h: number }

/** Il posto della master adesso: il pannello sotto la home, il pannello di lato, quello della plancia, o la riga. */
export const PLACES = ['.dpanel.under', 'aside.dpanel:not(.stackcol)', 'aside.mpanel', '.mbar'] as const

/** I tempi: lo spostamento della master al rilascio (il gruppo `master-morph` in v3.css), lo scivolare della sagoma da un posto
 *  all'altro, lo sfondo che si scurisce, la sagoma che sparisce. */
export const MOVE_MS = 380, SLIDE_MS = 200, DIM_MS = 160, FADE_MS = 200
export const MOVE_EASE = 'cubic-bezier(0.23, 1, 0.32, 1)'

const css = (b: Box) => ({ left: `${b.x}px`, top: `${b.y}px`, width: `${b.w}px`, height: `${b.h}px` })
const same = (a: Box, b: Box) => a.x === b.x && a.y === b.y && a.w === b.w && a.h === b.h

/** La sagoma di un gesto alla volta; `reduced`: col movimento ridotto niente scivolate e niente sfumature. */
export function ghostPreview(reduced: () => boolean) {
  let dim: HTMLElement | null = null, box: HTMLElement | null = null, tag: HTMLElement | null = null, at: Box | null = null
  // Le altre destinazioni (app-trascina-destinazioni-04-11): contorni tratteggiati con l'etichetta, e la linea a metà schermo.
  let marks: HTMLElement[] = [], mid: HTMLElement | null = null
  const fadeOut = (el: HTMLElement, ms: number) => {
    // Il nome della transizione serve solo finché la master arriva: uscendo lo lascia a una sagoma nuova.
    el.style.viewTransitionName = 'none'
    if (reduced()) { el.remove(); return }
    const a = el.animate([{ opacity: getComputedStyle(el).opacity }, { opacity: 0 }], { duration: ms, easing: 'ease-out', fill: 'forwards' })
    a.onfinish = a.oncancel = () => el.remove()
    // Anche se l'animazione non arriva in fondo (pagina nascosta, transizione saltata), la sagoma non resta sulla pagina.
    setTimeout(() => el.remove(), ms + 100)
  }
  const reset = () => { dim = box = tag = null; at = null }
  const clearHints = (fade: boolean) => {
    for (const el of [...marks, ...(mid ? [mid] : [])]) { if (fade) fadeOut(el, DIM_MS); else el.remove() }
    marks = []; mid = null
  }
  return {
    /** Mostra la sagoma su `b` con l'etichetta; se c'era già, scivola da dov'è adesso (anche a metà di una scivolata). */
    show(b: Box, text: string) {
      if (typeof document === 'undefined' || !b.w || !b.h) return
      if (!box) {
        dim = document.createElement('div'); dim.className = 'master-ghost-dim'; dim.setAttribute('aria-hidden', 'true')
        box = document.createElement('div'); box.className = 'master-ghost'; box.setAttribute('aria-hidden', 'true')
        // Il suo nome tiene la sagoma sopra la fotografia della pagina mentre la master arriva al rilascio.
        box.style.viewTransitionName = 'master-ghost'
        tag = document.createElement('span'); box.append(tag)
        Object.assign(box.style, css(b))
        document.body.append(dim, box)
        if (!reduced()) { dim.animate([{ opacity: 0 }, { opacity: 1 }], { duration: DIM_MS, easing: 'ease-out' }); box.animate([{ opacity: 0 }, { opacity: 1 }], { duration: DIM_MS, easing: 'ease-out' }) }
      } else if (at && !same(at, b)) {
        const r = box.getBoundingClientRect()
        for (const a of box.getAnimations()) a.cancel()
        Object.assign(box.style, css(b))
        if (!reduced()) box.animate([css({ x: r.left, y: r.top, w: r.width, h: r.height }), css(b)], { duration: SLIDE_MS, easing: MOVE_EASE })
      }
      tag!.textContent = text
      at = b
    },
    /**
     * Le altre destinazioni possibili (Franz 04/11 18:14: «dovrei vedere le due possibilità, altrimenti l'utente non sa dove può
     * rilasciare»): un contorno tratteggiato giallo leggero con la sua etichetta, senza riempimento, per ogni posto che non è
     * quello della sagoma. Si ridisegnano a ogni cambio: il contorno torna sul posto lasciato.
     */
    hint(places: { b: Box; text: string }[]) {
      if (typeof document === 'undefined') return
      while (marks.length > places.length) marks.pop()!.remove()
      places.forEach((p, i) => {
        let el = marks[i]
        if (!el) {
          el = document.createElement('div'); el.className = 'master-ghost-hint'; el.setAttribute('aria-hidden', 'true'); el.append(document.createElement('span'))
          document.body.append(el); marks.push(el)
          if (!reduced()) el.animate([{ opacity: 0 }, { opacity: 1 }], { duration: DIM_MS, easing: 'ease-out' })
        }
        Object.assign(el.style, css(p.b))
        el.firstElementChild!.textContent = p.text
      })
    },
    /** La linea verticale a metà schermo, oltre la quale la master va dall'altra parte; `lit`: il dito l'ha passata. */
    midline(x: number | null, lit = false) {
      if (typeof document === 'undefined') return
      if (x == null) { mid?.remove(); mid = null; return }
      if (!mid) { mid = document.createElement('div'); mid.className = 'master-ghost-mid'; mid.setAttribute('aria-hidden', 'true'); document.body.append(mid) }
      mid.style.left = `${x}px`
      mid.classList.toggle('lit', lit)
    },
    /** Il rilascio: lo sfondo torna subito, la sagoma resta finché la master non è arrivata (`arrived`), poi sparisce. */
    land(arrived: Promise<unknown>) {
      const d = dim, g = box
      reset(); clearHints(true)
      if (d) fadeOut(d, DIM_MS)
      // La master arriva in MOVE_MS: se la transizione non lo dice, la sagoma sparisce comunque poco dopo.
      if (g) void Promise.race([arrived.catch(() => {}), new Promise(r => setTimeout(r, MOVE_MS + 200))]).then(() => fadeOut(g, FADE_MS))
    },
    /** L'annullo: sfondo e sagoma spariscono subito, la master non si è mossa. */
    drop() {
      const d = dim, g = box
      reset(); clearHints(true)
      if (d) fadeOut(d, DIM_MS)
      if (g) fadeOut(g, DIM_MS)
    },
  }
}
