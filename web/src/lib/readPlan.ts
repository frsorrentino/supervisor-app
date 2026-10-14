// La cadenza delle letture della conversazione, la stessa del telefono (PhoneTerminal.kt): ogni 10 s se la sessione
// lavora, ogni minuto se è ferma, subito se il suo stato cambia, 21 s di attesa per una lettura rimasta senza risposta.
// Il 07/10 la web chiedeva una lettura per sessione a ogni stato, 888 all'ora, ed era il carico più grande sulla fila dei
// comandi del relay (piano prestazioni, Task 5).
export const BUSY_S = 10
export const IDLE_S = 60
export const LOST_S = 21

export type LastRead = { at: number; answered: boolean }

export function shouldRead(s: { state: string }, last: LastRead | undefined, changed: boolean, now: number): boolean {
  // La prima volta si legge sempre, anche una sessione chiusa: la sua colonna mostra la conversazione.
  if (!last) return true
  if (s.state === 'gone') return false
  if (!last.answered) return now - last.at >= LOST_S
  if (changed) return true
  const busy = s.state === 'busy' || s.state === 'awaiting'
  return now - last.at >= (busy ? BUSY_S : IDLE_S)
}

// La rilettura in sottofondo delle chat che non sono a schermo, la stessa del telefono (ChatFeed.due). Franz, 14/10 18:39:
// «quando cambio una chat vedo per qualche secondo il testo vecchio prima che si aggiorni». A ogni attività nuova, anche
// durante il turno, al massimo una lettura ogni 12 s per sessione, due alla volta.
export const BG_GAP_MS = 12_000
export const BG_PARALLEL = 2

/** L'ultima lettura in sottofondo: la firma dell'attività quando è partita, l'ora, se è arrivata. Fallita = firma vuota. */
export type BgRead = { sig: string; atMs: number; done: boolean }

type Active = {
  name: string; state: string; turn_started?: number | null; tool?: string | null; tool_note?: string | null
  question?: { id: string } | null; outcome?: { at: number } | null
}

/** La firma dell'attività: cambia con un turno nuovo, ogni strumento, una domanda e un esito. */
export function activity(s: Active): string {
  return `${s.state}|${s.turn_started ?? ''}|${s.tool ?? ''}|${s.tool_note ?? ''}|${s.question?.id ?? ''}|${s.outcome?.at ?? ''}`
}

/** Da rileggere in sottofondo: mai letta, o attività nuova e almeno 12 s dall'ultima lettura; né chiusa né a schermo. */
export function due(sessions: Active[], reads: Record<string, BgRead>, shown: (string | null)[], nowMs: number): string[] {
  return sessions.filter(s => {
    if (s.state === 'gone' || shown.includes(s.name)) return false
    const r = reads[s.name]
    return !r || (r.sig !== activity(s) && nowMs - r.atMs >= BG_GAP_MS)
  }).map(s => s.name)
}

/** La copia in cache è più vecchia dell'ultima attività: aprendo la chat si dice che si sta aggiornando. */
export function behind(s: Active, r: BgRead | undefined): boolean {
  return !r?.done || r.sig !== activity(s)
}
