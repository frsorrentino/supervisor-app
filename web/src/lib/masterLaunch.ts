import type { Session } from './contract'
import type { Freshness } from './durations'

// Porta di ContextActions.kt (master, isMaster, masterTarget) e MasterLaunch.kt (app-master-avvia-19-11).
const NAME = 'master'

/** D9: la master è quella col flag `sessions[].master`; finché il relay non lo manda è quella che si chiama «master». */
export const isMaster = (s: Session, all: Session[]) => all.some(x => x.master != null) ? s.master === true : s.name === NAME
/** La master aperta, se c'è. */
export const masterOf = (all: Session[]) => all.find(s => s.state !== 'gone' && isMaster(s, all)) ?? null
/** A chi mandare `reopen` per avviarla: la master nota, anche chiusa, altrimenti il nome di sempre. */
export const masterTarget = (all: Session[]) => all.find(s => isMaster(s, all))?.name ?? NAME

export type LaunchKind = 'launch' | 'starting' | 'failed' | 'pc_off'
export type LaunchCard = { kind: LaunchKind; error: string | null; button: boolean }
/** Quanto resta «Si sta avviando» dopo il tocco senza che la master compaia nello stato. */
export const STARTING_S = 120

/** Con lo stato vecchio il PC è spento o WSL è fermo: niente tasto, nessuno lo riceverebbe. */
export function launchCard(fresh: Freshness | null, sentAt: number | null, refused: string | null, now: number): LaunchCard {
  const kind: LaunchKind = fresh == null || fresh.stale ? 'pc_off' : refused != null ? 'failed' : sentAt != null && now - sentAt < STARTING_S ? 'starting' : 'launch'
  return { kind, error: kind === 'failed' ? refused : null, button: kind === 'launch' || kind === 'failed' }
}
