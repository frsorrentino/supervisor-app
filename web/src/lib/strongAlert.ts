import type { Attention, Session, State } from './contract'
import { MASTER } from './summary'

// Contratto 1.68, porta di StrongAlert.kt: un solo avviso forte per ogni interruzione, finché la voce resta in
// state.attention; chi è ferma per Franz conta come una domanda.

/** Lo stesso disegno di vibrazione di telefono e orologio. */
export const VIBRATION = [220, 120, 220]

export const STRONG = new Set(['question', 'needs_you', 'turn_blocking', 'turn_failed', 'quota_limit', 'attention_reminder', 'task_stalled'])

/** Perché la sessione è ferma per Franz: needs_you prima della voce bloccante; null se non lo è. */
export function stop(s: Session): { text: string; at: number } | null {
  if (s.state === 'gone') return null
  if (s.needs_you?.reason?.trim()) return { text: s.needs_you.reason, at: s.needs_you.at ?? 0 }
  if (s.turn_blocking?.text?.trim()) return { text: s.turn_blocking.text, at: s.turn_blocking.at ?? 0 }
  return null
}

/** Il banner nella casa della master: le voci delle altre sessioni, non le sue. */
export const forMaster = (st: State): Attention[] => (st.attention ?? []).filter(a => a.session !== MASTER)
