import type { State } from './contract'
import { stop } from './strongAlert'

// L'avviso delle altre sessioni nella chat di una sessione, porta di Elsewhere.kt: prima chi ti aspetta, poi un turno
// finito, una volta sola e solo se recente, delle sessioni che segui o a cui hai scritto. Mai la sessione aperta.
export type Alert = { type: 'waiting'; sessions: string[] } | { type: 'finished'; session: string; at: number }

export const key = (a: Alert): string => (a.type === 'waiting' ? 'w:' + a.sessions.join(',') : `f:${a.session}@${a.at}`)

/** Oltre questo tempo un esito non è più una novità. */
export const FRESH_S = 600

export function alert(state: State, current: string, now: number, mine: Set<string>, seen: Set<string>): Alert | null {
  const others = state.sessions.filter(s => s.name !== current && s.state !== 'gone')
  // Contratto 1.68: chi è ferma per Franz conta come una domanda.
  const waiting = others.flatMap(s => { const at = s.question?.asked_at ?? stop(s)?.at; return at == null ? [] : [{ n: s.name, at }] })
    .sort((a, b) => a.at - b.at).map(x => x.n)
  if (waiting.length) return { type: 'waiting', sessions: waiting }
  return others.filter(s => s.state === 'idle' && (s.followed || mine.has(s.name)))
    .flatMap(s => (s.outcome && now - s.outcome.at <= FRESH_S ? [{ type: 'finished' as const, session: s.name, at: s.outcome.at }] : []))
    .filter(a => !seen.has(key(a)))
    .reduce<Alert | null>((m, a) => (m == null || a.at > (m as { at: number }).at ? a : m), null)
}
