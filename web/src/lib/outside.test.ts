import { describe, expect, it } from 'vitest'
import { count, groups, place, tonight } from './outside'
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

  // Franz 24/10 00:46: «anche questo suggerimento dovrebbe apparire nella scheda della sessione»: una voce con una sessione aperta sta nella sua scheda; in Altro restano le altre.
  it('un suggerimento su una sessione aperta sta nella sua scheda', () => {
    const ctx: ForYouRow = { kind: 'context', title: 'win:bluey', session: 'win:bluey', number: 83 }
    const ctxGone: ForYouRow = { kind: 'context', title: 'old', session: 'old', number: 90 }
    const g = groups([ctx, ctxGone, row('scheduled'), row('next_step')], [gone('old')])
    const p = place(g, new Set(['win:bluey', 'app']))
    expect(p.inCards.get('win:bluey')).toEqual([ctx])
    expect(p.groups.map(x => x.category)).toEqual(['fix', 'scheduled', 'resume', 'closed'])
    expect(p.groups[0].rows).toEqual([ctxGone])
    const only = place(groups([ctx], []), new Set(['win:bluey']))
    expect(only.groups).toEqual([]); expect(only.inCards.get('win:bluey')).toEqual([ctx])
  })
})
