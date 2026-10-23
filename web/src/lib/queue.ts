import type { Session, State } from './contract'
import { stop } from './strongAlert'

// La coda «Ti aspettano», porta di AttentionQueue.kt: le domande aperte di tutte le sessioni, da rispondere in fila.
// Contratto 1.68: chi è ferma per Franz conta come una domanda, come nel banner e nella home (15/10: il banner diceva
// «2 sessioni ti aspettano», la coda «1 di 1»); la sua pagina dice il motivo e porta alla chat. `questionId` per una ferma
// è una chiave sua, mai l'id di una domanda.
/** Contratto 1.80: il tasto «Fatto» di una ferma, op `attention_done` {kind, arg = ref, since}. */
export type Done = { kind: 'needs_you' | 'turn_blocking'; ref: string; since?: number }
export type QueueItem = { session: string; questionId: string; askedAt: number; kind: 'question' | 'stop'; text: string; done: Done | null; expiresAt: number | null }

/** Il «Fatto» di una ferma: needs_you col nome della sessione; turn_blocking col nome e il since della voce. Una domanda no. */
export function doneOf(s: Session): Done | null {
  if (s.state === 'gone') return null
  if (s.needs_you?.reason?.trim()) return { kind: 'needs_you', ref: s.name }
  if (s.turn_blocking?.text?.trim()) return { kind: 'turn_blocking', ref: s.name, since: s.turn_blocking.at ?? 0 }
  return null
}

/** Le domande aperte e le ferme per Franz, dalla più vecchia. */
export const items = (state: State): QueueItem[] =>
  // Le sessioni chiuse mai, come nel banner (23/10): banner e coda mostrano le stesse voci.
  state.sessions.filter(s => s.state !== 'gone').flatMap((s): QueueItem[] => {
    if (s.question) return [{ session: s.name, questionId: s.question.id, askedAt: s.question.asked_at, kind: 'question', text: s.question.text, done: null, expiresAt: null }]
    const x = stop(s)
    return x ? [{ session: s.name, questionId: `stop:${s.name}@${x.at}`, askedAt: x.at, kind: 'stop', text: x.text, done: doneOf(s), expiresAt: (s.needs_you?.reason?.trim() ? s.needs_you.expires_at : null) ?? null }] : []
  }).sort((a, b) => a.askedAt - b.askedAt)

/** La domanda dopo `current`; dopo l'ultima, o se `current` non c'è più, si riparte dalla più vecchia. */
export function next(state: State, current: string | null): QueueItem | null {
  const all = items(state)
  const i = all.findIndex(x => x.questionId === current)
  return i < 0 ? all[0] ?? null : all[i + 1] ?? all[0] ?? null
}

/** La pagina della domanda `questionId` nella fila di adesso; null se non c'è più. */
export function page(list: QueueItem[], questionId: string | null): number | null {
  const i = list.findIndex(x => x.questionId === questionId)
  return i >= 0 ? i : null
}
