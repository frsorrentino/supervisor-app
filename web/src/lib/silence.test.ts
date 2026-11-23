import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import type { WaitItem } from './contract'
import { silenceOffer, silencePending, silenceWhenLong, silenceWhenShort } from './silence'

// Gli stessi casi di SilenceTest.kt (app-silenzio-assenso-23-11, contratto 1.159), sulla fixture delle attese.
const list = JSON.parse(readFileSync(new URL('../../../contract/attese-rd-sample.json', import.meta.url), 'utf8')).attese.items as WaitItem[]
const ops = ['attese', 'silence']
const item = (kind: string) => list.find(w => w.kind === kind)!

describe('il silenzio assenso', () => {
  it('solo su ok e prova', () => {
    expect(list.filter(w => silenceOffer(w, ops)).map(w => w.kind)).toEqual(['ok', 'prova'])
    expect(silenceOffer(item('ok'), ['attese'])).toBe(false)
  })
  it('mai su una scelta né sulla produzione', () => {
    expect(silenceOffer({ ...item('ok'), options: [{ n: 1, label: 'A', value: 'A' }, { n: 2, label: 'B', value: 'B' }] } as WaitItem, ops)).toBe(false)
    expect(silenceOffer({ ...item('ok'), label: 'choose' }, ops)).toBe(false)
    expect(silenceOffer({ ...item('ok'), card: { deploy: true } }, ops)).toBe(false)
    expect(silenceOffer({ ...item('ok'), task: null }, ops)).toBe(false)
    expect(silenceOffer({ ...item('ok'), state: 'approved' } as WaitItem, ops)).toBe(false)
  })
  it('le date', () => {
    const until = Date.UTC(2026, 10, 26, 12, 30) / 1000 // 13:30 a Roma
    expect(silenceWhenLong(until, false, 'Europe/Rome')).toBe('giovedì 26/11 alle 13:30')
    expect(silenceWhenLong(until, true, 'Europe/Rome')).toBe('Thursday 26/11 at 13:30')
    expect(silenceWhenShort(until, false, 'Europe/Rome')).toBe('gio 26/11')
    expect(silenceWhenShort(until, true, 'Europe/Rome')).toBe('Thu 26/11')
    expect(silencePending(until, until - 1)).toBe(true)
    expect(silencePending(until, until)).toBe(false)
    expect(silencePending(null, 0)).toBe(false)
  })
})
