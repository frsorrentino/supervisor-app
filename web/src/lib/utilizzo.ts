import { isPersonalQuota } from './briefCards'
import { shown } from './quotaLine'
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

/**
 * Il dato delle 5 ore che vale ancora: una lettura vecchia a finestra finita non dice più niente, e l'account conta come senza
 * 5 ore. Una lettura fresca resta com'è. Stessa regola di UtilizzoRules.fiveHourLive nell'app.
 */
export function fiveHourLive(h5: number | null | undefined, resetH5: number | null | undefined, stale: boolean | undefined, now: number): number | null {
  if (h5 == null) return null
  return stale && (resetH5 == null || resetH5 <= now) ? null : h5
}

/**
 * La settimana nello slot: l'account più carico fra quelli da mostrare. `alarm` (Franz 01/11 14:25): lettura fresca e settimana dal
 * 75%, o da 10 punti sopra il ritmo; una lettura vecchia non lo fa scattare. `many`: con più account la nota dice quale.
 */
export type WeekSlot = { account: string; personal: boolean; pct: number; resetAt: number | null; delta: number | null; day: number | null; stale: boolean; alarm: boolean; many: boolean }
type QuotaLike = { w7?: number | null; reset_w7?: number | null; stale?: boolean; kind?: string | null }
export function weekSlot(quota: Record<string, QuotaLike>, now: number): WeekSlot | null {
  const list = shown(Object.entries(quota)).filter(([, q]) => q.w7 != null)
  if (!list.length) return null
  const [account, q] = list.reduce((a, b) => ((b[1].w7 ?? 0) > (a[1].w7 ?? 0) ? b : a))
  const resetAt = q.reset_w7 != null && q.reset_w7 > now ? q.reset_w7 : null
  const pct = Math.max(0, Math.min(100, q.w7 ?? 0))
  const delta = weekDelta(q.w7, resetAt, now)
  const stale = !!q.stale
  return { account, personal: isPersonalQuota(account, q as never), pct, resetAt, delta, day: weekDay(resetAt, now), stale, alarm: !stale && (pct >= 75 || (delta ?? 0) >= 10), many: list.length > 1 }
}

/**
 * app-utilizzo-una-barra-03-11 (Franz 03/11 13:11, «Righe nel tabellone»), porta di UtilizzoRules.usageAlarm: la barra fissa delle 5 ore
 * lascia il tabellone; una finestra vicina al limite ci entra da sola in ambra. Le 5 ore dal 75% (lettura fresca), la settimana con
 * l'allarme dello slot; con tutte e due la più carica, a pari le 5 ore, che finiscono prima. Null senza allarme.
 */
export type UsageAlarm = { account: string; week: boolean; pct: number; resetAt: number | null }
export function usageAlarm(quota: Record<string, QuotaLike & { h5?: number | null; reset_h5?: number | null }>, now: number): UsageAlarm | null {
  const h5 = Object.entries(quota).filter(([, q]) => !q.stale).map(([account, q]) => {
    const live = fiveHourLive(q.h5, q.reset_h5, false, now)
    const pct = live == null ? -1 : Math.max(0, Math.min(100, live))
    return pct >= 75 ? { account, week: false, pct, resetAt: q.reset_h5 ?? null } : null
  }).filter((x): x is UsageAlarm => !!x).reduce<UsageAlarm | null>((a, b) => (!a || b.pct > a.pct ? b : a), null)
  const w = weekSlot(quota, now)
  const wk: UsageAlarm | null = w?.alarm ? { account: w.account, week: true, pct: w.pct, resetAt: w.resetAt } : null
  if (!h5) return wk
  if (!wk) return h5
  return wk.pct > h5.pct ? wk : h5
}

export type QuotaBar = { week: boolean; pct: number; resetAt: number | null; windowS: number; delta: number | null; day: number | null }

/**
 * quota-senza-cinque-ore: la barra di un account. Anthropic toglie il limite di 5 ore a tappe e per account: conta il dato
 * assente, mai il nome del piano. Senza `h5` la settimana, col giorno e il ritmo; senza nessuno dei due null, mai uno 0 %.
 */
export function quotaBar(h5: number | null | undefined, w7: number | null | undefined, resetH5: number | null | undefined, resetW7: number | null | undefined, now: number, stale = false): QuotaBar | null {
  const pc = (x: number) => Math.max(0, Math.min(100, x))
  const live = fiveHourLive(h5, resetH5, stale, now)
  if (live != null) return { week: false, pct: pc(live), resetAt: resetH5 ?? null, windowS: H5_S, delta: null, day: null }
  if (w7 == null) return null
  return { week: true, pct: pc(w7), resetAt: resetW7 ?? null, windowS: W7_S, delta: weekDelta(w7, resetW7, now), day: weekDay(resetW7, now) }
}

/** Il ritmo in parole: «12 punti sopra il ritmo pari», col giorno davanti se lo si passa. */
export function paceText(delta: number | null, day: number | null = null): string | null {
  if (delta == null) return null
  const p = delta < 0 ? t.usageBelowPace(-delta) : delta > 0 ? t.usageAbovePace(delta) : t.weekEven
  return day != null ? t.weekDayPace(day, p) : p
}
