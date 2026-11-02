import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeSeguiti } from './decideDecode'
import { decodeState } from './contract'
import { age, canAccept, canAlign, changedPaths, clean, fired, ordered, total, whenText } from './seguiti'
import { known } from './suggestions'
import { STRONG } from './strongAlert'
import { newCmd } from './transport'

// Contratto 1.102, sulle fixture del relay: gli stessi casi di SeguitiTest.kt.
const sample = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')) as { cmd: { id: string; op: string }[]; result: { id: string; text: string }[] }
const id = sample.cmd.find(c => c.op === 'seguiti')!.id
const page = decodeSeguiti(sample.result.find(r => r.id === id)!.text)
const item = page.rimasto[0]
const run = page.altrove[0]

describe('rimasto_indietro', () => {
  it('rimasto_indietro: riga, pagina e perché', () => {
    expect(decodeState(readFileSync(new URL('../../../contract/state-3-stale.json', import.meta.url), 'utf8')).seguiti).toEqual({ rimasto: 2, altrove: 1, promemoria: 1 })
    expect(total({ rimasto: 2, altrove: 1, promemoria: 1 })).toBe(4)
    expect(total(null)).toBe(0)
    expect([item.product, item.key, item.where, item.aligned_version]).toEqual(['atlas-shop', 'demo', 'the demo on the site', 'v2.3.0'])
    expect(age(item)).toBe('behind')
    expect(changedPaths(item)).toBe(3)
  })
  it('rimasto_indietro: non si sa non è allineata', () => {
    const u = { ...item, stale: null, why: 'no_git' }
    expect(age(u)).toBe('unknown')
    expect(changedPaths(u)).toBeNull()
    expect(canAlign(u) && canAccept(u)).toBe(true)
  })
  it('rimasto_indietro: tasti e ordine', () => {
    expect(canAlign(item) && canAccept(item)).toBe(true)
    expect(canAlign({ ...item, task: 'allinea-1' })).toBe(false)
    const fresh = { ...item, key: 'f', stale: false, why: null }
    expect(canAlign(fresh) || canAccept(fresh)).toBe(false)
    const u = { ...item, key: 'u', stale: null, why: 'no_git' }
    expect(ordered([fresh, u, item]).map(x => x.key)).toEqual(['demo', 'u', 'f'])
  })
  it('rimasto_indietro: il tocco porta prodotto e voce, come device', () => {
    const c = newCmd('seguiti_azione', null, null, undefined, undefined, { action: 'allinea', product: 'atlas-shop', key: 'demo' })
    expect([c.action, c.product, c.key]).toEqual(['allinea', 'atlas-shop', 'demo'])
    expect('product' in newCmd('seguiti', null)).toBe(false)
  })
})

describe('stesso_difetto', () => {
  it('stesso_difetto: un errore o un parziale non sono mai un negativo', () => {
    expect(run.positive).toEqual(['personal/atlas-shop'])
    expect(clean(run)).toBe(false)
    expect(clean({ ...run, positive: [] })).toBe(true)
    expect(clean({ ...run, positive: [], errors: ['x'] })).toBe(false)
    expect(clean({ ...run, positive: [], complete: false, done: 5 })).toBe(false)
    expect(clean({ ...run, positive: [], total: 0, done: 0 })).toBe(false)
  })
  it('stesso_difetto: una pagina senza elenchi non rompe', () => {
    const p = decodeSeguiti('{"altrove":[{"id":"a","text":"x","complete":true,"total":1,"done":1,"running":false}]}')
    expect(p.rimasto).toEqual([])
    expect(p.altrove[0].positive).toEqual([])
    expect(p.promemoria_scattati).toEqual([])
  })
})

describe('promemoria', () => {
  it('promemoria: in attesa e scattati', () => {
    expect(page.promemoria[0]).toMatchObject({ id: '7f3e1a20', expires_at: null, at_time: null })
    expect(fired(page.promemoria_scattati[0])).toBe(true)
    expect(fired({ ...page.promemoria_scattati[0], outcome: 'expired' })).toBe(false)
    expect(whenText(0)).toMatch(/^\d\d\/\d\d \d\d:\d\d$/)
  })
  it('promemoria: il tipo nuovo del suggerimento è noto, l\'evento non è un\'interruzione', () => {
    expect(known('reminder')).toBe(true)
    expect(STRONG.has('reminder')).toBe(false)
  })
})
