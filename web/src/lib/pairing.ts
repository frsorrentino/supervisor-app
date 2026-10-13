// Accoppiamento del browser come dispositivo in più (contratto 1.30, `kind` web dalla 1.39), come FirebaseTransport.pair
// del core Android. L'invito è la riga di `relay pair --add --text` (il QR), il link `cmwatch://pair?q=` o il link della
// web app con la stessa riga in base64url dopo `#pair=`.

import type { Bus } from './bus'
import { checkCode, fromB64, fromHex, openBlob, publicB64, sharedKey } from './crypto'

export type Invite = {
  v: number; i: string; c: string; h: string; e: number; m?: string
  f: { k: string; p: string; a: string; d: string; t: string }
}

export type PairFailure = 'invalid' | 'expired' | 'full' | 'check' | 'timeout' | 'key'
export class PairError extends Error {
  constructor(readonly why: PairFailure) { super(why) }
}

const ID = /^[A-Za-z0-9_-]{22}$/

function fromB64Url(s: string): string | null {
  try {
    const b = s.replace(/-/g, '+').replace(/_/g, '/')
    return new TextDecoder().decode(fromB64(b + '='.repeat((4 - b.length % 4) % 4)))
  } catch { return null }
}

/** La riga dell'invito dentro un testo (riga, link cmwatch o link della web app); null se non è un invito valido. */
export function parseInvite(text: string): Invite | null {
  const t = text.trim()
  let line: string | null = t
  const link = /^cmwatch:\/\/pair\?(?:.*&)?q=([^&#]+)/.exec(t) ?? /#(?:.*&)?pair=([^&]+)/.exec(t)
  if (link) line = fromB64Url(link[1])
  if (!line) return null
  let q: Invite
  try { q = JSON.parse(line) } catch { return null }
  if (!q || typeof q !== 'object' || q.v !== 1 || typeof q.i !== 'string' || !ID.test(q.i) || !q.h || typeof q.e !== 'number') return null
  try { if (fromB64(q.c).length !== 32) return null } catch { return null }
  const f = q.f
  if (!f || ![f.k, f.p, f.a, f.t].every(x => typeof x === 'string' && x !== '') || typeof f.d !== 'string' || !f.d.startsWith('https://')) return null
  return q
}

/** Il link che apre la web app con l'invito già dentro (lo stesso base64url del link cmwatch). */
export function inviteLink(origin: string, line: string): string {
  const b = btoa(String.fromCharCode(...new TextEncoder().encode(line))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `${origin}/#pair=${b}`
}

type Ok = { host?: string; check?: string; key?: unknown }

/** La conferma del PC in `/ok`, o il rifiuto in `/error` («full» oltre gli otto dispositivi). */
async function confirmation(bus: Bus, id: string, deadline: number, pollMs: number): Promise<Ok> {
  for (;;) {
    if (await bus.get(`pair/${id}/error`) === 'full') throw new PairError('full')
    const ok = await bus.get(`pair/${id}/ok`) as Ok | null
    if (ok) return ok
    if (Date.now() > deadline) throw new PairError('timeout')
    await new Promise(r => setTimeout(r, pollMs))
  }
}

type Opts = { uid: string; name: string; keyPair: CryptoKeyPair; now?: () => number; pollMs?: number; timeoutMs?: number }

/**
 * Legge `/pair/<id>`, deriva la chiave del giro, scrive `/pair/<id>/watch` e aspetta `/ok` (o `/error`), ne verifica il
 * check (`id:pc`) e apre la chiave del relay. Con un invito non «add» (il primo dispositivo) la chiave è quella del giro.
 */
export async function pairAdd(bus: Bus, q: Invite, o: Opts): Promise<{ key: Uint8Array<ArrayBuffer>; host: string }> {
  const now = o.now ?? (() => Date.now() / 1000)
  if (now() > q.e) throw new PairError('expired')
  const doc = await bus.get(`pair/${q.i}`) as { pc_pub?: string; host?: string; exp?: number; mode?: string } | null
  if (!doc?.pc_pub) throw new PairError('invalid')
  if (doc.exp != null && now() > doc.exp) throw new PairError('expired')
  const shared = await sharedKey(o.keyPair, doc.pc_pub)
  await bus.put(`pair/${q.i}/watch`, {
    watch_pub: await publicB64(o.keyPair.publicKey), uid: o.uid, name: o.name, check: await checkCode(shared, q.i), kind: 'web',
  })
  const ok = await confirmation(bus, q.i, Date.now() + (o.timeoutMs ?? 60_000), o.pollMs ?? 1000)
  if (ok.check !== await checkCode(shared, `${q.i}:pc`)) throw new PairError('check')
  const host = ok.host ?? doc.host ?? q.h
  if (!ok.key) {
    if ((doc.mode ?? q.m) === 'add') throw new PairError('key')
    return { key: shared, host }
  }
  try {
    const hex = JSON.parse(await openBlob(JSON.stringify(ok.key), shared)).key
    if (typeof hex !== 'string' || !/^[0-9a-fA-F]{64}$/.test(hex)) throw new Error()
    return { key: fromHex(hex), host }
  } catch { throw new PairError('key') }
}
