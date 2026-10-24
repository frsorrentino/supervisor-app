import { workingText } from './masterHome'
import type { Approval, QuotaAccount, Session, State } from './contract'
import { DAY } from './durations'
import { stop } from './strongAlert'

// Il riepilogo unico della home, porta di Summary.kt: ogni sessione una volta, nell'ordine del bisogno (ti aspetta, ha
// finito, al lavoro, ferme), le chiuse a parte. La master sta nella lista solo con un ok o una richiesta per Franz (24/10
// 18:49); le sue domande e il resto stanno nella sua barra e nel campo di scrittura (21/10 21:15).
export const MASTER = 'master'
export const FINISHED_S = 12 * 3600
export type Group = 'waiting' | 'finished' | 'working' | 'still'
export type Row = {
  group: Group; session: Session; text: string | null; at: number | null; key?: string; quota?: QuotaAccount
  /** Gli ok del registro che chiede questa sessione, dal più vecchio, ognuno col suo Approva nella card. */
  approvals: Approval[]
}
/** Una riga prima degli ok e della quota, che `build` aggiunge alla fine. */
type Bare = Omit<Row, 'approvals' | 'quota'>
export type Model = { rows: Row[]; closed: Session[]; open: number; master: Session | null }
/** Un messaggio mandato da questo dispositivo, per sapere a chi abbiamo scritto. */
export type Sent = { session: string; sentAt: number }

export const finishedKey = (session: string, at: number) => `finished:${session}@${at}`

/**
 * Franz 23/10 19:34: «i tasti di riapprovazione starebbero meglio nelle schede della sessione»: gli ok del registro stanno
 * nella card della sessione che li chiede, non in schede a parte. L'impegno con lo stesso compito dice la sessione; senza
 * sessione vale il suo progetto. Una sessione chiusa non ha card: il suo ok resta fra le righe «Per te».
 */
export const approvalsOf = (state: State, s: Session): Approval[] =>
  (state.approvals ?? []).filter(a => {
    const c = state.commitments?.find(x => x.task === a.task)
    if (!c) return false
    return c.session === s.name || (c.session == null && !!c.project && !!s.project && (c.project.endsWith(s.project) || s.project.endsWith(c.project)))
  }).sort((a, b) => a.requested_at - b.requested_at)

/** I compiti i cui ok stanno già in una card: le righe «Per te» della casa non li ripetono. */
export const attachedTasks = (rows: Row[]): Set<string> => new Set(rows.flatMap(r => r.approvals.map(a => a.task)))

export function build(state: State, sent: Sent[], now: number, read: Set<string>): Model {
  const live = state.sessions.filter(s => s.state !== 'gone')
  const master = live.find(s => s.name === MASTER) ?? null
  const others = live.filter(s => s.name !== MASTER)
  // Contratto 1.68: chi è ferma per Franz conta come una domanda, col motivo; tutte dalla più vecchia.
  const waiting: Bare[] = others.flatMap((s): Bare[] => {
    if (s.question) return [{ group: 'waiting', session: s, text: s.question.text, at: s.question.asked_at }]
    const x = stop(s)
    return x ? [{ group: 'waiting', session: s, text: x.text, at: x.at }] : []
  }).sort((a, b) => a.at! - b.at!)
  const asking = new Set(waiting.map(r => r.session.name))
  // Chi ha finito resta finché non lo apri o non gli scrivi, per 12 ore; solo le seguite o quelle a cui hai scritto.
  const finished: Bare[] = others.filter(s => s.state === 'idle' && s.outcome && !asking.has(s.name))
    .filter(s => {
      const o = s.outcome!
      return now - o.at <= FINISHED_S && !read.has(finishedKey(s.name, o.at)) &&
        (s.followed || sent.some(x => x.session === s.name)) && !sent.some(x => x.session === s.name && x.sentAt > o.at)
    })
    .sort((a, b) => b.outcome!.at - a.outcome!.at)
    .map(s => ({ group: 'finished', session: s, text: s.outcome!.full, at: s.outcome!.at, key: finishedKey(s.name, s.outcome!.at) }))
  const working: Bare[] = others.filter(s => (s.state === 'busy' || s.state === 'awaiting') && !asking.has(s.name))
    .map(s => ({ group: 'working', session: s, text: workingText(s), at: s.turn_started ?? s.since }))
  // Franz 24/10 18:49 (ok-master-dove-24-10): la master ha una scheda come le altre, ma solo quando ha qualcosa per Franz,
  // un ok del registro (con o senza opzioni) o una richiesta ferma; il resto di quello che chiede sta nella sua barra.
  if (master) {
    const oks = approvalsOf(state, master), x = stop(master)
    if (oks.length || x) waiting.push({ group: 'waiting', session: master, text: x?.text ?? null, at: x?.at ?? oks[0].requested_at })
    waiting.sort((a, b) => a.at! - b.at!)
  }
  const taken = new Set([...waiting, ...finished, ...working].map(r => r.session.name))
  const still: Bare[] = others.filter(s => !taken.has(s.name)).sort((a, b) => b.since - a.since)
    .map(s => ({ group: 'still', session: s, text: s.outcome?.full ?? null, at: s.since }))
  return {
    rows: [...waiting, ...finished, ...working, ...still].map(r => ({ ...r, quota: state.quota[r.session.account], approvals: approvalsOf(state, r.session) })),
    closed: state.sessions.filter(s => s.state === 'gone'),
    open: others.length,
    master,
  }
}

/** «5 m», «3 h», «2 g» dall'istante `since` a `now` (epoch s). */
export function age(since: number, now: number): string {
  const s = Math.max(0, now - since)
  if (s < 3600) return `${Math.max(1, Math.round(s / 60))} m`
  if (s < 86400) return `${Math.round(s / 3600)} h`
  return `${Math.round(s / 86400)} ${DAY}`
}

/** Franz 24/10 19:46 (B2): l'esito di una sessione ferma più vecchio di 6 ore si spegne e dice da quante ore; null se è fresco. */
export const STALE_S = 6 * 3600
export function staleHours(r: Row, now: number): number | null {
  const o = r.session.outcome
  if (r.group !== 'still' || !o?.at || now - o.at <= STALE_S) return null
  return Math.floor((now - o.at) / 3600)
}

/** Contratto 1.84: la richiesta di un'altra sessione per un compito di questa (`needs_you.from`, `task`); null altrimenti. */
export function askedFor(s: Session): { from: string; task: string | null } | null {
  const n = s.needs_you
  return n?.from && n.from !== s.name ? { from: n.from, task: n.task ?? null } : null
}

