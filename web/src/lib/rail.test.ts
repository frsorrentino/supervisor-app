import { describe, expect, it } from 'vitest'
import { glyphOf, span } from './rail'
import type { Session } from './contract'

describe('la colonna dei minuti', () => {
  it('sotto l\'ora conta i minuti interi', () => {
    expect(span(1000, 1000)).toEqual({ n: 0, unit: 'min' })
    expect(span(1000, 1000 + 12 * 60 + 59)).toEqual({ n: 12, unit: 'min' })
    expect(span(1000, 1000 + 59 * 60)).toEqual({ n: 59, unit: 'min' })
  })
  it('dall\'ora in su le ore, da un giorno i giorni', () => {
    expect(span(0, 60 * 60)).toEqual({ n: 1, unit: 'h' })
    expect(span(0, 23 * 3600 + 3599)).toEqual({ n: 23, unit: 'h' })
    expect(span(0, 25 * 3600)).toEqual({ n: 1, unit: 'd' })
  })
  it('un orologio avanti non dà minuti negativi', () => {
    expect(span(2000, 1000)).toEqual({ n: 0, unit: 'min' })
  })
})

describe('il glifo di una sessione', () => {
  const s = (x: Partial<Session>) => ({ name: 'x', account: 'personale', ...x }) as Session
  it('chi ti aspetta ha il punto di domanda, anche con un turno in corso', () => {
    expect(glyphOf(s({ state: 'waiting' }))).toBe('waiting')
    expect(glyphOf(s({ state: 'busy', needs_you: { reason: 'login', at: 100 } }))).toBe('waiting')
  })
  it('al lavoro il play, anche in attesa di un processo; ferma e chiusa col tratteggio', () => {
    expect(glyphOf(s({ state: 'busy' }))).toBe('busy')
    expect(glyphOf(s({ state: 'awaiting' }))).toBe('busy')
    expect(glyphOf(s({ state: 'idle' }))).toBe('still')
    expect(glyphOf(s({ state: 'gone' }))).toBe('still')
  })
  it('chi ha finito e non l\'hai ancora visto ha la spunta verde', () => {
    expect(glyphOf(s({ state: 'idle' }), 'finished')).toBe('done')
    expect(glyphOf(s({ state: 'idle' }), 'still')).toBe('still')
  })
})
