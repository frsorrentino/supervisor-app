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

/** Quello che serve a [follow]: l'invio, il bus, la sessione com'è adesso, l'orologio e il passo (finti nei test). */
export type FollowDeps = {
  send: () => Promise<CmdResult | null>
  taken?: (() => Promise<boolean | null>) | undefined
  peek?: (() => Promise<CmdResult | null>) | undefined
  session?: () => Session | null
  now: () => number
  sleep: (ms: number) => Promise<void>
  /** A ogni fase nuova, la prima compresa. */
  onPhase: (p: Phase, r: CmdResult | null) => void
}

/**
 * Il giro di un tasto (track nella web, tapResult sul telefono): manda, poi a ogni passo guarda l'esito e il bus e dice la fase
 * quando cambia, finché non è finale; un «in coda» senza sessione da guardare, o dopo un'ora, si lascia lì.
 */
export async function follow(d: FollowDeps): Promise<CmdResult | null> {
  const sentAt = d.now()
  let r: CmdResult | null = null
  let settled = false
  const p = d.send().then(x => { if (x) r = x }, () => {}).finally(() => { settled = true })
  let shown: Phase = 'sending'
  d.onPhase(shown, null)
  for (;;) {
    // Finché l'invio aspetta, il suo esito sveglia subito il giro; dopo, solo il passo: un invio già chiuso vinceva ogni corsa
    // all'istante e il giro non lasciava più la pagina (la demo bloccata sul comando appeso, 20/11).
    const tick = d.sleep(POLL_MS)
    await (settled ? tick : Promise.race([p, tick]))
    const el = d.now() - sentAt
    if (!r && el >= WAIT_S) r = (await d.peek?.()) ?? null
    const taken = !r && el >= WAIT_S ? ((await d.taken?.()) ?? null) : null
    const ph = phase(el, r, taken, d.session?.() ?? null, sentAt)
    if (ph !== shown) { shown = ph; d.onPhase(ph, r) }
    if (final(ph) || (ph === 'queued' && (!d.session || el > 3600))) return r
  }
}
