import type { Commitment, KpiData, KpiTurn, TimelinePage } from './contract'
import { turnsSummary } from './kpi'

// Porta di SessionK.kt (app-scheda-sessione-semplice-19-11, scelta K di Franz il 19/11 alle 18:42): la scheda aperta di una sessione
// dall'alto in ordine di tempo — Prima, Risultati, Adesso, Prossimo — col tasto pieno in fondo.
export type KTurn = { start: number; minutes: number; ok: boolean; text: string | null }
export type KBefore = { turns: KTurn[]; hidden: number; total: number; minutes: number; failed: number }
export const SHOWN = 3

/** Prima: gli ultimi turni dal più vecchio al più nuovo, quanti ne restano sopra, il totale e i minuti di oggi. */
export function before(turns: KpiTurn[] | null | undefined, all = false): KBefore | null {
  const t = [...(turns ?? [])].sort((a, b) => a.start - b.start)
  if (!t.length) return null
  const shown = all ? t : t.slice(-SHOWN)
  const sum = turnsSummary(t)
  return {
    turns: shown.map(x => ({ start: x.start, minutes: Math.max(1, Math.floor((Math.max(0, x.end - x.start) + 30) / 60)), ok: x.outcome !== 'fail', text: x.text?.trim() || null })),
    hidden: t.length - shown.length, total: t.length, minutes: sum.minutes, failed: sum.failed,
  }
}

const CLOSED = new Set(['done', 'closed', 'dropped', 'abandoned'])
/** Il compito del registro su cui lavora la sessione: il più recente fra quelli aperti col suo nome. */
export function task(commitments: Commitment[] | null | undefined, session: string): Commitment | null {
  const l = (commitments ?? []).filter(c => c.session === session && !CLOSED.has(c.state))
  return l.length ? l.reduce((a, b) => ((b.idle_s ?? 0) < (a.idle_s ?? 0) ? b : a)) : null
}

/** Il prossimo passo: quello del registro, altrimenti il primo dei «Prossimi:» della sessione. */
export const nextStep = (c: Commitment | null, steps: string[]): string | null => c?.next?.trim() || steps[0]?.trim() || null

/**
 * La pagina Utilizzo dopo la K: barra della giornata, attività per ora, token per turno e «oggi Y%» per ogni sessione che ha
 * lavorato oggi, la più recente in cima. Una sessione conta se ha un evento da [from] o un turno di oggi.
 */
export function todaySessions(page: TimelinePage | null | undefined, data: KpiData | null | undefined, from: number): string[] {
  const last = new Map<string, number>()
  const seen = (name: string, at: number) => last.set(name, Math.max(last.get(name) ?? 0, at))
  for (const s of page?.sessions ?? []) for (const e of s.events) if (e.at >= from) seen(s.session, e.at)
  for (const x of data?.kpi_turns_today ?? []) for (const tu of x.turns) seen(x.session, tu.end)
  return [...last.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).map(([n]) => n)
}
