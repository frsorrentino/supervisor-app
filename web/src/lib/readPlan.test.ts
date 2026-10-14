import { describe, expect, it } from 'vitest'
import { shouldRead, due, activity, behind } from './readPlan'

// Piano prestazioni, Task 5 (A4a): la stessa cadenza del telefono (PhoneTerminal.kt). Il 07/10 la web chiedeva 888
// letture all'ora, una per sessione a ogni stato.
describe('cadenza delle letture', () => {
  it('la prima lettura parte subito', () => expect(shouldRead({ state: 'idle' }, undefined, false, 100)).toBe(true))
  it('ferma: una al minuto', () => {
    expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, false, 159)).toBe(false)
    expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, false, 160)).toBe(true)
  })
  it('al lavoro o in attesa del polso: ogni 10 s', () => {
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: true }, false, 109)).toBe(false)
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: true }, false, 110)).toBe(true)
    expect(shouldRead({ state: 'awaiting' }, { at: 100, answered: true }, false, 110)).toBe(true)
  })
  it('stato della sessione cambiato: subito', () => expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, true, 101)).toBe(true))
  it('lettura senza risposta: si riprova dopo 21 s, anche se lo stato è cambiato', () => {
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: false }, true, 120)).toBe(false)
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: false }, true, 121)).toBe(true)
  })
  it('una sessione chiusa non si legge più', () => expect(shouldRead({ state: 'gone' }, { at: 100, answered: true }, true, 500)).toBe(false))
})

describe('rilettura in sottofondo (Franz, 14/10 18:39: «vedo per qualche secondo il testo vecchio»)', () => {
  const s = { name: 'atlas-shop', state: 'busy', turn_started: 100, tool: 'Bash', tool_note: null, question: null, outcome: null }
  const read = { sig: activity(s), atMs: 1_000, done: true }
  it('legge chi non è mai stato letto o ha attività nuova, al massimo ogni 12 s, mai a schermo né chiusa', () => {
    expect(due([s], {}, [null], 1_000)).toEqual(['atlas-shop'])
    expect(due([s], { 'atlas-shop': read }, [], 60_000)).toEqual([])
    const next = { ...s, tool: 'Edit' }
    expect(due([next], { 'atlas-shop': read }, [], 12_999)).toEqual([])
    expect(due([next], { 'atlas-shop': read }, [], 13_000)).toEqual(['atlas-shop'])
    expect(due([{ ...s, state: 'idle', tool: null, outcome: { at: 200 } }], { 'atlas-shop': read }, [], 13_000)).toEqual(['atlas-shop'])
    expect(due([next], {}, ['atlas-shop'], 13_000)).toEqual([])
    expect(due([{ ...next, state: 'gone' }], {}, [], 13_000)).toEqual([])
  })
  it('la copia è attuale solo se la lettura è arrivata e nessuna attività è venuta dopo', () => {
    expect(behind(s, read)).toBe(false)
    expect(behind({ ...s, tool: 'Edit' }, read)).toBe(true)
    expect(behind(s, { ...read, done: false })).toBe(true)
    expect(behind(s, undefined)).toBe(true)
  })
})
