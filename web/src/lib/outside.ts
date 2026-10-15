import type { Night, Session } from './contract'
import type { ForYouRow, Kind } from './masterHome'

// «Fuori dalle sessioni», porta di OutsideSessions.kt (mockup A, scelto da Franz il 03/10 alle 22:34): sotto le sessioni le
// righe di servizio divise in categorie, dalla più urgente. Da sistemare (contesto alto), programmati, da riprendere (i
// prossimi passi del diario), stanotte (resoconto e coda), chiuse. Una categoria vuota non si mostra.
export type Category = 'fix' | 'scheduled' | 'resume' | 'night' | 'closed'
export type Group = { category: Category; rows: ForYouRow[]; closed: Session[] }

const CATEGORY: Partial<Record<Kind, Category>> = { context: 'fix', scheduled: 'scheduled', next_step: 'resume', night_report: 'night', night: 'night' }
const ORDER: Category[] = ['fix', 'scheduled', 'resume', 'night', 'closed']

export const count = (g: Group) => (g.category === 'closed' ? g.closed.length : g.rows.length)

export function groups(service: ForYouRow[], closed: Session[]): Group[] {
  return ORDER.flatMap((c): Group[] => {
    if (c === 'closed') return closed.length ? [{ category: c, rows: [], closed }] : []
    // Domande e turni finiti hanno le loro card fra le sessioni.
    const rows = service.filter(r => CATEGORY[r.kind] === c)
    // La notte: prima il resoconto (la mattina), poi la coda.
    if (c === 'night') rows.sort((a, b) => (a.kind === 'night_report' ? 0 : 1) - (b.kind === 'night_report' ? 0 : 1))
    return rows.length ? [{ category: c, rows, closed: [] }] : []
  })
}

/** La scheda Stanotte, porta di TonightCard.kt: quello in corso per primo con da quanti minuti, poi in ordine di arrivo. */
export type TonightItem = { id: string; name: string; prompt: string; runningMin: number | null }
export function tonight(night: Night, now: number): { items: TonightItem[]; running: number; queued: number } {
  const items = [...(night.items ?? [])].sort((a, b) => (a.started == null ? 1 : 0) - (b.started == null ? 1 : 0) || a.added - b.added)
    .map(i => ({ id: i.id, name: i.name, prompt: i.prompt.trim(), runningMin: i.started != null ? Math.max(0, Math.floor((now - i.started) / 60)) : null }))
  const running = items.filter(i => i.runningMin != null).length
  return { items, running, queued: items.length - running }
}
