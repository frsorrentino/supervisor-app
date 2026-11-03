import { describe, expect, it } from 'vitest'
import { altrove, decision, prima, prove, proveKo, rimasto, shown, split, suggestion } from './itemRows'
import type { DecisionGroup, ProveItem } from './contract'

// app-richiede-te-righe-02-11: gli stessi casi di ItemRowsTest.kt.
const g = (o: Partial<DecisionGroup>): DecisionGroup => ({ key: 'k', title: '', items: [], unblocks: [], weight: 0, since: 0, refs: [], ...o })
const p: ProveItem = { id: 7, task: 'app-x', short: 'Zoom', session: 'supervisor-app', build: 'zoom-punto', fai: "Pizzica un'immagine", atteso: '', at: 0 }

describe('le voci delle sezioni come righe', () => {
  it('una decisione da qui è ambra e dice cosa sblocca', () => {
    const r = decision(g({ key: 'k1', title: 'README di fable-director', items: [{ kind: 'ok', task: 't1', reason: 'Ritocchi: versione 1.61', since: 0 }], weight: 2, where: 'here' }))
    expect(r.name).toBe('README di fable-director')
    expect(r.sub).toBe('Ritocchi: versione 1.61')
    expect(r.tone).toBe('wait')
    expect(r.right).toEqual([{ kind: 'here' }, { kind: 'unblocks', n: 2 }])
  })
  it('una decisione al PC è grigia e sblocca solo da due', () => {
    const r = decision(g({ key: 'k2', title: 'Studio da loggato', items: [{ kind: 'pause', reason: "Prova con l'impronta", since: 0 }], weight: 1, where: 'pc' }))
    expect(r.tone).toBe('still')
    expect(r.right).toEqual([{ kind: 'at_pc' }])
  })
  it('una prova è ambra solo con la versione installata', () => {
    expect(prove(p, true).tone).toBe('wait')
    expect(prove(p, true).sub).toBe("Pizzica un'immagine")
    // A destra la sessione, non la versione: quella è la testata del gruppo.
    expect(prove(p, true).right).toEqual([{ kind: 'product', text: 'supervisor-app' }])
    expect(prove(p, false).tone).toBe('still')
    expect(prove(p, false).right.map(x => x.kind)).toEqual(['to_install', 'product'])
    expect(proveKo({ id: 8, task: 'app-y', build: '', fai: '', atteso: '', at: 0, text: 'Non si apre' }).tone).toBe('red')
  })
  it('da seguire prende il colore dal suo stato', () => {
    expect(rimasto({ product: 'sito', key: 'k', title: 'Pagina prezzi', stale: true }).tone).toBe('wait')
    expect(rimasto({ product: 'sito', key: 'k', title: '', stale: false }).tone).toBe('good')
    expect(rimasto({ product: 'sito', key: 'k', title: '', stale: null }).name).toBe('k')
    const run = { id: 'a', text: 'Cerca X', complete: false, total: 0, done: 0, positive: [] as string[], errors: [] as string[], running: false }
    expect(altrove({ ...run, running: true, positive: ['p'] }).tone).toBe('busy')
    expect(altrove({ ...run, positive: ['repo-1'] }).tone).toBe('wait')
    expect(altrove({ ...run, positive: ['repo-1'] }).sub).toBe('repo-1')
    expect(altrove({ ...run, total: 5, done: 3 }).right).toEqual([{ kind: 'checked', text: '3/5' }])
  })
  it('una sezione della prima raccolta è verde se raccolta e dice perché se vuota', () => {
    expect(prima({ key: 'hosts', n: 2, text: 'win e Chromebook' }).tone).toBe('good')
    const e = prima({ key: 'night', n: 0, why: 'Nessuna notte ancora' })
    expect(e.tone).toBe('still')
    expect(e.sub).toBe('Nessuna notte ancora')
  })
  it('un suggerimento si divide in nome e resto', () => {
    const r = suggestion({ id: 's1', kind: 'cleanup', key: '', text: 'Chiudi le tre sessioni ferme. Sono inattive da due giorni.', at: 0 })
    expect(r.name).toBe('Chiudi le tre sessioni ferme.')
    expect(r.sub).toBe('Sono inattive da due giorni.')
    expect(r.tone).toBe('wait')
    const one = suggestion({ id: 's2', kind: 'idea', key: '', text: 'Un testo solo', at: 0 })
    expect(one.name).toBe('Un testo solo')
    expect(one.sub).toBeNull()
    expect(split('Prima riga\n  seconda riga')).toEqual(['Prima riga', 'seconda riga'])
  })
  it('tre righe, il resto dietro un tocco', () => {
    expect(shown(10, false)).toBe(3)
    expect(shown(10, true)).toBe(10)
    expect(shown(4, false)).toBe(4)
  })
})
