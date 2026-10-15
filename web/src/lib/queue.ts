import type { State } from './contract'
import { stop } from './strongAlert'

// La coda «Ti aspettano», porta di AttentionQueue.kt: le domande aperte di tutte le sessioni, da rispondere in fila.
// Contratto 1.68: chi è ferma per Franz conta come una domanda, come nel banner e nella home (15/10: il banner diceva
// «2 sessioni ti aspettano», la coda «1 di 1»); la sua pagina dice il motivo e porta alla chat. `questionId` per una ferma
// è una chiave sua, mai l'id di una domanda.
export type QueueItem = { session: string; questionId: string; askedAt: number; kind: 'question' | 'stop'; text: string }

/** Le domande aperte e le ferme per Franz, dalla più vecchia. */
export const items = (state: State): QueueItem[] =>
  state.sessions.flatMap((s): QueueItem[] => {
    if (s.question) return [{ session: s.name, questionId: s.question.id, askedAt: s.question.asked_at, kind: 'question', text: s.question.text }]
    const x = stop(s)
    return x ? [{ session: s.name, questionId: `stop:${s.name}@${x.at}`, askedAt: x.at, kind: 'stop', text: x.text }] : []
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
