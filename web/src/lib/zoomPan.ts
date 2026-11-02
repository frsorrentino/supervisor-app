// Il visore delle immagini, come ZoomPan.kt (app-chat-zoom-02-11): lo spostamento segue il dito 1:1, lo zoom resta attorno
// alle dita o al puntatore, l'immagine non esce dai bordi, il doppio tocco porta almeno a tutta larghezza o torna a 1x.
// Coordinate in pixel della vista, con l'immagine adattata (`cw`, `ch`) al centro e lo strato scalato attorno al centro.
export const MAX = 6
export const DOUBLE_TAP = 2.5

export type View = { scale: number; x: number; y: number }
export const IDENTITY: View = { scale: 1, x: 0, y: 0 }

/** Quanto lo strato può spostarsi su un asse prima che l'immagine lasci un bordo vuoto. */
export const limit = (content: number, viewport: number, scale: number) => Math.max(0, (content * scale - viewport) / 2)

export function clamp(v: View, cw: number, ch: number, vw: number, vh: number): View {
  const lx = limit(cw, vw, v.scale), ly = limit(ch, vh, v.scale)
  return { scale: v.scale, x: Math.min(lx, Math.max(-lx, v.x)), y: Math.min(ly, Math.max(-ly, v.y)) }
}

/** La misura che porta l'immagine a tutta larghezza; il pizzico arriva al triplo di questa, mai sotto MAX. */
export const fillWidth = (cw: number, vw: number) => (cw <= 0 ? 1 : Math.max(1, vw / cw))
export const maxScale = (cw: number, vw: number) => Math.max(MAX, 3 * fillWidth(cw, vw))

/** Un passo del gesto: `zoom` attorno a `cx, cy` (in pixel della vista), poi lo spostamento, 1:1. */
export function step(v: View, zoom: number, panX: number, panY: number, cx: number, cy: number, cw: number, ch: number, vw: number, vh: number, max = MAX): View {
  const s = Math.min(max, Math.max(1, v.scale * zoom))
  const k = s / v.scale
  const dx = cx - vw / 2, dy = cy - vh / 2
  return clamp({ scale: s, x: dx - (dx - v.x) * k + panX, y: dy - (dy - v.y) * k + panY }, cw, ch, vw, vh)
}

/** Il doppio tocco: da 1x ingrandisce sul punto, almeno fino a tutta larghezza; altrimenti torna a 1x. */
export const doubleTap = (v: View, tx: number, ty: number, cw: number, ch: number, vw: number, vh: number): View =>
  v.scale > 1.01 ? IDENTITY : step(IDENTITY, Math.max(DOUBLE_TAP, fillWidth(cw, vw)), 0, 0, tx, ty, cw, ch, vw, vh, maxScale(cw, vw))

/** La misura dell'immagine adattata alla vista (object-fit: contain). */
export function fit(iw: number, ih: number, vw: number, vh: number): [number, number] {
  if (iw <= 0 || ih <= 0) return [vw, vh]
  const r = Math.min(vw / iw, vh / ih)
  return [iw * r, ih * r]
}
