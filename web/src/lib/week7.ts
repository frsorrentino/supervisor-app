// La settimana ora per ora (proposte 2A e 2B del 25/10, contratto 1.86), porta di Week7.kt: la linea cumulata contro il
// ritmo pari, la proiezione al ritmo delle ultime 48 ore, la spesa di ogni giorno contro il budget di 100/7. Una voce di
// `steps` vale dall'ora in cui è letta fino alla voce dopo; prima della prima voce si sa solo lo 0 del rinnovo. Un giorno
// senza il valore del suo inizio non ha barra (niente numeri inventati).
import type { WeekPoints } from './contract'

export const WEEK_S = 7 * 86400
export const HOURS = 168
/** Il budget di un giorno: la settimana in parti uguali. */
export const BUDGET = 100 / 7
/** Sotto le 12 ore di campioni il ritmo delle ultime 48 ore è rumore: si usa la media della settimana. */
const MIN_SPAN_H = 12

/** `points` la linea a gradini fino ad adesso (x da 0 a 1); `fullAt` l'ora del 100% prima del rinnovo; `projected` al rinnovo, o 100. */
export type WeekChart = { start: number; resetAt: number; pct: number; points: [number, number][]; nowX: number; fullAt: number | null; projected: number | null }
/** Un giorno di 24 ore dal rinnovo: `spent` null se non si sa (prima dei campioni, o `partial` quando partono a metà). */
export type WeekDay = { index: number; start: number; spent: number | null; partial: boolean; today: boolean; future: boolean }

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v))
const stepsOf = (p: WeekPoints): [number, number][] => p.steps.filter(s => s.length >= 2).map(s => [s[0], s[1]] as [number, number]).sort((a, b) => a[0] - b[0])

/** Il valore prima dell'ora `h`: l'ultima voce letta in un'ora precedente; null se prima ci sono solo ore senza campioni. */
function before(st: [number, number][], h: number): number | null {
  if (h <= 0) return 0
  if (!st.length) return null
  const first = st[0][0]
  if (first > 0 && h <= first) return null
  let v = 0
  for (const [sh, sv] of st) if (sh < h) v = sv
  return v
}

/** Punti l'ora: dalle ultime 48 ore (o dalla prima voce, se più tarda) con almeno 12 ore, altrimenti la media della settimana. */
function rate(st: [number, number][], pct: number, nowH: number): number | null {
  const from = nowH - 48
  const base: [number, number] | null = !st.length ? null : st[0][0] >= from ? st[0] : [from, before(st, from + 1) ?? st[0][1]]
  if (base && nowH - base[0] >= MIN_SPAN_H) {
    const r = (pct - base[1]) / (nowH - base[0])
    return r > 0 ? r : null
  }
  if (nowH < 1 || pct <= 0) return null
  return pct / nowH
}

export function weekChart(p: WeekPoints, pct: number, resetAt: number, now: number): WeekChart {
  const st = stepsOf(p)
  const nowH = clamp(Math.trunc((now - p.start) / 3600), 0, HOURS)
  const nowX = clamp((now - p.start) / WEEK_S, 0, 1)
  const points: [number, number][] = []
  st.forEach(([h, v], i) => {
    if (i > 0) points.push([h / HOURS, st[i - 1][1]])
    points.push([h / HOURS, v])
  })
  if (points.length) points.push([nowX, pct])
  const r = rate(st, pct, nowH)
  const left = Math.max(0, resetAt - now)
  const at = r == null || pct >= 100 ? null : now + Math.ceil(((100 - pct) / r) * 3600)
  const fullAt = at != null && at < resetAt ? at : null
  const projected = pct >= 100 ? 100 : r == null ? null : fullAt != null ? 100 : clamp(Math.trunc(pct + (r * left) / 3600), 0, 100)
  return { start: p.start, resetAt, pct, points, nowX, fullAt, projected }
}

export function weekDays(p: WeekPoints, pct: number, now: number): WeekDay[] {
  const st = stepsOf(p)
  const nowH = Math.trunc((now - p.start) / 3600)
  const first = st.length ? st[0][0] : null
  return Array.from({ length: 7 }, (_, i) => {
    const from = i * 24, to = from + 24
    const today = nowH >= from && nowH < to
    const future = nowH < from
    const partial = first != null && first > 0 && first > from && first < to
    const startV = before(st, from)
    const endV = today ? pct : before(st, to)
    const spent = future || startV == null || endV == null ? null : Math.max(0, endV - startV)
    return { index: i, start: p.start + from * 3600, spent, partial, today, future }
  })
}

/** Da quando ci sono i campioni, se partono dopo il rinnovo; null se coprono la settimana. */
export function samplesFrom(p: WeekPoints): number | null {
  const st = stepsOf(p)
  return st.length && st[0][0] > 0 ? p.start + st[0][0] * 3600 : null
}
