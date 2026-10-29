import type { State, Suggestion } from './contract'

// La scheda «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup; contratto 1.84): al massimo tre, dal più
// recente, senza quelli già toccati da questo dispositivo né quelli scaduti. Stessa regola di Suggestions.kt.
// Contratto 1.88 (la coda, Franz 26/10 20:44): prima per `rank` (assente o fuori 0-9 = 5), poi dalla più recente; fra le
// tre al massimo due dello stesso tipo, ma se la coda non ha altro si riempie con le scartate.
export const MAX_SHOWN = 3
// I quattro del 1.84 e i sei della coda (documento suggerimenti-coda, §8.1); un tipo fuori lista si mostra neutro.
export const KINDS = ['recurring', 'cleanup', 'deadline', 'quota', 'choice', 'followup', 'unsaved', 'waiting', 'idea', 'proposal', 'observe'] as const
export const known = (kind: string) => (KINDS as readonly string[]).includes(kind)

export const MAX_PER_KIND = 2
const rank = (s: Suggestion) => (s.rank != null && s.rank >= 0 && s.rank <= 9 ? s.rank : 5)

export function visible(st: State, done: Set<string>, now: number): { shown: Suggestion[]; total: number } {
  const all = (st.suggestions ?? []).filter(s => !done.has(s.id) && (s.expires_at == null || s.expires_at > now)).sort((a, b) => rank(a) - rank(b) || b.at - a.at)
  const shown: Suggestion[] = []
  for (const s of all) if (shown.length < MAX_SHOWN && shown.filter(x => x.kind === s.kind).length < MAX_PER_KIND) shown.push(s)
  for (const s of all) if (shown.length < MAX_SHOWN && !shown.includes(s)) shown.push(s)
  return { shown: all.filter(s => shown.includes(s)), total: all.length }
}

// I tasti di una `choice`: le sue 2-4 etichette; altrimenti nessuno, e la voce ha Accetta come le altre.
export const options = (s: Suggestion): string[] => (s.kind === 'choice' && s.options && s.options.length >= 2 && s.options.length <= 4 ? s.options : [])
