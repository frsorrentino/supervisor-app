import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { adviceText, decodeConti, modelName, monthName, pagellaRows, ratePct } from './conti'
import { t } from './t'

// app-rendiconti-2-02-11: la pagina dei conti della fixture del relay (contratto 1.103, cmd-result-sample …0913).
const sample = JSON.parse(readFileSync('../contract/cmd-result-sample.json', 'utf8')) as { result: { id: string; text: string }[] }
const page = decodeConti(sample.result.find(r => r.id.endsWith('0913'))!.text)

describe('rendiconti e pagella', () => {
  it('il mese coi clienti e il dettaglio del rendiconto', () => {
    expect(page.mese).toBe('2026-09')
    expect(page.clienti).toEqual([{ project: 'work/acme-site', name: 'acme-site', n: 7, draft_at: null }])
    expect(page.dettaglio?.measured).toEqual({ tasks: 7, commits: 41, releases: 2 })
    expect(monthName(page.mese)).toBe('settembre 2026')
  })
  it('la pagella solo da 10 casi in su: Opus 14 casi sì, Sonnet 6 no', () => {
    const rows = pagellaRows(page.pagella)
    expect(rows.map(r => [modelName(r.model), r.kind, r.n, ratePct(r)])).toEqual([['Opus 5.5', 'fix', 14, 14]])
  })
  it('senza righe abbastanza piene la pagella non c\'è', () => {
    expect(pagellaRows({ since: 0, min: 10, rows: [{ model: 'claude-sonnet-5-5', kind: 'fix', n: 9, enough: false }] })).toEqual([])
    expect(pagellaRows(null)).toEqual([])
  })
})

// app-pagella-effort-28-11: dal contratto 1.103 il consiglio è un oggetto; una stringa resta testo.
describe('consiglio della pagella', () => {
  const a = { kind: 'fix', better: 'claude-opus-5-5', worse: 'claude-sonnet-5-5', rate_better: 0.08, rate_worse: 0.21 }
  it('oggetto coi tassi', () => expect(adviceText(a, t)).toBe('Correzioni: meglio Opus 5.5 (8% con difetti) che Sonnet 5.5 (21%)'))
  it('oggetto senza tassi', () => expect(adviceText({ ...a, rate_better: null }, t)).toBe('Correzioni: meglio Opus 5.5 che Sonnet 5.5'))
  it('una stringa resta com’è', () => expect(adviceText('un consiglio scritto', t)).toBe('un consiglio scritto'))
})
