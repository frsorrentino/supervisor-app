import type { Session } from './contract'

// La riga «Prossimi: a · b · c» in fondo alla risposta, come NextSteps.parse dell'app Android: si toglie dal testo e diventa
// al massimo 3 consigli di non più di 40 caratteri. Conta l'ultima riga non vuota, saltando quella per l'orologio e quella
// dell'esito: una sessione li aveva scritti prima dell'esito e la riga restava testo (12/10 11:17).
export const MAX = 3
export const MAX_CHARS = 40

/** Contratto 1.38: `blocking` = le voci scritte con «!» davanti, che sbloccano un lavoro fermo; il «!» si toglie. */
export function parseSteps(text: string): { text: string; steps: string[]; blocking: Set<string> } {
  const lines = text.split('\n')
  let idx = -1
  const closing = (l: string) => l.trimStart().startsWith('Watch:') || l.trimStart().startsWith('Esito:')
  for (let i = lines.length - 1; i >= 0; i--) if (lines[i].trim() && !closing(lines[i])) { idx = i; break }
  if (idx < 0 || !lines[idx].trim().startsWith('Prossimi:')) return { text, steps: [], blocking: new Set() }
  // Il «!» non conta nei 40 caratteri (regola 10 del kernel).
  const raw = lines[idx].trim().slice('Prossimi:'.length).split('·').map(s => s.trim())
    .map(s => ({ bang: s.startsWith('!'), s: s.replace(/^!\s*/, '') })).filter(x => x.s && x.s.length <= MAX_CHARS).slice(0, MAX)
  // Tolta fra due righe vuote, una delle due se ne va con lei.
  const before = lines.slice(0, idx)
  if (before.length && !before[before.length - 1].trim() && idx + 1 < lines.length && !lines[idx + 1].trim()) before.pop()
  return { text: [...before, ...lines.slice(idx + 1)].join('\n').trimEnd(), steps: raw.map(x => x.s), blocking: new Set(raw.filter(x => x.bang).map(x => x.s)) }
}

/**
 * Contratto 1.38: quali Prossimi sbloccano un lavoro fermo. Dalla conversazione li dice il «!» (`parsed`); dall'esito, dove
 * il relay l'ha già tolto, li dice `next_steps` della sessione.
 */
export function blockingOf(s: Session | null | undefined, parsed: Set<string>): Set<string> {
  const out = new Set(parsed)
  for (const n of s?.next_steps ?? []) if (n.blocking) out.add(n.text)
  return out
}
