import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import type { Subagents } from './contract'
import { duration, more, parseAnswer, rows, summary, tokens } from './subagents'

// Gli stessi casi di SubagentRulesTest.kt (app-sottoagenti-23-11), sulle fixture vere del relay (contratto 1.158).
const root = JSON.parse(readFileSync(new URL('../../../contract/subagents-sample.json', import.meta.url), 'utf8'))
const win = root.win as Subagents
const wf = root.workflow as Subagents

describe('i sottoagenti di una sessione', () => {
  it('il riassunto conta come finiti tutti gli altri', () => {
    expect(summary(win)).toEqual({ running: 0, done: 3 })
    expect(summary(wf)).toEqual({ running: 1, done: 6 })
  })
  it('le righe di una sessione di win', () => {
    const r = rows(win, 1795436000)
    expect(r.map(x => x.title)).toEqual(['Fixture sottoagente in corso', 'Fixture sottoagente background', 'Fixture sottoagente primo piano'])
    expect(r[2]).toEqual({ id: 'a76eb4828a2262868', title: 'Fixture sottoagente primo piano', running: false, type: 'Explore', model: 'Haiku 5.5', bg: true, phase: null, tokens: [69142, 589], seconds: 5 })
    expect(more(win)).toBeNull()
  })
  it('un agente di Workflow al lavoro e un fork senza trascrizione', () => {
    const r = rows(wf, 1795436100)
    expect(r[0]).toEqual({ id: 'b000000000000000c', title: 'fixture wf 3', running: true, type: 'workflow-subagent', model: 'Haiku 5.5', bg: false, phase: 'Review', tokens: null, seconds: 178 })
    expect(r[1]).toEqual({ id: 'a1f3685a786f72627', title: 'fixture 1', running: false, type: 'fork', model: 'inherit', bg: true, phase: null, tokens: null, seconds: null })
    expect(more(wf)).toBe(7)
  })
  it("la risposta dell'op", () => {
    expect(parseAnswer(JSON.stringify(root.op))?.items).toHaveLength(3)
    expect(parseAnswer(JSON.stringify(root.op_none))).toBeNull()
    expect(parseAnswer('nessuna sessione x')).toBeNull()
  })
  it('i numeri corti', () => {
    expect([69142, 211219, 589, 200, 1_234_567, 6_900, 1_000].map(n => tokens(n))).toEqual(['69k', '211k', '0,6k', '0,2k', '1,2M', '6,9k', '1k'])
    expect(tokens(589, true)).toBe('0.6k')
    expect([18, 240, 3900].map(duration)).toEqual(['18 s', '4 m', '1 h 05'])
  })
})
