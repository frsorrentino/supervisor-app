import type { UsageToday } from './contract'

// «Oggi · quota per sessione» (Franz 24/10 19:46, B5 del mockup): al posto degli eventi per ora, quanta parte della giornata
// dell'account ha usato ogni sessione. Le voci sotto il 3 % stanno insieme in «altre N», se sono almeno due. Stessa regola di UsageTodayRules.kt.
export const SMALL = 0.03
export type UsageRow = { session: string; pct: number }
export type UsageRows = { rows: UsageRow[]; others: { n: number; pct: number } | null }

export function usageRows(list: UsageToday[] | null | undefined): UsageRows | null {
  if (!list?.length) return null
  const sorted = [...list].sort((a, b) => b.share - a.share)
  // Una sola voce piccola resta col suo nome: «altre 1» non dice niente (prova dal vivo del 24/10, 21:24).
  const few = sorted.filter(u => u.share < SMALL).length < 2
  const big = sorted.filter(u => few || u.share >= SMALL), small = few ? [] : sorted.filter(u => u.share < SMALL)
  const pct = (x: number) => Math.round(x * 100)
  return {
    rows: big.map(u => ({ session: u.session, pct: pct(u.share) })),
    others: small.length ? { n: small.length, pct: pct(small.reduce((n, u) => n + u.share, 0)) } : null,
  }
}
