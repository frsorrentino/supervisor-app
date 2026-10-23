import type { State } from './contract'

// Franz 23/10 21:24 («ho già cliccato su approva ma continua ad apparire»): dopo Approva la voce sparisce subito, su app e
// web, e non torna finché il registro non conferma; con un rifiuto torna col motivo. Porta di Approvals.kt.

/** Lo stato senza gli ok dei compiti `tasks`: l'ok tolto e l'impegno già approvato, senza attesa della persona. */
export function withoutApproved(state: State, tasks: Set<string>): State {
  if (!tasks.size) return state
  return {
    ...state,
    approvals: (state.approvals ?? []).filter(a => !tasks.has(a.task)),
    commitments: state.commitments?.map(c => (tasks.has(c.task) ? { ...c, approved: true, state: 'approved', waits: null } : c)),
  }
}

/** I compiti che il registro ha già portato oltre l'ok: si dimenticano, così una richiesta nuova dello stesso compito torna a vedersi. */
export function settled(state: State, tasks: Set<string>): Set<string> {
  const asking = new Set((state.approvals ?? []).map(a => a.task))
  const awaiting = new Set((state.commitments ?? []).filter(c => c.state === 'awaiting_ok').map(c => c.task))
  return new Set([...tasks].filter(t => !asking.has(t) && !awaiting.has(t)))
}
