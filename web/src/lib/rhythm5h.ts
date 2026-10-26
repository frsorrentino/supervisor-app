// Il ritmo delle cinque ore (proposte 1A e 1B del 25/10), porta di Rhythm5h.kt: quanto della finestra è passato contro
// quanta quota è usata, e la proiezione al ritmo dell'ultima ora fino al 100% o al reset. Senza un ritmo misurato la
// proiezione non c'è.
import { WINDOW_S, window, type Sample } from './quotaHistory'

/** Sotto i 10 minuti fra due campioni il ritmo è rumore. */
const MIN_SPAN_S = 600
const HOUR_S = 3600

/**
 * `elapsed` e `ahead` in punti; `points` la linea della finestra (x da 0 a 1); `fullAt` l'ora del 100% se cade prima del
 * reset, `idleMin` i minuti fermi fra il 100% e il reset; `projected` dove si arriva al reset, o 100.
 */
export type Rhythm = {
  start: number; resetAt: number; pct: number; elapsed: number; ahead: number
  points: [number, number][]; nowX: number; fullAt: number | null; idleMin: number | null; projected: number | null
}

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v))

export function rhythm(samples: Sample[], pct: number, resetAt: number, now: number): Rhythm {
  const start = resetAt - WINDOW_S
  const x = (t: number) => clamp((t - start) / WINDOW_S, 0, 1)
  const elapsed = clamp(Math.trunc(((now - start) * 100) / WINDOW_S), 0, 100)
  const points = window(samples, resetAt)
  const r = rate(points)
  const left = Math.max(0, resetAt - now)
  const at = r == null || pct >= 100 ? null : now + Math.ceil((100 - pct) / r)
  const fullAt = at != null && at < resetAt ? at : null
  const projected = pct >= 100 ? 100 : r == null ? null : fullAt != null ? 100 : clamp(Math.trunc(pct + r * left), 0, 100)
  return {
    start, resetAt, pct, elapsed, ahead: pct - elapsed, points: points.map(s => [x(s.ts), s.pct]), nowX: x(now),
    fullAt, idleMin: fullAt == null ? null : Math.trunc((resetAt - fullAt) / 60), projected,
  }
}

/** Punti al secondo dal primo campione nell'ora prima dell'ultimo fino all'ultimo; null senza salita misurabile. */
function rate(points: Sample[]): number | null {
  const last = points[points.length - 1]
  if (!last) return null
  const base = points.find(s => s.ts >= last.ts - HOUR_S)!
  const span = last.ts - base.ts
  if (span < MIN_SPAN_S || last.pct <= base.pct) return null
  return (last.pct - base.pct) / span
}
