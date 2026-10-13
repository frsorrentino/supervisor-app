import { describe, expect, it } from 'vitest'
import { pickLang } from './lang'

describe('la lingua della web app (Franz, 13/10 21:48)', () => {
  it('inglese di base, italiano se il browser mette l’italiano per primo', () => {
    expect(pickLang('', ['en-US', 'it'])).toBe('en')
    expect(pickLang('', ['it-IT', 'en'])).toBe('it')
    expect(pickLang('', ['IT'])).toBe('it')
    expect(pickLang('', ['de-DE'])).toBe('en')
    expect(pickLang('', [])).toBe('en')
  })

  it('?lang=en e ?lang=it la forzano, anche insieme a ?demo', () => {
    expect(pickLang('?demo&lang=en', ['it-IT'])).toBe('en')
    expect(pickLang('?lang=it&demo', ['en-US'])).toBe('it')
    expect(pickLang('?lang=fr', ['it-IT'])).toBe('it')
  })
})
