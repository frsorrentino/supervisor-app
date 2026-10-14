import type { Event, Session } from './contract'

// Gli eventi che chiedono una riga o un tasto, porta di AlertEvents.kt. Contratto 1.57: il controllo di un task allo stop,
// verde o rosso. Contratto 1.58: quota finita, con le strade che il relay propone.

const title = (e: Event) => e.preview?.title ?? e.title
/** 1.57: «✓ <task>: check green» o «✗ <task>: check red». */
export const green = (e: Event) => title(e).trimStart().startsWith('✓')

export type Words = { otherAccount: (a: string) => string; atReset: (t: string) => string; pass: string }
/** Una strada: la parola del tasto e l'`arg` di `quota_route`; null per «pass», dove la sessione la sceglie l'utente. */
export type Way = { action: string; label: string; arg: string | null }

export function ways(e: Event, w: Words, hhmm: (s: number) => string): Way[] {
  return (e.actions ?? []).flatMap((a): Way[] => {
    if (a === 'other_account') return e.other_account ? [{ action: a, label: w.otherAccount(e.other_account), arg: a }] : []
    if (a === 'at_reset') return e.reset_at != null ? [{ action: a, label: w.atReset(hhmm(e.reset_at)), arg: a }] : []
    if (a === 'pass') return [{ action: a, label: w.pass, arg: null }]
    return []
  })
}

export const passArg = (session: string) => `pass:${session}`
export const passTargets = (sessions: Session[], stopped: string) => sessions.filter(s => s.state !== 'gone' && s.name !== stopped).map(s => s.name)

/** Il quota_limit ancora da decidere: l'ultimo, fino al reset (5 ore senza `reset_at`) e finché la sessione non riprende. */
export function openLimit(events: Event[], s: Session, nowS: number): Event | null {
  const e = events.filter(x => x.kind === 'quota_limit' && x.ref === s.name).sort((a, b) => b.ts - a.ts)[0]
  if (!e) return null
  return (e.reset_at ?? e.ts + 5 * 3600) > nowS && (s.turn_started ?? 0) <= e.ts && s.state !== 'gone' ? e : null
}
