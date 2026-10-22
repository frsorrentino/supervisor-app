import type { Attention, Event, Session } from './contract'
import { stripEmoji } from './emoji'

// Gli eventi che chiedono una riga o un tasto, porta di AlertEvents.kt. Contratto 1.57: il controllo di un task allo stop,
// verde o rosso. Contratto 1.58: quota finita, con le strade che il relay propone.

const title = (e: Event) => e.preview?.title ?? e.title
/** 1.78: l'esito sta in `ok`; con un relay precedente lo dice il «✓» in testa al titolo grezzo (1.57). */
export const green = (e: Event) => e.ok ?? title(e).trimStart().startsWith('✓')

/**
 * Un evento come l'app lo tiene (Franz, 22/10: niente emoji): titolo, corpo e anteprima senza simboli; su un `task_check`
 * di un relay prima dell'1.78 l'esito letto dal «✓» del titolo finisce in `ok`, perché il titolo pulito non lo dice più.
 */
export function cleanEvent(e: Event): Event {
  const ok = e.kind === 'task_check' ? green(e) : e.ok
  return {
    ...e, ok, title: stripEmoji(e.title), body: e.body === undefined ? undefined : stripEmoji(e.body),
    preview: e.preview ? { title: stripEmoji(e.preview.title), body: e.preview.body === undefined ? undefined : stripEmoji(e.preview.body) } : e.preview,
  }
}

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

/**
 * Chiuso dopo l'evento (16/10: turn_started è null a turno finito, e da solo riapriva la riga): un turno partito, un esito
 * arrivato, la voce uscita da `state.attention` (passata solo quando lo stato c'è), `settled_at` del relay 0.8.30.
 */
function closed(e: Event, s: Session, attention?: Attention[] | null): boolean {
  if (e.settled_at != null || (s.turn_started ?? 0) > e.ts || (s.outcome?.at ?? 0) > e.ts) return true
  return attention !== undefined && !(attention ?? []).some(a => a.kind === e.kind && a.ref === s.name)
}

/** Il quota_limit ancora da decidere: l'ultimo, fino al reset (5 ore senza `reset_at`) e finché non è chiuso. */
export function openLimit(events: Event[], s: Session, nowS: number, attention?: Attention[] | null): Event | null {
  const e = events.filter(x => x.kind === 'quota_limit' && x.ref === s.name).sort((a, b) => b.ts - a.ts)[0]
  if (!e) return null
  return (e.reset_at ?? e.ts + 5 * 3600) > nowS && !closed(e, s, attention) && s.state !== 'gone' ? e : null
}

/** Contratto 1.61: il turno fallito ancora da riprendere, l'ultimo, finché non è chiuso e per al massimo 6 ore. */
export function openFailure(events: Event[], s: Session, nowS: number, attention?: Attention[] | null): Event | null {
  const e = events.filter(x => x.kind === 'turn_failed' && x.ref === s.name).sort((a, b) => b.ts - a.ts)[0]
  if (!e?.continue_text?.trim()) return null
  return nowS - e.ts < 6 * 3600 && !closed(e, s, attention) && s.state !== 'gone' && s.state !== 'busy' ? e : null
}
