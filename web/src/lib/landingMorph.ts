// Il segno d'arrivo diventa il posto (app-trascina-morphing-03-11, punto 2): al rilascio il segno giallo si allarga fino al
// contorno del posto della master, poi svanisce. Un elemento suo, animato a mano: con le View Transitions il segno e il posto
// avrebbero lo stesso nome nella stessa fotografia.
export type Box = { x: number; y: number; w: number; h: number }
export type LandMark = { x: number; y: number; v: boolean }

/** Il rettangolo del segno d'arrivo com'è a terra: 4 px di spessore, lungo come un segno acceso (48 + 0,6 × 88). */
export function markBox(m: LandMark): Box {
  const len = 48 + 0.6 * 88
  return m.v ? { x: m.x - 2, y: m.y - len / 2, w: 4, h: len } : { x: m.x - len / 2, y: m.y - 2, w: len, h: 4 }
}

/** Il posto della master dopo il rilascio: il pannello sotto la home, il pannello di lato, quello della plancia, o la riga. */
export const PLACES = ['.dpanel.under', 'aside.dpanel:not(.stackcol)', 'aside.mpanel', '.mbar'] as const

export function landOn(from: Box, to: Box, reduced: boolean): void {
  if (reduced || typeof document === 'undefined' || !to.w || !to.h) return
  const el = document.createElement('div')
  el.className = 'land-morph'
  el.setAttribute('aria-hidden', 'true')
  document.body.appendChild(el)
  const at = (b: Box) => ({ left: `${b.x}px`, top: `${b.y}px`, width: `${b.w}px`, height: `${b.h}px` })
  const a = el.animate(
    [{ ...at(from), opacity: 1, borderRadius: '2px' }, { ...at(to), opacity: 1, borderRadius: '14px', offset: 0.65 }, { ...at(to), opacity: 0, borderRadius: '14px' }],
    { duration: 560, easing: 'cubic-bezier(.2, .8, .2, 1)', fill: 'forwards' },
  )
  a.onfinish = a.oncancel = () => el.remove()
}
