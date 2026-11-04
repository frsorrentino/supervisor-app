import type { Session } from './contract'

// Il pannello di chiusura di una sessione (CloseSplash.kt; Franz, 08/10 18:06-18:29, variante A): la pagina non sparisce.
// Dopo /exit dice che si sta chiudendo, con i passi veri; poi che è chiusa e a che ora, e dopo 3 s si torna alla home.
export const CLOSED_SHOW_MS = 3_000
export const SLOW_S = 20
/** Contratto 1.120: «Chiudi comunque» si chiude a fine turno o entro 180 s; il pannello dice «più del solito» solo dopo. */
export const FORCE_S = 180
/** I motivi per cui il relay arma una chiusura (`close` con when after o force): mai una attaccata o che non gira. */
export const ARMABLE = ['busy', 'question', 'background']

// `refused`: il motivo per cui il relay non l'ha chiusa, già in chiaro, e `armable` se si può chiudere quando finisce o
// comunque (app-chiudi-esito-04-11, contratto 1.120); `force`: la chiusura è un «Chiudi comunque».
export type Leaving = { name: string; sentAt: number; delivered: boolean; refused?: string | null; armable?: boolean; force?: boolean }
export type Phase =
  | { kind: 'closing'; name: string; sentAt: number; delivered: boolean; slow: boolean; force: boolean }
  | { kind: 'closed'; name: string; at: number; byMe: boolean }
  | { kind: 'refused'; name: string; why: string; armable: boolean }

/**
 * La fase per la pagina di `name`; null = niente pannello. `lastSeen` è la sessione com'era l'ultima volta nello stato.
 * Chiusa per conto suo ma ancora nello stato resta una pagina con «Riapri» (03/10 19:57): il pannello arriva quando esce.
 */
export function closeSplash(name: string, sessions: Session[], leaving: Leaving | null, lastSeen: Session | null, now: number): Phase | null {
  const s = sessions.find(x => x.name === name)
  const mine = leaving?.name === name
  // Per una gone `since` è l'ultimo avvistamento, cioè quando si è chiusa; per una viva no: allora vale adesso.
  const at = s?.since ?? (lastSeen?.state === 'gone' ? lastSeen.since : now)
  if (!s) return { kind: 'closed', name, at, byMe: mine }
  if (s.state === 'gone') return mine ? { kind: 'closed', name, at, byMe: true } : null
  // Il Chiudi dà sempre un esito (Franz, 04/11): rifiutata, il pannello dice perché finché non lo togli.
  if (mine && leaving!.refused) return { kind: 'refused', name, why: leaving!.refused, armable: !!leaving!.armable }
  // Dal vivo 08/10 19:29: dopo /exit Claude Code può chiedere cosa fare dei lavori in background; il pannello non copre la
  // domanda, e torna quando è risposta.
  if (mine && s.question) return null
  if (mine) {
    const force = !!leaving!.force
    return { kind: 'closing', name, sentAt: leaving!.sentAt, delivered: leaving!.delivered, slow: now - leaving!.sentAt >= (force ? FORCE_S + SLOW_S : SLOW_S), force }
  }
  return null
}

/**
 * La sessione fantasma per la pagina di `name` quando non è più nello stato: com'era l'ultima volta, chiusa e senza domanda.
 * Mai vista in questa apertura (colonna appuntata o pagina ricordata da un'altra volta): un segnaposto chiuso, mai null,
 * perché la pagina la legge subito (`s.state`) e un null faceva crollare l'app (23/10).
 */
export function ghostSession(name: string, lastSeen: Record<string, Session>): Session {
  const x = lastSeen[name]
  return x ? { ...x, state: 'gone', question: null } : { id: name, name, account: '', project: '', state: 'gone', since: 0, question: null }
}

/** Una riga «<nome> not closed: <motivo>» del relay (op close e /exit), col motivo già in chiaro. */
export type Refusal = { name: string; why: string; armable: boolean }
const NOT_CLOSED = /(?:^|\s)(\S+) not closed: (.+)$/

/** Il motivo del relay come una delle chiavi di `close_preview.blocked_by`; null se non lo riconosce. */
export function refusalKind(reason: string): string | null {
  if (/background/i.test(reason)) return 'background'
  if (/question/i.test(reason)) return 'question'
  if (/working|busy/i.test(reason)) return 'busy'
  if (/attached|terminal|open at the PC/i.test(reason)) return 'attached'
  return null
}

/** Le sessioni che il relay non ha chiuso, dal testo del risultato; `texts` sono i motivi in chiaro (`t.closeBlocked`). */
export function closeRefusals(text: string, texts: Record<string, string>, other: (reason: string) => string): Refusal[] {
  return text.split('\n').flatMap(l => {
    const m = NOT_CLOSED.exec(l.trim())
    if (!m) return []
    const k = refusalKind(m[2])
    return [{ name: m[1], why: (k && texts[k]) || other(m[2]), armable: !!k && ARMABLE.includes(k) }]
  })
}
