import { describe, expect, it } from 'vitest'
import { count, groups, tonight } from './outside'
import type { ForYouRow } from './masterHome'
import type { Session } from './contract'

// Gli stessi casi di OutsideSessionsTest e TonightCardTest in Kotlin.
const row = (kind: ForYouRow['kind'], title = kind): ForYouRow => ({ kind, title })
const gone = (name: string) => ({ name, state: 'gone' }) as Session

describe('OutsideSessions', () => {
  it('le categorie dalla più urgente, le vuote no', () => {
    const g = groups([row('next_step'), row('context'), row('night'), row('night_report')], [gone('x')])
    expect(g.map(x => x.category)).toEqual(['fix', 'resume', 'night', 'closed'])
    expect(g[2].rows.map(r => r.kind)).toEqual(['night_report', 'night'])
    expect(count(g[3])).toBe(1)
  })
  it('domande e turni finiti restano fra le sessioni', () => expect(groups([row('question'), row('finished')], [])).toEqual([]))
})

describe('TonightCard', () => {
  it('quello in corso per primo, con i minuti; gli altri in ordine di arrivo', () => {
    const m = tonight({ queued: 3, items: [
      { id: 'b', dir: '/b', name: 'b', prompt: ' due ', added: 200 },
      { id: 'a', dir: '/a', name: 'a', prompt: 'uno', added: 100 },
      { id: 'c', dir: '/c', name: 'c', prompt: 'tre', added: 300, started: 1000 },
    ] }, 1000 + 125)
    expect(m.items.map(i => [i.id, i.runningMin])).toEqual([['c', 2], ['a', null], ['b', null]])
    expect(m.items[2].prompt).toBe('due')
    expect([m.running, m.queued]).toEqual([1, 2])
  })
})
