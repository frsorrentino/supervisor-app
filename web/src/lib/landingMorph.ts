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

/** Le durate del segno d'arrivo: lo spostamento come la master, poi acceso e la sfumatura. */
export const MOVE_MS = 380, HOLD_MS = 320, FADE_MS = 300
export const MOVE_EASE = 'cubic-bezier(0.23, 1, 0.32, 1)'

export function landOn(from: Box, to: Box, reduced: boolean): void {
  if (reduced || typeof document === 'undefined' || !to.w || !to.h) return
  const el = document.createElement('div')
  el.className = 'land-morph'
  el.setAttribute('aria-hidden', 'true')
  document.body.appendChild(el)
  const at = (b: Box) => ({ left: `${b.x}px`, top: `${b.y}px`, width: `${b.w}px`, height: `${b.h}px` })
  // Sincronizzato con la master (app-trascina-rifinitura-04-11): arriva al posto in MOVE_MS con la curva del gruppo
  // `master-morph` (v3.css), poi resta acceso HOLD_MS perché si veda, e sfuma in FADE_MS.
  const total = MOVE_MS + HOLD_MS + FADE_MS
  const a = el.animate(
    [
      { ...at(from), opacity: 1, borderRadius: '2px', easing: MOVE_EASE },
      { ...at(to), opacity: 1, borderRadius: '14px', offset: MOVE_MS / total },
      { ...at(to), opacity: 1, borderRadius: '14px', offset: (MOVE_MS + HOLD_MS) / total, easing: 'ease-out' },
      { ...at(to), opacity: 0, borderRadius: '14px' },
    ],
    { duration: total, fill: 'forwards' },
  )
  a.onfinish = a.oncancel = () => el.remove()
}
