// Gli stessi casi di SearchViewTest in Kotlin, sul search «tuesday» della fixture: 3 voci di orbit-docs chiusa, 2 di field-notes viva.
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { withConversations } from './search'
import { dayStart, FILTER, groups, RECENT_MAX, remember, whenOf, type Recent } from './searchView'

const root = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8'))
const id = root.cmd.find((c: { op: string }) => c.op === 'search').id
const hits = withConversations([], JSON.parse(root.result.find((r: { id: string }) => r.id === id).text))

describe('pagina Cerca', () => {
  it('raggruppa per sessione, dal più recente', () => {
    const g = groups(hits, FILTER, 0)
    expect(g.map(x => x.session)).toEqual(['orbit-docs', 'field-notes'])
    expect(g.map(x => x.live)).toEqual([false, true])
    expect(g.map(x => x.hits.length)).toEqual([3, 2])
    expect(g[0].hits[0].entry).toBe('c5.0')
  })
  it('filtra per sessione, giorno e chiuse', () => {
    expect(groups(hits, { ...FILTER, closed: false }, 0).map(x => x.session)).toEqual(['field-notes'])
    expect(groups(hits, { ...FILTER, session: 'field-notes' }, 0).map(x => x.hits.length)).toEqual([2])
    expect(groups(hits, { ...FILTER, today: true }, 1789286400).map(x => x.session)).toEqual(['orbit-docs'])
    expect(groups(hits, { ...FILTER, today: true, closed: false }, 1789286400)).toEqual([])
  })
  it('le recenti salgono e restano sei', () => {
    let r: Recent[] = [{ q: 'deploy 2.8' }, { q: 'Tuesday', session: 'orbit-docs' }]
    r = remember(r, { q: '  tuesday ' })
    expect(r.map(x => x.q)).toEqual(['tuesday', 'deploy 2.8'])
    for (let i = 1; i <= 10; i++) r = remember(r, { q: `q${i}` })
    expect(r.length).toBe(RECENT_MAX); expect(r[0].q).toBe('q10')
    expect(remember(r, { q: '   ' })).toBe(r)
  })
  it("l'ora come nel mockup", () => {
    const now = 1789286460
    expect(whenOf(now, now, 'UTC')).toEqual({ kind: 'time', hhmm: '08:01' })
    expect(whenOf(now - 86_400, now, 'UTC')).toEqual({ kind: 'yesterday' })
    expect(whenOf(now - 3 * 86_400, now, 'UTC')).toEqual({ kind: 'weekday', day: 4 })
    expect(whenOf(now - 7 * 86_400, now, 'UTC')).toEqual({ kind: 'date', ddmm: '06/09' })
    expect(dayStart(now, 'UTC')).toBe(1789257600)
  })
})
