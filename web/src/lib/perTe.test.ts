import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { ResultGroup, WaitAction, WaitItem } from './contract'
import { groupOf, mainOf, parseBlocks, voci } from './perTe'

// Gli stessi casi di PerTeTest.kt.
const file = JSON.parse(readFileSync(new URL('../../../core/src/test/resources/per-te-cases.json', import.meta.url), 'utf8')) as {
  main: { name: string; actions: Pick<WaitAction, 'label' | 'style' | 'recommended' | 'text_field'>[]; out: unknown }[]
  voci: { name: string; session: string; waits: Partial<WaitItem>[]; results: { session: string; unseen: boolean; id: string; title: string; at: number }[]; out: unknown[] }[]
}
const group = (r: { session: string; unseen: boolean; id: string; title: string; at: number }): ResultGroup => ({
  title: r.title, session: r.session, kind: 'document', at: r.at, versions: 1, unseen: r.unseen,
  items: [{ id: r.id, title: r.title, path: `out/${r.id}.md`, kind: 'document', at: r.at, version: 1, versions: 1, session: r.session, seen_at: r.unseen ? null : r.at }] as ResultGroup['items'],
})

describe('formato unico: il tasto sulla voce', () => {
  for (const c of file.main) it(c.name, () => expect(mainOf(c.actions)).toEqual(c.out))
  it('senza elenco di tasti (un relay di prima)', () => expect(mainOf(null)).toBeNull())
})

describe('formato unico: le voci della sessione', () => {
  for (const c of file.voci) it(c.name, () => {
    const got = voci(c.waits as WaitItem[], c.results.map(group), c.session)
    expect(got.map(v => ({ key: v.key, group: v.group, title: v.title, context: v.context }))).toEqual(c.out)
  })
  it('una verifica porta il risultato da segnare visto, una richiesta sé stessa', () => {
    const got = voci([{ id: 'w1', label: 'reply', session: 'a', text: 'Sì?', at: 1, actions: [{ label: 'sì', style: 'option', cmd: { op: 'answer' } }] } as WaitItem], [group({ session: 'a', unseen: true, id: 'r1', title: 'Rapporto', at: 2 })], 'a')
    expect(got[0].wait?.id).toBe('w1'); expect(got[0].main).toEqual({ index: 0, label: '1 · sì', direct: true }); expect(got[0].result).toBeNull()
    expect(got[1].result).toEqual({ id: 'r1', kind: 'document', version: null }); expect(got[1].wait).toBeNull()
  })
  it('le etichette', () => expect(['reply', 'decide', 'choose', 'verify', 'altro'].map(groupOf)).toEqual(['aspetta', 'decidi', 'decidi', 'prova', 'decidi']))
})

describe('i blocchi ridotti', () => {
  it('chi non ha mai scelto, o una memoria rotta: niente di aperto', () => {
    expect(parseBlocks(null)).toEqual({}); expect(parseBlocks('x')).toEqual({}); expect(parseBlocks('[1]')).toEqual({})
    expect(parseBlocks('{"perte":true}')).toEqual({ perte: true })
  })
})
