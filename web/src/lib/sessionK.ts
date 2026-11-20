import type { Commitment, KpiData, KpiResult, KpiTurn, TimelinePage } from './contract'
import { turnsSummary } from './kpi'

// Porta di SessionK.kt (app-scheda-sessione-semplice-19-11, scelta K di Franz il 19/11 alle 18:42): la scheda aperta di una sessione
// dall'alto in ordine di tempo — Prima, Risultati, Adesso, Prossimo — col tasto pieno in fondo.
export type KTurn = { start: number; minutes: number; ok: boolean; text: string | null; entry: string | null }
export type KBefore = { turns: KTurn[]; hidden: number; total: number; minutes: number; failed: number }
export const SHOWN = 3

/** Prima: gli ultimi turni dal più vecchio al più nuovo, quanti ne restano sopra, il totale e i minuti di oggi. */
export function before(turns: KpiTurn[] | null | undefined, all = false): KBefore | null {
  const t = [...(turns ?? [])].sort((a, b) => a.start - b.start)
  if (!t.length) return null
  const shown = all ? t : t.slice(-SHOWN)
  const sum = turnsSummary(t)
  return {
    turns: shown.map(x => ({ start: x.start, minutes: Math.max(1, Math.floor((Math.max(0, x.end - x.start) + 30) / 60)), ok: x.outcome !== 'fail', text: x.text?.trim() || null, entry: x.entry?.trim() || null })),
    hidden: t.length - shown.length, total: t.length, minutes: sum.minutes, failed: sum.failed,
  }
}

/** Contratto 1.156: un risultato di oggi da mostrare — il tocco apre il link, o la voce della chat che ha mandato il file. */
export type KResult = { kind: 'image' | 'file' | 'link' | 'release'; label: string; url: string | null; entry: string | null; at: number }
export type KResults = { shown: KResult[]; hidden: number; total: number }
export const RESULTS_SHOWN = 4
const KINDS = new Set(['image', 'file', 'link', 'release'])

/** Risultati: i più nuovi a sinistra; quelli senza nome o che non si possono aprire restano fuori; null quando non ce n'è. */
export function results(list: KpiResult[] | null | undefined, all = false): KResults | null {
  const l = (list ?? []).flatMap((x): KResult[] => {
    const label = x.label?.trim(), url = /^https?:\/\//.test(x.url ?? '') ? x.url! : null, entry = x.entry?.trim() || null
    if (!label || !KINDS.has(x.kind)) return []
    const kind = x.kind as KResult['kind']
    if ((kind === 'link' || kind === 'release') ? !url : !entry) return []
    return [{ kind, label, url: kind === 'link' || kind === 'release' ? url : null, entry, at: x.at }]
  }).sort((a, b) => b.at - a.at).slice(0, 20)
  if (!l.length) return null
  const shown = all || l.length <= RESULTS_SHOWN + 1 ? l : l.slice(0, RESULTS_SHOWN)
  return { shown, hidden: l.length - shown.length, total: l.length }
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
