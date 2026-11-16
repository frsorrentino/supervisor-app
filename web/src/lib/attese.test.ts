import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { closedIds, counts, isDecide, isVerify, perSession, visible } from './attese'
import type { State, WaitItem } from './contract'

// app-riall-1-dati-16-11 (contratto 1.151): la lista unica di ciò che aspetta la persona. Gli stessi casi di AtteseTest.kt, dalla
// fixture del relay: i conti delle voci sono quelli di `state.tocca` e di `sessions[].tocca`, così il numero sono le righe.
const f = JSON.parse(readFileSync(new URL('../../../contract/attese-rd-sample.json', import.meta.url), 'utf8')) as {
  attese: { rev: string; items: WaitItem[] }; state: Pick<State, 'attese' | 'tocca'> & { sessions: { name: string; tocca: { prove: number; decidi: number; rispondi: number } }[] }
}
const items = f.attese.items

describe('attese_lista_unica', () => {
  it('i conti di tutte le voci sono quelli dello stato', () => {
    const c = counts(items), t = f.state.tocca!
    expect(c).toEqual({ n: t.n, decidi: t.decidi, prove: t.prove, rispondi: t.rispondi })
    expect(f.state.attese!.n).toBe(items.length); expect(f.state.attese!.rev).toBe(f.attese.rev)
  })
  it('i conti di ogni sessione sono quelli della sua tocca', () => {
    for (const s of f.state.sessions) {
      const c = counts(items, s.name)
      expect([c.prove, c.decidi, c.rispondi], s.name).toEqual([s.tocca.prove, s.tocca.decidi, s.tocca.rispondi])
    }
  })
  it('Decidi e Verifica dividono le voci senza resti; le chip per sessione', () => {
    expect(items.filter(isDecide).length + items.filter(isVerify).length + items.filter(w => w.label === 'reply').length).toBe(items.length)
    const ps = perSession(items)
    for (const s of f.state.sessions) expect(ps[s.name], s.name).toEqual({ d: s.tocca.decidi, v: s.tocca.prove })
  })
  it('una voce risposta o chiusa altrove esce, e i conti scendono', () => {
    const one = items[1].id
    const v = visible(items, new Set([one]))
    expect(v.map(w => w.id)).not.toContain(one); expect(counts(v).n).toBe(items.length - 1)
    expect(visible([{ ...items[0], state: 'approved' }], new Set())).toEqual([])
    expect(closedIds({ attese: { rev: 'x', n: 0, closed: [{ id: 'w1', at: 1 }] } })).toEqual(['w1'])
    expect(closedIds({})).toEqual([])
  })
})
