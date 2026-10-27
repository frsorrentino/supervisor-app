import type { State, Suggestion } from './contract'

// La scheda «Suggerimenti della master» (Franz 24/10 19:46, scheda A del mockup; contratto 1.84): al massimo tre, dal più
// recente, senza quelli già toccati da questo dispositivo né quelli scaduti. Stessa regola di Suggestions.kt.
export const MAX_SHOWN = 3
// I quattro del 1.84 e i sei della coda (documento suggerimenti-coda, §8.1); un tipo fuori lista si mostra neutro.
export const KINDS = ['recurring', 'cleanup', 'deadline', 'quota', 'choice', 'followup', 'unsaved', 'waiting', 'idea', 'proposal'] as const
export const known = (kind: string) => (KINDS as readonly string[]).includes(kind)

export function visible(st: State, done: Set<string>, now: number): { shown: Suggestion[]; total: number } {
  const all = (st.suggestions ?? []).filter(s => !done.has(s.id) && (s.expires_at == null || s.expires_at > now)).sort((a, b) => b.at - a.at)
  return { shown: all.slice(0, MAX_SHOWN), total: all.length }
}
