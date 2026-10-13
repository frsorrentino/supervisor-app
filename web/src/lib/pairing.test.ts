import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { pairAdd, PairError, parseInvite } from './pairing'
import { privateFromScalar, toHex } from './crypto'
import { MemoryBus } from './bus'

const raw = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')
const fixture = (n: string) => JSON.parse(raw(n))
const range = (a: number, b: number) => Uint8Array.from({ length: b - a }, (_, i) => a + i)

describe('invito: la riga di pair --add, il link cmwatch:// e il link della web app', () => {
  const link = fixture('pair-link.json')

  it('la riga del QR', () => {
    const q = parseInvite(link.line)
    expect(q?.i).toBe('q3Vb2mXz0rT8yKp1LwN4sA')
    expect(q?.m).toBe('add')
    expect(q?.f.d).toBe('https://cmwatch-demo-default-rtdb.europe-west1.firebasedatabase.app')
  })

  it('il link cmwatch://pair?q=', () => {
    expect(parseInvite(link.uri)).toEqual(parseInvite(link.line))
  })

  it('il link della web app, con la riga dopo #pair=', () => {
    const q = link.uri.split('q=')[1]
    expect(parseInvite(`https://supervisor-app.web.app/#pair=${q}`)).toEqual(parseInvite(link.line))
  })

  it('un altro testo, un id corto o un database non https: niente', () => {
    expect(parseInvite('ciao')).toBeNull()
    expect(parseInvite(link.line.replace('q3Vb2mXz0rT8yKp1LwN4sA', 'corto'))).toBeNull()
    expect(parseInvite(link.line.replace('https://cmwatch', 'http://cmwatch'))).toBeNull()
  })
})

describe('accoppiamento in più dal browser (1.30, 1.39): pair-add.json', () => {
  const f = fixture('pair-add.json')
  const qr = parseInvite(JSON.stringify(f.qr))!
  const device = () => privateFromScalar(range(64, 96))
  const now = () => f.qr.e - 60

  it('scrive /watch con kind web, legge la conferma del PC e apre la chiave del relay', async () => {
    const bus = new MemoryBus({ [`pair/${f.qr.i}`]: { pc_pub: f.qr.c, host: f.qr.h, exp: f.qr.e, mode: 'add' } })
    bus.onPut = (path) => { if (path === `pair/${f.qr.i}/watch`) bus.data[`pair/${f.qr.i}/ok`] = f.ok }
    const r = await pairAdd(bus, qr, { uid: f.watch.uid, name: 'Safari su iPhone', keyPair: await device(), now, pollMs: 1 })
    const w = bus.data[`pair/${f.qr.i}/watch`] as Record<string, string>
    expect(w).toEqual({ watch_pub: f.watch.watch_pub, uid: f.watch.uid, name: 'Safari su iPhone', check: f.watch.check, kind: 'web' })
    expect(toHex(r.key)).toBe(f.relay_key)
    expect(r.host).toBe('penguin')
  })

  it('invito usato o scaduto sul bus: «codice non valido»', async () => {
    await expect(pairAdd(new MemoryBus({}), qr, { uid: 'u', name: 'n', keyPair: await device(), now, pollMs: 1 }))
      .rejects.toEqual(new PairError('invalid'))
  })

  it('il PC ha già otto dispositivi: «full» subito', async () => {
    const bus = new MemoryBus({ [`pair/${f.qr.i}`]: { pc_pub: f.qr.c, host: f.qr.h, exp: f.qr.e, mode: 'add' } })
    bus.onPut = () => { bus.data[`pair/${f.qr.i}/error`] = 'full' }
    await expect(pairAdd(bus, qr, { uid: f.watch.uid, name: 'n', keyPair: await device(), now, pollMs: 1 }))
      .rejects.toEqual(new PairError('full'))
  })

  it('una conferma con il check sbagliato non si accetta', async () => {
    const bus = new MemoryBus({ [`pair/${f.qr.i}`]: { pc_pub: f.qr.c, host: f.qr.h, exp: f.qr.e, mode: 'add' } })
    bus.onPut = () => { bus.data[`pair/${f.qr.i}/ok`] = { ...f.ok, check: '0000000000000000' } }
    await expect(pairAdd(bus, qr, { uid: f.watch.uid, name: 'n', keyPair: await device(), now, pollMs: 1 }))
      .rejects.toEqual(new PairError('check'))
  })

  it('invito scaduto: errore senza scrivere', async () => {
    const bus = new MemoryBus({ [`pair/${f.qr.i}`]: { pc_pub: f.qr.c, host: f.qr.h, exp: f.qr.e, mode: 'add' } })
    await expect(pairAdd(bus, qr, { uid: 'u', name: 'n', keyPair: await device(), now: () => f.qr.e + 1, pollMs: 1 }))
      .rejects.toEqual(new PairError('expired'))
    expect(bus.data[`pair/${f.qr.i}/watch`]).toBeUndefined()
  })

  it('il PC non conferma entro il tempo', async () => {
    const bus = new MemoryBus({ [`pair/${f.qr.i}`]: { pc_pub: f.qr.c, host: f.qr.h, exp: f.qr.e, mode: 'add' } })
    await expect(pairAdd(bus, qr, { uid: f.watch.uid, name: 'n', keyPair: await device(), now, pollMs: 1, timeoutMs: 20 }))
      .rejects.toEqual(new PairError('timeout'))
  })
})
