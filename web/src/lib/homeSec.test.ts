import { describe, expect, it } from 'vitest'
import { perTeSec } from './homeSec'
import { t } from './t'

// app-per-te-trascina-nome-26-11 (Franz 26/11 22:12): la sezione `decisioni` si chiama «Per te» anche mentre la trascini.
describe('perTeSec', () => {
  it('nome e icona di Per te, mai «Decidi»', () => {
    expect(perTeSec()).toEqual({ icon: 'svg:hand', name: t.forYou })
    expect(perTeSec().name).not.toBe('Decidi')
  })
})
