import type { CmdResult, Session } from './contract'
import { SKEW_S } from './chatRules'

/**
 * app-tasti-riscontro-20-11, estensione di Franz del 20/11 alle 09:46 («Estendi il controllo ai comandi appesi»): a che punto è un
 * comando mandato da un tasto, anche quando l'esito non torna. Porta di CmdTrack.kt (core), stesse soglie e stesse fasi.
 */
export const WAIT_S = 4
export const MAX_S = 180
export const POLL_MS = 2_000
export type Phase = 'sending' | 'not_taken' | 'working' | 'done' | 'queued' | 'delivered' | 'refused' | 'no_answer'

export const final = (p: Phase) => p === 'done' || p === 'delivered' || p === 'refused' || p === 'no_answer'

/** Il prompt in coda è stato consegnato: la sessione ha cominciato un turno dopo l'invio, o è tornata ferma senza domanda. */
export function delivered(s: Session | null | undefined, sentAt: number): boolean {
  if (!s) return false
  return (s.state === 'busy' && s.turn_started != null && s.turn_started >= sentAt - SKEW_S) || s.state === 'idle'
}

/** `taken`: false = ancora in /cmd, true = il PC l'ha preso, null = non si sa. */
export function phase(elapsedS: number, result: CmdResult | null, taken: boolean | null, session: Session | null = null, sentAt = 0): Phase {
  if (result && !result.ok) return 'refused'
  if (result && result.text?.startsWith('queued:')) return delivered(session, sentAt) ? 'delivered' : 'queued'
  if (result) return 'done'
  if (elapsedS >= MAX_S) return 'no_answer'
  if (elapsedS < WAIT_S) return 'sending'
  if (taken === false) return 'not_taken'
  if (taken === true) return 'working'
  return 'sending'
}
