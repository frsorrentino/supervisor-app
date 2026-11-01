import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { catalog, grouped, missingSections, tappable } from './features'
import { DEFAULT_ORDER } from './homeOrder'

const ROUTES = ['home', 'queue', 'box-aspetta', 'box-lavoro', 'box-esiti', 'lavori', 'launch', 'night', 'search', 'settings', 'homeorder', 'usage']
const NEXT = ['banco_prova', 'cinque_minuti', 'ok_informato', 'rimasto_indietro', 'stesso_difetto', 'promemoria', 'rendiconto', 'pagella', 'panoramica', 'prima_raccolta']
const read = (f: string) => readFileSync(new URL(`../../../${f}`, import.meta.url), 'utf8')

describe('panoramica', () => {
  it('panoramica: ogni sezione della home ha la sua voce', () => {
    expect(missingSections(catalog, DEFAULT_ORDER)).toEqual([])
    expect(missingSections(catalog, ['nuova'])).toEqual(['nuova'])
  })
  it('panoramica: ogni voce che c\'è ha la prova nel codice', () => {
    for (const f of catalog.features.filter(x => x.state === 'ready')) {
      expect(f.phone ?? f.web, f.key).toBeTruthy()
      for (const p of [f.phone, f.web]) if (p) expect(read(p.file).includes(p.needle), `${f.key}: ${p.file} ${p.needle}`).toBe(true)
    }
  })
  it('panoramica: le dieci del programma sono in arrivo e non portano da nessuna parte', () => {
    const coming = catalog.features.filter(f => f.state === 'coming')
    expect(coming.map(f => f.key).sort()).toEqual([...NEXT].sort())
    expect(coming.some(tappable)).toBe(false)
  })
  it('panoramica: chiavi uniche, rotte conosciute, gruppi senza orfani, testi nelle due lingue', () => {
    const keys = catalog.features.map(f => f.key)
    expect(new Set(keys).size).toBe(keys.length)
    for (const f of catalog.features) {
      if (f.route) expect(ROUTES, f.key).toContain(f.route)
      expect(catalog.groups.map(g => g.key), f.key).toContain(f.group)
      expect(f.name.it && f.name.en && f.what.it && f.what.en, f.key).toBeTruthy()
    }
    expect(grouped(catalog).reduce((n, g) => n + g.rows.length, 0)).toBe(keys.length)
  })
})
