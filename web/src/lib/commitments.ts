// «Impegni» (P2 e P4 del 21/10, contratto 1.76), porta di Commitments.kt: la vista del registro in cima alla home, sopra le
// sessioni. Ogni impegno approvato e non compiuto resta in vista con l'età, chi deve fare il prossimo passo e da quanto è
// fermo; quando aspetta l'utente e c'è un ok del registro collegato, la riga ha «Approva» (lo stesso `approve` di sempre:
// dopo l'ok il relay risveglia la sessione). Ordine del relay (prima i fermi, poi per inattività), in pausa in fondo.
import type { Approval, Commitment, State } from './contract'

export type Tone = 'normal' | 'warn' | 'alert'
export const PERSON = 'person'
export const WARN_S = 2 * 3600
export const ALERT_S = 8 * 3600

/** `idle`/`age` adesso (quelli del relay più il tempo dalla pubblicazione); `turn`: la sessione a cui tocca, null se tocca all'utente. */
export type Row = { c: Commitment; idle: number; age: number; tone: Tone; yourTurn: boolean; turn: string | null; approval: Approval | null }

export function rows(st: State, now: number): Row[] {
  const list = st.commitments
  if (!list) return []
  const passed = Math.max(0, now - (st.published_at ?? st.ts))
  const out = list.map((c): Row => {
    const idle = c.idle_s + passed
    return { c, idle, age: c.age_s + passed, tone: tone(c, idle), yourTurn: c.waits?.on === PERSON, turn: turnOf(c), approval: (st.approvals ?? []).find(a => a.task === c.task) ?? null }
  })
  // sort è stabile: l'ordine del relay resta, gli impegni in pausa vanno in fondo.
  return out.sort((a, b) => Number(a.c.paused != null) - Number(b.c.paused != null))
}

/** Ambra da due ore senza un passo, rosso da otto; niente per un impegno in pausa. */
export function tone(c: Commitment, idle: number): Tone {
  if (c.paused) return 'normal'
  if (idle >= ALERT_S) return 'alert'
  if (c.stalled || idle >= WARN_S) return 'warn'
  return 'normal'
}

export const turnOf = (c: Commitment): string | null => (c.waits?.on && c.waits.on !== PERSON ? c.waits.on : null)

/** Quanti impegni aspettano l'utente adesso (non in pausa). */
export const yours = (st: State): number => (st.commitments ?? []).filter(c => c.waits?.on === PERSON && !c.paused).length
