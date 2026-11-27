import { describe, expect, it } from 'vitest'
import { arrivalGate } from './arrivo'

// Franz 27/11 15:17: arriva con un'animazione solo quello che nasce a chat aperta e ferma (come ArrivoRispostaTest.kt).
const open = (from = 1000) => { const g = arrivalGate(); g.settle(); g.working('busy', from); return g }

describe('arrivalGate', () => {
  it('passa solo la risposta arrivata dopo l\'apertura, una volta', () => {
    const g = open()
    expect(g.claim('a', 1005)).toBe(false)
    g.prime(['b'])
    expect(g.claim('b', 1005)).toBe(false)
    expect(g.claim('c', 1005)).toBe(true)
    expect(g.claim('c', 1005)).toBe(false)
  })
  it('una risposta nata prima del turno visto aperto, o senza ora, non arriva', () => {
    const g = open(1000)
    g.prime([])
    expect(g.claim('old', 990)).toBe(false)
    expect(g.claim('noat', null)).toBe(false)
    expect(g.claim('new', 1005)).toBe(true)
  })
  it('niente prima che la chat sia ferma, né senza lavoro visto a chat ferma', () => {
    const g = arrivalGate()
    g.prime([])
    g.working('busy', 1000)
    expect(g.claim('a', 1005)).toBe(false)
    g.settle()
    expect(g.live).toBe(false)
    expect(g.claim('b', 1005)).toBe(false)
    g.working('idle', 1000)
    expect(g.live).toBe(false)
    g.working('awaiting', 1000)
    expect(g.live).toBe(true)
    expect(g.claim('a', 1005)).toBe(false)
    expect(g.claim('c', 1005)).toBe(true)
  })
  it('il confine è il primo turno visto', () => {
    const g = arrivalGate()
    g.prime([])
    g.settle()
    g.working('busy', null)
    expect(g.live).toBe(false)
    g.working('busy', 1000)
    g.working('busy', 2000)
    expect(g.claim('x', 999)).toBe(false)
    expect(g.claim('y', 1500)).toBe(true)
  })
})
