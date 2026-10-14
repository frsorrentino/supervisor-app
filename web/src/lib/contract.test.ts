import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { blankState, decodeState, tailKey, withTail } from './contract'

const fixture = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')

describe('contratto', () => {
  it('legge i tre stati delle fixture', () => {
    for (const n of ['state-1-question.json', 'state-2-idle.json', 'state-3-stale.json']) {
      const s = decodeState(fixture(n))
      expect(s.v).toBe(1)
      expect(Array.isArray(s.sessions)).toBe(true)
    }
  })

  it('porta i ricorrenti della 1.33 e i dispositivi della 1.32', () => {
    const s = decodeState(fixture('state-2-idle.json'))
    expect(s.recurring?.map(r => r.id)).toEqual(['x-posts', 'release-changelog', 'plugin-rivals'])
    expect(s.devices?.map(d => d.kind)).toEqual(['phone', 'watch', 'tablet', 'chromebook', null, 'web'])
  })

  it('porta consiglio, approvazioni e sessioni finite della 1.37, con i default quando mancano', () => {
    const s = decodeState(fixture('state-1-question.json'))
    const atlas = s.sessions.find(x => x.name === 'atlas-shop')!
    expect(atlas.advice).toMatchObject({ model: 'claude-fable-5-1', effort: 'high', switch_cost_tokens: 36000, when: 'next_task', differs: true })
    expect(s.sessions.find(x => x.name === 'field-notes')!.finished).toBe(true)
    expect(atlas.finished).toBe(false)
    expect(atlas.duplicate_of).toBeNull()
    expect(s.sessions.find(x => x.name === 'field-notes')!.advice).toBeNull()
    expect(s.approvals.map(a => [a.task, a.deploy])).toEqual([['atlas-release-2-4', true]])
    expect(decodeState(fixture('state-3-stale.json')).approvals).toEqual([])
    expect(s.ops).toEqual(expect.arrayContaining(['approve', 'decision']))
  })

  it('porta i Prossimi della 1.38, con le voci che sbloccano', () => {
    const s = decodeState(fixture('state-1-question.json'))
    expect(s.sessions.find(x => x.name === 'atlas-shop')!.next_steps).toEqual([
      { text: 'ok to deploy on staging', blocking: true }, { text: 'review the test seeds', blocking: false },
    ])
    expect(s.sessions.find(x => x.name === 'field-notes')!.next_steps).toBeUndefined()
  })

  it('rifiuta quello che non è uno stato', () => {
    expect(() => decodeState('{"x":1}')).toThrow()
  })
})

describe('lo stato prima del primo /state del relay (supervisor, 13/10 20:48)', () => {
  it('vuoto, mai i dati di prova: nessuna sessione, quota, recap o dispositivo', () => {
    const s = blankState(1789210000)
    expect(s.ts).toBe(1789210000)
    expect(s.sessions).toEqual([])
    expect(s.quota).toEqual({})
    expect(s.recap.items).toEqual([])
    expect(s.devices ?? []).toEqual([])
  })
})

describe('chat da /tail (contratto 1.59)', () => {
  const e = (id: string) => ({ id, role: 'assistant', text: id, at: 1 })
  const tail = { session: 'atlas-shop', id: 's1', at: 100, entries: [e('b'), e('c'), e('d')], more: true }
  it('si aggancia alla copia o la sostituisce', () => {
    expect(withTail([e('a'), e('b')], tail).map(x => x.id)).toEqual(['a', 'b', 'c', 'd'])
    expect(withTail([e('x')], tail).map(x => x.id)).toEqual(['b', 'c', 'd'])
    expect(withTail([e('x')], { ...tail, entries: [] }).map(x => x.id)).toEqual(['x'])
    expect(tailKey('abc.def#g$h[i]j/k')).toBe('abc_def_g_h_i_j_k')
  })
})
