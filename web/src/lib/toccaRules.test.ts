// Contratto 1.113 (app-pulizia-tocca-03-11), gli stessi casi di ToccaRulesTest in Kotlin sulla fixture del relay.
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { closedToday, toccaCounts } from './toccaRules'

const s = decodeState(readFileSync(new URL('../../../contract/state-2-idle.json', import.meta.url), 'utf8'))

describe('Tocca a te, un conto solo', () => {
  it('il conto viene dal relay', () => {
    const t = s.tocca!
    expect(t.prove + t.decidi).toBe(t.n)
    expect(toccaCounts({ ...s, decisions: { n: 99, here: 0, tasks: 0, snoozed: 0 }, prove: { n: 98, builds: 0, red: 0, ko: 0 } })).toEqual({ decidi: t.decidi, prove: t.prove })
  })
  it('con un relay precedente restano i conti delle pagine', () => {
    expect(toccaCounts({ ...s, tocca: undefined, decisions: { n: 3, here: 0, tasks: 0, snoozed: 0 }, prove: { n: 2, builds: 0, red: 0, ko: 0 } })).toEqual({ decidi: 3, prove: 2 })
  })
  it('le chiuse di oggi', () => {
    const td = closedToday(s)!
    expect(td.n).toBeGreaterThanOrEqual(td.items.length)
    expect(new Set(td.items.map(x => x.kind))).toEqual(new Set(['pause', 'prova']))
    expect(new Set(td.items.map(x => x.rule))).toEqual(new Set([1, 2]))
    expect(closedToday({ ...s, tocca: { ...s.tocca!, today: null } })).toBeNull()
    expect(closedToday({ ...s, tocca: undefined })).toBeNull()
  })
})
