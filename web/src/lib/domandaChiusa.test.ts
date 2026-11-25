import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { waitGone } from './waitGone'

// app-domanda-chiusa-25-11: una domanda chiusa, risposta o sostituita non è un errore; gli errori veri sì.
describe('domanda chiusa', () => {
  it('il rifiuto del contratto è una domanda chiusa', () => {
    expect(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')).toContain('this question is no longer waiting')
    expect(waitGone('w0000000000: this question is no longer waiting (closed or already answered)')).toBe(true)
  })
  it('il testo italiano del relay va in silenzio', () => {
    expect(waitGone('waf1a079167: questa domanda non aspetta più (chiusa o già risposta)')).toBe(true)
  })
  it('gli errori veri restano errori', () => {
    expect(waitGone('offline')).toBe(false)
    expect(waitGone('permission denied: device not paired')).toBe(false)
    expect(waitGone(null)).toBe(false)
  })
})
