import { describe, expect, it } from 'vitest'
import { answer, canSet, choices, MAX, MIN, OP, valid } from './maxSessions'

// Contratto 1.145; gli stessi casi di MaxSessionsTest.kt.
describe('impostazioni: sessioni al massimo', () => {
  it('si cambia solo se il relay offre l\'op', () => {
    expect(OP).toBe('max_sessions_set')
    expect([canSet(['kpi', 'max_sessions_set']), canSet(['kpi']), canSet(null), canSet(undefined)]).toEqual([true, false, false, false])
  })
  it('da 1 a 20 compresi, solo interi', () => {
    expect([MIN, MAX]).toEqual([1, 20])
    expect([0, 1, 7, 20, 21, 2.5].map(valid)).toEqual([false, true, true, true, false, false])
  })
  it('le scelte: da 1 a 20, più il valore di adesso se è fuori', () => {
    expect(choices(5)).toEqual(Array.from({ length: 20 }, (_, i) => i + 1))
    expect(choices(null)).toHaveLength(20)
    expect(choices(30).at(-1)).toBe(30)
    expect(choices(0)[0]).toBe(0)
  })
  it('la risposta del relay', () => {
    expect(answer('{"max_sessions": 7}')).toBe(7)
    expect([answer('{}'), answer('ok'), answer('{"max_sessions": "7"}')]).toEqual([null, null, null])
  })
})
