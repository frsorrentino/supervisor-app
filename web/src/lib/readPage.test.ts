import { describe, expect, it } from 'vitest'
import { decodeDecisions, readPage } from './decideDecode'
import { resultTimeoutMs } from './transport'

// «Da decidere rimane "in arrivo dal PC" dalla PWA» (app-pwa-decisioni-03-11): il 03/11 alle 12:46 il comando è arrivato al
// relay 29,5 s dopo la partenza; a 20 s la PWA l'aveva già lasciato e la pagina restava vuota senza dirlo.
describe('pagine lette dal PC', () => {
  it('aspettano più del ritardo visto dal vivo', () => {
    for (const op of ['decisioni', 'prove', 'seguiti', 'prima_raccolta'] as const) expect(resultTimeoutMs(op)).toBeGreaterThan(29_500)
    expect(resultTimeoutMs('transcript')).toBe(20_000)
  })
  // app-monitoraggi-attesa-10-11: dal log del relay del 10/11 `monitors` è rimasto in fila fino a 51,1 s; a 20 s la sezione Monitoraggi spariva.
  it('i Monitoraggi aspettano più della fila vista dal vivo', () => {
    for (const op of ['monitors', 'monitor_refresh'] as const) expect(resultTimeoutMs(op)).toBeGreaterThan(51_100)
  })
  it('senza risposta dicono che il PC non ha risposto, invece di restare in arrivo', () => {
    expect(readPage(null, decodeDecisions, null)).toEqual({ page: null, failed: true })
    expect(readPage({ id: 'x', ok: false, text: 'timeout' } as never, decodeDecisions, null).failed).toBe(true)
  })
  it('una risposta illeggibile è un errore, non un caricamento infinito', () => {
    expect(readPage({ id: 'x', ok: true, text: '{"groups": [' } as never, decodeDecisions, null)).toEqual({ page: null, failed: true })
  })
  it('Riprova con la risposta carica la pagina; un secondo errore non toglie quella già in vista', () => {
    const ok = readPage({ id: 'x', ok: true, text: '{"n": 20, "groups": []}' } as never, decodeDecisions, null)
    expect(ok.failed).toBe(false)
    expect(ok.page?.n).toBe(20)
    expect(readPage(null, decodeDecisions, ok.page)).toEqual({ page: ok.page, failed: false })
  })
})
