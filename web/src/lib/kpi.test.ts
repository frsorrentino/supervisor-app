import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type TimelineEvent, type TimelinePage } from './contract'
import { cells, day, dayFigures, hourly, workSeconds } from './kpi'

const read = (f: string) => readFileSync(new URL(`../../../contract/${f}`, import.meta.url), 'utf8')
const sample = JSON.parse(read('cmd-result-sample.json'))
const tid = sample.cmd.find((c: { op: string }) => c.op === 'timeline').id
const page: TimelinePage = JSON.parse(sample.result.find((r: { id: string }) => r.id === tid).text)
const now = 1789210000
const ev = (at: number, kind = 'prompt'): TimelineEvent => ({ at, kind, text: 'x' })
const of = (name: string) => page.sessions.find(s => s.session === name)!.events

describe('KPI coi dati già nel relay (proposte 6, 11, 15, 18)', () => {
  it('kpi-6: le ultime sei ore, un conto per ora, la più vecchia prima', () => {
    expect(hourly(of('ledger-api'), now)).toEqual([0, 0, 0, 0, 2, 4])
    expect(hourly(of('field-notes'), now)).toEqual([0, 0, 0, 0, 0, 6])
    expect(hourly([ev(now - 7 * 3600), ev(now + 60)], now)).toEqual([0, 0, 0, 0, 0, 0])
  })

  it('kpi-11: lavoro dove gli eventi sono vicini, ferma in mezzo, ti aspetta solo per il tratto in corso', () => {
    const from = now - 4 * 3600
    const events = [ev(from + 600), ev(from + 900), ev(from + 1500), ev(from + 3 * 3600)]
    const spans = day(events, from, now, null)
    expect(spans.map(s => s.band)).toEqual(['still', 'work', 'still', 'work', 'still'])
    expect([spans[1].from, spans[1].to]).toEqual([from + 600, from + 1500])
    expect(workSeconds(spans)).toBe(900 + 600)
    const waiting = day(events, from, now, now - 1800)
    expect(waiting.map(s => s.band)).toEqual(['still', 'work', 'still', 'work', 'still', 'wait'])
    expect([waiting.at(-1)!.from, waiting.at(-1)!.to]).toEqual([now - 1800, now])
    for (let i = 1; i < waiting.length; i++) expect(waiting[i].from).toBe(waiting[i - 1].to)
  })

  it('kpi-15: sei caselle, ambra nell\'ora in corso per chi aspetta adesso', () => {
    expect(cells([ev(now - 4000)], now, true)).toEqual(['still', 'still', 'still', 'still', 'work', 'wait'])
    expect(cells([ev(now - 4000)], now, false)).toEqual(['still', 'still', 'still', 'still', 'work', 'still'])
  })

  it('kpi-18: la giornata in cifre, e un minimo al tetto di 40 eventi', () => {
    const st = decodeState(read('state-2-idle.json'))
    const f = dayFigures(page, st, now, 'Europe/Rome')
    expect([f.prompts, f.commits, f.atLeast]).toEqual([7, 1, false])
    expect(f.tokens).toBe(st.usage_today!.reduce((n, u) => n + u.tokens, 0))
    expect(f.topSession).toBe('atlas-shop')
    expect(f.perHour.reduce((n, [p]) => n + p, 0)).toBe(7)
    const full = { ...page, sessions: [{ ...page.sessions[0], events: Array.from({ length: 40 }, () => ev(now - 100)) }] }
    expect(dayFigures(full, st, now, 'Europe/Rome').atLeast).toBe(true)
  })
})
