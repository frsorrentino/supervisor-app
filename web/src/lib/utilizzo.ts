import { t } from './t'

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

/** Il giorno della settimana di quota, da 1 a 7; null senza rinnovo o a settimana finita. */
export function weekDay(resetAt: number | null | undefined, now: number): number | null {
  if (resetAt == null || resetAt <= now) return null
  return Math.max(1, Math.min(7, Math.floor((now - (resetAt - W7_S)) / 86400) + 1))
}

export type QuotaBar = { week: boolean; pct: number; resetAt: number | null; windowS: number; delta: number | null; day: number | null }

/**
 * quota-senza-cinque-ore: la barra di un account. Anthropic toglie il limite di 5 ore a tappe e per account: conta il dato
 * assente, mai il nome del piano. Senza `h5` la settimana, col giorno e il ritmo; senza nessuno dei due null, mai uno 0 %.
 */
export function quotaBar(h5: number | null | undefined, w7: number | null | undefined, resetH5: number | null | undefined, resetW7: number | null | undefined, now: number): QuotaBar | null {
  const pc = (x: number) => Math.max(0, Math.min(100, x))
  if (h5 != null) return { week: false, pct: pc(h5), resetAt: resetH5 ?? null, windowS: H5_S, delta: null, day: null }
  if (w7 == null) return null
  return { week: true, pct: pc(w7), resetAt: resetW7 ?? null, windowS: W7_S, delta: weekDelta(w7, resetW7, now), day: weekDay(resetW7, now) }
}

/** Il ritmo in parole: «12 punti sopra il ritmo pari», col giorno davanti se lo si passa. */
export function paceText(delta: number | null, day: number | null = null): string | null {
  if (delta == null) return null
  const p = delta < 0 ? t.usageBelowPace(-delta) : delta > 0 ? t.usageAbovePace(delta) : t.weekEven
  return day != null ? t.weekDayPace(day, p) : p
}
