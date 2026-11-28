import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { Approval, Question, WaitItem } from './contract'
import { damping, FIRE_MS, LEAVE_MS, holdIndex, holdOption, holdMs, holds, kick, modeOf, placeOf, progress, publishes, refuses, release, RELEASE, type Place } from './tieniPremuto'

// Il tasto «tieni premuto» (app-tieni-premuto-28-11): gli stessi casi di TieniPremutoTest.kt, dallo stesso file per le carte.
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/tieni-premuto-cases.json', import.meta.url), 'utf8')) as {
  publishes: { text: string; out: boolean }[]
  cards: { name: string; item: WaitItem; hold: number | null; places: (Place | null)[] }[]
  approvals: { approval: Approval; out: boolean }[]
  questions: { name: string; question: Question; out: number | null }[]
}

describe('tieni premuto: i tempi e il movimento', () => {
  it('ogni posto ha il suo tempo, i posti a un tocco nessuno', () => {
    expect((['chiudi', 'riparti', 'pubblica', 'approva', 'va'] as Place[]).map(holdMs)).toEqual([1200, 1000, 1500, 0, 0])
  })
  it('si tiene premuto solo nei tre posti di Franz, ogni altro ok e ogni Va sono un tocco', () => {
    expect((['chiudi', 'riparti', 'pubblica', 'approva', 'va'] as Place[]).map(modeOf)).toEqual(['hold', 'hold', 'hold', 'tap', 'tap'])
  })
  it('il pieno cresce lineare col tempo tenuto', () => {
    expect([progress(0, 1200), progress(300, 1200), progress(600, 1200), progress(1800, 1200), progress(-40, 1200), progress(0, 0)]).toEqual([0, 0.25, 0.5, 1, 0, 1])
  })
  it('lasciato prima non parte niente, al tempo pieno parte', () => {
    expect([release(1199, 1200), release(1200, 1200), release(1400, 1500), release(1000, 1000)]).toEqual(['early', 'complete', 'early', 'complete'])
  })
  it('la scossa del no cresce col pieno fino a 260 e sparisce col moto ridotto', () => {
    expect(kick(0)).toBeCloseTo(78)
    expect(kick(0.2)).toBeCloseTo(182)
    expect(kick(0.35)).toBeCloseTo(260)
    expect(kick(0.9)).toBe(260)
    expect(kick(-0.1)).toBeCloseTo(78)
    expect(kick(0.5, true)).toBe(0)
  })
  it('col moto ridotto le molle arrivano senza rimbalzo', () => {
    expect(damping(...RELEASE, false)).toBe(14)
    expect(damping(...RELEASE, true)).toBeCloseTo(2 * Math.sqrt(260))
    expect(FIRE_MS).toBe(610)
    expect(LEAVE_MS).toBe(800)
  })
})

describe('tieni premuto: quale ok pubblica', () => {
  for (const c of file.publishes) it(`${c.out ? 'pubblica' : 'non pubblica'}: ${c.text}`, () => expect(publishes([c.text])).toBe(c.out))
  for (const c of file.cards) it(c.name, () => {
    expect(holdIndex(c.item)).toBe(c.hold)
    expect((c.item.actions ?? []).map((_, i) => placeOf(c.item, i))).toEqual(c.places)
  })
  for (const c of file.questions) it(`una domanda: ${c.name}`, () => expect(holdOption(c.question)).toBe(c.out))
  for (const c of file.approvals) it(`un ok dello stato: ${c.approval.title}`, () => expect(holds(c.approval)).toBe(c.out))
  it('un no o un rimanda non diventano mai il tasto da tenere premuto', () => {
    expect(['no · aspetta l\'ok del cliente', '2 · Non ora', 'Tomorrow morning', 'domattina'].map(refuses)).toEqual([true, true, true, true])
    expect(['sì · Deploy 2.8.0 in produzione adesso', 'Release now', 'Approva'].map(refuses)).toEqual([false, false, false])
  })
  it('una carta senza tasti, dall\'archivio, non ha posti', () => {
    const w = { id: 'w', label: 'decide', title: 'Pubblica la 2.4', actions: null } as unknown as WaitItem
    expect(holdIndex(w)).toBeNull()
    expect(placeOf(w, 0)).toBeNull()
    expect(publishes([null, 'Pubblica la 2.4'])).toBe(true)
    expect(publishes([null, undefined])).toBe(false)
  })
})
