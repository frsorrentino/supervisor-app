import { describe, expect, it } from 'vitest'
import { newCmd } from './transport'
import type { Cmd, Session, StudioPage } from './contract'
import { backStep, chip, scopeFor, stripScope, studioModel, studioResult, RECENT_S } from './studio'

describe('studio: comando', () => {
  it('lo scope viaggia sul prompt', () => {
    const c: Cmd = { ...newCmd('prompt', 'master', 'ciao'), scope: 'studio' }
    expect(c.scope).toBe('studio')
    expect(newCmd('studio', null).op).toBe('studio')
  })
})

const now = 1760800000
const page: StudioPage = {
  cases: [
    { id: 'a', name: 'Accordo Idemedia', project: 'personali/francescosorrentino.com' },
    { id: 'b', name: 'Permessi 104', project: 'personali/francescosorrentino.com' },
    { id: 'r', name: 'Rino: procedimento penale', project: 'personali/rino' },
    { id: 't', name: 'Termocamino Antonietta', project: 'personali/termocamino' },
  ],
  deadlines: [
    { uid: 'd1', summary: 'Deposito memoria', date: '2026-10-21', days_left: 3, due: true, case: 'r' },
    { uid: 'd2', summary: 'Rata 4 accordo', date: '2026-11-30', days_left: 43, due: false, case: '' },
  ],
  outcomes: [
    { project: 'personali/francescosorrentino.com', session: 'fs', at: now - 3600, short: 'Lettera pronta' },
    { project: 'personali/termocamino', session: 'tc', at: now - RECENT_S - 10, short: 'Vecchio' },
  ],
  error: '',
}
const live = { name: 'fs', project: 'personali/francescosorrentino.com', state: 'idle', since: now - 60 } as Session
const step = (d: { summary: string }) => ({ label: chip('Prepara', d.summary), send: `Prepara: ${d.summary}` })

describe('studioModel', () => {
  it('scadenziario con il nome della pratica, la più vicina prima', () => {
    const m = studioModel(page, [live], now, step)
    expect(m.deadlines.map(d => [d.summary, d.caseName])).toEqual([['Deposito memoria', 'Rino: procedimento penale'], ['Rata 4 accordo', null]])
  })
  it("una cartella condivisa: la sessione su tutte le pratiche, l'esito una volta sola nel recap", () => {
    const m = studioModel(page, [live], now, step)
    expect(m.open.map(c => c.id)).toEqual(['r', 'a', 'b'])
    expect(m.open.find(c => c.id === 'b')?.session?.name).toBe('fs')
    expect(m.recap.filter(o => o.short === 'Lettera pronta').map(o => o.names)).toEqual([['Accordo Idemedia', 'Permessi 104']])
  })
  it('collassate le pratiche senza scadenza in avviso, sessione o attività recente', () => {
    expect(studioModel({ ...page, outcomes: [] }, [], now, step).rest.map(c => c.id)).toEqual(['a', 'b', 't'])
    expect(studioModel(page, [], now, step).rest.map(c => c.id)).toEqual(['t'])
  })
  it('una sessione chiusa non conta come accesa', () => {
    expect(studioModel(page, [{ ...live, state: 'gone' } as Session], now, step).open.find(c => c.id === 'a')?.session).toBe(null)
  })
  it('i Prossimi solo dalle scadenze in avviso, al massimo 3', () => {
    const many = { ...page, deadlines: Array.from({ length: 5 }, (_, i) => ({ ...page.deadlines[0], uid: `x${i}` })) }
    expect(studioModel(page, [], now, step).steps.map(s => s.label)).toEqual(['Prepara Deposito memoria'])
    expect(studioModel(many, [], now, step).steps).toHaveLength(3)
  })
})

describe('chip', () => {
  it('entro 40 caratteri, a fine parola, senza puntini', () => {
    const c = chip('Prepara', 'Ricorso al tribunale amministrativo regionale della Calabria')
    expect(c.length).toBeLessThanOrEqual(40)
    expect(c).toBe('Prepara Ricorso al tribunale')
    expect(c).not.toMatch(/…|\.\.\./)
  })
})

describe('stripScope', () => {
  it('toglie il blocco [studio] del relay e la riga vuota dopo', () => {
    expect(stripScope('[studio]\nPratiche: x → y\n[/studio]\n\nChe cosa scade?')).toBe('Che cosa scade?')
    expect(stripScope('Che cosa scade?')).toBe('Che cosa scade?')
    expect(stripScope('[studio] senza chiusura')).toBe('[studio] senza chiusura')
  })
})

describe('fix della revisione finale', () => {
  it('con la pagina Studio aperta ogni messaggio alla master porta lo scope, non solo il primo', () => {
    expect(scopeFor('master', 'studio')).toBe('studio')
    expect(scopeFor('master', null)).toBe(undefined)
    expect(scopeFor('blog', 'studio')).toBe(undefined)
    expect(scopeFor('master', null, 'studio')).toBe('studio')
  })
  it('Indietro sul telefono riduce prima il dock aperto della Studio, poi chiude la pagina', () => {
    expect(backStep('studio', true, false, null)).toBe('master')
    expect(backStep('studio', false, false, null)).toBe('page')
    expect(backStep('studio', true, true, null)).toBe('page')
    expect(backStep('recap', true, false, null)).toBe('page')
    expect(backStep(null, false, false, 'blog')).toBe('session')
    expect(backStep(null, true, false, null)).toBe('master')
    expect(backStep(null, false, false, null)).toBe(null)
  })
  it('un rifiuto del relay toglie i dati vecchi; calendario spento con le pratiche le tiene', () => {
    const off = 'Calendario spento'
    expect(studioResult({ id: 'x', ok: false, text: 'calendar off', at: 0 }, off)).toEqual({ page: null, error: off })
    expect(studioResult({ id: 'x', ok: false, text: 'boom', at: 0 }, off)).toEqual({ page: null, error: 'boom' })
    const r = studioResult({ id: 'x', ok: true, text: JSON.stringify({ ...page, deadlines: [], error: 'calendar off' }), at: 0 }, off)
    expect(r.error).toBe(off)
    expect(r.page?.cases).toHaveLength(4)
    expect(studioResult({ id: 'x', ok: true, text: JSON.stringify(page), at: 0 }, off).error).toBe(null)
  })
})
