// Il blocco Utilizzo aperto (scelta D di Franz, 30/10 20:11, compito app-utilizzo-d-30-10; UtilizzoRules.kt nell'app).
export const H5_S = 5 * 3600
export const W7_S = 7 * 86400

/** Quanto è passato della finestra, da 0 a 100; null senza ora di ripartenza o a finestra finita. La tacca e «adesso» usano questo. */
export function elapsedPct(resetAt: number | null | undefined, windowS: number, now: number): number | null {
  if (resetAt == null || resetAt <= now) return null
  return Math.max(0, Math.min(100, (1 - (resetAt - now) / windowS) * 100))
}

/** Punti sopra (positivi) o sotto il ritmo pari della settimana; null senza dati. */
export function weekDelta(w7: number | null | undefined, resetAt: number | null | undefined, now: number): number | null {
  const e = elapsedPct(resetAt, W7_S, now)
  return e == null || w7 == null ? null : Math.round(w7 - e)
}

/** L'ora con più prompt e commit, null in una giornata vuota. */
export function peakHour(perHour: [number, number][], firstHour: number): number | null {
  let best = -1, top = 0
  perHour.forEach(([p, c], i) => { if (p + c > top) { top = p + c; best = i } })
  return best < 0 ? null : (firstHour + best) % 24
}

/** L'ultima mediana delle attese e quante chiuse in tutto; null senza attese chiuse. */
export function waitsLine(week: { median_min?: number | null; n?: number }[]): { medianMin: number; closed: number } | null {
  const last = [...week].reverse().find(d => d.median_min != null)
  return last ? { medianMin: Math.round(last.median_min!), closed: week.reduce((s, d) => s + (d.n ?? 0), 0) } : null
}
