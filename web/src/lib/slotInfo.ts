// Cosa mostra una voce agganciata nello slot del tabellone (Franz 01/11 17:44, compito app-slot-info-01-11), porta di SlotInfo.kt.
// Chiuse non è qui: il relay non dice quando una sessione chiusa dal PC si è chiusa (`since` e `closed.at` ripiegano su `visto_ts`),
// quindi resta il numero e nessuna ora.
import type { HostStatus, Session, State, Suggestion, SystemStatus } from './contract'
import type { InProgress } from './lavori'

/** `ramPct` null = la macchina è spenta e lo si dice al posto della percentuale. */
export type HostRam = { name: string; ramPct: number | null }
/** `sessions` = le attive come «N aperte» in testa alla home: la master non conta. `pct` può passare 100 (più carico dei core); la barra si ferma a `barPct`. */
export type SystemInfo = { pct: number; barPct: number; level: string; ramPct: number; hosts: HostRam[]; sessions: number }

export function systemInfo(sys: SystemStatus, hosts: HostStatus[], sessions: Session[]): SystemInfo {
  const pct = Math.max(0, Math.round(sys.load_per_core * 100))
  const used = (1 - sys.mem_available_mb / Math.max(1, sys.mem_total_mb)) * 100
  return {
    pct, barPct: Math.min(100, pct), level: sys.load_level, ramPct: Math.min(100, Math.max(0, Math.round(used))),
    hosts: hosts.flatMap((h): HostRam[] => {
      if (!h.up) return [{ name: h.name, ramPct: null }]
      return (h.ram_total ?? 0) > 0 && h.ram_used != null ? [{ name: h.name, ramPct: Math.min(100, Math.max(0, Math.round(h.ram_used / h.ram_total! * 100))) }] : []
    }),
    sessions: sessions.filter(s => s.state !== 'gone' && s.name !== 'master').length,
  }
}

/** I numeri sono quelli della sezione Lavori (`inProgress`); `last` è l'ultimo che aspetta Franz. */
export type LavoriInfo = { you: number; total: number; youPct: number; last: string | null; lastSince: number | null }

export function lavoriInfo(ip: InProgress): LavoriInfo {
  const you = ip.you.reduce((n, l) => n + l.works.length, 0)
  const last = ip.you.reduce<InProgress['you'][number] | null>((a, l) => (!a || (l.since ?? 0) > (a.since ?? 0) ? l : a), null)
  return { you, total: ip.total, youPct: ip.total > 0 ? Math.round(you * 100 / ip.total) : 0, last: last?.short ?? null, lastSince: last?.since ?? null }
}

export const NAME_MAX = 30
const NAME_SEP = ' · '

/**
 * I nomi sotto i Suggerimenti agganciati (Franz 01/11 20:02, «Solo i nomi»): la parte prima dei primi due punti se è lunga al massimo
 * `NAME_MAX`, altrimenti l'etichetta del tipo. Nell'ordine della card, una volta sola.
 */
export function suggestionNames(all: Suggestion[], label: (kind: string) => string): string[] {
  return [...new Set(all.map(s => { const i = s.text.indexOf(':'), n = i < 0 ? '' : s.text.slice(0, i).trim(); return n && n.length <= NAME_MAX ? n : label(s.kind) }))]
}

/** Nomi interi finché stanno in `width`, poi «+N» coi nomi non mostrati; se non ci sta neanche il primo, solo «+N». */
export function fitNames(names: string[], width: number, measure: (x: string) => number): string | null {
  if (!names.length) return null
  for (let k = names.length; k >= 1; k--) {
    const rest = names.length - k
    const line = names.slice(0, k).join(NAME_SEP) + (rest > 0 ? `${NAME_SEP}+${rest}` : '')
    if (measure(line) <= width) return line
  }
  return `+${names.length}`
}

/** Sotto Plugin e skill: i nomi da aggiornare, oppure quando è stato il controllo. */
export type PluginsInfo = { names: string[]; checkedAt: number | null }

export function pluginsInfo(s: NonNullable<State['plugins']>): PluginsInfo {
  const names = [...new Set(s.accounts.flatMap(a => a.updates).map(u => u.split('@')[0]))]
  return { names, checkedAt: names.length ? null : s.checked_at ?? null }
}
