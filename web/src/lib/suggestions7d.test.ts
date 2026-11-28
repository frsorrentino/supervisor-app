import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import type { KpiSuggestions } from './contract'
import { acceptRate, suggestions7d } from './kpi'

// app-suggerimenti-push-27-11 (Franz 27/11 21:16, «la percentuale non si aggiorna in tempo reale»): contratto 1.164, la fixture del relay.
const sample = JSON.parse(readFileSync(new URL('../../../contract/suggestions-7d-sample.json', import.meta.url), 'utf8')) as { suggestions_7d: KpiSuggestions }
const kpi: KpiSuggestions = { accepted: 0, discussed: 0, later: 0, never: 0, expired: 23 }

describe('suggerimenti dei sette giorni dalla push (contratto 1.164)', () => {
  it('la fixture del relay dà 5 accettati su 23, cioè 22%', () => {
    expect(acceptRate(sample.suggestions_7d)).toBe(22)
  })

  it('con il campo nella push vale la push, anche se l\'op kpi è più vecchia', () => {
    expect(suggestions7d(sample, { kpi_suggestions_7d: kpi })).toEqual(sample.suggestions_7d)
  })

  it('senza il campo, con un relay precedente, vale l\'op kpi come prima', () => {
    expect(suggestions7d({}, { kpi_suggestions_7d: kpi })).toEqual(kpi)
    expect(suggestions7d({ suggestions_7d: null }, { kpi_suggestions_7d: kpi })).toEqual(kpi)
  })

  it('senza push né op kpi non c\'è niente da mostrare', () => {
    expect(suggestions7d(null, null)).toBeNull()
  })
})
