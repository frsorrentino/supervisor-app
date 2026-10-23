import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type Session } from './contract'
import { expiry, freshness, orderSessions, publishedTs, since, waitingPc } from './durations'

// Gli stessi casi di OrderTest in Kotlin.
const st = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const NB = ' '
describe('ordine, freschezza, durate', () => {
  // Contratto 1.80: la scadenza di un needs-you si legge con il giorno (23/10 21:23: «scade alle 21:23» per una scadenza a 24 ore).
  it('la scadenza dice il giorno: oggi solo l\'ora, domani «domani», poi la data', () => {
    const now = Math.round(new Date(2026, 9, 23, 21, 23).getTime() / 1000)
    expect(expiry(now + 97 * 60, now)).toEqual({ day: 'today', hm: '23:00' })
    expect(expiry(now + 86400, now)).toEqual({ day: 'tomorrow', hm: '21:23' })
    expect(expiry(Math.round(new Date(2026, 9, 26, 9, 0).getTime() / 1000), now)).toEqual({ day: 'later', hm: '09:00', date: '26/10' })
    expect(expiry(now - 83 * 60, now)).toEqual({ day: 'today', hm: '20:00' })
  })
  it('waiting, busy, idle, gone', () => {
    const shuffled = [...st.sessions].reverse()
    expect(orderSessions(shuffled).map(s => s.name)).toEqual(['ledger-api', 'atlas-shop', 'field-notes', 'orbit-docs'])
  })
  it('a parità di stato alfabetico', () => {
    const a = { ...st.sessions[2], name: 'zeta' }, b = { ...st.sessions[2], name: 'alpha' }
    expect(orderSessions([a, b]).map(s => s.name)).toEqual(['alpha', 'zeta'])
  })
  it('awaiting conta come busy', () => {
    const awaiting: Session = { ...st.sessions[1], name: 'aaa-await', state: 'awaiting' }
    expect(orderSessions([...st.sessions, awaiting]).map(s => s.name)).toEqual(['ledger-api', 'aaa-await', 'atlas-shop', 'field-notes', 'orbit-docs'])
  })
  it('vecchio da tre minuti; senza published_at (relay precedente alla 1.43) l\'età si conta da ts', () => {
    expect(freshness({ ts: 1000 }, 1179)).toEqual({ stale: false })
    expect(freshness({ ts: 1000 }, 1180)).toEqual({ stale: true, minutes: 3 })
    expect(freshness({ ts: 1000 }, 1000 + 65 * 60 + 5)).toEqual({ stale: true, minutes: 65 })
  })
  it('1.43: l\'età si conta dalla pubblicazione; PC lento quando la raccolta ha preso 30 s o più', () => {
    expect(freshness({ ts: 1000, published_at: 1005 }, 1005 + 10)).toEqual({ stale: false })
    expect(freshness({ ts: 1000, published_at: 1045 }, 1045 + 10)).toEqual({ stale: false, slowS: 45 })
    expect(freshness({ ts: 1000, published_at: 1010 }, 1010 + 200)).toEqual({ stale: true, minutes: 3 })
    expect(publishedTs({ ts: 1000, published_at: 1045.7 })).toBe(1045)
    expect(publishedTs({ ts: 1000 })).toBe(1000)
  })
  it('una lettura in volo da più di 10 s, con la chat già piena, dice da quanto aspetta il PC', () => {
    expect(waitingPc(true, null, 50_000)).toBeNull()
    expect(waitingPc(true, 40_000, 50_000)).toBeNull()
    expect(waitingPc(true, 39_000, 50_000)).toBe(11)
    expect(waitingPc(false, 0, 50_000)).toBeNull()
  })
  it('le durate', () => {
    expect(since(100, 130)).toBe(`0${NB}m`); expect(since(100, 250)).toBe(`2${NB}m`)
    expect(since(0, 65 * 60)).toBe(`1${NB}h${NB}05`); expect(since(0, 3 * 86400 + 100)).toBe(`3${NB}g`)
  })
})
