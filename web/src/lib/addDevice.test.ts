import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { left, offerOf, refusalOf } from './addDevice'
import { parseInvite } from './pairing'

const fixture = (n: string) => JSON.parse(readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8'))

describe('pair_add dalla web app (contratto 1.31)', () => {
  const sample = fixture('cmd-result-sample.json')
  const result = sample.result.find((r: { id: string }) => r.id === '6f1c2d3e-0200-4000-8000-000000000200')

  it("l'invito: la riga del QR uguale a quella di pair --add --text, il codice, la scadenza e il link #pair=", () => {
    const o = offerOf(result.text, 'https://supervisor.example')!
    expect(o.qr).toBe(fixture('pair-link.json').line)
    expect(o.code).toBe('482913')
    expect(o.exp).toBe(1789211100)
    expect(o.link?.startsWith('https://supervisor.example/#pair=')).toBe(true)
    expect(parseInvite(o.link!)).toEqual(parseInvite(o.qr!))
  })

  it('senza i dati Firebase sul PC: niente QR e niente link, resta il codice', () => {
    const o = offerOf('{"qr": null, "code": "123456", "exp": 1789211100}', 'https://supervisor.example')!
    expect(o.qr).toBeNull()
    expect(o.link).toBeNull()
    expect(o.code).toBe('123456')
  })

  it('una risposta che non è un invito: null', () => {
    expect(offerOf('delivered', 'https://x')).toBeNull()
    expect(offerOf('{"code": 5}', 'https://x')).toBeNull()
  })

  it('i rifiuti del relay', () => {
    expect(refusalOf('a pairing is already open on the PC')).toEqual({ kind: 'busy', reason: 'a pairing is already open on the PC' })
    expect(refusalOf('pair --add: nessuna chiave salvata sul PC').kind).toBe('nokey')
    expect(refusalOf('already 8 devices: no room for another')).toEqual({ kind: 'full', reason: '8' })
    expect(refusalOf('pairing not started: timeout')).toEqual({ kind: 'failed', reason: 'timeout' })
    expect(refusalOf('boh').kind).toBe('other')
  })

  it('il tempo che resta, mai sotto zero', () => {
    expect(left(1000, 700)).toBe('5:00')
    expect(left(1000, 991)).toBe('0:09')
    expect(left(1000, 1200)).toBe('0:00')
  })
})
