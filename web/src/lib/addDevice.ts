// Aggiungere un dispositivo dalla web app (Franz, 13/10 19:05), come AddDeviceSheet del telefono: op `pair_add` del
// contratto 1.31. Il relay apre da sé `relay pair --add` per 5 minuti e risponde `{qr, code, exp}`; la web app mostra il
// QR, il link `#pair=` da condividere e la scadenza. I rifiuti si riconoscono dal testo del relay, come PairAddText.

import { inviteLink } from './pairing'

/** `qr` = la riga compatta di `relay pair --add --text`, null se il PC non ha i dati dell'app Firebase; `link` lo stesso invito come link della web app. */
export type Offer = { qr: string | null; link: string | null; code: string; exp: number }

export type RefusalKind = 'busy' | 'nokey' | 'full' | 'failed' | 'other'
/** `reason`: il numero di dispositivi per 'full', il motivo dopo «pairing not started:» per 'failed', il testo intero altrimenti. */
export type Refusal = { kind: RefusalKind; reason: string }

/** L'invito da una risposta `ok` di `pair_add`; null se il testo non lo è. */
export function offerOf(text: string, origin: string): Offer | null {
  let o: { qr?: unknown; code?: unknown; exp?: unknown }
  try { o = JSON.parse(text) } catch { return null }
  if (!o || typeof o !== 'object' || typeof o.code !== 'string' || typeof o.exp !== 'number') return null
  const qr = o.qr && typeof o.qr === 'object' ? JSON.stringify(o.qr) : null
  return { qr, link: qr ? inviteLink(origin, qr) : null, code: o.code, exp: o.exp }
}

/** Un rifiuto di `pair_add` (inglese anche col relay in italiano, tranne la chiave mancante). */
export function refusalOf(text: string): Refusal {
  if (text.includes('a pairing is already open')) return { kind: 'busy', reason: text }
  if (text.startsWith('pair --add:')) return { kind: 'nokey', reason: text }
  const full = /already (\d+) devices/.exec(text)
  if (full) return { kind: 'full', reason: full[1] }
  if (text.startsWith('pairing not started')) return { kind: 'failed', reason: text.slice(text.indexOf(':') + 1).trim() }
  return { kind: 'other', reason: text }
}

/** «m:ss» fino alla scadenza, mai sotto zero. */
export function left(exp: number, now: number): string {
  const s = Math.max(0, exp - now)
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}
