import type { WaitItem } from './contract'
import { isOpenItem } from './attese'

// app-silenzio-assenso-23-11 (Franz 23/11 13:20 e 13:23): una carta di ok o di prova, di un cliente o di Franz, si può dare per
// risolta se nessuno risponde entro una scadenza (3 giorni). Contratto 1.159: `silence_until` sulle voci di attese, l'op `silence` col
// tasto «Dai per risolto se tace», `silence` nei chiusi. Chiuso così non è mai un ok di Franz. Porta di Silence.kt.

/** La durata del tasto: 3 giorni, quella che il relay mette col testo vuoto. */
export const SILENCE_DEFAULT = '3d'

/**
 * Il tasto c'è su una carta aperta con un compito, se il relay ha l'op e la voce dice `silence_ok: true` (contratto 1.160): la regola è
 * quella del registro (mai su una scelta, né su ciò che pubblica o paga), l'app non ne ha una sua. Senza il campo, niente.
 */
export function silenceOffer(w: WaitItem, ops: string[] | null | undefined): boolean {
  return !!ops?.includes('silence') && w.task != null && isOpenItem(w) && w.silence_ok === true
}

/** La scadenza c'è e non è passata. */
export const silencePending = (until: number | null | undefined, now: number): boolean => until != null && until > now

const tz = (timeZone?: string) => (timeZone ? { timeZone } : {})
/** «giovedì 26/11 alle 13:30» (in inglese «Thursday 26/11 at 13:30»). */
export function silenceWhenLong(until: number, en: boolean, timeZone?: string): string {
  const d = new Date(until * 1000)
  const day = new Intl.DateTimeFormat(en ? 'en-GB' : 'it-IT', { weekday: 'long', ...tz(timeZone) }).format(d)
  const p = Object.fromEntries(new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23', ...tz(timeZone) }).formatToParts(d).map(x => [x.type, x.value]))
  return `${day} ${p.day}/${p.month} ${en ? 'at' : 'alle'} ${p.hour}:${p.minute}`
}
/** «gio 26/11»: la data corta della riga di Per te. */
export function silenceWhenShort(until: number, en: boolean, timeZone?: string): string {
  const d = new Date(until * 1000)
  const day = new Intl.DateTimeFormat(en ? 'en-GB' : 'it-IT', { weekday: 'short', ...tz(timeZone) }).format(d).replace(/\.$/, '')
  const p = Object.fromEntries(new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'numeric', ...tz(timeZone) }).formatToParts(d).map(x => [x.type, x.value]))
  return `${day} ${p.day}/${p.month}`
}
